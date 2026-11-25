<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!doctype html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="color-scheme" content="dark">
    <meta name="text-scale" content="scale">
    <meta name="description" content="Dompetku mencatat pemasukan, pengeluaran, budget, dan target tabungan dalam satu tampilan yang mudah dipahami.">
    <title>Dompetku &mdash; Keuangan pribadi yang lebih jelas</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="icon" type="image/svg+xml" href="${ctx}/assets/img/favicon.svg">
    <link rel="stylesheet" href="${ctx}/assets/css/app.css?v=20261119-31">
    <link rel="stylesheet" href="${ctx}/assets/css/landing.css?v=20261119-24">
</head>
<body class="landing-body">
<a class="skip-link" href="#mainContent">Lewati ke konten</a>

<header class="landing-nav nav-floating">
    <div class="landing-nav-inner">
        <a class="brand landing-brand" href="${ctx}/" aria-label="Dompetku, kembali ke halaman awal">
            <span class="brand-mark">
                <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                    <path d="M3 8.5A3.5 3.5 0 0 1 6.5 5H18a2 2 0 0 1 2 2v1.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>
                    <rect x="3" y="7.5" width="18" height="12.5" rx="3.2" stroke="currentColor" stroke-width="1.7"/>
                    <path d="M15.5 14.2h2.2" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/>
                    <path d="M7 7.5V6.2A2.2 2.2 0 0 1 9.2 4l6.6 1.3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
            </span>
            <span class="brand-text"><strong>Dompetku</strong></span>
        </a>

        <nav class="landing-nav-links" aria-label="Navigasi utama">
            <a href="#alur" data-nav="alur">Alur</a>
            <a href="#coba" data-nav="coba">Coba</a>
            <a href="#fitur" data-nav="fitur">Fitur</a>
            <a href="#privasi" data-nav="privasi">Privasi</a>
        </nav>

        <div class="landing-nav-actions">
            <a class="btn btn-ghost landing-login" href="${ctx}/login">Masuk</a>
            <a class="btn btn-primary landing-register" href="${ctx}/register">Daftar</a>
        </div>
    </div>
</header>

<main id="mainContent" class="min-main">
    <section class="min-hero" id="alur" aria-labelledby="heroTitle">
        <div class="min-copy">
            <h1 id="heroTitle">Semua uangmu,<br>punya arah.</h1>
            <p>Catat, atur, dan lihat ke mana uangmu pergi dalam satu tampilan.</p>

            <div class="landing-actions">
                <a class="btn btn-primary btn-lg" href="${ctx}/register">Mulai sekarang</a>
                <a class="btn min-btn-soft btn-lg" href="#coba">Coba sekarang</a>
            </div>
        </div>

        <aside class="min-chart" aria-labelledby="cashflowTitle">
            <div class="min-chart-head">
                <div>
                    <p class="min-kicker">Arus kas</p>
                    <h2 id="cashflowTitle">6 bulan terakhir</h2>
                </div>
                <div class="min-chart-controls">
                    <div class="min-chart-switch" role="group" aria-label="Pilih data arus kas">
                        <button type="button" class="is-active" data-series="income" aria-pressed="true">Masuk</button>
                        <button type="button" data-series="expense" aria-pressed="false">Keluar</button>
                    </div>
                    <div class="min-chart-range" role="group" aria-label="Pilih rentang waktu">
                        <button type="button" class="is-active" data-range="6" aria-pressed="true">6 bln</button>
                        <button type="button" data-range="12" aria-pressed="false">12 bln</button>
                    </div>
                </div>
            </div>

            <div class="min-chart-summary">
                <span id="chartLabel">Total pemasukan</span>
                <strong id="chartTotal">Rp41,7jt</strong>
                <small id="chartHint">+12% dibanding 6 bulan sebelumnya</small>
            </div>

            <div class="min-chart-plot" id="chartPlot">
                <svg id="cashflowChart" viewBox="0 0 420 168" role="img" tabindex="0"
                     aria-label="Grafik arus kas enam bulan terakhir. Gunakan tombol panah untuk melihat nilai per bulan.">
                    <defs>
                        <linearGradient id="chartAreaFill" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="0%" stop-color="#5eead4" stop-opacity=".16"/>
                            <stop offset="100%" stop-color="#5eead4" stop-opacity="0"/>
                        </linearGradient>
                    </defs>
                    <g class="min-chart-grid" aria-hidden="true">
                        <line x1="10" y1="14" x2="410" y2="14"/>
                        <line x1="10" y1="56" x2="410" y2="56"/>
                        <line x1="10" y1="98" x2="410" y2="98"/>
                        <line x1="10" y1="140" x2="410" y2="140"/>
                    </g>
                    <path id="chartComparePath" class="min-chart-compare" d=""/>
                    <path id="chartAreaPath" class="min-chart-area" fill="url(#chartAreaFill)" d=""/>
                    <path id="chartLinePath" class="min-chart-line" d=""/>
                    <g id="chartPoints"></g>
                    <line id="chartHoverLine" class="min-chart-hover-line" y1="14" y2="140" x1="0" x2="0" hidden/>
                </svg>
                <div class="min-chart-tooltip" id="chartTooltip" role="status" aria-live="polite" hidden>
                    <span id="chartTooltipMonth"></span>
                    <strong id="chartTooltipValue"></strong>
                </div>
            </div>

            <div class="min-chart-axis" id="chartAxis" aria-hidden="true">
                <span>Jun</span><span>Jul</span><span>Agu</span><span>Sep</span><span>Okt</span><span>Nov</span>
            </div>
            <div class="min-chart-legend" aria-hidden="true">
                <span><i class="is-current"></i>Sekarang</span>
                <span><i class="is-previous"></i>Sebelumnya</span>
            </div>
        </aside>
    </section>

    <section class="min-section min-demo" id="coba" aria-labelledby="demoTitle">
        <div class="min-demo-copy">
            <h2 id="demoTitle">Coba Dompetku.</h2>
        </div>
        <div class="min-demo-widget">
            <div class="min-demo-tabs" role="tablist" aria-label="Coba fitur Dompetku">
                <button type="button" class="is-active" id="demo-tab-catat" data-pane="catat" role="tab" aria-selected="true" aria-controls="demo-panel-catat">Catat</button>
                <button type="button" id="demo-tab-atur" data-pane="atur" role="tab" aria-selected="false" aria-controls="demo-panel-atur" tabindex="-1">Atur</button>
                <button type="button" id="demo-tab-baca" data-pane="baca" role="tab" aria-selected="false" aria-controls="demo-panel-baca" tabindex="-1">Baca</button>
            </div>

            <div class="min-demo-pane is-active" id="demo-panel-catat" role="tabpanel" aria-labelledby="demo-tab-catat">
                <div class="min-demo-row"><span>Kategori</span><strong id="catatCategory">Makan</strong></div>
                <div class="min-demo-chips" data-category-group>
                    <button type="button" class="is-active" data-category="Makan">Makan</button>
                    <button type="button" data-category="Transport">Transport</button>
                    <button type="button" data-category="Hiburan">Hiburan</button>
                </div>
                <div class="min-demo-amount"><span>Nominal</span><strong id="catatAmount">Rp250.000</strong></div>
                <div class="min-demo-chips" data-amount-group>
                    <button type="button" data-amount="50000">50rb</button>
                    <button type="button" class="is-active" data-amount="250000">250rb</button>
                    <button type="button" data-amount="500000">500rb</button>
                </div>
            </div>

            <div class="min-demo-pane" id="demo-panel-atur" role="tabpanel" aria-labelledby="demo-tab-atur" hidden>
                <div class="min-demo-row"><span>Budget makan</span><strong>Rp800.000</strong></div>
                <div class="min-demo-amount"><span>Terpakai</span><strong id="demoAmount">Rp250.000</strong></div>
                <input id="demoRange" type="range" min="0" max="800000" step="10000" value="250000" aria-label="Nominal pengeluaran">
                <div class="min-demo-row"><span>Sisa budget</span><strong id="demoRemaining">Rp550.000</strong></div>
                <div class="min-demo-meter"><i id="demoMeter" style="transform:scaleX(.6875)"></i></div>
            </div>

            <div class="min-demo-pane" id="demo-panel-baca" role="tabpanel" aria-labelledby="demo-tab-baca" hidden>
                <div class="min-demo-row"><span>Status</span><strong id="demoStatus">Aman</strong></div>
                <div class="min-demo-amount"><span>Terpakai</span><strong id="demoReadAmount">Rp250.000</strong></div>
                <div class="min-demo-row"><span>Sisa budget</span><strong id="demoReadRemaining">Rp550.000</strong></div>
                <div class="min-demo-meter"><i id="demoReadMeter" style="transform:scaleX(.6875)"></i></div>
            </div>
        </div>
    </section>

    <section class="min-section" id="fitur" aria-labelledby="fiturTitle">
        <div class="min-section-head">
            <h2 id="fiturTitle">Ke mana uangmu pergi, terlihat jelas.</h2>
        </div>

        <div class="min-features">
            <article class="min-feature">
                <span class="min-feature-icon"><ui:icon name="transactions"/></span>
                <div><h3>Catat transaksi dengan cepat</h3><p>Pemasukan, pengeluaran, dan transfer dalam satu riwayat.</p></div>
            </article>

            <article class="min-feature">
                <span class="min-feature-icon"><ui:icon name="budget"/></span>
                <div><h3>Budget per kategori</h3><p>Batas pengeluaran per kategori.</p></div>
            </article>

            <article class="min-feature">
                <span class="min-feature-icon"><ui:icon name="goals"/></span>
                <div><h3>Target tabungan yang terukur</h3><p>Progres tabungan, utang, dan transaksi berulang.</p></div>
            </article>

            <article class="min-feature">
                <span class="min-feature-icon"><ui:icon name="reports"/></span>
                <div><h3>Laporan arus kas</h3><p>Arus kas enam bulan per kategori.</p></div>
            </article>
        </div>
    </section>

    <section class="min-section" id="privasi" aria-labelledby="privacyTitle">
        <div class="min-privacy">
            <div>
                <h2 id="privacyTitle">Data uangmu tetap milikmu.</h2>
            </div>
            <ul>
                <li><span><ui:icon name="shield"/></span><div><strong>Password di-hash sebelum disimpan.</strong></div></li>
                <li><span><ui:icon name="lock"/></span><div><strong>Workspace hanya bisa diakses setelah login.</strong></div></li>
                <li><span><ui:icon name="check"/></span><div><strong>Data setiap akun terpisah.</strong></div></li>
            </ul>
        </div>
    </section>

    <section class="min-cta" aria-labelledby="ctaTitle">
        <div>
            <h2 id="ctaTitle">Beri uangmu arah yang jelas.</h2>
        </div>
        <div class="min-cta-side">
            <div class="landing-actions">
                <a class="btn btn-primary btn-lg" href="${ctx}/register">Buat akun</a>
                <a class="btn min-btn-soft btn-lg" href="${ctx}/login">Masuk</a>
            </div>
        </div>
    </section>
</main>

<footer class="landing-footer">
    <span class="landing-footer-links">
        <a href="#fitur">Fitur</a>
        <a href="#privasi">Privasi</a>
        <a href="${ctx}/login">Masuk</a>
    </span>
</footer>

<script src="${ctx}/assets/js/app.js?v=20261119-29"></script>
<script src="${ctx}/assets/js/landing.js?v=20261119-13"></script>
</body>
</html>
