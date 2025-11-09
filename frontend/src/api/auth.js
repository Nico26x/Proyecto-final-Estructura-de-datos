// src/api/auth.js
import axios from "axios";

const API = process.env.REACT_APP_API_URL || "http://localhost:8080";

export const http = axios.create({
    baseURL: API,
    headers: { "Content-Type": "application/json" },
});

// --- ENDPOINTS (compatibles con tu backend actual sin DTO) ---

// Login con parámetros query: /api/usuarios/login?username=...&password=...
export async function loginUser({ username, password }) {
    const url = `${API}/api/usuarios/login?username=${encodeURIComponent(username)}&password=${encodeURIComponent(password)}`;
    const { data } = await axios.post(url);
    // Normaliza a { ok, token, message }
    if (data && data.token) return { ok: true, token: data.token };
    if (data && data.error) return { ok: false, message: data.error };
    return { ok: false, message: "Respuesta desconocida" };
}

// Registro: /api/usuarios/registrar?username=...&password=...&nombre=...
export async function registerUser({ username, password, nombre }) {
    const url = `${API}/api/usuarios/registrar?username=${encodeURIComponent(username)}&password=${encodeURIComponent(password)}&nombre=${encodeURIComponent(nombre)}`;
    try {
        const res = await axios.post(url, null);
        return { ok: true, message: res.data };
    } catch (err) {
        if (err.response && err.response.status === 409) {
            // usuario duplicado
            return { ok: false, message: err.response.data || "Usuario ya existe" };
        }
        throw err; // otros errores
    }
}


// Sesión: requiere header Authorization: Bearer <token>
export function getSession(token) {
    return http.get("/api/usuarios/sesion", {
        headers: { Authorization: `Bearer ${token}` },
    });
}

// Export por defecto NOMBRADO (evita warning de ESLint)
const api = { http, loginUser, registerUser, getSession };
export default api;
