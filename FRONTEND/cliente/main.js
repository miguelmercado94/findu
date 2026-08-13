import './style.css';

// Variables de entorno
const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;
const API_GATEWAY_URL = import.meta.env.VITE_API_GATEWAY_URL;
const ROLE_NAME = 'ROLE_OUR_CLIENTE';

// Mapeo de imágenes por defecto (High Definition UI placeholders)
const DEFAULT_SERVICE_IMAGES = {
  plomeria: 'https://images.unsplash.com/photo-1585704032915-c3400ca199e7?auto=format&fit=crop&w=500&q=80',
  electrica: 'https://images.unsplash.com/photo-1621905251189-08b45d6a269e?auto=format&fit=crop&w=500&q=80',
  pintura: 'https://images.unsplash.com/photo-1589939705384-5185137a7f0f?auto=format&fit=crop&w=500&q=80',
  limpieza: 'https://images.unsplash.com/photo-1581578731548-c64695cc6952?auto=format&fit=crop&w=500&q=80',
  corte: 'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?auto=format&fit=crop&w=500&q=80',
  manicure: 'https://images.unsplash.com/photo-1604654894610-df63bc536371?auto=format&fit=crop&w=500&q=80',
  maquillaje: 'https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?auto=format&fit=crop&w=500&q=80',
  pc: 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?auto=format&fit=crop&w=500&q=80',
  red: 'https://images.unsplash.com/photo-1544197150-b99a580bb7a8?auto=format&fit=crop&w=500&q=80',
  enfermeria: 'https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&w=500&q=80',
  fisioterapia: 'https://images.unsplash.com/photo-1576091160550-2173dba999ef?auto=format&fit=crop&w=500&q=80',
  nutricion: 'https://images.unsplash.com/photo-1498837167922-ddd27525d352?auto=format&fit=crop&w=500&q=80',
  ninera: 'https://images.unsplash.com/photo-1502086223501-7ea6ecd79368?auto=format&fit=crop&w=500&q=80',
  cuidador: 'https://images.unsplash.com/photo-1581579438747-104c53d5fbc4?auto=format&fit=crop&w=500&q=80',
  mascotas: 'https://images.unsplash.com/photo-1548767797-d8c844163c4c?auto=format&fit=crop&w=500&q=80',
  default: 'https://images.unsplash.com/photo-1521791136064-7986c2920216?auto=format&fit=crop&w=500&q=80'
};

function getServiceImage(service) {
  if (service.urlImagen) return service.urlImagen;
  const name = (service.nombre || '').toLowerCase();
  if (name.includes('plomería') || name.includes('tubería') || name.includes('cañer') || name.includes('sanitaria')) return DEFAULT_SERVICE_IMAGES.plomeria;
  if (name.includes('eléctrica') || name.includes('tomas') || name.includes('revisión')) return DEFAULT_SERVICE_IMAGES.electrica;
  if (name.includes('pintura')) return DEFAULT_SERVICE_IMAGES.pintura;
  if (name.includes('limpieza') || name.includes('desinfección')) return DEFAULT_SERVICE_IMAGES.limpieza;
  if (name.includes('corte') || name.includes('tinte') || name.includes('alisado')) return DEFAULT_SERVICE_IMAGES.corte;
  if (name.includes('manicure') || name.includes('pedicure') || name.includes('uñas')) return DEFAULT_SERVICE_IMAGES.manicure;
  if (name.includes('maquillaje')) return DEFAULT_SERVICE_IMAGES.maquillaje;
  if (name.includes('pc') || name.includes('laptop') || name.includes('formateo')) return DEFAULT_SERVICE_IMAGES.pc;
  if (name.includes('red') || name.includes('wifi') || name.includes('cableado') || name.includes('office') || name.includes('impresora')) return DEFAULT_SERVICE_IMAGES.red;
  if (name.includes('enfermería') || name.includes('muestras') || name.includes('postoperatorio')) return DEFAULT_SERVICE_IMAGES.enfermeria;
  if (name.includes('fisioterapia')) return DEFAULT_SERVICE_IMAGES.fisioterapia;
  if (name.includes('nutricional')) return DEFAULT_SERVICE_IMAGES.nutricion;
  if (name.includes('niñera')) return DEFAULT_SERVICE_IMAGES.ninera;
  if (name.includes('cuidador')) return DEFAULT_SERVICE_IMAGES.cuidador;
  if (name.includes('canino') || name.includes('mascota')) return DEFAULT_SERVICE_IMAGES.mascotas;
  return DEFAULT_SERVICE_IMAGES.default;
}

// Estado de la aplicación
let state = {
  token: localStorage.getItem('findu_token') || null,
  profile: null,
  coreProfile: null,
  municipios: null,
  selectedDepartamento: '',
  currentView: 'login', // 'login', 'register', 'profile', 'forgot-password', 'complete-profile', 'services'
  activeTab: 'services', // 'services', 'requests', 'profile'
  googleData: null,
  alert: { type: '', message: '' },
  fechaNacimiento: '',
  regPhone: '',
  regPhoneCode: '+57',
  regUsername: '',
  regEmail: '',
  avatarBase64: null,
  completeProfileForm: {
    nombreCompleto: '',
    numeroIdentificacion: '',
    tipoIdentificacion: 'CC',
    sexo: 'M',
    direccionTexto: '',
    piso: '',
    apartamento: '',
    referencia: '',
    municipioId: ''
  },
  categorias: [],
  selectedCategoriaId: null,
  servicios: [],
  searchQuery: '',
  
  // Solicitud de servicio - Wizard Multi-Paso & Fotos & Direcciones
  selectedServiceForRequest: null,
  wizardStep: 1, // 1: Dirección, 2: Detalles & Presupuesto, 3: Fotos, 4: Resumen
  requestAddressId: null,
  requestDate: new Date().toISOString().split('T')[0],
  requestTime: '09:00',
  requestMaxHours: 2,
  requestMaxBudget: '',
  requestDetails: '',
  requestPhotos: [], // Array de cadenas Base64 (máx 10)

  // Modal Flotante de Dirección (Agregar / Editar)
  showAddressModal: false,
  editingAddress: null,
  addressForm: {
    etiqueta: 'Domicilio residencial',
    direccionTexto: '',
    departamento: '',
    municipioId: '',
    piso: '',
    apartamento: '',
    referencia: '',
    esPrincipal: false
  },

  requests: [],
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
  if (view === 'services') {
    fetchCategorias();
    fetchServicios();
  }
  render();
}

// Mostrar alertas
function showAlert(type, message) {
  state.alert = { type, message };
  render();
}

// Parsear JSON de forma segura
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

// Cargar perfil del usuario y sus direcciones
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
        if (attempt < retries && res.status >= 500) {
          await new Promise(r => setTimeout(r, 2000));
          continue;
        }
        throw new Error('Error al obtener perfil');
      }

      const data = await safeParseJson(res);
      state.profile = data;

      if (data.phone) state.regPhone = data.phone;
      if (data.codPhoneInternational) state.regPhoneCode = data.codPhoneInternational;
      if (data.username) state.regUsername = data.username;
      if (data.email) state.regEmail = data.email;

      if (!data.phone && state.googleData) {
        setView('register');
        showAlert('success', 'Autenticado con Google con éxito. Por favor, completa tu teléfono para finalizar el registro.');
        return;
      }

      if (data.roleName === ROLE_NAME) {
        try {
          const coreRes = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfil-cliente/usuario/${data.id}`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
          });
          if (coreRes.ok) {
            const coreProfile = await coreRes.json();
            state.coreProfile = coreProfile;
            if (coreProfile.celular) state.regPhone = coreProfile.celular;
            if (coreProfile.codPhoneInternational) state.regPhoneCode = coreProfile.codPhoneInternational;

            // Establecer dirección principal por defecto en el wizard si existe
            if (coreProfile.direcciones && coreProfile.direcciones.length > 0) {
              const principal = coreProfile.direcciones.find(d => d.esPrincipal) || coreProfile.direcciones[0];
              state.requestAddressId = principal.id;
            }

            if (coreProfile.estado === 'INCOMPLETO') {
              setView('complete-profile');
              return;
            } else {
              setView('services');
              return;
            }
          } else if (coreRes.status === 404) {
            setView('complete-profile');
            return;
          }
        } catch (coreErr) {
          console.error("Error fetching core profile:", coreErr);
          setView('complete-profile');
          return;
        }
      }

      setView('services');
      return;
    } catch (err) {
      console.error(`fetchProfile attempt ${attempt}/${retries}:`, err);
      if (attempt < retries) {
        await new Promise(r => setTimeout(r, 2000));
        continue;
      }
      logout();
    }
  }
}

// Refrescar direcciones del cliente desde findu-core
async function fetchClientAddresses() {
  if (!state.coreProfile?.id) return;
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfil-cliente/${state.coreProfile.id}/direcciones`, {
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.ok) {
      const addresses = await res.json();
      state.coreProfile.direcciones = addresses;
      if (!state.requestAddressId && addresses.length > 0) {
        const principal = addresses.find(d => d.esPrincipal) || addresses[0];
        state.requestAddressId = principal.id;
      }
      render();
    }
  } catch (e) {
    console.error("Error fetching addresses:", e);
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
    state.regPhone = phone;
    state.regPhoneCode = codPhoneInternational;
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

// Registro tradicional
async function handleRegister(e) {
  e.preventDefault();
  const username = document.getElementById('reg-username').value.trim();
  const email = document.getElementById('reg-email').value.trim();
  const phone = document.getElementById('reg-phone').value.trim();
  const codPhoneInternational = document.getElementById('reg-phone-code').value;
  const password = state.googleData ? 'GoogleAccountLinked123*' : document.getElementById('reg-password').value;
  const birthdateVal = document.getElementById('reg-birthdate').value;

  try {
    if (!birthdateVal) throw new Error('La fecha de nacimiento es obligatoria');
    const birthDate = new Date(birthdateVal);
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const m = today.getMonth() - birthDate.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) age--;
    if (age < 18) throw new Error('Debes ser mayor de 18 años para registrarte en FINDU');
    
    state.fechaNacimiento = birthdateVal;
    state.regUsername = username;
    state.regEmail = email;
    state.regPhone = phone;
    state.regPhoneCode = codPhoneInternational;

    if (state.token && state.googleData) {
      const updateRes = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/profile`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${state.token}`
        },
        body: JSON.stringify({ username, phone, codPhoneInternational })
      });
      if (updateRes.ok) {
        state.googleData = null;
        fetchProfile();
        return;
      }
    }

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

    const loginRes = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ usernameOrEmail: username, password, role: ROLE_NAME })
    });
    const loginData = await safeParseJson(loginRes);
    if (loginRes.ok) {
      localStorage.setItem('findu_token', loginData.jwt);
      state.token = loginData.jwt;
    }

    fetchProfile();
  } catch (err) {
    showAlert('error', err.message);
  }
}

// Solicitud de recuperación
async function handleRequestRecovery(e) {
  e.preventDefault();
  let email = state.recoveryMethod === 'email' ? document.getElementById('rec-email').value.trim() : null;
  let phone = state.recoveryMethod === 'phone' ? document.getElementById('rec-phone').value.trim() : null;
  let codPhoneInternational = state.recoveryMethod === 'phone' ? document.getElementById('rec-phone-code').value : null;

  try {
    const res = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/auth/forgot-password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, phone, codPhoneInternational })
    });
    const data = await safeParseJson(res);
    if (!res.ok) throw new Error(data.message || 'Error al solicitar recuperación');

    state.recoveryStep = 2;
    showAlert('success', 'Si la cuenta existe, se ha enviado un código de recuperación.');
  } catch (err) {
    showAlert('error', err.message);
  }
}

// Reset de contraseña
async function handleResetPassword(e) {
  e.preventDefault();
  let email = state.recoveryMethod === 'email' ? document.getElementById('rec-email-confirm').value.trim() : null;
  let phone = state.recoveryMethod === 'phone' ? document.getElementById('rec-phone-confirm').value.trim() : null;
  let codPhoneInternational = state.recoveryMethod === 'phone' ? document.getElementById('rec-phone-code-confirm').value : null;
  const code = document.getElementById('rec-code').value.trim();
  const newPassword = document.getElementById('rec-new-password').value;

  try {
    const res = await fetch(`${API_GATEWAY_URL}/security-auth/api/v1/auth/reset-password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, phone, codPhoneInternational, code, newPassword })
    });
    const data = await safeParseJson(res);
    if (!res.ok) throw new Error(data.message || 'Código inválido o expirado');

    showAlert('success', 'Contraseña restablecida con éxito. Inicia sesión.');
    setTimeout(() => setView('login'), 2000);
  } catch (err) {
    showAlert('error', err.message);
  }
}

// Cerrar sesión
function logout() {
  localStorage.removeItem('findu_token');
  state.token = null;
  state.profile = null;
  state.coreProfile = null;
  state.googleData = null;
  state.avatarBase64 = null;
  state.selectedServiceForRequest = null;
  state.wizardStep = 1;
  state.requestPhotos = [];
  setView('login');
}

// Decodificador de JWT
function decodeJwt(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));
    return JSON.parse(jsonPayload);
  } catch (e) {
    return {};
  }
}

// Google Sign-In
let googleInitialized = false;
function initGoogleSignIn(retries = 10) {
  if (typeof google !== 'undefined') {
    if (!googleInitialized) {
      google.accounts.id.initialize({
        client_id: GOOGLE_CLIENT_ID,
        callback: window.handleGoogleCredentialResponse
      });
      googleInitialized = true;
    }
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
        username: email.split('@')[0],
        role: ROLE_NAME
      })
    });

    if (res.ok) {
      const data = await safeParseJson(res);
      localStorage.setItem('findu_token', data.jwt);
      state.token = data.jwt;
      fetchProfile();
    } else {
      setView('register');
      showAlert('success', 'Autenticado con Google. Por favor, completa tu teléfono.');
    }
  } catch (err) {
    showAlert('error', 'Fallo en la autenticación federada con Google');
  }
};

// Obtener municipios desde findu-core
async function fetchMunicipios() {
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/direcciones/municipios`);
    if (!res.ok) throw new Error(`Error del servidor (${res.status})`);
    state.municipios = await res.json();
    render();
  } catch (err) {
    showAlert('error', 'Error al cargar municipios: ' + err.message);
  }
}

// Obtener categorías desde findu-core
async function fetchCategorias() {
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/categorias?page=0&size=50`);
    if (res.ok) {
      const data = await res.json();
      state.categorias = data.content || [];
      render();
    }
  } catch (err) {
    console.error("Error fetching categorias:", err);
  }
}

// Obtener servicios desde findu-core
async function fetchServicios(categoriaId = null) {
  try {
    let url = `${API_GATEWAY_URL}/findu-core/api/v1/servicios?page=0&size=50`;
    if (categoriaId) url += `&categoriaId=${categoriaId}`;
    const res = await fetch(url);
    if (res.ok) {
      const data = await res.json();
      state.servicios = data.content || [];
      render();
    }
  } catch (err) {
    console.error("Error fetching servicios:", err);
  }
}

// Completar perfil (Guardar perfil de cliente y dirección principal)
async function handleCompleteProfile(e) {
  e.preventDefault();
  const nombreCompleto = state.completeProfileForm.nombreCompleto.trim();
  const tipoIdentificacion = state.completeProfileForm.tipoIdentificacion;
  const numeroIdentificacion = state.completeProfileForm.numeroIdentificacion.trim();
  const sexo = state.completeProfileForm.sexo;
  const celularVal = state.regPhone || state.profile?.phone || state.loginPhone || '';
  const codPhoneIntVal = state.regPhoneCode || state.profile?.codPhoneInternational || '+57';
  const municipioId = state.completeProfileForm.municipioId;
  const direccionTexto = state.completeProfileForm.direccionTexto.trim();
  const reference = state.completeProfileForm.referencia.trim();
  const floor = state.completeProfileForm.piso.trim();
  const apartment = state.completeProfileForm.apartamento.trim();

  try {
    if (!nombreCompleto) throw new Error("El nombre completo es obligatorio.");
    if (!numeroIdentificacion) throw new Error("El número de identificación es obligatorio.");
    if (!celularVal) throw new Error("El número de celular es obligatorio.");
    if (!municipioId) throw new Error("Debes seleccionar una ciudad o municipio.");
    if (!direccionTexto) throw new Error("La dirección de residencia es obligatoria.");

    const payload = decodeJwt(state.token);
    const authUserId = payload.userId;
    if (!authUserId) throw new Error("No se pudo obtener el ID de usuario del token JWT.");

    const profilePayload = {
      authUserId: parseInt(authUserId),
      username: state.regUsername || state.profile?.username || payload.sub,
      email: state.regEmail || state.profile?.email || payload.sub,
      nombreCompleto: nombreCompleto,
      numeroIdentificacion: numeroIdentificacion,
      tipoIdentificacion: tipoIdentificacion,
      fechaNacimiento: state.fechaNacimiento || "2000-01-01",
      sexo: sexo,
      celular: celularVal,
      codPhoneInternational: codPhoneIntVal,
      urlImagenPerfil: state.avatarBase64 || state.profile?.urlImagenPerfil || null
    };

    const profileRes = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfil-cliente`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${state.token}`
      },
      body: JSON.stringify(profilePayload)
    });

    const profileData = await safeParseJson(profileRes);
    if (!profileRes.ok) throw new Error(profileData.message || 'Error al guardar el perfil');

    const profileId = profileData.id;
    const addressPayload = {
      etiqueta: "Principal",
      direccionTexto: direccionTexto,
      municipioId: parseInt(municipioId),
      latitud: 6.2086,
      longitud: -75.5659,
      piso: floor || null,
      apartamento: apartment || null,
      referencia: reference || null,
      esPrincipal: true
    };

    const addressRes = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfil-cliente/${profileId}/direcciones`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${state.token}`
      },
      body: JSON.stringify(addressPayload)
    });

    if (!addressRes.ok) {
      const addressData = await safeParseJson(addressRes);
      throw new Error(addressData.message || 'Error al registrar la dirección principal');
    }

    state.alert = { type: 'success', message: '¡Perfil y dirección configurados con éxito!' };
    fetchProfile();
  } catch (err) {
    showAlert('error', err.message);
  }
}

// Operaciones CRUD de Direcciones (Crear / Editar / Marcar Principal / Eliminar)
async function handleSaveAddress(e) {
  e.preventDefault();
  const clienteId = state.coreProfile?.id;
  if (!clienteId) return;

  const etiqueta = state.addressForm.etiqueta.trim() || 'Domicilio residencial';
  const direccionTexto = state.addressForm.direccionTexto.trim();
  const municipioId = state.addressForm.municipioId;
  const piso = state.addressForm.piso.trim();
  const apartamento = state.addressForm.apartamento.trim();
  const referencia = state.addressForm.referencia.trim();
  const esPrincipal = state.addressForm.esPrincipal;

  try {
    if (!direccionTexto) throw new Error("La dirección es obligatoria.");
    if (!municipioId) throw new Error("Debes seleccionar el municipio.");

    let res;
    if (state.editingAddress) {
      // Editar dirección existente via PUT /api/v1/direcciones/{id}
      res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/direcciones/${state.editingAddress.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${state.token}`
        },
        body: JSON.stringify({
          etiqueta,
          direccionTexto,
          municipioId: parseInt(municipioId),
          latitud: 6.2086,
          longitud: -75.5659,
          piso: piso || null,
          apartamento: apartamento || null,
          referencia: referencia || null,
          esPrincipal
        })
      });
    } else {
      // Crear nueva dirección via POST /api/v1/perfil-cliente/{id}/direcciones
      res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/perfil-cliente/${clienteId}/direcciones`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${state.token}`
        },
        body: JSON.stringify({
          etiqueta,
          direccionTexto,
          municipioId: parseInt(municipioId),
          latitud: 6.2086,
          longitud: -75.5659,
          piso: piso || null,
          apartamento: apartamento || null,
          referencia: referencia || null,
          esPrincipal
        })
      });
    }

    if (!res.ok) {
      const data = await safeParseJson(res);
      throw new Error(data.message || 'Error al guardar la dirección');
    }

    const savedAddr = await res.json();
    state.showAddressModal = false;
    state.editingAddress = null;
    if (savedAddr && savedAddr.id) {
      state.requestAddressId = savedAddr.id;
    }
    showAlert('success', '¡Dirección guardada exitosamente!');
    fetchClientAddresses();
  } catch (err) {
    showAlert('error', err.message);
  }
}

async function handleSetPrincipalAddress(direccionId) {
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/direcciones/${direccionId}/principal`, {
      method: 'PATCH',
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.ok) {
      state.requestAddressId = direccionId;
      showAlert('success', 'Dirección establecida como principal');
      fetchClientAddresses();
    }
  } catch (e) {
    showAlert('error', 'Error al establecer dirección principal');
  }
}

async function handleDeleteAddress(direccionId) {
  if (!confirm('¿Estás seguro de eliminar esta dirección?')) return;
  try {
    const res = await fetch(`${API_GATEWAY_URL}/findu-core/api/v1/direcciones/${direccionId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${state.token}` }
    });
    if (res.ok || res.status === 204) {
      showAlert('success', 'Dirección eliminada');
      fetchClientAddresses();
    } else {
      const data = await safeParseJson(res);
      throw new Error(data.message || 'No se puede eliminar la dirección principal');
    }
  } catch (e) {
    showAlert('error', e.message);
  }
}

// Iniciar Wizard de Solicitud de Servicio
function handleStartServiceRequest(servicio) {
  state.selectedServiceForRequest = servicio;
  state.wizardStep = 1;
  state.requestDate = new Date().toISOString().split('T')[0];
  state.requestTime = '09:00';
  state.requestMaxHours = 2;
  state.requestMaxBudget = '';
  state.requestDetails = '';
  state.requestPhotos = [];

  // Seleccionar la dirección principal por defecto si está disponible
  if (state.coreProfile?.direcciones?.length > 0) {
    const principal = state.coreProfile.direcciones.find(d => d.esPrincipal) || state.coreProfile.direcciones[0];
    state.requestAddressId = principal.id;
  }
  render();
}

// Finalizar Solicitud de Servicio (Submit del Wizard)
function submitServiceRequestWizard(e) {
  e.preventDefault();
  const serv = state.selectedServiceForRequest;
  if (!serv) return;

  const chosenAddr = state.coreProfile?.direcciones?.find(d => d.id == state.requestAddressId);
  const addressSummary = chosenAddr 
    ? `${chosenAddr.direccionTexto} (${chosenAddr.municipioNombre || 'Ciudad'})`
    : 'Dirección Registrada';

  const newRequest = {
    id: 'REQ-' + Math.floor(100000 + Math.random() * 900000),
    serviceName: serv.nombre,
    category: serv.descripcion || 'General',
    status: 'SOLICITADO',
    date: `${state.requestDate} ${state.requestTime}`,
    address: addressSummary,
    maxHours: serv.tipoCobro === 'POR_HORA' ? `${state.requestMaxHours} Horas` : 'N/A',
    maxBudget: state.requestMaxBudget ? `$ ${state.requestMaxBudget} COP` : 'A convenir',
    notes: state.requestDetails,
    photosCount: state.requestPhotos.length,
    photos: state.requestPhotos,
    priceType: serv.tipoCobro === 'POR_HORA' ? 'Por Hora' : 'Tarifa Fija'
  };

  state.requests.unshift(newRequest);
  state.selectedServiceForRequest = null;
  state.wizardStep = 1;
  state.requestPhotos = [];
  state.activeTab = 'requests';
  showAlert('success', `¡Solicitud de servicio creada exitosamente para "${serv.nombre}"!`);
}

// Renderizador Estilizado Estilo Mercado Libre para Tarjetas de Dirección
function renderMercadoLibreAddressCard(d, isSelected, showActions = true) {
  const displayPhone = state.regPhone || state.profile?.phone || '';
  const displayPhoneCode = state.regPhoneCode || state.profile?.codPhoneInternational || '+57';
  const userDisplayName = state.coreProfile?.nombreCompleto || state.profile?.username || 'Cliente';

  return `
    <div class="ml-address-card ${d.esPrincipal ? 'principal' : ''} ${isSelected ? 'selected' : ''}" 
         data-id="${d.id}" 
         style="background: rgba(30, 41, 59, 0.7); border: 2px solid ${isSelected ? 'var(--accent-color)' : d.esPrincipal ? 'rgba(99,102,241,0.5)' : 'var(--border-color)'}; border-radius: 16px; padding: 16px; margin-bottom: 12px; position: relative; transition: all 0.25s; cursor: pointer;">
      
      <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 6px;">
        <div style="display: flex; align-items: center; gap: 10px;">
          <input type="radio" name="wizard-address-radio" value="${d.id}" ${isSelected ? 'checked' : ''} style="accent-color: var(--accent-color); width: 18px; height: 18px; cursor: pointer;">
          <h4 style="font-size: 1.05rem; font-weight: 700; color: #fff; margin: 0;">${d.direccionTexto} ${d.piso ? `- Piso ${d.piso}` : ''} ${d.apartamento ? `Apto ${d.apartamento}` : ''}</h4>
        </div>
        
        <div style="display: flex; align-items: center; gap: 6px;">
          ${d.esPrincipal ? `<span style="font-size: 0.7rem; font-weight: 700; padding: 3px 10px; border-radius: 12px; background: rgba(99,102,241,0.25); color: #a5b4fc; border: 1px solid rgba(99,102,241,0.4);">⭐ Principal</span>` : ''}
          ${showActions ? `
            <button type="button" class="edit-addr-btn btn-icon" data-id="${d.id}" style="background: rgba(255,255,255,0.08); border: 1px solid var(--border-color); color: #fff; border-radius: 8px; padding: 4px 8px; font-size: 0.8rem; cursor: pointer;" title="Editar dirección">✏️</button>
            ${!d.esPrincipal ? `<button type="button" class="delete-addr-btn btn-icon" data-id="${d.id}" style="background: rgba(239, 68, 68, 0.15); border: 1px solid rgba(239,68,68,0.3); color: #ef4444; border-radius: 8px; padding: 4px 8px; font-size: 0.8rem; cursor: pointer;" title="Eliminar dirección">🗑️</button>` : ''}
          ` : ''}
        </div>
      </div>

      <div style="padding-left: 28px;">
        <p style="font-size: 0.85rem; color: var(--text-secondary); margin: 0 0 6px 0;">
          ${d.departamento || 'Departamento'} - ${d.municipioNombre || 'Ciudad'} ${d.referencia ? `(${d.referencia})` : ''}
        </p>
        <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap; font-size: 0.8rem; color: #94a3b8;">
          <span style="background: rgba(255,255,255,0.06); padding: 2px 10px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.1); color: #cbd5e1; font-weight: 500;">📍 ${d.etiqueta || 'Domicilio residencial'}</span>
          <span>👤 ${userDisplayName} — 📞 (${displayPhoneCode}) ${displayPhone}</span>
        </div>
        
        ${showActions && !d.esPrincipal ? `
          <button type="button" class="set-principal-btn" data-id="${d.id}" style="background: transparent; border: none; color: var(--accent-color); font-size: 0.8rem; font-weight: 600; cursor: pointer; padding: 6px 0 0 0; text-decoration: underline;">Establecer como principal</button>
        ` : ''}
      </div>
    </div>
  `;
}

// Renderizador General del DOM
function render() {
  let html = '';
  const alertHtml = state.alert.message ? `<div class="alert ${state.alert.type}">${state.alert.message}</div>` : '';

  if (state.currentView === 'login') {
    html = `
      <div class="card">
        <div class="brand">
          <h1>FIND-U</h1>
          <p>Portal de Clientes</p>
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
              <input type="text" id="login-username" placeholder="ej. juan.perez" required value="${state.loginUsername}">
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
          <p>Portal de Clientes</p>
        </div>
        <h2>Registro de Cliente</h2>
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
            <input type="email" id="reg-email" value="${emailValue}" placeholder="ej. juan.perez@gmail.com" ${isGoogle ? 'disabled' : 'required'}>
          </div>

          <div class="form-group">
            <label for="reg-username">Nombre de Usuario</label>
            <input type="text" id="reg-username" value="${usernameValue}" placeholder="ej. juan.perez" required>
          </div>

          <div class="form-group">
            <label for="reg-phone">Teléfono Móvil (Celular)</label>
            <div style="display: flex; gap: 8px;">
              <select id="reg-phone-code" style="width: 140px; background: rgba(15, 23, 42, 0.6); border: 1px solid var(--border-color); border-radius: 12px; padding: 14px 10px; color: var(--text-primary); font-family: inherit; font-size: 0.95rem; outline: none; cursor: pointer;">
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

          <div class="form-group">
            <label for="reg-birthdate">Fecha de Nacimiento</label>
            <input type="date" id="reg-birthdate" required value="${state.fechaNacimiento || ''}">
          </div>

          <button type="submit" class="btn">Finalizar Registro</button>
        </form>

        <div class="switch-auth">
          ¿Ya tienes cuenta? <a href="#" id="go-login">Inicia sesión</a>
        </div>
      </div>
    `;
  } else if (state.currentView === 'complete-profile') {
    if (!state.municipios) {
      fetchMunicipios();
      html = `
        <div class="card">
          <h2>Cargando departamentos y ciudades...</h2>
          <div style="text-align: center; margin: 20px 0;">
            <div style="border: 4px solid rgba(255,255,255,0.1); width: 36px; height: 36px; border-radius: 50%; border-left-color: var(--accent-color); animation: spin 1s linear infinite; display: inline-block;"></div>
          </div>
        </div>
      `;
    } else {
      const depts = [...new Set(state.municipios.map(m => m.departamento))].sort();
      const filteredMunicipios = state.selectedDepartamento 
        ? state.municipios.filter(m => m.departamento === state.selectedDepartamento).sort((a,b) => a.nombre.localeCompare(b.nombre))
        : [];
      
      const deptOptions = `<option value="">Selecciona Departamento</option>` + depts.map(d => `<option value="${d}" ${state.selectedDepartamento === d ? 'selected' : ''}>${d}</option>`).join('');
      const muniOptions = `<option value="">Selecciona Ciudad / Municipio</option>` + filteredMunicipios.map(m => `<option value="${m.id}" ${state.completeProfileForm.municipioId == m.id ? 'selected' : ''}>${m.nombre}</option>`).join('');
      
      const displayPhone = state.regPhone || state.profile?.phone || state.loginPhone || '';
      const displayPhoneCode = state.regPhoneCode || state.profile?.codPhoneInternational || '+57';
      const displayUsername = state.regUsername || state.profile?.username || '';

      html = `
        <div class="card" style="max-width: 520px;">
          <div class="brand">
            <h1>FIND-U</h1>
            <p>Completar Perfil de Cliente</p>
          </div>
          <h2>Paso 2: Completa tus Datos</h2>
          ${alertHtml}
          
          <form id="complete-profile-form">
            <div style="text-align: center; margin-bottom: 24px; background: rgba(255,255,255,0.02); padding: 16px; border-radius: 16px; border: 1px solid var(--border-color);">
              <div style="position: relative; width: 90px; height: 90px; margin: 0 auto;">
                <img id="avatar-preview" src="${state.avatarBase64 || state.profile?.urlImagenPerfil || 'data:image/svg+xml;utf8,<svg xmlns=\'http://www.w3.org/2000/svg\' fill=\'%2364748b\' viewBox=\'0 0 24 24\'><path d=\'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z\'/></svg>'}" style="width: 90px; height: 90px; border-radius: 50%; object-fit: cover; border: 3px solid var(--accent-color); background: rgba(15, 23, 42, 0.8);">
                <button type="button" id="cp-avatar-btn" style="position: absolute; bottom: 0; right: 0; background: var(--accent-color); color: white; border: none; border-radius: 50%; width: 30px; height: 30px; font-size: 14px; cursor: pointer; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 8px rgba(0,0,0,0.4);" title="Cambiar foto de perfil">✏️</button>
              </div>
              <input type="file" id="cp-avatar-file" accept="image/*" style="display: none;">
              <div style="font-size: 1rem; font-weight: 700; color: #fff; margin-top: 10px;">@${displayUsername}</div>
              <p style="font-size: 0.75rem; color: var(--text-secondary); margin-top: 2px;">Haz clic en el lápiz para cargar tu foto de perfil</p>
            </div>

            <h3 style="font-size: 1rem; margin-bottom: 12px; border-bottom: 1px solid var(--border-color); padding-bottom: 6px; color: var(--accent-color);">Datos Personales</h3>
            
            <div class="form-group">
              <label for="cp-name">Nombre Completo</label>
              <input type="text" id="cp-name" value="${state.completeProfileForm.nombreCompleto}" placeholder="ej. Juan Pérez" required>
            </div>

            <div style="display: flex; gap: 10px;">
              <div class="form-group" style="flex: 1;">
                <label for="cp-id-type">Tipo ID</label>
                <select id="cp-id-type" style="width: 100%; background: rgba(15, 23, 42, 0.6); border: 1px solid var(--border-color); border-radius: 12px; padding: 14px 16px; color: var(--text-primary); font-family: inherit; font-size: 0.95rem; outline: none; cursor: pointer;">
                  <option value="CC" ${state.completeProfileForm.tipoIdentificacion === 'CC' ? 'selected' : ''}>CC</option>
                  <option value="CE" ${state.completeProfileForm.tipoIdentificacion === 'CE' ? 'selected' : ''}>CE</option>
                  <option value="TI" ${state.completeProfileForm.tipoIdentificacion === 'TI' ? 'selected' : ''}>TI</option>
                  <option value="PA" ${state.completeProfileForm.tipoIdentificacion === 'PA' ? 'selected' : ''}>PA</option>
                </select>
              </div>
              <div class="form-group" style="flex: 2;">
                <label for="cp-id-number">Número de Identificación</label>
                <input type="text" id="cp-id-number" value="${state.completeProfileForm.numeroIdentificacion}" placeholder="ej. 10456789" required>
              </div>
            </div>

            <div class="form-group">
              <label for="cp-sex">Sexo</label>
              <select id="cp-sex" style="width: 100%; background: rgba(15, 23, 42, 0.6); border: 1px solid var(--border-color); border-radius: 12px; padding: 14px 16px; color: var(--text-primary); font-family: inherit; font-size: 0.95rem; outline: none; cursor: pointer;">
                <option value="M" ${state.completeProfileForm.sexo === 'M' ? 'selected' : ''}>Masculino</option>
                <option value="F" ${state.completeProfileForm.sexo === 'F' ? 'selected' : ''}>Femenino</option>
                <option value="O" ${state.completeProfileForm.sexo === 'O' ? 'selected' : ''}>Otro</option>
              </select>
            </div>

            <div class="form-group">
              <label for="cp-phone">Teléfono Móvil (Registrado)</label>
              <div style="display: flex; gap: 8px;">
                <select id="cp-phone-code" disabled style="width: 130px; background: rgba(15, 23, 42, 0.4); border: 1px solid var(--border-color); border-radius: 12px; padding: 14px 10px; color: var(--text-secondary); font-family: inherit; font-size: 0.9rem; cursor: not-allowed;">
                  <option value="${displayPhoneCode}">${displayPhoneCode} (Col)</option>
                </select>
                <input type="text" id="cp-phone" value="${displayPhone}" disabled style="flex: 1; background: rgba(15, 23, 42, 0.4); color: var(--text-secondary); cursor: not-allowed;" required>
              </div>
              <span style="font-size: 0.75rem; color: var(--text-secondary); margin-top: 4px; display: block;">🔒 Teléfono verificado de la pantalla anterior</span>
            </div>

            <h3 style="font-size: 1rem; margin-top: 24px; margin-bottom: 12px; border-bottom: 1px solid var(--border-color); padding-bottom: 6px; color: var(--accent-color);">Dirección Principal</h3>

            <div class="form-group">
              <label for="cp-departamento">Departamento</label>
              <select id="cp-departamento" required style="width: 100%; background: rgba(15, 23, 42, 0.6); border: 1px solid var(--border-color); border-radius: 12px; padding: 14px 16px; color: var(--text-primary); font-family: inherit; font-size: 0.95rem; outline: none; cursor: pointer;">
                ${deptOptions}
              </select>
            </div>

            <div class="form-group">
              <label for="cp-municipio">Ciudad / Municipio</label>
              <select id="cp-municipio" required style="width: 100%; background: rgba(15, 23, 42, 0.6); border: 1px solid var(--border-color); border-radius: 12px; padding: 14px 16px; color: var(--text-primary); font-family: inherit; font-size: 0.95rem; outline: none; cursor: pointer;">
                ${muniOptions}
              </select>
            </div>

            <div class="form-group">
              <label for="cp-address">Dirección</label>
              <input type="text" id="cp-address" value="${state.completeProfileForm.direccionTexto}" placeholder="ej. Calle 45 # 56-78 Apto 301" required>
            </div>

            <div style="display: flex; gap: 10px;">
              <div class="form-group" style="flex: 1;">
                <label for="cp-floor">Piso (Opcional)</label>
                <input type="text" id="cp-floor" value="${state.completeProfileForm.piso}" placeholder="ej. 3">
              </div>
              <div class="form-group" style="flex: 1;">
                <label for="cp-apartment">Apto/Oficina (Opcional)</label>
                <input type="text" id="cp-apartment" value="${state.completeProfileForm.apartamento}" placeholder="ej. 301">
              </div>
            </div>

            <div class="form-group">
              <label for="cp-reference">Indicaciones / Referencia (Opcional)</label>
              <input type="text" id="cp-reference" value="${state.completeProfileForm.referencia}" placeholder="ej. Frente al parque principal">
            </div>

            <button type="submit" class="btn" style="margin-top: 10px;">Guardar y Activar Perfil</button>
          </form>
        </div>
      `;
    }
  } else if (state.currentView === 'services') {
    const userDisplayName = state.coreProfile?.nombreCompleto || state.profile?.username || 'Cliente';
    const avatarSrc = state.avatarBase64 || state.coreProfile?.urlImagenPerfil || state.profile?.urlImagenPerfil || 'data:image/svg+xml;utf8,<svg xmlns=\'http://www.w3.org/2000/svg\' fill=\'%2364748b\' viewBox=\'0 0 24 24\'><path d=\'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z\'/></svg>';

    // Filtrado de servicios
    let filteredServicios = state.servicios;
    if (state.searchQuery) {
      const q = state.searchQuery.toLowerCase();
      filteredServicios = filteredServicios.filter(s => 
        (s.nombre || '').toLowerCase().includes(q) || (s.descripcion && s.descripcion.toLowerCase().includes(q))
      );
    }

    // Clasificación de Direcciones para Formato Mercado Libre
    const addresses = state.coreProfile?.direcciones || [];
    const principalAddr = addresses.find(d => d.esPrincipal);
    const secondaryAddrs = addresses.filter(d => !d.esPrincipal);

    html = `
      <div style="width: 100%; max-width: 1000px; margin: 0 auto; box-sizing: border-box; padding: 0 10px;">
        <!-- Header Navigation Bar -->
        <header style="display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; background: rgba(30, 41, 59, 0.85); backdrop-filter: blur(12px); border: 1px solid var(--border-color); padding: 12px 18px; border-radius: 18px; margin-bottom: 24px; box-sizing: border-box; width: 100%;">
          <div style="display: flex; align-items: center; gap: 10px;">
            <h1 style="font-size: 1.3rem; font-weight: 800; background: linear-gradient(135deg, #6366f1, #a855f7); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin: 0;">FIND-U</h1>
            <span style="font-size: 0.7rem; background: rgba(99, 102, 241, 0.2); color: #a5b4fc; padding: 3px 8px; border-radius: 20px; border: 1px solid rgba(99,102,241,0.3);">CLIENTE</span>
          </div>

          <nav style="display: flex; gap: 6px; flex-wrap: wrap;">
            <button id="tab-services" class="btn" style="padding: 6px 14px; font-size: 0.85rem; height: auto; width: auto; background: ${state.activeTab === 'services' ? 'var(--accent-color)' : 'transparent'}; border: ${state.activeTab === 'services' ? 'none' : '1px solid transparent'};">🏠 Servicios</button>
            <button id="tab-requests" class="btn" style="padding: 6px 14px; font-size: 0.85rem; height: auto; width: auto; background: ${state.activeTab === 'requests' ? 'var(--accent-color)' : 'transparent'}; border: ${state.activeTab === 'requests' ? 'none' : '1px solid transparent'};">📋 Mis Solicitudes ${state.requests.length ? `(${state.requests.length})` : ''}</button>
            <button id="tab-profile" class="btn" style="padding: 6px 14px; font-size: 0.85rem; height: auto; width: auto; background: ${state.activeTab === 'profile' ? 'var(--accent-color)' : 'transparent'}; border: ${state.activeTab === 'profile' ? 'none' : '1px solid transparent'};">👤 Mi Perfil</button>
          </nav>

          <div style="display: flex; align-items: center; gap: 10px;">
            <div style="display: flex; align-items: center; gap: 8px; cursor: pointer;" id="header-profile-btn" title="Ver mi perfil">
              <img src="${avatarSrc}" style="width: 36px; height: 36px; border-radius: 50%; object-fit: cover; border: 2px solid var(--accent-color);">
            </div>
            <button id="header-logout-btn" style="background: rgba(239, 68, 68, 0.15); color: #ef4444; border: 1px solid rgba(239,68,68,0.3); border-radius: 10px; padding: 6px 12px; font-size: 0.8rem; cursor: pointer; white-space: nowrap;">Salir</button>
          </div>
        </header>

        ${alertHtml}

        ${state.activeTab === 'services' ? `
          <!-- Search & Filter Header -->
          <div style="margin-bottom: 24px;">
            <div style="position: relative; margin-bottom: 16px;">
              <input type="text" id="services-search" placeholder="🔍 Buscar servicio (ej. Plomería, Electricista, Limpieza, Corte...)" value="${state.searchQuery}" style="width: 100%; padding: 14px 20px; border-radius: 14px; background: rgba(30, 41, 59, 0.8); border: 1px solid var(--border-color); color: #fff; font-size: 0.95rem; outline: none; box-sizing: border-box;">
            </div>

            <!-- Categories Container con Flex Wrap -->
            <div style="display: flex; flex-wrap: wrap; gap: 8px; width: 100%; margin-bottom: 16px;">
              <button class="cat-pill ${state.selectedCategoriaId === null ? 'active' : ''}" data-id="all" style="padding: 8px 16px; border-radius: 20px; font-size: 0.85rem; font-weight: 600; border: 1px solid var(--border-color); background: ${state.selectedCategoriaId === null ? 'var(--accent-color)' : 'rgba(30,41,59,0.7)'}; color: #fff; cursor: pointer; display: flex; align-items: center; gap: 6px;">✨ Todas las Categorías</button>
              ${state.categorias.map(c => `
                <button class="cat-pill ${state.selectedCategoriaId === c.id ? 'active' : ''}" data-id="${c.id}" style="padding: 8px 16px; border-radius: 20px; font-size: 0.85rem; font-weight: 600; border: 1px solid var(--border-color); background: ${state.selectedCategoriaId === c.id ? 'var(--accent-color)' : 'rgba(30,41,59,0.7)'}; color: #fff; cursor: pointer; display: flex; align-items: center; gap: 6px;">
                  ${c.iconoUrl ? `<img src="${c.iconoUrl}" style="width: 18px; height: 18px; border-radius: 50%; object-fit: cover;">` : '📁'}
                  ${c.nombre}
                </button>
              `).join('')}
            </div>
          </div>

          <!-- Services Grid (Tarjetas Modernas) -->
          <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 20px;">
            ${filteredServicios.length > 0 ? filteredServicios.map(s => {
              const imgUrl = getServiceImage(s);
              return `
                <div class="card" style="margin: 0; padding: 0; overflow: hidden; display: flex; flex-direction: column; justify-content: space-between; border: 1px solid var(--border-color); background: rgba(30, 41, 59, 0.7); border-radius: 20px; transition: transform 0.25s, box-shadow 0.25s;" onmouseover="this.style.transform='translateY(-6px)'; this.style.boxShadow='0 12px 24px rgba(0,0,0,0.4)'" onmouseout="this.style.transform='translateY(0)'; this.style.boxShadow='none'">
                  
                  <div style="position: relative; width: 100%; height: 150px; overflow: hidden; background: #0f172a;">
                    <img src="${imgUrl}" alt="${s.nombre}" style="width: 100%; height: 100%; object-fit: cover;">
                    <span style="position: absolute; top: 10px; right: 10px; font-size: 0.7rem; font-weight: 700; padding: 4px 10px; border-radius: 12px; background: rgba(15, 23, 42, 0.85); backdrop-filter: blur(4px); color: #c084fc; border: 1px solid rgba(168,85,247,0.4);">
                      ${s.tipoCobro === 'POR_HORA' ? '⏱ POR HORA' : '🏷 TARIFA FIJA'}
                    </span>
                  </div>

                  <div style="padding: 16px; flex: 1; display: flex; flex-direction: column; justify-content: space-between;">
                    <div>
                      <h3 style="font-size: 1.05rem; font-weight: 700; color: #fff; margin: 0 0 6px 0;">${s.nombre}</h3>
                      <p style="font-size: 0.82rem; color: var(--text-secondary); line-height: 1.45; margin: 0 0 14px 0; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;">${s.descripcion || 'Servicio profesional garantizado por proveedores certificados en FINDU.'}</p>
                    </div>

                    <button class="btn start-wizard-btn" data-id="${s.id}" style="width: 100%; padding: 10px; font-size: 0.88rem; font-weight: 600; border-radius: 12px; background: linear-gradient(135deg, #6366f1, #a855f7);">Solicitar Servicio</button>
                  </div>
                </div>
              `;
            }).join('') : `
              <div style="grid-column: 1 / -1; text-align: center; padding: 40px; background: rgba(30,41,59,0.4); border-radius: 16px; border: 1px solid var(--border-color);">
                <p style="color: var(--text-secondary); margin: 0;">No se encontraron servicios que coincidan con el filtro seleccionado.</p>
              </div>
            `}
          </div>
        ` : state.activeTab === 'requests' ? `
          <!-- Mis Solicitudes View -->
          <div class="card" style="margin: 0;">
            <h2 style="font-size: 1.3rem; margin-bottom: 16px;">Mis Solicitudes de Servicio</h2>
            ${state.requests.length > 0 ? `
              <div style="display: flex; flex-direction: column; gap: 14px;">
                ${state.requests.map(r => `
                  <div style="background: rgba(15,23,42,0.7); padding: 18px; border-radius: 16px; border: 1px solid var(--border-color); display: flex; flex-direction: column; gap: 10px;">
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                      <div style="display: flex; align-items: center; gap: 10px;">
                        <span style="font-size: 0.8rem; font-weight: 800; color: var(--accent-color); background: rgba(99,102,241,0.15); padding: 3px 8px; border-radius: 8px;">${r.id}</span>
                        <h4 style="font-size: 1.05rem; color: #fff; margin: 0;">${r.serviceName}</h4>
                      </div>
                      <span style="font-size: 0.75rem; padding: 4px 12px; border-radius: 12px; background: rgba(34, 197, 94, 0.2); color: #4ade80; font-weight: 700; border: 1px solid rgba(34,197,94,0.3);">${r.status}</span>
                    </div>

                    <div style="font-size: 0.85rem; color: var(--text-secondary); line-height: 1.5;">
                      <div>📍 <strong>Atención en:</strong> ${r.address}</div>
                      <div>📅 <strong>Fecha/Hora:</strong> ${r.date} | ⏱ <strong>Tipo:</strong> ${r.priceType} (${r.maxHours})</div>
                      <div>💰 <strong>Presupuesto Máximo:</strong> ${r.maxBudget}</div>
                      ${r.notes ? `<div style="margin-top: 4px;">📝 <strong>Detalles:</strong> ${r.notes}</div>` : ''}
                    </div>

                    ${r.photos && r.photos.length > 0 ? `
                      <div style="display: flex; gap: 8px; overflow-x: auto; padding-top: 6px;">
                        ${r.photos.map(img => `<img src="${img}" style="width: 50px; height: 50px; border-radius: 8px; object-fit: cover; border: 1px solid var(--border-color);">`).join('')}
                      </div>
                    ` : ''}
                  </div>
                `).join('')}
              </div>
            ` : `
              <div style="text-align: center; padding: 30px; color: var(--text-secondary);">
                <p>Aún no has realizado ninguna solicitud de servicio.</p>
                <button id="go-to-services-btn" class="btn" style="margin-top: 10px; width: auto; padding: 8px 20px;">Explorar Servicios</button>
              </div>
            `}
          </div>
        ` : `
          <!-- Mi Perfil & Gestión Completa de Direcciones Estilo Mercado Libre -->
          <div class="card" style="margin: 0;">
            <div style="text-align: center; margin-bottom: 24px;">
              <img src="${avatarSrc}" style="width: 90px; height: 90px; border-radius: 50%; object-fit: cover; border: 3px solid var(--accent-color); margin-bottom: 10px;">
              <h2 style="margin: 0; font-size: 1.2rem;">${userDisplayName}</h2>
              <span style="font-size: 0.85rem; color: var(--accent-color);">@${state.profile?.username || ''}</span>
            </div>

            <div class="profile-info" style="margin-bottom: 24px;">
              <div class="profile-field"><span>Correo:</span><span>${state.profile?.email || ''}</span></div>
              <div class="profile-field"><span>Teléfono:</span><span>(${state.regPhoneCode || '+57'}) ${state.regPhone || state.profile?.phone || ''}</span></div>
              <div class="profile-field"><span>Documento:</span><span>${state.coreProfile?.tipoIdentificacion || 'CC'} ${state.coreProfile?.numeroIdentificacion || ''}</span></div>
              <div class="profile-field"><span>Sexo:</span><span>${state.coreProfile?.sexo === 'M' ? 'Masculino' : state.coreProfile?.sexo === 'F' ? 'Femenino' : 'Otro'}</span></div>
              <div class="profile-field"><span>Estado Perfil:</span><span class="badge" style="background: #22c55e;">${state.coreProfile?.estado || 'ACTIVO'}</span></div>
            </div>

            <!-- Sección de Mis Direcciones (Estilo Mercado Libre) -->
            <div style="border-top: 1px solid var(--border-color); padding-top: 20px;">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                <h3 style="font-size: 1.1rem; color: #fff; margin: 0;">Mis Direcciones de Entrega / Atención</h3>
                <button type="button" id="profile-add-addr-btn" class="btn" style="width: auto; padding: 6px 14px; font-size: 0.82rem;">➕ Nueva Dirección</button>
              </div>

              ${principalAddr ? `
                <div style="margin-bottom: 14px;">
                  <span style="font-size: 0.75rem; font-weight: 700; color: var(--accent-color); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 8px; display: block;">Dirección Principal</span>
                  ${renderMercadoLibreAddressCard(principalAddr, false, true)}
                </div>
              ` : ''}

              ${secondaryAddrs.length > 0 ? `
                <div style="border-top: 2px dashed var(--border-color); margin: 18px 0; padding-top: 14px;">
                  <span style="font-size: 0.75rem; font-weight: 700; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 10px; display: block;">Otras Direcciones</span>
                  ${secondaryAddrs.map(d => renderMercadoLibreAddressCard(d, false, true)).join('')}
                </div>
              ` : ''}
            </div>

            <button id="logout-btn-profile" class="btn" style="background: transparent; border: 1px solid var(--accent-color); color: var(--accent-color); margin-top: 24px;">Cerrar Sesión</button>
          </div>
        `}

        <!-- MODAL WIZARD MULTI-PASO DE SOLICITUD DE SERVICIO -->
        ${state.selectedServiceForRequest ? `
          <div style="position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(15, 23, 42, 0.85); backdrop-filter: blur(8px); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 20px;">
            <div class="card" style="max-width: 580px; width: 100%; margin: 0; max-height: 90vh; overflow-y: auto; box-shadow: 0 20px 40px rgba(0,0,0,0.6); border-radius: 24px;">
              
              <!-- Wizard Header & Steps Indicator -->
              <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border-color); padding-bottom: 14px; margin-bottom: 20px;">
                <div>
                  <h2 style="font-size: 1.25rem; margin: 0; color: #fff;">Solicitar ${state.selectedServiceForRequest.nombre}</h2>
                  <span style="font-size: 0.8rem; color: var(--accent-color);">Paso ${state.wizardStep} de 4</span>
                </div>
                <button type="button" id="close-wizard-btn" style="background: transparent; border: none; color: var(--text-secondary); font-size: 1.4rem; cursor: pointer;">✕</button>
              </div>

              <!-- Bar de Pasos Visual -->
              <div style="display: flex; gap: 6px; margin-bottom: 24px;">
                <div style="flex: 1; height: 4px; border-radius: 4px; background: ${state.wizardStep >= 1 ? 'var(--accent-color)' : 'rgba(255,255,255,0.1)'};"></div>
                <div style="flex: 1; height: 4px; border-radius: 4px; background: ${state.wizardStep >= 2 ? 'var(--accent-color)' : 'rgba(255,255,255,0.1)'};"></div>
                <div style="flex: 1; height: 4px; border-radius: 4px; background: ${state.wizardStep >= 3 ? 'var(--accent-color)' : 'rgba(255,255,255,0.1)'};"></div>
                <div style="flex: 1; height: 4px; border-radius: 4px; background: ${state.wizardStep >= 4 ? 'var(--accent-color)' : 'rgba(255,255,255,0.1)'};"></div>
              </div>

              <!-- PASO 1: SELECCIÓN DE DIRECCIÓN (ESTILO MERCADO LIBRE) -->
              ${state.wizardStep === 1 ? `
                <div>
                  <h3 style="font-size: 1.05rem; color: #fff; margin-bottom: 6px;">📍 Paso 1: Selecciona la Dirección de Atención</h3>
                  <p style="font-size: 0.82rem; color: var(--text-secondary); margin-bottom: 16px;">Selecciona dónde el especialista prestará el servicio o edita/agrega una dirección.</p>

                  ${principalAddr ? `
                    <div style="margin-bottom: 12px;">
                      <span style="font-size: 0.72rem; font-weight: 700; color: var(--accent-color); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 6px; display: block;">Dirección Principal</span>
                      ${renderMercadoLibreAddressCard(principalAddr, state.requestAddressId == principalAddr.id, true)}
                    </div>
                  ` : ''}

                  ${secondaryAddrs.length > 0 ? `
                    <div style="border-top: 2px dashed var(--border-color); margin: 16px 0; padding-top: 12px;">
                      <span style="font-size: 0.72rem; font-weight: 700; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 8px; display: block;">Otras Direcciones</span>
                      ${secondaryAddrs.map(d => renderMercadoLibreAddressCard(d, state.requestAddressId == d.id, true)).join('')}
                    </div>
                  ` : ''}

                  <button type="button" id="wizard-add-addr-btn" style="width: 100%; padding: 12px; background: rgba(99, 102, 241, 0.1); border: 2px dashed var(--accent-color); color: #a5b4fc; border-radius: 14px; font-weight: 600; font-size: 0.88rem; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; margin-top: 12px;">
                    ➕ Agregar una nueva dirección
                  </button>

                  <div style="display: flex; justify-content: flex-end; margin-top: 24px;">
                    <button type="button" id="wizard-to-step2" class="btn" style="width: auto; padding: 10px 24px;">Continuar (Paso 2) ➔</button>
                  </div>
                </div>
              ` : state.wizardStep === 2 ? `
                <!-- PASO 2: CONFIGURACIÓN Y PRESUPUESTO -->
                <div>
                  <h3 style="font-size: 1.05rem; color: #fff; margin-bottom: 6px;">⚙️ Paso 2: Detalles del Servicio y Presupuesto</h3>
                  <p style="font-size: 0.82rem; color: var(--text-secondary); margin-bottom: 16px;">Indica cuándo requieres la atención y tu presupuesto estimado.</p>

                  <div style="display: flex; gap: 12px; margin-bottom: 16px;">
                    <div class="form-group" style="flex: 1; margin: 0;">
                      <label for="w-date">Fecha Deseada</label>
                      <input type="date" id="w-date" value="${state.requestDate}" required>
                    </div>
                    <div class="form-group" style="flex: 1; margin: 0;">
                      <label for="w-time">Hora Estimada</label>
                      <input type="time" id="w-time" value="${state.requestTime}" required>
                    </div>
                  </div>

                  ${state.selectedServiceForRequest.tipoCobro === 'POR_HORA' ? `
                    <div class="form-group">
                      <label for="w-max-hours">Duración Máxima (Horas)</label>
                      <input type="number" id="w-max-hours" value="${state.requestMaxHours}" min="1" max="24" placeholder="ej. 2">
                    </div>
                  ` : ''}

                  <div class="form-group">
                    <label for="w-budget">Presupuesto Máximo Estimado (COP)</label>
                    <input type="number" id="w-budget" value="${state.requestMaxBudget}" placeholder="ej. 150000">
                  </div>

                  <div class="form-group">
                    <label for="w-details">Detalles del Servicio / Indicaciones del Problema</label>
                    <textarea id="w-details" rows="3" placeholder="Describe lo que necesitas (ej. Daño en tubería bajo el lavamanos, requiere cambio de sifón)..." style="width: 100%; background: rgba(15, 23, 42, 0.6); border: 1px solid var(--border-color); border-radius: 12px; padding: 12px; color: #fff; outline: none; font-family: inherit; font-size: 0.9rem; resize: vertical;">${state.requestDetails}</textarea>
                  </div>

                  <div style="display: flex; justify-content: space-between; margin-top: 24px;">
                    <button type="button" id="wizard-back-step1" class="btn" style="background: transparent; border: 1px solid var(--border-color); width: auto; padding: 10px 20px;">⬅ Atrás</button>
                    <button type="button" id="wizard-to-step3" class="btn" style="width: auto; padding: 10px 24px;">Continuar (Paso 3) ➔</button>
                  </div>
                </div>
              ` : state.wizardStep === 3 ? `
                <!-- PASO 3: FOTOS DEL SERVICIO (HASTA 10 FOTOS BASE64 / S3) -->
                <div>
                  <h3 style="font-size: 1.05rem; color: #fff; margin-bottom: 6px;">📷 Paso 3: Adjuntar Fotos de Referencia</h3>
                  <p style="font-size: 0.82rem; color: var(--text-secondary); margin-bottom: 16px;">Sube hasta 10 fotos del daño o espacio para cotizar con precisión.</p>

                  <div style="border: 2px dashed var(--accent-color); background: rgba(99,102,241,0.05); border-radius: 16px; padding: 20px; text-align: center; margin-bottom: 16px; cursor: pointer;" id="w-photo-dropzone">
                    <input type="file" id="w-photo-input" accept="image/*" multiple style="display: none;">
                    <div style="font-size: 2rem; margin-bottom: 6px;">📸</div>
                    <div style="font-size: 0.9rem; font-weight: 600; color: #fff;">Haz clic aquí para seleccionar fotos</div>
                    <span style="font-size: 0.75rem; color: var(--text-secondary); display: block; margin-top: 4px;">Adjuntadas ${state.requestPhotos.length} de 10 fotos (Base64 / S3 Bucket)</span>
                  </div>

                  <!-- Grid de Previsualizaciones -->
                  <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(80px, 1fr)); gap: 10px; margin-bottom: 20px;">
                    ${state.requestPhotos.map((imgBase64, idx) => `
                      <div style="position: relative; width: 100%; height: 80px; border-radius: 10px; overflow: hidden; border: 1px solid var(--accent-color);">
                        <img src="${imgBase64}" style="width: 100%; height: 100%; object-fit: cover;">
                        <button type="button" class="remove-photo-btn" data-idx="${idx}" style="position: absolute; top: 4px; right: 4px; background: rgba(239,68,68,0.9); color: white; border: none; border-radius: 50%; width: 22px; height: 22px; font-size: 11px; cursor: pointer; display: flex; align-items: center; justify-content: center;">✕</button>
                      </div>
                    `).join('')}
                  </div>

                  <div style="display: flex; justify-content: space-between; margin-top: 24px;">
                    <button type="button" id="wizard-back-step2" class="btn" style="background: transparent; border: 1px solid var(--border-color); width: auto; padding: 10px 20px;">⬅ Atrás</button>
                    <button type="button" id="wizard-to-step4" class="btn" style="width: auto; padding: 10px 24px;">Continuar (Resumen) ➔</button>
                  </div>
                </div>
              ` : `
                <!-- PASO 4: RESUMEN Y CONFIRMACIÓN -->
                <div>
                  <h3 style="font-size: 1.05rem; color: #fff; margin-bottom: 6px;">🚀 Paso 4: Revisa y Confirma tu Solicitud</h3>
                  <p style="font-size: 0.82rem; color: var(--text-secondary); margin-bottom: 16px;">Verifica todos los datos antes de publicar tu requerimiento a los especialistas.</p>

                  <form id="wizard-final-form">
                    <div style="background: rgba(15,23,42,0.7); border: 1px solid var(--border-color); border-radius: 16px; padding: 16px; margin-bottom: 20px; font-size: 0.88rem; color: #e2e8f0; line-height: 1.6;">
                      <div style="border-bottom: 1px solid var(--border-color); padding-bottom: 8px; margin-bottom: 8px; font-weight: 700; color: #fff;">
                        🛠 Servicio: ${state.selectedServiceForRequest.nombre}
                      </div>
                      
                      ${(() => {
                        const chosen = state.coreProfile?.direcciones?.find(d => d.id == state.requestAddressId);
                        return chosen ? `<div>📍 <strong>Dirección:</strong> ${chosen.direccionTexto} (${chosen.municipioNombre || 'Ciudad'})</div>` : '';
                      })()}
                      
                      <div>📅 <strong>Fecha/Hora:</strong> ${state.requestDate} a las ${state.requestTime}</div>
                      ${state.selectedServiceForRequest.tipoCobro === 'POR_HORA' ? `<div>⏱ <strong>Duración Estimada:</strong> ${state.requestMaxHours} Horas</div>` : ''}
                      <div>💰 <strong>Presupuesto Máximo:</strong> ${state.requestMaxBudget ? `$ ${state.requestMaxBudget} COP` : 'A convenir con especialista'}</div>
                      <div>📝 <strong>Detalles:</strong> ${state.requestDetails || 'Sin especificaciones adicionales'}</div>
                      <div>📷 <strong>Fotos Adjuntas:</strong> ${state.requestPhotos.length} fotos cargadas</div>
                    </div>

                    <div style="display: flex; justify-content: space-between;">
                      <button type="button" id="wizard-back-step3" class="btn" style="background: transparent; border: 1px solid var(--border-color); width: auto; padding: 10px 20px;">⬅ Atrás</button>
                      <button type="submit" class="btn" style="width: auto; padding: 10px 28px; background: linear-gradient(135deg, #22c55e, #16a34a);">Confirmar y Publicar</button>
                    </div>
                  </form>
                </div>
              `}

            </div>
          </div>
        ` : ''}

        <!-- MODAL FLOTANTE DE AGREGAR / EDITAR DIRECCIÓN -->
        ${state.showAddressModal ? `
          <div style="position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.8); backdrop-filter: blur(6px); display: flex; align-items: center; justify-content: center; z-index: 1100; padding: 20px;">
            <div class="card" style="max-width: 480px; width: 100%; margin: 0; animation: fadeIn 0.2s ease-out;">
              <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border-color); padding-bottom: 10px; margin-bottom: 16px;">
                <h3 style="font-size: 1.1rem; margin: 0; color: #fff;">${state.editingAddress ? 'Editar Dirección' : 'Agregar Nueva Dirección'}</h3>
                <button type="button" id="close-addr-modal-btn" style="background: transparent; border: none; color: var(--text-secondary); font-size: 1.2rem; cursor: pointer;">✕</button>
              </div>

              <form id="address-form">
                <div class="form-group">
                  <label for="addr-tag">Etiqueta de la Dirección</label>
                  <select id="addr-tag" style="width: 100%; background: rgba(15, 23, 42, 0.6); border: 1px solid var(--border-color); border-radius: 12px; padding: 12px; color: #fff;">
                    <option value="Domicilio residencial" ${state.addressForm.etiqueta === 'Domicilio residencial' ? 'selected' : ''}>🏠 Domicilio residencial</option>
                    <option value="Trabajo / Oficina" ${state.addressForm.etiqueta === 'Trabajo / Oficina' ? 'selected' : ''}>🏢 Trabajo / Oficina</option>
                    <option value="Casa Familiar" ${state.addressForm.etiqueta === 'Casa Familiar' ? 'selected' : ''}>👨‍👩‍👧 Casa Familiar</option>
                    <option value="Otro" ${state.addressForm.etiqueta === 'Otro' ? 'selected' : ''}>📍 Otro</option>
                  </select>
                </div>

                <div class="form-group">
                  <label for="addr-departamento">Departamento</label>
                  <select id="addr-departamento" required style="width: 100%; background: rgba(15, 23, 42, 0.6); border: 1px solid var(--border-color); border-radius: 12px; padding: 12px; color: #fff;">
                    <option value="">Selecciona Departamento</option>
                    ${[...new Set((state.municipios || []).map(m => m.departamento))].sort().map(d => `
                      <option value="${d}" ${state.addressForm.departamento === d ? 'selected' : ''}>${d}</option>
                    `).join('')}
                  </select>
                </div>

                <div class="form-group">
                  <label for="addr-municipio">Ciudad / Municipio</label>
                  <select id="addr-municipio" required style="width: 100%; background: rgba(15, 23, 42, 0.6); border: 1px solid var(--border-color); border-radius: 12px; padding: 12px; color: #fff;">
                    <option value="">Selecciona Municipio</option>
                    ${(state.municipios || []).filter(m => !state.addressForm.departamento || m.departamento === state.addressForm.departamento).map(m => `
                      <option value="${m.id}" ${state.addressForm.municipioId == m.id ? 'selected' : ''}>${m.nombre}</option>
                    `).join('')}
                  </select>
                </div>

                <div class="form-group">
                  <label for="addr-texto">Dirección</label>
                  <input type="text" id="addr-texto" value="${state.addressForm.direccionTexto}" placeholder="ej. Calle 7c # 64a-30 casa 1" required>
                </div>

                <div style="display: flex; gap: 10px;">
                  <div class="form-group" style="flex: 1;">
                    <label for="addr-piso">Piso (Opcional)</label>
                    <input type="text" id="addr-piso" value="${state.addressForm.piso}" placeholder="ej. 3">
                  </div>
                  <div class="form-group" style="flex: 1;">
                    <label for="addr-apto">Apto (Opcional)</label>
                    <input type="text" id="addr-apto" value="${state.addressForm.apartamento}" placeholder="ej. 301">
                  </div>
                </div>

                <div class="form-group">
                  <label for="addr-ref">Indicaciones / Referencia</label>
                  <input type="text" id="addr-ref" value="${state.addressForm.referencia}" placeholder="ej. Vereda carrizales 17 km via palmas">
                </div>

                <div style="display: flex; gap: 10px; margin-top: 20px;">
                  <button type="button" id="cancel-addr-btn" class="btn" style="background: transparent; border: 1px solid var(--border-color); flex: 1;">Cancelar</button>
                  <button type="submit" class="btn" style="flex: 1;">Guardar Dirección</button>
                </div>
              </form>
            </div>
          </div>
        ` : ''}
      </div>
    `;
  }

  app.innerHTML = html;

  // Ligar eventos del DOM
  if (state.currentView === 'login') {
    document.getElementById('login-form')?.addEventListener('submit', handleLogin);
    document.getElementById('go-register')?.addEventListener('click', (e) => { e.preventDefault(); setView('register'); });
    document.getElementById('go-forgot')?.addEventListener('click', (e) => {
      e.preventDefault();
      state.recoveryStep = 1;
      setView('forgot-password');
    });
    document.getElementById('login-method-user')?.addEventListener('click', () => { state.loginMethod = 'user'; render(); });
    document.getElementById('login-method-phone')?.addEventListener('click', () => { state.loginMethod = 'phone'; render(); });
    initGoogleSignIn();
  } else if (state.currentView === 'register') {
    document.getElementById('register-form')?.addEventListener('submit', handleRegister);
    document.getElementById('go-login')?.addEventListener('click', (e) => { e.preventDefault(); setView('login'); });
    if (!state.googleData) initGoogleSignIn();
  } else if (state.currentView === 'forgot-password') {
    if (state.recoveryStep === 1) {
      document.getElementById('request-recovery-form')?.addEventListener('submit', handleRequestRecovery);
      document.getElementById('rec-method-email')?.addEventListener('click', () => { state.recoveryMethod = 'email'; render(); });
      document.getElementById('rec-method-phone')?.addEventListener('click', () => { state.recoveryMethod = 'phone'; render(); });
    } else {
      document.getElementById('reset-password-form')?.addEventListener('submit', handleResetPassword);
      document.getElementById('go-back-step1')?.addEventListener('click', (e) => {
        e.preventDefault();
        state.recoveryStep = 1;
        state.alert = { type: '', message: '' };
        render();
      });
    }
    document.getElementById('go-login-recovery')?.addEventListener('click', (e) => { e.preventDefault(); setView('login'); });
  } else if (state.currentView === 'complete-profile') {
    const cpForm = document.getElementById('complete-profile-form');
    if (cpForm) {
      document.getElementById('cp-name')?.addEventListener('input', (e) => state.completeProfileForm.nombreCompleto = e.target.value);
      document.getElementById('cp-id-number')?.addEventListener('input', (e) => state.completeProfileForm.numeroIdentificacion = e.target.value);
      document.getElementById('cp-id-type')?.addEventListener('change', (e) => state.completeProfileForm.tipoIdentificacion = e.target.value);
      document.getElementById('cp-sex')?.addEventListener('change', (e) => state.completeProfileForm.sexo = e.target.value);
      document.getElementById('cp-address')?.addEventListener('input', (e) => state.completeProfileForm.direccionTexto = e.target.value);
      document.getElementById('cp-floor')?.addEventListener('input', (e) => state.completeProfileForm.piso = e.target.value);
      document.getElementById('cp-apartment')?.addEventListener('input', (e) => state.completeProfileForm.apartamento = e.target.value);
      document.getElementById('cp-reference')?.addEventListener('input', (e) => state.completeProfileForm.referencia = e.target.value);

      cpForm.addEventListener('submit', handleCompleteProfile);
      document.getElementById('cp-departamento')?.addEventListener('change', (e) => { state.selectedDepartamento = e.target.value; render(); });
      document.getElementById('cp-municipio')?.addEventListener('change', (e) => { state.completeProfileForm.municipioId = e.target.value; });

      const avatarBtn = document.getElementById('cp-avatar-btn');
      const avatarFile = document.getElementById('cp-avatar-file');
      if (avatarBtn && avatarFile) {
        avatarBtn.addEventListener('click', () => avatarFile.click());
        avatarFile.addEventListener('change', (e) => {
          const file = e.target.files[0];
          if (file) {
            const reader = new FileReader();
            reader.onload = (evt) => {
              state.avatarBase64 = evt.target.result;
              const imgPreview = document.getElementById('avatar-preview');
              if (imgPreview) imgPreview.src = state.avatarBase64;
            };
            reader.readAsDataURL(file);
          }
        });
      }
    }
  } else if (state.currentView === 'services') {
    document.getElementById('tab-services')?.addEventListener('click', () => { state.activeTab = 'services'; render(); });
    document.getElementById('tab-requests')?.addEventListener('click', () => { state.activeTab = 'requests'; render(); });
    document.getElementById('tab-profile')?.addEventListener('click', () => { state.activeTab = 'profile'; render(); });
    document.getElementById('header-profile-btn')?.addEventListener('click', () => { state.activeTab = 'profile'; render(); });
    document.getElementById('header-logout-btn')?.addEventListener('click', logout);
    document.getElementById('logout-btn-profile')?.addEventListener('click', logout);
    document.getElementById('go-to-services-btn')?.addEventListener('click', () => { state.activeTab = 'services'; render(); });

    // Búsqueda de Servicios
    const searchInput = document.getElementById('services-search');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        state.searchQuery = e.target.value;
        render();
        const updatedSearch = document.getElementById('services-search');
        if (updatedSearch) {
          updatedSearch.focus();
          updatedSearch.setSelectionRange(updatedSearch.value.length, updatedSearch.value.length);
        }
      });
    }

    // Filtro por categoría
    document.querySelectorAll('.cat-pill').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const catId = e.currentTarget.getAttribute('data-id');
        state.selectedCategoriaId = catId === 'all' ? null : parseInt(catId);
        render();
        fetchServicios(state.selectedCategoriaId);
      });
    });

    // Iniciar Wizard Solicitar Servicio
    document.querySelectorAll('.start-wizard-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const servId = parseInt(e.currentTarget.getAttribute('data-id'));
        const serv = state.servicios.find(s => s.id === servId);
        if (serv) handleStartServiceRequest(serv);
      });
    });

    // Botón Agregar Dirección (Perfil)
    document.getElementById('profile-add-addr-btn')?.addEventListener('click', () => {
      state.editingAddress = null;
      state.addressForm = { etiqueta: 'Domicilio residencial', direccionTexto: '', departamento: '', municipioId: '', piso: '', apartamento: '', referencia: '', esPrincipal: false };
      state.showAddressModal = true;
      if (!state.municipios) fetchMunicipios();
      render();
    });

    // Eventos de Tarjetas Mercado Libre (Radio, Editar, Set Principal, Eliminar)
    document.querySelectorAll('input[name="wizard-address-radio"]').forEach(radio => {
      radio.addEventListener('change', (e) => {
        state.requestAddressId = parseInt(e.target.value);
        render();
      });
    });

    document.querySelectorAll('.edit-addr-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const addrId = parseInt(e.currentTarget.getAttribute('data-id'));
        const addr = state.coreProfile?.direcciones?.find(d => d.id === addrId);
        if (addr) {
          state.editingAddress = addr;
          state.addressForm = {
            etiqueta: addr.etiqueta || 'Domicilio residencial',
            direccionTexto: addr.direccionTexto || '',
            departamento: addr.departamento || '',
            municipioId: addr.municipioId || '',
            piso: addr.piso || '',
            apartamento: addr.apartamento || '',
            referencia: addr.referencia || '',
            esPrincipal: addr.esPrincipal
          };
          state.showAddressModal = true;
          if (!state.municipios) fetchMunicipios();
          render();
        }
      });
    });

    document.querySelectorAll('.set-principal-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const addrId = parseInt(e.currentTarget.getAttribute('data-id'));
        handleSetPrincipalAddress(addrId);
      });
    });

    document.querySelectorAll('.delete-addr-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const addrId = parseInt(e.currentTarget.getAttribute('data-id'));
        handleDeleteAddress(addrId);
      });
    });

    // Eventos del Wizard Multi-Paso
    if (state.selectedServiceForRequest) {
      document.getElementById('close-wizard-btn')?.addEventListener('click', () => {
        state.selectedServiceForRequest = null;
        render();
      });

      document.getElementById('wizard-add-addr-btn')?.addEventListener('click', () => {
        state.editingAddress = null;
        state.addressForm = { etiqueta: 'Domicilio residencial', direccionTexto: '', departamento: '', municipioId: '', piso: '', apartamento: '', referencia: '', esPrincipal: false };
        state.showAddressModal = true;
        if (!state.municipios) fetchMunicipios();
        render();
      });

      // Navegación entre pasos
      document.getElementById('wizard-to-step2')?.addEventListener('click', () => { state.wizardStep = 2; render(); });
      document.getElementById('wizard-back-step1')?.addEventListener('click', () => { state.wizardStep = 1; render(); });
      
      document.getElementById('wizard-to-step3')?.addEventListener('click', () => {
        state.requestDate = document.getElementById('w-date')?.value || state.requestDate;
        state.requestTime = document.getElementById('w-time')?.value || state.requestTime;
        const maxH = document.getElementById('w-max-hours');
        if (maxH) state.requestMaxHours = maxH.value;
        const budget = document.getElementById('w-budget');
        if (budget) state.requestMaxBudget = budget.value;
        const details = document.getElementById('w-details');
        if (details) state.requestDetails = details.value;

        state.wizardStep = 3;
        render();
      });

      document.getElementById('wizard-back-step2')?.addEventListener('click', () => { state.wizardStep = 2; render(); });
      document.getElementById('wizard-to-step4')?.addEventListener('click', () => { state.wizardStep = 4; render(); });
      document.getElementById('wizard-back-step3')?.addEventListener('click', () => { state.wizardStep = 3; render(); });

      // Carga de Fotos (Paso 3 - Máximo 10 fotos)
      const photoDropzone = document.getElementById('w-photo-dropzone');
      const photoInput = document.getElementById('w-photo-input');
      if (photoDropzone && photoInput) {
        photoDropzone.addEventListener('click', () => photoInput.click());
        photoInput.addEventListener('change', (e) => {
          const files = Array.from(e.target.files);
          if (files.length === 0) return;
          
          let remaining = 10 - state.requestPhotos.length;
          if (remaining <= 0) {
            showAlert('error', 'Ya has alcanzado el límite máximo de 10 fotos.');
            return;
          }

          const filesToProcess = files.slice(0, remaining);
          let loaded = 0;
          filesToProcess.forEach(file => {
            const reader = new FileReader();
            reader.onload = (evt) => {
              state.requestPhotos.push(evt.target.result);
              loaded++;
              if (loaded === filesToProcess.length) {
                render();
              }
            };
            reader.readAsDataURL(file);
          });
        });
      }

      document.querySelectorAll('.remove-photo-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
          const idx = parseInt(e.currentTarget.getAttribute('data-idx'));
          state.requestPhotos.splice(idx, 1);
          render();
        });
      });

      document.getElementById('wizard-final-form')?.addEventListener('submit', submitServiceRequestWizard);
    }

    // Modal Flotante de Dirección
    if (state.showAddressModal) {
      document.getElementById('close-addr-modal-btn')?.addEventListener('click', () => { state.showAddressModal = false; render(); });
      document.getElementById('cancel-addr-btn')?.addEventListener('click', () => { state.showAddressModal = false; render(); });
      document.getElementById('address-form')?.addEventListener('submit', handleSaveAddress);

      document.getElementById('addr-tag')?.addEventListener('change', (e) => state.addressForm.etiqueta = e.target.value);
      document.getElementById('addr-texto')?.addEventListener('input', (e) => state.addressForm.direccionTexto = e.target.value);
      document.getElementById('addr-piso')?.addEventListener('input', (e) => state.addressForm.piso = e.target.value);
      document.getElementById('addr-apto')?.addEventListener('input', (e) => state.addressForm.apartamento = e.target.value);
      document.getElementById('addr-ref')?.addEventListener('input', (e) => state.addressForm.referencia = e.target.value);

      document.getElementById('addr-departamento')?.addEventListener('change', (e) => {
        state.addressForm.departamento = e.target.value;
        render();
      });
      document.getElementById('addr-municipio')?.addEventListener('change', (e) => {
        state.addressForm.municipioId = e.target.value;
      });
    }
  }
}

// Inicializar
init();
