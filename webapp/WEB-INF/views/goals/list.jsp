<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Target Tabungan"/>
<c:set var="eyebrow" value="Masa depan yang direncanakan"/>
<c:set var="active" value="goals"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <h2>Target tabungan</h2>
        <p>Pecah impian besar menjadi setoran kecil yang konsisten.</p>
    </div>
    <a class="btn btn-primary" href="${ctx}/app/goals/new"><ui:icon name="plus"/>Tambah target</a>
</section>

<section class="mini-stat-row three">
    <div class="mini-stat">
        <span class="mini-icon mini-balance"><ui:icon name="goals"/></span>
        <div><small>Total target</small><strong>${f:rupiah(totalTarget)}</strong></div>
    </div>
    <div class="mini-stat">
        <span class="mini-icon mini-income"><ui:icon name="wallet"/></span>
        <div><small>Sudah terkumpul</small><strong>${f:rupiah(totalSaved)}</strong></div>
    </div>
    <div class="mini-stat">
        <span class="mini-icon mini-warning"><ui:icon name="sparkles"/></span>
        <div><small>Target tercapai</small><strong>${completed} dari ${items.size()}</strong></div>
    </div>
</section>

<c:choose>
    <c:when test="${empty items}">
        <article class="card">
            <div class="empty-state">
                <span class="empty-icon"><ui:icon name="goals"/></span>
                <strong>Belum ada target tabungan</strong>
                <p>Mulai dari dana darurat, liburan, atau gadget impian.</p>
                <a class="btn btn-primary btn-sm" href="${ctx}/app/goals/new"><ui:icon name="plus"/>Buat target</a>
            </div>
        </article>
    </c:when>
    <c:otherwise>
        <div class="goal-grid">
            <c:forEach var="g" items="${items}">
                <article class="goal-card" style="--gc:${g.color}">
                    <div class="goal-card-head">
                        <span class="goal-ring" style="--p:${g.progressPercent};--c:${g.color}"><span>${g.progressPercent}%</span></span>
                        <div class="goal-card-title">
                            <h3>${f:esc(g.name)}</h3>
                            <small>
                                <c:choose>
                                    <c:when test="${empty g.deadline}">Tanpa tenggat</c:when>
                                    <c:when test="${g.daysLeft < 0}">Tenggat terlewat</c:when>
                                    <c:when test="${g.daysLeft == 0}">Tenggat hari ini</c:when>
                                    <c:otherwise>${g.daysLeft} hari lagi</c:otherwise>
                                </c:choose>
                            </small>
                        </div>
                        <div class="row-actions">
                            <a class="icon-btn small" href="${ctx}/app/goals/edit/${g.id}" title="Edit"><ui:icon name="edit"/></a>
                            <form method="post" action="${ctx}/app/goals/delete"
                                  data-confirm="Hapus target ${f:esc(g.name)} beserta seluruh riwayat setorannya?">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="id" value="${g.id}">
                                <button type="submit" class="icon-btn small danger" title="Hapus"><ui:icon name="trash"/></button>
                            </form>
                        </div>
                    </div>
                    <div class="goal-amounts">
                        <strong>${f:rupiah(g.savedAmount)}</strong>
                        <span>dari ${f:rupiah(g.targetAmount)}</span>
                    </div>
                    <div class="progress progress-lg">
                        <span class="progress-bar" style="--bar-width:${g.progressPercent};background:${g.color}"></span>
                    </div>
                    <div class="goal-foot">
                        <span>
                            <c:choose>
                                <c:when test="${g.completed}">Target tercapai!</c:when>
                                <c:otherwise>Kurang ${f:rupiah(g.remaining)}</c:otherwise>
                            </c:choose>
                        </span>
                        <a href="${ctx}/app/goals/view/${g.id}">Detail &amp; setor <ui:icon name="arrow-right"/></a>
                    </div>
                </article>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
