<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="${mode eq 'edit' ? 'Edit Transaksi' : 'Transaksi Baru'}"/>
<c:set var="eyebrow" value="${mode eq 'edit' ? 'Perbarui catatan' : 'Catat arus kas'}"/>
<c:set var="active" value="transactions"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <a class="back-link" href="${ctx}/app/transactions"><ui:icon name="arrow-left"/>Kembali ke transaksi</a>
        <h2>${mode eq 'edit' ? 'Perbarui transaksi' : 'Catat transaksi baru'}</h2>
        <p>Isi detail transaksi. Saldo dompet akan dihitung otomatis.</p>
    </div>
</section>

<div class="form-layout">
    <article class="card form-card">
        <form method="post" action="${ctx}/app/transactions/${mode eq 'edit' ? 'update' : 'create'}" id="transactionForm">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="id" value="${tx.id}">

            <div class="field">
                <span>Jenis transaksi</span>
                <div class="segmented">
                    <label class="segment segment-expense ${tx.type eq 'expense' ? 'is-active' : ''}">
                        <input type="radio" name="type" value="expense" ${tx.type eq 'expense' ? 'checked' : ''}>
                        <span><ui:icon name="arrow-up"/>Pengeluaran</span>
                    </label>
                    <label class="segment segment-income ${tx.type eq 'income' ? 'is-active' : ''}">
                        <input type="radio" name="type" value="income" ${tx.type eq 'income' ? 'checked' : ''}>
                        <span><ui:icon name="arrow-down"/>Pemasukan</span>
                    </label>
                </div>
                <c:if test="${not empty errors.type}"><small class="error-text">${f:esc(errors.type)}</small></c:if>
            </div>

            <label class="field">
                <span>Nominal</span>
                <span class="input-wrap input-money">
                    <span class="input-prefix">Rp</span>
                    <input type="text" name="amount" id="txAmount" inputmode="decimal" data-money-input
                           value="${f:moneyInput(tx.amount)}" placeholder="0" autocomplete="off" required>
                </span>
                <c:if test="${not empty errors.amount}"><small class="error-text">${f:esc(errors.amount)}</small></c:if>
                <small class="hint">Pemisah ribuan otomatis. Gunakan koma untuk desimal, misalnya 1500,50.</small>
            </label>

            <div class="field-grid">
                <label class="field">
                    <span>Dompet</span>
                    <select name="walletId" id="txWallet" required>
                        <option value="">Pilih dompet</option>
                        <c:forEach var="w" items="${wallets}">
                            <option value="${w.id}" ${tx.walletId eq w.id ? 'selected' : ''}>${f:esc(w.name)}</option>
                        </c:forEach>
                    </select>
                    <c:if test="${not empty errors.walletId}"><small class="error-text">${f:esc(errors.walletId)}</small></c:if>
                </label>
                <label class="field">
                    <span>Kategori</span>
                    <select name="categoryId" id="txCategory" ${mode eq 'create' or not empty tx.categoryId ? 'required' : ''}>
                        <option value="" ${empty tx.categoryId ? 'selected' : ''} ${mode eq 'create' ? 'disabled' : ''}>${mode eq 'create' ? 'Pilih kategori' : 'Tanpa kategori'}</option>
                        <c:forEach var="cat" items="${categories}">
                            <option value="${cat.id}" data-type="${cat.type}" ${tx.categoryId eq cat.id ? 'selected' : ''}>${f:esc(cat.name)}</option>
                        </c:forEach>
                    </select>
                    <c:if test="${not empty errors.categoryId}"><small class="error-text">${f:esc(errors.categoryId)}</small></c:if>
                </label>
            </div>

            <div class="field-grid">
                <label class="field">
                    <span>Tanggal</span>
                    <input type="date" name="date" id="txDate" value="${tx.transactionDate}" required>
                    <c:if test="${not empty errors.date}"><small class="error-text">${f:esc(errors.date)}</small></c:if>
                </label>
                <label class="field">
                    <span>Catatan <small class="muted">opsional</small></span>
                    <input type="text" name="note" id="txNote" maxlength="255" value="${f:esc(tx.note)}"
                           placeholder="Contoh: Makan siang di warteg">
                </label>
            </div>

            <div class="form-actions">
                <a class="btn btn-ghost" href="${ctx}/app/transactions">Batal</a>
                <button class="btn btn-primary" type="submit">
                    <ui:icon name="check"/>${mode eq 'edit' ? 'Simpan perubahan' : 'Simpan transaksi'}
                </button>
            </div>
        </form>
    </article>

    <aside class="form-aside">
        <div class="summary-card">
            <p class="eyebrow">Pratinjau</p>
            <strong class="summary-amount" data-preview-amount>Rp 0</strong>
            <span class="badge badge-preview" data-preview-type>Pengeluaran</span>
            <dl class="summary-list">
                <div><dt>Dompet</dt><dd data-preview-wallet>-</dd></div>
                <div><dt>Kategori</dt><dd data-preview-category>-</dd></div>
                <div><dt>Tanggal</dt><dd data-preview-date>-</dd></div>
            </dl>
            <div class="summary-tip">
                <ui:icon name="sparkles"/>
                <p>Konsisten mencatat transaksi kecil membuat laporan bulanan jauh lebih akurat.</p>
            </div>
        </div>
        <div class="help-card">
            <span class="mini-icon"><ui:icon name="shield"/></span>
            <div><strong>Data aman</strong><p>Setiap transaksi terikat ke akunmu dan tidak bisa diakses pengguna lain.</p></div>
        </div>
    </aside>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
