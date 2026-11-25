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
    <title>Daftar &middot; Dompetku</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=DM+Serif+Display&display=swap" rel="stylesheet">
    <link rel="icon" type="image/svg+xml" href="${ctx}/assets/img/favicon.svg">
    <link rel="stylesheet" href="${ctx}/assets/css/app.css?v=20261119-31">
</head>
<body class="auth-body">
<div class="auth-shell">
    <section class="auth-brand-panel auth-brand-register">
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
            <h1 class="auth-mobile-title">Daftar ke Dompetku</h1>

            <div class="auth-form-head">
                <p class="eyebrow">Buat akun baru</p>
                <h2>Mulai dengan Dompetku</h2>
                <p>Isi data di bawah, kurang dari satu menit.</p>
            </div>

            <c:if test="${not empty errors}">
                <div class="alert alert-error alert-summary" role="alert" tabindex="-1" id="formError">
                    <ui:icon name="alert"/>
                    <div>
                        <strong>Periksa kembali data yang Anda isi.</strong>
                        <ul class="error-summary-list">
                            <c:if test="${not empty errors.name}"><li><a href="#nameInput">${f:esc(errors.name)}</a></li></c:if>
                            <c:if test="${not empty errors.email}"><li><a href="#emailInput">${f:esc(errors.email)}</a></li></c:if>
                            <c:if test="${not empty errors.password}"><li><a href="#registerPassword">${f:esc(errors.password)}</a></li></c:if>
                            <c:if test="${not empty errors.confirmPassword}"><li><a href="#registerConfirm">${f:esc(errors.confirmPassword)}</a></li></c:if>
                            <c:if test="${not empty errors.agree}"><li><a href="#agreeInput">${f:esc(errors.agree)}</a></li></c:if>
                        </ul>
                    </div>
                </div>
            </c:if>

            <form method="post" action="${ctx}/register" class="auth-form">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <label class="field">
                    <span>Nama lengkap</span>
                    <span class="input-wrap">
                        <ui:icon name="user"/>
                        <input type="text" name="name" id="nameInput" value="${f:esc(name)}" placeholder="Nama lengkap"
                               maxlength="100" autocomplete="name" aria-describedby="nameError" aria-invalid="${not empty errors.name}" required autofocus>
                    </span>
                    <c:if test="${not empty errors.name}"><small class="error-text" id="nameError">${f:esc(errors.name)}</small></c:if>
                </label>
                <label class="field">
                    <span>Email</span>
                    <span class="input-wrap">
                        <ui:icon name="mail"/>
                        <input type="email" name="email" id="emailInput" value="${f:esc(email)}" placeholder="nama@email.com"
                               maxlength="150" autocomplete="email" aria-describedby="emailError" aria-invalid="${not empty errors.email}" required>
                    </span>
                    <c:if test="${not empty errors.email}"><small class="error-text" id="emailError">${f:esc(errors.email)}</small></c:if>
                </label>
                <div class="field-grid">
                    <label class="field">
                        <span>Password</span>
                        <span class="input-wrap">
                            <ui:icon name="lock"/>
                            <input type="password" name="password" id="registerPassword" value="${f:esc(password)}"
                                   placeholder="8+ karakter" autocomplete="new-password" spellcheck="false"
                                   autocapitalize="off" aria-describedby="passwordRules passwordError" aria-invalid="${not empty errors.password}" required>
                            <button type="button" class="input-action" data-password-toggle="#registerPassword"
                                    aria-label="Tampilkan password" aria-pressed="false"><ui:icon name="eye"/></button>
                        </span>
                        <c:if test="${not empty errors.password}"><small class="error-text" id="passwordError">${f:esc(errors.password)}</small></c:if>
                    </label>
                    <label class="field">
                        <span>Konfirmasi</span>
                        <span class="input-wrap">
                            <ui:icon name="shield"/>
                            <input type="password" name="confirmPassword" id="registerConfirm" value="${f:esc(confirmPassword)}"
                                   placeholder="Ulangi password" autocomplete="new-password" spellcheck="false"
                                   autocapitalize="off" aria-describedby="passwordMatch confirmError" aria-invalid="${not empty errors.confirmPassword}" required>
                        </span>
                        <c:if test="${not empty errors.confirmPassword}"><small class="error-text" id="confirmError">${f:esc(errors.confirmPassword)}</small></c:if>
                    </label>
                </div>
                <div class="password-meter" id="passwordMeter" data-password-meter
                     data-password="#registerPassword" data-confirm="#registerConfirm" hidden>
                    <ul class="password-rules" id="passwordRules">
                        <li data-rule="length"><span class="rule-dot"></span>Minimal 8 karakter</li>
                        <li data-rule="letter"><span class="rule-dot"></span>Mengandung huruf</li>
                        <li data-rule="digit"><span class="rule-dot"></span>Mengandung angka</li>
                        <li data-rule="symbol"><span class="rule-dot"></span>Mengandung simbol</li>
                    </ul>
                    <p class="password-confirm" id="passwordMatch" data-confirm-hint aria-live="polite" hidden></p>
                </div>
                <label class="checkbox" for="agreeInput">
                    <input type="checkbox" id="agreeInput" name="agree" value="1" required
                           ${not empty agree ? 'checked' : ''} aria-describedby="agreeError"
                           aria-invalid="${not empty errors.agree}">
                    Saya setuju dengan ketentuan penggunaan Dompetku
                </label>
                <c:if test="${not empty errors.agree}"><small class="error-text" id="agreeError">${f:esc(errors.agree)}</small></c:if>
                <button type="submit" class="btn btn-primary btn-block btn-lg">
                    Buat akun saya <ui:icon name="arrow-right"/>
                </button>
            </form>

            <p class="auth-switch">Sudah punya akun? <a href="${ctx}/login">Masuk di sini</a></p>
        </div>
    </section>
</div>
<%@ include file="/WEB-INF/views/layout/flash.jspf" %>
<script src="${ctx}/assets/js/app.js?v=20261119-29"></script>
</body>
</html>
