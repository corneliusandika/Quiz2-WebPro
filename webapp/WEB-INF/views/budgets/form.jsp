<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="${mode eq 'edit' ? 'Edit Budget' : 'Budget Baru'}"/>
<c:set var="eyebrow" value="Rencanakan pengeluaran"/>
<c:set var="active" value="budgets"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <a class="back-link" href="${ctx}/app/budgets?month=${budget.monthKey}"><ui:icon name="arrow-left"/>Kembali ke budget</a>
        <h2>${mode eq 'edit' ? 'Perbarui budget' : 'Buat budget baru'}</h2>
        <p>Satu kategori hanya boleh memiliki satu budget per bulan.</p>
    </div>
</section>

<div class="form-layout">
    <article class="card form-card">
        <form method="post" action="${ctx}/app/budgets/${mode eq 'edit' ? 'update' : 'create'}" id="budgetForm">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="id" value="${budget.id}">

            <label class="field">
                <span>Kategori pengeluaran</span>
                <select name="categoryId" id="budgetCategory" required>
                    <option value="">Pilih kategori</option>
                    <c:forEach var="cat" items="${categories}">
                        <option value="${cat.id}" data-color="${cat.color}" ${budget.categoryId eq cat.id ? 'selected' : ''}>${f:esc(cat.name)}</option>
                    </c:forEach>
                </select>
                <c:if test="${not empty errors.categoryId}"><small class="error-text">${f:esc(errors.categoryId)}</small></c:if>
            </label>

            <div class="field-grid">
                <label class="field">
                    <span>Bulan</span>
                    <input type="month" name="month" value="${budget.monthKey}" required>
                    <c:if test="${not empty errors.month}"><small class="error-text">${f:esc(errors.month)}</small></c:if>
                </label>
                <label class="field">
                    <span>Batas budget</span>
                    <span class="input-wrap input-money">
                        <span class="input-prefix">Rp</span>
                        <input type="text" name="amount" id="budgetAmount" inputmode="decimal" data-money-input
                               value="${f:moneyInput(budget.amount)}" placeholder="0" autocomplete="off" required>
                    </span>
                    <c:if test="${not empty errors.amount}"><small class="error-text">${f:esc(errors.amount)}</small></c:if>
                </label>
            </div>

            <div class="form-actions">
                <a class="btn btn-ghost" href="${ctx}/app/budgets?month=${budget.monthKey}">Batal</a>
                <button class="btn btn-primary" type="submit">
                    <ui:icon name="check"/>${mode eq 'edit' ? 'Simpan perubahan' : 'Simpan budget'}
                </button>
            </div>
        </form>
    </article>

    <aside class="form-aside">
        <div class="summary-card">
            <p class="eyebrow">Pratinjau</p>
            <span class="category-icon large" id="budgetPreviewIcon" style="background:#f973161f;color:#f97316">
                <ui:icon name="utensils"/>
            </span>
            <strong class="preview-category-name" id="budgetPreviewName">Pilih kategori</strong>
            <strong class="summary-amount" id="budgetPreviewAmount">Rp 0</strong>
            <p class="muted">Batas pengeluaran yang kamu izinkan untuk kategori ini.</p>
        </div>
        <div class="help-card">
            <span class="mini-icon"><ui:icon name="sparkles"/></span>
            <div><strong>Rumus sehat</strong><p>Gunakan budget 80% dari estimasi pengeluaran normal, sisakan 20% untuk kejutan.</p></div>
        </div>
    </aside>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
