<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Utang &amp; Piutang"/>
<c:set var="eyebrow" value="Komitmen dengan orang lain"/>
<c:set var="active" value="debts"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <h2>Utang &amp; piutang</h2>
        <p>Catat yang harus dibayar dan yang harus diterima agar tidak lupa.</p>
    </div>
    <a class="btn btn-primary" href="${ctx}/app/debts/new"><ui:icon name="plus"/>Catatan baru</a>
</section>

<section class="mini-stat-row three">
    <div class="mini-stat">
        <span class="mini-icon mini-expense"><ui:icon name="arrow-up"/></span>
        <div><small>Total harus dibayar</small><strong>${f:rupiah(payable)}</strong></div>
    </div>
    <div class="mini-stat">
        <span class="mini-icon mini-income"><ui:icon name="arrow-down"/></span>
        <div><small>Akan diterima</small><strong>${f:rupiah(receivable)}</strong></div>
    </div>
    <div class="mini-stat">
        <span class="mini-icon mini-warning"><ui:icon name="clock"/></span>
        <div><small>Belum selesai</small><strong>${unsettled} catatan</strong></div>
    </div>
</section>

<form class="filters card" method="get" action="${ctx}/app/debts">
    <label class="field field-compact">
        <span>Jenis</span>
        <select name="type">
            <option value="">Semua jenis</option>
            <option value="payable" ${filterType eq 'payable' ? 'selected' : ''}>Harus dibayar</option>
            <option value="receivable" ${filterType eq 'receivable' ? 'selected' : ''}>Akan diterima</option>
        </select>
    </label>
    <label class="field field-compact">
        <span>Status</span>
        <select name="status">
            <option value="">Semua status</option>
            <option value="unpaid" ${filterStatus eq 'unpaid' ? 'selected' : ''}>Belum bayar</option>
            <option value="partial" ${filterStatus eq 'partial' ? 'selected' : ''}>Sebagian</option>
            <option value="overdue" ${filterStatus eq 'overdue' ? 'selected' : ''}>Terlambat</option>
            <option value="paid" ${filterStatus eq 'paid' ? 'selected' : ''}>Lunas</option>
        </select>
    </label>
    <label class="field field-grow">
        <span>Cari</span>
        <span class="input-wrap">
            <ui:icon name="search"/>
            <input type="search" name="q" value="${f:esc(filterQ)}" placeholder="Cari nama atau catatan...">
        </span>
    </label>
    <div class="filter-actions">
        <button class="btn btn-primary" type="submit"><ui:icon name="filter"/>Terapkan</button>
        <a class="btn btn-ghost" href="${ctx}/app/debts">Reset</a>
    </div>
</form>

<c:choose>
    <c:when test="${empty items}">
        <article class="card">
            <div class="empty-state">
                <span class="empty-icon"><ui:icon name="debts"/></span>
                <strong>Tidak ada catatan</strong>
                <p>Semua komitmen sudah beres, atau belum ada yang dicatat.</p>
                <a class="btn btn-primary btn-sm" href="${ctx}/app/debts/new"><ui:icon name="plus"/>Catatan baru</a>
            </div>
        </article>
    </c:when>
    <c:otherwise>
        <div class="debt-cards">
            <c:forEach var="d" items="${items}">
                <article class="debt-card debt-${d.status}">
                    <div class="debt-card-top">
                        <span class="debt-avatar">${f:initials(d.counterpart)}</span>
                        <div class="debt-card-title">
                            <h3>${f:esc(d.counterpart)}</h3>
                            <small>
                                <span class="badge-inline ${d.type eq 'payable' ? 'expense' : 'income'}">
                                    ${d.type eq 'payable' ? 'Harus dibayar' : 'Akan diterima'}
                                </span>
                                ${empty d.note ? '' : ' &middot; '} ${f:esc(d.note)}
                            </small>
                        </div>
                        <span class="badge badge-${d.status}">
                            <c:choose>
                                <c:when test="${d.status eq 'paid'}">Lunas</c:when>
                                <c:when test="${d.status eq 'overdue'}">Terlambat</c:when>
                                <c:when test="${d.status eq 'partial'}">Sebagian</c:when>
                                <c:otherwise>Belum bayar</c:otherwise>
                            </c:choose>
                        </span>
                        <div class="row-actions">
                            <a class="icon-btn small" href="${ctx}/app/debts/edit/${d.id}" title="Edit"><ui:icon name="edit"/></a>
                            <form method="post" action="${ctx}/app/debts/delete"
                                  data-confirm="Hapus catatan ${f:esc(d.counterpart)}?">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="id" value="${d.id}">
                                <button type="submit" class="icon-btn small danger" title="Hapus"><ui:icon name="trash"/></button>
                            </form>
                        </div>
                    </div>
                    <div class="debt-card-numbers">
                        <div><small>Total</small><strong>${f:rupiah(d.amount)}</strong></div>
                        <div><small>Terbayar</small><strong>${f:rupiah(d.paidAmount)}</strong></div>
                        <div><small>Sisa</small><strong class="${d.type eq 'payable' ? 'text-rose' : 'text-emerald'}">${f:rupiah(d.remaining)}</strong></div>
                        <div><small>Tenggat</small><strong>${empty d.dueDate ? '-' : f:date(d.dueDate)}</strong></div>
                    </div>
                    <div class="progress progress-lg">
                        <span class="progress-bar ${d.status eq 'overdue' ? 'over' : (d.status eq 'paid' ? 'safe' : 'warning')}"
                              style="--bar-width:${d.progressPercent}"></span>
                    </div>
                    <div class="debt-card-foot">
                        <span>${d.progressPercent}% terbayar</span>
                        <c:if test="${not d.settled}">
                            <form method="post" action="${ctx}/app/debts/pay" class="pay-form">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="id" value="${d.id}">
                                <span class="input-wrap input-money input-sm">
                                    <span class="input-prefix">Rp</span>
                                    <input type="text" name="amount" inputmode="decimal" data-money-input
                                           placeholder="Nominal" autocomplete="off" required>
                                </span>
                                <button type="submit" class="btn btn-primary btn-sm"><ui:icon name="check"/>${d.type eq 'receivable' ? 'Terima' : 'Bayar'}</button>
                            </form>
                        </c:if>
                        <c:if test="${d.settled}">
                            <span class="settled-label"><ui:icon name="check"/>Selesai</span>
                        </c:if>
                    </div>
                </article>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
