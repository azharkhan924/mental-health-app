/**
 * Breathe Heal Grow — Mobile Navigation & Global UI Handlers
 * Uses EVENT DELEGATION for maximum reliability across all browsers and devices.
 */
(function () {
  'use strict';

  /* =========================================================
     HELPER: Open / Close / Toggle mobile drawer
     ========================================================= */
  function openDrawer() {
    var drawer = document.getElementById('mobileNavDrawer');
    var backdrop = document.getElementById('mobileNavBackdrop');
    var toggle = document.getElementById('navToggle');
    if (drawer) {
      drawer.classList.add('open');
      drawer.style.visibility = 'visible';
      drawer.style.pointerEvents = 'auto';
    }
    if (backdrop) {
      backdrop.classList.add('open');
      backdrop.style.display = 'block';
      backdrop.style.pointerEvents = 'auto';
    }
    if (toggle) {
      toggle.classList.add('active');
      toggle.setAttribute('aria-expanded', 'true');
    }
    document.body.style.overflow = 'hidden';
  }

  function closeDrawer() {
    var drawer = document.getElementById('mobileNavDrawer');
    var backdrop = document.getElementById('mobileNavBackdrop');
    var toggle = document.getElementById('navToggle');
    if (drawer) {
      drawer.classList.remove('open');
      drawer.style.visibility = '';
      drawer.style.pointerEvents = '';
    }
    if (backdrop) {
      backdrop.classList.remove('open');
      backdrop.style.display = 'none';
      backdrop.style.pointerEvents = '';
    }
    if (toggle) {
      toggle.classList.remove('active');
      toggle.setAttribute('aria-expanded', 'false');
    }
    document.body.style.overflow = '';
  }

  function toggleDrawer() {
    var drawer = document.getElementById('mobileNavDrawer');
    if (drawer && drawer.classList.contains('open')) {
      closeDrawer();
    } else {
      openDrawer();
    }
  }

  /* =========================================================
     HELPER: Switch dashboard tabs
     ========================================================= */
  function switchDashboardTab(tabId, updateHash) {
    var targetPanel = document.getElementById('tab-' + tabId);
    if (!targetPanel) return;

    // Update Tab Panels with direct display property
    var panels = document.querySelectorAll('.dash-tab-content');
    panels.forEach(function (panel) {
      if (panel.id === 'tab-' + tabId) {
        panel.classList.add('active');
        panel.style.display = 'block';
      } else {
        panel.classList.remove('active');
        panel.style.display = 'none';
      }
    });

    // Update Tab Buttons (.dash-tab-btn)
    document.querySelectorAll('.dash-tab-btn').forEach(function (btn) {
      btn.classList.toggle('active', btn.getAttribute('data-tab') === tabId);
    });

    // Update Bottom Nav Items
    document.querySelectorAll('.mobile-bottom-nav__item[data-tab]').forEach(function (item) {
      item.classList.toggle('active', item.getAttribute('data-tab') === tabId);
    });

    // Update nav switchers (drawer links)
    document.querySelectorAll('[data-tab-switch]').forEach(function (item) {
      item.classList.toggle('active', item.getAttribute('data-tab-switch') === tabId);
    });

    if (updateHash && history.replaceState) {
      try {
        history.replaceState(null, null, '#' + tabId);
      } catch (e) {}
    }
  }

  // Expose to window object
  window.openDrawer = openDrawer;
  window.closeDrawer = closeDrawer;
  window.toggleDrawer = toggleDrawer;
  window.switchDashboardTab = switchDashboardTab;

  /* =========================================================
     SINGLE EVENT DELEGATION on document (click)
     ========================================================= */
  document.addEventListener('click', function (e) {
    var target = e.target;

    // --- Hamburger Toggle Button ---
    var navToggle = target.closest('#navToggle');
    if (navToggle) {
      e.preventDefault();
      e.stopPropagation();
      var drawer = document.getElementById('mobileNavDrawer');
      if (drawer && drawer.classList.contains('open')) {
        closeDrawer();
      } else {
        openDrawer();
      }
      return;
    }

    // --- Mobile Nav Close Button ---
    if (target.closest('#mobileNavClose')) {
      e.preventDefault();
      closeDrawer();
      return;
    }

    // --- Mobile Nav Backdrop ---
    if (target.id === 'mobileNavBackdrop') {
      closeDrawer();
      return;
    }

    // --- Dashboard Tab Buttons (.dash-tab-btn) ---
    var tabBtn = target.closest('.dash-tab-btn');
    if (tabBtn) {
      e.preventDefault();
      var tabId = tabBtn.getAttribute('data-tab');
      if (tabId) {
        switchDashboardTab(tabId, true);
      }
      return;
    }

    // --- Mobile Bottom Nav Items ---
    var bottomItem = target.closest('.mobile-bottom-nav__item[data-tab]');
    if (bottomItem) {
      e.preventDefault();
      var tabId = bottomItem.getAttribute('data-tab');
      if (tabId && document.getElementById('tab-' + tabId)) {
        switchDashboardTab(tabId, true);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
      return;
    }

    // --- Nav Switcher Links (drawer tab links) ---
    var navSwitch = target.closest('[data-tab-switch]');
    if (navSwitch) {
      var tabId = navSwitch.getAttribute('data-tab-switch');
      if (tabId && document.getElementById('tab-' + tabId)) {
        e.preventDefault();
        closeDrawer();
        switchDashboardTab(tabId, true);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
      return;
    }

    // --- In-page hash links that match a dashboard tab ---
    var hashLink = target.closest('a[href^="#"]');
    if (hashLink) {
      var hash = hashLink.getAttribute('href').substring(1);
      var tabKey = hash.replace(/-section$/, '');
      if (tabKey && document.getElementById('tab-' + tabKey)) {
        e.preventDefault();
        closeDrawer();
        switchDashboardTab(tabKey, true);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    }
  });

  /* =========================================================
     KEYBOARD: Close drawer on ESC
     ========================================================= */
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
      var drawer = document.getElementById('mobileNavDrawer');
      if (drawer && drawer.classList.contains('open')) {
        closeDrawer();
      }
    }
  });

  /* =========================================================
     HASH: Restore tab from URL on page load & on hash change
     ========================================================= */
  function restoreTabFromHash() {
    var hash = window.location.hash ? window.location.hash.substring(1) : '';
    var tabKey = hash.replace(/-section$/, '');
    if (tabKey && document.getElementById('tab-' + tabKey)) {
      switchDashboardTab(tabKey, false);
    }
  }

  // On page load
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () {
      restoreTabFromHash();
      // Also check ?tab= query param
      var urlParams = new URLSearchParams(window.location.search);
      var tabParam = urlParams.get('tab');
      if (tabParam && document.getElementById('tab-' + tabParam)) {
        switchDashboardTab(tabParam, false);
      }
    });
  } else {
    restoreTabFromHash();
    var urlParams = new URLSearchParams(window.location.search);
    var tabParam = urlParams.get('tab');
    if (tabParam && document.getElementById('tab-' + tabParam)) {
      switchDashboardTab(tabParam, false);
    }
  }

  window.addEventListener('hashchange', restoreTabFromHash);

  /* =========================================================
     LUCIDE: Initialize icons (on DOMContentLoaded & helper)
     ========================================================= */
  function initIcons() {
    if (typeof lucide !== 'undefined' && lucide.createIcons) {
      lucide.createIcons();
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initIcons);
  } else {
    initIcons();
  }

  // Global helper to refresh icons after dynamic DOM changes
  window.refreshIcons = initIcons;
})();
