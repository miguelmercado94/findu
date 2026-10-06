import './style.css';

const API_GATEWAY_URL = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080';
const DEFAULT_ROLE = 'ROLE_OUR_SOPORTE';

let state = {
  token: localStorage.getItem('findu_soporte_token') || null,
  user: null,
  loginUsername: 'soporte',
  loginPassword: 'Soporte1*',
  tickets: [],
  selectedTicket: null,
  filterStatus: 'ALL',
  filterCategory: 'ALL',
  replyText: '',
  isInternalNote: false,
  alert: { type: '', message: '' },
  loading: false
};

const app = document.getElementById('app');

function showAlert(type, message) {
  state.alert = { type, message };
  render();
}

function clearAlert() {
  state.alert = { type: '', message: '' };
}

async function safeParseJson(res) {
  try {
    const text = await res.text();
    return text ? JSON.parse(text) : {};
  } catch (e) {
    return {};
  }
}

async function handleLogin(e) {
  e.preventDefault();
  state.loading = true;
  render();

  try {
    const res = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        usernameOrEmail: state.loginUsername,
        password: state.loginPassword,
        role: DEFAULT_ROLE
      })
    });

    const data = await safeParseJson(res);
    if (!res.ok || !data.jwt) {
      throw new Error(data.message || 'Credenciales de soporte inválidas.');
    }

    state.token = data.jwt;
    state.user = data.user || { username: state.loginUsername, role: DEFAULT_ROLE };
    localStorage.setItem('findu_soporte_token', data.jwt);
    clearAlert();
    await fetchTickets();
  } catch (err) {
    showAlert('error', err.message);
  } finally {
    state.loading = false;
    render();
  }
}

function logout() {
  localStorage.removeItem('findu_soporte_token');
  state.token = null;
  state.user = null;
  state.tickets = [];
  state.selectedTicket = null;
  render();
}

async function fetchTickets() {
  if (!state.token) return;
  state.loading = true;
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-help-v2/api/v1/tickets`, {
      headers: { 'Authorization': `Bearer ${state.token}` }
    });

    if (res.status === 401) {
      logout();
      return;
    }

    if (res.ok) {
      const data = await safeParseJson(res);
      state.tickets = Array.isArray(data) ? data : (data.content || []);
      if (state.selectedTicket) {
        const updated = state.tickets.find(t => t.id === state.selectedTicket.id || t.ticketNumber === state.selectedTicket.ticketNumber);
        if (updated) state.selectedTicket = updated;
      }
    }
  } catch (err) {
    console.error("Error al cargar tickets:", err);
  } finally {
    state.loading = false;
    render();
  }
}

async function selectTicket(ticket) {
  state.selectedTicket = ticket;
  render();
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-help-v2/api/v1/tickets/${ticket.id || ticket.ticketNumber}`, {
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.ok) {
      state.selectedTicket = await safeParseJson(res);
      render();
    }
  } catch (e) {
    console.error("Error obteniendo detalle de ticket:", e);
  }
}

async function handleSendReply(e) {
  e.preventDefault();
  if (!state.replyText.trim() || !state.selectedTicket) return;

  const ticketId = state.selectedTicket.id || state.selectedTicket.ticketNumber;
  const content = state.replyText.trim();
  const isInternal = state.isInternalNote;

  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-help-v2/api/v1/tickets/${ticketId}/messages`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${state.token}`
      },
      body: JSON.stringify({
        content: content,
        isInternalNote: isInternal,
        senderType: 'AGENT',
        senderName: 'Soporte FindU'
      })
    });

    if (!res.ok) {
      const errData = await safeParseJson(res);
      throw new Error(errData.message || 'Error enviando mensaje');
    }

    state.replyText = '';
    await fetchTickets();
    if (state.selectedTicket) {
      await selectTicket(state.selectedTicket);
    }
    showAlert('success', 'Respuesta enviada correctamente.');
  } catch (err) {
    showAlert('error', err.message);
  }
}

async function handleChangeStatus(newStatus) {
  if (!state.selectedTicket) return;
  const ticketId = state.selectedTicket.id || state.selectedTicket.ticketNumber;

  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-help-v2/api/v1/tickets/${ticketId}/status?status=${newStatus}`, {
      method: 'PUT',
      headers: { 'Authorization': `Bearer ${state.token}` }
    });

    if (res.ok) {
      showAlert('success', `Estado actualizado a ${newStatus}`);
      await fetchTickets();
      if (state.selectedTicket) {
        await selectTicket(state.selectedTicket);
      }
    }
  } catch (err) {
    showAlert('error', 'Error actualizando estado');
  }
}

function render() {
  if (!state.token) {
    app.innerHTML = `
      <div class="auth-container">
        <div class="auth-header">
          <h1>⚡ FIND-U SOPORTE</h1>
          <p>Portal Interno de Atenci&oacute;n al Cliente y Proveedor</p>
        </div>

        ${state.alert.message ? `<div class="alert-toast alert-${state.alert.type}">${state.alert.message}</div>` : ''}

        <form id="login-form">
          <div class="form-group">
            <label>Usuario / Email de Soporte</label>
            <input type="text" id="login-username" class="form-control" value="${state.loginUsername}" required>
          </div>
          <div class="form-group">
            <label>Contrase&ntilde;a</label>
            <input type="password" id="login-password" class="form-control" value="${state.loginPassword}" required>
          </div>
          <button type="submit" class="btn-primary" ${state.loading ? 'disabled' : ''}>
            ${state.loading ? 'Ingresando...' : 'Iniciar Sesi&oacute;n como Agente'}
          </button>
        </form>
      </div>
    `;

    document.getElementById('login-username').addEventListener('input', (e) => state.loginUsername = e.target.value);
    document.getElementById('login-password').addEventListener('input', (e) => state.loginPassword = e.target.value);
    document.getElementById('login-form').addEventListener('submit', handleLogin);
    return;
  }

  const filteredTickets = state.tickets.filter(t => {
    const matchesStatus = state.filterStatus === 'ALL' || t.status === state.filterStatus;
    const matchesCat = state.filterCategory === 'ALL' || t.category === state.filterCategory;
    return matchesStatus && matchesCat;
  });

  app.innerHTML = `
    <header class="navbar">
      <div class="brand">
        ⚡ FIND-U <span>| Portal de Soporte & Atenci&oacute;n</span>
      </div>
      <div class="user-info">
        <span>Agente: <strong>${state.user?.username || 'soporte'}</strong></span>
        <button id="btn-refresh" class="btn-logout">🔄 Actualizar</button>
        <button id="btn-logout" class="btn-logout">Cerrar Sesi&oacute;n</button>
      </div>
    </header>

    <main class="container">
      ${state.alert.message ? `<div class="alert-toast alert-${state.alert.type}">${state.alert.message}</div>` : ''}

      <div class="dashboard-grid">
        <!-- Sidebar Tickets List -->
        <aside class="tickets-sidebar">
          <div class="sidebar-header">
            <h3>Tickets de Ayuda (${filteredTickets.length})</h3>
            <div class="filters-bar">
              <select id="filter-status" class="select-filter">
                <option value="ALL" ${state.filterStatus === 'ALL' ? 'selected' : ''}>Todos los Estados</option>
                <option value="OPEN" ${state.filterStatus === 'OPEN' ? 'selected' : ''}>ABIERTO (OPEN)</option>
                <option value="IN_PROGRESS" ${state.filterStatus === 'IN_PROGRESS' ? 'selected' : ''}>EN PROCESO</option>
                <option value="RESOLVED" ${state.filterStatus === 'RESOLVED' ? 'selected' : ''}>RESUELTO</option>
                <option value="CLOSED" ${state.filterStatus === 'CLOSED' ? 'selected' : ''}>CERRADO</option>
              </select>

              <select id="filter-category" class="select-filter">
                <option value="ALL" ${state.filterCategory === 'ALL' ? 'selected' : ''}>Todas Categor&iacute;as</option>
                <option value="FINANCIERO">FINANCIERO</option>
                <option value="TECNICO">T&Eacute;CNICO</option>
                <option value="SERVICIO">SERVICIO</option>
              </select>
            </div>
          </div>

          <div class="tickets-list">
            ${filteredTickets.length === 0 ? '<div style="padding:2rem;text-align:center;color:#64748b;">No hay tickets registrados</div>' : ''}
            ${filteredTickets.map(t => `
              <div class="ticket-item ${state.selectedTicket?.id === t.id ? 'active' : ''}" data-id="${t.id}">
                <div class="ticket-item-header">
                  <span class="ticket-code">#${t.ticketNumber || t.id?.substring(0,8)}</span>
                  <span class="badge-status badge-${(t.status || 'open').toLowerCase()}">${t.status}</span>
                </div>
                <div class="ticket-subject">${t.subject}</div>
                <div class="ticket-meta">
                  <span>${t.userRole || 'USUARIO'} (${t.category})</span>
                  <span>${t.createdAt ? new Date(t.createdAt).toLocaleDateString() : ''}</span>
                </div>
              </div>
            `).join('')}
          </div>
        </aside>

        <!-- Main Ticket Detail & Conversation -->
        <section class="ticket-detail-panel">
          ${!state.selectedTicket ? `
            <div style="flex:1;display:flex;align-items:center;justify-content:center;color:#64748b;">
              Selecciona un ticket de la lista para ver la conversaci&oacute;n y responder.
            </div>
          ` : `
            <div class="detail-header">
              <div class="detail-title">
                <h2>${state.selectedTicket.subject}</h2>
                <div class="detail-submeta">
                  Ticket #${state.selectedTicket.ticketNumber || state.selectedTicket.id} | Rol: <strong>${state.selectedTicket.userRole || 'CLIENTE/PROVEEDOR'}</strong> | Categ: <strong>${state.selectedTicket.category}</strong>
                </div>
              </div>

              <div class="detail-actions">
                <select id="change-status-select" class="select-filter">
                  <option value="OPEN" ${state.selectedTicket.status === 'OPEN' ? 'selected' : ''}>ABIERTO</option>
                  <option value="IN_PROGRESS" ${state.selectedTicket.status === 'IN_PROGRESS' ? 'selected' : ''}>EN PROCESO</option>
                  <option value="RESOLVED" ${state.selectedTicket.status === 'RESOLVED' ? 'selected' : ''}>RESUELTO</option>
                  <option value="CLOSED" ${state.selectedTicket.status === 'CLOSED' ? 'selected' : ''}>CERRADO</option>
                </select>
              </div>
            </div>

            <div class="chat-messages" id="chat-messages-container">
              <!-- Mensaje Inicial -->
              <div class="message-bubble message-user">
                <div class="message-header-info">
                  <span>${state.selectedTicket.userName || 'Usuario'} (${state.selectedTicket.userRole || 'USUARIO'})</span>
                  <span>${state.selectedTicket.createdAt ? new Date(state.selectedTicket.createdAt).toLocaleString() : ''}</span>
                </div>
                <div>${state.selectedTicket.description || 'Sin descripción'}</div>
              </div>

              <!-- Respuestas e Interacciones -->
              ${(state.selectedTicket.messages || []).map(m => `
                <div class="message-bubble ${m.isInternalNote ? 'message-internal' : (m.senderType === 'AGENT' ? 'message-support' : 'message-user')}">
                  <div class="message-header-info">
                    <span>${m.isInternalNote ? '📌 NOTA INTERNA DE AGENTE' : (m.senderName || m.senderType)}</span>
                    <span>${m.createdAt ? new Date(m.createdAt).toLocaleString() : ''}</span>
                  </div>
                  <div>${m.content}</div>
                </div>
              `).join('')}
            </div>

            <form class="chat-input-area" id="reply-form">
              <div class="input-toolbar">
                <label class="checkbox-label">
                  <input type="checkbox" id="chk-internal" ${state.isInternalNote ? 'checked' : ''}>
                  Nota Interna (visible solo para agentes de soporte)
                </label>
              </div>

              <div class="input-box-row">
                <textarea id="reply-text" placeholder="Escribe tu respuesta para el cliente o proveedor..." required>${state.replyText}</textarea>
                <button type="submit" class="btn-send">Enviar</button>
              </div>
            </form>
          `}
        </section>
      </div>
    </main>
  `;

  document.getElementById('btn-logout').addEventListener('click', logout);
  document.getElementById('btn-refresh').addEventListener('click', fetchTickets);

  document.getElementById('filter-status').addEventListener('change', (e) => {
    state.filterStatus = e.target.value;
    render();
  });

  document.getElementById('filter-category').addEventListener('change', (e) => {
    state.filterCategory = e.target.value;
    render();
  });

  document.querySelectorAll('.ticket-item').forEach(item => {
    item.addEventListener('click', () => {
      const id = item.getAttribute('data-id');
      const found = state.tickets.find(t => t.id === id);
      if (found) selectTicket(found);
    });
  });

  if (state.selectedTicket) {
    const statusSelect = document.getElementById('change-status-select');
    if (statusSelect) {
      statusSelect.addEventListener('change', (e) => handleChangeStatus(e.target.value));
    }

    const replyForm = document.getElementById('reply-form');
    if (replyForm) {
      document.getElementById('reply-text').addEventListener('input', (e) => state.replyText = e.target.value);
      document.getElementById('chk-internal').addEventListener('change', (e) => state.isInternalNote = e.target.checked);
      replyForm.addEventListener('submit', handleSendReply);
    }

    const chatContainer = document.getElementById('chat-messages-container');
    if (chatContainer) {
      chatContainer.scrollTop = chatContainer.scrollHeight;
    }
  }
}

fetchTickets();
render();
