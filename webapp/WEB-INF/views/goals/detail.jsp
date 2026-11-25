<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Detail Target"/>
<c:set var="eyebrow" value="Perjalanan menuju target"/>
<c:set var="active" value="goals"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <a class="back-link" href="${ctx}/app/goals"><ui:icon name="arrow-left"/>Kembali ke target</a>
        <h2>${f:esc(goal.name)}</h2>
        <p>Catat setiap setoran dan lihat progresmu bertambah.</p>
    </div>
    <div class="toolbar-actions">
        <a class="btn btn-ghost" href="${ctx}/app/goals/edit/${goal.id}"><ui:icon name="edit"/>Edit target</a>
        <form method="post" action="${ctx}/app/goals/delete"
              data-confirm="Hapus target ${f:esc(goal.name)} beserta seluruh riwayat setorannya?">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="id" value="${goal.id}">
            <button type="submit" class="btn btn-danger-ghost"><ui:icon name="trash"/>Hapus</button>
        </form>
    </div>
</section>

<section class="goal-hero" style="--gc:${goal.color}">
    <div class="goal-hero-ring">
        <span class="goal-ring xl" style="--p:${goal.progressPercent};--c:${goal.color}"><span>${goal.progressPercent}%</span></span>
    </div>
    <div class="goal-hero-main">
        <span class="pill pill-soft"><ui:icon name="goals"/>${goal.completed ? 'Target tercapai' : 'Sedang berjalan'}</span>
        <h2>${f:esc(goal.name)}</h2>
        <p>
            <c:choose>
                <c:when test="${empty goal.deadline}">Tanpa tenggat waktu. Tetap konsisten menyisihkan dana.</c:when>
                <c:when test="${goal.daysLeft < 0}">Tenggat sudah terlewat ${-goal.daysLeft} hari. Yuk kejar kembali!</c:when>
                <c:when test="${goal.daysLeft == 0}">Tenggat hari ini. Saatnya menuntaskan target.</c:when>
                <c:otherwise>Tersisa ${goal.daysLeft} hari lagi untuk mencapai target.</c:otherwise>
            </c:choose>
        </p>
        <div class="goal-hero-stats">
            <div><small>Terkumpul</small><strong>${f:rupiah(goal.savedAmount)}</strong></div>
            <div><small>Target</small><strong>${f:rupiah(goal.targetAmount)}</strong></div>
            <div><small>Sisa</small><strong>${f:rupiah(goal.remaining)}</strong></div>
            <div><small>Tenggat</small><strong>${empty goal.deadline ? 'Tanpa tenggat' : f:date(goal.deadline)}</strong></div>
        </div>
    </div>
</section>

<div class="goal-detail-grid">
    <article class="card form-card">
        <div class="card-head">
            <div>
                <h2>Tambah setoran</h2>
                <p>Setiap setoran langsung menambah progres target.</p>
            </div>
            <span class="soft-badge"><ui:icon name="plus"/>Setoran</span>
        </div>
        <form method="post" action="${ctx}/app/goals/deposit" class="deposit-form">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="goalId" value="${goal.id}">
            <div class="field-grid">
                <label class="field">
                    <span>Nominal setoran</span>
                    <span class="input-wrap input-money">
                        <span class="input-prefix">Rp</span>
                        <input type="text" name="amount" inputmode="decimal" data-money-input
                               value="${f:moneyInput(empty deposit ? null : deposit.amount)}" placeholder="0"
                               autocomplete="off" required>
                    </span>
                    <c:if test="${not empty errors.amount}"><small class="error-text">${f:esc(errors.amount)}</small></c:if>
                </label>
                <label class="field">
                    <span>Tanggal</span>
                    <input type="date" name="date" value="${empty deposit ? today : deposit.depositDate}">
                </label>
            </div>
            <label class="field">
                <span>Catatan <small class="muted">opsional</small></span>
                <input type="text" name="note" maxlength="255" placeholder="Contoh: Bonus proyek, tabungan bulanan"
                       value="${f:esc(empty deposit ? '' : deposit.note)}">
            </label>
            <button class="btn btn-primary" type="submit"><ui:icon name="plus"/>Catat setoran</button>
        </form>
    </article>

    <aside class="form-aside">
        <div class="summary-card">
            <p class="eyebrow">Ringkasan setoran</p>
            <div class="deposit-stats">
                <div><small>Total setoran</small><strong>${f:rupiah(totalDeposits)}</strong></div>
                <div><small>Jumlah transaksi</small><strong>${deposits.size()} setoran</strong></div>
                <div><small>Rata-rata</small><strong>${f:rupiah(avgDeposit)}</strong></div>
            </div>
            <div class="summary-tip">
                <ui:icon name="sparkles"/>
                <p>Ubah setoran menjadi kebiasaan otomatis setiap tanggal gajian.</p>
            </div>
        </div>
        <div class="help-card">
            <span class="mini-icon"><ui:icon name="shield"/></span>
            <div><strong>Konsistensi</strong><p>Setoran kecil yang rutin lebih efektif daripada setoran besar yang jarang.</p></div>
        </div>
    </aside>
</div>

<article class="card table-card">
    <div class="card-head">
        <div>
            <h2>Riwayat setoran</h2>
            <p>${deposits.size()} setoran tercatat</p>
        </div>
        <span class="soft-badge"><ui:icon name="clock"/>Terbaru</span>
    </div>
    <c:choose>
        <c:when test="${empty deposits}">
            <div class="empty-state compact">
                <span class="empty-icon"><ui:icon name="inbox"/></span>
                <strong>Belum ada setoran</strong>
                <p>Setoran pertama akan muncul di sini.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table class="data-table">
                    <thead>
                    <tr><th>Tanggal</th><th>Catatan</th><th class="right">Nominal</th><th class="right">Aksi</th></tr>
                    </thead>
                    <tbody>
                    <c:forEach var="d" items="${deposits}">
                        <tr>
                            <td><span class="date-cell">${f:date(d.depositDate)}</span></td>
                            <td>${f:esc(empty d.note ? 'Setoran target' : d.note)}</td>
                            <td class="right"><span class="amount income">+${f:rupiah(d.amount)}</span></td>
                            <td class="right">
                                <div class="row-actions">
                                    <a class="icon-btn small" href="${ctx}/app/goals/deposit/edit/${d.id}"
                                       title="Edit setoran"><ui:icon name="edit"/></a>
                                    <form method="post" action="${ctx}/app/goals/deposit/delete"
                                          data-confirm="Hapus setoran ini? Saldo target akan dikurangi kembali.">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="id" value="${d.id}">
                                        <input type="hidden" name="goalId" value="${goal.id}">
                                        <button type="submit" class="icon-btn small danger" title="Hapus"><ui:icon name="trash"/></button>
                                    </form>
                                </div>
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
