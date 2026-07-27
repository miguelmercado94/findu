import './style.css';

// Variables de entorno
const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;
const API_GATEWAY_URL = import.meta.env.VITE_API_GATEWAY_URL;
const ROLE_NAME = 'ROLE_OUR_PROVEEDOR'; // Rol fijo para el frontend de Proveedores

// Estado de la aplicación
let state = {
  token: localStorage.getItem('findu_token') || null,
  profile: null,
  currentView: 'login', // 'login', 'register', 'profile', 'forgot-password'
  googleData: null, // Guardará la info de Google temporalmente si se registra vía Google
  alert: { type: '', message: '' },
  recoveryStep: 1,
  recoveryMethod: 'email',
  recoveryEmail: '',
  recoveryPhone: '',
  recoveryPhoneCode: '+57',
  loginMethod: 'user',
  loginUsername: '',
  loginPhone: '',
  loginPhoneCode: '+57'
};

// Elemento raíz
const app = document.getElementById('app');

// Inicializar la aplicación
function init() {
  if (state.token) {
    fetchProfile();
  } else {
    render();
  }
}

// Cambiar de vista
function setView(view) {
  state.currentView = view;
  state.alert = { type: '', message: '' };
  if (view !== 'register') {
    state.googleData = null;
  }
  render();
}

// Mostrar alertas
function showAlert(type, message) {
  state.alert = { type, message };
  render();
}

// Parsear JSON de forma segura controlando respuestas vacías o no-JSON
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

// Cargar perfil del usuario
async function fetchProfile() {
  try {
    const res = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/profile`, {
      headers: {
        'Authorization': `Bearer ${state.token}`
      }
    });

    if (res.status === 401) {
      logout();
      return;
    }

    if (!res.ok) throw new Error('Error al obtener perfil');

    const data = await safeParseJson(res);
    state.profile = data;
    setView('profile');
  } catch (err) {
    console.error(err);
    logout();
  }
}

// Login tradicional
async function handleLogin(e) {
  e.preventDefault();

  let usernameOrEmail = null;
  let phone = null;
  let codPhoneInternational = null;

  if (state.loginMethod === 'user') {
    usernameOrEmail = document.getElementById('login-username').value.trim();
    state.loginUsername = usernameOrEmail;
  } else {
    phone = document.getElementById('login-phone').value.trim();
    codPhoneInternational = document.getElementById('login-phone-code').value;
    state.loginPhone = phone;
    state.loginPhoneCode = codPhoneInternational;
  }

  const password = document.getElementById('login-password').value;

  try {
    const res = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        usernameOrEmail: usernameOrEmail,
        phone: phone,
        codPhoneInternational: codPhoneInternational,
        password: password,
        role: ROLE_NAME
      })
    });

    const data = await safeParseJson(res);
    if (!res.ok) {
      throw new Error(data.message || 'Credenciales inválidas');
    }

    localStorage.setItem('findu_token', data.jwt);
    state.token = data.jwt;
    fetchProfile();
  } catch (err) {
    showAlert('error', err.message);
  }
}

// Registro tradicional (o complementando Google)
async function handleRegister(e) {
  e.preventDefault();
  const username = document.getElementById('reg-username').value;
  const email = document.getElementById('reg-email').value;
  const phone = document.getElementById('reg-phone').value;
  const codPhoneInternational = document.getElementById('reg-phone-code').value;
  const password = state.googleData ? 'GoogleAccountLinked123*' : document.getElementById('reg-password').value;

  try {
    // 1. Crear el usuario en la BD mediante el endpoint de customers (funciona para ambos roles en el backend)
    const res = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/customers`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username,
        email,
        phone,
        codPhoneInternational,
        password,
        roleName: ROLE_NAME
      })
    });

    const data = await safeParseJson(res);
    if (!res.ok) {
      throw new Error(data.message || `Error en el registro (${res.status})`);
    }

    // 2. Si el registro fue a través de Google, enlazamos la identidad federada en el backend
    if (state.googleData) {
      const fedRes = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/auth/federated`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          providerName: 'google',
          providerUserId: state.googleData.sub,
          email: state.googleData.email,
          username: username,
          role: ROLE_NAME
        })
      });

      const fedData = await safeParseJson(fedRes);
      if (!fedRes.ok) {
        throw new Error(fedData.message || 'Usuario creado pero falló el enlace con Google.');
      }

      localStorage.setItem('findu_token', fedData.jwt);
      state.token = fedData.jwt;
    } else {
      // Si fue tradicional, iniciamos sesión automáticamente
      const loginRes = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          usernameOrEmail: username,
          password,
          role: ROLE_NAME
        })
      });

      const loginData = await safeParseJson(loginRes);
      if (!loginRes.ok) throw new Error('Usuario creado con éxito. Por favor inicia sesión.');

      localStorage.setItem('findu_token', loginData.jwt);
      state.token = loginData.jwt;
    }

    fetchProfile();
  } catch (err) {
    showAlert('error', err.message);
  }
}

// Actualizar perfil de usuario
async function handleUpdateProfile(e) {
  e.preventDefault();
  const username = document.getElementById('up-username').value.trim();
  const phone = document.getElementById('up-phone').value.trim();
  const codPhoneInternational = document.getElementById('up-phone-code').value;

  try {
    const res = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/profile`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${state.token}`
      },
      body: JSON.stringify({
        username,
        phone,
        codPhoneInternational
      })
    });

    const data = await safeParseJson(res);
    if (!res.ok) {
      throw new Error(data.message || `Error al actualizar perfil (${res.status})`);
    }

    state.profile = data;
    showAlert('success', 'Perfil actualizado con éxito');
    render();
  } catch (err) {
    showAlert('error', err.message);
  }
}

// Cierre de sesión
function logout() {
  localStorage.removeItem('findu_token');
  state.token = null;
  state.profile = null;
  setView('login');
}

// Solicitar código de recuperación
async function handleRequestRecovery(e) {
  e.preventDefault();
  const method = state.recoveryMethod;
  let email = null;
  let phone = null;
  let codPhoneInternational = null;

  if (method === 'email') {
    email = document.getElementById('rec-email').value.trim();
    state.recoveryEmail = email;
  } else {
    phone = document.getElementById('rec-phone').value.trim();
    codPhoneInternational = document.getElementById('rec-phone-code').value;
    state.recoveryPhone = phone;
    state.recoveryPhoneCode = codPhoneInternational;
  }

  try {
    const res = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/auth/forgot-password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, phone, codPhoneInternational })
    });

    if (!res.ok) {
      const data = await safeParseJson(res);
      throw new Error(data.message || `Error al solicitar código (${res.status})`);
    }

    state.recoveryStep = 2;
    showAlert('success', 'Código de recuperación enviado. Usa "123456" para restablecer tu contraseña.');
  } catch (err) {
    showAlert('error', err.message);
  }
}

// Restablecer contraseña con código
async function handleResetPassword(e) {
  e.preventDefault();
  const code = document.getElementById('rec-code').value.trim();
  const newPassword = document.getElementById('rec-new-password').value;

  let email = null;
  let phone = null;
  let codPhoneInternational = null;

  if (state.recoveryMethod === 'email') {
    email = document.getElementById('rec-email-confirm').value.trim();
    state.recoveryEmail = email;
  } else {
    phone = document.getElementById('rec-phone-confirm').value.trim();
    codPhoneInternational = document.getElementById('rec-phone-code-confirm').value;
    state.recoveryPhone = phone;
    state.recoveryPhoneCode = codPhoneInternational;
  }

  const payload = {
    code,
    newPassword,
    email,
    phone,
    codPhoneInternational
  };

  try {
    const res = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/auth/reset-password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const data = await safeParseJson(res);
      throw new Error(data.message || `Error al restablecer contraseña (${res.status})`);
    }

    setView('login');
    showAlert('success', 'Contraseña restablecida con éxito. Inicia sesión.');
  } catch (err) {
    showAlert('error', err.message);
  }
}

// Función callback que llama Google tras loguearse
window.handleGoogleCredentialResponse = async function(response) {
  try {
    const idToken = response.credential;
    const payload = decodeJwt(idToken);
    
    // Almacenamos la info básica que retorna Google
    state.googleData = {
      sub: payload.sub,
      email: payload.email,
      firstName: payload.given_name || '',
      lastName: payload.family_name || ''
    };

    // Si ya estamos en la pantalla de registro, simplemente cargamos los datos y no hacemos fetch de login
    if (state.currentView === 'register') {
      setView('register');
      showAlert('success', 'Datos de Google cargados con éxito. Completa tu teléfono y usuario.');
      return;
    }

    // Intentamos hacer login federado directo (por si ya está registrado/enlazado)
    let res;
    try {
      res = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/auth/federated`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          providerName: 'google',
          providerUserId: state.googleData.sub,
          email: state.googleData.email,
          username: state.googleData.email.split('@')[0], // username fallback
          role: ROLE_NAME
        })
      });
    } catch (fetchErr) {
      console.warn('Network error or CORS during federated login, falling back to register form:', fetchErr);
      setView('register');
      showAlert('success', 'Autenticado con Google con éxito. Por favor, completa tu teléfono y usuario para finalizar el registro.');
      return;
    }

    if (res.ok) {
      const data = await safeParseJson(res);
      localStorage.setItem('findu_token', data.jwt);
      state.token = data.jwt;
      fetchProfile();
    } else {
      setView('register');
      showAlert('success', 'Autenticado con Google con éxito. Por favor, completa tu teléfono y usuario para finalizar el registro.');
    }
  } catch (err) {
    console.error(err);
    showAlert('error', 'Fallo en la autenticación federada con Google');
  }
};

// Decodificador simple de JWT
function decodeJwt(token) {
  const base64Url = token.split('.')[1];
  const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
  const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
    return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
  }).join(''));
  return JSON.parse(jsonPayload);
}

// Inicializar el botón de Google One-Tap/Button
function initGoogleSignIn() {
  if (typeof google !== 'undefined') {
    google.accounts.id.initialize({
      client_id: GOOGLE_CLIENT_ID,
      callback: window.handleGoogleCredentialResponse
    });
    
    const btnDiv = document.getElementById('google-btn');
    if (btnDiv) {
      google.accounts.id.renderButton(btnDiv, {
        theme: 'outline',
        size: 'large',
        width: 320
      });
    }
  }
}

// Renderizador del DOM
function render() {
  let html = '';

  const alertHtml = state.alert.message 
    ? `<div class="alert ${state.alert.type}">${state.alert.message}</div>`
    : '';

  if (state.currentView === 'login') {
    html = `
      <div class="card">
        <div class="brand">
          <h1>FIND-U</h1>
          <p>Portal de Proveedores</p>
        </div>
        <h2>Iniciar Sesión</h2>
        ${alertHtml}
        
        <div class="google-btn-container">
          <div id="google-btn"></div>
        </div>

        <div style="display: flex; gap: 10px; margin-bottom: 20px; justify-content: center;">
          <button id="login-method-user" class="btn" style="padding: 8px 12px; font-size: 0.85rem; background: ${state.loginMethod === 'user' ? 'var(--accent-color)' : 'rgba(255,255,255,0.05)'}; border: 1px solid var(--border-color); flex: 1; height: auto;">Usuario / Correo</button>
          <button id="login-method-phone" class="btn" style="padding: 8px 12px; font-size: 0.85rem; background: ${state.loginMethod === 'phone' ? 'var(--accent-color)' : 'rgba(255,255,255,0.05)'}; border: 1px solid var(--border-color); flex: 1; height: auto;">Teléfono</button>
        </div>

        <form id="login-form">
          ${state.loginMethod === 'user' ? `
            <div class="form-group">
              <label for="login-username">Usuario o Correo</label>
              <input type="text" id="login-username" placeholder="ej. proveedor.juan" required value="${state.loginUsername}">
            </div>
          ` : `
            <div class="form-group">
              <label for="login-phone">Teléfono Móvil</label>
              <div style="display: flex; gap: 8px;">
                <select id="login-phone-code" style="width: 130px; background: rgba(15, 23, 42, 0.6); border: 1px solid var(--border-color); border-radius: 12px; padding: 14px 10px; color: var(--text-primary); font-family: inherit; font-size: 0.95rem; outline: none; cursor: pointer;">
                  <option value="+57" ${state.loginPhoneCode === '+57' ? 'selected' : ''}>+57 (Colombia)</option>
                </select>
                <input type="text" id="login-phone" placeholder="ej. 3001234567" style="flex: 1;" required value="${state.loginPhone}">
              </div>
            </div>
          `}
          <div class="form-group">
            <label for="login-password">Contraseña</label>
            <input type="password" id="login-password" placeholder="••••••••" required>
          </div>
          <a href="#" id="go-forgot" style="display: block; text-align: right; margin-top: -12px; margin-bottom: 20px; font-size: 0.85rem; color: var(--accent-color); text-decoration: none; font-weight: 500;">¿Olvidaste tu contraseña?</a>
          <button type="submit" class="btn">Ingresar</button>
        </form>

        <div class="switch-auth">
          ¿No tienes cuenta? <a href="#" id="go-register">Regístrate aquí</a>
        </div>
      </div>
    `;
  } else if (state.currentView === 'register') {
    const isGoogle = !!state.googleData;
    const emailValue = isGoogle ? state.googleData.email : '';
    const nameValue = isGoogle ? `${state.googleData.firstName} ${state.googleData.lastName}` : '';
    const usernameValue = isGoogle ? state.googleData.email.split('@')[0] : '';

    html = `
      <div class="card">
        <div class="brand">
          <h1>FIND-U</h1>
          <p>Portal de Proveedores</p>
        </div>
        <h2>Registro de Proveedor</h2>
        ${alertHtml}

        ${!isGoogle ? `
          <div class="google-btn-container">
            <div id="google-btn"></div>
          </div>
          <div class="divider">o completa tus datos</div>
        ` : ''}

        <form id="register-form">
          ${isGoogle ? `
            <div class="form-group">
              <label>Nombre de Google</label>
              <input type="text" value="${nameValue}" disabled>
            </div>
          ` : ''}
          
          <div class="form-group">
            <label for="reg-email">Correo Electrónico</label>
            <input type="email" id="reg-email" value="${emailValue}" placeholder="ej. juan.proveedor@gmail.com" ${isGoogle ? 'disabled' : 'required'}>
          </div>

          <div class="form-group">
            <label for="reg-username">Nombre de Usuario</label>
            <input type="text" id="reg-username" value="${usernameValue}" placeholder="ej. juan.proveedor" required>
          </div>

          <div class="form-group">
            <label for="reg-phone">Teléfono Móvil</label>
            <div style="display: flex; gap: 8px;">
              <select id="reg-phone-code" style="width: 140px; background: rgba(24, 12, 10, 0.6); border: 1px solid var(--border-color); border-radius: 12px; padding: 14px 10px; color: var(--text-primary); font-family: inherit; font-size: 0.95rem; outline: none; cursor: pointer;">
                <option value="+57" selected>+57 (Colombia)</option>
              </select>
              <input type="text" id="reg-phone" placeholder="ej. 3001234567" style="flex: 1;" required>
            </div>
          </div>

          ${!isGoogle ? `
            <div class="form-group">
              <label for="reg-password">Contraseña</label>
              <input type="password" id="reg-password" placeholder="••••••••" required>
            </div>
          ` : ''}

          <button type="submit" class="btn">Finalizar Registro</button>
        </form>

        <div class="switch-auth">
          ¿Ya tienes cuenta? <a href="#" id="go-login">Inicia sesión</a>
        </div>
      </div>
    `;
  } else if (state.currentView === 'forgot-password') {
    if (state.recoveryStep === 1) {
      html = `
        <div class="card">
          <div class="brand">
            <h1>FIND-U</h1>
            <p>Portal de Proveedores</p>
          </div>
          <h2>Recuperar Contraseña</h2>
          ${alertHtml}
          
          <div style="display: flex; gap: 10px; margin-bottom: 20px; justify-content: center;">
            <button id="rec-method-email" class="btn" style="padding: 8px 12px; font-size: 0.85rem; background: ${state.recoveryMethod === 'email' ? 'var(--accent-color)' : 'rgba(255,255,255,0.05)'}; border: 1px solid var(--border-color); flex: 1; height: auto;">Email</button>
            <button id="rec-method-phone" class="btn" style="padding: 8px 12px; font-size: 0.85rem; background: ${state.recoveryMethod === 'phone' ? 'var(--accent-color)' : 'rgba(255,255,255,0.05)'}; border: 1px solid var(--border-color); flex: 1; height: auto;">Teléfono</button>
          </div>

          <form id="request-recovery-form">
            ${state.recoveryMethod === 'email' ? `
              <div class="form-group">
                <label for="rec-email">Correo Electrónico</label>
                <input type="email" id="rec-email" placeholder="ej. juan.perez@gmail.com" required value="${state.recoveryEmail}">
              </div>
            ` : `
              <div class="form-group">
                <label for="rec-phone">Teléfono Móvil</label>
                <div style="display: flex; gap: 8px;">
                  <select id="rec-phone-code" style="width: 130px; background: rgba(15, 23, 42, 0.6); border: 1px solid var(--border-color); border-radius: 12px; padding: 14px 10px; color: var(--text-primary); font-family: inherit; font-size: 0.95rem; outline: none; cursor: pointer;">
                    <option value="+57" ${state.recoveryPhoneCode === '+57' ? 'selected' : ''}>+57 (Colombia)</option>
                  </select>
                  <input type="text" id="rec-phone" placeholder="ej. 3001234567" style="flex: 1;" required value="${state.recoveryPhone}">
                </div>
              </div>
            `}
            <button type="submit" class="btn">Enviar Código</button>
          </form>

          <div class="switch-auth">
            <a href="#" id="go-login-recovery">Volver a Iniciar Sesión</a>
          </div>
        </div>
      `;
    } else {
      html = `
        <div class="card">
          <div class="brand">
            <h1>FIND-U</h1>
            <p>Portal de Proveedores</p>
          </div>
          <h2>Ingresar Código</h2>
          ${alertHtml}

          <form id="reset-password-form">
            ${state.recoveryMethod === 'email' ? `
              <div class="form-group">
                <label for="rec-email-confirm">Correo Electrónico</label>
                <input type="email" id="rec-email-confirm" placeholder="ej. juan.perez@gmail.com" required value="${state.recoveryEmail}">
              </div>
            ` : `
              <div class="form-group">
                <label for="rec-phone-confirm">Teléfono Móvil</label>
                <div style="display: flex; gap: 8px;">
                  <select id="rec-phone-code-confirm" style="width: 130px; background: rgba(15, 23, 42, 0.6); border: 1px solid var(--border-color); border-radius: 12px; padding: 14px 10px; color: var(--text-primary); font-family: inherit; font-size: 0.95rem; outline: none; cursor: pointer;">
                    <option value="+57" ${state.recoveryPhoneCode === '+57' ? 'selected' : ''}>+57 (Colombia)</option>
                  </select>
                  <input type="text" id="rec-phone-confirm" placeholder="ej. 3001234567" style="flex: 1;" required value="${state.recoveryPhone}">
                </div>
              </div>
            `}
            <div class="form-group">
              <label for="rec-code">Código de 6 dígitos</label>
              <input type="text" id="rec-code" placeholder="ej. 123456" required maxlength="6">
            </div>
            <div class="form-group">
              <label for="rec-new-password">Nueva Contraseña</label>
              <input type="password" id="rec-new-password" placeholder="••••••••" required minlength="6">
            </div>
            <button type="submit" class="btn">Restablecer Contraseña</button>
          </form>

          <div class="switch-auth">
            <a href="#" id="go-back-step1">Atrás</a> o <a href="#" id="go-login-recovery">Volver a Iniciar Sesión</a>
          </div>
        </div>
      `;
    }
  } else if (state.currentView === 'profile' && state.profile) {
    const ops = state.profile.operationNames || [];
    html = `
      <div class="card">
        <div class="brand">
          <h1>FIND-U</h1>
          <p>Portal de Proveedores</p>
        </div>

        ${state.alert.message ? `
          <div class="alert ${state.alert.type}">
            ${state.alert.message}
          </div>
        ` : ''}

        ${!state.profile.phone ? `
          <div style="background: rgba(239, 68, 68, 0.15); border: 1px solid var(--accent-color); border-radius: 12px; padding: 15px; margin-bottom: 20px; color: var(--text-primary);">
            <h4 style="margin: 0 0 5px 0; display: flex; align-items: center; gap: 8px;">
              ⚠️ Perfil incompleto
            </h4>
            <p style="margin: 0; font-size: 0.9rem; color: var(--text-secondary);">
              Por favor, completa tu número de teléfono y nombre de usuario para terminar de configurar tu cuenta.
            </p>
          </div>
        ` : ''}

        <h2>Mi Perfil</h2>
        
        <div class="profile-info">
          <div class="profile-field">
            <span>Usuario:</span>
            <span>${state.profile.username}</span>
          </div>
          <div class="profile-field">
            <span>Correo:</span>
            <span>${state.profile.email}</span>
          </div>
          <div class="profile-field">
            <span>Teléfono:</span>
            <span>${state.profile.phone ? `(${state.profile.codPhoneInternational || '+57'}) ${state.profile.phone}` : 'No registrado'}</span>
          </div>
          <div class="profile-field">
            <span>Rol:</span>
            <span class="badge" style="background: rgba(249, 115, 22, 0.15); color: #fed7aa;">${state.profile.roleName}</span>
          </div>
          <div style="margin-top: 15px;">
            <span style="font-size: 0.9rem; color: var(--text-secondary);">Permisos habilitados:</span>
            <div class="operation-list">
              ${ops.map(op => `<span class="operation-tag">${op}</span>`).join('')}
            </div>
          </div>
        </div>

        <div style="margin-top: 25px; padding-top: 20px; border-top: 1px solid var(--border-color); text-align: left;">
          <h3 style="margin-bottom: 15px; font-size: 1.1rem; color: var(--text-primary);">Actualizar Información</h3>
          <form id="update-profile-form">
            <div class="form-group" style="margin-bottom: 12px;">
              <label for="up-username" style="font-size: 0.85rem; margin-bottom: 4px;">Nombre de Usuario</label>
              <input type="text" id="up-username" value="${state.profile.username || ''}" required style="padding: 10px 14px;">
            </div>
            <div class="form-group" style="margin-bottom: 15px;">
              <label for="up-phone" style="font-size: 0.85rem; margin-bottom: 4px;">Teléfono Móvil</label>
              <div style="display: flex; gap: 8px;">
                <select id="up-phone-code" style="width: 130px; background: rgba(15, 23, 42, 0.6); border: 1px solid var(--border-color); border-radius: 12px; padding: 10px; color: var(--text-primary); font-family: inherit; font-size: 0.9rem; outline: none; cursor: pointer;">
                  <option value="+57" ${state.profile.codPhoneInternational === '+57' ? 'selected' : ''}>+57 (Col)</option>
                </select>
                <input type="text" id="up-phone" value="${state.profile.phone || ''}" placeholder="ej. 3001234567" style="flex: 1; padding: 10px 14px;" required>
              </div>
            </div>
            <button type="submit" class="btn" style="padding: 10px 16px; font-size: 0.9rem;">Guardar Cambios</button>
          </form>
        </div>

        <button id="logout-btn" class="btn" style="background: transparent; border: 1px solid var(--accent-color); color: var(--accent-color); margin-top: 20px;">Cerrar Sesión</button>
      </div>
    `;
  }

  app.innerHTML = html;

  // Ligar eventos del DOM
  if (state.currentView === 'login') {
    document.getElementById('login-form').addEventListener('submit', handleLogin);
    document.getElementById('go-register').addEventListener('click', (e) => { e.preventDefault(); setView('register'); });
    document.getElementById('go-forgot').addEventListener('click', (e) => {
      e.preventDefault();
      state.recoveryStep = 1;
      setView('forgot-password');
    });
    document.getElementById('login-method-user').addEventListener('click', () => {
      state.loginMethod = 'user';
      render();
    });
    document.getElementById('login-method-phone').addEventListener('click', () => {
      state.loginMethod = 'phone';
      render();
    });
    initGoogleSignIn();
  } else if (state.currentView === 'register') {
    document.getElementById('register-form').addEventListener('submit', handleRegister);
    document.getElementById('go-login').addEventListener('click', (e) => { e.preventDefault(); setView('login'); });
    if (!state.googleData) {
      initGoogleSignIn();
    }
  } else if (state.currentView === 'forgot-password') {
    if (state.recoveryStep === 1) {
      document.getElementById('request-recovery-form').addEventListener('submit', handleRequestRecovery);
      document.getElementById('rec-method-email').addEventListener('click', () => {
        state.recoveryMethod = 'email';
        render();
      });
      document.getElementById('rec-method-phone').addEventListener('click', () => {
        state.recoveryMethod = 'phone';
        render();
      });
    } else {
      document.getElementById('reset-password-form').addEventListener('submit', handleResetPassword);
      document.getElementById('go-back-step1').addEventListener('click', (e) => {
        e.preventDefault();
        state.recoveryStep = 1;
        state.alert = { type: '', message: '' };
        render();
      });
    }
    document.getElementById('go-login-recovery').addEventListener('click', (e) => {
      e.preventDefault();
      setView('login');
    });
  } else if (state.currentView === 'profile') {
    document.getElementById('logout-btn').addEventListener('click', logout);
    const updateForm = document.getElementById('update-profile-form');
    if (updateForm) {
      updateForm.addEventListener('submit', handleUpdateProfile);
    }
  }
}

// Inicializar
init();
