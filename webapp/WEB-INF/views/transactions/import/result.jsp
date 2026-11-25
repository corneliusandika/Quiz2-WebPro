<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Hasil Import"/>
<c:set var="eyebrow" value="Import selesai"/>
<c:set var="active" value="transactions"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <h2>Hasil import CSV</h2>
        <p>${result.imported} transaksi berhasil disimpan.</p>
    </div>
    <div class="toolbar-actions">
        <a class="btn btn-ghost" href="${ctx}/app/transactions/import"><ui:icon name="refresh"/>Import lagi</a>
        <a class="btn btn-primary" href="${ctx}/app/transactions"><ui:icon name="transactions"/>Lihat transaksi</a>
    </div>
</section>

<section class="mini-stat-row three">
    <div class="mini-stat">
        <span class="mini-icon mini-income"><ui:icon name="check"/></span>
        <div><small>Berhasil</small><strong>${result.imported}</strong></div>
    </div>
    <div class="mini-stat">
        <span class="mini-icon mini-warning"><ui:icon name="refresh"/></span>
        <div><small>Duplikat dilewati</small><strong>${result.duplicate}</strong></div>
    </div>
    <div class="mini-stat">
        <span class="mini-icon mini-expense"><ui:icon name="alert"/></span>
        <div><small>Bermasalah</small><strong>${result.invalid}</strong></div>
    </div>
</section>

<c:if test="${not empty result.errors}">
    <article class="card table-card">
        <div class="card-head">
            <div>
                <h2>Baris yang tidak diimport</h2>
                <p>Perbaiki file atau mapping, lalu ulangi import.</p>
            </div>
        </div>
        <div class="table-wrap">
            <table class="data-table">
                <thead>
                <tr><th>Baris</th><th>Tanggal</th><th>Nominal</th><th>Keterangan</th></tr>
                </thead>
                <tbody>
                <c:forEach var="row" items="${result.errors}" varStatus="s">
                    <c:if test="${s.index < 100}">
                        <tr>
                            <td>${row.line}</td>
                            <td>${f:date(row.date)}</td>
                            <td>${f:rupiah(row.amount)}</td>
                            <td>${f:esc(empty row.error ? (row.duplicate ? row.duplicateReason : 'Tidak diimport') : row.error)}</td>
                        </tr>
                    </c:if>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </article>
</c:if>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
