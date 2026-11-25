<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="${mode eq 'edit' ? 'Edit Kategori' : 'Kategori Baru'}"/>
<c:set var="eyebrow" value="Kelompokkan transaksi"/>
<c:set var="active" value="categories"/>
<c:set var="iconOptions" value="tag,salary,briefcase,trending-up,gift,utensils,car,shopping-bag,receipt,film,heart-pulse,book,home,coffee,coins,zap,card,bank,cash,ewallet"/>
<c:set var="palette" value="#10b981,#0ea5e9,#8b5cf6,#f59e0b,#f97316,#ef4444,#ec4899,#14b8a6,#6366f1,#64748b,#d97706,#a855f7"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <a class="back-link" href="${ctx}/app/categories"><ui:icon name="arrow-left"/>Kembali ke kategori</a>
        <h2>${mode eq 'edit' ? 'Perbarui kategori' : 'Buat kategori baru'}</h2>
        <p>Pilih nama, warna, dan ikon yang mudah dikenali.</p>
    </div>
</section>

<div class="form-layout">
    <article class="card form-card">
        <form method="post" action="${ctx}/app/categories/${mode eq 'edit' ? 'update' : 'create'}" id="categoryForm">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="id" value="${category.id}">

            <label class="field">
                <span>Nama kategori</span>
                <input type="text" name="name" id="categoryName" maxlength="60" value="${f:esc(category.name)}"
                       placeholder="Contoh: Makan &amp; Minum" required>
                <c:if test="${not empty errors.name}"><small class="error-text">${f:esc(errors.name)}</small></c:if>
            </label>

            <div class="field">
                <span>Jenis</span>
                <div class="segmented">
                    <label class="segment segment-expense ${category.type eq 'expense' ? 'is-active' : ''}">
                        <input type="radio" name="type" value="expense" ${category.type eq 'expense' ? 'checked' : ''}>
                        <span><ui:icon name="arrow-up"/>Pengeluaran</span>
                    </label>
                    <label class="segment segment-income ${category.type eq 'income' ? 'is-active' : ''}">
                        <input type="radio" name="type" value="income" ${category.type eq 'income' ? 'checked' : ''}>
                        <span><ui:icon name="arrow-down"/>Pemasukan</span>
                    </label>
                </div>
                <c:if test="${not empty errors.type}"><small class="error-text">${f:esc(errors.type)}</small></c:if>
            </div>

            <div class="field">
                <span>Warna</span>
                <div class="color-row">
                    <input type="color" name="color" id="categoryColor" value="${category.color}">
                    <div class="palette">
                        <c:forTokens var="color" items="${palette}" delims=",">
                            <button type="button" class="swatch" style="background:${color}" data-set-color="${color}"
                                    aria-label="Pilih warna ${color}"></button>
                        </c:forTokens>
                    </div>
                </div>
            </div>

            <div class="field">
                <span>Ikon</span>
                <div class="icon-grid">
                    <c:forTokens var="ic" items="${iconOptions}" delims=",">
                        <label class="icon-option ${category.icon eq ic ? 'is-active' : ''}" title="${ic}">
                            <input type="radio" name="icon" value="${ic}" ${category.icon eq ic ? 'checked' : ''}>
                            <span><ui:icon name="${ic}"/></span>
                        </label>
                    </c:forTokens>
                </div>
            </div>

            <div class="form-actions">
                <a class="btn btn-ghost" href="${ctx}/app/categories">Batal</a>
                <button class="btn btn-primary" type="submit">
                    <ui:icon name="check"/>${mode eq 'edit' ? 'Simpan perubahan' : 'Simpan kategori'}
                </button>
            </div>
        </form>
    </article>

    <aside class="form-aside">
        <div class="summary-card preview-category-card">
            <p class="eyebrow">Pratinjau</p>
            <span class="category-icon large" id="categoryPreviewIcon"
                  style="background:${category.color}1f;color:${category.color}">
                <ui:icon name="${category.icon}"/>
            </span>
            <strong class="preview-category-name" id="categoryPreviewName">${f:esc(empty category.name ? 'Nama kategori' : category.name)}</strong>
            <span class="badge badge-preview" id="categoryPreviewType">${category.type eq 'income' ? 'Pemasukan' : 'Pengeluaran'}</span>
        </div>
        <div class="help-card">
            <span class="mini-icon"><ui:icon name="sparkles"/></span>
            <div><strong>Tips warna</strong><p>Gunakan warna berbeda untuk kebutuhan, keinginan, dan tagihan agar grafik lebih mudah dibaca.</p></div>
        </div>
    </aside>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
