<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="true" %>
<%
    if (session.getAttribute("user") != null) {
        response.sendRedirect(request.getContextPath() + "/app/dashboard");
        return;
    }
    request.getRequestDispatcher("/WEB-INF/views/landing.jsp").forward(request, response);
%>
