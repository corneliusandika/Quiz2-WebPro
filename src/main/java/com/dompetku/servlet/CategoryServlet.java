package com.dompetku.servlet;

import com.dompetku.dao.BudgetDAO;
import com.dompetku.dao.CategoryDAO;
import com.dompetku.model.Category;
import com.dompetku.model.User;
import com.dompetku.web.BaseServlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/app/categories/*")
public class CategoryServlet extends BaseServlet {

    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final BudgetDAO budgetDAO = new BudgetDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = currentUser(req);
        String path = req.getPathInfo();
        try {
            if (path == null || "/".equals(path)) {
                req.setAttribute("items", categoryDAO.findAll(user.getId()));
                render(req, resp, "categories/list");
            } else if ("/new".equals(path)) {
                form(req, resp, user, null);
            } else if (path.startsWith("/edit/")) {
                Integer id = parseInt(path.substring("/edit/".length()));
                Category category = id == null ? null : categoryDAO.findById(id, user.getId());
                if (category == null) {
                    notFound(resp, "Kategori tidak ditemukan.");
                    return;
                }
                form(req, resp, user, category);
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
            if ("/create".equals(path)) {
                save(req, resp, user, false);
            } else if ("/update".equals(path)) {
                save(req, resp, user, true);
            } else if ("/delete".equals(path)) {
                int id = intParamOrZero(req, "id");
                if (categoryDAO.findById(id, user.getId()) == null) {
                    notFound(resp, "Kategori tidak ditemukan.");
                    return;
                }
                if (budgetDAO.existsForCategory(user.getId(), id)) {
                    flash(req, "error", "Kategori tidak bisa dihapus karena masih memiliki budget.");
                    redirect(req, resp, "/app/categories");
                    return;
                }
                categoryDAO.delete(id, user.getId());
                flash(req, "success", "Kategori dihapus. Transaksi terkait kini menjadi tanpa kategori.");
                redirect(req, resp, "/app/categories");
            } else {
                notFound(resp, "Halaman tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void form(HttpServletRequest req, HttpServletResponse resp, User user, Category category)
            throws ServletException, IOException {
        Category item = category;
        if (item == null) {
            item = new Category();
            item.setUserId(user.getId());
            item.setType("expense");
            item.setColor("#f97316");
            item.setIcon("tag");
        }
        req.setAttribute("category", item);
        req.setAttribute("mode", category == null ? "create" : "edit");
        render(req, resp, "categories/form");
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, User user, boolean update)
            throws SQLException, ServletException, IOException {
        Map<String, String> errors = new HashMap<>();
        Category category = readForm(req, user, errors);
        if (update) {
            if (category.getId() <= 0 || categoryDAO.findById(category.getId(), user.getId()) == null) {
                notFound(resp, "Kategori tidak ditemukan.");
                return;
            }
        }
        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("category", category);
            req.setAttribute("mode", update ? "edit" : "create");
            render(req, resp, "categories/form");
            return;
        }
        if (update) {
            categoryDAO.update(category);
            flash(req, "success", "Kategori berhasil diperbarui.");
        } else {
            categoryDAO.create(category);
            flash(req, "success", "Kategori baru berhasil dibuat.");
        }
        redirect(req, resp, "/app/categories");
    }

    private Category readForm(HttpServletRequest req, User user, Map<String, String> errors) throws SQLException {
        Category category = new Category();
        category.setUserId(user.getId());
        category.setId(intParamOrZero(req, "id"));

        String name = safe(param(req, "name"), 60);
        if (name == null || name.length() < 2) {
            errors.put("name", "Nama kategori minimal 2 karakter.");
        }
        category.setName(name);

        String type = param(req, "type");
        if (!"income".equals(type) && !"expense".equals(type)) {
            errors.put("type", "Pilih jenis kategori.");
            type = "expense";
        }
        category.setType(type);

        String color = param(req, "color");
        if (color == null || !color.matches("^#[0-9a-fA-F]{6}$")) {
            color = "expense".equals(type) ? "#f97316" : "#10b981";
        }
        category.setColor(color);

        String icon = param(req, "icon");
        if (icon == null || !icon.matches("^[a-z0-9-]{1,40}$")) {
            icon = "tag";
        }
        category.setIcon(icon);

        if (name != null && categoryDAO.exists(user.getId(), name, type,
                category.getId() == 0 ? null : category.getId())) {
            errors.put("name", "Kategori dengan nama dan jenis ini sudah ada.");
        }
        return category;
    }

    private Integer parseInt(String value) {
        try {
            return Integer.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }
}
