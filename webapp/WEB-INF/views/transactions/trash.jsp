<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Sampah Transaksi"/>
<c:set var="eyebrow" value="Transaksi yang bisa dipulihkan"/>
<c:set var="active" value="transactions"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <a class="back-link" href="${ctx}/app/transactions"><ui:icon name="arrow-left"/>Kembali ke transaksi</a>
        <h2>Sampah transaksi</h2>
        <p>Transaksi di sini tidak memengaruhi saldo, laporan, atau budget.</p>
    </div>
    <span class="soft-badge"><ui:icon name="trash"/>${total} transaksi</span>
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

<article class="card table-card">
    <div class="card-head">
        <div>
            <h2>${total} transaksi di sampah</h2>
            <p>Pulihkan jika masih dibutuhkan.</p>
        </div>
    </div>
    <c:choose>
        <c:when test="${empty items}">
            <div class="empty-state">
                <span class="empty-icon"><ui:icon name="trash"/></span>
                <strong>Sampah kosong</strong>
                <p>Transaksi yang dihapus akan muncul di sini.</p>
                <a class="btn btn-primary btn-sm" href="${ctx}/app/transactions"><ui:icon name="arrow-left"/>Kembali</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table class="data-table">
                    <thead>
                    <tr>
                        <th>Transaksi</th>
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
                                        <strong>${f:esc(empty tx.note ? (empty tx.categoryName ? 'Tanpa kategori' : tx.categoryName) : tx.note)}</strong>
                                        <small class="badge-inline ${tx.type}">${tx.type eq 'income' ? 'Pemasukan' : 'Pengeluaran'}</small>
                                    </div>
                                </div>
                            </td>
                            <td><span class="wallet-pill"><span class="wallet-dot"></span>${f:esc(tx.walletName)}</span></td>
                            <td><span class="date-cell">${f:date(tx.transactionDate)}</span></td>
                            <td class="right"><span class="amount ${tx.type}">${tx.type eq 'income' ? '+' : '-'}${f:rupiah(tx.amount)}</span></td>
                            <td class="right">
                                <form method="post" action="${ctx}/app/transactions/restore">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="id" value="${tx.id}">
                                    <button type="submit" class="btn btn-ghost btn-sm"><ui:icon name="refresh"/>Pulihkan</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</article>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
