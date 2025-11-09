import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { loginUser } from "../api/auth"; // asegúrate que tu api exporte esta función
import "../styles/login.css";

export default function Login() {
    const navigate = useNavigate();

    const [form, setForm] = useState({ username: "", password: "" });
    const [loading, setLoading] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");
    const [touched, setTouched] = useState({});

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
            // Contrato esperado: loginUser devuelve { ok, token, message } o lanza error.
            const res = await loginUser(form);

            if (typeof res === "string") {
                // Por compatibilidad si tu API aún devuelve string de error
                if (res.toLowerCase().includes("error") || res.toLowerCase().includes("incorrectas")) {
                    setErrorMsg(res.replace("⚠️", "").trim() || "Credenciales incorrectas.");
                    return;
                }
                // Si devuelve string “feliz”, navega
                navigate("/home");  // Redirige al Home
                return;
            }

            if (!res || res.ok === false) {
                setErrorMsg((res && res.message) || "Credenciales incorrectas.");
                return;
            }

            // Si guardas token en localStorage/context, hazlo aquí si aplica
            // localStorage.setItem("token", res.token);
            navigate("/home");  // Redirige al Home
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
