<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="${mode eq 'edit' ? 'Edit Dompet' : 'Dompet Baru'}"/>
<c:set var="eyebrow" value="Atur sumber dana"/>
<c:set var="active" value="wallets"/>
<c:set var="palette" value="#f59e0b,#2563eb,#10b981,#8b5cf6,#ec4899,#0ea5e9,#ef4444,#14b8a6"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <a class="back-link" href="${ctx}/app/wallets"><ui:icon name="arrow-left"/>Kembali ke dompet</a>
        <h2>${mode eq 'edit' ? 'Perbarui dompet' : 'Tambah dompet baru'}</h2>
        <p>Saldo awal membantu Dompetku menghitung saldo berjalan secara akurat.</p>
    </div>
</section>

<div class="form-layout">
    <article class="card form-card">
        <form method="post" action="${ctx}/app/wallets/${mode eq 'edit' ? 'update' : 'create'}" id="walletForm">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="id" value="${wallet.id}">

            <label class="field">
                <span>Nama dompet</span>
                <input type="text" name="name" id="walletName" maxlength="80" value="${f:esc(wallet.name)}"
                       placeholder="Contoh: BCA, GoPay, Dompet Tunai" required>
                <c:if test="${not empty errors.name}"><small class="error-text">${f:esc(errors.name)}</small></c:if>
            </label>

            <div class="field">
                <span>Tipe dompet</span>
                <div class="type-options">
                    <label class="type-option ${wallet.type eq 'cash' ? 'is-active' : ''}">
                        <input type="radio" name="type" value="cash" ${wallet.type eq 'cash' ? 'checked' : ''}>
                        <span class="type-option-icon"><ui:icon name="cash"/></span>
                        <span><strong>Tunai</strong><small>Uang fisik</small></span>
                    </label>
                    <label class="type-option ${wallet.type eq 'bank' ? 'is-active' : ''}">
                        <input type="radio" name="type" value="bank" ${wallet.type eq 'bank' ? 'checked' : ''}>
                        <span class="type-option-icon"><ui:icon name="bank"/></span>
                        <span><strong>Bank</strong><small>Rekening &amp; tabungan</small></span>
                    </label>
                    <label class="type-option ${wallet.type eq 'ewallet' ? 'is-active' : ''}">
                        <input type="radio" name="type" value="ewallet" ${wallet.type eq 'ewallet' ? 'checked' : ''}>
                        <span class="type-option-icon"><ui:icon name="ewallet"/></span>
                        <span><strong>E-Wallet</strong><small>GoPay, OVO, Dana</small></span>
                    </label>
                </div>
                <c:if test="${not empty errors.type}"><small class="error-text">${f:esc(errors.type)}</small></c:if>
            </div>

            <label class="field">
                <span>Saldo awal</span>
                <span class="input-wrap input-money">
                    <span class="input-prefix">Rp</span>
                    <input type="text" name="initialBalance" id="walletBalance" inputmode="decimal" data-money-input
                           value="${f:moneyInput(wallet.initialBalance)}" placeholder="0" autocomplete="off">
                </span>
                <c:if test="${not empty errors.initialBalance}"><small class="error-text">${f:esc(errors.initialBalance)}</small></c:if>
                <small class="hint">Saldo saat dompet mulai dipakai di Dompetku.</small>
            </label>

            <div class="field">
                <span>Warna kartu</span>
                <div class="color-row">
                    <input type="color" name="color" id="walletColor" value="${wallet.color}">
                    <div class="palette">
                        <c:forTokens var="color" items="${palette}" delims=",">
                            <button type="button" class="swatch" style="background:${color}" data-set-color="${color}"
                                    aria-label="Pilih warna ${color}"></button>
                        </c:forTokens>
                    </div>
                </div>
            </div>

            <div class="form-actions">
                <a class="btn btn-ghost" href="${ctx}/app/wallets">Batal</a>
                <button class="btn btn-primary" type="submit">
                    <ui:icon name="check"/>${mode eq 'edit' ? 'Simpan perubahan' : 'Simpan dompet'}
                </button>
            </div>
        </form>
    </article>

    <aside class="form-aside">
        <div class="wallet-preview" id="walletPreview" style="--wc:${wallet.color}">
            <span class="wallet-preview-type">Pratinjau dompet</span>
            <span class="wallet-preview-icon"><ui:icon name="${wallet.type eq 'cash' ? 'cash' : (wallet.type eq 'bank' ? 'bank' : 'ewallet')}"/></span>
            <strong id="walletPreviewName">${f:esc(empty wallet.name ? 'Nama dompet' : wallet.name)}</strong>
            <span class="wallet-preview-balance" id="walletPreviewBalance">${f:rupiah(wallet.initialBalance)}</span>
            <small>Saldo berjalan akan bertambah dari transaksi.</small>
        </div>
        <div class="help-card">
            <span class="mini-icon"><ui:icon name="shield"/></span>
            <div><strong>Pengamanan</strong><p>Dompet yang sudah memiliki transaksi tidak dapat dihapus agar riwayat tetap utuh.</p></div>
        </div>
    </aside>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
