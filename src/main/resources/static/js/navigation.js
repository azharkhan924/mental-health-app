/**
 * Breathe Heal Grow — Mobile Navigation & Global UI Handlers
 */
(function () {
  'use strict';

  document.addEventListener('DOMContentLoaded', function () {
    // 1. Mobile Hamburger Drawer Toggle
    var navToggle = document.getElementById('navToggle');
    var mobileDrawer = document.getElementById('mobileNavDrawer');
    var mobileBackdrop = document.getElementById('mobileNavBackdrop');
    var mobileClose = document.getElementById('mobileNavClose');

    function openDrawer() {
      if (!mobileDrawer) return;
      mobileDrawer.classList.add('open');
      if (mobileBackdrop) mobileBackdrop.classList.add('open');
      if (navToggle) {
        navToggle.classList.add('active');
        navToggle.setAttribute('aria-expanded', 'true');
      }
      document.body.style.overflow = 'hidden';
    }

    function closeDrawer() {
      if (!mobileDrawer) return;
      mobileDrawer.classList.remove('open');
      if (mobileBackdrop) mobileBackdrop.classList.remove('open');
      if (navToggle) {
        navToggle.classList.remove('active');
        navToggle.setAttribute('aria-expanded', 'false');
      }
      document.body.style.overflow = '';
    }

    if (navToggle) {
      navToggle.addEventListener('click', function (e) {
        e.preventDefault();
        if (mobileDrawer && mobileDrawer.classList.contains('open')) {
          closeDrawer();
        } else {
          openDrawer();
        }
      });
    }

    if (mobileClose) {
      mobileClose.addEventListener('click', function (e) {
        e.preventDefault();
        closeDrawer();
      });
    }

    if (mobileBackdrop) {
      mobileBackdrop.addEventListener('click', function () {
        closeDrawer();
      });
    }

    // Close on ESC key
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && mobileDrawer && mobileDrawer.classList.contains('open')) {
        closeDrawer();
      }
    });

    // 2. Dashboard Segmented Tabs Switching (if present on page)
    var tabButtons = document.querySelectorAll('.dash-tab-btn');
    var tabPanels = document.querySelectorAll('.dash-tab-content');
    var bottomNavItems = document.querySelectorAll('.mobile-bottom-nav__item[data-tab]');
    var navSwitchers = document.querySelectorAll('[data-tab-switch]');

    function switchDashboardTab(tabId, updateHash) {
      if (!tabPanels.length) return;

      var targetPanel = document.getElementById('tab-' + tabId);
      if (!targetPanel) return;

      // Update Tab Buttons
      tabButtons.forEach(function (btn) {
        if (btn.getAttribute('data-tab') === tabId) {
          btn.classList.add('active');
        } else {
          btn.classList.remove('active');
        }
      });

      // Update Tab Panels
      tabPanels.forEach(function (panel) {
        if (panel.id === 'tab-' + tabId) {
          panel.classList.add('active');
        } else {
          panel.classList.remove('active');
        }
      });

      // Update Bottom Nav Items
      bottomNavItems.forEach(function (item) {
        if (item.getAttribute('data-tab') === tabId) {
          item.classList.add('active');
        } else {
          item.classList.remove('active');
        }
      });

      // Update any nav switchers
      navSwitchers.forEach(function (item) {
        if (item.getAttribute('data-tab-switch') === tabId) {
          item.classList.add('active');
        } else {
          item.classList.remove('active');
        }
      });

      if (updateHash && history.replaceState) {
        history.replaceState(null, null, '#' + tabId);
      }
    }

    if (tabButtons.length) {
      tabButtons.forEach(function (btn) {
        btn.addEventListener('click', function () {
          var tabId = btn.getAttribute('data-tab');
          switchDashboardTab(tabId, true);
        });
      });
    }

    if (bottomNavItems.length) {
      bottomNavItems.forEach(function (item) {
        item.addEventListener('click', function (e) {
          var tabId = item.getAttribute('data-tab');
          if (document.getElementById('tab-' + tabId)) {
            e.preventDefault();
            switchDashboardTab(tabId, true);
            window.scrollTo({ top: 0, behavior: 'smooth' });
          }
        });
      });
    }

    if (navSwitchers.length) {
      navSwitchers.forEach(function (item) {
        item.addEventListener('click', function (e) {
          var tabId = item.getAttribute('data-tab-switch');
          if (document.getElementById('tab-' + tabId)) {
            e.preventDefault();
            closeDrawer();
            switchDashboardTab(tabId, true);
            window.scrollTo({ top: 0, behavior: 'smooth' });
          }
        });
      });
    }

    // Intercept in-page hash links if they match a dashboard tab
    document.querySelectorAll('a[href^="#"]').forEach(function (anchor) {
      anchor.addEventListener('click', function (e) {
        var hash = anchor.getAttribute('href').substring(1);
        var tabKey = hash.replace(/-section$/, '');
        if (document.getElementById('tab-' + tabKey)) {
          e.preventDefault();
          closeDrawer();
          switchDashboardTab(tabKey, true);
          window.scrollTo({ top: 0, behavior: 'smooth' });
        }
      });
    });

    // Check URL hash on page load
    var currentHash = window.location.hash ? window.location.hash.substring(1) : '';
    var initialTabKey = currentHash.replace(/-section$/, '');
    if (initialTabKey && document.getElementById('tab-' + initialTabKey)) {
      switchDashboardTab(initialTabKey, false);
    } else {
      var urlParams = new URLSearchParams(window.location.search);
      var tabParam = urlParams.get('tab');
      if (tabParam && document.getElementById('tab-' + tabParam)) {
        switchDashboardTab(tabParam, false);
      }
    }

    window.addEventListener('hashchange', function () {
      var hash = window.location.hash ? window.location.hash.substring(1) : '';
      var tabKey = hash.replace(/-section$/, '');
      if (tabKey && document.getElementById('tab-' + tabKey)) {
        switchDashboardTab(tabKey, false);
      }
    });
  });
})();
