import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { registerUser } from "../api/auth";
import "../styles/register.css"; // estilos del formulario oscuro

export default function Register() {
    const navigate = useNavigate();

    const [form, setForm] = useState({ nombre: "", username: "", password: "" });
    const [loading, setLoading] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");
    const [touched, setTouched] = useState({}); // para saber si mostrar error visual

    const setField = (e) => {
        setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
    };

    const markTouched = (e) => {
        setTouched((prev) => ({ ...prev, [e.target.name]: true }));
    };

    const hasError = (name) => {
        // regla simple: requerido
        return touched[name] && !String(form[name]).trim();
    };

    const onSubmit = async (e) => {
        e.preventDefault();
        setErrorMsg("");
        setLoading(true);
        // marcar todos como tocados para validar
        setTouched({ nombre: true, username: true, password: true });

        // validación mínima del front
        if (!form.nombre.trim() || !form.username.trim() || !form.password.trim()) {
            setLoading(false);
            return;
        }

        try {
            // === IMPORTANTE: mantenemos tu contrato actual ===
            // 1) si tu API devuelve un string con “El usuario ya existe” → no navegar
            // 2) si cambiaste registerUser para que devuelva { ok, message }, también lo soportamos
            const result = await registerUser(form);

            // soporte dual
            if (typeof result === "string") {
                if (result.includes("El usuario ya existe") || result.includes("⚠️")) {
                    setErrorMsg(
                        result.replace("⚠️", "").trim() || "El usuario ya existe."
                    );
                    return;
                }
                // si llegó un string feliz, navega
                navigate("/login");
                return;
            }

            if (result && result.ok === false) {
                setErrorMsg(result.message || "El usuario ya existe");
                return; // NO navegar
            }

            navigate("/login");
        } catch (err) {
            setErrorMsg("Error al registrar. Intenta de nuevo.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="register-bg d-flex align-items-center justify-content-center">
            <div className="auth-card shadow-lg">
                <div className="text-center mb-3">
                    <div className="logo-note">♫</div>
                    <h1 className="app-title">SyncUp</h1>
                    <p className="app-subtitle">Únete a la comunidad musical</p>
                </div>

                {errorMsg && (
                    <div className="alert alert-danger py-2" role="alert">
                        {errorMsg}
                    </div>
                )}

                <form onSubmit={onSubmit} noValidate>
                    {/* Nombre completo */}
                    <div className="mb-3">
                        <label className="form-label text-light-weak">Nombre Completo</label>
                        <input
                            className={`form-control glass-input ${
                                hasError("nombre") ? "is-soft-error" : ""
                            }`}
                            name="nombre"
                            value={form.nombre}
                            onChange={setField}
                            onBlur={markTouched}
                            placeholder="escriba el nombre"
                            autoComplete="off"
                            spellCheck={false}
                            autoCorrect="off"
                            autoCapitalize="none"
                            onFocus={(e) => e.currentTarget.setAttribute("autocomplete", "off")}
                        />
                        {hasError("nombre") && (
                            <small className="text-danger-weak">Este campo es obligatorio.</small>
                        )}
                    </div>

                    {/* Username */}
                    <div className="mb-3">
                        <label className="form-label text-light-weak">Nombre de Usuario</label>
                        <input
                            className={`form-control glass-input ${
                                hasError("username") ? "is-soft-error" : ""
                            }`}
                            name="username"
                            value={form.username}
                            onChange={setField}
                            onBlur={markTouched}
                            placeholder="escriba el username"
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
                            className={`form-control glass-input ${
                                hasError("password") ? "is-soft-error" : ""
                            }`}
                            name="password"
                            value={form.password}
                            onChange={setField}
                            onBlur={markTouched}
                            placeholder="••••••••"
                        />
                        {hasError("password") && (
                            <small className="text-danger-weak">La contraseña es obligatoria.</small>
                        )}
                    </div>

                    <button className="btn btn-danger w-100 rounded-pill fw-semibold" disabled={loading}>
                        {loading ? "Creando..." : "Registrarse"}
                    </button>
                </form>

                <div className="text-center mt-3">
                    <span className="text-light-weak me-1">¿Ya tienes una cuenta?</span>
                    <Link to="/login" className="link-accent">
                        Inicia sesión
                    </Link>
                </div>
            </div>
        </div>
    );
}
