// Loanmust - UI behaviours

(function () {
    var sidebar = document.getElementById('sidebar');
    var overlay = document.getElementById('overlay');
    var menuBtn = document.getElementById('menuBtn');

    function openSidebar() {
        sidebar.classList.remove('-translate-x-full');
        overlay.classList.remove('hidden');
    }
    function closeSidebar() {
        sidebar.classList.add('-translate-x-full');
        overlay.classList.add('hidden');
    }

    var moreBtn = document.getElementById('moreBtn');
    if (menuBtn) menuBtn.addEventListener('click', openSidebar);
    if (moreBtn) moreBtn.addEventListener('click', openSidebar);
    if (overlay) overlay.addEventListener('click', closeSidebar);

    document.querySelectorAll('#sidebar .nav-link').forEach(function (link) {
        link.addEventListener('click', function () {
            if (window.innerWidth < 1024) closeSidebar();
        });
    });

    // Auto-dismiss the toast
    var toast = document.getElementById('toast');
    if (toast) {
        setTimeout(function () {
            toast.style.transition = 'opacity .4s ease';
            toast.style.opacity = '0';
            setTimeout(function () { toast.remove(); }, 400);
        }, 4000);
    }
})();

// --- Debounced list search: submit the existing GET form as the user types ----
(function () {
    document.querySelectorAll('form[data-live-search]').forEach(function (form) {
        var input = form.querySelector('input[type=search]');
        if (!input) return;
        var timer;
        var initial = input.value;
        input.addEventListener('input', function () {
            clearTimeout(timer);
            timer = setTimeout(function () {
                if (input.value === initial) return;
                form.submit();
            }, 350);
        });
        form.addEventListener('submit', function () { clearTimeout(timer); });
        if (initial) {
            input.focus();
            input.setSelectionRange(initial.length, initial.length);
        }
    });
})();

// --- Whole-row links on list screens -----------------------------------------
(function () {
    document.querySelectorAll('tr[data-href]').forEach(function (row) {
        row.addEventListener('click', function (e) {
            if (e.target.closest('.js-action') || e.target.closest('a') || e.target.closest('button')) return;
            window.location.href = row.dataset.href;
        });
    });
})();

// --- Confirm before archiving --------------------------------------------------
(function () {
    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            if (!window.confirm(form.getAttribute('data-confirm'))) {
                e.preventDefault();
            }
        });
    });
})();

// --- Country-code phone inputs -------------------------------------------------
(function () {
    var CODES = ['+91', '+971', '+880', '+977', '+94', '+65', '+61', '+44', '+1'];
    function initPhone(g) {
        var cc = g.querySelector('.phone-cc'), num = g.querySelector('.phone-num'),
            hid = g.querySelector('input[type=hidden]');
        if (!cc || !num || !hid) return;
        var val = (g.getAttribute('data-value') || hid.value || '').replace(/\s/g, '');
        if (val && val !== 'null') {
            var matched = null;
            CODES.slice().sort(function (a, b) { return b.length - a.length; })
                .forEach(function (c) { if (!matched && val.indexOf(c) === 0) matched = c; });
            if (matched) { cc.value = matched; num.value = val.substring(matched.length); }
            else { num.value = val.replace(/^\+/, ''); }
        }
        function sync() { hid.value = cc.value + num.value.replace(/[^0-9]/g, ''); }
        cc.addEventListener('change', sync);
        num.addEventListener('input', sync);
        sync();
    }
    function initAll() { document.querySelectorAll('.phone-group').forEach(initPhone); }
    if (document.readyState !== 'loading') initAll();
    else document.addEventListener('DOMContentLoaded', initAll);
})();

// --- Theme switcher (Light / Dark / System) -----------------------------------
(function () {
    var KEY = 'ni-theme';
    var mql = window.matchMedia('(prefers-color-scheme: dark)');

    function apply(mode) {
        var dark = mode === 'dark' || (mode === 'system' && mql.matches);
        document.documentElement.classList.toggle('dark', dark);
        document.documentElement.setAttribute('data-theme', mode);
        markActive(mode);
    }
    function current() { return localStorage.getItem(KEY) || 'system'; }
    function markActive(mode) {
        document.querySelectorAll('[data-theme-set]').forEach(function (b) {
            var on = b.getAttribute('data-theme-set') === mode;
            b.classList.toggle('bg-brand-50', on);
            b.classList.toggle('text-brand-700', on);
            b.classList.toggle('font-semibold', on);
        });
    }

    var wrap = document.querySelector('[data-theme-menu]');
    if (wrap) {
        var btn = wrap.querySelector('[data-theme-btn]');
        var panel = wrap.querySelector('[data-theme-panel]');
        btn.addEventListener('click', function (e) {
            e.stopPropagation();
            panel.classList.toggle('hidden');
            markActive(current());
        });
        document.addEventListener('click', function (e) {
            if (!wrap.contains(e.target)) panel.classList.add('hidden');
        });
        wrap.querySelectorAll('[data-theme-set]').forEach(function (item) {
            item.addEventListener('click', function () {
                var mode = item.getAttribute('data-theme-set');
                localStorage.setItem(KEY, mode);
                apply(mode);
                panel.classList.add('hidden');
            });
        });
    }
    // Follow the OS when in "system" mode.
    mql.addEventListener('change', function () {
        if (current() === 'system') apply('system');
    });
    apply(current());
})();

// --- Language switcher ---------------------------------------------------------
(function () {
    var wrap = document.querySelector('[data-lang-menu]');
    if (!wrap) return;
    var btn = wrap.querySelector('[data-lang-btn]');
    var panel = wrap.querySelector('[data-lang-panel]');
    if (btn && panel) {
        btn.addEventListener('click', function (e) {
            e.stopPropagation();
            panel.classList.toggle('hidden');
        });
        document.addEventListener('click', function (e) {
            if (!wrap.contains(e.target)) panel.classList.add('hidden');
        });
        panel.querySelectorAll('[data-lang]').forEach(function (item) {
            item.addEventListener('click', function () {
                var url = new URL(window.location.href);
                url.searchParams.set('lang', item.getAttribute('data-lang'));
                window.location = url.toString();
            });
        });
    }
})();

// --- Copy-to-clipboard buttons -------------------------------------------------
(function () {
    document.querySelectorAll('[data-copy-btn]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var text = btn.getAttribute('data-copy');
            if (!text) return;
            var done = function () {
                var original = btn.innerHTML;
                btn.innerHTML = '<i class="bi bi-check-lg"></i> Copied';
                setTimeout(function () { btn.innerHTML = original; }, 1600);
            };
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(text).then(done, done);
            } else {
                var t = document.createElement('textarea');
                t.value = text; document.body.appendChild(t); t.select();
                try { document.execCommand('copy'); } catch (e) {}
                document.body.removeChild(t); done();
            }
        });
    });
})();

// --- Collapsible advanced filter panels ---------------------------------------
(function () {
    document.querySelectorAll('[data-filter-toggle]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var panel = document.querySelector(btn.getAttribute('data-filter-toggle'));
            if (panel) panel.classList.toggle('hidden');
        });
    });
})();
