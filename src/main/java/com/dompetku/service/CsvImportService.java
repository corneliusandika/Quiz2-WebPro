package com.dompetku.service;

import com.dompetku.dao.CategoryDAO;
import com.dompetku.dao.TransactionDAO;
import com.dompetku.dao.WalletDAO;
import com.dompetku.model.Category;
import com.dompetku.model.Transaction;
import com.dompetku.model.User;
import com.dompetku.model.Wallet;
import com.dompetku.util.MoneyParser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CsvImportService {

    public static final int MAX_BYTES = 2 * 1024 * 1024;
    public static final int MAX_ROWS = 2000;

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("d/M/yy")
    };

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final WalletDAO walletDAO = new WalletDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    public Batch parse(byte[] bytes, int defaultWalletId, boolean createCategories) throws ImportException {
        if (bytes == null || bytes.length == 0) {
            throw new ImportException("File CSV kosong.");
        }
        if (bytes.length > MAX_BYTES) {
            throw new ImportException("File terlalu besar. Maksimal 2 MB.");
        }
        String text = new String(bytes, StandardCharsets.UTF_8).replace("\uFEFF", "");
        List<List<String>> table = readCsv(text);
        if (table.size() < 2) {
            throw new ImportException("CSV harus berisi baris header dan minimal satu data.");
        }
        if (table.size() - 1 > MAX_ROWS) {
            throw new ImportException("Maksimal " + MAX_ROWS + " baris data per import.");
        }
        List<String> headers = table.get(0);
        if (headers.size() < 2) {
            throw new ImportException("Header CSV tidak valid.");
        }
        Batch batch = new Batch();
        batch.headers = headers;
        batch.rows = table.subList(1, table.size());
        batch.defaultWalletId = defaultWalletId;
        batch.createCategories = createCategories;
        batch.mapping = autoMap(headers);
        return batch;
    }

    public Preview preview(Batch batch, User user) throws SQLException {
        Preview preview = new Preview();
        preview.batch = batch;
        preview.mapping = batch.mapping;
        if (!batch.mapping.isReady()) {
            preview.message = "Pilih kolom tanggal, nominal, dan dompet terlebih dahulu.";
            return preview;
        }

        Map<String, Integer> wallets = walletMap(user.getId());
        Map<String, Category> categories = categoryMap(user.getId());
        boolean validDefaultWallet = wallets.containsValue(batch.defaultWalletId);
        Set<String> batchKeys = new HashSet<>();
        Set<String> databaseKeys = duplicateKeysInRange(batch, user);

        for (int i = 0; i < batch.rows.size(); i++) {
            Row row = parseRow(batch, batch.rows.get(i), i + 2, user, wallets, categories,
                    validDefaultWallet, batchKeys, databaseKeys);
            if (row.error == null && row.duplicate) {
                preview.duplicateCount++;
            } else if (row.error == null) {
                preview.validCount++;
            } else {
                preview.errorCount++;
            }
            preview.rows.add(row);
        }
        preview.canImport = preview.validCount > 0;
        preview.message = preview.canImport
                ? preview.validCount + " baris siap diimport."
                : "Tidak ada baris valid untuk diimport.";
        return preview;
    }

    public Result importRows(Batch batch, User user) throws SQLException {
        Preview preview = preview(batch, user);
        Result result = new Result();
        result.total = preview.rows.size();
        result.invalid = preview.errorCount;
        result.duplicate = preview.duplicateCount;
        if (!preview.canImport) {
            result.errors.addAll(preview.rows);
            return result;
        }

        Map<String, Integer> createdCategories = new HashMap<>();
        List<Transaction> pending = new ArrayList<>();
        List<Row> pendingRows = new ArrayList<>();
        for (Row row : preview.rows) {
            if (row.error != null || row.duplicate) {
                result.errors.add(row);
                continue;
            }
            try {
                Integer categoryId = row.categoryId;
                if (categoryId == null && row.newCategory) {
                    String key = row.type + "|" + row.categoryName.toLowerCase(Locale.ROOT);
                    categoryId = createdCategories.get(key);
                    if (categoryId == null) {
                        Category category = new Category();
                        category.setUserId(user.getId());
                        category.setName(row.categoryName);
                        category.setType(row.type);
                        category.setColor("income".equals(row.type) ? "#10b981" : "#ef4444");
                        category.setIcon("tag");
                        categoryDAO.create(category);
                        categoryId = category.getId();
                        createdCategories.put(key, categoryId);
                    }
                }
                Transaction transaction = new Transaction();
                transaction.setUserId(user.getId());
                transaction.setWalletId(row.walletId);
                transaction.setCategoryId(categoryId);
                transaction.setType(row.type);
                transaction.setAmount(row.amount);
                transaction.setTransactionDate(row.date);
                transaction.setNote(row.note);
                pending.add(transaction);
                pendingRows.add(row);
            } catch (Exception e) {
                row.error = "Gagal menyimpan baris ini.";
                result.invalid++;
                result.errors.add(row);
            }
        }

        try {
            transactionDAO.createBatch(pending);
            result.imported = pending.size();
        } catch (SQLException e) {
            for (Row row : pendingRows) {
                row.error = "Gagal menyimpan batch. Coba import ulang.";
                result.errors.add(row);
            }
            result.invalid += pendingRows.size();
        }
        return result;
    }

    private Set<String> duplicateKeysInRange(Batch batch, User user) throws SQLException {
        LocalDate from = null;
        LocalDate to = null;
        for (List<String> cells : batch.rows) {
            LocalDate date = parseDate(cell(cells, batch.mapping.dateIndex));
            if (date == null) {
                continue;
            }
            if (from == null || date.isBefore(from)) { from = date; }
            if (to == null || date.isAfter(to)) { to = date; }
        }
        Set<String> keys = new HashSet<>();
        if (from == null) {
            return keys;
        }
        for (Transaction transaction : transactionDAO.findDuplicateCandidates(user.getId(), from, to)) {
            keys.add(duplicateKey(transaction.getTransactionDate(), transaction.getType(),
                    transaction.getAmount(), transaction.getWalletId(), transaction.getCategoryId(),
                    null, transaction.getNote()));
        }
        return keys;
    }

    private static String duplicateKey(LocalDate date, String type, BigDecimal amount, Integer walletId,
                                       Integer categoryId, String categoryName, String note) {
        String categoryKey = categoryId != null
                ? "id:" + categoryId
                : categoryName != null
                    ? "new:" + categoryName.toLowerCase(Locale.ROOT)
                    : "none";
        return date + "|" + type + "|" + amount.stripTrailingZeros().toPlainString() + "|" + walletId
                + "|" + categoryKey + "|" + (note == null ? "" : note);
    }

    private Row parseRow(Batch batch, List<String> cells, int line, User user,
                         Map<String, Integer> wallets, Map<String, Category> categories,
                         boolean validDefaultWallet, Set<String> batchKeys, Set<String> databaseKeys) {
        Row row = new Row();
        row.line = line;
        row.rawDate = cell(cells, batch.mapping.dateIndex);
        row.rawType = cell(cells, batch.mapping.typeIndex);
        row.rawCategory = cell(cells, batch.mapping.categoryIndex);
        row.rawWallet = cell(cells, batch.mapping.walletIndex);
        row.rawAmount = cell(cells, batch.mapping.amountIndex);
        row.rawNote = cell(cells, batch.mapping.noteIndex);

        if (row.rawDate == null) {
            row.error = "Tanggal kosong.";
            return row;
        }
        row.date = parseDate(row.rawDate);
        if (row.date == null) {
            row.error = "Tanggal tidak valid.";
            return row;
        }
        if (row.rawAmount == null) {
            row.error = "Nominal kosong.";
            return row;
        }
        row.amount = parseAmount(row.rawAmount);
        if (row.amount == null || row.amount.signum() <= 0) {
            row.error = "Nominal tidak valid.";
            return row;
        }
        if (row.amount.compareTo(new BigDecimal("9999999999999.99")) > 0) {
            row.error = "Nominal terlalu besar.";
            return row;
        }
        if (row.amount.scale() > 2) {
            row.amount = row.amount.setScale(2, RoundingMode.HALF_UP);
        }

        row.type = parseType(row.rawType);
        if (row.type == null) {
            row.error = "Jenis transaksi tidak dikenali.";
            return row;
        }

        if (row.rawWallet != null) {
            row.walletId = wallets.get(row.rawWallet.toLowerCase(Locale.ROOT));
            row.walletName = row.rawWallet;
        }
        if (row.walletId == null && validDefaultWallet) {
            row.walletId = batch.defaultWalletId;
            row.walletName = "Dompet default";
        }
        if (row.walletId == null) {
            row.error = "Dompet tidak ditemukan.";
            return row;
        }

        if (row.rawCategory != null) {
            Category category = categories.get(row.type + "|" + row.rawCategory.toLowerCase(Locale.ROOT));
            if (category != null) {
                row.categoryId = category.getId();
                row.categoryName = category.getName();
            } else if (row.rawCategory.length() > 60) {
                row.error = "Nama kategori terlalu panjang.";
                return row;
            } else if (batch.createCategories) {
                row.categoryName = row.rawCategory;
                row.newCategory = true;
            } else {
                row.error = "Kategori tidak ditemukan. Aktifkan pembuatan kategori otomatis.";
                return row;
            }
        }

        row.note = row.rawNote == null ? null
                : row.rawNote.length() > 255 ? row.rawNote.substring(0, 255) : row.rawNote;
        String key = duplicateKey(row.date, row.type, row.amount, row.walletId,
                row.categoryId, row.categoryName, row.note);
        if (!batchKeys.add(key)) {
            row.duplicate = true;
            row.duplicateReason = "Duplikat di dalam file.";
            return row;
        }
        if (databaseKeys.contains(key)) {
            row.duplicate = true;
            row.duplicateReason = "Sudah ada di database.";
        }
        return row;
    }

    private Map<String, Integer> walletMap(int userId) throws SQLException {
        Map<String, Integer> map = new HashMap<>();
        for (Wallet wallet : walletDAO.findAll(userId)) {
            map.put(wallet.getName().toLowerCase(Locale.ROOT), wallet.getId());
        }
        return map;
    }

    private Map<String, Category> categoryMap(int userId) throws SQLException {
        Map<String, Category> map = new HashMap<>();
        for (Category category : categoryDAO.findAll(userId)) {
            map.put(category.getType() + "|" + category.getName().toLowerCase(Locale.ROOT), category);
        }
        return map;
    }

    private static Mapping autoMap(List<String> headers) {
        Mapping mapping = new Mapping();
        for (int i = 0; i < headers.size(); i++) {
            String key = normalizeHeader(headers.get(i));
            if (mapping.dateIndex < 0 && matches(key, "tanggal", "date", "tgl", "transactiondate", "waktu")) {
                mapping.dateIndex = i;
            } else if (mapping.typeIndex < 0 && matches(key, "jenis", "type", "tipe", "jenistransaksi", "masukkeluar")) {
                mapping.typeIndex = i;
            } else if (mapping.categoryIndex < 0 && matches(key, "kategori", "category")) {
                mapping.categoryIndex = i;
            } else if (mapping.walletIndex < 0 && matches(key, "dompet", "wallet", "sumber", "akun")) {
                mapping.walletIndex = i;
            } else if (mapping.amountIndex < 0 && matches(key, "nominal", "amount", "jumlah", "nilai", "total")) {
                mapping.amountIndex = i;
            } else if (mapping.noteIndex < 0 && matches(key, "catatan", "note", "keterangan", "deskripsi", "memo")) {
                mapping.noteIndex = i;
            }
        }
        return mapping;
    }

    private static boolean matches(String value, String... aliases) {
        for (String alias : aliases) {
            if (alias.equals(value)) { return true; }
        }
        return false;
    }

    private static String normalizeHeader(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String cell(List<String> cells, int index) {
        if (index < 0 || index >= cells.size()) { return null; }
        String value = cells.get(index);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    public static LocalDate parseDate(String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() > 10 && text.charAt(10) == ' ') {
            text = text.substring(0, 10);
        }
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next accepted format.
            }
        }
        return null;
    }

    public static BigDecimal parseAmount(String value) {
        return MoneyParser.parse(value);
    }

    private static String parseType(String value) {
        if (value == null) { return "expense"; }
        String type = value.toLowerCase(Locale.ROOT).trim();
        if ("income".equals(type) || "pemasukan".equals(type) || "masuk".equals(type)
                || "kredit".equals(type) || "credit".equals(type) || "in".equals(type)) {
            return "income";
        }
        if ("expense".equals(type) || "pengeluaran".equals(type) || "keluar".equals(type)
                || "debit".equals(type) || "debit".equals(type) || "out".equals(type)) {
            return "expense";
        }
        return null;
    }

    private static List<List<String>> readCsv(String text) throws ImportException {
        char delimiter = detectDelimiter(text);
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cell.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == delimiter) {
                row.add(cell.toString().trim());
                cell.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') { i++; }
                row.add(cell.toString().trim());
                cell.setLength(0);
                if (hasValue(row)) { rows.add(row); }
                row = new ArrayList<>();
                if (rows.size() > MAX_ROWS + 1) {
                    throw new ImportException("Maksimal " + MAX_ROWS + " baris data per import.");
                }
            } else {
                cell.append(c);
            }
        }
        row.add(cell.toString().trim());
        if (hasValue(row)) { rows.add(row); }
        return rows;
    }

    private static boolean hasValue(List<String> row) {
        for (String value : row) {
            if (value != null && !value.trim().isEmpty()) { return true; }
        }
        return false;
    }

    private static char detectDelimiter(String text) {
        int commas = 0;
        int semicolons = 0;
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            } else if (!quoted && (c == '\n' || c == '\r')) {
                break;
            } else if (!quoted && c == ',') {
                commas++;
            } else if (!quoted && c == ';') {
                semicolons++;
            }
        }
        return semicolons > commas ? ';' : ',';
    }

    public static class ImportException extends Exception {
        public ImportException(String message) {
            super(message);
        }
    }

    public static class Mapping {
        public int dateIndex = -1;
        public int typeIndex = -1;
        public int categoryIndex = -1;
        public int walletIndex = -1;
        public int amountIndex = -1;
        public int noteIndex = -1;

        public boolean isReady() {
            return dateIndex >= 0 && amountIndex >= 0;
        }

        public int getDateIndex() { return dateIndex; }
        public int getTypeIndex() { return typeIndex; }
        public int getCategoryIndex() { return categoryIndex; }
        public int getWalletIndex() { return walletIndex; }
        public int getAmountIndex() { return amountIndex; }
        public int getNoteIndex() { return noteIndex; }
        public void setDateIndex(int value) { dateIndex = value; }
        public void setTypeIndex(int value) { typeIndex = value; }
        public void setCategoryIndex(int value) { categoryIndex = value; }
        public void setWalletIndex(int value) { walletIndex = value; }
        public void setAmountIndex(int value) { amountIndex = value; }
        public void setNoteIndex(int value) { noteIndex = value; }
    }

    public static class Batch {
        private List<String> headers;
        private List<List<String>> rows;
        private Mapping mapping;
        private int defaultWalletId;
        private boolean createCategories;

        public List<String> getHeaders() { return headers; }
        public List<List<String>> getRows() { return rows; }
        public Mapping getMapping() { return mapping; }
        public int getDefaultWalletId() { return defaultWalletId; }
        public boolean isCreateCategories() { return createCategories; }
        public int getRowCount() { return rows == null ? 0 : rows.size(); }
        public void setDefaultWalletId(int value) { defaultWalletId = value; }
        public void setCreateCategories(boolean value) { createCategories = value; }
    }

    public static class Preview {
        private Batch batch;
        private Mapping mapping;
        private final List<Row> rows = new ArrayList<>();
        private int validCount;
        private int duplicateCount;
        private int errorCount;
        private boolean canImport;
        private String message;

        public Batch getBatch() { return batch; }
        public Mapping getMapping() { return mapping; }
        public List<Row> getRows() { return rows; }
        public int getValidCount() { return validCount; }
        public int getDuplicateCount() { return duplicateCount; }
        public int getErrorCount() { return errorCount; }
        public boolean isCanImport() { return canImport; }
        public String getMessage() { return message; }
    }

    public static class Row {
        private int line;
        private String rawDate;
        private String rawType;
        private String rawCategory;
        private String rawWallet;
        private String rawAmount;
        private String rawNote;
        private LocalDate date;
        private BigDecimal amount;
        private String type;
        private Integer walletId;
        private String walletName;
        private Integer categoryId;
        private String categoryName;
        private String note;
        private String error;
        private boolean duplicate;
        private String duplicateReason;
        private boolean newCategory;

        public int getLine() { return line; }
        public String getRawDate() { return rawDate; }
        public String getRawType() { return rawType; }
        public String getRawCategory() { return rawCategory; }
        public String getRawWallet() { return rawWallet; }
        public String getRawAmount() { return rawAmount; }
        public String getRawNote() { return rawNote; }
        public LocalDate getDate() { return date; }
        public BigDecimal getAmount() { return amount; }
        public String getType() { return type; }
        public Integer getWalletId() { return walletId; }
        public String getWalletName() { return walletName; }
        public Integer getCategoryId() { return categoryId; }
        public String getCategoryName() { return categoryName; }
        public String getNote() { return note; }
        public String getError() { return error; }
        public boolean isDuplicate() { return duplicate; }
        public String getDuplicateReason() { return duplicateReason; }
        public boolean isNewCategory() { return newCategory; }
        public boolean isValid() { return error == null && !duplicate; }
    }

    public static class Result {
        private int total;
        private int imported;
        private int duplicate;
        private int invalid;
        private final List<Row> errors = new ArrayList<>();

        public int getTotal() { return total; }
        public int getImported() { return imported; }
        public int getDuplicate() { return duplicate; }
        public int getInvalid() { return invalid; }
        public List<Row> getErrors() { return errors; }
    }
}
