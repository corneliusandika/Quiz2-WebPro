package com.dompetku.service;

import com.dompetku.dao.TransactionDAO;
import com.dompetku.dao.WalletDAO;
import com.dompetku.model.CategoryTotal;
import com.dompetku.model.Transaction;
import com.dompetku.model.User;
import com.dompetku.model.Wallet;
import com.dompetku.util.Formats;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Generates the monthly report as an A4 PDF without requiring a browser. */
public class ReportPdfService {

    private static final int MAX_ROWS = 2000;
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final WalletDAO walletDAO = new WalletDAO();

    public void write(User user, LocalDate month, OutputStream out) throws SQLException, IOException {
        String key = String.format("%04d-%02d", month.getYear(), month.getMonthValue());
        BigDecimal income = transactionDAO.sum(user.getId(), "income", month);
        BigDecimal expense = transactionDAO.sum(user.getId(), "expense", month);
        BigDecimal net = income.subtract(expense);
        int transactionCount = transactionDAO.countFiltered(user.getId(), key, null, null, null, null);
        List<CategoryTotal> categories = transactionDAO.expenseByCategory(user.getId(), month);
        List<Wallet> wallets = walletDAO.findAll(user.getId());
        List<Transaction> topExpenses = transactionDAO.topExpenses(user.getId(), month, 5);
        List<Transaction> rows = transactionDAO.find(user.getId(), key, null, null, null, null, MAX_ROWS, 0);

        try (PDDocument document = new PDDocument()) {
            document.getDocumentInformation().setTitle("Laporan Dompetku - " + Formats.month(month));
            document.getDocumentInformation().setAuthor("Dompetku");
            document.getDocumentInformation().setSubject("Personal expense tracker report");

            PdfWriter writer = new PdfWriter(document, user, month);
            writer.summary(income, expense, net, transactionCount, walletDAO.totalBalance(user.getId()));
            writer.categories(categories);
            writer.wallets(wallets);
            writer.topExpenses(topExpenses);
            writer.transactions(rows, transactionCount);
            writer.finish();
            document.save(out);
        }
    }

    private static final class PdfWriter {
        private static final float MARGIN = 42;
        private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
        private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
        private static final float CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2);
        private static final float FOOTER_Y = 24;
        private static final Color TEXT = new Color(15, 23, 42);
        private static final Color MUTED = new Color(91, 107, 130);
        private static final Color GREEN = new Color(16, 185, 129);
        private static final Color ROSE = new Color(225, 29, 72);
        private static final Color BLUE = new Color(37, 99, 235);
        private static final Color VIOLET = new Color(124, 58, 237);
        private static final Color LINE = new Color(226, 232, 240);
        private static final Color SOFT = new Color(248, 250, 252);

        private final PDDocument document;
        private final User user;
        private final LocalDate month;
        private PDPage page;
        private PDPageContentStream content;
        private float y;
        private int pageNo;

        private PdfWriter(PDDocument document, User user, LocalDate month) throws IOException {
            this.document = document;
            this.user = user;
            this.month = month;
            newPage();
        }

        private void newPage() throws IOException {
            if (content != null) {
                footer();
                content.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            pageNo++;
            y = PAGE_HEIGHT - MARGIN;
            header();
        }

        private void header() throws IOException {
            text("DOMPETKU", MARGIN, y, PDType1Font.HELVETICA_BOLD, 17, TEXT);
            text("Laporan keuangan bulanan", MARGIN, y - 15, PDType1Font.HELVETICA, 8, MUTED);
            right(Formats.month(month), PAGE_WIDTH - MARGIN, y, PDType1Font.HELVETICA_BOLD, 10, TEXT);
            right(user.getName(), PAGE_WIDTH - MARGIN, y - 14, PDType1Font.HELVETICA, 8, MUTED);
            y -= 30;
            rule(MARGIN, y, CONTENT_WIDTH, GREEN);
            y -= 20;
        }

        private void footer() throws IOException {
            rule(MARGIN, FOOTER_Y + 12, CONTENT_WIDTH, LINE);
            text("Dompetku - Personal expense tracker", MARGIN, FOOTER_Y, PDType1Font.HELVETICA, 7, MUTED);
            right("Halaman " + pageNo, PAGE_WIDTH - MARGIN, FOOTER_Y, PDType1Font.HELVETICA, 7, MUTED);
        }

        private void summary(BigDecimal income, BigDecimal expense, BigDecimal net,
                             int count, BigDecimal balance) throws IOException {
            ensure(72);
            float gap = 8;
            float cardWidth = (CONTENT_WIDTH - gap * 3) / 4;
            float cardHeight = 55;
            summaryCard(MARGIN, y - cardHeight, cardWidth, cardHeight, "Pemasukan", Formats.rupiah(income), GREEN);
            summaryCard(MARGIN + cardWidth + gap, y - cardHeight, cardWidth, cardHeight,
                    "Pengeluaran", Formats.rupiah(expense), ROSE);
            summaryCard(MARGIN + (cardWidth + gap) * 2, y - cardHeight, cardWidth, cardHeight,
                    "Selisih bersih", Formats.rupiah(net), BLUE);
            summaryCard(MARGIN + (cardWidth + gap) * 3, y - cardHeight, cardWidth, cardHeight,
                    "Transaksi", count + " catatan", VIOLET);
            y -= cardHeight + 12;
            text("Total saldo dompet saat ini: " + Formats.rupiah(balance), MARGIN, y,
                    PDType1Font.HELVETICA_BOLD, 8, MUTED);
            y -= 24;
        }

        private void summaryCard(float x, float y, float width, float height,
                                 String label, String value, Color accent) throws IOException {
            fill(x, y, width, height, SOFT);
            fill(x, y + height - 3, width, 3, accent);
            text(label, x + 9, y + height - 18, PDType1Font.HELVETICA, 7, MUTED);
            text(value, x + 9, y + 17, PDType1Font.HELVETICA_BOLD, 11, TEXT);
        }

        private void categories(List<CategoryTotal> categories) throws IOException {
            section("Pengeluaran per kategori");
            if (categories.isEmpty()) {
                text("Belum ada pengeluaran pada bulan ini.", MARGIN, y, PDType1Font.HELVETICA, 8, MUTED);
                y -= 22;
                return;
            }
            float[] widths = {230, 130, 80};
            tableHeader(new String[]{"Kategori", "Total", "Porsi"}, widths);
            int index = 0;
            for (CategoryTotal item : categories) {
                if (y - 20 < FOOTER_Y + 28) {
                    newPage();
                    tableHeader(new String[]{"Kategori", "Total", "Porsi"}, widths);
                }
                rowBackground(index++);
                text(fit(item.getName(), PDType1Font.HELVETICA, 8, widths[0] - 14),
                        MARGIN + 7, y, PDType1Font.HELVETICA, 8, TEXT);
                right(Formats.rupiah(item.getTotal()), MARGIN + widths[0] + widths[1] - 7, y,
                        PDType1Font.HELVETICA, 8, TEXT);
                right(item.getPercentage() + "%", MARGIN + CONTENT_WIDTH - 7, y,
                        PDType1Font.HELVETICA, 8, MUTED);
                y -= 19;
            }
            y -= 8;
        }

        private void wallets(List<Wallet> wallets) throws IOException {
            section("Saldo per dompet");
            if (wallets.isEmpty()) {
                text("Belum ada dompet.", MARGIN, y, PDType1Font.HELVETICA, 8, MUTED);
                y -= 22;
                return;
            }
            float[] widths = {230, 120, 140};
            tableHeader(new String[]{"Dompet", "Jenis", "Saldo"}, widths);
            int index = 0;
            for (Wallet wallet : wallets) {
                if (y - 20 < FOOTER_Y + 28) {
                    newPage();
                    tableHeader(new String[]{"Dompet", "Jenis", "Saldo"}, widths);
                }
                rowBackground(index++);
                text(fit(wallet.getName(), PDType1Font.HELVETICA, 8, widths[0] - 14),
                        MARGIN + 7, y, PDType1Font.HELVETICA, 8, TEXT);
                text(walletType(wallet.getType()), MARGIN + widths[0] + 7, y,
                        PDType1Font.HELVETICA, 8, MUTED);
                right(Formats.rupiah(wallet.getBalance()), MARGIN + CONTENT_WIDTH - 7, y,
                        PDType1Font.HELVETICA, 8, TEXT);
                y -= 19;
            }
            y -= 8;
        }

        private void topExpenses(List<Transaction> items) throws IOException {
            if (items.isEmpty()) { return; }
            section("Pengeluaran terbesar");
            float[] widths = {65, 270, 130};
            tableHeader(new String[]{"Tanggal", "Catatan", "Nominal"}, widths);
            int index = 0;
            for (Transaction item : items) {
                if (y - 20 < FOOTER_Y + 28) {
                    newPage();
                    tableHeader(new String[]{"Tanggal", "Catatan", "Nominal"}, widths);
                }
                rowBackground(index++);
                text(Formats.date(item.getTransactionDate()), MARGIN + 7, y,
                        PDType1Font.HELVETICA, 8, MUTED);
                String note = item.getNote() == null || item.getNote().trim().isEmpty()
                        ? item.getCategoryName() : item.getNote();
                text(fit(note == null ? "-" : note, PDType1Font.HELVETICA, 8, widths[1] - 14),
                        MARGIN + widths[0] + 7, y, PDType1Font.HELVETICA, 8, TEXT);
                right(Formats.rupiah(item.getAmount()), MARGIN + CONTENT_WIDTH - 7, y,
                        PDType1Font.HELVETICA, 8, TEXT);
                y -= 19;
            }
            y -= 8;
        }

        private void transactions(List<Transaction> items, int totalCount) throws IOException {
            section("Daftar transaksi");
            if (items.isEmpty()) {
                text("Belum ada transaksi pada bulan ini.", MARGIN, y, PDType1Font.HELVETICA, 8, MUTED);
                y -= 22;
                return;
            }
            if (totalCount > items.size()) {
                text("Menampilkan " + items.size() + " dari " + totalCount
                                + " transaksi. Gunakan CSV untuk seluruh data.",
                        MARGIN, y, PDType1Font.HELVETICA, 7, MUTED);
                y -= 16;
            }
            float[] widths = {58, 54, 105, 90, 130, 74};
            String[] headers = {"Tanggal", "Jenis", "Kategori", "Dompet", "Catatan", "Nominal"};
            tableHeader(headers, widths);
            int index = 0;
            for (Transaction item : items) {
                if (y - 18 < FOOTER_Y + 28) {
                    newPage();
                    tableHeader(headers, widths);
                }
                rowBackground(index++);
                float x = MARGIN + 7;
                text(Formats.date(item.getTransactionDate()), x, y, PDType1Font.HELVETICA, 7.5f, MUTED);
                x += widths[0];
                text("income".equals(item.getType()) ? "Masuk" : "Keluar", x, y,
                        PDType1Font.HELVETICA, 7.5f, "income".equals(item.getType()) ? GREEN : ROSE);
                x += widths[1];
                text(fit(value(item.getCategoryName(), "Tanpa kategori"), PDType1Font.HELVETICA, 7.5f,
                        widths[2] - 8), x, y, PDType1Font.HELVETICA, 7.5f, TEXT);
                x += widths[2];
                text(fit(value(item.getWalletName(), "-"), PDType1Font.HELVETICA, 7.5f,
                        widths[3] - 8), x, y, PDType1Font.HELVETICA, 7.5f, TEXT);
                x += widths[3];
                text(fit(value(item.getNote(), "-"), PDType1Font.HELVETICA, 7.5f,
                        widths[4] - 8), x, y, PDType1Font.HELVETICA, 7.5f, MUTED);
                x += widths[4];
                right(Formats.rupiah(item.getAmount()), MARGIN + CONTENT_WIDTH - 7, y,
                        PDType1Font.HELVETICA, 7.5f, TEXT);
                y -= 18;
            }
        }

        private void section(String title) throws IOException {
            ensure(40);
            y -= 4;
            text(title, MARGIN, y, PDType1Font.HELVETICA_BOLD, 11, TEXT);
            y -= 17;
        }

        private void tableHeader(String[] headers, float[] widths) throws IOException {
            ensure(22);
            fill(MARGIN, y - 4, CONTENT_WIDTH, 18, new Color(238, 242, 248));
            float x = MARGIN + 7;
            for (int i = 0; i < headers.length; i++) {
                text(headers[i], x, y, PDType1Font.HELVETICA_BOLD, 7, MUTED);
                x += widths[i];
            }
            y -= 18;
        }

        private void rowBackground(int index) throws IOException {
            if (index % 2 == 1) {
                fill(MARGIN, y - 4, CONTENT_WIDTH, 17, new Color(250, 251, 253));
            }
        }

        private void ensure(float height) throws IOException {
            if (y - height < FOOTER_Y + 28) {
                newPage();
            }
        }

        private void finish() throws IOException {
            footer();
            content.close();
        }

        private void text(String value, float x, float y, PDType1Font font, float size, Color color)
                throws IOException {
            content.beginText();
            content.setFont(font, size);
            setFill(color);
            content.newLineAtOffset(x, y);
            content.showText(safe(value));
            content.endText();
        }

        private void right(String value, float rightX, float y, PDType1Font font, float size, Color color)
                throws IOException {
            String safe = safe(value);
            float width = font.getStringWidth(safe) / 1000 * size;
            text(safe, rightX - width, y, font, size, color);
        }

        private void rule(float x, float y, float width, Color color) throws IOException {
            content.setStrokingColor(color.getRed(), color.getGreen(), color.getBlue());
            content.setLineWidth(.6f);
            content.moveTo(x, y);
            content.lineTo(x + width, y);
            content.stroke();
        }

        private void fill(float x, float y, float width, float height, Color color) throws IOException {
            setFill(color);
            content.addRect(x, y, width, height);
            content.fill();
        }

        private void setFill(Color color) throws IOException {
            content.setNonStrokingColor(color.getRed(), color.getGreen(), color.getBlue());
        }

        private String fit(String value, PDType1Font font, float size, float maxWidth) throws IOException {
            String text = safe(value);
            if (font.getStringWidth(text) / 1000 * size <= maxWidth) { return text; }
            String ellipsis = "...";
            while (text.length() > 0
                    && font.getStringWidth(text + ellipsis) / 1000 * size > maxWidth) {
                text = text.substring(0, text.length() - 1);
            }
            return text + ellipsis;
        }

        private String safe(String value) {
            if (value == null) { return "-"; }
            String normalized = value.replace('\r', ' ').replace('\n', ' ').trim();
            StringBuilder out = new StringBuilder(normalized.length());
            for (int i = 0; i < normalized.length(); i++) {
                char c = normalized.charAt(i);
                out.append(c >= 32 && c <= 255 ? c : '?');
            }
            return out.toString();
        }

        private String value(String value, String fallback) {
            return value == null || value.trim().isEmpty() ? fallback : value;
        }

        private String walletType(String type) {
            if ("cash".equals(type)) { return "Tunai"; }
            if ("bank".equals(type)) { return "Bank"; }
            if ("ewallet".equals(type)) { return "E-Wallet"; }
            return type == null ? "-" : type;
        }
    }
}
