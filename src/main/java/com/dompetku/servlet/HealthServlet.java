package com.dompetku.servlet;

import com.dompetku.db.DB;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

/** Lightweight liveness/readiness endpoint for load balancers and uptime checks. */
@WebServlet("/health")
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        boolean databaseOk;
        try (Connection con = DB.getConnection();
             Statement st = con.createStatement()) {
            st.execute("SELECT 1");
            databaseOk = true;
        } catch (Exception e) {
            databaseOk = false;
        }
        resp.setStatus(databaseOk ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        resp.getWriter().write(databaseOk
                ? "{\"status\":\"ok\",\"database\":\"ok\"}"
                : "{\"status\":\"degraded\",\"database\":\"down\"}");
    }
}
