<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Transaksi"/>
<c:set var="eyebrow" value="Catatan pemasukan &amp; pengeluaran"/>
<c:set var="active" value="transactions"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <h2>Semua transaksi</h2>
        <p>Telusuri, filter, dan kelola setiap arus uang yang tercatat.</p>
    </div>
    <div class="toolbar-actions">
        <a class="btn btn-ghost" href="${ctx}/app/transactions/trash"><ui:icon name="trash"/>Sampah</a>
        <a class="btn btn-ghost" href="${ctx}/app/transactions/import"><ui:icon name="upload"/>Import CSV</a>
        <a class="btn btn-primary" href="${ctx}/app/transactions/new"><ui:icon name="plus"/>Tambah transaksi</a>
    </div>
</section>

<c:if test="${not empty undoTransactionId}">
    <div class="undo-banner" role="status">
        <span class="mini-icon"><ui:icon name="trash"/></span>
        <div><strong>Transaksi dipindahkan ke sampah.</strong><p>Kamu bisa mengurungkannya sekarang.</p></div>
        <form method="post" action="${ctx}/app/transactions/restore">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="id" value="${undoTransactionId}">
            <button class="btn btn-ghost" type="submit"><ui:icon name="refresh"/>Urungkan</button>
        </form>
    </div>
</c:if>

<section class="mini-stat-row">
    <div class="mini-stat">
        <span class="mini-icon mini-income"><ui:icon name="arrow-down"/></span>
        <div><small>Pemasukan ${empty filterMonth ? '(semua)' : ''}</small><strong>${f:rupiah(monthIncome)}</strong></div>
    </div>
    <div class="mini-stat">
        <span class="mini-icon mini-expense"><ui:icon name="arrow-up"/></span>
        <div><small>Pengeluaran ${empty filterMonth ? '(semua)' : ''}</small><strong>${f:rupiah(monthExpense)}</strong></div>
    </div>
    <div class="mini-stat">
        <span class="mini-icon mini-balance"><ui:icon name="transactions"/></span>
        <div><small>Transaksi ditemukan</small><strong>${total} catatan</strong></div>
    </div>
</section>

<form class="filters filters-grid card" method="get" action="${ctx}/app/transactions">
    <label class="field field-compact">
        <span>Bulan</span>
        <input type="month" name="month" value="${filterMonth}">
    </label>
    <label class="field field-compact">
        <span>Jenis</span>
        <select name="type">
            <option value="">Semua jenis</option>
            <option value="income" ${filterType eq 'income' ? 'selected' : ''}>Pemasukan</option>
            <option value="expense" ${filterType eq 'expense' ? 'selected' : ''}>Pengeluaran</option>
        </select>
    </label>
    <label class="field field-compact">
        <span>Kategori</span>
        <select name="category">
            <option value="">Semua kategori</option>
            <c:forEach var="cat" items="${categories}">
                <option value="${cat.id}" ${filterCategory eq cat.id ? 'selected' : ''}>${f:esc(cat.name)}</option>
            </c:forEach>
        </select>
    </label>
    <label class="field field-compact">
        <span>Dompet</span>
        <select name="wallet">
            <option value="">Semua dompet</option>
            <c:forEach var="w" items="${wallets}">
                <option value="${w.id}" ${filterWallet eq w.id ? 'selected' : ''}>${f:esc(w.name)}</option>
            </c:forEach>
        </select>
    </label>
    <label class="field field-grow">
        <span>Kata kunci</span>
        <span class="input-wrap">
            <ui:icon name="search"/>
            <input type="search" name="q" value="${f:esc(filterQ)}" placeholder="Cari catatan...">
        </span>
    </label>
    <div class="filter-actions">
        <button type="submit" class="btn btn-primary"><ui:icon name="filter"/>Terapkan</button>
        <a class="btn btn-ghost" href="${ctx}/app/transactions">Reset</a>
    </div>
</form>

<article class="card table-card">
    <div class="card-head">
        <div>
            <h2>${total} transaksi</h2>
            <p>Halaman ${page} dari ${totalPages}</p>
        </div>
        <span class="soft-badge"><ui:icon name="transactions"/>${total} catatan</span>
    </div>

    <c:choose>
        <c:when test="${empty items}">
            <div class="empty-state">
                <span class="empty-icon"><ui:icon name="inbox"/></span>
                <strong>Tidak ada transaksi yang cocok</strong>
                <p>Coba ubah filter atau mulai catat transaksi baru.</p>
                <a class="btn btn-primary btn-sm" href="${ctx}/app/transactions/new"><ui:icon name="plus"/>Transaksi baru</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table class="data-table">
                    <thead>
                    <tr>
                        <th>Transaksi</th>
                        <th>Kategori</th>
                        <th>Dompet</th>
                        <th>Tanggal</th>
                        <th class="right">Nominal</th>
                        <th class="right">Aksi</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="tx" items="${items}">
                        <tr>
                            <td>
                                <div class="cell-main">
                                    <span class="tx-icon" style="background:${empty tx.categoryColor ? '#94a3b8' : tx.categoryColor}1f;color:${empty tx.categoryColor ? '#94a3b8' : tx.categoryColor}">
                                        <ui:icon name="${empty tx.categoryIcon ? 'tag' : tx.categoryIcon}"/>
                                    </span>
                                    <div>
                                        <strong>${f:esc(empty tx.note ? tx.categoryName : tx.note)}</strong>
                                        <small class="badge-inline ${tx.type}">${tx.type eq 'income' ? 'Pemasukan' : 'Pengeluaran'}</small>
                                    </div>
                                </div>
                            </td>
                            <td><span class="chip" style="--chip:${empty tx.categoryColor ? '#94a3b8' : tx.categoryColor}">${f:esc(empty tx.categoryName ? 'Tanpa kategori' : tx.categoryName)}</span></td>
                            <td><span class="wallet-pill"><span class="wallet-dot"></span>${f:esc(tx.walletName)}</span></td>
                            <td><span class="date-cell">${f:date(tx.transactionDate)}</span></td>
                            <td class="right">
                                <span class="amount ${tx.type}">${tx.type eq 'income' ? '+' : '-'}${f:rupiah(tx.amount)}</span>
                            </td>
                            <td class="right">
                                <div class="row-actions">
                                    <a class="icon-btn small" href="${ctx}/app/transactions/edit/${tx.id}" title="Edit">
                                        <ui:icon name="edit"/>
                                    </a>
                                    <form method="post" action="${ctx}/app/transactions/delete"
                                          data-confirm="Pindahkan transaksi ini ke sampah? Kamu bisa mengurungkannya.">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="id" value="${tx.id}">
                                        <button type="submit" class="icon-btn small danger" title="Hapus">
                                            <ui:icon name="trash"/>
                                        </button>
                                    </form>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>

            <c:if test="${totalPages > 1}">
                <nav class="pagination">
                    <c:url var="prevUrl" value="/app/transactions">
                        <c:param name="month" value="${filterMonth}"/>
                        <c:param name="type" value="${filterType}"/>
                        <c:param name="category" value="${filterCategory}"/>
                        <c:param name="wallet" value="${filterWallet}"/>
                        <c:param name="q" value="${filterQ}"/>
                        <c:param name="page" value="${page - 1}"/>
                    </c:url>
                    <c:url var="nextUrl" value="/app/transactions">
                        <c:param name="month" value="${filterMonth}"/>
                        <c:param name="type" value="${filterType}"/>
                        <c:param name="category" value="${filterCategory}"/>
                        <c:param name="wallet" value="${filterWallet}"/>
                        <c:param name="q" value="${filterQ}"/>
                        <c:param name="page" value="${page + 1}"/>
                    </c:url>
                    <a class="page-btn ${page <= 1 ? 'is-disabled' : ''}" href="${page <= 1 ? '#' : prevUrl}">
                        <ui:icon name="chevron-left"/>
                    </a>
                    <c:forEach begin="${page - 2 < 1 ? 1 : page - 2}" end="${page + 2 > totalPages ? totalPages : page + 2}" var="p">
                        <c:url var="pageUrl" value="/app/transactions">
                            <c:param name="month" value="${filterMonth}"/>
                            <c:param name="type" value="${filterType}"/>
                            <c:param name="category" value="${filterCategory}"/>
                            <c:param name="wallet" value="${filterWallet}"/>
                            <c:param name="q" value="${filterQ}"/>
                            <c:param name="page" value="${p}"/>
                        </c:url>
                        <a class="page-btn ${p eq page ? 'is-active' : ''}" href="${pageUrl}">${p}</a>
                    </c:forEach>
                    <a class="page-btn ${page >= totalPages ? 'is-disabled' : ''}" href="${page >= totalPages ? '#' : nextUrl}">
                        <ui:icon name="chevron-right"/>
                    </a>
                </nav>
            </c:if>
        </c:otherwise>
    </c:choose>
</article>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
