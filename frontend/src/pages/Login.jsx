import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { loginUser } from "../api/auth"; // asegúrate que tu api exporte esta función
import "../styles/login.css";

const API = "http://localhost:8080"; // respaldo por si hace falta llamar directo

// === NUEVO: utils de JWT (rol y expiración) ===
function parseJwt(jwt) {
    try {
        const [, payload] = jwt.split(".");
        return JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
    } catch {
        return null;
    }
}
function isTokenValid(jwt) {
    if (!jwt || jwt.split(".").length !== 3) return false;
    const data = parseJwt(jwt);
    if (!data) return false;
    if (!data.exp) return true; // si el backend no pone exp, lo consideramos válido
    const now = Math.floor(Date.now() / 1000);
    return data.exp > now;
}
// obtiene rol de varios posibles campos
function decodeRoleFromToken(jwt) {
    const data = parseJwt(jwt);
    if (!data) return null;
    // back tuyo suele guardar rol como "rol" o "role", pero contemplamos variantes
    const raw = data.rol ?? data.role ?? data.Rol ?? data.Role ?? data.authorities ?? null;
    if (Array.isArray(raw)) {
        // authorities tipo ["ROLE_ADMIN", ...]
        const str = raw.join(",").toUpperCase();
        return str.includes("ADMIN") ? "ADMIN" : (str.includes("USER") ? "USER" : null);
    }
    const s = String(raw || "").toUpperCase();
    if (!s) return null;
    if (s.includes("ADMIN")) return "ADMIN";
    if (s.includes("USER")) return "USER";
    return raw || null;
}

export default function Login() {
    const navigate = useNavigate();

    const [form, setForm] = useState({ username: "", password: "" });
    const [loading, setLoading] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");
    const [touched, setTouched] = useState({});

    // === NUEVO: al abrir login, purga tokens inválidos y redirige si ya hay token válido
    useEffect(() => {
        const userTok = localStorage.getItem("token");
        const adminTok = localStorage.getItem("admin_token");

        const validAdmin = isTokenValid(adminTok);
        const validUser = isTokenValid(userTok);

        if (!validAdmin && adminTok) localStorage.removeItem("admin_token");
        if (!validUser && userTok) localStorage.removeItem("token");

        if (validAdmin || validUser) {
            navigate("/home", { replace: true });
        }
    }, [navigate]);

    const setField = (e) => {
        setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
    };

    const markTouched = (e) => {
        setTouched((prev) => ({ ...prev, [e.target.name]: true }));
    };

    const hasError = (name) => touched[name] && !String(form[name]).trim();

    const onSubmit = async (e) => {
        e.preventDefault();
        setErrorMsg("");
        setLoading(true);
        setTouched({ username: true, password: true });

        if (!form.username.trim() || !form.password.trim()) {
            setLoading(false);
            return;
        }

        try {
            // 1) Intento principal: usar tu API util loginUser(form)
            const res = await loginUser(form);

            // Puede devolver string o un objeto. Probamos a leer el token de varias formas:
            let token =
                (res && (res.token || res?.data?.token)) ||
                null;

            // Además, intentamos capturar el rol si viene explícito
            let role =
                (res && (res.role || res.rol || res?.data?.role || res?.data?.rol || res?.usuario?.rol || res?.usuario?.role)) ||
                null;

            // Si recibiste string con mensaje de error
            if (!token && typeof res === "string") {
                const low = res.toLowerCase();
                if (low.includes("error") || low.includes("incorrectas") || low.includes("inválid")) {
                    setErrorMsg(res.replace("⚠️", "").trim() || "Credenciales incorrectas.");
                    return;
                }
            }

            // 2) Respaldo: si aún no hay token, intenta el endpoint directo del backend
            if (!token) {
                const r = await fetch(
                    `${API}/api/usuarios/login?username=${encodeURIComponent(form.username)}&password=${encodeURIComponent(form.password)}`,
                    { method: "POST" }
                );
                if (!r.ok) {
                    // Intento alterno: body JSON (por si tu backend soporta ambas formas)
                    const r2 = await fetch(`${API}/api/usuarios/login`, {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ username: form.username, password: form.password }),
                    });
                    if (!r2.ok) throw new Error(`Login ${r2.status}`);
                    const json2 = await r2.json();
                    token = json2.token || json2?.data?.token || null;
                    role = role || json2.role || json2.rol || json2?.data?.role || json2?.data?.rol || json2?.usuario?.rol || json2?.usuario?.role || null;
                } else {
                    const json = await r.json();
                    token = json.token || json?.data?.token || null;
                    role = role || json.role || json.rol || json?.data?.role || json?.data?.rol || json?.usuario?.rol || json?.usuario?.role || null;
                }
            }

            if (!token) {
                setErrorMsg("El backend no retornó un token de sesión.");
                return;
            }

            // === NUEVO: valida expiración antes de guardar
            if (!isTokenValid(token)) {
                setErrorMsg("Tu sesión ha expirado. Vuelve a intentarlo.");
                return;
            }

            // === NUEVO: si no llegó el rol, lo deducimos del JWT
            if (!role) {
                role = decodeRoleFromToken(token);
            }

            // ✅ Guardar token según rol (admin → admin_token, user → token)
            if (String(role).toUpperCase() === "ADMIN") {
                localStorage.removeItem("token");       // evitar choques
                localStorage.setItem("admin_token", token);
            } else {
                localStorage.removeItem("admin_token"); // evitar choques
                localStorage.setItem("token", token);
            }

            navigate("/home");
        } catch {
            setErrorMsg("No se pudo iniciar sesión. Intenta de nuevo.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-bg d-flex align-items-center justify-content-center">
            <div className="auth-card shadow-lg">
                {/* === Mismo encabezado/logo que Register === */}
                <div className="text-center mb-3">
                    <div className="logo-note">♫</div>
                    <h1 className="app-title">SyncUp</h1>
                    <p className="app-subtitle">Bienvenido de nuevo</p>
                </div>

                {errorMsg && (
                    <div className="alert alert-danger py-2" role="alert">
                        {errorMsg}
                    </div>
                )}

                <form onSubmit={onSubmit} noValidate>
                    {/* Username */}
                    <div className="mb-3">
                        <label className="form-label text-light-weak">Nombre de usuario</label>
                        <input
                            className={`form-control glass-input ${hasError("username") ? "is-soft-error" : ""}`}
                            name="username"
                            value={form.username}
                            onChange={setField}
                            onBlur={markTouched}
                            placeholder="Nombre de usuario"
                            autoComplete="off"
                            spellCheck={false}
                            autoCorrect="off"
                            autoCapitalize="none"
                            onFocus={(e) => e.currentTarget.setAttribute("autocomplete", "off")}
                        />
                        {hasError("username") && (
                            <small className="text-danger-weak">Este campo es obligatorio.</small>
                        )}
                    </div>

                    {/* Password */}
                    <div className="mb-4">
                        <label className="form-label text-light-weak">Contraseña</label>
                        <input
                            type="password"
                            className={`form-control glass-input ${hasError("password") ? "is-soft-error" : ""}`}
                            name="password"
                            value={form.password}
                            onChange={setField}
                            onBlur={markTouched}
                            placeholder="Contraseña"
                            autoComplete="off"
                            onFocus={(e) => e.currentTarget.setAttribute("autocomplete", "new-password")}
                        />
                        {hasError("password") && (
                            <small className="text-danger-weak">La contraseña es obligatoria.</small>
                        )}
                    </div>

                    <button className="btn btn-danger w-100 rounded-pill fw-semibold" disabled={loading}>
                        {loading ? "Ingresando..." : "Iniciar sesión"}
                    </button>
                </form>

                <div className="text-center mt-3">
                    <span className="text-light-weak me-1">¿No tienes cuenta?</span>
                    <Link to="/register" className="link-accent">
                        Regístrate
                    </Link>
                </div>
            </div>
        </div>
    );
}
