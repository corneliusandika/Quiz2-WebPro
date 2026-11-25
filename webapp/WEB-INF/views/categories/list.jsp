<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Kategori"/>
<c:set var="eyebrow" value="Struktur pengeluaran &amp; pemasukan"/>
<c:set var="active" value="categories"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <h2>Kategori transaksi</h2>
        <p>Kelompokkan transaksi agar laporan lebih mudah dibaca.</p>
    </div>
    <a class="btn btn-primary" href="${ctx}/app/categories/new"><ui:icon name="plus"/>Tambah kategori</a>
</section>

<c:if test="${empty items}">
    <article class="card">
        <div class="empty-state">
            <span class="empty-icon"><ui:icon name="categories"/></span>
            <strong>Belum ada kategori</strong>
            <p>Buat kategori pertama untuk mulai mengelompokkan transaksi.</p>
            <a class="btn btn-primary btn-sm" href="${ctx}/app/categories/new"><ui:icon name="plus"/>Kategori baru</a>
        </div>
    </article>
</c:if>

<c:if test="${not empty items}">
    <div class="section-head">
        <h2><span class="dot dot-rose"></span>Pengeluaran</h2>
        <p>Kategori yang dipakai untuk transaksi keluar.</p>
    </div>
    <div class="category-grid">
        <c:forEach var="cat" items="${items}">
            <c:if test="${cat.type eq 'expense'}">
                <article class="category-card">
                    <span class="category-icon" style="background:${cat.color}1f;color:${cat.color}">
                        <ui:icon name="${cat.icon}"/>
                    </span>
                    <div class="category-main">
                        <strong>${f:esc(cat.name)}</strong>
                        <small>${cat.transactionCount} transaksi</small>
                    </div>
                    <div class="row-actions">
                        <a class="icon-btn small" href="${ctx}/app/categories/edit/${cat.id}" title="Edit">
                            <ui:icon name="edit"/>
                        </a>
                        <form method="post" action="${ctx}/app/categories/delete"
                              data-confirm="Hapus kategori ${f:esc(cat.name)}? Transaksi terkait akan menjadi tanpa kategori.">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="id" value="${cat.id}">
                            <button type="submit" class="icon-btn small danger" title="Hapus"><ui:icon name="trash"/></button>
                        </form>
                    </div>
                </article>
            </c:if>
        </c:forEach>
    </div>

    <div class="section-head">
        <h2><span class="dot dot-emerald"></span>Pemasukan</h2>
        <p>Kategori untuk sumber dana yang masuk.</p>
    </div>
    <div class="category-grid">
        <c:forEach var="cat" items="${items}">
            <c:if test="${cat.type eq 'income'}">
                <article class="category-card">
                    <span class="category-icon" style="background:${cat.color}1f;color:${cat.color}">
                        <ui:icon name="${cat.icon}"/>
                    </span>
                    <div class="category-main">
                        <strong>${f:esc(cat.name)}</strong>
                        <small>${cat.transactionCount} transaksi</small>
                    </div>
                    <div class="row-actions">
                        <a class="icon-btn small" href="${ctx}/app/categories/edit/${cat.id}" title="Edit">
                            <ui:icon name="edit"/>
                        </a>
                        <form method="post" action="${ctx}/app/categories/delete"
                              data-confirm="Hapus kategori ${f:esc(cat.name)}? Transaksi terkait akan menjadi tanpa kategori.">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="id" value="${cat.id}">
                            <button type="submit" class="icon-btn small danger" title="Hapus"><ui:icon name="trash"/></button>
                        </form>
                    </div>
                </article>
            </c:if>
        </c:forEach>
    </div>
</c:if>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
