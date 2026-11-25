<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!doctype html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Lupa Password &middot; Dompetku</title>
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
            <span class="pill pill-glass"><ui:icon name="shield"/> Akun aman</span>
            <h1>Kembali ke<br><span>workspace-mu.</span></h1>
            <p>Masukkan email akun. Kami akan mengirim tautan untuk membuat password baru.</p>
        </div>
    </section>
    <section class="auth-form-panel">
        <div class="auth-form-wrap">
            <a class="brand auth-mobile-brand" href="${ctx}/login">
                <span class="brand-mark"><ui:icon name="wallet"/></span>
                <span class="brand-text"><strong>Dompetku</strong><small>finance workspace</small></span>
            </a>
            <div class="auth-form-head">
                <p class="eyebrow">Pemulihan akun</p>
                <h2>Lupa password?</h2>
                <p>Masukkan email yang terdaftar.</p>
            </div>
            <c:if test="${sent}">
                <div class="alert alert-success" role="status">
                    <ui:icon name="check"/>
                    <span>Jika email terdaftar, tautan reset sudah dikirim. Periksa kotak masuk dan folder spam.</span>
                </div>
            </c:if>
            <c:if test="${not empty error}">
                <div class="alert alert-error" role="alert">
                    <ui:icon name="alert"/><span>${f:esc(error)}</span>
                </div>
            </c:if>
            <form method="post" action="${ctx}/forgot-password" class="auth-form">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <label class="field">
                    <span>Email</span>
                    <span class="input-wrap">
                        <ui:icon name="mail"/>
                        <input type="email" name="email" placeholder="nama@email.com" maxlength="150"
                               autocomplete="email" required autofocus>
                    </span>
                </label>
                <button type="submit" class="btn btn-primary btn-block btn-lg">
                    Kirim tautan reset <ui:icon name="arrow-right"/>
                </button>
            </form>
            <p class="auth-switch"><a href="${ctx}/login">Kembali ke halaman masuk</a></p>
        </div>
    </section>
</div>
<%@ include file="/WEB-INF/views/layout/flash.jspf" %>
<script src="${ctx}/assets/js/app.js?v=20261119-29"></script>
</body>
</html>
