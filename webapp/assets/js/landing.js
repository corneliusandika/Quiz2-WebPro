(function () {
    'use strict';

    var reducedMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    var rupiah = new Intl.NumberFormat('id-ID', {
        style: 'currency', currency: 'IDR', maximumFractionDigits: 0
    });

    function formatRupiah(value) {
        return rupiah.format(value).replace(/\u00a0/g, ' ').replace(/^Rp\s+/, 'Rp');
    }

    function compact(value) {
        if (value >= 1000000) {
            return 'Rp' + (value / 1000000).toLocaleString('id-ID', {
                maximumFractionDigits: 1
            }) + 'jt';
        }
        return formatRupiah(value);
    }

    /* --------------------------------------------------------------- nav active */
    var navLinks = Array.prototype.slice.call(document.querySelectorAll('.landing-nav-links a[data-nav]'));
    var sections = navLinks.map(function (link) {
        return document.getElementById(link.getAttribute('data-nav'));
    }).filter(Boolean);

    if (navLinks.length && sections.length && 'IntersectionObserver' in window) {
        var navObserver = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (!entry.isIntersecting) { return; }
                navLinks.forEach(function (link) {
                    var active = link.getAttribute('data-nav') === entry.target.id;
                    link.classList.toggle('is-active', active);
                    if (active) { link.setAttribute('aria-current', 'location'); }
                    else { link.removeAttribute('aria-current'); }
                });
            });
        }, { rootMargin: '-24% 0px -62% 0px', threshold: 0 });
        sections.forEach(function (section) { navObserver.observe(section); });
    }

    /* -------------------------------------------------------- cashflow chart */
    (function initCashflowChart() {
        var svg = document.getElementById('cashflowChart');
        if (!svg) { return; }

        var linePath = document.getElementById('chartLinePath');
        var comparePath = document.getElementById('chartComparePath');
        var areaPath = document.getElementById('chartAreaPath');
        var pointsGroup = document.getElementById('chartPoints');
        var hoverLine = document.getElementById('chartHoverLine');
        var tooltip = document.getElementById('chartTooltip');
        var tooltipMonth = document.getElementById('chartTooltipMonth');
        var tooltipValue = document.getElementById('chartTooltipValue');
        var label = document.getElementById('chartLabel');
        var total = document.getElementById('chartTotal');
        var hint = document.getElementById('chartHint');
        var title = document.getElementById('cashflowTitle');
        var axis = document.getElementById('chartAxis');
        var seriesButtons = Array.prototype.slice.call(document.querySelectorAll('.min-chart-switch button'));
        var rangeButtons = Array.prototype.slice.call(document.querySelectorAll('.min-chart-range button'));

        var labels = {
            6: ['Jun', 'Jul', 'Agu', 'Sep', 'Okt', 'Nov'],
            12: ['Des', 'Jan', 'Feb', 'Mar', 'Apr', 'Mei', 'Jun', 'Jul', 'Agu', 'Sep', 'Okt', 'Nov']
        };

        var data = {
            income: {
                label: 'Total pemasukan',
                max: 9000000,
                hint: {
                    6: '+12% dibanding 6 bulan sebelumnya',
                    12: '+9% dibanding 12 bulan sebelumnya'
                },
                six: [6200000, 5400000, 6800000, 7100000, 7800000, 8400000],
                twelve: [5200000, 4800000, 5600000, 5900000, 6100000, 5800000, 6200000, 5400000, 6800000, 7100000, 7800000, 8400000],
                previous: {
                    6: [5200000, 4800000, 5600000, 5900000, 6100000, 5800000],
                    12: [4600000, 4400000, 4700000, 4900000, 5000000, 4800000, 5200000, 4800000, 5600000, 5900000, 6100000, 5800000]
                }
            },
            expense: {
                label: 'Total pengeluaran',
                max: 4800000,
                hint: {
                    6: '-4% dibanding 6 bulan sebelumnya',
                    12: '-2% dibanding 12 bulan sebelumnya'
                },
                six: [3100000, 3400000, 2900000, 3800000, 3200000, 4280000],
                twelve: [2700000, 2500000, 2900000, 3000000, 3300000, 2800000, 3100000, 3400000, 2900000, 3800000, 3200000, 4280000],
                previous: {
                    6: [2700000, 2500000, 2900000, 3000000, 3300000, 2800000],
                    12: [2500000, 2400000, 2600000, 2700000, 3000000, 2800000, 2700000, 2500000, 2900000, 3000000, 3300000, 2800000]
                }
            }
        };

        var seriesKey = 'income';
        var range = 6;
        var activeIndex = null;
        var pointNodes = [];
        var lastTotal = 0;
        var totalToken = 0;
        var W = 420;
        var H = 168;
        var X0 = 10;
        var X1 = 410;
        var Y0 = 14;
        var Y1 = 140;

        function currentValues() {
            return data[seriesKey][range === 6 ? 'six' : 'twelve'];
        }

        function currentPrevious() {
            return data[seriesKey].previous[range];
        }

        function xFor(index, count) {
            return X0 + (index * (X1 - X0) / (count - 1));
        }

        function yFor(value, max) {
            return Y0 + ((1 - (value / max)) * (Y1 - Y0));
        }

        function buildPath(values, max) {
            return values.map(function (value, index) {
                return (index === 0 ? 'M' : 'L') + xFor(index, values.length).toFixed(2) + ' ' + yFor(value, max).toFixed(2);
            }).join(' ');
        }

        function buildArea(values, max) {
            var points = values.map(function (value, index) {
                return xFor(index, values.length).toFixed(2) + ' ' + yFor(value, max).toFixed(2);
            });
            return 'M' + X0 + ' ' + Y1 + ' L' + points.join(' L') + ' L' + X1 + ' ' + Y1 + ' Z';
        }

        function hideTooltip() {
            tooltip.hidden = true;
            hoverLine.hidden = true;
            pointNodes.forEach(function (point) { point.classList.remove('is-active'); });
            activeIndex = null;
        }

        function showPoint(index) {
            var series = data[seriesKey];
            var values = currentValues();
            var value = values[index];
            var x = xFor(index, values.length);
            var y = yFor(value, series.max);

            activeIndex = index;
            pointNodes.forEach(function (point, pointIndex) {
                point.classList.toggle('is-active', pointIndex === index);
            });

            hoverLine.hidden = false;
            hoverLine.setAttribute('x1', x);
            hoverLine.setAttribute('x2', x);

            tooltipMonth.textContent = labels[range][index];
            tooltipValue.textContent = compact(value);
            tooltip.style.left = (x / W * 100) + '%';
            tooltip.style.top = (y / H * 100) + '%';
            tooltip.style.transform = index === 0
                ? 'translate(0, -130%)'
                : index === values.length - 1
                    ? 'translate(-100%, -130%)'
                    : 'translate(-50%, -130%)';
            tooltip.hidden = false;
        }

        function drawLine() {
            if (reducedMotion) {
                linePath.style.strokeDasharray = 'none';
                linePath.style.strokeDashoffset = '0';
                return;
            }
            var length = linePath.getTotalLength();
            linePath.style.transition = 'none';
            linePath.style.strokeDasharray = length;
            linePath.style.strokeDashoffset = length;
            linePath.getBoundingClientRect();
            linePath.style.transition = 'stroke-dashoffset 650ms cubic-bezier(.2, .8, .2, 1)';
            linePath.style.strokeDashoffset = '0';
        }

        function animateTotal(target) {
            if (reducedMotion) {
                total.textContent = compact(target);
                lastTotal = target;
                return;
            }
            var from = lastTotal;
            lastTotal = target;
            var token = ++totalToken;
            var started = null;
            var duration = 480;

            function frame(now) {
                if (token !== totalToken) { return; }
                if (started === null) { started = now; }
                var progress = Math.min(1, (now - started) / duration);
                var eased = 1 - Math.pow(1 - progress, 3);
                total.textContent = compact(from + ((target - from) * eased));
                if (progress < 1) { requestAnimationFrame(frame); }
                else { total.textContent = compact(target); }
            }

            requestAnimationFrame(frame);
        }

        function render(key, nextRange) {
            seriesKey = key;
            range = nextRange;

            var series = data[seriesKey];
            var values = currentValues();
            var previous = currentPrevious();

            linePath.setAttribute('d', buildPath(values, series.max));
            comparePath.setAttribute('d', buildPath(previous, series.max));
            areaPath.setAttribute('d', buildArea(values, series.max));

            pointsGroup.textContent = '';
            pointNodes = values.map(function (value, index) {
                var point = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
                point.setAttribute('class', 'min-chart-point');
                point.setAttribute('cx', xFor(index, values.length));
                point.setAttribute('cy', yFor(value, series.max));
                point.setAttribute('r', range === 12 ? '2.8' : '3.25');
                point.style.animationDelay = (index * 24) + 'ms';
                pointsGroup.appendChild(point);
                return point;
            });

            axis.textContent = '';
            labels[range].forEach(function (month) {
                var span = document.createElement('span');
                span.textContent = month;
                axis.appendChild(span);
            });

            label.textContent = series.label;
            hint.textContent = series.hint[range];
            title.textContent = range === 6 ? '6 bulan terakhir' : '12 bulan terakhir';
            svg.setAttribute('aria-label', 'Grafik ' + series.label.toLowerCase() + ' ' + range + ' bulan terakhir. Gunakan tombol panah untuk melihat nilai per bulan.');

            seriesButtons.forEach(function (button) {
                var active = button.getAttribute('data-series') === key;
                button.classList.toggle('is-active', active);
                button.setAttribute('aria-pressed', active ? 'true' : 'false');
            });
            rangeButtons.forEach(function (button) {
                var active = Number(button.getAttribute('data-range')) === range;
                button.classList.toggle('is-active', active);
                button.setAttribute('aria-pressed', active ? 'true' : 'false');
            });

            animateTotal(values.reduce(function (sum, value) { return sum + value; }, 0));
            hideTooltip();
            drawLine();
        }

        seriesButtons.forEach(function (button) {
            button.addEventListener('click', function () {
                render(button.getAttribute('data-series'), range);
            });
        });
        rangeButtons.forEach(function (button) {
            button.addEventListener('click', function () {
                render(seriesKey, Number(button.getAttribute('data-range')));
            });
        });

        svg.addEventListener('pointermove', function (event) {
            var values = currentValues();
            var rect = svg.getBoundingClientRect();
            var x = ((event.clientX - rect.left) / rect.width) * W;
            var step = (X1 - X0) / (values.length - 1);
            var index = Math.round((x - X0) / step);
            index = Math.max(0, Math.min(values.length - 1, index));
            showPoint(index);
        });
        svg.addEventListener('pointerleave', function () {
            if (document.activeElement !== svg) { hideTooltip(); }
        });
        svg.addEventListener('focus', function () { showPoint(currentValues().length - 1); });
        svg.addEventListener('blur', hideTooltip);
        svg.addEventListener('keydown', function (event) {
            var count = currentValues().length;
            var next = activeIndex === null ? count - 1 : activeIndex;
            if (event.key === 'ArrowRight') { next = Math.min(count - 1, next + 1); }
            else if (event.key === 'ArrowLeft') { next = Math.max(0, next - 1); }
            else if (event.key === 'Home') { next = 0; }
            else if (event.key === 'End') { next = count - 1; }
            else { return; }
            event.preventDefault();
            showPoint(next);
        });

        render('income', 6);
    })();

    /* --------------------------------------------------------- feature demo */
    (function initFeatureDemo() {
        var widget = document.querySelector('.min-demo-widget');
        if (!widget) { return; }

        var tabs = Array.prototype.slice.call(widget.querySelectorAll('[role="tab"]'));
        var panes = Array.prototype.slice.call(widget.querySelectorAll('[role="tabpanel"]'));

        function activateTab(tab, focusTab) {
            tabs.forEach(function (item) {
                var active = item === tab;
                item.classList.toggle('is-active', active);
                item.setAttribute('aria-selected', active ? 'true' : 'false');
                item.tabIndex = active ? 0 : -1;
            });
            panes.forEach(function (pane) {
                var active = pane.id === tab.getAttribute('aria-controls');
                pane.classList.toggle('is-active', active);
                pane.hidden = !active;
            });
            if (focusTab) { tab.focus(); }
        }

        tabs.forEach(function (tab, index) {
            tab.addEventListener('click', function () { activateTab(tab, false); });
            tab.addEventListener('keydown', function (event) {
                var next = null;
                if (event.key === 'ArrowRight') { next = tabs[(index + 1) % tabs.length]; }
                if (event.key === 'ArrowLeft') { next = tabs[(index - 1 + tabs.length) % tabs.length]; }
                if (event.key === 'Home') { next = tabs[0]; }
                if (event.key === 'End') { next = tabs[tabs.length - 1]; }
                if (next) {
                    event.preventDefault();
                    activateTab(next, true);
                }
            });
        });

        function bindChips(group, callback) {
            if (!group) { return; }
            var chips = Array.prototype.slice.call(group.querySelectorAll('button'));
            chips.forEach(function (chip) {
                chip.addEventListener('click', function () {
                    chips.forEach(function (item) { item.classList.toggle('is-active', item === chip); });
                    callback(chip);
                });
            });
        }

        var category = document.getElementById('catatCategory');
        var catatAmount = document.getElementById('catatAmount');

        bindChips(widget.querySelector('[data-category-group]'), function (chip) {
            if (category) { category.textContent = chip.getAttribute('data-category'); }
        });
        bindChips(widget.querySelector('[data-amount-group]'), function (chip) {
            if (catatAmount) { catatAmount.textContent = formatRupiah(Number(chip.getAttribute('data-amount'))); }
        });

        var input = document.getElementById('demoRange');
        if (!input) { return; }
        var amount = document.getElementById('demoAmount');
        var remaining = document.getElementById('demoRemaining');
        var meter = document.getElementById('demoMeter');
        var readAmount = document.getElementById('demoReadAmount');
        var readRemaining = document.getElementById('demoReadRemaining');
        var readMeter = document.getElementById('demoReadMeter');
        var status = document.getElementById('demoStatus');
        var budget = Number(input.max) || 800000;

        function updateBudget() {
            var value = Number(input.value) || 0;
            var left = Math.max(0, budget - value);
            var percent = Math.min(100, (value / budget) * 100);
            var remainingPercent = 100 - percent;
            var label = percent >= 100 ? 'Penuh' : percent >= 80 ? 'Hampir habis' : 'Aman';

            amount.textContent = formatRupiah(value);
            remaining.textContent = formatRupiah(left);
            meter.style.transform = 'scaleX(' + (remainingPercent / 100) + ')';
            if (readAmount) { readAmount.textContent = formatRupiah(value); }
            if (readRemaining) { readRemaining.textContent = formatRupiah(left); }
            if (readMeter) { readMeter.style.transform = 'scaleX(' + (remainingPercent / 100) + ')'; }
            if (status) { status.textContent = label; }
        }

        input.addEventListener('input', updateBudget);
        updateBudget();
    })();

    /* Feature demo and chart initialize above; no global boot flag is needed. */
})();
