<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="${mode eq 'edit' ? 'Edit Target' : 'Target Baru'}"/>
<c:set var="eyebrow" value="Rancang impianmu"/>
<c:set var="active" value="goals"/>
<c:set var="palette" value="#10b981,#0ea5e9,#8b5cf6,#f59e0b,#ec4899,#14b8a6,#f97316,#6366f1"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <a class="back-link" href="${ctx}/app/goals"><ui:icon name="arrow-left"/>Kembali ke target</a>
        <h2>${mode eq 'edit' ? 'Perbarui target tabungan' : 'Buat target tabungan'}</h2>
        <p>Target yang jelas membuat setiap setoran terasa lebih berarti.</p>
    </div>
</section>

<div class="form-layout">
    <article class="card form-card">
        <form method="post" action="${ctx}/app/goals/${mode eq 'edit' ? 'update' : 'create'}" id="goalForm">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="id" value="${goal.id}">

            <label class="field">
                <span>Nama target</span>
                <input type="text" name="name" id="goalName" maxlength="100" value="${f:esc(goal.name)}"
                       placeholder="Contoh: Dana Darurat, Liburan Bali" required>
                <c:if test="${not empty errors.name}"><small class="error-text">${f:esc(errors.name)}</small></c:if>
            </label>

            <div class="field-grid">
                <label class="field">
                    <span>Target nominal</span>
                    <span class="input-wrap input-money">
                        <span class="input-prefix">Rp</span>
                        <input type="text" name="targetAmount" id="goalAmount" inputmode="decimal" data-money-input
                               value="${f:moneyInput(goal.targetAmount)}" placeholder="0" autocomplete="off" required>
                    </span>
                    <c:if test="${not empty errors.targetAmount}"><small class="error-text">${f:esc(errors.targetAmount)}</small></c:if>
                </label>
                <label class="field">
                    <span>Tenggat <small class="muted">opsional</small></span>
                    <input type="date" name="deadline" value="${goal.deadline}">
                </label>
            </div>

            <div class="field">
                <span>Warna target</span>
                <div class="color-row">
                    <input type="color" name="color" id="goalColor" value="${goal.color}">
                    <div class="palette">
                        <c:forTokens var="color" items="${palette}" delims=",">
                            <button type="button" class="swatch" style="background:${color}" data-set-color="${color}"
                                    aria-label="Pilih warna ${color}"></button>
                        </c:forTokens>
                    </div>
                </div>
            </div>

            <div class="form-actions">
                <a class="btn btn-ghost" href="${ctx}/app/goals">Batal</a>
                <button class="btn btn-primary" type="submit">
                    <ui:icon name="check"/>${mode eq 'edit' ? 'Simpan perubahan' : 'Buat target'}
                </button>
            </div>
        </form>
    </article>

    <aside class="form-aside">
        <div class="summary-card preview-goal-card" id="goalPreview" style="--gc:${goal.color}">
            <p class="eyebrow">Pratinjau</p>
            <span class="goal-ring xl" style="--p:0;--c:${goal.color}"><span>0%</span></span>
            <strong class="preview-category-name" id="goalPreviewName">${f:esc(empty goal.name ? 'Nama target' : goal.name)}</strong>
            <strong class="summary-amount" id="goalPreviewAmount">Rp 0</strong>
            <p class="muted">Kumpulkan sedikit demi sedikit. Konsistensi mengalahkan nominal besar.</p>
        </div>
        <div class="help-card">
            <span class="mini-icon"><ui:icon name="sparkles"/></span>
            <div><strong>Tips setoran</strong><p>Sisihkan 10&ndash;20% pemasukan setiap bulan tepat setelah gajian.</p></div>
        </div>
    </aside>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
