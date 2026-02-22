// ══════════════════════════════════════════════════════════════
// CONFIG & STATE
// ══════════════════════════════════════════════════════════════
const API = '/api';
let token        = sessionStorage.getItem('fg_token');
let currentUser  = JSON.parse(sessionStorage.getItem('fg_user') || 'null');
let currentPage  = 'dashboard';
let alertPage    = 0;
let alertTotal   = 0;
let resolveAlertId = null;
let trendChart   = null;
let refreshTimer = null;

// ══════════════════════════════════════════════════════════════
// BOOTSTRAP
// ══════════════════════════════════════════════════════════════
function init() {
  startClock();
  if (token) showApp();
  else       showLogin();
  document.addEventListener('keydown', e => { if (e.key === 'Escape') closeAllModals(); });
}

// ══════════════════════════════════════════════════════════════
// CLOCK
// ══════════════════════════════════════════════════════════════
function startClock() {
  const el = document.getElementById('live-clock');
  const tick = () => { el.textContent = new Date().toLocaleTimeString(); };
  tick();
  setInterval(tick, 1000);
}

// ══════════════════════════════════════════════════════════════
// API HELPER
// ══════════════════════════════════════════════════════════════
async function apiFetch(method, path, body) {
  const opts = {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) }
  };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(API + path, opts);
  if (res.status === 401) { doLogout(); return null; }
  if (res.status === 204) return {};
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    const msg = data.message
      || (data.errors && data.errors[0] && data.errors[0].defaultMessage)
      || data.error
      || ('Request failed (' + res.status + ')');
    throw new Error(msg);
  }
  return data;
}

// ══════════════════════════════════════════════════════════════
// AUTH
// ══════════════════════════════════════════════════════════════
function switchAuthTab(tab) {
  document.getElementById('form-login').classList.toggle('hidden', tab !== 'login');
  document.getElementById('form-register').classList.toggle('hidden', tab !== 'register');
  document.getElementById('tab-login').classList.toggle('active', tab === 'login');
  document.getElementById('tab-register').classList.toggle('active', tab !== 'login');
}

async function doLogin() {
  const username = document.getElementById('login-username').value.trim();
  const password = document.getElementById('login-password').value;
  if (!username || !password) return toast('Enter username and password', 'error');
  try {
    const data = await apiFetch('POST', '/auth/login', { username, password });
    if (!data) return;
    token = data.token;
    currentUser = { username: data.username, role: data.role };
    sessionStorage.setItem('fg_token', token);
    sessionStorage.setItem('fg_user', JSON.stringify(currentUser));
    showApp();
    toast('Welcome back, ' + data.username + '!', 'success');
  } catch (e) { toast(e.message, 'error'); }
}

async function doRegister() {
  const username = document.getElementById('reg-username').value.trim();
  const password = document.getElementById('reg-password').value;
  const role     = document.getElementById('reg-role').value;
  if (!username || !password) return toast('Fill in all fields', 'error');
  try {
    await apiFetch('POST', '/auth/register', { username, password, role });
    toast('Account created! Please sign in.', 'success');
    switchAuthTab('login');
    document.getElementById('login-username').value = username;
  } catch (e) { toast(e.message, 'error'); }
}

function doLogout() {
  token = null; currentUser = null;
  sessionStorage.clear();
  clearInterval(refreshTimer);
  showLogin();
}

function showLogin() {
  document.getElementById('login-page').classList.remove('hidden');
  document.getElementById('main-app').classList.add('hidden');
}

function showApp() {
  document.getElementById('login-page').classList.add('hidden');
  document.getElementById('main-app').classList.remove('hidden');
  document.getElementById('sidebar-username').textContent = currentUser?.username || '—';
  document.getElementById('sidebar-role').textContent     = currentUser?.role    || '—';
  document.getElementById('user-avatar').textContent      = (currentUser?.username || 'U')[0].toUpperCase();
  navigate('dashboard');
  startAutoRefresh();
}

// ══════════════════════════════════════════════════════════════
// NAVIGATION
// ══════════════════════════════════════════════════════════════
const PAGE_META = {
  dashboard: { title: 'Dashboard',         sub: 'Real-time fleet alert overview' },
  alerts:    { title: 'Alert Management',  sub: 'Browse, filter, ingest and resolve alerts' },
  rules:     { title: 'Escalation Rules',  sub: 'Configure rule-based escalation logic' }
};

function navigate(page) {
  currentPage = page;
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.getElementById('page-' + page).classList.add('active');
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  document.getElementById('nav-' + page)?.classList.add('active');
  const m = PAGE_META[page] || {};
  document.getElementById('page-title').textContent    = m.title || '';
  document.getElementById('page-subtitle').textContent = m.sub   || '';
  if (page === 'dashboard') loadDashboard();
  if (page === 'alerts')    loadAlerts(0);
  if (page === 'rules')     loadRules();
}

function refreshCurrentPage() { navigate(currentPage); }

function startAutoRefresh() {
  clearInterval(refreshTimer);
  refreshTimer = setInterval(() => {
    if (currentPage === 'dashboard') loadDashboard();
  }, 30000);
}

// ══════════════════════════════════════════════════════════════
// DASHBOARD
// ══════════════════════════════════════════════════════════════
async function loadDashboard() {
  try {
    const [overview, trends, offenders, events] = await Promise.all([
      apiFetch('GET', '/dashboard/overview'),
      apiFetch('GET', '/dashboard/trends?days=7'),
      apiFetch('GET', '/dashboard/top-offenders?limit=5'),
      apiFetch('GET', '/dashboard/recent-events?limit=10')
    ]);
    if (overview)  renderOverview(overview);
    if (trends)    renderTrendChart(trends);
    if (offenders) renderTopOffenders(offenders);
    if (events)    renderRecentEvents(events);
  } catch (e) { toast('Dashboard load failed: ' + e.message, 'error'); }
}

function renderOverview(d) {
  document.getElementById('stat-open').textContent      = d.open      ?? 0;
  document.getElementById('stat-escalated').textContent = d.escalated ?? 0;
  document.getElementById('stat-critical').textContent  = d.criticalCount ?? 0;
  document.getElementById('stat-resolved').textContent  = d.resolved  ?? 0;
  const badge = document.getElementById('nav-open-count');
  const openCnt = d.open ?? 0;
  badge.style.display = openCnt > 0 ? '' : 'none';
  badge.textContent = openCnt;
}

function renderTrendChart(trends) {
  const daily = trends.dailyAlerts || [];
  const esc   = trends.dailyEscalations || [];
  const labels = [...new Set([...daily.map(r => r.date), ...esc.map(r => r.date)])].sort();
  const toMap  = arr => Object.fromEntries(arr.map(r => [r.date, r.count]));
  const dMap   = toMap(daily);
  const eMap   = toMap(esc);

  const ctx = document.getElementById('trend-chart').getContext('2d');
  if (trendChart) trendChart.destroy();
  trendChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: 'New Alerts',
          data: labels.map(d => dMap[d] || 0),
          borderColor: '#6366f1', backgroundColor: 'rgba(99,102,241,.15)',
          fill: true, tension: .4, pointRadius: 4, pointBackgroundColor: '#6366f1'
        },
        {
          label: 'Escalations',
          data: labels.map(d => eMap[d] || 0),
          borderColor: '#ef4444', backgroundColor: 'rgba(239,68,68,.10)',
          fill: true, tension: .4, pointRadius: 4, pointBackgroundColor: '#ef4444'
        }
      ]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { labels: { color: '#94a3b8', font: { size: 12 } } } },
      scales: {
        x: { grid: { color: 'rgba(255,255,255,.04)' }, ticks: { color: '#475569', font: { size: 11 } } },
        y: { grid: { color: 'rgba(255,255,255,.04)' }, ticks: { color: '#475569', font: { size: 11 } }, beginAtZero: true }
      }
    }
  });
}

function renderTopOffenders(list) {
  const el = document.getElementById('top-offenders-list');
  if (!list || list.length === 0) {
    el.innerHTML = '<div class="empty-state"><i class="fa fa-user-check"></i><p>No open alerts by driver</p></div>';
    return;
  }
  const max = Math.max(...list.map(o => o.openAlertCount));
  el.innerHTML = list.map((o, i) => `
    <div class="offender-item">
      <div class="offender-rank ${i === 0 ? 'top' : ''}">${i + 1}</div>
      <div class="offender-id"><code>${o.driverId}</code></div>
      <div class="offender-bar-wrap">
        <div class="offender-bar" style="width:${Math.round((o.openAlertCount / max) * 100)}%"></div>
      </div>
      <div class="offender-count">${o.openAlertCount} alert${o.openAlertCount !== 1 ? 's' : ''}</div>
    </div>`).join('');
}

function renderRecentEvents(events) {
  const tbody = document.getElementById('recent-events-body');
  if (!events || events.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6" class="empty-state" style="padding:30px;text-align:center"><i class="fa fa-inbox" style="margin-right:8px"></i>No recent events</td></tr>';
    return;
  }
  tbody.innerHTML = events.map(a => `
    <tr onclick="viewAlert('${a.id}')">
      <td><code>${a.alertId}</code></td>
      <td>${a.sourceType}</td>
      <td>${severityBadge(a.severity)}</td>
      <td>${statusBadge(a.status)}</td>
      <td>${a.metadata?.driverId ? '<code>' + a.metadata.driverId + '</code>' : '<span class="text-muted">—</span>'}</td>
      <td class="text-sm" style="color:var(--text-3)">${fmtDate(a.updatedAt || a.createdAt)}</td>
    </tr>`).join('');
}

// ══════════════════════════════════════════════════════════════
// ALERTS
// ══════════════════════════════════════════════════════════════
async function loadAlerts(page) {
  if (page !== undefined) alertPage = page;
  const status   = document.getElementById('filter-status').value;
  const severity = document.getElementById('filter-severity').value;
  let path = `/alerts?page=${alertPage}&size=10`;
  if (status)   path += '&status=' + status;
  if (severity) path += '&severity=' + severity;
  try {
    const data = await apiFetch('GET', path);
    if (!data) return;
    alertTotal = data.totalElements || 0;
    document.getElementById('alerts-count-label').textContent =
      alertTotal + ' alert' + (alertTotal !== 1 ? 's' : '') + ' found';
    renderAlertsTable(data.content || []);
    renderPagination(data);
  } catch (e) { toast('Failed to load alerts: ' + e.message, 'error'); }
}

function renderAlertsTable(alerts) {
  const tbody = document.getElementById('alerts-table-body');
  if (!alerts.length) {
    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:40px;color:var(--text-3)"><i class="fa fa-inbox" style="margin-right:8px"></i>No alerts match filters</td></tr>';
    return;
  }
  tbody.innerHTML = alerts.map(a => {
    const meta = a.metadata || {};
    const canResolve = ['OPEN','ESCALATED'].includes(a.status);
    return `
    <tr onclick="viewAlert('${a.id}')">
      <td><code>${a.alertId}</code></td>
      <td style="color:var(--text-3)">${a.sourceType}</td>
      <td>${severityBadge(a.severity)}</td>
      <td>${statusBadge(a.status)}</td>
      <td>${meta.driverId ? '<code>' + meta.driverId + '</code>' : '<span class="text-muted">—</span>'}</td>
      <td class="text-sm" style="color:var(--text-3)">${fmtDate(a.timestamp)}</td>
      <td onclick="event.stopPropagation()" style="white-space:nowrap">
        <button class="btn btn-ghost btn-sm" onclick="viewAlert('${a.id}')">
          <i class="fa fa-eye"></i>
        </button>
        ${canResolve ? `<button class="btn btn-success btn-sm" style="margin-left:4px" onclick="openResolveModal('${a.id}')">
          <i class="fa fa-check"></i>
        </button>` : ''}
      </td>
    </tr>`;
  }).join('');
}

function renderPagination(page) {
  const el = document.getElementById('alerts-pagination');
  const total = page.totalPages || 0;
  const cur   = page.number || 0;
  if (total <= 1) { el.innerHTML = ''; return; }
  let html = `<span class="page-info">Page ${cur + 1} of ${total}</span>`;
  html += `<button class="page-btn" ${cur === 0 ? 'disabled' : ''} onclick="loadAlerts(${cur - 1})"><i class="fa fa-chevron-left"></i></button>`;
  for (let i = Math.max(0, cur - 2); i <= Math.min(total - 1, cur + 2); i++) {
    html += `<button class="page-btn ${i === cur ? 'active' : ''}" onclick="loadAlerts(${i})">${i + 1}</button>`;
  }
  html += `<button class="page-btn" ${cur >= total - 1 ? 'disabled' : ''} onclick="loadAlerts(${cur + 1})"><i class="fa fa-chevron-right"></i></button>`;
  el.innerHTML = html;
}

// ── Alert Detail ──────────────────────────────────────────────
async function viewAlert(id) {
  try {
    const a = await apiFetch('GET', '/alerts/' + id);
    if (!a) return;
    document.getElementById('detail-modal-title').textContent = 'Alert: ' + a.alertId;
    const canResolve = ['OPEN','ESCALATED'].includes(a.status);
    document.getElementById('detail-resolve-btn').style.display = canResolve ? '' : 'none';
    resolveAlertId = a.id;

    // Meta info grid
    const meta = [
      ['Alert ID', `<code>${a.alertId}</code>`],
      ['Source',   a.sourceType],
      ['Severity', severityBadge(a.severity)],
      ['Status',   statusBadge(a.status)],
      ['Timestamp', fmtDate(a.timestamp)],
      ['Created',   fmtDate(a.createdAt)]
    ];
    document.getElementById('detail-meta').innerHTML = meta.map(([k,v]) =>
      `<div class="meta-item"><div class="meta-key">${k}</div><div class="meta-val">${v}</div></div>`).join('');

    // Payload
    const payload = a.metadata || {};
    const payloadEntries = Object.entries(payload);
    document.getElementById('detail-payload').innerHTML = payloadEntries.length
      ? payloadEntries.map(([k,v]) =>
          `<div class="meta-item"><div class="meta-key">${k}</div><div class="meta-val"><code>${v}</code></div></div>`).join('')
      : '<div class="text-muted" style="padding:8px">No metadata available</div>';

    // History timeline
    const hist = a.history || [];
    document.getElementById('detail-history').innerHTML = hist.length === 0
      ? '<p class="text-muted">No history entries</p>'
      : hist.map(h => `
        <div class="tl-item">
          <div class="tl-dot">${h.toStatus === 'OPEN' ? '○' : h.toStatus === 'ESCALATED' ? '⬆' : h.toStatus === 'RESOLVED' ? '✓' : '⊙'}</div>
          <div class="tl-content">
            <div class="tl-title">
              ${h.fromStatus ? h.fromStatus + ' → ' + h.toStatus : h.toStatus}
              ${h.toStatus === 'ESCALATED' ? '&nbsp;' + severityBadge('CRITICAL') : ''}
            </div>
            <div class="tl-meta">${h.triggeredBy} &nbsp;·&nbsp; ${fmtDate(h.changedAt)}</div>
            ${h.notes ? `<div class="tl-meta" style="margin-top:2px;font-style:italic">"${h.notes}"</div>` : ''}
          </div>
        </div>`).join('');
    openModal('modal-detail-overlay');
  } catch(e) { toast('Could not load alert: ' + e.message, 'error'); }
}

// ── Resolve from detail modal ─────────────────────────────────
function resolveCurrentAlert() {
  closeModal('modal-detail-overlay');
  openResolveModal(resolveAlertId);
}

function openResolveModal(id) {
  resolveAlertId = id;
  document.getElementById('resolve-notes').value = '';
  openModal('modal-resolve');
}

async function submitResolve() {
  const notes = document.getElementById('resolve-notes').value;
  try {
    await apiFetch('PUT', '/alerts/' + resolveAlertId + '/resolve', { resolutionNotes: notes });
    toast('Alert resolved successfully', 'success');
    closeAllModals();
    if (currentPage === 'alerts')    loadAlerts();
    if (currentPage === 'dashboard') loadDashboard();
  } catch(e) { toast(e.message, 'error'); }
}

// ── Ingest New Alert ─────────────────────────────────────────
async function submitIngestAlert() {
  const alertId  = document.getElementById('ing-alert-id').value.trim();
  const source   = document.getElementById('ing-source').value;
  const severity = document.getElementById('ing-severity').value;
  const driverId = document.getElementById('ing-driver-id').value.trim();
  const speed    = document.getElementById('ing-speed').value.trim();
  if (!alertId) return toast('Alert ID is required', 'error');
  const metadata = {};
  if (driverId) metadata.driverId = driverId;
  if (speed)    metadata.speed    = parseInt(speed) || speed;
  try {
    await apiFetch('POST', '/alerts', {
      alertId, sourceType: source, severity,
      timestamp: new Date().toISOString(), metadata
    });
    toast('Alert ingested: ' + alertId, 'success');
    closeModal('modal-ingest');
    document.getElementById('ing-alert-id').value = '';
    document.getElementById('ing-driver-id').value = '';
    document.getElementById('ing-speed').value = '';
    if (currentPage === 'alerts')    loadAlerts(0);
    if (currentPage === 'dashboard') loadDashboard();
  } catch(e) { toast(e.message, 'error'); }
}

// ══════════════════════════════════════════════════════════════
// RULES
// ══════════════════════════════════════════════════════════════
async function loadRules() {
  try {
    const rules = await apiFetch('GET', '/rules');
    renderRulesTable(rules || []);
  } catch(e) { toast('Failed to load rules: ' + e.message, 'error'); }
}

function renderRulesTable(rules) {
  const tbody = document.getElementById('rules-table-body');
  if (!rules.length) {
    tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:40px;color:var(--text-3)"><i class="fa fa-cogs" style="margin-right:8px"></i>No rules configured yet</td></tr>';
    return;
  }
  tbody.innerHTML = rules.map(r => `
    <tr>
      <td><span style="font-weight:600;color:var(--text)">${r.name}</span><br/><span class="text-muted">${r.description || ''}</span></td>
      <td><code>${r.targetSourceType}</code></td>
      <td><span class="badge badge-info">${r.conditionType}</span></td>
      <td><span style="font-weight:700;color:${r.action==='ESCALATE'?'var(--purple)':'var(--cyan)'}">${r.thresholdCount}×</span></td>
      <td>${r.timeWindowMinutes}m</td>
      <td><span class="badge ${r.action==='ESCALATE'?'badge-escalated':'badge-auto_closed'}">${r.action}</span></td>
      <td onclick="event.stopPropagation()">
        <label class="toggle"><input type="checkbox" ${r.isActive?'checked':''} onchange="toggleRule('${r.id}',${r.isActive})"/><span class="toggle-slider"></span></label>
      </td>
      <td onclick="event.stopPropagation()" style="white-space:nowrap">
        <button class="btn btn-ghost btn-sm" onclick="openEditRuleModal(${JSON.stringify(r).replace(/"/g,'&quot;')})">
          <i class="fa fa-pen"></i>
        </button>
        <button class="btn btn-danger btn-sm" style="margin-left:4px" onclick="deleteRule('${r.id}','${r.name}')">
          <i class="fa fa-trash"></i>
        </button>
      </td>
    </tr>`).join('');
}

function openCreateRuleModal() {
  document.getElementById('rule-modal-title').textContent = 'Create Escalation Rule';
  document.getElementById('rule-edit-id').value = '';
  document.getElementById('rule-name').value = '';
  document.getElementById('rule-source').value = 'TELEMATICS';
  document.getElementById('rule-action').value = 'ESCALATE';
  document.getElementById('rule-condition').value = 'FREQUENCY';
  document.getElementById('rule-groupby').value = 'driverId';
  document.getElementById('rule-threshold').value = '3';
  document.getElementById('rule-window').value = '60';
  document.getElementById('rule-escalation-sev').value = 'CRITICAL';
  document.getElementById('rule-desc').value = '';
  openModal('modal-rule');
}

function openEditRuleModal(r) {
  document.getElementById('rule-modal-title').textContent = 'Edit Rule';
  document.getElementById('rule-edit-id').value = r.id;
  document.getElementById('rule-name').value = r.name;
  document.getElementById('rule-source').value = r.targetSourceType;
  document.getElementById('rule-action').value = r.action;
  document.getElementById('rule-condition').value = r.conditionType;
  document.getElementById('rule-groupby').value = r.groupByMetadataKey || 'driverId';
  document.getElementById('rule-threshold').value = r.thresholdCount;
  document.getElementById('rule-window').value = r.timeWindowMinutes;
  document.getElementById('rule-escalation-sev').value = r.escalationSeverity || '';
  document.getElementById('rule-desc').value = r.description || '';
  openModal('modal-rule');
}

async function submitRule() {
  const id   = document.getElementById('rule-edit-id').value;
  const name = document.getElementById('rule-name').value.trim();
  if (!name) return toast('Rule name is required', 'error');
  const body = {
    name,
    description:       document.getElementById('rule-desc').value,
    targetSourceType:  document.getElementById('rule-source').value,
    action:            document.getElementById('rule-action').value,
    conditionType:     document.getElementById('rule-condition').value,
    groupByMetadataKey:document.getElementById('rule-groupby').value,
    thresholdCount:    parseInt(document.getElementById('rule-threshold').value),
    timeWindowMinutes: parseInt(document.getElementById('rule-window').value),
    escalationSeverity:document.getElementById('rule-escalation-sev').value || null,
    isActive: true
  };
  try {
    if (id) await apiFetch('PUT',  '/rules/' + id, body);
    else    await apiFetch('POST', '/rules', body);
    toast(id ? 'Rule updated' : 'Rule created', 'success');
    closeModal('modal-rule');
    loadRules();
  } catch(e) { toast(e.message, 'error'); }
}

async function toggleRule(id, current) {
  try {
    await apiFetch('PATCH', '/rules/' + id + '/toggle', { isActive: !current });
    toast('Rule ' + (!current ? 'enabled' : 'disabled'), 'info');
    loadRules();
  } catch(e) { toast(e.message, 'error'); }
}

async function deleteRule(id, name) {
  if (!confirm('Delete rule "' + name + '"?')) return;
  try {
    await apiFetch('DELETE', '/rules/' + id);
    toast('Rule deleted', 'success');
    loadRules();
  } catch(e) { toast(e.message, 'error'); }
}

// ══════════════════════════════════════════════════════════════
// MODALS
// ══════════════════════════════════════════════════════════════
function openModal(id) {
  document.getElementById(id).classList.add('open');
}
function closeModal(id) {
  document.getElementById(id).classList.remove('open');
}
function closeAllModals() {
  document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('open'));
}
function handleOverlayClick(e, id) {
  if (e.target === document.getElementById(id)) closeModal(id);
}

// ══════════════════════════════════════════════════════════════
// TOAST
// ══════════════════════════════════════════════════════════════
function toast(msg, type = 'info') {
  const icons = { success: 'fa-check-circle', error: 'fa-exclamation-circle', info: 'fa-info-circle' };
  const el = document.createElement('div');
  el.className = 'toast toast-' + type;
  el.innerHTML = `<i class="fa ${icons[type] || icons.info}"></i> ${msg}`;
  document.getElementById('toasts').appendChild(el);
  setTimeout(() => { el.style.opacity = '0'; el.style.transition = '.3s'; setTimeout(() => el.remove(), 300); }, 3500);
}

// ══════════════════════════════════════════════════════════════
// BADGE & DATE HELPERS
// ══════════════════════════════════════════════════════════════
function severityBadge(s) {
  const cls = { INFO: 'badge-info', WARNING: 'badge-warning', CRITICAL: 'badge-critical' };
  return `<span class="badge ${cls[s] || 'badge-info'}">${s}</span>`;
}
function statusBadge(s) {
  const key = (s || '').toLowerCase();
  return `<span class="badge badge-${key}">${s}</span>`;
}
function fmtDate(iso) {
  if (!iso) return '—';
  try {
    const d = new Date(typeof iso === 'number' ? iso * 1000 : iso);
    return d.toLocaleDateString() + ' ' + d.toLocaleTimeString();
  } catch { return iso; }
}

// ══════════════════════════════════════════════════════════════
// START
// ══════════════════════════════════════════════════════════════
init();
