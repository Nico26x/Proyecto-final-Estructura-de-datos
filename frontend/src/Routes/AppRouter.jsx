// Routes/AppRouter.jsx
import { Routes, Route, Navigate } from "react-router-dom";
import Home from "../pages/Home";
import Login from "../pages/Login";
import Register from "../pages/Register";
import { useAuth } from "../context/AuthContext";

// ⬇️ Páginas admin (tus nombres)
import AdminCanciones from "../pages/AdminCanciones";
import AdminUsuarios from "../pages/AdminUsuarios";

// ⬇️ NUEVO: Perfil (usuario no admin)
import Perfil from "../pages/Perfil";

/* ===== Helpers de token (nuevos) ===== */
function readRawToken() {
    return localStorage.getItem("token") || localStorage.getItem("admin_token") || "";
}
function parseJwt(t) {
    try {
        const [, payload] = t.split(".");
        return JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
    } catch {
        return null;
    }
}
function isTokenValid(t) {
    if (!t || t.split(".").length !== 3) return false;
    const data = parseJwt(t);
    if (!data) return false;
    // Si trae exp (segundos UNIX), validar; si no trae, lo consideramos válido.
    if (!data.exp) return true;
    const now = Math.floor(Date.now() / 1000);
    return data.exp > now;
}
function getActiveValidToken() {
    const userTok = localStorage.getItem("token");
    const adminTok = localStorage.getItem("admin_token");

    if (isTokenValid(adminTok)) return adminTok;
    if (adminTok) localStorage.removeItem("admin_token");

    if (isTokenValid(userTok)) return userTok;
    if (userTok) localStorage.removeItem("token");

    return "";
}

/* ===== Tu PrivateRoute (con validación añadida) ===== */
function PrivateRoute({ children }) {
    // tolerante: si el contexto aún no está listo, valida por localStorage
    const authCtx = useAuth?.();
    const ctxIsAuth = authCtx?.isAuthenticated ?? false;

    // 🔁 Aceptar token válido de usuario o de admin
    const validToken = !!getActiveValidToken();
    const isAuthed = ctxIsAuth || validToken;

    return isAuthed ? children : <Navigate to="/login" replace />;
}

/* ===== Helpers admin (actualizados para usar el token válido) ===== */
function isAdminFromLocal() {
    try {
        const t = getActiveValidToken();
        if (!t) return false;
        const data = parseJwt(t);
        const raw =
            data?.rol || data?.role || data?.authorities || data?.Rol || data?.Role || "";
        const role = String(raw).toUpperCase();
        // Acepta 'ADMIN', 'ROLE_ADMIN' o arrays serializados que contengan ADMIN
        return role.includes("ADMIN");
    } catch {
        return false;
    }
}

function AdminRoute({ children }) {
    // 👉 usamos también readRawToken() para evitar warning “no usado”
    const t = getActiveValidToken() || readRawToken();
    if (!t) return <Navigate to="/login" replace />;
    if (!isAdminFromLocal()) return <Navigate to="/home" replace />;
    return children;
}

export default function AppRouter() {
    // 🔁 clave dinámica considera ambos tokens, pero solo si son válidos
    // 👉 usamos también readRawToken() para evitar warning “no usado”
    const token = getActiveValidToken() || readRawToken();
    const key = token ? "auth" : "guest";

    return (
        <Routes key={key}>
            {/* si entras a /, manda a /home (si autenticado) o /login */}
            <Route
                path="/"
                element={
                    token ? <Navigate to="/home" replace /> : <Navigate to="/login" replace />
                }
            />

            {/* /login: si ya tienes token, no te deja volver a ver el login */}
            <Route
                path="/login"
                element={token ? <Navigate to="/home" replace /> : <Login />}
            />

            <Route path="/register" element={<Register />} />

            <Route
                path="/home"
                element={
                    <PrivateRoute>
                        <Home />
                    </PrivateRoute>
                }
            />

            {/* ⬇️ NUEVO: Perfil (solo usuarios NO admin) */}
            <Route
                path="/perfil"
                element={
                    <PrivateRoute>
                        {isAdminFromLocal() ? <Navigate to="/home" replace /> : <Perfil />}
                    </PrivateRoute>
                }
            />

            {/* ⬇️ Rutas ADMIN protegidas */}
            <Route
                path="/admin/canciones"
                element={
                    <AdminRoute>
                        <AdminCanciones />
                    </AdminRoute>
                }
            />
            <Route
                path="/admin/usuarios"
                element={
                    <AdminRoute>
                        <AdminUsuarios />
                    </AdminRoute>
                }
            />

            <Route
                path="*"
                element={<Navigate to={token ? "/home" : "/login"} replace />}
            />
        </Routes>
    );
}
