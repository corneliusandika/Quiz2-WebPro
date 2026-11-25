<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!doctype html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Verifikasi Email &middot; Dompetku</title>
    <link rel="icon" type="image/svg+xml" href="${ctx}/assets/img/favicon.svg">
    <link rel="stylesheet" href="${ctx}/assets/css/app.css?v=20261119-31">
</head>
<body class="auth-body">
<div class="auth-shell">
    <section class="auth-brand-panel auth-brand-login">
        <a class="brand brand-light" href="${ctx}/login">
            <span class="brand-mark"><ui:icon name="wallet"/></span>
            <span class="brand-text"><strong>Dompetku</strong><small>finance workspace</small></span>
        </a>
        <div class="auth-pitch">
            <span class="pill pill-glass"><ui:icon name="mail"/> Satu langkah lagi</span>
            <h1>Verifikasi<br><span>emailmu.</span></h1>
            <p>Kami mengirim tautan verifikasi agar akun dan datamu tetap aman.</p>
        </div>
    </section>
    <section class="auth-form-panel">
        <div class="auth-form-wrap">
            <a class="brand auth-mobile-brand" href="${ctx}/login">
                <span class="brand-mark"><ui:icon name="wallet"/></span>
                <span class="brand-text"><strong>Dompetku</strong><small>finance workspace</small></span>
            </a>
            <div class="auth-form-head">
                <p class="eyebrow">Verifikasi akun</p>
                <h2>Periksa emailmu</h2>
                <p>Kami mengirim tautan verifikasi ke <strong>${f:esc(email)}</strong>.</p>
            </div>
            <c:if test="${not empty mailError}">
                <div class="alert alert-error" role="alert"><ui:icon name="alert"/><span>${f:esc(mailError)}</span></div>
            </c:if>
            <div class="alert alert-success" role="status">
                <ui:icon name="check"/>
                <span>Setelah email diverifikasi, kamu bisa masuk ke Dompetku.</span>
            </div>
            <a class="btn btn-primary btn-block" href="${ctx}/login"><ui:icon name="arrow-right"/>Ke halaman masuk</a>
        </div>
    </section>
</div>
<%@ include file="/WEB-INF/views/layout/flash.jspf" %>
<script src="${ctx}/assets/js/app.js?v=20261119-29"></script>
</body>
</html>
