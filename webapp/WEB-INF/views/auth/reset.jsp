<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!doctype html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Reset Password &middot; Dompetku</title>
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
            <span class="pill pill-glass"><ui:icon name="lock"/> Password baru</span>
            <h1>Amankan<br><span>akunmu.</span></h1>
            <p>Buat password baru yang kuat dan belum pernah dipakai di layanan lain.</p>
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
                <h2>Buat password baru</h2>
                <p>Password minimal 8 karakter, mengandung huruf, angka, dan simbol.</p>
            </div>
            <c:choose>
                <c:when test="${tokenValid}">
                    <c:if test="${not empty errors.token}">
                        <div class="alert alert-error" role="alert"><ui:icon name="alert"/><span>${f:esc(errors.token)}</span></div>
                    </c:if>
                    <form method="post" action="${ctx}/reset-password" class="auth-form">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="token" value="${f:esc(token)}">
                        <label class="field">
                            <span>Password baru</span>
                            <span class="input-wrap">
                                <ui:icon name="lock"/>
                                <input type="password" name="password" placeholder="Min. 8 karakter" autocomplete="new-password" required autofocus>
                            </span>
                            <c:if test="${not empty errors.password}"><small class="error-text">${f:esc(errors.password)}</small></c:if>
                        </label>
                        <label class="field">
                            <span>Konfirmasi password</span>
                            <span class="input-wrap">
                                <ui:icon name="lock"/>
                                <input type="password" name="confirmPassword" placeholder="Ulangi password baru" autocomplete="new-password" required>
                            </span>
                            <c:if test="${not empty errors.confirmPassword}"><small class="error-text">${f:esc(errors.confirmPassword)}</small></c:if>
                        </label>
                        <button type="submit" class="btn btn-primary btn-block btn-lg"><ui:icon name="check"/>Simpan password</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <div class="alert alert-error" role="alert">
                        <ui:icon name="alert"/>
                        <span>Tautan reset tidak valid atau sudah kedaluwarsa. Minta tautan baru.</span>
                    </div>
                    <a class="btn btn-primary btn-block" href="${ctx}/forgot-password"><ui:icon name="refresh"/>Minta tautan baru</a>
                </c:otherwise>
            </c:choose>
            <p class="auth-switch"><a href="${ctx}/login">Kembali ke halaman masuk</a></p>
        </div>
    </section>
</div>
<%@ include file="/WEB-INF/views/layout/flash.jspf" %>
<script src="${ctx}/assets/js/app.js?v=20261119-29"></script>
</body>
</html>
