<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!doctype html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="color-scheme" content="light dark">
    <meta name="text-scale" content="scale">
    <title>Masuk &middot; Dompetku</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=DM+Serif+Display&display=swap" rel="stylesheet">
    <link rel="icon" type="image/svg+xml" href="${ctx}/assets/img/favicon.svg">
    <link rel="stylesheet" href="${ctx}/assets/css/app.css?v=20261119-31">
</head>
<body class="auth-body">
<div class="auth-shell">
    <section class="auth-brand-panel auth-brand-login">
        <span class="auth-orb auth-orb-1"></span>
        <span class="auth-orb auth-orb-2"></span>
        <span class="auth-orb auth-orb-3"></span>

        <a class="brand brand-light" href="${ctx}/login">
            <span class="brand-mark">
                <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                    <path d="M3 8.5A3.5 3.5 0 0 1 6.5 5H18a2 2 0 0 1 2 2v1.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>
                    <rect x="3" y="7.5" width="18" height="12.5" rx="3.2" stroke="currentColor" stroke-width="1.7"/>
                    <path d="M15.5 14.2h2.2" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/>
                    <path d="M7 7.5V6.2A2.2 2.2 0 0 1 9.2 4l6.6 1.3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
            </span>
            <span class="brand-text"><strong>Dompetku</strong><small>finance workspace</small></span>
        </a>

        <div class="auth-pitch">
            <h1>Uang rapi,<br><span>hidup lebih tenang.</span></h1>
            <p>Satu workspace untuk mencatat transaksi, menjaga budget, dan mewujudkan target tabunganmu.</p>
            <ul class="auth-points">
                <li><span><ui:icon name="check"/></span>Catat pemasukan &amp; pengeluaran dalam hitungan detik</li>
                <li><span><ui:icon name="check"/></span>Pantau budget dan arus kas lewat visual yang jernih</li>
                <li><span><ui:icon name="check"/></span>Raih target tabungan dengan progres yang terukur</li>
            </ul>
        </div>
    </section>

    <section class="auth-form-panel">
        <div class="auth-form-wrap">
            <a class="brand auth-mobile-brand" href="${ctx}/login">
                <span class="brand-mark">
                    <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                        <path d="M3 8.5A3.5 3.5 0 0 1 6.5 5H18a2 2 0 0 1 2 2v1.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>
                        <rect x="3" y="7.5" width="18" height="12.5" rx="3.2" stroke="currentColor" stroke-width="1.7"/>
                        <path d="M15.5 14.2h2.2" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/>
                        <path d="M7 7.5V6.2A2.2 2.2 0 0 1 9.2 4l6.6 1.3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                    </svg>
                </span>
                <span class="brand-text"><strong>Dompetku</strong><small>finance workspace</small></span>
            </a>
            <h1 class="auth-mobile-title">Masuk ke Dompetku</h1>

            <div class="auth-form-head">
                <p class="eyebrow">Selamat datang kembali</p>
                <h2>Masuk ke Dompetku</h2>
                <p>Lanjutkan perjalanan finansialmu dari tempat terakhir.</p>
            </div>

            <c:if test="${not empty error}">
                <div class="alert alert-error" role="alert" tabindex="-1" id="formError">
                    <ui:icon name="alert"/>
                    <span>${f:esc(error)}</span>
                </div>
            </c:if>

            <form method="post" action="${ctx}/login" class="auth-form">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <label class="field">
                    <span>Email</span>
                    <span class="input-wrap">
                        <ui:icon name="mail"/>
                        <input type="email" name="email" value="${f:esc(email)}" placeholder="nama@email.com"
                               maxlength="150" autocomplete="email" required autofocus>
                    </span>
                </label>
                <label class="field">
                    <span>Password</span>
                    <span class="input-wrap">
                        <ui:icon name="lock"/>
                        <input type="password" name="password" id="loginPassword" placeholder="Masukkan password"
                               autocomplete="current-password" required>
                        <button type="button" class="input-action" data-password-toggle="#loginPassword"
                                aria-label="Tampilkan password" aria-pressed="false"><ui:icon name="eye"/></button>
                    </span>
                </label>
                <div class="auth-row">
                    <span class="auth-secure"><ui:icon name="shield"/> Sesi terenkripsi &middot; aman 8 jam</span>
                    <a class="auth-forgot" href="${ctx}/forgot-password">Lupa password?</a>
                </div>
                <button type="submit" class="btn btn-primary btn-block btn-lg">
                    Masuk ke workspace <ui:icon name="arrow-right"/>
                </button>
            </form>

            <p class="auth-switch">Belum punya akun? <a href="${ctx}/register">Daftar gratis</a></p>
        </div>
    </section>
</div>
<%@ include file="/WEB-INF/views/layout/flash.jspf" %>
<script src="${ctx}/assets/js/app.js?v=20261119-29"></script>
</body>
</html>
