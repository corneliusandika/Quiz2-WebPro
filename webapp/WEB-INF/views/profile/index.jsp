<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Profil Saya"/>
<c:set var="eyebrow" value="Akun &amp; keamanan"/>
<c:set var="active" value="profile"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <h2>Profil &amp; pengaturan</h2>
        <p>Kelola identitas akun dan keamanan passwordmu.</p>
    </div>
</section>

<section class="profile-hero">
    <span class="avatar avatar-xl">${f:initials(profile.name)}</span>
    <div class="profile-hero-main">
        <h2>${f:esc(profile.name)}</h2>
        <p>${f:esc(profile.email)}</p>
        <span class="pill pill-soft"><ui:icon name="shield"/>Personal account</span>
    </div>
    <div class="profile-hero-stats">
        <div><strong>${transactionCount}</strong><small>transaksi</small></div>
        <div><strong>${walletCount}</strong><small>dompet</small></div>
        <div><strong>${categoryCount}</strong><small>kategori</small></div>
    </div>
</section>

<div class="profile-grid">
    <article class="card form-card">
        <div class="card-head">
            <div>
                <h2>Informasi akun</h2>
                <p>Perbarui nama dan email yang dipakai untuk masuk.</p>
            </div>
        </div>
        <form method="post" action="${ctx}/app/profile/update">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <label class="field">
                <span>Nama lengkap</span>
                <input type="text" name="name" maxlength="100" value="${f:esc(profile.name)}" required>
                <c:if test="${not empty errors.name}"><small class="error-text">${f:esc(errors.name)}</small></c:if>
            </label>
            <label class="field">
                <span>Email</span>
                <input type="email" name="email" value="${f:esc(profile.email)}" maxlength="150" required>
                <c:if test="${not empty errors.email}"><small class="error-text">${f:esc(errors.email)}</small></c:if>
            </label>
            <div class="form-actions">
                <button class="btn btn-primary" type="submit"><ui:icon name="check"/>Simpan profil</button>
            </div>
        </form>
    </article>

    <aside class="profile-side">
        <div class="summary-card">
            <p class="eyebrow">Status akun</p>
            <div class="profile-meta">
                <div><span><ui:icon name="calendar"/>Bergabung</span><strong>${empty profile.createdAt ? '-' : f:date(profile.createdAt.toLocalDate())}</strong></div>
                <div><span><ui:icon name="shield"/>Keamanan</span><strong>Password di-hash</strong></div>
                <div><span><ui:icon name="lock"/>Session</span><strong>8 jam</strong></div>
            </div>
        </div>
        <div class="help-card">
            <span class="mini-icon"><ui:icon name="sparkles"/></span>
            <div><strong>Data aman</strong><p>Data keuanganmu hanya bisa diakses oleh akunmu.</p></div>
        </div>
    </aside>
</div>

<article class="card form-card security-card">
    <div class="card-head">
        <div>
            <h2>Ganti password</h2>
            <p>Gunakan password kuat yang belum pernah dipakai di layanan lain.</p>
        </div>
        <span class="soft-badge"><ui:icon name="lock"/>Keamanan</span>
    </div>
    <form method="post" action="${ctx}/app/profile/password">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <div class="field-grid three">
            <label class="field">
                <span>Password saat ini</span>
                <span class="input-wrap">
                    <ui:icon name="lock"/>
                    <input type="password" name="currentPassword" id="currentPassword" placeholder="Password lama" required>
                    <button type="button" class="input-action" data-password-toggle="#currentPassword" aria-label="Tampilkan">
                        <ui:icon name="eye"/>
                    </button>
                </span>
                <c:if test="${not empty passwordErrors.currentPassword}"><small class="error-text">${passwordErrors.currentPassword}</small></c:if>
            </label>
            <label class="field">
                <span>Password baru</span>
                <span class="input-wrap">
                    <ui:icon name="shield"/>
                    <input type="password" name="newPassword" id="newPassword" placeholder="Min. 8 karakter, huruf, angka, simbol" required>
                    <button type="button" class="input-action" data-password-toggle="#newPassword" aria-label="Tampilkan">
                        <ui:icon name="eye"/>
                    </button>
                </span>
                <c:if test="${not empty passwordErrors.newPassword}"><small class="error-text">${passwordErrors.newPassword}</small></c:if>
            </label>
            <label class="field">
                <span>Konfirmasi</span>
                <span class="input-wrap">
                    <ui:icon name="shield"/>
                    <input type="password" name="confirmPassword" id="confirmPassword" placeholder="Ulangi password baru" required>
                    <button type="button" class="input-action" data-password-toggle="#confirmPassword" aria-label="Tampilkan">
                        <ui:icon name="eye"/>
                    </button>
                </span>
                <c:if test="${not empty passwordErrors.confirmPassword}"><small class="error-text">${passwordErrors.confirmPassword}</small></c:if>
            </label>
        </div>
        <div class="form-actions">
            <button class="btn btn-primary" type="submit"><ui:icon name="shield"/>Perbarui password</button>
        </div>
    </form>
</article>

<article class="card activity-card">
    <div class="card-head">
        <div>
            <h2>Riwayat aktivitas</h2>
            <p>Jejak perubahan transaksi terakhir di akunmu.</p>
        </div>
        <span class="soft-badge"><ui:icon name="clock"/>8 terakhir</span>
    </div>
    <c:choose>
        <c:when test="${empty auditLogs}">
            <div class="empty-inline"><ui:icon name="clock"/>Belum ada aktivitas transaksi.</div>
        </c:when>
        <c:otherwise>
            <div class="activity-list">
                <c:forEach var="log" items="${auditLogs}">
                    <div class="activity-row">
                        <span class="activity-dot ${log.action eq 'DELETE' ? 'danger' : (log.action eq 'RESTORE' ? 'good' : 'neutral')}"></span>
                        <div class="activity-copy">
                            <strong>${f:esc(log.actionLabel)}</strong>
                            <p>${f:esc(log.details)}</p>
                        </div>
                        <span class="activity-date">${empty log.createdAt ? '-' : f:date(log.createdAt.toLocalDate())}</span>
                    </div>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</article>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
