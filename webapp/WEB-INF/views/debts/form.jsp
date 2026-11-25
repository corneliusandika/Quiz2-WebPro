<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="${mode eq 'edit' ? 'Edit Catatan' : 'Catatan Baru'}"/>
<c:set var="eyebrow" value="Catat komitmen"/>
<c:set var="active" value="debts"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <a class="back-link" href="${ctx}/app/debts"><ui:icon name="arrow-left"/>Kembali ke utang &amp; piutang</a>
        <h2>${mode eq 'edit' ? 'Perbarui catatan' : 'Catat utang atau piutang'}</h2>
        <p>Catatan membantu menjaga hubungan dan kesehatan finansial.</p>
    </div>
</section>

<div class="form-layout">
    <article class="card form-card">
        <form method="post" action="${ctx}/app/debts/${mode eq 'edit' ? 'update' : 'create'}" id="debtForm">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="id" value="${debt.id}">

            <div class="field">
                <span>Jenis catatan</span>
                <div class="type-options two">
                    <label class="type-option ${debt.type eq 'payable' ? 'is-active' : ''}">
                        <input type="radio" name="type" value="payable" ${debt.type eq 'payable' ? 'checked' : ''}>
                        <span class="type-option-icon"><ui:icon name="arrow-up"/></span>
                        <span><strong>Harus dibayar</strong><small>Utang saya</small></span>
                    </label>
                    <label class="type-option ${debt.type eq 'receivable' ? 'is-active' : ''}">
                        <input type="radio" name="type" value="receivable" ${debt.type eq 'receivable' ? 'checked' : ''}>
                        <span class="type-option-icon"><ui:icon name="arrow-down"/></span>
                        <span><strong>Akan diterima</strong><small>Piutang saya</small></span>
                    </label>
                </div>
                <c:if test="${not empty errors.type}"><small class="error-text">${f:esc(errors.type)}</small></c:if>
            </div>

            <label class="field">
                <span>Nama pihak</span>
                <input type="text" name="counterpart" maxlength="100" value="${f:esc(debt.counterpart)}"
                       placeholder="Contoh: Bayu, Kos Bu Rina" required>
                <c:if test="${not empty errors.counterpart}"><small class="error-text">${f:esc(errors.counterpart)}</small></c:if>
            </label>

            <div class="field-grid">
                <label class="field">
                    <span>Total nominal</span>
                    <span class="input-wrap input-money">
                        <span class="input-prefix">Rp</span>
                        <input type="text" name="amount" inputmode="decimal" data-money-input
                               value="${f:moneyInput(debt.amount)}" placeholder="0" autocomplete="off" required>
                    </span>
                    <c:if test="${not empty errors.amount}"><small class="error-text">${f:esc(errors.amount)}</small></c:if>
                </label>
                <label class="field">
                    <span>Sudah dibayar</span>
                    <span class="input-wrap input-money">
                        <span class="input-prefix">Rp</span>
                        <input type="text" name="paidAmount" inputmode="decimal" data-money-input
                               value="${f:moneyInput(debt.paidAmount)}" placeholder="0" autocomplete="off">
                    </span>
                    <c:if test="${not empty errors.paidAmount}"><small class="error-text">${f:esc(errors.paidAmount)}</small></c:if>
                </label>
            </div>

            <div class="field-grid">
                <label class="field">
                    <span>Tenggat <small class="muted">opsional</small></span>
                    <input type="date" name="dueDate" value="${debt.dueDate}">
                </label>
                <label class="field">
                    <span>Catatan <small class="muted">opsional</small></span>
                    <input type="text" name="note" maxlength="255" value="${f:esc(debt.note)}" placeholder="Contoh: Patungan liburan">
                </label>
            </div>

            <div class="form-actions">
                <a class="btn btn-ghost" href="${ctx}/app/debts">Batal</a>
                <button class="btn btn-primary" type="submit">
                    <ui:icon name="check"/>${mode eq 'edit' ? 'Simpan perubahan' : 'Simpan catatan'}
                </button>
            </div>
        </form>
    </article>

    <aside class="form-aside">
        <div class="summary-card">
            <p class="eyebrow">Pratinjau</p>
            <span class="debt-avatar large" id="debtPreviewAvatar">?</span>
            <strong class="preview-category-name" id="debtPreviewName">Nama pihak</strong>
            <span class="badge badge-preview" id="debtPreviewType">Harus dibayar</span>
            <strong class="summary-amount" id="debtPreviewAmount">Rp 0</strong>
            <p class="muted">Sisa yang belum diselesaikan akan tampil di dashboard.</p>
        </div>
        <div class="help-card">
            <span class="mini-icon"><ui:icon name="sparkles"/></span>
            <div><strong>Tips</strong><p>Catat tenggat agar pengingat muncul di dashboard sebelum terlambat.</p></div>
        </div>
    </aside>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
