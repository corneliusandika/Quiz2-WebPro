(function () {
    'use strict';

    var $ = function (selector, scope) { return (scope || document).querySelector(selector); };
    var $$ = function (selector, scope) { return Array.prototype.slice.call((scope || document).querySelectorAll(selector)); };
    var currencyMeta = document.querySelector('meta[name="currency-decimals"]');
    var currencyMode = currencyMeta ? currencyMeta.getAttribute('content') : 'auto';
    var rupiah = new Intl.NumberFormat('id-ID', {
        style: 'currency',
        currency: 'IDR',
        minimumFractionDigits: currencyMode === 'always' ? 2 : 0,
        maximumFractionDigits: currencyMode === 'never' ? 0 : 2
    });
    var rupiahFraction = new Intl.NumberFormat('id-ID', {
        style: 'currency',
        currency: 'IDR',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
    var chartInstances = [];

    function addChart(target, config) {
        var chart = new Chart(target, config);
        chartInstances.push(chart);
        return chart;
    }

    function toNumber(value) {
        if (typeof value === 'number') {
            return isFinite(value) ? value : 0;
        }
        var raw = String(value == null ? '' : value).trim();
        raw = raw.indexOf(',') >= 0
            ? raw.replace(/\./g, '').replace(',', '.')
            : raw.replace(/\./g, '');
        var number = Number(raw.replace(/[^\d.-]/g, ''));
        return isFinite(number) ? number : 0;
    }

    function formatRupiah(value) {
        var number = toNumber(value);
        var formatter = currencyMode !== 'never' && !Number.isInteger(number) ? rupiahFraction : rupiah;
        return formatter.format(number).replace(/\u00a0/g, ' ');
    }

    function formatMoneyInput(input) {
        var sanitized = String(input.value || '').replace(/[^\d.,]/g, '');
        if (!sanitized) { input.value = ''; return; }

        var lastComma = sanitized.lastIndexOf(',');
        var lastDot = sanitized.lastIndexOf('.');
        var decimalIndex = -1;
        if (lastComma >= 0 && (lastDot < 0 || lastComma > lastDot)) {
            decimalIndex = lastComma;
        } else if (lastDot >= 0) {
            var fractionLength = sanitized.length - lastDot - 1;
            if (fractionLength <= 2) {
                decimalIndex = lastDot;
            }
        }

        var integerPart = decimalIndex < 0 ? sanitized : sanitized.slice(0, decimalIndex);
        var fraction = decimalIndex < 0 ? '' : sanitized.slice(decimalIndex + 1).replace(/\D/g, '').slice(0, 2);
        var integerDigits = integerPart.replace(/\D/g, '');
        var formatted = new Intl.NumberFormat('id-ID').format(Number(integerDigits || 0));
        if (decimalIndex >= 0) { formatted += ',' + fraction; }
        input.value = formatted;
    }

    function formSubmitButton(form) {
        return form ? form.querySelector('button[type="submit"], button:not([type])') : null;
    }

    function setButtonLoading(button, loading) {
        if (!button || button.tagName !== 'BUTTON' || !button.classList.contains('btn')) { return; }
        if (loading) {
            if (button.dataset.loading === '1') { return; }
            button.dataset.loading = '1';
            button.dataset.originalHtml = button.innerHTML;
            button.dataset.originalMinWidth = button.style.minWidth || '';
            button.style.minWidth = button.offsetWidth + 'px';
            button.classList.add('is-loading');
            button.disabled = true;
            button.setAttribute('aria-busy', 'true');
            button.innerHTML = '<span class="btn-spinner" aria-hidden="true"></span><span>'
                + (button.getAttribute('data-loading-label') || 'Memproses...') + '</span>';
        } else {
            if (button.dataset.loading !== '1') { return; }
            button.classList.remove('is-loading');
            button.disabled = false;
            button.removeAttribute('aria-busy');
            button.innerHTML = button.dataset.originalHtml || button.innerHTML;
            button.style.minWidth = button.dataset.originalMinWidth || '';
            delete button.dataset.loading;
            delete button.dataset.originalHtml;
            delete button.dataset.originalMinWidth;
        }
    }

    function trapFocus(container, event) {
        if (!container) { return; }
        var focusables = $$('a[href], button, input, select, textarea, [tabindex]:not([tabindex="-1"])', container)
            .filter(function (el) { return !el.disabled && el.getClientRects().length > 0; });
        if (!focusables.length) { return; }
        var first = focusables[0];
        var last = focusables[focusables.length - 1];
        if (focusables.indexOf(document.activeElement) === -1) {
            event.preventDefault();
            first.focus();
        } else if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    }

    function initMoneyInputs() {
        $$('[data-money-input]').forEach(function (input) {
            input.addEventListener('input', function () {
                input.value = String(input.value || '').replace(/[^\d.,]/g, '');
            });
            input.addEventListener('blur', function () { formatMoneyInput(input); });
            formatMoneyInput(input);
        });
    }

    function verticalGradient(topColor, bottomColor) {
        return function (context) {
            var chart = context.chart;
            var area = chart.chartArea;
            if (!area) { return topColor; }
            var gradient = chart.ctx.createLinearGradient(0, area.top, 0, area.bottom);
            gradient.addColorStop(0, topColor);
            gradient.addColorStop(1, bottomColor);
            return gradient;
        };
    }

    function compact(value) {
        var number = Math.abs(toNumber(value));
        var decimals = currencyMode === 'always' ? 2 : currencyMode === 'never' ? 0 : 1;
        if (number >= 1000000000) { return 'Rp ' + (number / 1000000000).toFixed(decimals).replace('.', ',') + ' M'; }
        if (number >= 1000000) { return 'Rp ' + (number / 1000000).toFixed(decimals).replace('.', ',') + ' jt'; }
        if (number >= 1000) { return 'Rp ' + (number / 1000).toFixed(decimals).replace('.', ',') + ' rb'; }
        return formatRupiah(number);
    }

    function cssVar(name) {
        return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    }

    /* ---------------------------------------------------------------- theme */
    var themeToggle = document.getElementById('themeToggle');
    var systemTheme = window.matchMedia('(prefers-color-scheme: dark)');

    function currentThemeMode() {
        return document.documentElement.getAttribute('data-theme-mode') || 'system';
    }

    function themeToggleLabel(mode) {
        if (mode === 'light') { return 'Tema terang. Klik untuk kembali ke tema sistem'; }
        if (mode === 'dark') { return 'Tema gelap. Klik untuk kembali ke tema sistem'; }
        return systemTheme.matches
            ? 'Tema sistem gelap. Klik untuk tema terang'
            : 'Tema sistem terang. Klik untuk tema gelap';
    }

    function syncThemeToggle() {
        if (!themeToggle) { return; }
        var mode = currentThemeMode();
        themeToggle.removeAttribute('aria-pressed');
        var label = themeToggleLabel(mode);
        themeToggle.setAttribute('aria-label', label);
        themeToggle.setAttribute('title', label);
    }

    function applyThemeMode(mode, persist) {
        var root = document.documentElement;
        var resolved = mode === 'system' ? (systemTheme.matches ? 'dark' : 'light') : mode;
        root.setAttribute('data-theme', resolved);
        root.setAttribute('data-theme-mode', mode);
        var meta = document.querySelector('meta[name="color-scheme"]');
        if (meta) { meta.content = mode === 'system' ? 'light dark' : mode; }
        if (persist !== false) {
            try {
                if (mode === 'system') { localStorage.removeItem('dompetku-theme'); }
                else { localStorage.setItem('dompetku-theme', mode); }
            } catch (e) { /* ignore */ }
        }
        syncThemeToggle();
        refreshChartTheme();
    }

    function applyThemeWithTransition(mode, persist) {
        if (document.startViewTransition && !prefersReducedMotion()) {
            try {
                document.startViewTransition(function () { applyThemeMode(mode, persist); });
                return;
            } catch (e) { /* fall back to an instant theme switch */ }
        }
        applyThemeMode(mode, persist);
    }

    if (themeToggle) {
        themeToggle.addEventListener('click', function () {
            var mode = currentThemeMode();
            if (mode === 'system') {
                applyThemeWithTransition(systemTheme.matches ? 'light' : 'dark', true);
            } else {
                applyThemeWithTransition('system', true);
            }
        });
        if (!document.documentElement.getAttribute('data-theme-mode')) {
            applyThemeMode('system', false);
        } else {
            syncThemeToggle();
        }
        if (systemTheme.addEventListener) {
            systemTheme.addEventListener('change', function () {
                if (currentThemeMode() === 'system') { applyThemeMode('system', false); }
            });
        } else if (systemTheme.addListener) {
            systemTheme.addListener(function () {
                if (currentThemeMode() === 'system') { applyThemeMode('system', false); }
            });
        }
        window.addEventListener('storage', function (event) {
            if (event.key !== 'dompetku-theme') { return; }
            applyThemeMode(event.newValue === 'light' || event.newValue === 'dark' ? event.newValue : 'system', false);
        });
    }

    /* -------------------------------------------------------------- sidebar */
    var sidebar = document.getElementById('sidebar');
    var backdrop = document.getElementById('sidebarBackdrop');
    var menuToggle = document.getElementById('menuToggle');
    var sidebarClose = document.getElementById('sidebarClose');
    var sidebarPin = document.getElementById('sidebarPin');
    var mainShell = document.querySelector('.main');
    var sidebarReturnFocus = null;

    function setSidebarOpen(open, returnFocus) {
        var wasOpen = !!(sidebar && sidebar.classList.contains('is-open'));
        if (open && sidebar && !wasOpen) {
            sidebarReturnFocus = document.activeElement;
            sidebar.setAttribute('role', 'dialog');
            sidebar.setAttribute('aria-modal', 'true');
            sidebar.setAttribute('aria-label', 'Menu navigasi');
            if (mainShell) { mainShell.inert = true; }
            document.body.classList.add('drawer-open');
        }
        if (sidebar) { sidebar.classList.toggle('is-open', open); }
        if (backdrop) { backdrop.classList.toggle('is-visible', open); }
        if (menuToggle) {
            menuToggle.setAttribute('aria-expanded', open ? 'true' : 'false');
            menuToggle.setAttribute('aria-label', open ? 'Tutup menu' : 'Buka menu');
        }
        if (open && sidebar && !wasOpen) {
            if (sidebarClose) { sidebarClose.focus(); }
        } else if (!open && wasOpen) {
            if (mainShell) { mainShell.inert = false; }
            sidebar.removeAttribute('role');
            sidebar.removeAttribute('aria-modal');
            sidebar.removeAttribute('aria-label');
            document.body.classList.remove('drawer-open');
            if (returnFocus !== false && sidebarReturnFocus) {
                sidebarReturnFocus.focus();
                sidebarReturnFocus = null;
            }
        }
    }

    function closeSidebar(returnFocus) {
        setSidebarOpen(false, returnFocus);
    }

    if (menuToggle && sidebar) {
        menuToggle.addEventListener('click', function () {
            setSidebarOpen(!sidebar.classList.contains('is-open'));
        });
    }
    if (backdrop) { backdrop.addEventListener('click', function () { closeSidebar(); }); }
    if (sidebarClose) { sidebarClose.addEventListener('click', function () { closeSidebar(); }); }

    var desktopSidebarQuery = window.matchMedia('(min-width: 1181px)');
    var sidebarStateKey = 'dompetku-sidebar-expanded';

    function setSidebarExpanded(expanded, persist) {
        if (!sidebar) { return; }
        if (!desktopSidebarQuery.matches) { expanded = false; }
        sidebar.classList.toggle('is-expanded', expanded);
        document.documentElement.classList.toggle('sidebar-expanded', expanded);
        if (mainShell) { mainShell.classList.toggle('sidebar-expanded-main', expanded); }
        if (sidebarPin) {
            sidebarPin.setAttribute('aria-expanded', expanded ? 'true' : 'false');
            sidebarPin.setAttribute('aria-label', expanded ? 'Tutup menu' : 'Buka menu');
            sidebarPin.setAttribute('data-sidebar-tooltip', expanded ? 'Tutup menu' : 'Buka menu');
        }
        if (persist) {
            try {
                window.localStorage.setItem(sidebarStateKey, expanded ? 'true' : 'false');
            } catch (e) { /* storage can be disabled */ }
        }
        window.setTimeout(function () {
            window.dispatchEvent(new Event('resize'));
        }, 260);
    }

    if (sidebarPin) {
        sidebarPin.addEventListener('click', function () {
            setSidebarExpanded(!sidebar.classList.contains('is-expanded'), true);
            hideSidebarTooltip();
        });
    }

    function onDesktopSidebarChange(event) {
        if (event.matches && sidebar && sidebar.classList.contains('is-open')) {
            closeSidebar(false);
        }
        if (!event.matches) {
            setSidebarExpanded(false, false);
        } else {
            var stored = false;
            try { stored = window.localStorage.getItem(sidebarStateKey) === 'true'; } catch (e) { /* ignore */ }
            setSidebarExpanded(stored, false);
        }
    }
    if (desktopSidebarQuery.addEventListener) {
        desktopSidebarQuery.addEventListener('change', onDesktopSidebarChange);
    } else if (desktopSidebarQuery.addListener) {
        desktopSidebarQuery.addListener(onDesktopSidebarChange);
    }

    var storedExpanded = false;
    try { storedExpanded = window.localStorage.getItem(sidebarStateKey) === 'true'; } catch (e) { /* ignore */ }
    setSidebarExpanded(storedExpanded, false);

    /* Tooltips are rendered outside the scrolling sidebar so they are never clipped. */
    var sidebarTooltip = document.createElement('div');
    sidebarTooltip.className = 'sidebar-tooltip';
    sidebarTooltip.setAttribute('role', 'tooltip');
    document.body.appendChild(sidebarTooltip);

    function tooltipTargets() {
        if (!sidebar) { return []; }
        var targets = Array.prototype.slice.call(sidebar.querySelectorAll(
            '.nav-item, .brand, .sidebar-user'));
        if (sidebarPin) { targets.push(sidebarPin); }
        return targets;
    }

    function tooltipLabel(target) {
        var explicit = target.getAttribute('data-sidebar-tooltip');
        if (explicit) { return explicit; }
        var text = target.querySelector('.nav-item > span, .brand-text strong, .sidebar-user-text strong');
        return text ? text.textContent.trim() : (target.getAttribute('aria-label') || '');
    }

    function hideSidebarTooltip() {
        sidebarTooltip.classList.remove('is-visible');
    }

    function showSidebarTooltip(target) {
        if (!desktopSidebarQuery.matches || !sidebar || sidebar.classList.contains('is-expanded')) { return; }
        var label = tooltipLabel(target);
        if (!label) { return; }
        var rect = target.getBoundingClientRect();
        sidebarTooltip.textContent = label;
        sidebarTooltip.style.left = Math.round(rect.right + 10) + 'px';
        sidebarTooltip.style.top = Math.round(rect.top + rect.height / 2) + 'px';
        sidebarTooltip.classList.add('is-visible');
    }

    tooltipTargets().forEach(function (target) {
        target.addEventListener('mouseenter', function () { showSidebarTooltip(target); });
        target.addEventListener('mouseleave', hideSidebarTooltip);
        target.addEventListener('focus', function () { showSidebarTooltip(target); });
        target.addEventListener('blur', hideSidebarTooltip);
    });
    if (sidebar) { sidebar.addEventListener('scroll', hideSidebarTooltip, { passive: true }); }

    document.addEventListener('keydown', function (event) {
        if (event.key !== 'Tab' || !sidebar || !sidebar.classList.contains('is-open')) { return; }
        trapFocus(sidebar, event);
    });

    /* ------------------------------------------------------ a11y enhancements */
    $$('.nav-item.is-active').forEach(function (link) { link.setAttribute('aria-current', 'page'); });
    $$('a.icon-btn[title], button.icon-btn[title]').forEach(function (el) {
        if (!el.getAttribute('aria-label')) { el.setAttribute('aria-label', el.getAttribute('title')); }
    });
    $$('.data-table thead th').forEach(function (th) {
        if (!th.hasAttribute('scope')) { th.setAttribute('scope', 'col'); }
    });
    var generatedFieldId = 0;
    $$('label.field').forEach(function (label) {
        var control = label.querySelector('input, select, textarea');
        if (!control) { return; }
        if (!control.id) {
            generatedFieldId += 1;
            control.id = 'field-' + generatedFieldId;
        }
        if (!label.getAttribute('for')) { label.setAttribute('for', control.id); }
    });
    $$('.table-wrap').forEach(function (wrapper) {
        var table = wrapper.querySelector('table');
        if (!table) { return; }
        wrapper.setAttribute('tabindex', '0');
        wrapper.setAttribute('role', 'region');
        var card = wrapper.closest('.card, article');
        var heading = card ? card.querySelector('h2, h3') : null;
        var label = heading ? heading.textContent.trim() : 'Tabel data';
        wrapper.setAttribute('aria-label', label);
        if (!table.querySelector('caption')) {
            var caption = document.createElement('caption');
            caption.className = 'sr-only';
            caption.textContent = label;
            table.prepend(caption);
        }
        var headers = $$('thead th', table).map(function (th) { return th.textContent.trim(); });
        $$('tbody tr', table).forEach(function (row) {
            $$('td', row).forEach(function (cell, index) {
                if (!cell.dataset.label) { cell.dataset.label = headers[index] || ''; }
            });
        });
    });
    $$('[aria-describedby]').forEach(function (element) {
        var ids = (element.getAttribute('aria-describedby') || '').split(/\s+/).filter(function (id) {
            return id && document.getElementById(id);
        });
        if (ids.length) { element.setAttribute('aria-describedby', ids.join(' ')); }
        else { element.removeAttribute('aria-describedby'); }
    });
    $$('.error-text').forEach(function (error, index) {
        var field = error.closest('.field');
        var control = field ? field.querySelector('input, select, textarea') : null;
        if (!control) { return; }
        if (!error.id) { error.id = 'field-error-' + (index + 1); }
        var describedBy = (control.getAttribute('aria-describedby') || '').split(/\s+/).filter(Boolean);
        if (describedBy.indexOf(error.id) === -1) { describedBy.push(error.id); }
        control.setAttribute('aria-describedby', describedBy.join(' '));
        control.setAttribute('aria-invalid', 'true');
    });
    var formError = document.getElementById('formError');
    if (formError) { formError.focus(); }
    $$('.error-summary-list a').forEach(function (link) {
        link.addEventListener('click', function (event) {
            var target = document.querySelector(link.getAttribute('href'));
            if (!target) { return; }
            event.preventDefault();
            target.focus();
            target.scrollIntoView({ block: 'center' });
        });
    });

    /* --------------------------------------------------------------- toasts */
    $$('[data-toast-close]').forEach(function (button) {
        button.addEventListener('click', function () {
            var toast = button.closest('.toast');
            if (!toast) { return; }
            toast.classList.add('is-leaving');
            window.setTimeout(function () { toast.remove(); }, prefersReducedMotion() ? 0 : 350);
        });
    });
    var flashToast = document.getElementById('flashToast');
    if (flashToast) {
        window.setTimeout(function () {
            if (!flashToast.parentNode) { return; }
            flashToast.style.transition = prefersReducedMotion()
                ? 'none'
                : 'opacity var(--duration-medium) var(--ease-smooth-out), transform var(--duration-medium) var(--ease-smooth-out)';
            flashToast.style.opacity = '0';
            flashToast.style.transform = 'translateY(calc(var(--distance-small) * -1))';
            window.setTimeout(function () { flashToast.remove(); }, prefersReducedMotion() ? 0 : 350);
        }, 5000);
    }

    /* -------------------------------------------------------------- confirm */
    var modal = document.getElementById('confirmModal');
    var confirmMessage = document.getElementById('confirmMessage');
    var confirmAccept = document.getElementById('confirmAccept');
    var pendingForm = null;
    var modalClosing = false;

    function closeConfirm() {
        if (modal && modal.open && !modalClosing) {
            modalClosing = true;
            modal.classList.add('is-closing');
            window.setTimeout(function () {
                modal.classList.remove('is-closing');
                modal.close();
                modalClosing = false;
            }, prefersReducedMotion() ? 0 : 150);
        }
        pendingForm = null;
    }

    if (modal) {
        modal.addEventListener('cancel', function (event) {
            event.preventDefault();
            closeConfirm();
        });
        modal.addEventListener('close', function () {
            modal.classList.remove('is-closing');
            modalClosing = false;
            pendingForm = null;
        });
        modal.addEventListener('click', function (event) {
            if (event.target === modal) { closeConfirm(); }
        });
    }

    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (form.matches('form[data-confirm]') && !form.dataset.confirmed) {
            event.preventDefault();
            pendingForm = form;
            if (modal) {
                confirmMessage.textContent = form.getAttribute('data-confirm') || 'Apakah Anda yakin?';
                modal.classList.remove('is-closing');
                modalClosing = false;
                if (!modal.open) { modal.showModal(); }
            }
        }
    });

    if (confirmAccept) {
        confirmAccept.addEventListener('click', function () {
            if (pendingForm) {
                pendingForm.dataset.confirmed = '1';
                setButtonLoading(confirmAccept, true);
                pendingForm.submit();
            }
        });
    }
    $$('[data-confirm-cancel]').forEach(function (el) {
        el.addEventListener('click', closeConfirm);
    });
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') { closeConfirm(); closeSidebar(); }
    });

    /* ------------------------------------------------------------ passwords */
    $$('[data-password-toggle]').forEach(function (button) {
        button.addEventListener('click', function () {
            var input = $(button.getAttribute('data-password-toggle'));
            if (!input) { return; }
            var show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            button.classList.toggle('is-on', show);
            button.setAttribute('aria-pressed', show ? 'true' : 'false');
            button.setAttribute('aria-label', show ? 'Sembunyikan password' : 'Tampilkan password');
        });
    });

    /* -------------------------------------------------------- password rules */
    var passwordMeter = $('[data-password-meter]');
    if (passwordMeter) {
        var meterPassword = $(passwordMeter.getAttribute('data-password'));
        var meterConfirm = $(passwordMeter.getAttribute('data-confirm'));
        var confirmHint = $('[data-confirm-hint]', passwordMeter);
        var meterForm = meterPassword ? meterPassword.closest('form') : null;
        var meterTouched = !!(document.getElementById('passwordError') || document.getElementById('confirmError'));

        var updateConfirmHint = function () {
            if (!meterPassword || !meterConfirm || !confirmHint) { return; }
            var confirmation = meterConfirm.value;
            confirmHint.hidden = !confirmation;
            if (!confirmation) {
                confirmHint.textContent = '';
                confirmHint.classList.remove('is-match', 'is-mismatch');
                return;
            }
            var matches = confirmation === meterPassword.value;
            confirmHint.classList.toggle('is-match', matches);
            confirmHint.classList.toggle('is-mismatch', !matches);
            confirmHint.textContent = matches ? 'Konfirmasi password cocok.' : 'Konfirmasi password belum sama.';
        };

        var syncMeterVisibility = function () {
            if (!meterPassword) { return; }
            var focused = document.activeElement === meterPassword
                || document.activeElement === meterConfirm;
            passwordMeter.hidden = !focused;
        };

        var updatePasswordMeter = function () {
            if (!meterPassword) { return null; }
            var value = meterPassword.value;
            var hasValue = value.length > 0;
            var checks = {
                length: value.length >= 8,
                letter: /[A-Za-z]/.test(value),
                digit: /\d/.test(value),
                symbol: /[^A-Za-z0-9\s]/.test(value)
            };
            $$('[data-rule]', passwordMeter).forEach(function (rule) {
                var met = !!checks[rule.getAttribute('data-rule')];
                rule.classList.toggle('is-met', hasValue && met);
                rule.classList.toggle('is-missing', hasValue && meterTouched && !met);
                if (hasValue && (met || meterTouched)) {
                    rule.setAttribute('aria-label', rule.textContent.trim() + (met ? ', terpenuhi' : ', belum terpenuhi'));
                } else {
                    rule.removeAttribute('aria-label');
                }
            });
            if (meterPassword) {
                var passwordInvalid = meterTouched && hasValue
                    && (!checks.length || !checks.letter || !checks.digit || !checks.symbol);
                meterPassword.setAttribute('aria-invalid', passwordInvalid ? 'true' : 'false');
            }
            if (meterConfirm) {
                var confirmInvalid = meterTouched && meterConfirm.value.length > 0
                    && meterConfirm.value !== meterPassword.value;
                meterConfirm.setAttribute('aria-invalid', confirmInvalid ? 'true' : 'false');
            }
            updateConfirmHint();
            syncMeterVisibility();
            return checks;
        };

        var rejectPasswordMeter = function () {
            passwordMeter.classList.remove('is-rejected');
            void passwordMeter.offsetWidth;
            passwordMeter.classList.add('is-rejected');
            window.setTimeout(function () { passwordMeter.classList.remove('is-rejected'); }, 320);
        };

        if (meterPassword) {
            meterPassword.addEventListener('focus', function () {
                passwordMeter.hidden = false;
            });
            meterPassword.addEventListener('input', updatePasswordMeter);
            meterPassword.addEventListener('blur', function () {
                meterTouched = true;
                setTimeout(function () { updatePasswordMeter(); syncMeterVisibility(); }, 0);
            });
        }
        if (meterConfirm) {
            meterConfirm.addEventListener('focus', function () {
                passwordMeter.hidden = false;
            });
            meterConfirm.addEventListener('input', function () {
                updateConfirmHint();
                syncMeterVisibility();
            });
            meterConfirm.addEventListener('blur', function () {
                meterTouched = true;
                setTimeout(function () { updateConfirmHint(); syncMeterVisibility(); }, 0);
            });
        }
        if (meterForm) {
            meterForm.addEventListener('submit', function (event) {
                meterTouched = true;
                var checks = updatePasswordMeter();
                if (!checks || !checks.length || !checks.letter || !checks.digit || !checks.symbol) {
                    event.preventDefault();
                    rejectPasswordMeter();
                    meterPassword.focus();
                    return;
                }
                if (meterConfirm && meterConfirm.value !== meterPassword.value) {
                    event.preventDefault();
                    rejectPasswordMeter();
                    meterConfirm.focus();
                    updateConfirmHint();
                }
            });
        }
        updatePasswordMeter();
    }

    /* ------------------------------------------------------------ form loading */
    document.addEventListener('submit', function (event) {
        if (event.defaultPrevented) { return; }
        var form = event.target;
        if (!form || !form.matches || !form.matches('form')) { return; }
        setButtonLoading(event.submitter || formSubmitButton(form), true);
    });

    window.addEventListener('pageshow', function () {
        $$('.btn.is-loading').forEach(function (button) { setButtonLoading(button, false); });
    });

    /* ------------------------------------------------------------ color swatches */
    $$('[data-set-color]').forEach(function (button) {
        button.addEventListener('click', function () {
            var form = button.closest('form');
            var input = form ? form.querySelector('input[type="color"]') : null;
            if (!input) { return; }
            input.value = button.getAttribute('data-set-color');
            input.dispatchEvent(new Event('input', { bubbles: true }));
            var palette = button.parentNode;
            if (palette) {
                $$('.swatch', palette).forEach(function (swatch) { swatch.classList.remove('is-active'); });
            }
            button.classList.add('is-active');
        });
    });

    /* --------------------------------------------------- segmented/type active */
    function refreshChoiceGroups(scope) {
        $$('.segment input, .type-option input, .icon-option input', scope).forEach(function (input) {
            var wrapper = input.closest('.segment, .type-option, .icon-option');
            if (wrapper) { wrapper.classList.toggle('is-active', input.checked); }
        });
    }
    document.addEventListener('change', function (event) {
        if (event.target.matches('.segment input, .type-option input, .icon-option input')) {
            refreshChoiceGroups(document);
        }
    });
    refreshChoiceGroups(document);

    /* ------------------------------------------------------- transaction form */
    function initTransactionForm() {
        var form = document.getElementById('transactionForm');
        if (!form) { return; }
        var amount = $('#txAmount');
        var wallet = $('#txWallet');
        var category = $('#txCategory');
        var date = $('#txDate');
        var typeRadios = $$('input[name="type"]', form);
        var previewAmount = $('[data-preview-amount]');
        var previewType = $('[data-preview-type]');
        var previewWallet = $('[data-preview-wallet]');
        var previewCategory = $('[data-preview-category]');
        var previewDate = $('[data-preview-date]');

        function selectedType() {
            var checked = form.querySelector('input[name="type"]:checked');
            return checked ? checked.value : 'expense';
        }

        function filterCategories() {
            var type = selectedType();
            var selectedOk = false;
            $$('option', category).forEach(function (option) {
                if (!option.value) { return; }
                var match = option.getAttribute('data-type') === type;
                option.hidden = !match;
                option.disabled = !match;
                if (match && option.selected) { selectedOk = true; }
            });
            if (!selectedOk && category.value) {
                category.value = '';
            }
        }

        function update() {
            var formatted = formatRupiah(amount.value);
            if (previewAmount) { previewAmount.textContent = formatted; }
            var type = selectedType();
            if (previewType) {
                previewType.textContent = type === 'income' ? 'Pemasukan' : 'Pengeluaran';
                previewType.className = 'badge badge-preview';
                previewType.style.background = type === 'income' ? 'rgba(16,185,129,.12)' : 'rgba(225,29,72,.10)';
                previewType.style.color = type === 'income' ? '#059669' : '#be123c';
            }
            if (previewWallet) {
                previewWallet.textContent = wallet.selectedIndex >= 0 ? wallet.options[wallet.selectedIndex].text : '-';
            }
            if (previewCategory) {
                previewCategory.textContent = category.selectedIndex >= 0 ? category.options[category.selectedIndex].text : '-';
            }
            if (previewDate) {
                if (date.value) {
                    var d = new Date(date.value + 'T00:00:00');
                    previewDate.textContent = d.toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' });
                } else {
                    previewDate.textContent = '-';
                }
            }
            $$('.segment', form).forEach(function (segment) {
                var radio = segment.querySelector('input');
                segment.classList.toggle('is-active', radio && radio.checked);
            });
        }

        if (amount) { amount.addEventListener('input', update); }
        if (wallet) { wallet.addEventListener('change', update); }
        if (category) { category.addEventListener('change', update); }
        if (date) { date.addEventListener('change', update); }
        typeRadios.forEach(function (radio) {
            radio.addEventListener('change', function () { filterCategories(); update(); });
        });
        filterCategories();
        update();
    }

    /* ---------------------------------------------------------- category form */
    function initCategoryForm() {
        var form = document.getElementById('categoryForm');
        if (!form) { return; }
        var name = $('#categoryName');
        var color = $('#categoryColor');
        var previewName = $('#categoryPreviewName');
        var previewIcon = $('#categoryPreviewIcon');
        var previewType = $('#categoryPreviewType');

        function paint() {
            if (previewName) {
                previewName.textContent = name.value.trim() || 'Nama kategori';
            }
            if (previewIcon) {
                previewIcon.style.background = color.value + '1f';
                previewIcon.style.color = color.value;
            }
            var type = form.querySelector('input[name="type"]:checked');
            if (previewType && type) {
                previewType.textContent = type.value === 'income' ? 'Pemasukan' : 'Pengeluaran';
            }
        }

        name.addEventListener('input', paint);
        color.addEventListener('input', paint);
        $$('input[name="icon"]', form).forEach(function (radio) {
            radio.addEventListener('change', function () {
                if (!previewIcon) { return; }
                var label = radio.closest('.icon-option');
                var visual = label ? label.querySelector('span') : null;
                if (visual) { previewIcon.innerHTML = visual.innerHTML; }
                paint();
            });
        });
        $$('input[name="type"]', form).forEach(function (radio) {
            radio.addEventListener('change', paint);
        });
        paint();
    }

    /* ------------------------------------------------------------ wallet form */
    function initWalletForm() {
        var form = document.getElementById('walletForm');
        if (!form) { return; }
        var name = $('#walletName');
        var balance = $('#walletBalance');
        var color = $('#walletColor');
        var preview = $('#walletPreview');
        var previewName = $('#walletPreviewName');
        var previewBalance = $('#walletPreviewBalance');
        var previewIcon = preview ? preview.querySelector('.wallet-preview-icon') : null;

        function paint() {
            if (previewName) { previewName.textContent = name.value.trim() || 'Nama dompet'; }
            if (previewBalance) { previewBalance.textContent = formatRupiah(balance.value); }
            if (preview) { preview.style.setProperty('--wc', color.value); }
        }

        name.addEventListener('input', paint);
        balance.addEventListener('input', paint);
        color.addEventListener('input', paint);
        $$('input[name="type"]', form).forEach(function (radio) {
            radio.addEventListener('change', function () {
                var label = radio.closest('.type-option');
                var visual = label ? label.querySelector('.type-option-icon') : null;
                if (previewIcon && visual) { previewIcon.innerHTML = visual.innerHTML; }
            });
        });
        paint();
    }

    /* ------------------------------------------------------------ budget form */
    function initBudgetForm() {
        var form = document.getElementById('budgetForm');
        if (!form) { return; }
        var category = $('#budgetCategory');
        var amount = $('#budgetAmount');
        var previewName = $('#budgetPreviewName');
        var previewAmount = $('#budgetPreviewAmount');
        var previewIcon = $('#budgetPreviewIcon');

        function paint() {
            if (previewName) {
                previewName.textContent = category.selectedIndex > 0 ? category.options[category.selectedIndex].text : 'Pilih kategori';
            }
            if (previewAmount) { previewAmount.textContent = formatRupiah(amount.value); }
            if (previewIcon && category.selectedIndex > 0) {
                var optionColor = category.options[category.selectedIndex].getAttribute('data-color') || '#f97316';
                previewIcon.style.background = optionColor + '1f';
                previewIcon.style.color = optionColor;
            }
        }

        category.addEventListener('change', paint);
        amount.addEventListener('input', paint);
        paint();
    }

    /* -------------------------------------------------------------- goal form */
    function initGoalForm() {
        var form = document.getElementById('goalForm');
        if (!form) { return; }
        var name = $('#goalName');
        var amount = $('#goalAmount');
        var color = $('#goalColor');
        var previewName = $('#goalPreviewName');
        var previewAmount = $('#goalPreviewAmount');
        var preview = $('#goalPreview');
        var ring = preview ? preview.querySelector('.goal-ring') : null;

        function paint() {
            if (previewName) { previewName.textContent = name.value.trim() || 'Nama target'; }
            if (previewAmount) { previewAmount.textContent = formatRupiah(amount.value); }
            if (ring) { ring.style.setProperty('--c', color.value); }
        }

        name.addEventListener('input', paint);
        amount.addEventListener('input', paint);
        color.addEventListener('input', paint);
        paint();
    }

    /* -------------------------------------------------------------- debt form */
    function initDebtForm() {
        var form = document.getElementById('debtForm');
        if (!form) { return; }
        var counterpart = form.querySelector('input[name="counterpart"]');
        var amount = form.querySelector('input[name="amount"]');
        var previewName = $('#debtPreviewName');
        var previewAmount = $('#debtPreviewAmount');
        var previewAvatar = $('#debtPreviewAvatar');
        var previewType = $('#debtPreviewType');

        function initials(value) {
            var parts = (value || '').trim().split(/\s+/).filter(Boolean);
            if (!parts.length) { return '?'; }
            return (parts[0].charAt(0) + (parts.length > 1 ? parts[parts.length - 1].charAt(0) : '')).toUpperCase();
        }

        function paint() {
            if (previewName) { previewName.textContent = counterpart.value.trim() || 'Nama pihak'; }
            if (previewAvatar) { previewAvatar.textContent = initials(counterpart.value); }
            if (previewAmount) { previewAmount.textContent = formatRupiah(amount.value); }
            var type = form.querySelector('input[name="type"]:checked');
            if (previewType && type) {
                previewType.textContent = type.value === 'payable' ? 'Harus dibayar' : 'Akan diterima';
            }
        }

        counterpart.addEventListener('input', paint);
        amount.addEventListener('input', paint);
        $$('input[name="type"]', form).forEach(function (radio) {
            radio.addEventListener('change', paint);
        });
        paint();
    }

    /* --------------------------------------------------------- recurring form */
    function initRecurringForm() {
        var form = document.getElementById('recurringForm');
        if (!form) { return; }
        var name = $('#recurringName');
        var amount = form.querySelector('input[name="amount"]');
        var category = $('#recurringCategory');
        var nextDate = form.querySelector('input[name="nextDate"]');
        var frequency = form.querySelector('select[name="frequency"]');
        var previewName = $('#recurringPreviewName');
        var previewAmount = $('#recurringPreviewAmount');
        var previewSchedule = $('#recurringPreviewSchedule');

        function filterCategories() {
            var type = form.querySelector('input[name="type"]:checked');
            var value = type ? type.value : 'expense';
            $$('option', category).forEach(function (option) {
                if (!option.value) { return; }
                var match = option.getAttribute('data-type') === value;
                option.hidden = !match;
                option.disabled = !match;
            });
            if (category.selectedOptions.length === 0) { category.value = ''; }
        }

        function paint() {
            if (previewName) { previewName.textContent = name.value.trim() || 'Nama jadwal'; }
            if (previewAmount) { previewAmount.textContent = formatRupiah(amount.value); }
            if (previewSchedule) {
                var label = frequency.options[frequency.selectedIndex].text;
                var dateText = '';
                if (nextDate.value) {
                    var d = new Date(nextDate.value + 'T00:00:00');
                    dateText = d.toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' });
                }
                previewSchedule.textContent = label + ' · ' + (dateText || 'jadwal berikutnya');
            }
        }

        $$('input[name="type"]', form).forEach(function (radio) {
            radio.addEventListener('change', function () { filterCategories(); paint(); });
        });
        name.addEventListener('input', paint);
        amount.addEventListener('input', paint);
        category.addEventListener('change', paint);
        nextDate.addEventListener('change', paint);
        frequency.addEventListener('change', paint);
        filterCategories();
        paint();
    }

    /* ----------------------------------------------------------------- charts */
    function initCharts() {
        if (typeof Chart === 'undefined') { return; }

        var muted = cssVar('--muted') || '#94a3b8';
        var border = cssVar('--border') || '#e6ebf3';
        var text = cssVar('--text-soft') || '#475569';
        Chart.defaults.font.family = "'Plus Jakarta Sans', system-ui, sans-serif";
        Chart.defaults.font.size = 10;
        Chart.defaults.color = muted;

        var grid = { color: border, drawTicks: false };
        var ticks = { color: muted, font: { size: 10, weight: '600' }, padding: 8 };
        var legend = { display: false };
        var tooltip = {
            backgroundColor: cssVar('--sidebar-bg') || '#0f172a',
            titleColor: '#f8fafc',
            bodyColor: '#e2e8f0',
            padding: 11,
            cornerRadius: 10,
            displayColors: true,
            boxWidth: 8,
            boxHeight: 8,
            usePointStyle: true
        };

        function moneyTooltip(context) {
            var value = context.parsed && typeof context.parsed === 'object'
                ? (context.parsed.y !== undefined ? context.parsed.y : context.parsed.x)
                : context.parsed;
            return ' ' + context.dataset.label + ': ' + formatRupiah(value);
        }

        function yAxis() {
            return {
                beginAtZero: true,
                grid: grid,
                border: { display: false },
                ticks: Object.assign({}, ticks, { callback: function (value) { return compact(value).replace('Rp ', ''); } })
            };
        }

        var data = window.DOMPETKU_CHART;
        if (data) {
            var cashflow = document.getElementById('cashflowChart');
            if (cashflow) {
                var cashflowChart = addChart(cashflow, {
                    type: 'bar',
                    data: {
                        labels: data.months,
                        datasets: [
                            {
                                label: 'Pemasukan',
                                data: data.monthIncome,
                                backgroundColor: verticalGradient('rgba(16,185,129,.95)', 'rgba(16,185,129,.32)'),
                                hoverBackgroundColor: '#10b981',
                                borderRadius: 7,
                                borderSkipped: false,
                                maxBarThickness: 26
                            },
                            {
                                label: 'Pengeluaran',
                                data: data.monthExpense,
                                backgroundColor: verticalGradient('rgba(251,113,133,.9)', 'rgba(251,113,133,.28)'),
                                hoverBackgroundColor: '#fb7185',
                                borderRadius: 7,
                                borderSkipped: false,
                                maxBarThickness: 26
                            }
                        ]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        animation: prefersReducedMotion() ? false : { duration: 700, easing: 'easeOutQuart' },
                        plugins: { legend: legend, tooltip: Object.assign({ callbacks: { label: moneyTooltip } }, tooltip) },
                        scales: { x: { grid: { display: false }, border: { display: false }, ticks: ticks }, y: yAxis() },
                        interaction: { mode: 'index', intersect: false },
                        onClick: function (event, elements) {
                            if (!elements || !elements.length) { return; }
                            var key = data.monthKeys && data.monthKeys[elements[0].index];
                            if (!key || !window.DOMPETKU_CTX) { return; }
                            window.location.href = window.DOMPETKU_CTX + '/app/transactions?month=' + encodeURIComponent(key);
                        },
                        onHover: function (event, elements) {
                            if (event.native && event.native.target) {
                                event.native.target.style.cursor = elements && elements.length ? 'pointer' : 'default';
                            }
                        }
                    }
                });
                bindCashflowLegend(cashflowChart);
            }

            var daily = document.getElementById('dailyChart');
            if (daily) {
                addChart(daily, {
                    type: 'line',
                    data: {
                        labels: data.days,
                        datasets: [
                            {
                                label: 'Pemasukan',
                                data: data.dayIncome,
                                borderColor: '#10b981',
                                backgroundColor: verticalGradient('rgba(16,185,129,.20)', 'rgba(16,185,129,0)'),
                                fill: true, tension: .38, borderWidth: 2.2,
                                pointRadius: 0, pointHoverRadius: 4
                            },
                            {
                                label: 'Pengeluaran',
                                data: data.dayExpense,
                                borderColor: '#fb7185',
                                backgroundColor: verticalGradient('rgba(251,113,133,.16)', 'rgba(251,113,133,0)'),
                                fill: true, tension: .38, borderWidth: 2.2,
                                pointRadius: 0, pointHoverRadius: 4
                            }
                        ]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        animation: prefersReducedMotion() ? false : { duration: 800, easing: 'easeOutQuart' },
                        plugins: { legend: legend, tooltip: Object.assign({ callbacks: { label: moneyTooltip } }, tooltip) },
                        scales: {
                            x: { grid: { display: false }, border: { display: false }, ticks: Object.assign({}, ticks, { maxTicksLimit: 15 }) },
                            y: yAxis()
                        },
                        interaction: { mode: 'index', intersect: false }
                    }
                });
            }

            var category = document.getElementById('categoryChart');
            if (category && data.categoryValues && data.categoryValues.length) {
                addChart(category, {
                    type: 'doughnut',
                    data: {
                        labels: data.categoryLabels,
                        datasets: [{
                            data: data.categoryValues,
                            backgroundColor: data.categoryColors,
                            borderWidth: 0,
                            hoverOffset: 7,
                            spacing: 2,
                            borderRadius: 4
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        cutout: '73%',
                        plugins: {
                            legend: legend,
                            tooltip: Object.assign({
                                callbacks: {
                                    label: function (context) {
                                        return ' ' + context.label + ': ' + formatRupiah(context.parsed);
                                    }
                                }
                            }, tooltip)
                        }
                    }
                });
            }
        }

        var report = window.DOMPETKU_REPORT;
        if (report) {
            var reportDaily = document.getElementById('reportDailyChart');
            if (reportDaily) {
                addChart(reportDaily, {
                    type: 'line',
                    data: {
                        labels: report.days,
                        datasets: [
                            {
                                label: 'Pemasukan', data: report.dayIncome,
                                borderColor: '#10b981', backgroundColor: verticalGradient('rgba(16,185,129,.20)', 'rgba(16,185,129,0)'),
                                fill: true, tension: .38, borderWidth: 2.2, pointRadius: 0, pointHoverRadius: 4
                            },
                            {
                                label: 'Pengeluaran', data: report.dayExpense,
                                borderColor: '#fb7185', backgroundColor: verticalGradient('rgba(251,113,133,.16)', 'rgba(251,113,133,0)'),
                                fill: true, tension: .38, borderWidth: 2.2, pointRadius: 0, pointHoverRadius: 4
                            }
                        ]
                    },
                    options: {
                        responsive: true, maintainAspectRatio: false,
                        plugins: { legend: legend, tooltip: Object.assign({ callbacks: { label: moneyTooltip } }, tooltip) },
                        scales: {
                            x: { grid: { display: false }, border: { display: false }, ticks: Object.assign({}, ticks, { maxTicksLimit: 15 }) },
                            y: yAxis()
                        },
                        interaction: { mode: 'index', intersect: false }
                    }
                });
            }

            var reportMonthly = document.getElementById('reportMonthlyChart');
            if (reportMonthly) {
                addChart(reportMonthly, {
                    type: 'bar',
                    data: {
                        labels: report.months,
                        datasets: [
                            {
                                label: 'Pemasukan', data: report.monthIncome,
                                backgroundColor: verticalGradient('rgba(16,185,129,.95)', 'rgba(16,185,129,.32)'),
                                hoverBackgroundColor: '#10b981',
                                borderRadius: 7, borderSkipped: false, maxBarThickness: 28
                            },
                            {
                                label: 'Pengeluaran', data: report.monthExpense,
                                backgroundColor: verticalGradient('rgba(251,113,133,.9)', 'rgba(251,113,133,.28)'),
                                hoverBackgroundColor: '#fb7185',
                                borderRadius: 7, borderSkipped: false, maxBarThickness: 28
                            }
                        ]
                    },
                    options: {
                        responsive: true, maintainAspectRatio: false,
                        plugins: { legend: legend, tooltip: Object.assign({ callbacks: { label: moneyTooltip } }, tooltip) },
                        scales: { x: { grid: { display: false }, border: { display: false }, ticks: ticks }, y: yAxis() },
                        interaction: { mode: 'index', intersect: false }
                    }
                });
            }
        }
    }

    function bindCashflowLegend(chart) {
        if (!chart) { return; }
        var buttons = document.querySelectorAll('.chart-legend-item[data-chart="cashflowChart"]');
        buttons.forEach(function (button) {
            button.addEventListener('click', function () {
                var index = parseInt(button.getAttribute('data-dataset'), 10);
                var dataset = chart.data.datasets[index];
                if (!dataset) { return; }
                dataset.hidden = !dataset.hidden;
                button.setAttribute('aria-pressed', dataset.hidden ? 'false' : 'true');
                chart.update();
            });
        });
    }

    function prefersReducedMotion() {
        return window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    }

    function initCountUps() {
        if (prefersReducedMotion()) { return; }
        document.querySelectorAll('[data-countup]').forEach(function (element) {
            var target = Number(element.getAttribute('data-countup'));
            if (!isFinite(target) || target === 0) { return; }
            var format = element.getAttribute('data-format');
            var started = null;
            var duration = 500;
            function frame(now) {
                if (started === null) { started = now; }
                var progress = Math.min(1, (now - started) / duration);
                var eased = 1 - Math.pow(1 - progress, 3);
                var value = target * eased;
                element.textContent = format === 'percent'
                    ? Math.round(value) + '%'
                    : formatRupiah(value);
                if (progress < 1) {
                    requestAnimationFrame(frame);
                } else {
                    element.textContent = format === 'percent'
                        ? Math.round(target) + '%'
                        : formatRupiah(target);
                }
            }
            requestAnimationFrame(frame);
        });
    }

    function initProgressAnimations() {
        if (prefersReducedMotion()) { return; }
        document.querySelectorAll('.progress-bar[data-width]').forEach(function (bar) {
            var width = Number(bar.getAttribute('data-width')) || 0;
            bar.style.transition = 'none';
            bar.style.setProperty('--bar-width', '0');
            void bar.offsetWidth;
            bar.style.transition = '';
            bar.style.setProperty('--bar-width', String(width));
        });
        document.querySelectorAll('.goal-ring[data-progress]').forEach(function (ring) {
            var progress = ring.getAttribute('data-progress');
            ring.style.transition = 'none';
            ring.style.setProperty('--p', '0');
            void ring.offsetWidth;
            ring.style.transition = '';
            ring.style.setProperty('--p', progress);
        });
    }

    function refreshChartTheme() {
        if (typeof Chart === 'undefined' || !chartInstances.length) { return; }
        var muted = cssVar('--muted') || '#94a3b8';
        var border = cssVar('--border') || '#e6ebf3';
        var tooltipBg = cssVar('--sidebar-bg') || '#0f172a';
        Chart.defaults.color = muted;
        chartInstances.forEach(function (chart) {
            var scales = chart.options.scales || {};
            Object.keys(scales).forEach(function (key) {
                var scale = scales[key];
                if (scale.ticks) { scale.ticks.color = muted; }
                if (scale.grid) { scale.grid.color = border; }
            });
            var tooltip = chart.options.plugins && chart.options.plugins.tooltip;
            if (tooltip) { tooltip.backgroundColor = tooltipBg; }
            chart.update('none');
        });
    }

    /* ------------------------------------------------------------- user menu */
    (function initUserMenu() {
        var toggle = document.getElementById('userMenuToggle');
        var menu = document.getElementById('userMenu');
        if (!toggle || !menu) { return; }

        function closeMenu(returnFocus) {
            if (menu.hidden) { return; }
            menu.hidden = true;
            toggle.setAttribute('aria-expanded', 'false');
            if (returnFocus) { toggle.focus(); }
        }

        function openMenu() {
            menu.hidden = false;
            toggle.setAttribute('aria-expanded', 'true');
        }

        toggle.addEventListener('click', function () {
            if (menu.hidden) { openMenu(); } else { closeMenu(false); }
        });

        document.addEventListener('click', function (event) {
            if (menu.hidden) { return; }
            if (!menu.contains(event.target) && !toggle.contains(event.target)) {
                closeMenu(false);
            }
        });

        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && !menu.hidden) {
                closeMenu(true);
            }
        });
    })();

    /* ------------------------------------------------------------------ boot */
    initMoneyInputs();
    initTransactionForm();
    initCategoryForm();
    initWalletForm();
    initBudgetForm();
    initGoalForm();
    initDebtForm();
    initRecurringForm();
    initCharts();
    initCountUps();
    initProgressAnimations();
})();
