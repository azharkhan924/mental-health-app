/**
 * Breathe Heal Grow — Mobile Navigation & Global UI Handlers
 * Uses EVENT DELEGATION for maximum reliability across all browsers and devices.
 */
(function () {
  'use strict';

  /* =========================================================
     HELPER: Open / Close / Toggle mobile drawer
     ========================================================= */
  function openDrawer(e) {
    if (e) {
      if (typeof e.preventDefault === 'function') e.preventDefault();
      if (typeof e.stopPropagation === 'function') e.stopPropagation();
    }
    var drawer = document.getElementById('mobileNavDrawer');
    var backdrop = document.getElementById('mobileNavBackdrop');
    var toggle = document.getElementById('navToggle');
    if (drawer) {
      drawer.classList.add('open');
      drawer.style.setProperty('visibility', 'visible', 'important');
      drawer.style.setProperty('pointer-events', 'auto', 'important');
      drawer.style.setProperty('transform', 'translateX(0)', 'important');
    }
    if (backdrop) {
      backdrop.classList.add('open');
      backdrop.style.setProperty('display', 'block', 'important');
      backdrop.style.setProperty('visibility', 'visible', 'important');
      backdrop.style.setProperty('pointer-events', 'auto', 'important');
      backdrop.style.setProperty('opacity', '1', 'important');
    }
    if (toggle) {
      toggle.classList.add('active');
      toggle.setAttribute('aria-expanded', 'true');
    }
    document.body.style.overflow = 'hidden';
  }

  function closeDrawer(e) {
    if (e) {
      if (typeof e.preventDefault === 'function') e.preventDefault();
      if (typeof e.stopPropagation === 'function') e.stopPropagation();
    }
    var drawer = document.getElementById('mobileNavDrawer');
    var backdrop = document.getElementById('mobileNavBackdrop');
    var toggle = document.getElementById('navToggle');
    if (drawer) {
      drawer.classList.remove('open');
      drawer.style.setProperty('visibility', 'hidden', 'important');
      drawer.style.setProperty('pointer-events', 'none', 'important');
      drawer.style.setProperty('transform', 'translateX(100%)', 'important');
    }
    if (backdrop) {
      backdrop.classList.remove('open');
      backdrop.style.setProperty('display', 'none', 'important');
      backdrop.style.setProperty('visibility', 'hidden', 'important');
      backdrop.style.setProperty('pointer-events', 'none', 'important');
      backdrop.style.setProperty('opacity', '0', 'important');
    }
    if (toggle) {
      toggle.classList.remove('active');
      toggle.setAttribute('aria-expanded', 'false');
    }
    document.body.style.overflow = '';
  }

  function toggleDrawer(e) {
    if (e) {
      if (typeof e.preventDefault === 'function') e.preventDefault();
      if (typeof e.stopPropagation === 'function') e.stopPropagation();
    }
    var drawer = document.getElementById('mobileNavDrawer');
    if (drawer && (drawer.classList.contains('open') || drawer.style.visibility === 'visible')) {
      closeDrawer(e);
    } else {
      openDrawer(e);
    }
  }

  /* =========================================================
     HELPER: Switch dashboard tabs
     ========================================================= */
  function switchDashboardTab(tabId, updateHash) {
    if (!tabId) return;
    var targetPanel = document.getElementById('tab-' + tabId);
    if (!targetPanel) return;

    // 1. Show target panel, hide all other panels directly with important
    var panels = document.querySelectorAll('.dash-tab-content');
    for (var i = 0; i < panels.length; i++) {
      if (panels[i].id === 'tab-' + tabId) {
        panels[i].classList.add('active');
        panels[i].style.setProperty('display', 'block', 'important');
      } else {
        panels[i].classList.remove('active');
        panels[i].style.setProperty('display', 'none', 'important');
      }
    }

    // 2. Desktop Tab Buttons (.dash-tab-btn)
    var btns = document.querySelectorAll('.dash-tab-btn');
    for (var j = 0; j < btns.length; j++) {
      btns[j].classList.toggle('active', btns[j].getAttribute('data-tab') === tabId);
    }

    // 3. Mobile Bottom Nav Items (.mobile-bottom-nav__item)
    var bottomItems = document.querySelectorAll('.mobile-bottom-nav__item');
    for (var k = 0; k < bottomItems.length; k++) {
      bottomItems[k].classList.toggle('active', bottomItems[k].getAttribute('data-tab') === tabId);
    }

    // 4. Drawer Links ([data-tab-switch])
    var links = document.querySelectorAll('[data-tab-switch]');
    for (var l = 0; l < links.length; l++) {
      links[l].classList.toggle('active', links[l].getAttribute('data-tab-switch') === tabId);
    }

    // 5. Update URL hash
    if (updateHash !== false && window.history && window.history.replaceState) {
      try {
        window.history.replaceState(null, null, '#' + tabId);
      } catch (e) {}
    }

    // 6. If the user was scrolled far down the page (e.g. from bottom nav), gently bring tabs bar into view
    if (updateHash !== false) {
      var tabsBar = document.querySelector('.dash-tabs-bar');
      if (tabsBar) {
        var barRect = tabsBar.getBoundingClientRect();
        if (barRect.top < 60) {
          var topPos = barRect.top + (window.pageYOffset || document.documentElement.scrollTop) - 75;
          window.scrollTo({ top: Math.max(0, topPos), behavior: 'smooth' });
        }
      }
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
    if (e.defaultPrevented) return;

    var target = e.target;

    // --- Hamburger Toggle Button ---
    var navToggle = target.closest('#navToggle');
    if (navToggle) {
      toggleDrawer(e);
      return;
    }

    // --- Mobile Nav Close Button ---
    if (target.closest('#mobileNavClose')) {
      closeDrawer(e);
      return;
    }

    // --- Mobile Nav Backdrop ---
    if (target.id === 'mobileNavBackdrop') {
      closeDrawer(e);
      return;
    }

    // --- Dashboard Tab Buttons (.dash-tab-btn) ---
    var tabBtn = target.closest('.dash-tab-btn');
    if (tabBtn) {
      var tabId = tabBtn.getAttribute('data-tab');
      if (tabId) {
        if (typeof e.preventDefault === 'function') e.preventDefault();
        switchDashboardTab(tabId, true);
      }
      return;
    }

    // --- Mobile Bottom Nav Items ---
    var bottomItem = target.closest('.mobile-bottom-nav__item[data-tab]');
    if (bottomItem) {
      var tabId = bottomItem.getAttribute('data-tab');
      if (tabId && document.getElementById('tab-' + tabId)) {
        if (typeof e.preventDefault === 'function') e.preventDefault();
        switchDashboardTab(tabId, true);
      }
      return;
    }

    // --- Nav Switcher Links (drawer tab links) ---
    var navSwitch = target.closest('[data-tab-switch]');
    if (navSwitch) {
      var tabId = navSwitch.getAttribute('data-tab-switch');
      if (tabId && document.getElementById('tab-' + tabId)) {
        if (typeof e.preventDefault === 'function') e.preventDefault();
        closeDrawer(e);
        switchDashboardTab(tabId, true);
      }
      return;
    }

    // --- In-page hash links that match a dashboard tab ---
    var hashLink = target.closest('a[href^="#"]');
    if (hashLink) {
      var hash = hashLink.getAttribute('href').substring(1);
      var tabKey = hash.replace(/-section$/, '');
      if (tabKey && document.getElementById('tab-' + tabKey)) {
        if (typeof e.preventDefault === 'function') e.preventDefault();
        closeDrawer(e);
        switchDashboardTab(tabKey, true);
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
