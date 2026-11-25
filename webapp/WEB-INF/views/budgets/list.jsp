<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Budget"/>
<c:set var="eyebrow" value="Kendali pengeluaran bulanan"/>
<c:set var="active" value="budgets"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <h2>Budget ${f:month(month)}</h2>
        <p>Tetapkan batas pengeluaran per kategori dan pantau realisasinya.</p>
    </div>
    <div class="toolbar-actions">
        <div class="month-nav">
            <a class="icon-btn" href="${ctx}/app/budgets?month=${prevMonthKey}" aria-label="Bulan lalu"><ui:icon name="chevron-left"/></a>
            <span>${f:month(month)}</span>
            <a class="icon-btn" href="${ctx}/app/budgets?month=${nextMonthKey}" aria-label="Bulan depan"><ui:icon name="chevron-right"/></a>
        </div>
        <a class="btn btn-primary" href="${ctx}/app/budgets/new?month=${monthKey}"><ui:icon name="plus"/>Buat budget</a>
    </div>
</section>

<section class="mini-stat-row four">
    <div class="mini-stat">
        <span class="mini-icon mini-balance"><ui:icon name="budget"/></span>
        <div><small>Total budget</small><strong>${f:rupiah(totalBudget)}</strong></div>
    </div>
    <div class="mini-stat">
        <span class="mini-icon mini-expense"><ui:icon name="arrow-up"/></span>
        <div><small>Terpakai</small><strong>${f:rupiah(totalSpent)}</strong></div>
    </div>
    <div class="mini-stat">
        <span class="mini-icon mini-income"><ui:icon name="wallet"/></span>
        <div><small>Sisa budget</small><strong>${f:rupiah(totalRemaining)}</strong></div>
    </div>
    <div class="mini-stat">
        <span class="mini-icon mini-warning"><ui:icon name="alert"/></span>
        <div><small>Melewati batas</small><strong>${overCount} kategori</strong></div>
    </div>
</section>

<c:choose>
    <c:when test="${empty items}">
        <article class="card">
            <div class="empty-state">
                <span class="empty-icon"><ui:icon name="budget"/></span>
                <strong>Belum ada budget bulan ini</strong>
                <p>Tentukan batas pengeluaran agar tidak kebablasan di akhir bulan.</p>
                <a class="btn btn-primary btn-sm" href="${ctx}/app/budgets/new?month=${monthKey}"><ui:icon name="plus"/>Buat budget</a>
            </div>
        </article>
    </c:when>
    <c:otherwise>
        <div class="budget-grid">
            <c:forEach var="b" items="${items}">
                <article class="budget-card budget-${b.status}">
                    <div class="budget-top">
                        <span class="category-icon" style="background:${b.categoryColor}1f;color:${b.categoryColor}">
                            <ui:icon name="${empty b.categoryIcon ? 'tag' : b.categoryIcon}"/>
                        </span>
                        <div class="budget-title">
                            <strong>${f:esc(b.categoryName)}</strong>
                            <small>${f:month(b.month)}</small>
                        </div>
                        <div class="row-actions">
                            <a class="icon-btn small" href="${ctx}/app/budgets/edit/${b.id}" title="Edit"><ui:icon name="edit"/></a>
                            <form method="post" action="${ctx}/app/budgets/delete"
                                  data-confirm="Hapus budget ${f:esc(b.categoryName)} untuk ${f:month(b.month)}?">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="id" value="${b.id}">
                                <input type="hidden" name="returnMonth" value="${monthKey}">
                                <button type="submit" class="icon-btn small danger" title="Hapus"><ui:icon name="trash"/></button>
                            </form>
                        </div>
                    </div>
                    <div class="budget-numbers">
                        <strong>${f:rupiah(b.spent)}</strong>
                        <span>dari ${f:rupiah(b.amount)}</span>
                    </div>
                    <div class="progress progress-lg">
                        <span class="progress-bar ${b.status}" style="--bar-width:${b.barWidth}"></span>
                    </div>
                    <div class="budget-foot">
                        <span class="badge badge-${b.status}">
                            ${b.percentage}%
                            <c:choose>
                                <c:when test="${b.status eq 'over'}">terpakai &middot; over</c:when>
                                <c:when test="${b.status eq 'warning'}">terpakai &middot; hampir habis</c:when>
                                <c:otherwise>terpakai &middot; aman</c:otherwise>
                            </c:choose>
                        </span>
                        <span class="budget-remaining ${b.remaining.signum() < 0 ? 'negative' : ''}">
                            ${b.remaining.signum() < 0 ? 'Lebih ' : 'Sisa '}${f:rupiah(b.remaining.abs())}
                        </span>
                    </div>
                </article>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
