<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Edit Setoran"/>
<c:set var="eyebrow" value="Perbarui riwayat setoran"/>
<c:set var="active" value="goals"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <a class="back-link" href="${ctx}/app/goals/view/${goal.id}"><ui:icon name="arrow-left"/>Kembali ke detail target</a>
        <h2>Edit setoran</h2>
        <p>Perubahan nominal akan otomatis menyesuaikan dana terkumpul pada target.</p>
    </div>
</section>

<div class="form-layout">
    <article class="card form-card">
        <form method="post" action="${ctx}/app/goals/deposit/update">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="id" value="${deposit.id}">
            <input type="hidden" name="goalId" value="${goal.id}">

            <div class="field">
                <span>Target tabungan</span>
                <div class="readonly-value"><ui:icon name="goals"/>${f:esc(goal.name)}</div>
            </div>

            <div class="field-grid">
                <label class="field">
                    <span>Nominal setoran</span>
                    <span class="input-wrap input-money">
                        <span class="input-prefix">Rp</span>
                        <input type="text" name="amount" inputmode="decimal" data-money-input
                               value="${f:moneyInput(deposit.amount)}" placeholder="0" autocomplete="off" required>
                    </span>
                    <c:if test="${not empty errors.amount}"><small class="error-text">${f:esc(errors.amount)}</small></c:if>
                </label>
                <label class="field">
                    <span>Tanggal</span>
                    <input type="date" name="date" value="${deposit.depositDate}">
                </label>
            </div>

            <label class="field">
                <span>Catatan <small class="muted">opsional</small></span>
                <input type="text" name="note" maxlength="255"
                       value="${f:esc(deposit.note)}" placeholder="Contoh: Bonus proyek, tabungan bulanan">
            </label>

            <div class="form-actions">
                <a class="btn btn-ghost" href="${ctx}/app/goals/view/${goal.id}">Batal</a>
                <button class="btn btn-primary" type="submit"><ui:icon name="check"/>Simpan perubahan</button>
            </div>
        </form>
    </article>

    <aside class="form-aside">
        <div class="summary-card">
            <p class="eyebrow">Posisi target</p>
            <span class="goal-ring xl" style="--p:${goal.progressPercent};--c:${goal.color}">
                <span>${goal.progressPercent}%</span>
            </span>
            <strong class="preview-category-name">${f:esc(goal.name)}</strong>
            <div class="deposit-stats">
                <div><small>Terkumpul</small><strong>${f:rupiah(goal.savedAmount)}</strong></div>
                <div><small>Target</small><strong>${f:rupiah(goal.targetAmount)}</strong></div>
                <div><small>Sisa</small><strong>${f:rupiah(goal.remaining)}</strong></div>
            </div>
        </div>
        <div class="help-card">
            <span class="mini-icon"><ui:icon name="sparkles"/></span>
            <div><strong>Penyesuaian otomatis</strong><p>Jika setoran diperkecil, dana terkumpul berkurang sebesar selisihnya. Jika diperbesar, dana terkumpul bertambah.</p></div>
        </div>
    </aside>
</div>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
