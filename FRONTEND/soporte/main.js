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
  loading: false,
  wsConnected: false
};

const app = document.getElementById('app');
let ws = null;
let wsReconnectTimeout = null;

function showAlert(type, message) {
  state.alert = { type, message };
  render();
  setTimeout(() => {
    if (state.alert.message === message) {
      clearAlert();
    }
  }, 5000);
}

function clearAlert() {
  state.alert = { type: '', message: '' };
  render();
}

async function safeParseJson(res) {
  try {
    const text = await res.text();
    return text ? JSON.parse(text) : {};
  } catch (e) {
    return {};
  }
}

// ==========================================
// AUTENTICACIÓN
// ==========================================
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
    state.user = data.user || { id: 1, username: state.loginUsername, role: DEFAULT_ROLE, email: 'soporte@findu.com' };
    localStorage.setItem('findu_soporte_token', data.jwt);
    clearAlert();
    await fetchTickets();
    connectWebSocket();
  } catch (err) {
    showAlert('error', err.message);
  } finally {
    state.loading = false;
    render();
  }
}

function logout() {
  if (ws) {
    try { ws.close(); } catch (e) {}
    ws = null;
  }
  clearTimeout(wsReconnectTimeout);
  localStorage.removeItem('findu_soporte_token');
  state.token = null;
  state.user = null;
  state.tickets = [];
  state.selectedTicket = null;
  state.wsConnected = false;
  render();
}

// ==========================================
// WEBSOCKET EN TIEMPO REAL
// ==========================================
function connectWebSocket() {
  if (!state.token) return;
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) return;

  const wsBase = API_GATEWAY_URL.replace(/^http/, 'ws');
  // Conectar al canal general de soporte y presence
  const wsUrl = `${wsBase}/ws/events?userId=SOPORTE_GENERAL&token=${encodeURIComponent(state.token)}`;

  try {
    ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      console.log("WebSocket conectado al portal de soporte:", wsUrl);
      state.wsConnected = true;
      render();
    };

    ws.onmessage = (event) => {
      try {
        const envelope = JSON.parse(event.data);
        handleRealtimeEvent(envelope);
      } catch (err) {
        console.error("Error al parsear mensaje WebSocket:", err);
      }
    };

    ws.onclose = () => {
      console.warn("WebSocket cerrado. Reintentando reconexión en 5s...");
      state.wsConnected = false;
      render();
      clearTimeout(wsReconnectTimeout);
      wsReconnectTimeout = setTimeout(connectWebSocket, 5000);
    };

    ws.onerror = (err) => {
      console.error("Error en WebSocket de soporte:", err);
    };
  } catch (e) {
    console.error("No se pudo inicializar WebSocket:", e);
  }
}

function handleRealtimeEvent(envelope) {
  if (!envelope) return;

  // Manejo de heartbeat PING
  if (envelope.eventType === 'PING') {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ eventType: 'PONG' }));
    }
    return;
  }

  const payload = envelope.payload || {};
  console.log("Evento en tiempo real recibido:", envelope.eventType, payload);

  if (envelope.eventType === 'TICKET_CREATED') {
    const code = payload.ticketCode || payload.ticketId || '';
    const subject = payload.subject || '';
    showAlert('success', `⚡ Nuevo ticket creado #${code}: ${subject}`);
    fetchTickets();
  } else if (envelope.eventType === 'CHAT_MESSAGE') {
    const msgTicketCode = payload.ticketCode || payload.ticketId;
    const currentTicketCode = state.selectedTicket?.ticketCode || state.selectedTicket?.id;

    if (state.selectedTicket && msgTicketCode === currentTicketCode) {
      // Si el ticket abierto coincide, incorporar mensaje al chat de inmediato
      const newMsg = {
        id: payload.messageId || 'MSG-' + Date.now(),
        senderId: payload.senderId,
        senderType: payload.senderType || 'USUARIO',
        senderName: payload.senderName || 'Usuario',
        content: payload.content,
        isInternalNote: Boolean(payload.isInternalNote),
        createdAt: payload.occurredOn || new Date().toISOString()
      };

      if (!state.selectedTicket.messages) {
        state.selectedTicket.messages = [];
      }
      const exists = state.selectedTicket.messages.some(m => m.id === newMsg.id);
      if (!exists) {
        state.selectedTicket.messages.push(newMsg);
        render();
      }
    } else {
      showAlert('info', `💬 Nuevo mensaje en ticket #${msgTicketCode} de ${payload.senderName || 'Usuario'}`);
      fetchTickets();
    }
  } else if (envelope.eventType === 'TICKET_ASSIGNED' || envelope.eventType === 'SERVICE_UPDATE' || envelope.eventType === 'TICKET_CLOSED') {
    fetchTickets();
    if (state.selectedTicket && (state.selectedTicket.ticketCode === payload.ticketCode || state.selectedTicket.id === payload.ticketId)) {
      selectTicket(state.selectedTicket);
    }
  }
}

// ==========================================
// CONSUMO DE TICKETS (findu-help-v2)
// ==========================================
async function fetchTickets() {
  if (!state.token) return;
  state.loading = true;
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-help-v2/api/v1/help/tickets`, {
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
        const updated = state.tickets.find(t => 
          (t.ticketCode && t.ticketCode === state.selectedTicket.ticketCode) || 
          (t.id && t.id === state.selectedTicket.id)
        );
        if (updated) {
          state.selectedTicket = { ...state.selectedTicket, ...updated };
        }
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
  const ticketRef = ticket.ticketCode || ticket.id;
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-help-v2/api/v1/help/tickets/${ticketRef}`, {
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

async function handleTakeTicket() {
  if (!state.selectedTicket) return;
  await handleChangeStatus('ASIGNADO');
}

async function handleChangeStatus(newStatus) {
  if (!state.selectedTicket) return;
  const ticketRef = state.selectedTicket.ticketCode || state.selectedTicket.id;

  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-help-v2/api/v1/help/tickets/${ticketRef}/status`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${state.token}`
      },
      body: JSON.stringify({
        newStatus: newStatus,
        agentId: state.user?.id || 1,
        agentName: state.user?.username || 'soporte',
        agentEmail: state.user?.email || 'soporte@findu.com',
        reason: 'Actualización de estado en portal de soporte'
      })
    });

    if (res.ok) {
      showAlert('success', `Estado de ticket #${ticketRef} actualizado a: ${newStatus}`);
      await fetchTickets();
      if (state.selectedTicket) {
        await selectTicket(state.selectedTicket);
      }
    } else {
      const errData = await safeParseJson(res);
      throw new Error(errData.message || 'Error actualizando estado del ticket');
    }
  } catch (err) {
    showAlert('error', err.message || 'Error actualizando estado');
  }
}

async function handleSendReply(e) {
  e.preventDefault();
  if (!state.replyText.trim() || !state.selectedTicket) return;

  const ticketRef = state.selectedTicket.ticketCode || state.selectedTicket.id;
  const content = state.replyText.trim();
  const isInternal = state.isInternalNote;

  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-help-v2/api/v1/help/tickets/${ticketRef}/messages`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${state.token}`
      },
      body: JSON.stringify({
        senderId: state.user?.id || 1,
        senderType: 'AGENTE_INTERNO',
        senderName: state.user?.username ? `Soporte (${state.user.username})` : 'Soporte FindU',
        content: content,
        isInternalNote: isInternal
      })
    });

    if (!res.ok) {
      const errData = await safeParseJson(res);
      throw new Error(errData.message || 'Error enviando mensaje');
    }

    state.replyText = '';
    showAlert('success', 'Mensaje enviado correctamente al ticket.');
    await fetchTickets();
    if (state.selectedTicket) {
      await selectTicket(state.selectedTicket);
    }
  } catch (err) {
    showAlert('error', err.message);
  }
}

// ==========================================
// RENDERIZADO UI
// ==========================================
function render() {
  if (!state.token) {
    app.innerHTML = `
      <div class="auth-container">
        <div class="auth-header">
          <h1>⚡ FIND-U SOPORTE</h1>
          <p>Portal Interno de Atención al Cliente y Proveedor</p>
        </div>

        ${state.alert.message ? `<div class="alert-toast alert-${state.alert.type}">${state.alert.message}</div>` : ''}

        <form id="login-form">
          <div class="form-group">
            <label>Usuario / Email de Soporte</label>
            <input type="text" id="login-username" class="form-control" value="${state.loginUsername}" required>
          </div>
          <div class="form-group">
            <label>Contraseña</label>
            <input type="password" id="login-password" class="form-control" value="${state.loginPassword}" required>
          </div>
          <button type="submit" class="btn-primary" ${state.loading ? 'disabled' : ''}>
            ${state.loading ? 'Ingresando...' : 'Iniciar Sesión como Agente'}
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
    const cat = t.categoryName || t.category || '';
    const matchesCat = state.filterCategory === 'ALL' || cat.toUpperCase().includes(state.filterCategory);
    return matchesStatus && matchesCat;
  });

  app.innerHTML = `
    <header class="navbar">
      <div class="brand">
        ⚡ FIND-U <span>| Portal de Soporte & Atención</span>
        <span style="font-size: 0.75rem; padding: 2px 8px; border-radius: 12px; margin-left: 10px; background: ${state.wsConnected ? '#10b981' : '#ef4444'}; color: #fff;">
          ${state.wsConnected ? '● WebSocket Conectado' : '○ Reconectando WS...'}
        </span>
      </div>
      <div class="user-info">
        <span>Agente: <strong>${state.user?.username || 'soporte'}</strong></span>
        <button id="btn-refresh" class="btn-logout">🔄 Actualizar</button>
        <button id="btn-logout" class="btn-logout">Cerrar Sesión</button>
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
                <option value="ABIERTO" ${state.filterStatus === 'ABIERTO' ? 'selected' : ''}>ABIERTO</option>
                <option value="ASIGNADO" ${state.filterStatus === 'ASIGNADO' ? 'selected' : ''}>ASIGNADO</option>
                <option value="RESUELTO" ${state.filterStatus === 'RESUELTO' ? 'selected' : ''}>RESUELTO</option>
                <option value="CERRADO" ${state.filterStatus === 'CERRADO' ? 'selected' : ''}>CERRADO</option>
              </select>

              <select id="filter-category" class="select-filter">
                <option value="ALL" ${state.filterCategory === 'ALL' ? 'selected' : ''}>Todas Categorías</option>
                <option value="FINANZAS" ${state.filterCategory === 'FINANZAS' ? 'selected' : ''}>FINANZAS</option>
                <option value="SERVICIO" ${state.filterCategory === 'SERVICIO' ? 'selected' : ''}>SERVICIO</option>
                <option value="PERFIL" ${state.filterCategory === 'PERFIL' ? 'selected' : ''}>PERFIL</option>
                <option value="TECNICO" ${state.filterCategory === 'TECNICO' ? 'selected' : ''}>TÉCNICO</option>
              </select>
            </div>
          </div>

          <div class="tickets-list">
            ${filteredTickets.length === 0 ? '<div style="padding:2rem;text-align:center;color:#64748b;">No hay tickets registrados</div>' : ''}
            ${filteredTickets.map(t => {
              const code = t.ticketCode || t.ticketNumber || t.id?.substring(0,8);
              const isSelected = (state.selectedTicket?.ticketCode === t.ticketCode) || (state.selectedTicket?.id === t.id);
              const role = t.requester?.userRole || t.userRole || 'USUARIO';
              const cat = t.categoryName || t.category || 'General';
              const subcat = t.subcategoryName ? ` • ${t.subcategoryName}` : '';
              return `
                <div class="ticket-item ${isSelected ? 'active' : ''}" data-code="${code}" data-id="${t.id}">
                  <div class="ticket-item-header">
                    <span class="ticket-code">#${code}</span>
                    <span class="badge-status badge-${(t.status || 'abierto').toLowerCase()}">${t.status}</span>
                  </div>
                  <div class="ticket-subject">${t.subject}</div>
                  <div class="ticket-meta">
                    <span>${role} (${cat}${subcat})</span>
                    <span>${t.createdAt ? new Date(t.createdAt).toLocaleDateString() : ''}</span>
                  </div>
                </div>
              `;
            }).join('')}
          </div>
        </aside>

        <!-- Main Ticket Detail & Conversation -->
        <section class="ticket-detail-panel">
          ${!state.selectedTicket ? `
            <div style="flex:1;display:flex;align-items:center;justify-content:center;color:#64748b;">
              Selecciona un ticket de la lista para ver la conversación y responder.
            </div>
          ` : `
            <div class="detail-header">
              <div class="detail-title">
                <h2>${state.selectedTicket.subject}</h2>
                <div class="detail-submeta">
                  Ticket #${state.selectedTicket.ticketCode || state.selectedTicket.ticketNumber || state.selectedTicket.id} |
                  Solicitante: <strong>${state.selectedTicket.requester?.name || state.selectedTicket.userName || 'Usuario'} (${state.selectedTicket.requester?.userRole || state.selectedTicket.userRole || 'USUARIO'})</strong> |
                  Categoría: <strong>${state.selectedTicket.categoryName || state.selectedTicket.category || 'General'}</strong>
                  ${state.selectedTicket.subcategoryName ? `(<em>${state.selectedTicket.subcategoryName}</em>)` : ''}
                  ${state.selectedTicket.assignedAgent ? ` | Agente asignado: <strong>${state.selectedTicket.assignedAgent.name}</strong>` : ''}
                </div>
              </div>

              <div class="detail-actions" style="display:flex;gap:0.5rem;align-items:center;">
                ${state.selectedTicket.status === 'ABIERTO' ? `
                  <button id="btn-take-ticket" class="btn-take-ticket">⚡ Tomar Ticket</button>
                ` : ''}

                <select id="change-status-select" class="select-filter">
                  <option value="ABIERTO" ${state.selectedTicket.status === 'ABIERTO' ? 'selected' : ''}>ABIERTO</option>
                  <option value="ASIGNADO" ${state.selectedTicket.status === 'ASIGNADO' ? 'selected' : ''}>ASIGNADO</option>
                  <option value="RESUELTO" ${state.selectedTicket.status === 'RESUELTO' ? 'selected' : ''}>RESUELTO</option>
                  <option value="CERRADO" ${state.selectedTicket.status === 'CERRADO' ? 'selected' : ''}>CERRADO</option>
                </select>
              </div>
            </div>

            <div class="chat-messages" id="chat-messages-container">
              <!-- Mensaje Inicial de apertura -->
              <div class="message-bubble message-user">
                <div class="message-header-info">
                  <span>${state.selectedTicket.requester?.name || state.selectedTicket.userName || 'Usuario'} (${state.selectedTicket.requester?.userRole || state.selectedTicket.userRole || 'USUARIO'})</span>
                  <span>${state.selectedTicket.createdAt ? new Date(state.selectedTicket.createdAt).toLocaleString() : ''}</span>
                </div>
                <div>${state.selectedTicket.description || 'Sin descripción'}</div>
              </div>

              <!-- Mensajes e Interacciones -->
              ${(state.selectedTicket.messages || []).map(m => {
                const isSupport = m.senderType === 'AGENTE_INTERNO' || m.senderType === 'AGENTE_EXTERNO' || m.senderType === 'AGENT';
                const bubbleClass = m.isInternalNote ? 'message-internal' : (isSupport ? 'message-support' : 'message-user');
                return `
                  <div class="message-bubble ${bubbleClass}">
                    <div class="message-header-info">
                      <span>${m.isInternalNote ? '📌 NOTA INTERNA DE AGENTE' : (m.senderName || m.senderType)}</span>
                      <span>${m.createdAt ? new Date(m.createdAt).toLocaleString() : ''}</span>
                    </div>
                    <div>${m.content}</div>

                    <!-- Archivos Adjuntos / Evidencias si existen -->
                    ${(m.attachments && m.attachments.length > 0) ? `
                      <div class="chat-attachments-grid">
                        ${m.attachments.map(att => `
                          <a href="${att.s3Url || att.url || '#'}" target="_blank" title="${att.filename || 'Adjunto'}">
                            <img class="chat-attachment-thumb" src="${att.s3Url || att.url || '#'}" alt="${att.filename || 'Evidencia'}">
                          </a>
                        `).join('')}
                      </div>
                    ` : ''}
                  </div>
                `;
              }).join('')}
            </div>

            <form class="chat-input-area" id="reply-form">
              <div class="input-toolbar">
                <label class="checkbox-label">
                  <input type="checkbox" id="chk-internal" ${state.isInternalNote ? 'checked' : ''}>
                  Nota Interna (visible solo para agentes de soporte)
                </label>
              </div>

              <div class="input-box-row">
                <textarea id="reply-text" placeholder="Escribe tu respuesta para el proveedor o cliente..." required>${state.replyText}</textarea>
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
      const code = item.getAttribute('data-code');
      const id = item.getAttribute('data-id');
      const found = state.tickets.find(t => (t.ticketCode && t.ticketCode === code) || t.id === id);
      if (found) selectTicket(found);
    });
  });

  if (state.selectedTicket) {
    const btnTake = document.getElementById('btn-take-ticket');
    if (btnTake) {
      btnTake.addEventListener('click', handleTakeTicket);
    }

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

// Inicialización
if (state.token) {
  connectWebSocket();
  fetchTickets();
}
render();
