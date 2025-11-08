// src/api/auth.js
import axios from "axios";

const API = process.env.REACT_APP_API_URL || "http://localhost:8080";

export const http = axios.create({
    baseURL: API,
    headers: { "Content-Type": "application/json" },
});

// --- ENDPOINTS (compatibles con tu backend actual sin DTO) ---

// Login con parámetros query: /api/usuarios/login?username=...&password=...
export function loginUser(username, password) {
    return http.post(
        `/api/usuarios/login?username=${encodeURIComponent(username)}&password=${encodeURIComponent(password)}`
    );
}

// Registro: /api/usuarios/registrar?username=...&password=...&nombre=...
export function registerUser({ username, password, nombre }) {
    return http.post("/api/usuarios/registrar", null, {
        params: { username, password, nombre },
    });
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
