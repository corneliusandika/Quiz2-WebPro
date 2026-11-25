package com.dompetku.servlet;

import com.dompetku.dao.AuditDAO;
import com.dompetku.dao.WalletDAO;
import com.dompetku.model.User;
import com.dompetku.service.CsvImportService;
import com.dompetku.service.CsvImportService.Batch;
import com.dompetku.service.CsvImportService.ImportException;
import com.dompetku.service.CsvImportService.Mapping;
import com.dompetku.service.CsvImportService.Result;
import com.dompetku.web.BaseServlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

@WebServlet("/app/transactions/import/*")
@MultipartConfig(maxFileSize = CsvImportService.MAX_BYTES, maxRequestSize = CsvImportService.MAX_BYTES + 65536)
public class TransactionImportServlet extends BaseServlet {

    private static final String BATCH = "csvImportBatch";
    private final CsvImportService importService = new CsvImportService();
    private final WalletDAO walletDAO = new WalletDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = currentUser(req);
        String path = req.getPathInfo();
        try {
            if (path == null || "/".equals(path)) {
                req.setAttribute("wallets", walletDAO.findAll(user.getId()));
                render(req, resp, "transactions/import/upload");
            } else if ("/review".equals(path)) {
                review(req, resp, user);
            } else {
                notFound(resp, "Halaman tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = currentUser(req);
        String path = req.getPathInfo();
        try {
            if ("/preview".equals(path)) {
                preview(req, resp, user);
            } else if ("/confirm".equals(path)) {
                confirm(req, resp, user);
            } else {
                notFound(resp, "Halaman tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void preview(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException, ServletException, SQLException {
        Batch batch = (Batch) req.getSession().getAttribute(BATCH);
        Part part = null;
        try {
            part = req.getPart("file");
        } catch (IllegalStateException e) {
            flash(req, "error", "File terlalu besar. Maksimal 2 MB.");
            redirect(req, resp, "/app/transactions/import");
            return;
        } catch (ServletException e) {
            // A normal (non-multipart) mapping submit has no part.
        }

        if (part != null && part.getSize() > 0) {
            try {
                byte[] bytes = read(part);
                batch = importService.parse(bytes, intParamOrZero(req, "defaultWalletId"),
                        req.getParameter("createCategories") != null);
                req.getSession().setAttribute(BATCH, batch);
                flash(req, "success", "File dibaca. Periksa mapping dan pratinjau sebelum import.");
            } catch (ImportException e) {
                flash(req, "error", e.getMessage());
                redirect(req, resp, "/app/transactions/import");
                return;
            }
        } else if (batch != null) {
            Mapping mapping = batch.getMapping();
            int limit = batch.getHeaders().size();
            mapping.setDateIndex(mappingIndex(req, "mapDate", limit));
            mapping.setTypeIndex(mappingIndex(req, "mapType", limit));
            mapping.setCategoryIndex(mappingIndex(req, "mapCategory", limit));
            mapping.setWalletIndex(mappingIndex(req, "mapWallet", limit));
            mapping.setAmountIndex(mappingIndex(req, "mapAmount", limit));
            mapping.setNoteIndex(mappingIndex(req, "mapNote", limit));
            batch.setDefaultWalletId(intParamOrZero(req, "defaultWalletId"));
            batch.setCreateCategories(req.getParameter("createCategories") != null);
        }
        if (batch == null) {
            flash(req, "error", "Upload file CSV terlebih dahulu.");
            redirect(req, resp, "/app/transactions/import");
            return;
        }
        redirect(req, resp, "/app/transactions/import/review");
    }

    private void review(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, ServletException, IOException {
        Batch batch = (Batch) req.getSession().getAttribute(BATCH);
        if (batch == null) {
            flash(req, "error", "Sesi import tidak ditemukan. Upload ulang file CSV.");
            redirect(req, resp, "/app/transactions/import");
            return;
        }
        req.setAttribute("batch", batch);
        req.setAttribute("preview", importService.preview(batch, user));
        req.setAttribute("wallets", walletDAO.findAll(user.getId()));
        render(req, resp, "transactions/import/review");
    }

    private void confirm(HttpServletRequest req, HttpServletResponse resp, User user)
            throws SQLException, ServletException, IOException {
        Batch batch = (Batch) req.getSession().getAttribute(BATCH);
        if (batch == null) {
            flash(req, "error", "Sesi import tidak ditemukan. Upload ulang file CSV.");
            redirect(req, resp, "/app/transactions/import");
            return;
        }
        Result result = importService.importRows(batch, user);
        req.getSession().removeAttribute(BATCH);
        auditDAO.log(user.getId(), "IMPORT", "transaction", null,
                "Import CSV: " + result.getImported() + " transaksi, "
                        + result.getDuplicate() + " duplikat, " + result.getInvalid() + " gagal.");
        req.setAttribute("result", result);
        render(req, resp, "transactions/import/result");
    }

    private byte[] read(Part part) throws IOException, ImportException {
        if (part.getSize() > CsvImportService.MAX_BYTES) {
            throw new ImportException("File terlalu besar. Maksimal 2 MB.");
        }
        try (InputStream in = part.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > CsvImportService.MAX_BYTES) {
                    throw new ImportException("File terlalu besar. Maksimal 2 MB.");
                }
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private int mappingIndex(HttpServletRequest req, String name, int limit) {
        Integer value = intParam(req, name);
        if (value == null || value < 0 || value >= limit) {
            return -1;
        }
        return value;
    }
}
