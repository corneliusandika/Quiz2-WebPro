<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="${mode eq 'edit' ? 'Edit Jadwal' : 'Jadwal Baru'}"/>
<c:set var="eyebrow" value="Atur transaksi rutin"/>
<c:set var="active" value="recurring"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <a class="back-link" href="${ctx}/app/recurring"><ui:icon name="arrow-left"/>Kembali ke jadwal</a>
        <h2>${mode eq 'edit' ? 'Perbarui jadwal berulang' : 'Buat jadwal berulang'}</h2>
        <p>Dompetku akan mengingatkan jadwal dan mencatat transaksi saat dijalankan.</p>
    </div>
</section>

<div class="form-layout">
    <article class="card form-card">
        <form method="post" action="${ctx}/app/recurring/${mode eq 'edit' ? 'update' : 'create'}" id="recurringForm">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="id" value="${item.id}">

            <label class="field">
                <span>Nama jadwal</span>
                <input type="text" name="name" id="recurringName" maxlength="100" value="${f:esc(item.name)}"
                       placeholder="Contoh: Kos Bulanan, Spotify, Gaji" required>
                <c:if test="${not empty errors.name}"><small class="error-text">${f:esc(errors.name)}</small></c:if>
            </label>

            <div class="field">
                <span>Jenis transaksi</span>
                <div class="segmented">
                    <label class="segment segment-expense ${item.type eq 'expense' ? 'is-active' : ''}">
                        <input type="radio" name="type" value="expense" ${item.type eq 'expense' ? 'checked' : ''}>
                        <span><ui:icon name="arrow-up"/>Pengeluaran</span>
                    </label>
                    <label class="segment segment-income ${item.type eq 'income' ? 'is-active' : ''}">
                        <input type="radio" name="type" value="income" ${item.type eq 'income' ? 'checked' : ''}>
                        <span><ui:icon name="arrow-down"/>Pemasukan</span>
                    </label>
                </div>
                <c:if test="${not empty errors.type}"><small class="error-text">${f:esc(errors.type)}</small></c:if>
            </div>

            <div class="field-grid">
                <label class="field">
                    <span>Dompet</span>
                    <select name="walletId" required>
                        <option value="">Pilih dompet</option>
                        <c:forEach var="w" items="${wallets}">
                            <option value="${w.id}" ${item.walletId eq w.id ? 'selected' : ''}>${f:esc(w.name)}</option>
                        </c:forEach>
                    </select>
                    <c:if test="${not empty errors.walletId}"><small class="error-text">${f:esc(errors.walletId)}</small></c:if>
                </label>
                <label class="field">
                    <span>Kategori</span>
                    <select name="categoryId" id="recurringCategory" required>
                        <option value="">Pilih kategori</option>
                        <c:forEach var="cat" items="${categories}">
                            <option value="${cat.id}" data-type="${cat.type}" ${item.categoryId eq cat.id ? 'selected' : ''}>${f:esc(cat.name)}</option>
                        </c:forEach>
                    </select>
                    <c:if test="${not empty errors.categoryId}"><small class="error-text">${f:esc(errors.categoryId)}</small></c:if>
                </label>
            </div>

            <div class="field-grid">
                <label class="field">
                    <span>Nominal</span>
                    <span class="input-wrap input-money">
                        <span class="input-prefix">Rp</span>
                        <input type="text" name="amount" inputmode="decimal" data-money-input
                               value="${f:moneyInput(item.amount)}" placeholder="0" autocomplete="off" required>
                    </span>
                    <c:if test="${not empty errors.amount}"><small class="error-text">${f:esc(errors.amount)}</small></c:if>
                </label>
                <label class="field">
                    <span>Frekuensi</span>
                    <select name="frequency">
                        <option value="daily" ${item.frequency eq 'daily' ? 'selected' : ''}>Harian</option>
                        <option value="weekly" ${item.frequency eq 'weekly' ? 'selected' : ''}>Mingguan</option>
                        <option value="monthly" ${item.frequency eq 'monthly' ? 'selected' : ''}>Bulanan</option>
                    </select>
                    <c:if test="${not empty errors.frequency}"><small class="error-text">${f:esc(errors.frequency)}</small></c:if>
                </label>
            </div>

            <div class="field-grid">
                <label class="field">
                    <span>Jadwal berikutnya</span>
                    <input type="date" name="nextDate" value="${item.nextDate}" required>
                    <c:if test="${not empty errors.nextDate}"><small class="error-text">${f:esc(errors.nextDate)}</small></c:if>
                </label>
                <div class="field toggle-field">
                    <span>Status</span>
                    <span class="toggle-row">
                        <input type="checkbox" name="active" id="recurringActive" ${item.active ? 'checked' : ''}>
                        <label for="recurringActive">Aktifkan jadwal ini</label>
                    </span>
                    <small class="hint">Jadwal jeda tidak bisa dijalankan.</small>
                </div>
            </div>

            <div class="form-actions">
                <a class="btn btn-ghost" href="${ctx}/app/recurring">Batal</a>
                <button class="btn btn-primary" type="submit">
                    <ui:icon name="check"/>${mode eq 'edit' ? 'Simpan perubahan' : 'Simpan jadwal'}
                </button>
            </div>
        </form>
    </article>

    <aside class="form-aside">
        <div class="summary-card">
            <p class="eyebrow">Pratinjau</p>
            <span class="category-icon large" style="background:#0ea5e91f;color:#0ea5e9"><ui:icon name="recurring"/></span>
            <strong class="preview-category-name" id="recurringPreviewName">Nama jadwal</strong>
            <strong class="summary-amount" id="recurringPreviewAmount">Rp 0</strong>
            <p class="muted" id="recurringPreviewSchedule">Bulanan &middot; jadwal berikutnya</p>
        </div>
        <div class="help-card">
            <span class="mini-icon"><ui:icon name="sparkles"/></span>
            <div><strong>Otomatis vs manual</strong><p>Klik &ldquo;Jalankan&rdquo; untuk mencatat transaksi dan memajukan jadwal. Tidak ada transaksi ganda.</p></div>
        </div>
    </aside>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
