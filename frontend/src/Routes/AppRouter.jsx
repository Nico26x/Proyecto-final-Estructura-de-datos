// Routes/AppRouter.jsx
import { Routes, Route, Navigate } from "react-router-dom";
import Home from "../pages/Home";
import Login from "../pages/Login";
import Register from "../pages/Register";
import { useAuth } from "../context/AuthContext";

// ⬇️ Páginas admin (tus nombres)
import AdminCanciones from "../pages/AdminCanciones";
import AdminUsuarios from "../pages/AdminUsuarios";

function PrivateRoute({ children }) {
    // tolerante: si el contexto aún no está listo, valida por localStorage
    const authCtx = useAuth?.();
    const ctxIsAuth = authCtx?.isAuthenticated ?? false;

    // 🔁 Aceptar token de usuario o de admin
    const hasUserToken = !!localStorage.getItem("token");
    const hasAdminToken = !!localStorage.getItem("admin_token");
    const hasAnyToken = hasUserToken || hasAdminToken;

    const isAuthed = ctxIsAuth || hasAnyToken;
    return isAuthed ? children : <Navigate to="/login" replace />;
}

// ⬇️ Helpers para proteger rutas admin SIN tocar PrivateRoute
function isAdminFromLocal() {
    try {
        const t =
            localStorage.getItem("token") ||
            localStorage.getItem("admin_token") ||
            "";
        if (!t || !t.includes(".")) return false;
        const [, payload] = t.split(".");
        const data = JSON.parse(
            atob(payload.replace(/-/g, "+").replace(/_/g, "/"))
        );
        const roleRaw =
            data.rol || data.role || data.authorities || data.Rol || data.Role || "";
        const role = String(roleRaw).toUpperCase();
        // Acepta 'ADMIN' o 'ROLE_ADMIN' o arrays que contengan ADMIN
        return role.includes("ADMIN");
    } catch {
        return false;
    }
}

function AdminRoute({ children }) {
    const hasAnyToken = !!(
        localStorage.getItem("token") || localStorage.getItem("admin_token")
    );
    if (!hasAnyToken) return <Navigate to="/login" replace />;
    if (!isAdminFromLocal()) return <Navigate to="/home" replace />;
    return children;
}

export default function AppRouter() {
    // 🔁 clave dinámica considera ambos tokens
    const token =
        localStorage.getItem("token") || localStorage.getItem("admin_token");
    const key = token ? "auth" : "guest";

    return (
        <Routes key={key}>
            {/* si entras a /, manda a /login (o /home si ya estás autenticado) */}
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
