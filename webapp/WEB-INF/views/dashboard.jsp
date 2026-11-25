<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://dompetku.app/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Dashboard"/>
<c:set var="eyebrow" value="Ringkasan keuangan"/>
<c:set var="active" value="dashboard"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<section class="hero-strip">
    <div class="hero-copy">
        <span class="hero-kicker"><span class="pulse-dot"></span>${f:month(month)} &middot; siap dikelola</span>
        <h2>Halo, ${f:esc(u.name)}</h2>
        <p>Ini ringkasan finansialmu bulan ini. Semua angka diperbarui otomatis dari transaksi yang kamu catat.</p>
    </div>
    <div class="hero-actions">
        <a class="btn btn-ghost btn-sm" href="${ctx}/app/dashboard?month=${prevMonthKey}">
            <ui:icon name="chevron-left"/><span>Bulan lalu</span>
        </a>
        <span class="month-pill"><ui:icon name="calendar"/>${f:month(month)}</span>
        <a class="btn btn-ghost btn-sm" href="${ctx}/app/dashboard?month=${nextMonthKey}">
            <span>Bulan depan</span><ui:icon name="chevron-right"/>
        </a>
    </div>
</section>

<c:if test="${empty recent}">
    <c:set var="onboardBudgetDone" value="${not empty budgets ? 1 : 0}"/>
    <c:set var="onboardGoalDone" value="${not empty goals ? 1 : 0}"/>
    <c:set var="onboardDone" value="${onboardBudgetDone + onboardGoalDone}"/>
    <c:set var="onboardPct" value="${onboardDone * 100 / 3}"/>
    <section class="onboarding-card" aria-labelledby="onboardTitle">
        <div class="onboarding-copy">
            <span class="onboarding-icon"><ui:icon name="sparkles"/></span>
            <div>
                <p class="eyebrow">Mulai dari sini</p>
                <h2 id="onboardTitle">Bikin dashboard-mu hidup.</h2>
                <p>Belum ada transaksi. Tiga langkah kecil ini cukup untuk melihat ringkasan pertamamu.</p>
            </div>
        </div>
        <div class="onboarding-meter">
            <div class="progress progress-lg" role="progressbar" aria-label="Progres langkah onboarding"
                 aria-valuemin="0" aria-valuemax="3" aria-valuenow="${onboardDone}"
                 aria-valuetext="${onboardDone} dari 3 langkah selesai">
                <span class="progress-bar" style="--bar-width:${onboardPct}" data-width="${onboardPct}"></span>
            </div>
            <span>${onboardDone}/3 langkah</span>
        </div>
        <ol class="onboarding-steps">
            <li class="is-active">
                <span class="step-index">1</span>
                <span><strong>Catat transaksi</strong><small>Pemasukan atau pengeluaran pertama.</small></span>
                <a class="link-more" href="${ctx}/app/transactions/new">Mulai <ui:icon name="arrow-right"/></a>
            </li>
            <li class="${onboardBudgetDone == 1 ? 'is-done' : ''}">
                <span class="step-index"><c:choose><c:when test="${onboardBudgetDone == 1}"><ui:icon name="check"/></c:when><c:otherwise>2</c:otherwise></c:choose></span>
                <span><strong>Atur budget</strong><small>Beri batas yang masuk akal.</small></span>
                <a class="link-more" href="${ctx}/app/budgets/new">Atur <ui:icon name="arrow-right"/></a>
            </li>
            <li class="${onboardGoalDone == 1 ? 'is-done' : ''}">
                <span class="step-index"><c:choose><c:when test="${onboardGoalDone == 1}"><ui:icon name="check"/></c:when><c:otherwise>3</c:otherwise></c:choose></span>
                <span><strong>Buat target</strong><small>Mulai dari satu tujuan kecil.</small></span>
                <a class="link-more" href="${ctx}/app/goals/new">Buat <ui:icon name="arrow-right"/></a>
            </li>
        </ol>
    </section>
</c:if>

<section class="stat-grid">
    <article class="stat-card stat-emerald">
        <div class="stat-top">
            <span class="stat-icon"><ui:icon name="wallet"/></span>
            <span class="stat-label">Total saldo</span>
            <span class="stat-chip"><ui:icon name="check"/>live</span>
        </div>
        <strong class="stat-value" data-countup="${totalBalance}" data-format="rupiah">${f:rupiah(totalBalance)}</strong>
        <div class="stat-bottom">
            <span>${wallets.size()} dompet aktif</span>
            <div class="mini-spark">
                <svg viewBox="0 0 90 24" preserveAspectRatio="none">
                    <path d="M0 18 L12 15 L24 17 L36 10 L48 13 L60 7 L72 9 L90 3" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
            </div>
        </div>
    </article>

    <article class="stat-card stat-blue">
        <div class="stat-top">
            <span class="stat-icon"><ui:icon name="arrow-down"/></span>
            <span class="stat-label">Pemasukan</span>
            <span class="stat-chip ${incomeChange >= 0 ? 'up' : 'down'}">
                <ui:icon name="${incomeChange >= 0 ? 'arrow-up' : 'arrow-down'}"/>${incomeChange >= 0 ? '+' : ''}${incomeChange}%
            </span>
        </div>
        <strong class="stat-value" data-countup="${income}" data-format="rupiah">${f:rupiah(income)}</strong>
        <div class="stat-bottom">
            <span>${f:month(month)}</span>
            <span class="stat-note">dibanding bulan lalu</span>
        </div>
    </article>

    <article class="stat-card stat-rose">
        <div class="stat-top">
            <span class="stat-icon"><ui:icon name="arrow-up"/></span>
            <span class="stat-label">Pengeluaran</span>
            <span class="stat-chip ${expenseChange <= 0 ? 'up' : 'down'}">
                <ui:icon name="${expenseChange <= 0 ? 'arrow-down' : 'arrow-up'}"/>${expenseChange >= 0 ? '+' : ''}${expenseChange}%
            </span>
        </div>
        <strong class="stat-value" data-countup="${expense}" data-format="rupiah">${f:rupiah(expense)}</strong>
        <div class="stat-bottom">
            <span>${f:month(month)}</span>
            <span class="stat-note">dibanding bulan lalu</span>
        </div>
    </article>

    <article class="stat-card stat-violet">
        <div class="stat-top">
            <span class="stat-icon"><ui:icon name="sparkles"/></span>
            <span class="stat-label">Rasio tabungan</span>
            <c:choose>
                <c:when test="${savingsRate >= 20}"><span class="stat-chip up"><ui:icon name="check"/>sehat</span></c:when>
                <c:when test="${savingsRate >= 0}"><span class="stat-chip"><ui:icon name="clock"/>cukup</span></c:when>
                <c:otherwise><span class="stat-chip down"><ui:icon name="alert"/>defisit</span></c:otherwise>
            </c:choose>
        </div>
        <strong class="stat-value" data-countup="${savingsRate}" data-format="percent">${savingsRate}%</strong>
        <div class="stat-bottom">
            <span>Selisih ${f:rupiah(net)}</span>
            <span class="stat-note">dari pemasukan</span>
        </div>
    </article>
</section>

<c:if test="${not empty recent}">
    <section class="insight-strip" aria-labelledby="insightTitle">
        <span class="insight-badge"><ui:icon name="sparkles"/></span>
        <div class="insight-copy">
            <p class="eyebrow">Insight bulan ini</p>
            <h2 id="insightTitle">
                <c:choose>
                    <c:when test="${not empty byCategory}"><strong>${f:esc(byCategory[0].name)}</strong> jadi pengeluaran terbesar.</c:when>
                    <c:otherwise>Belum ada pengeluaran bulan ini.</c:otherwise>
                </c:choose>
            </h2>
            <p>
                <c:choose>
                    <c:when test="${not empty byCategory}">Menyumbang ${byCategory[0].percentage}% dari total pengeluaranmu bulan ini.</c:when>
                    <c:otherwise>Catat transaksi agar insight mulai muncul.</c:otherwise>
                </c:choose>
            </p>
        </div>
        <div class="insight-chips">
            <span class="insight-chip ${expenseChange <= 0 ? 'good' : 'bad'}">
                <ui:icon name="${expenseChange <= 0 ? 'arrow-down' : 'arrow-up'}"/>
                <c:choose>
                    <c:when test="${expenseChange == 0}">Pengeluaran stabil</c:when>
                    <c:otherwise>Pengeluaran ${expenseChange > 0 ? '+' : ''}${expenseChange}%</c:otherwise>
                </c:choose>
            </span>
            <span class="insight-chip ${empty budgets ? 'neutral' : (overBudget > 0 ? 'bad' : 'good')}">
                <ui:icon name="${overBudget > 0 ? 'alert' : 'check'}"/>
                <c:choose>
                    <c:when test="${empty budgets}">Belum ada budget</c:when>
                    <c:when test="${overBudget > 0}">${overBudget} kategori lewat budget</c:when>
                    <c:otherwise>Budget aman</c:otherwise>
                </c:choose>
            </span>
        </div>
        <a class="link-more" href="${ctx}/app/reports?month=${monthKey}">Buka laporan <ui:icon name="arrow-right"/></a>
    </section>
</c:if>

<section class="dashboard-grid">
    <article class="card chart-card span-2">
        <div class="card-head">
            <div>
                <h2>Arus kas 6 bulan</h2>
                <p>Perbandingan pemasukan dan pengeluaran</p>
            </div>
            <div class="chart-legend">
                <button type="button" class="chart-legend-item" data-chart="cashflowChart" data-dataset="0" aria-pressed="true">
                    <i class="legend-dot legend-income"></i>Pemasukan
                </button>
                <button type="button" class="chart-legend-item" data-chart="cashflowChart" data-dataset="1" aria-pressed="true">
                    <i class="legend-dot legend-expense"></i>Pengeluaran
                </button>
            </div>
        </div>
        <div class="chart-body${cashflowHasData ? '' : ' is-empty'}" data-drilldown="${cashflowHasData}">
            <canvas id="cashflowChart" height="250" role="img" aria-label="Grafik arus kas enam bulan: pemasukan dan pengeluaran" aria-describedby="cashflowSummary"></canvas>
            <c:if test="${not cashflowHasData}">
                <div class="chart-empty">
                    <span><ui:icon name="transactions"/></span>
                    <strong>Belum ada arus kas</strong>
                    <p>Catat transaksi pertamamu untuk melihat grafik di sini.</p>
                    <a class="btn btn-primary btn-sm" href="${ctx}/app/transactions/new"><ui:icon name="plus"/>Tambah transaksi</a>
                </div>
            </c:if>
        </div>
        <c:if test="${cashflowHasData}">
            <p class="chart-hint">Klik batang bulan atau pilih tautan bulan untuk melihat transaksi.</p>
            <nav class="chart-months" aria-label="Transaksi per bulan">
                <c:forEach var="m" items="${monthly}">
                    <a href="${ctx}/app/transactions?month=${m.key}">${m.label}</a>
                </c:forEach>
            </nav>
        </c:if>
        <div class="sr-only" id="cashflowSummary">
            <table>
                <caption>Arus kas enam bulan terakhir</caption>
                <thead><tr><th scope="col">Bulan</th><th scope="col">Pemasukan</th><th scope="col">Pengeluaran</th></tr></thead>
                <tbody>
                <c:forEach var="m" items="${monthly}">
                    <tr><th scope="row">${m.label}</th><td>${f:rupiah(m.income)}</td><td>${f:rupiah(m.expense)}</td></tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </article>

    <article class="card chart-card">
        <div class="card-head">
            <div>
                <h2>Pengeluaran per kategori</h2>
                <p>${f:month(month)}</p>
            </div>
            <a class="link-more" href="${ctx}/app/reports?month=${monthKey}">Detail <ui:icon name="arrow-right"/></a>
        </div>
        <div class="donut-wrap">
            <canvas id="categoryChart" height="210" role="img" aria-label="Diagram donat pengeluaran per kategori" aria-describedby="categoryLegend"></canvas>
            <div class="donut-center">
                <small>Total</small>
                <strong>${f:compact(expense)}</strong>
            </div>
        </div>
        <div class="donut-legend" id="categoryLegend">
            <c:forEach var="cat" items="${byCategory}" varStatus="loop" end="4">
                <div class="donut-legend-row">
                    <span class="legend-dot" style="background:${cat.color}"></span>
                    <span class="donut-legend-name">${f:esc(cat.name)}</span>
                    <span class="donut-legend-value">${f:compact(cat.total)}</span>
                    <span class="donut-legend-pct">${cat.percentage}%</span>
                </div>
            </c:forEach>
            <c:if test="${empty byCategory}">
                <div class="empty-inline"><ui:icon name="inbox"/>Belum ada pengeluaran bulan ini.</div>
            </c:if>
        </div>
    </article>

    <article class="card chart-card span-2">
        <div class="card-head">
            <div>
                <h2>Arus kas harian</h2>
                <p>Ritme pemasukan dan pengeluaran sepanjang ${f:month(month)}</p>
            </div>
            <span class="soft-badge"><ui:icon name="calendar"/>${f:month(month)}</span>
        </div>
        <div class="chart-body${dailyHasData ? '' : ' is-empty'}">
            <canvas id="dailyChart" height="220" role="img" aria-label="Grafik arus kas harian bulan ini" aria-describedby="dailySummary"></canvas>
            <c:if test="${not dailyHasData}">
                <div class="chart-empty">
                    <span><ui:icon name="calendar"/></span>
                    <strong>Belum ada transaksi bulan ini</strong>
                    <p>Mulai catat pemasukan atau pengeluaran untuk melihat ritmenya.</p>
                    <a class="btn btn-primary btn-sm" href="${ctx}/app/transactions/new"><ui:icon name="plus"/>Tambah transaksi</a>
                </div>
            </c:if>
        </div>
        <div class="sr-only" id="dailySummary">
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
                <h2>Budget bulan ini</h2>
                <p>${overBudget} kategori melewati batas</p>
            </div>
            <a class="link-more" href="${ctx}/app/budgets?month=${monthKey}">Kelola <ui:icon name="arrow-right"/></a>
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
                    <div class="progress">
                        <span class="progress-bar ${b.status}" style="--bar-width:${b.barWidth}" data-width="${b.barWidth}"></span>
                    </div>
                </div>
            </c:forEach>
            <c:if test="${empty budgets}">
                <div class="empty-inline"><ui:icon name="budget"/>Belum ada budget. <a href="${ctx}/app/budgets/new">Buat sekarang</a></div>
            </c:if>
        </div>
    </article>

    <article class="card span-2">
        <div class="card-head">
            <div>
                <h2>Transaksi terbaru</h2>
                <p>6 aktivitas terakhir yang kamu catat</p>
            </div>
            <a class="link-more" href="${ctx}/app/transactions">Lihat semua <ui:icon name="arrow-right"/></a>
        </div>
        <div class="tx-list">
            <c:forEach var="tx" items="${recent}">
                <a class="tx-row" href="${ctx}/app/transactions/edit/${tx.id}">
                    <span class="tx-icon" style="background:${empty tx.categoryColor ? '#94a3b8' : tx.categoryColor}1f;color:${empty tx.categoryColor ? '#94a3b8' : tx.categoryColor}">
                        <ui:icon name="${empty tx.categoryIcon ? 'tag' : tx.categoryIcon}"/>
                    </span>
                    <span class="tx-main">
                        <strong>${f:esc(empty tx.note ? tx.categoryName : tx.note)}</strong>
                        <small>${f:esc(empty tx.categoryName ? 'Tanpa kategori' : tx.categoryName)} &middot; ${f:esc(tx.walletName)}</small>
                    </span>
                    <span class="tx-date">${f:date(tx.transactionDate)}</span>
                    <span class="amount ${tx.type}">${tx.type eq 'income' ? '+' : '-'}${f:rupiah(tx.amount)}</span>
                </a>
            </c:forEach>
            <c:if test="${empty recent}">
                <div class="empty-state compact">
                    <span class="empty-icon"><ui:icon name="transactions"/></span>
                    <strong>Belum ada transaksi</strong>
                    <p>Mulai catat pemasukan atau pengeluaran pertamamu.</p>
                    <a class="btn btn-primary btn-sm" href="${ctx}/app/transactions/new"><ui:icon name="plus"/>Tambah transaksi</a>
                </div>
            </c:if>
        </div>
    </article>

    <article class="card">
        <div class="card-head">
            <div>
                <h2>Target tabungan</h2>
                <p>${f:compact(goalSaved)} dari ${f:compact(goalTotal)}</p>
            </div>
            <a class="link-more" href="${ctx}/app/goals">Semua <ui:icon name="arrow-right"/></a>
        </div>
        <div class="goal-mini-list">
            <c:forEach var="g" items="${goals}">
                <a class="goal-mini" href="${ctx}/app/goals/view/${g.id}">
                    <span class="goal-ring" style="--p:${g.progressPercent};--c:${g.color}" data-progress="${g.progressPercent}"><span>${g.progressPercent}%</span></span>
                    <span class="goal-mini-text">
                        <strong>${f:esc(g.name)}</strong>
                        <small>${f:compact(g.savedAmount)} dari ${f:compact(g.targetAmount)}</small>
                    </span>
                    <ui:icon name="chevron-right"/>
                </a>
            </c:forEach>
            <c:if test="${empty goals}">
                <div class="empty-inline"><ui:icon name="goals"/>Belum ada target. <a href="${ctx}/app/goals/new">Buat target</a></div>
            </c:if>
        </div>
    </article>

    <article class="card span-3">
        <div class="card-head">
            <div>
                <h2>Jatuh tempo &amp; komitmen</h2>
                <p>Utang ${f:compact(debtPayable)} &middot; piutang ${f:compact(debtReceivable)}</p>
            </div>
            <a class="link-more" href="${ctx}/app/debts">Kelola <ui:icon name="arrow-right"/></a>
        </div>
        <div class="debt-list">
            <c:forEach var="d" items="${debts}">
                <div class="debt-row">
                    <span class="debt-avatar">${f:initials(d.counterpart)}</span>
                    <div class="debt-main">
                        <strong>${f:esc(d.counterpart)}</strong>
                        <small>${d.type eq 'payable' ? 'Harus dibayar' : 'Akan diterima'} &middot; ${empty d.dueDate ? 'tanpa tenggat' : f:date(d.dueDate)}</small>
                    </div>
                    <span class="badge badge-${d.status}">
                        <c:choose>
                            <c:when test="${d.status eq 'paid'}">Lunas</c:when>
                            <c:when test="${d.status eq 'overdue'}">Terlambat</c:when>
                            <c:when test="${d.status eq 'partial'}">Sebagian</c:when>
                            <c:otherwise>Belum bayar</c:otherwise>
                        </c:choose>
                    </span>
                    <div class="debt-progress">
                        <div class="progress"><span class="progress-bar ${d.status eq 'overdue' ? 'over' : ''}" style="--bar-width:${d.progressPercent}" data-width="${d.progressPercent}"></span></div>
                        <small>${d.progressPercent}% terbayar</small>
                    </div>
                    <strong class="amount ${d.type eq 'payable' ? 'expense' : 'income'}">${f:rupiah(d.remaining)}</strong>
                </div>
            </c:forEach>
            <c:if test="${empty debts}">
                <div class="empty-inline"><ui:icon name="check"/>Tidak ada komitmen yang menunggu. Bersih!</div>
            </c:if>
        </div>
    </article>
</section>

<script>window.DOMPETKU_CTX = '${ctx}'; window.DOMPETKU_CHART = ${chartJson};</script>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
