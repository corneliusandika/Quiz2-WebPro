<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Laporan"/>
<c:set var="eyebrow" value="Rekap bulanan yang bisa diunduh"/>
<c:set var="active" value="reports"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="page-toolbar">
    <div>
        <h2>Laporan ${f:month(month)}</h2>
        <p>Analisis pemasukan, pengeluaran, dan kebiasaan finansial bulan ini.</p>
    </div>
    <div class="toolbar-actions">
        <div class="month-nav">
            <a class="icon-btn" href="${ctx}/app/reports?month=${prevMonthKey}" aria-label="Bulan lalu"><ui:icon name="chevron-left"/></a>
            <span>${f:month(month)}</span>
            <a class="icon-btn" href="${ctx}/app/reports?month=${nextMonthKey}" aria-label="Bulan depan"><ui:icon name="chevron-right"/></a>
        </div>
        <a class="btn btn-ghost" href="${ctx}/app/reports/pdf?month=${monthKey}">
            <ui:icon name="download"/>Unduh PDF
        </a>
        <a class="btn btn-primary" href="${ctx}/app/reports/export?month=${monthKey}">
            <ui:icon name="download"/>Unduh CSV
        </a>
    </div>
</section>

<section class="mini-stat-row four">
    <div class="mini-stat">
        <span class="mini-icon mini-income"><ui:icon name="arrow-down"/></span>
        <div><small>Pemasukan</small><strong>${f:rupiah(income)}</strong></div>
    </div>
    <div class="mini-stat">
        <span class="mini-icon mini-expense"><ui:icon name="arrow-up"/></span>
        <div><small>Pengeluaran</small><strong>${f:rupiah(expense)}</strong></div>
    </div>
    <div class="mini-stat">
        <span class="mini-icon mini-balance"><ui:icon name="wallet"/></span>
        <div><small>Selisih bersih</small><strong>${f:rupiah(net)}</strong></div>
    </div>
    <div class="mini-stat">
        <span class="mini-icon mini-warning"><ui:icon name="transactions"/></span>
        <div><small>Transaksi</small><strong>${transactionCount} catatan</strong></div>
    </div>
</section>

<div class="dashboard-grid">
    <article class="card chart-card span-2">
        <div class="card-head">
            <div>
                <h2>Arus kas harian</h2>
                <p>Pola pemasukan dan pengeluaran sepanjang ${f:month(month)}</p>
            </div>
            <div class="chart-legend">
                <span><i class="legend-dot legend-income"></i>Masuk</span>
                <span><i class="legend-dot legend-expense"></i>Keluar</span>
            </div>
        </div>
        <div class="chart-body">
            <canvas id="reportDailyChart" height="250" role="img" aria-label="Grafik arus kas harian laporan bulan ini" aria-describedby="reportDailySummary"></canvas>
        </div>
        <div class="sr-only" id="reportDailySummary">
            <table>
                <caption>Arus kas harian ${f:month(month)}</caption>
                <thead><tr><th scope="col">Tanggal</th><th scope="col">Pemasukan</th><th scope="col">Pengeluaran</th></tr></thead>
                <tbody>
                <c:forEach var="d" items="${daily}">
                    <tr><th scope="row">${d.day}</th><td>${f:rupiah(d.income)}</td><td>${f:rupiah(d.expense)}</td></tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </article>

    <article class="card">
        <div class="card-head">
            <div>
                <h2>Komposisi pengeluaran</h2>
                <p>${categories.size()} kategori terpakai</p>
            </div>
            <span class="soft-badge"><ui:icon name="budget"/>${f:compact(expense)}</span>
        </div>
        <div class="breakdown-list">
            <c:forEach var="cat" items="${categories}">
                <div class="breakdown-row">
                    <div class="breakdown-head">
                        <span class="legend-dot" style="background:${cat.color}"></span>
                        <strong>${f:esc(cat.name)}</strong>
                        <span class="breakdown-pct">${cat.percentage}%</span>
                        <span class="amount">${f:rupiah(cat.total)}</span>
                    </div>
                    <div class="progress">
                        <span class="progress-bar" style="--bar-width:${cat.percentage};background:${cat.color}"></span>
                    </div>
                </div>
            </c:forEach>
            <c:if test="${empty categories}">
                <div class="empty-inline"><ui:icon name="inbox"/>Belum ada pengeluaran.</div>
            </c:if>
        </div>
    </article>

    <article class="card chart-card span-2">
        <div class="card-head">
            <div>
                <h2>Tren 6 bulan</h2>
                <p>Perbandingan pemasukan dan pengeluaran</p>
            </div>
            <div class="chart-legend">
                <span><i class="legend-dot legend-income"></i>Pemasukan</span>
                <span><i class="legend-dot legend-expense"></i>Pengeluaran</span>
            </div>
        </div>
        <div class="chart-body">
            <canvas id="reportMonthlyChart" height="230" role="img" aria-label="Grafik tren pemasukan dan pengeluaran enam bulan" aria-describedby="reportMonthlySummary"></canvas>
        </div>
        <div class="sr-only" id="reportMonthlySummary">
            <table>
                <caption>Tren pemasukan dan pengeluaran enam bulan</caption>
                <thead><tr><th scope="col">Bulan</th><th scope="col">Pemasukan</th><th scope="col">Pengeluaran</th></tr></thead>
                <tbody>
                <c:forEach var="m" items="${monthly}">
                    <tr><th scope="row">${m.label}</th><td>${f:rupiah(m.income)}</td><td>${f:rupiah(m.expense)}</td></tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </article>

    <article class="card">
        <div class="card-head">
            <div>
                <h2>Saldo per dompet</h2>
                <p>Posisi dana saat ini</p>
            </div>
            <a class="link-more" href="${ctx}/app/wallets">Kelola <ui:icon name="arrow-right"/></a>
        </div>
        <div class="wallet-balance-list">
            <c:forEach var="w" items="${wallets}">
                <div class="wallet-balance-row">
                    <span class="wallet-dot lg" style="background:${w.color}"></span>
                    <div><strong>${f:esc(w.name)}</strong><small>${w.type eq 'cash' ? 'Tunai' : (w.type eq 'bank' ? 'Bank' : 'E-Wallet')}</small></div>
                    <span class="amount">${f:rupiah(w.balance)}</span>
                </div>
            </c:forEach>
        </div>
    </article>

    <article class="card span-2">
        <div class="card-head">
            <div>
                <h2>Pengeluaran terbesar</h2>
                <p>5 transaksi dengan nominal tertinggi</p>
            </div>
            <a class="link-more" href="${ctx}/app/transactions?month=${monthKey}&type=expense">Lihat semua <ui:icon name="arrow-right"/></a>
        </div>
        <c:choose>
            <c:when test="${empty topExpenses}">
                <div class="empty-inline"><ui:icon name="inbox"/>Belum ada pengeluaran bulan ini.</div>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="data-table compact-table">
                        <thead><tr><th>Transaksi</th><th>Kategori</th><th>Tanggal</th><th class="right">Nominal</th></tr></thead>
                        <tbody>
                        <c:forEach var="tx" items="${topExpenses}">
                            <tr>
                                <td><strong>${f:esc(empty tx.note ? 'Pengeluaran' : tx.note)}</strong></td>
                                <td><span class="chip" style="--chip:${empty tx.categoryColor ? '#94a3b8' : tx.categoryColor}">${f:esc(tx.categoryName)}</span></td>
                                <td><span class="date-cell">${f:date(tx.transactionDate)}</span></td>
                                <td class="right"><span class="amount expense">-${f:rupiah(tx.amount)}</span></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </article>

    <article class="card">
        <div class="card-head">
            <div>
                <h2>Realisasi budget</h2>
                <p>${budgets.size()} budget bulan ini</p>
            </div>
            <a class="link-more" href="${ctx}/app/budgets?month=${monthKey}">Atur <ui:icon name="arrow-right"/></a>
        </div>
        <div class="budget-mini-list">
            <c:forEach var="b" items="${budgets}">
                <div class="budget-mini">
                    <div class="budget-mini-head">
                        <span class="mini-icon" style="background:${b.categoryColor}1f;color:${b.categoryColor}">
                            <ui:icon name="${empty b.categoryIcon ? 'tag' : b.categoryIcon}"/>
                        </span>
                        <div class="budget-mini-text">
                            <strong>${f:esc(b.categoryName)}</strong>
                            <small>${f:rupiah(b.spent)} / ${f:rupiah(b.amount)}</small>
                        </div>
                        <span class="badge badge-${b.status}">${b.percentage}%</span>
                    </div>
                    <div class="progress"><span class="progress-bar ${b.status}" style="--bar-width:${b.barWidth}"></span></div>
                </div>
            </c:forEach>
            <c:if test="${empty budgets}">
                <div class="empty-inline"><ui:icon name="budget"/>Belum ada budget bulan ini.</div>
            </c:if>
        </div>
    </article>
</div>

<section class="report-note">
    <span class="mini-icon"><ui:icon name="download"/></span>
    <div>
        <strong>Butuh laporan siap cetak?</strong>
        <p>Unduh ringkasan bulan ${f:month(month)} sebagai PDF, atau data mentah sebagai CSV untuk Excel.</p>
    </div>
    <div class="toolbar-actions">
        <a class="btn btn-ghost" href="${ctx}/app/reports/pdf?month=${monthKey}"><ui:icon name="download"/>PDF</a>
        <a class="btn btn-ghost" href="${ctx}/app/reports/export?month=${monthKey}"><ui:icon name="download"/>CSV</a>
    </div>
</section>

<script>window.DOMPETKU_REPORT = ${reportJson};</script>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
