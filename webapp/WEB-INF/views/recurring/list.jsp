<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Transaksi Berulang"/>
<c:set var="eyebrow" value="Otomatisasi rutinitas"/>
<c:set var="active" value="recurring"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <h2>Jadwal transaksi</h2>
        <p>Simpan transaksi rutin dan jalankan dengan satu klik saat jatuh tempo.</p>
    </div>
    <a class="btn btn-primary" href="${ctx}/app/recurring/new"><ui:icon name="plus"/>Jadwal baru</a>
</section>

<section class="mini-stat-row three">
    <div class="mini-stat">
        <span class="mini-icon mini-balance"><ui:icon name="recurring"/></span>
        <div><small>Total jadwal</small><strong>${items.size()} jadwal</strong></div>
    </div>
    <div class="mini-stat">
        <span class="mini-icon mini-warning"><ui:icon name="clock"/></span>
        <div><small>Jatuh tempo 7 hari</small><strong>${dueSoon} jadwal</strong></div>
    </div>
    <div class="mini-stat">
        <span class="mini-icon mini-expense"><ui:icon name="arrow-up"/></span>
        <div><small>Estimasi pengeluaran / bulan</small><strong>${f:rupiah(monthlyExpense)}</strong></div>
    </div>
</section>

<c:choose>
    <c:when test="${empty items}">
        <article class="card">
            <div class="empty-state">
                <span class="empty-icon"><ui:icon name="recurring"/></span>
                <strong>Belum ada jadwal berulang</strong>
                <p>Mulai dengan kos, langganan, atau gaji bulanan.</p>
                <a class="btn btn-primary btn-sm" href="${ctx}/app/recurring/new"><ui:icon name="plus"/>Jadwal baru</a>
            </div>
        </article>
    </c:when>
    <c:otherwise>
        <div class="recurring-list">
            <c:forEach var="r" items="${items}">
                <article class="recurring-card ${r.active ? '' : 'is-paused'} ${r.dueSoon ? 'is-due' : ''}">
                    <span class="tx-icon" style="background:${empty r.categoryColor ? '#94a3b8' : r.categoryColor}1f;color:${empty r.categoryColor ? '#94a3b8' : r.categoryColor}">
                        <ui:icon name="${empty r.categoryIcon ? 'recurring' : r.categoryIcon}"/>
                    </span>
                    <div class="recurring-main">
                        <h3>${f:esc(r.name)}</h3>
                        <small>
                            <span class="badge-inline ${r.type eq 'income' ? 'income' : 'expense'}">${r.type eq 'income' ? 'Pemasukan' : 'Pengeluaran'}</span>
                            <c:choose>
                                <c:when test="${r.frequency eq 'daily'}">Harian</c:when>
                                <c:when test="${r.frequency eq 'weekly'}">Mingguan</c:when>
                                <c:otherwise>Bulanan</c:otherwise>
                            </c:choose>
                            &middot; ${f:esc(r.walletName)}
                        </small>
                    </div>
                    <div class="recurring-next">
                        <small>Jadwal berikutnya</small>
                        <strong>${f:date(r.nextDate)}</strong>
                    </div>
                    <strong class="amount ${r.type eq 'income' ? 'income' : 'expense'}">${r.type eq 'income' ? '+' : '-'}${f:rupiah(r.amount)}</strong>
                    <span class="badge ${r.active ? 'badge-safe' : 'badge-paused'}">${r.active ? 'Aktif' : 'Jeda'}</span>
                    <div class="row-actions">
                        <c:if test="${r.active}">
                            <form method="post" action="${ctx}/app/recurring/run"
                                  data-confirm="Catat transaksi ${f:esc(r.name)} sekarang dan majukan jadwal berikutnya?">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="id" value="${r.id}">
                                <button type="submit" class="btn btn-ghost btn-sm"><ui:icon name="play"/>Jalankan</button>
                            </form>
                        </c:if>
                        <form method="post" action="${ctx}/app/recurring/toggle">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="id" value="${r.id}">
                            <input type="hidden" name="active" value="${r.active ? '0' : '1'}">
                            <button type="submit" class="icon-btn small" title="${r.active ? 'Jeda' : 'Aktifkan'}">
                                <ui:icon name="${r.active ? 'pause' : 'play'}"/>
                            </button>
                        </form>
                        <a class="icon-btn small" href="${ctx}/app/recurring/edit/${r.id}" title="Edit"><ui:icon name="edit"/></a>
                        <form method="post" action="${ctx}/app/recurring/delete"
                              data-confirm="Hapus jadwal ${f:esc(r.name)}? Transaksi yang sudah tercatat tetap tersimpan.">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="id" value="${r.id}">
                            <button type="submit" class="icon-btn small danger" title="Hapus"><ui:icon name="trash"/></button>
                        </form>
                    </div>
                </article>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
