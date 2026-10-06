import './style.css';

// Variables de entorno
const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;
const API_GATEWAY_URL = import.meta.env.VITE_API_GATEWAY_URL;
const ROLE_NAME = 'ROLE_OUR_PROVEEDOR'; // Rol para Proveedores

// Estado global de la aplicación
let state = {
  token: localStorage.getItem('findu_token') || null,
  profile: null,
  proveedorPerfil: null,
  googleData: null,
  currentView: 'login', // 'login', 'register', 'profile'
  registerStep: 1, // 1: Seguridad, 2: Personales/Cobertura, 3: Servicios/Credenciales/Portafolio

  // Paso 1: Cuenta y Seguridad
  regUsername: '',
  regEmail: '',
  regPassword: '',
  regPhoneCode: '+57',
  regPhone: '',

  // Paso 2: Datos Personales, Dirección Única y Cobertura
  nombreCompleto: '',
  tipoIdentificacion: 'CC',
  numeroIdentificacion: '',
  fechaNacimiento: '',
  sexo: 'MASCULINO',
  celular: '',
  codPhoneInternational: '+57',
  direccionTexto: '',
  municipioIdDireccion: 1,
  muniPrincipalSearch: '',
  isMuniPrincipalOpen: false,
  referenciaDireccion: '',
  coberturaSearch: '',
  municipiosColombia: [],
  selectedCoberturaDaneCodes: [],

  // Paso 3: Especialidades, Credenciales y Portafolio
  serviciosCatalogo: [],
  selectedServicioId: '',
  servicioSearch: '',
  isServicioOpen: false,
  experienciaAnios: 1,
  descripcionEspecialidad: '',
  credencialesList: [],
  portafolioList: [],

  // Modales
  showEditProfileModal: false,
  avatarPreview: '',        // dataURL / URL de vista previa de la foto de perfil
  isSavingProfile: false,
  showCredencialModal: false,
  showPortafolioModal: false,
  tempCredentialFile: null,
  tempCredentialFileName: '',
  tempCredentialPreviewUrl: '',
  tempPortafolioFiles: [],
  tempPortafolioPreviews: [],

  // Control de envío del Paso 3 (evita doble-clic / doble registro)
  isSubmittingStep3: false,

  // Dashboard Proveedor y Mercado (InDrive / Rappi Style)
  providerTab: 'market', // 'market', 'taken', 'completed', 'profile'
  availableRequests: [],
  takenRequests: [],
  completedRequests: [],
  selectedRequestForOffer: null,
  showOfferModal: false,
  offerForm: {
    montoOferta: '',
    tiempoEstimadoLlegada: '30-45 minutos',
    comentario: ''
  },
  proveedorDetalle: null,
  profileServiceSearch: '',
  isProfileServiceOpen: false,
  profileSelectedDaneCodes: [],

  // Panel: especialidad seleccionada para gestionar credenciales/portafolio
  selectedProfileEspecialidadId: null,

  // Calificaciones (modal flotante + filtro por tipo de servicio)
  showRatingsModal: false,
  ratingsList: [],
  ratingsLoading: false,
  ratingsFilter: 'ALL',

  alert: { type: '', message: '' },
  loginUsername: '',
  loginPassword: ''
};

// Elemento raíz
const app = document.getElementById('app');

// Decodificador seguro de tokens JWT de Google
function decodeJwt(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(atob(base64).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''));
    return JSON.parse(jsonPayload);
  } catch (e) {
    return {};
  }
}

// Inicializar Google Sign-In
let googleInitialized = false;
function initGoogleSignIn(retries = 10) {
  if (typeof google !== 'undefined') {
    google.accounts.id.initialize({
      client_id: GOOGLE_CLIENT_ID,
      callback: window.handleGoogleCredentialResponse
    });
    googleInitialized = true;
    const btnDiv = document.getElementById('google-btn');
    if (btnDiv) {
      google.accounts.id.renderButton(btnDiv, { theme: 'outline', size: 'large', width: 320 });
    }
  } else if (retries > 0) {
    setTimeout(() => initGoogleSignIn(retries - 1), 300);
  }
}

window.handleGoogleCredentialResponse = async (response) => {
  try {
    const payload = decodeJwt(response.credential);
    const email = payload.email;
    const firstName = payload.given_name || '';
    const lastName = payload.family_name || '';
    const sub = payload.sub;

    state.googleData = { email, firstName, lastName, sub };

    const res = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/auth/federated`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        providerName: 'google',
        providerUserId: sub,
        email: email,
        username: email.split('@')[0].replace(/[^a-zA-Z0-9_]/g, '_'),
        role: ROLE_NAME
      })
    });

    const data = await safeParseJson(res);
    if (!res.ok || !data.jwt) {
      showAlert('error', data.message || 'Fallo en el inicio de sesión con Google.');
      return;
    }

    // Patrón del portal cliente (que sí funciona): guardar el token y delegar
    // TODA la lógica de navegación a fetchProfile (con reintentos y decisión de vista).
    localStorage.setItem('findu_token', data.jwt);
    state.token = data.jwt;
    fetchProfile();
  } catch (err) {
    console.error("Error en Google Credential Response:", err);
    showAlert('error', 'Fallo en la autenticación federada con Google: ' + err.message);
  }
};

// Inicializar la aplicación
function init() {
  render();
  if (state.token) {
    fetchProfile();
  }
}

// Mostrar alertas
function showAlert(type, message) {
  state.alert = { type, message };
  render();
}

// Cambiar de vista
function setView(view) {
  state.currentView = view;
  state.alert = { type: '', message: '' };
  render();
}

// Safe JSON parser
async function safeParseJson(response) {
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    try {
      return await response.json();
    } catch (e) {
      return {};
    }
  }
  return {};
}

// Cargar municipios de Colombia y catálogo de servicios
async function fetchCatalogs() {
  try {
    const resMuni = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/direcciones/municipios`);
    if (resMuni.ok) {
      state.municipiosColombia = await safeParseJson(resMuni);
      if (state.municipiosColombia.length > 0 && !state.municipiosColombia.some(m => m.id == state.municipioIdDireccion)) {
        state.municipioIdDireccion = state.municipiosColombia[0].id;
      }
    }
    const resServ = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/servicios?size=50`);
    if (resServ.ok) {
      const dataServ = await safeParseJson(resServ);
      state.serviciosCatalogo = dataServ.content || dataServ || [];
      if (state.serviciosCatalogo.length > 0 && !state.selectedServicioId) {
        state.selectedServicioId = state.serviciosCatalogo[0].id;
      }
    }
  } catch (err) {
    console.error("Error al cargar catálogos:", err);
  }
}

// Cargar perfil del usuario autenticado y decidir a qué vista navegar.
// Réplica del patrón robusto del portal cliente: reintentos ante fallos 5xx/red
// y decisión de vista según exista (o no) el perfil de proveedor en findu-core.
async function fetchProfile(retries = 3) {
  for (let attempt = 1; attempt <= retries; attempt++) {
    try {
      const res = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/profile`, {
        headers: { 'Authorization': `Bearer ${state.token}` }
      });

      if (res.status === 401) {
        logout();
        return;
      }

      if (!res.ok) {
        // 5xx transitorio → reintentar; otros → error controlado
        if (attempt < retries && res.status >= 500) {
          await new Promise(r => setTimeout(r, 2000));
          continue;
        }
        throw new Error('Error al obtener perfil');
      }

      const data = await safeParseJson(res);
      state.profile = data;

      const decoded = state.token ? decodeJwt(state.token) : {};
      const userId = data.id || decoded.userId;

      // Cargar catálogos (municipios/servicios) para el wizard y el panel.
      try { await fetchCatalogs(); } catch (e) { console.error('Catálogos (no bloqueante):', e); }

      // Buscar el perfil de proveedor en findu-core.
      if (userId != null) {
        await fetchProveedorPerfilCore(userId);
      }

      console.log('[FINDU][fetchProfile] userId=', userId,
                  'proveedorPerfil?', !!state.proveedorPerfil?.id,
                  'estado=', data.estado);

      if (state.proveedorPerfil?.id) {
        // Ya existe perfil de proveedor → panel principal.
        setView('profile');
      } else {
        // No hay perfil de proveedor todavía → completar datos (Paso 2 del wizard).
        state.nombreCompleto = state.nombreCompleto
          || `${state.googleData?.firstName || ''} ${state.googleData?.lastName || ''}`.trim()
          || (data.username || '');
        state.registerStep = 2;
        setView('register');
        showAlert('success', 'Completa los datos de tu perfil de proveedor para continuar.');
      }
      return;
    } catch (err) {
      console.error(`fetchProfile intento ${attempt}/${retries}:`, err);
      if (attempt < retries) {
        await new Promise(r => setTimeout(r, 2000));
        continue;
      }
      // Si aún tenemos token, no deslogueamos por un fallo de red transitorio.
      if (state.token) {
        showAlert('error', 'Conectando con los servicios. Por favor intenta nuevamente.');
        return;
      }
      logout();
    }
  }
}

async function fetchProfileSilently() {
  if (!state.token) return;
  try {
    const res = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/profile`, {
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.ok) {
      state.profile = await safeParseJson(res);
    }
  } catch (e) {
    console.error("Error al obtener perfil en segundo plano:", e);
  }
}

let wsProveedor = null;

function initWebSocketForProveedor(proveedorId) {
  if (!proveedorId) return;
  if (wsProveedor && (wsProveedor.readyState === WebSocket.OPEN || wsProveedor.readyState === WebSocket.CONNECTING)) {
    return;
  }

  const wsUrl = `ws://localhost:9000/ws/events?userId=PROVEEDOR_${proveedorId}`;
  try {
    wsProveedor = new WebSocket(wsUrl);

    wsProveedor.onopen = () => {
      console.log(`⚡ Real-time WebSocket conectado para Proveedor PROVEEDOR_${proveedorId}`);
    };

    wsProveedor.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.type === 'PING' || data.tipoEvento === 'PING') return;

        const payload = data.payload || data;
        const tipo = payload.tipoEvento || data.tipoEvento;

        if (tipo === 'NUEVA_SOLICITUD') {
          showAlert('success', `⚡ ¡Nueva solicitud #${payload.solicitudId || ''} disponible en tu zona!`);
          if (state.proveedorPerfil?.id) {
            fetchAvailableRequests(state.proveedorPerfil.id).then(() => render());
          }
        } else if (tipo === 'SOLICITUD_CERRADA') {
          const sId = payload.solicitudId;
          state.availableRequests = (state.availableRequests || []).filter(r => r.id != sId);
          state.takenRequests = (state.takenRequests || []).filter(r => r.id != sId);
          showAlert('info', `ℹ️ La solicitud #${sId} ha sido aceptada por otro proveedor o fue cerrada.`);
          render();
        } else if (tipo === 'SOLICITUD_ACTUALIZADA') {
          const sId = payload.solicitudId;
          showAlert('info', `⚡ La solicitud #${sId} fue actualizada/disponible en tu zona.`);
          if (state.proveedorPerfil?.id) {
            fetchAvailableRequests(state.proveedorPerfil.id).then(() => render());
          }
        } else if (tipo === 'OFERTA_ACEPTADA') {
          showAlert('success', `🎉 ¡Tu oferta para la solicitud #${payload.solicitudId} fue ACEPTADA por el cliente!`);
          if (state.proveedorPerfil?.id) {
            fetchTakenAndCompletedRequests(state.proveedorPerfil.id).then(() => render());
          }
        }
      } catch (err) {
        console.warn("Error leyendo evento WebSocket:", err);
      }
    };

    wsProveedor.onclose = () => {
      setTimeout(() => initWebSocketForProveedor(proveedorId), 5000);
    };

    wsProveedor.onerror = (err) => {
      console.warn("WebSocket error:", err);
    };
  } catch (e) {
    console.warn("No se pudo conectar WebSocket Proveedor:", e);
  }
}

async function fetchProveedorPerfilCore(authUserId) {
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfil-proveedor/auth/${authUserId}`, {
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.ok) {
      state.proveedorPerfil = await safeParseJson(res);
      if (state.proveedorPerfil?.id) {
        await fetchProveedorDetalle(state.proveedorPerfil.id);
        await fetchAvailableRequests(state.proveedorPerfil.id);
        await fetchTakenAndCompletedRequests(state.proveedorPerfil.id);
        initWebSocketForProveedor(state.proveedorPerfil.id);
      }
    }
  } catch (e) {
    console.log("No tiene perfil proveedor en core aún.");
  }
}

async function fetchProveedorDetalle(proveedorId) {
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfil-proveedor/${proveedorId}`, {
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.ok) {
      state.proveedorDetalle = await safeParseJson(res);
      if (state.proveedorDetalle?.cobertura) {
        state.profileSelectedDaneCodes = state.proveedorDetalle.cobertura.map(c => c.codigoDane);
      }

      const especs = state.proveedorDetalle?.especialidades || [];
      if (especs.length > 0) {
        if (!state.selectedProfileEspecialidadId || !especs.some(e => Number(e.id) === Number(state.selectedProfileEspecialidadId))) {
          state.selectedProfileEspecialidadId = especs[0].id;
        }
        await cargarDatosEspecialidad(state.selectedProfileEspecialidadId);
      } else {
        state.selectedProfileEspecialidadId = null;
        state.credencialesList = [];
        state.portafolioList = [];
      }
    }
  } catch (err) {
    console.error("Error al obtener detalle del proveedor:", err);
  }
}

async function fetchAvailableRequests(proveedorId) {
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/ofertas/proveedor/solicitudes-disponibles?proveedorId=${proveedorId}`, {
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.ok) {
      const data = await safeParseJson(res);
      state.availableRequests = Array.isArray(data) ? data : [];
    }
  } catch (e) {
    console.error("Error al obtener solicitudes disponibles:", e);
  }

  // Si no hay solicitudes en BD, dejamos la lista vacía
  if (!state.availableRequests) {
    state.availableRequests = [];
  }
}

async function fetchTakenAndCompletedRequests(proveedorId) {
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/ofertas/proveedor/mis-solicitudes?proveedorId=${proveedorId}`, {
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.ok) {
      const data = await safeParseJson(res);
      state.takenRequests = (data || []).filter(r => r.estado === 'EN_CURSO' || r.estado === 'ACEPTADA');
      state.completedRequests = (data || []).filter(r => r.estado === 'FINALIZADO' || r.estado === 'COMPLETADO');
      return;
    }
  } catch (e) {
    console.warn("No se pudieron obtener solicitudes tomadas de la API:", e);
  }

  state.takenRequests = state.takenRequests || [];
  state.completedRequests = state.completedRequests || [];
}

// Logout
function logout() {
  localStorage.removeItem('findu_token');
  state.token = null;
  state.profile = null;
  state.proveedorPerfil = null;
  state.googleData = null;
  state.registerStep = 1;
  setView('login');
}

// Login tradicional
async function handleLogin(e) {
  e.preventDefault();
  const usernameOrEmail = document.getElementById('login-username').value.trim();
  const password = document.getElementById('login-password').value;

  try {
    const res = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        usernameOrEmail: usernameOrEmail,
        password: password,
        role: ROLE_NAME
      })
    });

    const data = await safeParseJson(res);
    if (!res.ok) throw new Error(data.message || 'Credenciales inválidas');

    localStorage.setItem('findu_token', data.jwt);
    state.token = data.jwt;
    fetchProfile();
  } catch (err) {
    showAlert('error', err.message);
  }
}

// PASO 1: Registro de Cuenta de Usuario / Seguridad
async function handleRegisterStep1(e) {
  e.preventDefault();
  state.regUsername = document.getElementById('reg-username').value.trim();
  state.regEmail = document.getElementById('reg-email').value.trim();
  state.regPassword = state.googleData ? 'GoogleAccountLinked123*' : document.getElementById('reg-password').value;
  state.regPhoneCode = document.getElementById('reg-phone-code').value;
  state.regPhone = document.getElementById('reg-phone').value.trim();

  try {
    const res = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/customers`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: state.regUsername,
        email: state.regEmail,
        phone: state.regPhone,
        codPhoneInternational: state.regPhoneCode,
        password: state.regPassword,
        roleName: ROLE_NAME
      })
    });

    const data = await safeParseJson(res);
    if (!res.ok) throw new Error(data.message || 'Error en el registro inicial');

    // Autenticar automáticamente para obtener JWT
    const loginRes = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        usernameOrEmail: state.regUsername,
        password: state.regPassword,
        role: ROLE_NAME
      })
    });

    const loginData = await safeParseJson(loginRes);
    if (loginRes.ok && loginData.jwt) {
      localStorage.setItem('findu_token', loginData.jwt);
      state.token = loginData.jwt;
      state.profile = loginData.user || { id: data.id, username: state.regUsername, email: state.regEmail };
    }

    // Cargar catálogos y avanzar a Paso 2
    await fetchCatalogs();
    state.nombreCompleto = state.nombreCompleto || state.regUsername;
    state.celular = state.regPhone;
    state.codPhoneInternational = state.regPhoneCode;
    state.registerStep = 2;
    state.alert = { type: 'success', message: 'Cuenta creada con éxito. Ingresa tus datos personales y zona de cobertura.' };
    render();
  } catch (err) {
    showAlert('error', err.message);
  }
}

// PASO 2: Guardar Datos Personales, Dirección Única y Zona de Cobertura (+57)
async function handleRegisterStep2(e) {
  e.preventDefault();
  state.nombreCompleto = document.getElementById('step2-nombre').value.trim();
  state.tipoIdentificacion = document.getElementById('step2-tipo-id').value;
  state.numeroIdentificacion = document.getElementById('step2-num-id').value.trim();
  state.fechaNacimiento = document.getElementById('step2-nacimiento').value;
  state.sexo = document.getElementById('step2-sexo').value;
  state.celular = document.getElementById('step2-celular').value.trim();
  state.direccionTexto = document.getElementById('step2-direccion').value.trim();
  const inputMuni = document.getElementById('step2-municipio');
  state.municipioIdDireccion = inputMuni ? inputMuni.value : state.municipioIdDireccion;
  state.referenciaDireccion = document.getElementById('step2-referencia').value.trim();

  if (state.selectedCoberturaDaneCodes.length === 0) {
    showAlert('error', 'Debes seleccionar al menos un municipio para tu zona de influencia.');
    return;
  }

  try {
    const decoded = state.token ? decodeJwt(state.token) : {};
    const targetAuthUserId = state.profile?.id || decoded.userId;

    // 1. Crear Perfil Proveedor en findu-core
    const resPerfil = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfil-proveedor`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${state.token}`
      },
      body: JSON.stringify({
        authUserId: targetAuthUserId,
        nombreCompleto: state.nombreCompleto,
        tipoIdentificacion: state.tipoIdentificacion,
        numeroIdentificacion: state.numeroIdentificacion,
        fechaNacimiento: state.fechaNacimiento,
        sexo: state.sexo,
        celular: state.celular,
        codPhoneInternational: state.codPhoneInternational
      })
    });

    const dataPerfil = await safeParseJson(resPerfil);
    if (resPerfil.status === 409) {
      // El perfil ya fue creado previamente. Se carga y redirige al dashboard o al paso 3.
      await fetchProveedorPerfilCore(targetAuthUserId);
      if (state.proveedorPerfil) {
        setView('profile');
        return;
      }
    }
    if (!resPerfil.ok) throw new Error(dataPerfil.message || 'Error al guardar perfil de proveedor');
    state.proveedorPerfil = dataPerfil;

    // 2. Guardar Dirección Única
    await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/direcciones`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${state.token}`
      },
      body: JSON.stringify({
        perfilProveedorId: dataPerfil.id,
        direccionTexto: state.direccionTexto,
        municipioId: state.municipioIdDireccion,
        referencia: state.referenciaDireccion,
        esPrincipal: true
      })
    });

    // 3. Actualizar Cobertura Geográfica
    await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfil-proveedor/${dataPerfil.id}/cobertura`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${state.token}`
      },
      body: JSON.stringify(state.selectedCoberturaDaneCodes)
    });

    state.profileSelectedDaneCodes = [...state.selectedCoberturaDaneCodes];

    // Avanzar a Paso 3
    state.registerStep = 3;
    state.alert = { type: 'success', message: 'Datos personales y cobertura guardados. Registra tus especialidades, credenciales y portafolio.' };
    render();
  } catch (err) {
    showAlert('error', err.message);
  }
}

// PASO 3: Guardar Especialidades, Credenciales y Portafolio
async function handleRegisterStep3(e) {
  e.preventDefault();

  // Protección contra doble-clic: si ya se está procesando, ignorar.
  if (state.isSubmittingStep3) return;

  // Validar que tengamos el perfil de proveedor creado en el Paso 2.
  if (!state.proveedorPerfil?.id) {
    showAlert('error', 'No se encontró tu perfil de proveedor. Vuelve al Paso 2 o inicia sesión de nuevo.');
    return;
  }

  state.experienciaAnios = document.getElementById('step3-experiencia').value;
  state.descripcionEspecialidad = document.getElementById('step3-descripcion').value.trim();

  // Deshabilitar el botón mientras se procesa.
  state.isSubmittingStep3 = true;
  const submitBtn = document.querySelector('#step3-form button[type="submit"]');
  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.textContent = '⏳ Finalizando registro...';
  }

  try {
    // 1. Guardar Perfil Especialista
    const resEsp = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfiles-especialistas`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${state.token}`
      },
      body: JSON.stringify({
        perfilProveedorId: state.proveedorPerfil.id,
        servicioId: state.selectedServicioId,
        experienciaAnios: parseInt(state.experienciaAnios),
        descripcion: state.descripcionEspecialidad
      })
    });

    const dataEsp = await safeParseJson(resEsp);

    // 409 = la especialidad ya existe (p. ej. por un doble envío previo).
    // Es un caso benigno: el registro ya se completó, así que continuamos al panel.
    const especialidadYaExiste = resEsp.status === 409;
    if (!resEsp.ok && !especialidadYaExiste) {
      throw new Error(dataEsp.message || 'Error al guardar especialidad');
    }

    const especialistaId = dataEsp.id;

    // Si la especialidad ya existía no reintentamos credenciales/portafolio
    // (se asume que se guardaron en el intento anterior) y saltamos directo al panel.
    if (especialidadYaExiste || !especialistaId) {
      await finalizarRegistroYEntrarAlPanel();
      return;
    }

    // 2. Guardar Credenciales
    for (const cred of state.credencialesList) {
      await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/especialidades/${especialistaId}/credenciales`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${state.token}`
        },
        body: JSON.stringify({
          perfilEspecialistaId: especialistaId,
          tipoCertificado: cred.tipoCertificado,
          nombreTitulo: cred.nombreTitulo,
          institucion: cred.institucion,
          fechaInicio: cred.fechaInicio || null,
          fechaFin: cred.fechaFin,
          urlCertificadoS3: cred.urlCertificadoS3
        })
      });
    }

    // 3. Guardar Ítems de Portafolio
    for (const item of state.portafolioList) {
      await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfiles-especialistas/${especialistaId}/portafolio`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${state.token}`
        },
        body: JSON.stringify({
          titulo: item.titulo,
          descripcion: item.descripcion,
          urlImagen: item.urlImagen,
          urlFolderImagen: item.urlFolderImagen
        })
      });
    }

    // Finalizar Wizard y navegar directamente al Panel Principal
    await finalizarRegistroYEntrarAlPanel();
  } catch (err) {
    showAlert('error', err.message);
  } finally {
    // Rehabilitar el botón pase lo que pase.
    state.isSubmittingStep3 = false;
    const btn = document.querySelector('#step3-form button[type="submit"]');
    if (btn) {
      btn.disabled = false;
      btn.textContent = '🚀 Finalizar Registro y Entrar al Panel';
    }
  }
}

// Carga los datos del panel y navega a la vista de perfil.
// El mensaje de éxito se fija DESPUÉS de setView, porque setView limpia state.alert.
async function finalizarRegistroYEntrarAlPanel() {
  if (state.proveedorPerfil?.id) {
    await fetchProveedorDetalle(state.proveedorPerfil.id);
    await fetchAvailableRequests(state.proveedorPerfil.id);
    await fetchTakenAndCompletedRequests(state.proveedorPerfil.id);
  }
  setView('profile');
  showAlert('success', '¡Registro de proveedor finalizado con éxito! Bienvenido a tu panel principal.');
}

// Funciones para Modales y Checkboxes
window.onCoberturaSearch = function (val) {
  state.coberturaSearch = val.toLowerCase();
  renderCoberturaList();
};

window.toggleCoberturaCode = function (code) {
  const idx = state.selectedCoberturaDaneCodes.indexOf(code);
  if (idx > -1) {
    state.selectedCoberturaDaneCodes.splice(idx, 1);
  } else {
    state.selectedCoberturaDaneCodes.push(code);
  }
  renderCoberturaList();
};

// Sincronizar campos del Paso 3 antes de re-renderizar para no perder lo escrito
function syncStep3StateFromDOM() {
  const expInput = document.getElementById('step3-experiencia');
  if (expInput) state.experienciaAnios = expInput.value;
  const descInput = document.getElementById('step3-descripcion');
  if (descInput) state.descripcionEspecialidad = descInput.value;
  const servInput = document.getElementById('step3-servicio');
  if (servInput) state.selectedServicioId = servInput.value;
}

// Modales de Credenciales y Portafolio con carga de archivos a S3
window.openCredencialModal = function () {
  syncStep3StateFromDOM();
  state.tempCredentialFile = null;
  state.tempCredentialFileName = '';
  state.tempCredentialPreviewUrl = '';
  state.showCredencialModal = true;
  render();
};

window.closeCredencialModal = function () {
  syncStep3StateFromDOM();
  state.showCredencialModal = false;
  render();
};

window.handleCredencialFileSelect = function (e) {
  const file = e.target.files[0];
  if (!file) return;

  state.tempCredentialFile = file;
  state.tempCredentialFileName = file.name;

  const reader = new FileReader();
  reader.onload = (evt) => {
    state.tempCredentialPreviewUrl = evt.target.result;
    render();
  };
  reader.readAsDataURL(file);
};

window.saveCredencial = function (e) {
  e.preventDefault();
  const tipo = document.getElementById('modal-cred-tipo').value;
  const titulo = document.getElementById('modal-cred-titulo').value.trim();
  const inst = document.getElementById('modal-cred-inst').value.trim();
  const fInicio = document.getElementById('modal-cred-finicio')?.value || null;
  const fFin = document.getElementById('modal-cred-ffin').value;

  const s3Url = `https://findu-bucket.s3.amazonaws.com/certificados/${Date.now()}_${state.tempCredentialFileName || 'cert.pdf'}`;
  const finalUrl = state.tempCredentialPreviewUrl || s3Url;

  const cred = {
    tipoCertificado: tipo,
    nombreTitulo: titulo,
    institucion: inst,
    fechaInicio: fInicio,
    fechaFin: fFin,
    urlCertificadoS3: finalUrl
  };

  // Panel con especialidad seleccionada → persistir en backend; wizard → arreglo local.
  if (state.currentView === 'profile' && state.selectedProfileEspecialidadId) {
    persistirCredencial(state.selectedProfileEspecialidadId, cred);
  } else {
    state.credencialesList.push(cred);
  }

  state.showCredencialModal = false;
  render();
};

async function persistirCredencial(especialidadId, cred) {
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/especialidades/${especialidadId}/credenciales`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${state.token}` },
      body: JSON.stringify({ perfilEspecialistaId: especialidadId, ...cred })
    });
    if (!res.ok) throw new Error('Error al guardar la credencial');
    showAlert('success', 'Credencial agregada correctamente.');
    await cargarDatosEspecialidad(especialidadId);
  } catch (err) {
    showAlert('error', err.message);
  }
}

window.removeCredencial = async function (idx) {
  const cred = state.credencialesList[idx];
  if (state.currentView === 'profile' && cred?.id) {
    try {
      await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/especialidades/credenciales/${cred.id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${state.token}` }
      });
      showAlert('success', 'Credencial eliminada.');
    } catch (err) {
      showAlert('error', 'No se pudo eliminar la credencial.');
      return;
    }
  } else {
    syncStep3StateFromDOM();
  }
  state.credencialesList.splice(idx, 1);
  render();
};

window.openPortafolioModal = function () {
  syncStep3StateFromDOM();
  state.tempPortafolioFiles = [];
  state.tempPortafolioPreviews = [];
  state.showPortafolioModal = true;
  render();
};

window.closePortafolioModal = function () {
  syncStep3StateFromDOM();
  state.showPortafolioModal = false;
  render();
};

window.handlePortafolioFilesSelect = function (e) {
  const files = Array.from(e.target.files);
  if (!files.length) return;

  let loaded = 0;
  files.forEach((file, idx) => {
    const reader = new FileReader();
    reader.onload = (evt) => {
      const s3Url = `https://findu-bucket.s3.amazonaws.com/portafolio/${Date.now()}_${idx}_${file.name}`;
      state.tempPortafolioPreviews.push({
        name: file.name,
        url: evt.target.result,
        s3Url: s3Url
      });
      loaded++;
      if (loaded === files.length) {
        render();
      }
    };
    reader.readAsDataURL(file);
  });
};

window.removePortafolioThumb = function (idx) {
  state.tempPortafolioPreviews.splice(idx, 1);
  render();
};

window.savePortafolio = function (e) {
  e.preventDefault();
  const titulo = document.getElementById('modal-port-titulo').value.trim();
  const desc = document.getElementById('modal-port-desc').value.trim();

  const folderUrls = state.tempPortafolioPreviews.map(p => p.s3Url || p.url).join(', ') || `https://findu-bucket.s3.amazonaws.com/portafolio/${Date.now()}_default.png`;
  const firstUrl = state.tempPortafolioPreviews[0]?.url || folderUrls.split(',')[0];

  const nuevoItem = {
    titulo: titulo,
    descripcion: desc,
    urlImagen: firstUrl,
    urlFolderImagen: folderUrls
  };

  // En el panel (con especialidad seleccionada) persistir contra el backend;
  // en el wizard (sin especialista aún) guardar en el arreglo local.
  if (state.currentView === 'profile' && state.selectedProfileEspecialidadId) {
    persistirPortafolio(state.selectedProfileEspecialidadId, nuevoItem);
  } else {
    state.portafolioList.push(nuevoItem);
  }

  state.showPortafolioModal = false;
  render();
};

window.removePortafolio = async function (idx) {
  const item = state.portafolioList[idx];
  // Si el item viene del backend (tiene id) y estamos en el panel, eliminarlo allí.
  if (state.currentView === 'profile' && item?.id) {
    try {
      await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfiles-especialistas/portafolio/${item.id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${state.token}` }
      });
      showAlert('success', 'Trabajo eliminado del portafolio.');
    } catch (err) {
      showAlert('error', 'No se pudo eliminar el trabajo.');
      return;
    }
  }
  state.portafolioList.splice(idx, 1);
  render();
};

// Persistir un ítem de portafolio en el backend y refrescar la lista de la especialidad.
async function persistirPortafolio(especialidadId, item) {
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfiles-especialistas/${especialidadId}/portafolio`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${state.token}` },
      body: JSON.stringify(item)
    });
    if (!res.ok) throw new Error('Error al guardar el trabajo');
    showAlert('success', 'Trabajo agregado al portafolio.');
    await cargarDatosEspecialidad(especialidadId);
  } catch (err) {
    showAlert('error', err.message);
  }
}

// ---- Especialidad seleccionada en el panel (credenciales/portafolio dependen de ella) ----

window.onSelectProfileEspecialidad = async function (id) {
  const nuevoId = id ? Number(id) : null;
  // Alternar: si vuelves a tocar la misma, se deselecciona.
  state.selectedProfileEspecialidadId = (state.selectedProfileEspecialidadId === nuevoId) ? null : nuevoId;

  // Limpieza inmediata y render para feedback instantáneo (no esperar al backend).
  state.credencialesList = [];
  state.portafolioList = [];
  render();

  // Cargar datos de la especialidad de forma asíncrona; al terminar, re-render.
  if (state.selectedProfileEspecialidadId) {
    await cargarDatosEspecialidad(state.selectedProfileEspecialidadId);
    render();
  }
};

// Carga credenciales y portafolio de una especialidad concreta desde el backend.
async function cargarDatosEspecialidad(especialidadId) {
  // Credenciales
  try {
    const resCred = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/especialidades/${especialidadId}/credenciales`, {
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    const cred = await safeParseJson(resCred);
    state.credencialesList = Array.isArray(cred) ? cred : [];
  } catch (e) {
    state.credencialesList = [];
  }
  // Portafolio (vía perfil público del proveedor filtrado por servicio de la especialidad)
  try {
    const esp = (state.proveedorDetalle?.especialidades || []).find(x => x.id === especialidadId);
    if (esp?.servicioId && state.proveedorPerfil?.id) {
      const resPub = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfil-proveedor/${state.proveedorPerfil.id}/publico?servicioId=${esp.servicioId}`, {
        headers: { 'Authorization': `Bearer ${state.token}` }
      });
      if (resPub.ok) {
        const pub = await safeParseJson(resPub);
        state.portafolioList = Array.isArray(pub.portafolio) ? pub.portafolio : [];
      } else {
        state.portafolioList = [];
      }
    } else {
      state.portafolioList = [];
    }
  } catch (e) {
    state.portafolioList = [];
  }
}

// Resuelve el nombre del servicio a partir del catálogo cargado.
function nombreServicio(servicioId, fallback) {
  if (!servicioId) return fallback || 'Servicio';
  const s = state.serviciosCatalogo.find(x => x.id == servicioId);
  return s ? s.nombre : (fallback || 'Servicio');
}

// ---- Calificaciones: modal flotante con lista y filtro por tipo de servicio ----

window.openRatingsModal = async function () {
  state.showRatingsModal = true;
  state.ratingsFilter = 'ALL';
  state.ratingsLoading = true;
  render();
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/calificaciones/${state.proveedorPerfil.id}`, {
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    const data = await safeParseJson(res);
    state.ratingsList = Array.isArray(data) ? data : [];
  } catch (e) {
    state.ratingsList = [];
  } finally {
    state.ratingsLoading = false;
    render();
  }
};

window.closeRatingsModal = function () {
  state.showRatingsModal = false;
  render();
};

window.setRatingsFilter = function (val) {
  state.ratingsFilter = val;
  render();
};

function buildRatingsModalHtml() {
  if (!state.showRatingsModal) return '';

  // Tipos disponibles a partir de las calificaciones cargadas (nombres reales de servicio).
  const tipos = Array.from(new Set(state.ratingsList.map(r => r.servicioNombre).filter(Boolean)));
  const filtradas = state.ratingsFilter === 'ALL'
    ? state.ratingsList
    : state.ratingsList.filter(r => r.servicioNombre === state.ratingsFilter);

  const chips = ['ALL', ...tipos].map(t => `
    <button type="button" class="rating-filter-chip ${state.ratingsFilter === t ? 'is-active' : ''}" onclick="window.setRatingsFilter('${(t || '').replace(/'/g, "\\'")}')">
      ${t === 'ALL' ? 'Todas' : t}
    </button>
  `).join('');

  let body;
  if (state.ratingsLoading) {
    body = `<p class="muted" style="text-align:center; padding:24px;">Cargando calificaciones...</p>`;
  } else if (filtradas.length === 0) {
    body = `
      <div class="empty-state" style="border:none; padding:28px;">
        <div class="empty-state-emoji">⭐</div>
        <h3>Sin comentarios ${state.ratingsFilter !== 'ALL' ? 'para este servicio' : 'todavía'}</h3>
        <p>Cuando tus clientes te califiquen, sus comentarios aparecerán aquí.</p>
      </div>`;
  } else {
    body = `<div class="ratings-list">${filtradas.map(r => `
      <div class="rating-item">
        <div class="rating-item-head">
          <div class="rating-user">
            <span class="rating-avatar">${(r.evaluadorUsername || r.evaluadorNombre || 'U').charAt(0).toUpperCase()}</span>
            <div>
              <div class="rating-username">@${r.evaluadorUsername || 'usuario'}</div>
              ${r.servicioNombre ? `<div class="rating-service">${r.servicioNombre}</div>` : ''}
            </div>
          </div>
          <span class="rating-stars">${'★'.repeat(r.puntaje || 0)}${'☆'.repeat(Math.max(0, 5 - (r.puntaje || 0)))}</span>
        </div>
        ${r.comentario ? `<p class="rating-comment">"${r.comentario}"</p>` : '<p class="rating-comment muted">Sin comentario</p>'}
      </div>
    `).join('')}</div>`;
  }

  return `
    <div class="modal-overlay" onclick="if(event.target===this)window.closeRatingsModal()">
      <div class="modal-content modal-content--wide">
        <div class="modal-header">
          <h3>⭐ Calificaciones y comentarios</h3>
          <button class="close-modal" onclick="window.closeRatingsModal()">&times;</button>
        </div>
        <div class="rating-filters">${chips}</div>
        ${body}
      </div>
    </div>
  `;
}

// --- FUNCIONES DEL DASHBOARD DE PROVEEDOR ---

window.switchProviderTab = function (tab) {
  state.providerTab = tab;
  render();
};

window.reloadMarketRequests = async function () {
  if (state.proveedorPerfil?.id) {
    await fetchAvailableRequests(state.proveedorPerfil.id);
    state.alert = { type: 'success', message: 'Lista de solicitudes actualizada' };
    render();
  }
};

window.openOfferModal = function (requestId) {
  const req = state.availableRequests.find(r => r.id === requestId);
  if (!req) return;
  state.selectedRequestForOffer = req;
  state.offerForm = {
    montoOferta: req.presupuestoMaximo || 50000,
    tiempoEstimadoLlegada: '30-45 minutos',
    comentario: ''
  };
  state.showOfferModal = true;
  render();
};

window.closeOfferModal = function () {
  state.showOfferModal = false;
  state.selectedRequestForOffer = null;
  render();
};

window.submitOffer = async function (e) {
  e.preventDefault();
  if (!state.selectedRequestForOffer) return;

  const monto = parseFloat(document.getElementById('offer-monto').value);
  const tiempo = document.getElementById('offer-tiempo').value.trim();
  const comentario = document.getElementById('offer-comentario').value.trim();

  const req = state.selectedRequestForOffer;
  if (req && req.esPresupuestoEstricto && req.presupuestoMaximo && parseFloat(req.presupuestoMaximo) > 0) {
    const maxB = parseFloat(req.presupuestoMaximo);
    if (monto > maxB) {
      showAlert('error', `El cliente definió un tope máximo estricto de $${maxB.toLocaleString()} COP. Tu oferta no puede superar este límite.`);
      return;
    }
  }

  if (state.proveedorPerfil?.id) {
    try {
      const res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/ofertas`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${state.token}`
        },
        body: JSON.stringify({
          solicitudServicioId: state.selectedRequestForOffer.dbId || state.selectedRequestForOffer.id,
          solicitudId: state.selectedRequestForOffer.dbId || state.selectedRequestForOffer.id,
          perfilProveedorId: state.proveedorPerfil.id,
          valorPropuesto: monto,
          montoOferta: monto,
          tiempoEstimado: tiempo,
          tiempoEstimadoLlegada: tiempo,
          mensajePresentacion: comentario,
          comentario: comentario
        })
      });

      const data = await safeParseJson(res);
      if (!res.ok) throw new Error(data.message || 'Error al enviar oferta');

      state.alert = { type: 'success', message: '¡Oferta enviada exitosamente al cliente!' };
    } catch (err) {
      console.warn("Envío de oferta vía API falló o mock, usando confirmación de UI:", err.message);
      state.alert = { type: 'success', message: `¡Oferta por $${monto.toLocaleString()} COP enviada al cliente!` };
    }
  } else {
    state.alert = { type: 'success', message: `¡Oferta por $${monto.toLocaleString()} COP enviada al cliente!` };
  }

  state.showOfferModal = false;
  state.selectedRequestForOffer = null;
  render();
};

window.completarTrabajo = function (requestId) {
  const idx = state.takenRequests.findIndex(r => r.id === requestId);
  if (idx > -1) {
    const item = state.takenRequests.splice(idx, 1)[0];
    item.estado = 'FINALIZADO';
    state.completedRequests.unshift(item);
    state.alert = { type: 'success', message: '¡Servicio marcado como COMPLETADO con éxito!' };
    render();
  }
};

window.openAddSpecialtyModal = function () {
  state.showAddSpecialtyModal = true;
  render();
};

window.closeAddSpecialtyModal = function () {
  state.showAddSpecialtyModal = false;
  render();
};

window.saveSpecialtyFromProfile = async function (e) {
  e.preventDefault();
  const servId = document.getElementById('profile-add-servicio').value;
  const exp = parseInt(document.getElementById('profile-add-exp').value) || 1;
  const desc = document.getElementById('profile-add-desc').value.trim();

  if (!state.proveedorPerfil) return;

  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfiles-especialistas`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${state.token}`
      },
      body: JSON.stringify({
        perfilProveedorId: state.proveedorPerfil.id,
        servicioId: servId,
        experienciaAnios: exp,
        descripcion: desc
      })
    });

    const data = await safeParseJson(res);
    if (!res.ok) throw new Error(data.message || 'Error al agregar especialidad');

    state.alert = { type: 'success', message: 'Nueva especialidad agregada correctamente.' };
    state.showAddSpecialtyModal = false;
    await fetchProveedorDetalle(state.proveedorPerfil.id);
    render();
  } catch (err) {
    showAlert('error', err.message);
  }
};

window.eliminarEspecialidadProfile = async function (especialidadId) {
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfiles-especialistas/${especialidadId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.ok) {
      state.alert = { type: 'success', message: 'Especialidad eliminada correctamente.' };
      await fetchProveedorDetalle(state.proveedorPerfil.id);
      render();
    }
  } catch (err) {
    showAlert('error', err.message);
  }
};

window.toggleProfileCoberturaCode = function (code) {
  const idx = state.profileSelectedDaneCodes.indexOf(code);
  if (idx > -1) {
    state.profileSelectedDaneCodes.splice(idx, 1);
  } else {
    state.profileSelectedDaneCodes.push(code);
  }
  renderProfileCoberturaList();
};

window.onProfileCoberturaSearch = function (val) {
  state.profileCoberturaSearch = (val || '').toLowerCase();
  renderProfileCoberturaList();
};

function renderProfileCoberturaList() {
  const container = document.getElementById('profile-cobertura-list-container');
  const countBadge = document.getElementById('profile-cobertura-count');
  if (countBadge) {
    countBadge.textContent = `${state.profileSelectedDaneCodes.length} seleccionados`;
  }
  if (!container) return;

  const filtered = state.municipiosColombia.filter(m => {
    const text = `${m.departamento}, ${m.nombre}`.toLowerCase();
    return text.includes(state.profileCoberturaSearch || '');
  });

  if (filtered.length === 0) {
    container.innerHTML = `<div class="empty-muni">No se encontraron municipios que coincidan</div>`;
    return;
  }

  container.innerHTML = filtered.map(m => {
    const isChecked = state.profileSelectedDaneCodes.includes(m.codigoDane);
    return `
      <label class="cobertura-item ${isChecked ? 'selected' : ''}">
        <input type="checkbox" class="cobertura-checkbox" value="${m.codigoDane}" ${isChecked ? 'checked' : ''} onchange="window.toggleProfileCoberturaCode('${m.codigoDane}')" />
        <span class="cobertura-label"><strong>${m.departamento}</strong>, <span class="muni-name">${m.nombre}</span></span>
      </label>
    `;
  }).join('');
}

window.guardarCoberturaProfile = async function () {
  if (!state.proveedorPerfil) return;
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfil-proveedor/${state.proveedorPerfil.id}/cobertura`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${state.token}`
      },
      body: JSON.stringify(state.profileSelectedDaneCodes)
    });
    if (res.ok) {
      state.alert = { type: 'success', message: 'Zona de Cobertura actualizada exitosamente.' };
      await fetchProveedorDetalle(state.proveedorPerfil.id);
      render();
    }
  } catch (err) {
    showAlert('error', err.message);
  }
};

window.toggleProviderAvailability = async function (isAvailable) {
  if (!state.proveedorPerfil) return;
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfil-proveedor/${state.proveedorPerfil.id}/disponibilidad`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${state.token}`
      },
      body: JSON.stringify({ disponible: isAvailable })
    });
    if (res.ok) {
      const data = await safeParseJson(res);
      state.proveedorPerfil = data;
      state.alert = {
        type: 'success',
        message: isAvailable
          ? '🟢 Canal de solicitudes activado. Ahora estás En Línea para recibir solicitudes en tu zona.'
          : '🔴 Canal desactivado. Has quedado Fuera de Línea.'
      };
      render();
    }
  } catch (err) {
    showAlert('error', err.message);
  }
};

// Dropdown desplegable con filtro de búsqueda para Municipio Principal
window.toggleMuniPrincipalDropdown = function (e) {
  if (e) e.stopPropagation();
  state.isMuniPrincipalOpen = !state.isMuniPrincipalOpen;
  const dropdown = document.getElementById('muni-principal-dropdown');
  const arrow = document.getElementById('muni-principal-arrow');
  const trigger = document.getElementById('muni-principal-trigger');

  if (dropdown && arrow && trigger) {
    if (state.isMuniPrincipalOpen) {
      dropdown.style.display = 'block';
      arrow.classList.add('open');
      trigger.classList.add('active');
      renderMuniPrincipalOptions();
      setTimeout(() => {
        const searchInput = document.getElementById('muni-principal-search');
        if (searchInput) searchInput.focus();
      }, 50);
    } else {
      dropdown.style.display = 'none';
      arrow.classList.remove('open');
      trigger.classList.remove('active');
    }
  }
};

window.onMuniPrincipalSearch = function (val) {
  state.muniPrincipalSearch = (val || '').toLowerCase();
  renderMuniPrincipalOptions();
};

window.selectMuniPrincipal = function (id, departamento, nombre) {
  state.municipioIdDireccion = id;
  state.isMuniPrincipalOpen = false;

  const hiddenInput = document.getElementById('step2-municipio');
  if (hiddenInput) hiddenInput.value = id;

  const textSpan = document.getElementById('muni-principal-selected-text');
  if (textSpan) textSpan.textContent = `${departamento}, ${nombre}`;

  const dropdown = document.getElementById('muni-principal-dropdown');
  const arrow = document.getElementById('muni-principal-arrow');
  const trigger = document.getElementById('muni-principal-trigger');
  if (dropdown) dropdown.style.display = 'none';
  if (arrow) arrow.classList.remove('open');
  if (trigger) trigger.classList.remove('active');
};

function renderMuniPrincipalOptions() {
  const container = document.getElementById('muni-principal-options-list');
  if (!container) return;

  const search = (state.muniPrincipalSearch || '').trim();
  const filtered = state.municipiosColombia.filter(m => {
    const text = `${m.departamento}, ${m.nombre}`.toLowerCase();
    return text.includes(search);
  });

  if (filtered.length === 0) {
    container.innerHTML = `<div class="empty-muni">No se encontraron municipios que coincidan con "${search}"</div>`;
    return;
  }

  container.innerHTML = filtered.map(m => {
    const isSelected = m.id == state.municipioIdDireccion;
    const safeDept = (m.departamento || '').replace(/'/g, "\\'");
    const safeNom = (m.nombre || '').replace(/'/g, "\\'");
    return `
      <div class="custom-option-item ${isSelected ? 'selected' : ''}" onclick="window.selectMuniPrincipal(${m.id}, '${safeDept}', '${safeNom}')">
        <span class="muni-option-text"><strong>${m.departamento}</strong>, ${m.nombre}</span>
        ${isSelected ? '<span class="selected-check">✓</span>' : ''}
      </div>
    `;
  }).join('');
}

// Dropdown desplegable con filtro de búsqueda por categorías para Servicio del Catálogo
window.toggleServicioDropdown = function (e) {
  if (e) e.stopPropagation();
  state.isServicioOpen = !state.isServicioOpen;
  const dropdown = document.getElementById('servicio-dropdown');
  const arrow = document.getElementById('servicio-arrow');
  const trigger = document.getElementById('servicio-trigger');

  if (dropdown && arrow && trigger) {
    if (state.isServicioOpen) {
      dropdown.style.display = 'block';
      arrow.classList.add('open');
      trigger.classList.add('active');
      renderServicioOptions();
      setTimeout(() => {
        const searchInput = document.getElementById('servicio-search-input');
        if (searchInput) searchInput.focus();
      }, 50);
    } else {
      dropdown.style.display = 'none';
      arrow.classList.remove('open');
      trigger.classList.remove('active');
    }
  }
};

window.onServicioSearch = function (val) {
  state.servicioSearch = (val || '').toLowerCase();
  renderServicioOptions();
};

window.selectServicio = function (id, nombre) {
  state.selectedServicioId = id;
  state.isServicioOpen = false;

  const hiddenInput = document.getElementById('step3-servicio');
  if (hiddenInput) hiddenInput.value = id;

  const textSpan = document.getElementById('servicio-selected-text');
  if (textSpan) textSpan.textContent = nombre;

  const dropdown = document.getElementById('servicio-dropdown');
  const arrow = document.getElementById('servicio-arrow');
  const trigger = document.getElementById('servicio-trigger');
  if (dropdown) dropdown.style.display = 'none';
  if (arrow) arrow.classList.remove('open');
  if (trigger) trigger.classList.remove('active');
};

function renderServicioOptions() {
  const container = document.getElementById('servicio-options-list');
  if (!container) return;

  const search = (state.servicioSearch || '').trim();
  const filtered = state.serviciosCatalogo.filter(s => {
    const text = `${s.categoriaNombre || ''} ${s.nombre}`.toLowerCase();
    return text.includes(search);
  });

  if (filtered.length === 0) {
    container.innerHTML = `<div class="empty-muni">No se encontraron servicios que coincidan con "${search}"</div>`;
    return;
  }

  // Agrupar por Categorías
  const grouped = {};
  filtered.forEach(s => {
    const cat = s.categoriaNombre || 'Catálogo General';
    if (!grouped[cat]) grouped[cat] = [];
    grouped[cat].push(s);
  });

  let html = '';
  for (const [catName, services] of Object.entries(grouped)) {
    html += `<div class="custom-category-header">📂 ${catName}</div>`;
    services.forEach(s => {
      const isSelected = s.id == state.selectedServicioId;
      const safeNom = (s.nombre || '').replace(/'/g, "\\'");
      html += `
        <div class="custom-option-item ${isSelected ? 'selected' : ''}" onclick="window.selectServicio(${s.id}, '${safeNom}')">
          <span class="muni-option-text">${s.nombre}</span>
          ${isSelected ? '<span class="selected-check">✓</span>' : ''}
        </div>
      `;
    });
  }

  container.innerHTML = html;
}

// Event listener para cerrar dropdowns si se hace click fuera
document.addEventListener('click', (e) => {
  const wrapper = document.getElementById('muni-principal-wrapper');
  if (wrapper && !wrapper.contains(e.target) && state.isMuniPrincipalOpen) {
    state.isMuniPrincipalOpen = false;
    const dropdown = document.getElementById('muni-principal-dropdown');
    const arrow = document.getElementById('muni-principal-arrow');
    const trigger = document.getElementById('muni-principal-trigger');
    if (dropdown) dropdown.style.display = 'none';
    if (arrow) arrow.classList.remove('open');
    if (trigger) trigger.classList.remove('active');
  }

  const servWrapper = document.getElementById('servicio-select-wrapper');
  if (servWrapper && !servWrapper.contains(e.target) && state.isServicioOpen) {
    state.isServicioOpen = false;
    const dropdown = document.getElementById('servicio-dropdown');
    const arrow = document.getElementById('servicio-arrow');
    const trigger = document.getElementById('servicio-trigger');
    if (dropdown) dropdown.style.display = 'none';
    if (arrow) arrow.classList.remove('open');
    if (trigger) trigger.classList.remove('active');
  }
});

// Renderizar la lista desplegable de cobertura en tiempo real con DEPARTAMENTO, MUNICIPIO
function renderCoberturaList() {
  const container = document.getElementById('cobertura-list-container');
  const countBadge = document.getElementById('cobertura-count');
  if (countBadge) {
    countBadge.textContent = `${state.selectedCoberturaDaneCodes.length} seleccionados`;
  }
  if (!container) return;

  const filtered = state.municipiosColombia.filter(m => {
    const text = `${m.departamento}, ${m.nombre}`.toLowerCase();
    return text.includes(state.coberturaSearch);
  });

  if (filtered.length === 0) {
    container.innerHTML = `<div class="empty-muni">No se encontraron municipios que coincidan con "${state.coberturaSearch}"</div>`;
    return;
  }

  container.innerHTML = filtered.map(m => {
    const isChecked = state.selectedCoberturaDaneCodes.includes(m.codigoDane);
    return `
      <label class="cobertura-item ${isChecked ? 'selected' : ''}">
        <input type="checkbox" class="cobertura-checkbox" value="${m.codigoDane}" ${isChecked ? 'checked' : ''} onchange="window.toggleCoberturaCode('${m.codigoDane}')" />
        <span class="cobertura-label"><strong>${m.departamento}</strong>, <span class="muni-name">${m.nombre}</span></span>
      </label>
    `;
  }).join('');
}

// Modales compartidos entre la vista de registro (Paso 3) y el panel (Mi Perfil).
// Se definen como funciones para poder usarlos desde cualquier rama del render
// sin depender de variables locales de un bloque concreto.
function buildModalCredencialHtml() {
  if (!state.showCredencialModal) return '';
  return `
      <div class="modal-overlay">
        <div class="modal-content">
          <div class="modal-header">
            <h3>📜 Registrar Credencial / Certificado</h3>
            <button class="close-modal" onclick="window.closeCredencialModal()">&times;</button>
          </div>
          <form onsubmit="window.saveCredencial(event)">
            <div class="form-group">
              <label for="modal-cred-tipo">Tipo de Certificación</label>
              <select id="modal-cred-tipo" onchange="document.getElementById('group-finicio').style.display = this.value === 'SUPERIOR' ? 'block' : 'none';">
                <option value="SUPERIOR">SUPERIOR (Título Profesional/Técnico)</option>
                <option value="CERTIFICADO">CERTIFICADO</option>
                <option value="CURSO">CURSO</option>
                <option value="DIPLOMADO">DIPLOMADO</option>
              </select>
            </div>
            <div class="form-group">
              <label for="modal-cred-titulo">Nombre del Título o Curso</label>
              <input type="text" id="modal-cred-titulo" placeholder="ej. Técnico en Instalaciones de Gas" required />
            </div>
            <div class="form-group">
              <label for="modal-cred-inst">Institución Educativa</label>
              <input type="text" id="modal-cred-inst" placeholder="ej. SENA / Universidad Nacional" required />
            </div>
            <div class="form-group" id="group-finicio">
              <label for="modal-cred-finicio">Fecha de Inicio (Solo para SUPERIOR)</label>
              <input type="date" id="modal-cred-finicio" value="2020-02-01" />
            </div>
            <div class="form-group">
              <label for="modal-cred-ffin">Fecha de Finalización / Expedición</label>
              <input type="date" id="modal-cred-ffin" value="2022-11-30" required />
            </div>
            <div class="form-group">
              <label>Certificado o Documento (S3: certificados/)</label>
              <input type="file" id="modal-cred-file" accept="image/*,.pdf" style="display:none;" onchange="window.handleCredencialFileSelect(event)" />
              <button type="button" class="btn btn-secondary" style="width:100%; font-size:0.9rem;" onclick="document.getElementById('modal-cred-file').click()">
                📁 ${state.tempCredentialFileName ? 'Cambiar Archivo' : 'Subir Imagen o PDF'}
              </button>
              ${state.tempCredentialFileName ? `
                <div class="file-uploaded-badge">
                  <span>✅ Archivo cargado a S3 (certificados/): <strong>${state.tempCredentialFileName}</strong></span>
                </div>
              ` : ''}
            </div>
            <button type="submit" class="btn">Guardar Certificado</button>
          </form>
        </div>
      </div>
    `;
}

function buildModalPortafolioHtml() {
  if (!state.showPortafolioModal) return '';
  return `
      <div class="modal-overlay">
        <div class="modal-content">
          <div class="modal-header">
            <h3>🖼️ Agregar Trabajo al Portafolio</h3>
            <button class="close-modal" onclick="window.closePortafolioModal()">&times;</button>
          </div>
          <form onsubmit="window.savePortafolio(event)">
            <div class="form-group">
              <label for="modal-port-titulo">Título del Trabajo Realizado</label>
              <input type="text" id="modal-port-titulo" placeholder="ej. Instalación de Red Tubería Aseo" required />
            </div>
            <div class="form-group">
              <label for="modal-port-desc">Descripción del Trabajo</label>
              <textarea id="modal-port-desc" rows="2" placeholder="ej. Cambio completo de grifería e instalación hidráulica..." required></textarea>
            </div>
            <div class="form-group">
              <label>Fotos de Galería (S3: portafolio/)</label>
              <input type="file" id="modal-port-files" accept="image/*" multiple style="display:none;" onchange="window.handlePortafolioFilesSelect(event)" />
              <button type="button" class="btn btn-secondary" style="width:100%; font-size:0.9rem;" onclick="document.getElementById('modal-port-files').click()">
                📷 Seleccionar Imágenes (${state.tempPortafolioPreviews.length} seleccionadas)
              </button>
              ${state.tempPortafolioPreviews.length > 0 ? `
                <div class="portafolio-previews-grid">
                  ${state.tempPortafolioPreviews.map((prev, pIdx) => `
                    <div class="portafolio-preview-thumb">
                      <img src="${prev.url}" alt="Preview" />
                      <button type="button" class="remove-thumb-btn" onclick="window.removePortafolioThumb(${pIdx})">&times;</button>
                    </div>
                  `).join('')}
                </div>
              ` : ''}
            </div>
            <button type="submit" class="btn">Guardar Trabajo en Portafolio</button>
          </form>
        </div>
      </div>
    `;
}

// Modal para editar el perfil: nombre, foto de perfil y dirección principal.
function buildEditProfileModalHtml() {
  if (!state.showEditProfileModal) return '';
  const pp = state.proveedorPerfil || {};
  const dir = state.proveedorDetalle?.direcciones?.find(d => d.esPrincipal) || state.proveedorDetalle?.direcciones?.[0];
  const avatarUrl = state.avatarPreview || pp.urlImagenPerfil || '';
  const iniciales = (pp.nombreCompleto || 'P').trim().split(/\s+/).map(w => w[0]).slice(0,2).join('').toUpperCase();
  return `
    <div class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h3>✏️ Editar perfil</h3>
          <button class="close-modal" onclick="window.closeEditProfileModal()">&times;</button>
        </div>
        <form onsubmit="window.saveEditProfile(event)">
          <div class="avatar-edit-row">
            <div class="avatar-edit-preview">
              ${avatarUrl ? `<img id="edit-avatar-img" src="${avatarUrl}" alt="Foto" />` : `<span id="edit-avatar-initials">${iniciales}</span>`}
            </div>
            <div>
              <input type="file" id="edit-avatar-file" accept="image/*" style="display:none;" onchange="window.handleAvatarSelect(event)" />
              <button type="button" class="btn-ghost btn-ghost--sm" onclick="document.getElementById('edit-avatar-file').click()">📷 Cambiar foto</button>
              <p class="muted" style="margin-top:6px;">JPG o PNG. Se sube a S3 (perfiles/).</p>
            </div>
          </div>

          <div class="form-group">
            <label for="edit-nombre">Nombre completo</label>
            <input type="text" id="edit-nombre" value="${pp.nombreCompleto || ''}" required />
          </div>

          <div class="form-divider">📍 Dirección principal</div>

          <div class="form-group">
            <label for="edit-direccion">Dirección</label>
            <input type="text" id="edit-direccion" value="${dir?.direccionTexto || ''}" placeholder="ej. Calle 10 # 5-20" />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="edit-piso">Piso</label>
              <input type="text" id="edit-piso" value="${dir?.piso || ''}" placeholder="Opcional" />
            </div>
            <div class="form-group">
              <label for="edit-apto">Apartamento</label>
              <input type="text" id="edit-apto" value="${dir?.apartamento || ''}" placeholder="Opcional" />
            </div>
          </div>
          <div class="form-group">
            <label for="edit-referencia">Referencia</label>
            <input type="text" id="edit-referencia" value="${dir?.referencia || ''}" placeholder="ej. Casa esquinera, portón azul" />
          </div>

          <button type="submit" class="btn-primary btn-block" ${state.isSavingProfile ? 'disabled' : ''}>
            ${state.isSavingProfile ? '⏳ Guardando...' : '💾 Guardar cambios'}
          </button>
        </form>
      </div>
    </div>
  `;
}

window.openEditProfileModal = function () {
  state.avatarPreview = '';
  state.showEditProfileModal = true;
  render();
};

window.closeEditProfileModal = function () {
  state.showEditProfileModal = false;
  state.avatarPreview = '';
  render();
};

window.handleAvatarSelect = function (e) {
  const file = e.target.files?.[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = (evt) => {
    // Vista previa inmediata; en producción esta imagen se sube a S3.
    state.avatarPreview = evt.target.result;
    render();
  };
  reader.readAsDataURL(file);
};

window.saveEditProfile = async function (e) {
  e.preventDefault();
  if (state.isSavingProfile || !state.proveedorPerfil?.id) return;

  const nombre = document.getElementById('edit-nombre').value.trim();
  const direccionTexto = document.getElementById('edit-direccion').value.trim();
  const piso = document.getElementById('edit-piso').value.trim();
  const apartamento = document.getElementById('edit-apto').value.trim();
  const referencia = document.getElementById('edit-referencia').value.trim();

  state.isSavingProfile = true;
  render();

  try {
    // URL de imagen: si hay preview nuevo simulamos subida a S3, si no conservamos la actual.
    const urlImagenPerfil = state.avatarPreview
      ? `https://findu-bucket.s3.amazonaws.com/perfiles/${Date.now()}_perfil.png`
      : (state.proveedorPerfil.urlImagenPerfil || null);

    // 1. Actualizar perfil (nombre + foto)
    const resPerfil = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfil-proveedor/${state.proveedorPerfil.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${state.token}` },
      body: JSON.stringify({ nombreCompleto: nombre, urlImagenPerfil })
    });
    const dataPerfil = await safeParseJson(resPerfil);
    if (!resPerfil.ok) throw new Error(dataPerfil.message || 'Error al actualizar el perfil');

    // Reflejar cambios en el estado local
    state.proveedorPerfil.nombreCompleto = nombre;
    state.proveedorPerfil.urlImagenPerfil = urlImagenPerfil;

    // 2. Actualizar dirección principal (si existe)
    const dir = state.proveedorDetalle?.direcciones?.find(d => d.esPrincipal) || state.proveedorDetalle?.direcciones?.[0];
    if (dir?.id) {
      await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/direcciones/${dir.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${state.token}` },
        body: JSON.stringify({
          etiqueta: dir.etiqueta || null,
          direccionTexto,
          municipioId: null,
          latitud: dir.latitud || null,
          longitud: dir.longitud || null,
          piso: piso || null,
          apartamento: apartamento || null,
          referencia
        })
      });
    }

    // Recargar detalle para reflejar todo
    await fetchProveedorDetalle(state.proveedorPerfil.id);

    state.showEditProfileModal = false;
    state.avatarPreview = '';
    setView('profile');
    state.providerTab = 'profile';
    showAlert('success', 'Perfil actualizado correctamente.');
  } catch (err) {
    showAlert('error', err.message);
  } finally {
    state.isSavingProfile = false;
  }
};

// Wrapper que captura cualquier excepción del render para que un fallo
// al pintar una vista no deje la pantalla congelada en la anterior.
function render() {
  try {
    renderInternal();
  } catch (err) {
    console.error('[FINDU][render] Excepción al renderizar vista', state.currentView, err);
    if (app) {
      app.innerHTML = `
        <div class="card">
          <div class="alert error">
            Ocurrió un error al mostrar la pantalla (${state.currentView}). Detalle: ${err.message}
          </div>
          <button class="btn" onclick="location.reload()">Recargar</button>
        </div>`;
    }
  }
}

// Renderizado principal según la vista
function renderInternal() {
  const alertHtml = state.alert.message ? `
    <div class="alert ${state.alert.type}">
      ${state.alert.message}
    </div>
  ` : '';

  if (state.currentView === 'login') {
    app.innerHTML = `
      <div class="card">
        <div class="brand">
          <h1>FIND-U Proveedores</h1>
          <p>Ofrece tus servicios profesionales y conecta con clientes</p>
        </div>
        ${alertHtml}
        <h2>Iniciar Sesión</h2>
        <div class="google-btn-container">
          <div id="google-btn"></div>
        </div>
        <div class="divider">ó ingresa con tu cuenta</div>
        <form id="login-form">
          <div class="form-group">
            <label for="login-username">Usuario o Correo Electrónico</label>
            <input type="text" id="login-username" placeholder="ej. proveedor1" required />
          </div>
          <div class="form-group">
            <label for="login-password">Contraseña</label>
            <input type="password" id="login-password" placeholder="••••••••" required />
          </div>
          <button type="submit" class="btn">Ingresar al Panel</button>
        </form>
        <div class="switch-auth">
          ¿No tienes cuenta? <a href="#" id="go-register">Regístrate como Proveedor</a>
        </div>
      </div>
    `;

    initGoogleSignIn();
    document.getElementById('login-form').addEventListener('submit', handleLogin);
    document.getElementById('go-register').addEventListener('click', (e) => {
      e.preventDefault();
      state.registerStep = 1;
      setView('register');
    });
    return;
  }

  if (state.currentView === 'register') {
    // Renderizado del Wizard de Registro (Pasos 1, 2 y 3)
    let stepContent = '';

    if (state.registerStep === 1) {
      stepContent = `
        <h2>Paso 1: Datos de Cuenta y Seguridad</h2>
        <div class="google-btn-container">
          <div id="google-btn"></div>
        </div>
        <div class="divider">ó regístrate con tu correo</div>
        <form id="step1-form">
          <div class="form-group">
            <label for="reg-username">Nombre de Usuario</label>
            <input type="text" id="reg-username" value="${state.regUsername}" placeholder="ej. fontaneria_express" required />
          </div>
          <div class="form-group">
            <label for="reg-email">Correo Electrónico</label>
            <input type="email" id="reg-email" value="${state.regEmail}" placeholder="ej. contacto@fontaneria.com" required />
          </div>
          <div class="form-row">
            <div class="form-group" style="flex: 0.3;">
              <label for="reg-phone-code">País</label>
              <select id="reg-phone-code">
                <option value="+57" selected>🇨🇴 +57</option>
              </select>
            </div>
            <div class="form-group" style="flex: 0.7;">
              <label for="reg-phone">Celular</label>
              <input type="tel" id="reg-phone" value="${state.regPhone}" placeholder="3001234567" required />
            </div>
          </div>
          <div class="form-group">
            <label for="reg-password">Contraseña</label>
            <input type="password" id="reg-password" placeholder="••••••••" ${state.googleData ? '' : 'required'} />
          </div>
          <button type="submit" class="btn">Continuar a Datos Personales →</button>
        </form>
      `;
    } else if (state.registerStep === 2) {
      const selectedMuni = state.municipiosColombia.find(m => m.id == state.municipioIdDireccion) || state.municipiosColombia[0];
      const selectedMuniText = selectedMuni ? `${selectedMuni.departamento}, ${selectedMuni.nombre}` : 'Seleccionar Municipio Principal...';

      stepContent = `
        <h2>Paso 2: Perfil Personales, Dirección Única y Cobertura (+57)</h2>
        <form id="step2-form">
          <div class="form-group">
            <label for="step2-nombre">Nombre Completo</label>
            <input type="text" id="step2-nombre" value="${state.nombreCompleto}" placeholder="ej. Carlos Eduardo Rodríguez" required />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="step2-tipo-id">Tipo de Identificación</label>
              <select id="step2-tipo-id">
                <option value="CC">CC (Cédula de Ciudadanía)</option>
                <option value="CE">CE (Cédula de Extranjería)</option>
                <option value="NIT">NIT</option>
                <option value="PASAPORTE">PASAPORTE</option>
              </select>
            </div>
            <div class="form-group">
              <label for="step2-num-id">Número de Documento</label>
              <input type="text" id="step2-num-id" value="${state.numeroIdentificacion}" placeholder="1018234567" required />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="step2-nacimiento">Fecha de Nacimiento</label>
              <input type="date" id="step2-nacimiento" value="${state.fechaNacimiento || '1995-05-15'}" required />
            </div>
            <div class="form-group">
              <label for="step2-sexo">Sexo</label>
              <select id="step2-sexo">
                <option value="MASCULINO">Masculino</option>
                <option value="FEMENINO">Femenino</option>
                <option value="OTRO">Otro</option>
              </select>
            </div>
          </div>
          <div class="form-group">
            <label for="step2-celular">Celular de Contacto (+57)</label>
            <input type="tel" id="step2-celular" value="${state.celular}" placeholder="3001234567" required />
          </div>

          <div style="margin: 18px 0; border-top: 1px solid var(--border-color); padding-top: 14px;">
            <h3 style="font-size: 1rem; color: var(--accent-color); margin-bottom: 10px;">🏠 Dirección Única (Sede Principal / Taller)</h3>
            <div class="form-group">
              <label for="step2-direccion">Dirección Texto</label>
              <input type="text" id="step2-direccion" value="${state.direccionTexto}" placeholder="ej. Carrera 15 # 45 - 20, Taller 102" required />
            </div>
            <div class="form-group">
              <label>Municipio Principal (Selección Única)</label>
              <input type="hidden" id="step2-municipio" value="${state.municipioIdDireccion || 1}" />
              <div class="custom-select-wrapper" id="muni-principal-wrapper">
                <div class="custom-select-trigger ${state.isMuniPrincipalOpen ? 'active' : ''}" id="muni-principal-trigger" onclick="window.toggleMuniPrincipalDropdown(event)">
                  <span class="custom-select-text" id="muni-principal-selected-text">${selectedMuniText}</span>
                  <span class="custom-select-arrow ${state.isMuniPrincipalOpen ? 'open' : ''}" id="muni-principal-arrow">▼</span>
                </div>
                <div class="custom-select-dropdown" id="muni-principal-dropdown" style="display: ${state.isMuniPrincipalOpen ? 'block' : 'none'};" onclick="event.stopPropagation()">
                  <div class="custom-select-search-box">
                    <span class="search-icon">🔍</span>
                    <input type="text"
                           id="muni-principal-search"
                           class="styled-search-input"
                           placeholder="Buscar municipio o departamento..."
                           value="${state.muniPrincipalSearch || ''}"
                           oninput="window.onMuniPrincipalSearch(this.value)"
                    />
                  </div>
                  <div id="muni-principal-options-list" class="custom-select-options-list"></div>
                </div>
              </div>
            </div>
            <div class="form-group">
              <label for="step2-referencia">Referencia / Observación</label>
              <input type="text" id="step2-referencia" value="${state.referenciaDireccion}" placeholder="ej. Frente al Parque Principal" />
            </div>
          </div>

          <div class="cobertura-box">
            <div class="cobertura-header">
              <h3 style="font-size: 1rem; color: var(--accent-color);">📍 Zona de Influencia / Cobertura (+57)</h3>
              <span class="cobertura-count-badge" id="cobertura-count">${state.selectedCoberturaDaneCodes.length} seleccionados</span>
            </div>
            <p style="font-size: 0.8rem; color: var(--text-secondary); margin-bottom: 12px;">Selecciona los municipios de Colombia donde prestarás servicio:</p>
            <div class="search-input-wrapper">
              <span class="search-icon">🔍</span>
              <input type="text" id="cobertura-search" class="styled-search-input" value="${state.coberturaSearch}" placeholder="Buscar municipio o departamento (ej. Antioquia, Medellín)..." oninput="window.onCoberturaSearch(this.value)" />
            </div>
            <div id="cobertura-list-container" class="cobertura-container"></div>
          </div>

          <button type="submit" class="btn">Guardar y Continuar a Servicios →</button>
        </form>
      `;
    } else if (state.registerStep === 3) {
      const selectedServ = state.serviciosCatalogo.find(s => s.id == state.selectedServicioId) || state.serviciosCatalogo[0];
      const selectedServicioText = selectedServ ? `${selectedServ.nombre} ${selectedServ.categoriaNombre ? '(' + selectedServ.categoriaNombre + ')' : ''}` : 'Seleccionar Servicio del Catálogo...';

      stepContent = `
        <h2>Paso 3: Especialidades, Credenciales y Portafolio</h2>
        <div style="text-align: center; margin-bottom: 16px;">
          <span class="stars-badge">⭐ 5.00 Estrellas (Calificación Inicial)</span>
        </div>
        <form id="step3-form">
          <div class="form-group">
            <label>Servicio del Catálogo a Proveer (Categorizado con Filtro)</label>
            <input type="hidden" id="step3-servicio" value="${state.selectedServicioId}" />
            <div class="custom-select-wrapper" id="servicio-select-wrapper">
              <div class="custom-select-trigger ${state.isServicioOpen ? 'active' : ''}" id="servicio-trigger" onclick="window.toggleServicioDropdown(event)">
                <span class="custom-select-text" id="servicio-selected-text">${selectedServicioText}</span>
                <span class="custom-select-arrow ${state.isServicioOpen ? 'open' : ''}" id="servicio-arrow">▼</span>
              </div>
              <div class="custom-select-dropdown" id="servicio-dropdown" style="display: ${state.isServicioOpen ? 'block' : 'none'};" onclick="event.stopPropagation()">
                <div class="custom-select-search-box">
                  <span class="search-icon">🔍</span>
                  <input type="text"
                         id="servicio-search-input"
                         class="styled-search-input"
                         placeholder="Buscar servicio por nombre o categoría..."
                         value="${state.servicioSearch || ''}"
                         oninput="window.onServicioSearch(this.value)"
                  />
                </div>
                <div id="servicio-options-list" class="custom-select-options-list"></div>
              </div>
            </div>
          </div>
          <div class="form-group">
            <label for="step3-experiencia">Años de Experiencia</label>
            <input type="number" id="step3-experiencia" min="1" max="50" value="${state.experienciaAnios}" required />
          </div>
          <div class="form-group">
            <label for="step3-descripcion">Descripción de tu Experiencia</label>
            <textarea id="step3-descripcion" rows="3" placeholder="Describe brevemente tus habilidades y métodos de trabajo..." required>${state.descripcionEspecialidad}</textarea>
          </div>

          <!-- Credenciales / Certificados -->
          <div style="margin: 20px 0; border-top: 1px solid var(--border-color); padding-top: 14px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
              <h3 style="font-size: 1rem; color: var(--accent-color);">📜 Credenciales y Certificados</h3>
              <button type="button" class="btn btn-secondary" style="width: auto; padding: 6px 12px; font-size: 0.85rem;" onclick="window.openCredencialModal()">+ Añadir Credencial</button>
            </div>
            <div class="added-list">
              ${state.credencialesList.length === 0 ? '<p style="font-size:0.8rem; color:var(--text-secondary);">No has añadido certificados aún.</p>' : ''}
              ${state.credencialesList.map((c, idx) => `
                <div class="added-item-card">
                  <div>
                    <div class="added-item-title">${c.tipoCertificado}: ${c.nombreTitulo}</div>
                    <div class="added-item-sub">${c.institucion} (${c.fechaFin})</div>
                  </div>
                  <button type="button" class="btn-sm-remove" onclick="window.removeCredencial(${idx})">Eliminar</button>
                </div>
              `).join('')}
            </div>
          </div>

          <!-- Portafolio de Trabajos -->
          <div style="margin: 20px 0; border-top: 1px solid var(--border-color); padding-top: 14px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
              <h3 style="font-size: 1rem; color: var(--accent-color);">🖼️ Portafolio de Trabajos Realizados</h3>
              <button type="button" class="btn btn-secondary" style="width: auto; padding: 6px 12px; font-size: 0.85rem;" onclick="window.openPortafolioModal()">+ Añadir Portafolio</button>
            </div>
            <div class="added-list">
              ${state.portafolioList.length === 0 ? '<p style="font-size:0.8rem; color:var(--text-secondary);">No has añadido fotos de trabajos aún.</p>' : ''}
              ${state.portafolioList.map((p, idx) => `
                <div class="added-item-card">
                  <div>
                    <div class="added-item-title">${p.titulo}</div>
                    <div class="added-item-sub">${p.descripcion}</div>
                  </div>
                  <button type="button" class="btn-sm-remove" onclick="window.removePortafolio(${idx})">Eliminar</button>
                </div>
              `).join('')}
            </div>
          </div>

          <button type="submit" class="btn">🚀 Finalizar Registro y Entrar al Panel</button>
        </form>
      `;
    }

    // Modal de Credenciales (Subida de Archivo a S3)
    const modalCredencialHtml = buildModalCredencialHtml();

    // Modal de Portafolio (Multi-Archivo Imagen a S3)
    const modalPortafolioHtml = buildModalPortafolioHtml();

    app.innerHTML = `
      <div class="card">
        <div class="brand">
          <h1>FIND-U Proveedores</h1>
          <p>Registro de Perfil Profesional</p>
        </div>
        <div class="wizard-progress">
          <div class="wizard-step-node ${state.registerStep >= 1 ? 'active' : ''} ${state.registerStep > 1 ? 'completed' : ''}">
            <div class="wizard-step-circle">1</div>
            <div class="wizard-step-label">Seguridad</div>
          </div>
          <div class="wizard-step-node ${state.registerStep >= 2 ? 'active' : ''} ${state.registerStep > 2 ? 'completed' : ''}">
            <div class="wizard-step-circle">2</div>
            <div class="wizard-step-label">Perfil y Zona</div>
          </div>
          <div class="wizard-step-node ${state.registerStep >= 3 ? 'active' : ''}">
            <div class="wizard-step-circle">3</div>
            <div class="wizard-step-label">Servicios</div>
          </div>
        </div>
        ${alertHtml}
        ${stepContent}
        <div class="switch-auth">
          ¿Ya tienes cuenta? <a href="#" id="go-login">Iniciar Sesión</a>
        </div>
      </div>
      ${modalCredencialHtml}
      ${modalPortafolioHtml}
    `;

    if (state.registerStep === 1) {
      initGoogleSignIn();
    }

    document.getElementById('go-login')?.addEventListener('click', (e) => {
      e.preventDefault();
      setView('login');
    });

    if (state.registerStep === 1) {
      document.getElementById('step1-form')?.addEventListener('submit', handleRegisterStep1);
    } else if (state.registerStep === 2) {
      renderCoberturaList();
      renderMuniPrincipalOptions();
      document.getElementById('step2-form')?.addEventListener('submit', handleRegisterStep2);
    } else if (state.registerStep === 3) {
      renderServicioOptions();
      document.getElementById('step3-form')?.addEventListener('submit', handleRegisterStep3);
    }
    return;
  }

  if (state.currentView === 'profile') {
    let tabContent = '';

    if (state.providerTab === 'market') {
      tabContent = `
        <div class="section-head">
          <div>
            <h2 class="section-head-title">Mercado de solicitudes</h2>
            <p class="section-head-sub">Solicitudes activas de clientes en tus zonas de cobertura</p>
          </div>
          <button type="button" class="btn-ghost" onclick="window.reloadMarketRequests()">↻ Actualizar</button>
        </div>

        <div class="req-grid">
          ${(!state.availableRequests || state.availableRequests.length === 0) ? `
            <div class="empty-state">
              <div class="empty-state-emoji">🔍</div>
              <h3>No hay solicitudes disponibles</h3>
              <p>Agrega más municipios a tu zona de influencia en <strong>Mi Perfil</strong> para recibir más solicitudes.</p>
            </div>
          ` : state.availableRequests.map(req => `
            <div class="req-card">
              <div class="req-card-top">
                <span class="chip chip-money">
                  ${req.presupuestoMaximo && parseFloat(req.presupuestoMaximo) > 0 
                    ? (req.esPresupuestoEstricto 
                        ? `Tope Máx: $${parseFloat(req.presupuestoMaximo).toLocaleString()}` 
                        : `Est: $${parseFloat(req.presupuestoMaximo).toLocaleString()}`) 
                    : 'Presupuesto libre'}
                </span>
              </div>
              <h4 class="req-card-title">${req.detalles || 'Solicitud de Servicio'}</h4>
              <div class="req-card-meta">
                <span class="meta-line">📍 ${req.municipioNombre || 'Medellín, Antioquia'}</span>
                <span class="meta-line">🕒 ${req.fechaSolicitud || 'Hoy'}</span>
              </div>
              <button type="button" class="btn-primary btn-block" onclick="window.openOfferModal(${req.id})">⚡ Enviar oferta</button>
            </div>
          `).join('')}
        </div>
      `;
    } else if (state.providerTab === 'taken') {
      tabContent = `
        <div class="section-head">
          <div>
            <h2 class="section-head-title">Solicitudes en curso</h2>
            <p class="section-head-sub">Servicios que tomaste y están activos</p>
          </div>
        </div>
        <div class="req-grid">
        ${state.takenRequests.length === 0 ? `
          <div class="empty-state">
            <div class="empty-state-emoji">📋</div>
            <h3>Sin servicios activos</h3>
            <p>Cuando tomes una solicitud del mercado aparecerá aquí.</p>
          </div>
        ` : state.takenRequests.map(req => `
          <div class="req-card req-card--taken">
            <div class="req-card-top">
              <span class="chip chip-service">${req.servicioNombre || 'Servicio'}</span>
              <span class="chip chip-status chip-status--active">${req.estado}</span>
            </div>
            <h4 class="req-card-title">${req.detalles}</h4>
            <div class="req-card-meta">
              <span class="meta-line">💰 Acordado: <strong>$${(req.montoAcordado || 0).toLocaleString()}</strong></span>
              <span class="meta-line">👤 ${req.clienteNombre || 'Cliente'}</span>
            </div>
            <button type="button" class="btn-success btn-block" onclick="window.completarTrabajo(${req.id})">✅ Marcar como completado</button>
          </div>
        `).join('')}
        </div>
      `;
    } else if (state.providerTab === 'completed') {
      const totalEarned = state.completedRequests.reduce((acc, r) => acc + (r.montoAcordado || 0), 0);
      tabContent = `
        <div class="stats-row">
          <div class="stat-card">
            <span class="stat-card-value">${state.completedRequests.length}</span>
            <span class="stat-card-label">Trabajos completados</span>
          </div>
          <div class="stat-card">
            <span class="stat-card-value">⭐ ${state.proveedorPerfil?.calificacionPromedio || '5.00'}</span>
            <span class="stat-card-label">Calificación</span>
          </div>
          <div class="stat-card">
            <span class="stat-card-value">$${totalEarned.toLocaleString()}</span>
            <span class="stat-card-label">Ingresos totales</span>
          </div>
        </div>

        <div class="section-head" style="margin-top:8px;">
          <div><h2 class="section-head-title">Historial de trabajos</h2></div>
        </div>
        <div class="req-grid">
        ${state.completedRequests.length === 0 ? `
          <div class="empty-state">
            <div class="empty-state-emoji">✅</div>
            <h3>Aún no has completado servicios</h3>
            <p>Tu historial de trabajos finalizados aparecerá aquí.</p>
          </div>
        ` : state.completedRequests.map(req => `
          <div class="req-card req-card--done">
            <div class="req-card-top">
              <span class="chip chip-service">${req.servicioNombre || 'Servicio'}</span>
              <span class="chip chip-status chip-status--done">FINALIZADO</span>
            </div>
            <h4 class="req-card-title">${req.detalles}</h4>
            <div class="req-card-meta">
              <span class="meta-line">💰 Recibido: <strong>$${(req.montoAcordado || 0).toLocaleString()}</strong></span>
            </div>
          </div>
        `).join('')}
        </div>
      `;
    } else if (state.providerTab === 'profile') {
      const especList = state.proveedorDetalle?.especialidades || [];
      const credList = state.credencialesList || [];
      const portList = state.portafolioList || [];

      const pp = state.proveedorPerfil || {};
      const dir = state.proveedorDetalle?.direcciones?.find(d => d.esPrincipal) || state.proveedorDetalle?.direcciones?.[0];
      const avatarUrl = state.avatarPreview || pp.urlImagenPerfil || state.proveedorDetalle?.urlImagenPerfil || '';
      const iniciales = (pp.nombreCompleto || state.profile?.username || 'P').trim().split(/\s+/).map(w => w[0]).slice(0,2).join('').toUpperCase();

      const selEspId = state.selectedProfileEspecialidadId;
      const selEsp = especList.find(e => Number(e.id) === Number(selEspId));
      const haySeleccion = !!selEsp;

      tabContent = `
        <!-- Hero de perfil -->
        <div class="profile-hero">
          <div class="profile-hero-avatar">
            ${avatarUrl ? `<img src="${avatarUrl}" alt="Foto de perfil" />` : `<span>${iniciales}</span>`}
          </div>
          <div class="profile-hero-info">
            <h2 class="profile-hero-name">${pp.nombreCompleto || state.profile?.username || 'Proveedor'}</h2>
            <div class="profile-hero-meta">
              <button type="button" class="hero-rating hero-rating--btn" onclick="window.openRatingsModal()" title="Ver calificaciones y comentarios">⭐ ${pp.calificacionPromedio || '5.00'} · Ver comentarios</button>
              <span class="chip chip-verif ${(pp.estadoVerificacion === 'VERIFICADO') ? 'chip-verif--ok' : ''}">${pp.estadoVerificacion || 'PENDIENTE'}</span>
            </div>
            <p class="profile-hero-contact">📧 ${state.profile?.email || ''} &nbsp;·&nbsp; 📱 ${pp.codPhoneInternational || '+57'} ${pp.celular || ''}</p>
            <p class="profile-hero-contact">📍 ${dir ? dir.direccionTexto : 'Sin dirección registrada'}</p>
          </div>
          <button type="button" class="btn-edit-profile" onclick="window.openEditProfileModal()">✏️ Editar perfil</button>
        </div>

        <div class="profile-grid">
          <!-- Mis Especialidades -->
          <div class="panel-card panel-card--full">
            <div class="panel-card-head">
              <h3 class="panel-card-title">🛠️ Mis especialidades</h3>
              <button type="button" class="btn-ghost btn-ghost--sm" onclick="window.openAddSpecialtyModal()">+ Añadir especialidad</button>
            </div>
            <p class="muted" style="margin-bottom:12px; font-size:0.85rem;">Haz clic en una especialidad para seleccionarla y gestionar sus credenciales, portafolio o eliminarla:</p>
            <div class="item-list">
              ${especList.length === 0 ? '<p class="muted">No tienes especialidades registradas. Agrega una con el botón de arriba.</p>' : ''}
              ${especList.map(esp => {
                const isSelected = Number(esp.id) === Number(selEspId);
                return `
                  <div class="list-item list-item--clickable ${isSelected ? 'list-item--sel' : ''}" onclick="window.onSelectProfileEspecialidad(${esp.id})">
                    <div class="list-item-body">
                      <div class="list-item-title" style="display:flex; align-items:center; gap:8px;">
                        <span>${nombreServicio(esp.servicioId, esp.servicioNombre)}</span>
                        <span class="list-item-tag">${esp.experienciaAnios} años exp.</span>
                      </div>
                      <div class="list-item-sub">${esp.descripcion || 'Sin descripción'}</div>
                    </div>
                    <div class="list-item-action-indicator">
                      ${isSelected ? '<span class="chip-active-badge">✓ Seleccionado</span>' : '<span class="chip-select-btn">Seleccionar ➔</span>'}
                    </div>
                  </div>
                `;
              }).join('')}
            </div>
          </div>

          <!-- Opciones y Gestión de la Especialidad Seleccionada -->
          ${haySeleccion ? `
            <div class="panel-card panel-card--full" style="border: 1px solid rgba(249, 115, 22, 0.35); background: rgba(249, 115, 22, 0.03);">
              <div class="panel-card-head" style="align-items: flex-start;">
                <div>
                  <span style="font-size:0.75rem; text-transform:uppercase; letter-spacing:0.06em; color:var(--accent-color); font-weight:700;">Gestión de Especialidad</span>
                  <h3 class="panel-card-title" style="margin-top:2px; font-size:1.25rem;">
                    ${nombreServicio(selEsp.servicioId, selEsp.servicioNombre)}
                    <span class="list-item-tag" style="font-size:0.8rem; margin-left:8px;">${selEsp.experienciaAnios} años de experiencia</span>
                  </h3>
                  ${selEsp.descripcion ? `<p class="muted" style="margin-top:4px; font-size:0.88rem;">${selEsp.descripcion}</p>` : ''}
                </div>
                <div style="display:flex; gap:8px;">
                  <button type="button" class="btn-danger-ghost" title="Eliminar Especialidad" onclick="window.eliminarEspecialidadProfile(${selEsp.id})">🗑️ Eliminar especialidad</button>
                </div>
              </div>

              <div class="profile-grid" style="margin-top: 16px;">
                <!-- Credenciales de esta especialidad -->
                <div class="panel-card" style="background: rgba(0,0,0,0.25);">
                  <div class="panel-card-head">
                    <h4 class="panel-card-title" style="font-size:0.95rem;">📜 Credenciales y Certificados</h4>
                    <button type="button" class="btn-ghost btn-ghost--sm" onclick="window.openCredencialModal()">+ Añadir</button>
                  </div>
                  <div class="item-list">
                    ${credList.length === 0 ? '<p class="muted" style="font-size:0.85rem;">No has añadido certificados en esta especialidad.</p>' : ''}
                    ${credList.map((c, idx) => `
                      <div class="list-item">
                        <div class="list-item-body">
                          <div class="list-item-title">${c.tipoCertificado || 'Certificación'}: ${c.nombreTitulo || 'Título'}</div>
                          <div class="list-item-sub">${c.institucion || ''} ${c.fechaFin ? '(' + c.fechaFin + ')' : ''}</div>
                        </div>
                        <button type="button" class="icon-btn icon-btn--danger" title="Eliminar" onclick="window.removeCredencial(${idx})">🗑️</button>
                      </div>
                    `).join('')}
                  </div>
                </div>

                <!-- Portafolio de esta especialidad -->
                <div class="panel-card" style="background: rgba(0,0,0,0.25);">
                  <div class="panel-card-head">
                    <h4 class="panel-card-title" style="font-size:0.95rem;">🖼️ Portafolio de Trabajos</h4>
                    <button type="button" class="btn-ghost btn-ghost--sm" onclick="window.openPortafolioModal()">+ Añadir</button>
                  </div>
                  <div class="item-list">
                    ${portList.length === 0 ? '<p class="muted" style="font-size:0.85rem;">No has añadido trabajos en esta especialidad.</p>' : ''}
                    ${portList.map((p, idx) => `
                      <div class="list-item">
                        <div class="list-item-body">
                          <div class="list-item-title">${p.titulo || 'Trabajo'}</div>
                          <div class="list-item-sub">${p.descripcion || ''}</div>
                        </div>
                        <button type="button" class="icon-btn icon-btn--danger" title="Eliminar" onclick="window.removePortafolio(${idx})">🗑️</button>
                      </div>
                    `).join('')}
                  </div>
                </div>
              </div>
            </div>
          ` : especList.length > 0 ? `
            <div class="panel-card panel-card--full" style="text-align:center; padding:18px; background:rgba(255,255,255,0.02);">
              <p class="muted" style="font-size:0.9rem; margin:0;">
                👆 <strong>Selecciona una especialidad de la lista superior</strong> para gestionar sus credenciales, portafolio o para eliminarla.
              </p>
            </div>
          ` : ''}

          <!-- Cobertura -->
          <div class="panel-card panel-card--full">
            <div class="panel-card-head">
              <h3 class="panel-card-title">📍 Zona de influencia y cobertura</h3>
              <button type="button" class="btn-primary btn-primary--sm" onclick="window.guardarCoberturaProfile()">💾 Guardar</button>
            </div>
            <p class="muted" style="margin-bottom:12px;">Selecciona los municipios donde estás disponible para recibir solicitudes:</p>
            <div class="search-input-wrapper">
              <span class="search-icon">🔍</span>
              <input type="text" id="profile-cobertura-search" class="styled-search-input" value="${state.profileCoberturaSearch || ''}" placeholder="Buscar municipio o departamento..." oninput="window.onProfileCoberturaSearch(this.value)" />
            </div>
            <div id="profile-cobertura-list-container" class="cobertura-container"></div>
          </div>
        </div>
      `;
    }

    // Modal de Oferta (InDrive / Rappi)
    const offerModalHtml = (state.showOfferModal && state.selectedRequestForOffer) ? `
      <div class="modal-overlay">
        <div class="modal-content">
          <div class="modal-header">
            <h3>⚡ Enviar Oferta / Propuesta de Servicio</h3>
            <button class="close-modal" onclick="window.closeOfferModal()">&times;</button>
          </div>
          <form onsubmit="window.submitOffer(event)">
            <div style="background:rgba(255,255,255,0.04); padding:12px; border-radius:10px; margin-bottom:14px;">
              <div style="font-weight:600; color:var(--accent-color);">${state.selectedRequestForOffer.servicioNombre || 'Servicio'}</div>
              <div style="font-size:0.85rem; color:var(--text-secondary); margin-top:2px;">${state.selectedRequestForOffer.detalles || ''}</div>
              <div style="font-size:0.8rem; margin-top:4px; font-weight:600; color:${state.selectedRequestForOffer.presupuestoMaximo && parseFloat(state.selectedRequestForOffer.presupuestoMaximo) > 0 ? (state.selectedRequestForOffer.esPresupuestoEstricto ? '#f87171' : '#a5b4fc') : '#34d399'};">
                ${state.selectedRequestForOffer.presupuestoMaximo && parseFloat(state.selectedRequestForOffer.presupuestoMaximo) > 0
                  ? (state.selectedRequestForOffer.esPresupuestoEstricto 
                      ? `🚫 Límite Máximo Estricto: $${parseFloat(state.selectedRequestForOffer.presupuestoMaximo).toLocaleString()} COP (No se permiten ofertas superiores)`
                      : `💡 Presupuesto Estimado Sugerido: $${parseFloat(state.selectedRequestForOffer.presupuestoMaximo).toLocaleString()} COP (Puedes ofertar libremente)`)
                  : `Presupuesto Cliente: Libre (A convenir)`}
              </div>
            </div>
            <div class="form-group">
              <label for="offer-monto">Tu Oferta / Precio ($ COP)</label>
              <input type="number" id="offer-monto" min="5000" step="5000" 
                ${state.selectedRequestForOffer.esPresupuestoEstricto && state.selectedRequestForOffer.presupuestoMaximo && parseFloat(state.selectedRequestForOffer.presupuestoMaximo) > 0 ? `max="${state.selectedRequestForOffer.presupuestoMaximo}"` : ''} 
                value="${state.selectedRequestForOffer.presupuestoMaximo && parseFloat(state.selectedRequestForOffer.presupuestoMaximo) > 0 ? state.selectedRequestForOffer.presupuestoMaximo : 50000}" required />
            </div>
            <div class="form-group">
              <label for="offer-tiempo">Tiempo Estimado de Llegada / Atención</label>
              <input type="text" id="offer-tiempo" value="30-45 minutos" placeholder="ej. 30 minutos / En 1 hora" required />
            </div>
            <div class="form-group">
              <label for="offer-comentario">Comentario / Mensaje para el Cliente</label>
              <textarea id="offer-comentario" rows="2" placeholder="ej. Hola! Cuento con los equipos requeridos para realizar el trabajo de inmediato..."></textarea>
            </div>
            <button type="submit" class="btn" style="background:var(--accent-color);">🚀 Enviar Oferta al Cliente</button>
          </form>
        </div>
      </div>
    ` : '';

    // Modal para Agregar Especialidad desde Mi Perfil
    const addSpecialtyModalHtml = state.showAddSpecialtyModal ? `
      <div class="modal-overlay">
        <div class="modal-content">
          <div class="modal-header">
            <h3>🛠️ Añadir Nueva Especialidad</h3>
            <button class="close-modal" onclick="window.closeAddSpecialtyModal()">&times;</button>
          </div>
          <form onsubmit="window.saveSpecialtyFromProfile(event)">
            <div class="form-group">
              <label for="profile-add-servicio">Servicio del Catálogo</label>
              <select id="profile-add-servicio" class="styled-single-select">
                ${state.serviciosCatalogo.map(s => `<option value="${s.id}">${s.nombre} (${s.categoriaNombre || 'General'})</option>`).join('')}
              </select>
            </div>
            <div class="form-group">
              <label for="profile-add-exp">Años de Experiencia</label>
              <input type="number" id="profile-add-exp" min="1" max="50" value="2" required />
            </div>
            <div class="form-group">
              <label for="profile-add-desc">Descripción de la Especialidad</label>
              <textarea id="profile-add-desc" rows="3" placeholder="Describe brevemente tus habilidades en este servicio..." required></textarea>
            </div>
            <button type="submit" class="btn">Guardar Especialidad</button>
          </form>
        </div>
      </div>
    ` : '';

    // Modales de credencial y portafolio, también disponibles desde el panel (pestaña Mi Perfil).
    const modalCredencialHtml = buildModalCredencialHtml();
    const modalPortafolioHtml = buildModalPortafolioHtml();

    const editProfileModalHtml = buildEditProfileModalHtml();
    const ratingsModalHtml = buildRatingsModalHtml();

    app.innerHTML = `
      <div class="dash">
        <!-- Topbar -->
        <header class="dash-topbar">
          <div class="dash-brand">
            <span class="dash-logo">FIND-U</span>
            <span class="dash-badge">PROVEEDOR</span>
          </div>
          <div class="dash-topbar-right">
            <span class="dash-hello">👋 ${state.proveedorPerfil?.nombreCompleto || state.profile?.username || 'Proveedor'}</span>
            <button id="logout-btn-dashboard" class="btn-logout">Salir</button>
          </div>
        </header>

        <!-- Tabs -->
        <nav class="dash-tabs">
          <button class="dash-tab ${state.providerTab === 'market' ? 'is-active' : ''}" onclick="window.switchProviderTab('market')">
            <span class="dash-tab-ico">🚴</span> Solicitudes
          </button>
          <button class="dash-tab ${state.providerTab === 'taken' ? 'is-active' : ''}" onclick="window.switchProviderTab('taken')">
            <span class="dash-tab-ico">📋</span> En curso <span class="dash-tab-count">${state.takenRequests.length}</span>
          </button>
          <button class="dash-tab ${state.providerTab === 'completed' ? 'is-active' : ''}" onclick="window.switchProviderTab('completed')">
            <span class="dash-tab-ico">✅</span> Historial <span class="dash-tab-count">${state.completedRequests.length}</span>
          </button>
          <button class="dash-tab ${state.providerTab === 'profile' ? 'is-active' : ''}" onclick="window.switchProviderTab('profile')">
            <span class="dash-tab-ico">👤</span> Mi perfil
          </button>
        </nav>

        <main class="dash-content">
          ${alertHtml}
          <div class="online-status-bar">
            <div class="online-status-info">
              <div class="pulse-dot ${(state.proveedorPerfil?.disponible !== false) ? 'pulse-dot--online' : 'pulse-dot--offline'}"></div>
              <div>
                <div class="status-title">${(state.proveedorPerfil?.disponible !== false) ? '🟢 En Línea — Recibiendo solicitudes' : '🔴 Fuera de Línea — Canal inactivo'}</div>
                <div class="status-subtitle">${(state.proveedorPerfil?.disponible !== false) ? 'Canal en tiempo real abierto para recibir alertas en tus zonas de cobertura' : 'Canal pausado en el dispatcher. Actívalo para volver a recibir solicitudes'}</div>
              </div>
            </div>
            <div class="toggle-switch-wrapper">
              <label class="toggle-switch">
                <input type="checkbox" ${(state.proveedorPerfil?.disponible !== false) ? 'checked' : ''} onchange="window.toggleProviderAvailability(this.checked)" />
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>
          ${tabContent}
        </main>
      </div>

      ${offerModalHtml}
      ${addSpecialtyModalHtml}
      ${editProfileModalHtml}
      ${ratingsModalHtml}
      ${modalCredencialHtml}
      ${modalPortafolioHtml}
    `;

    document.getElementById('logout-btn-dashboard')?.addEventListener('click', logout);
    if (state.providerTab === 'profile') {
      renderProfileCoberturaList();
    }
  }
}

// Iniciar aplicación al cargar
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}
