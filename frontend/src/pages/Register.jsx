import { useState } from "react";
import { registerUser } from "../api/auth";
import { useNavigate, Link } from "react-router-dom";

export default function Register() {
    const [form, setForm] = useState({ username: "", password: "", nombre: "" });
    const [loading, setLoading] = useState(false);
    const [msg, setMsg] = useState(null);
    const navigate = useNavigate();

    const onChange = (e) => {
        setForm({ ...form, [e.target.name]: e.target.value });
    };

    const onSubmit = async (e) => {
        e.preventDefault();
        setMsg(null);
        setLoading(true);
        try {
            const { data } = await registerUser(form);
            // El backend devuelve texto tipo: "✅ Usuario registrado correctamente" o "⚠️ El usuario ya existe"
            setMsg({ type: "success", text: data });
            // si quieres, navega al login tras 1.2s
            setTimeout(() => navigate("/login"), 1200);
        } catch (err) {
            const text =
                err?.response?.data ??
                "No se pudo registrar. Revisa el servidor.";
            setMsg({ type: "danger", text: String(text) });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container py-4" style={{ maxWidth: 520 }}>
            <h1 className="display-6 mb-3">Crear cuenta</h1>

            {msg && (
                <div className={`alert alert-${msg.type}`} role="alert">
                    {msg.text}
                </div>
            )}

            <form onSubmit={onSubmit}>
                <div className="mb-3">
                    <label className="form-label">Usuario</label>
                    <input
                        className="form-control"
                        name="username"
                        value={form.username}
                        onChange={onChange}
                        autoComplete="username"
                        required
                    />
                </div>

                <div className="mb-3">
                    <label className="form-label">Nombre</label>
                    <input
                        className="form-control"
                        name="nombre"
                        value={form.nombre}
                        onChange={onChange}
                        required
                    />
                </div>

                <div className="mb-3">
                    <label className="form-label">Contraseña</label>
                    <input
                        type="password"
                        className="form-control"
                        name="password"
                        value={form.password}
                        onChange={onChange}
                        autoComplete="new-password"
                        required
                    />
                </div>

                <button className="btn btn-primary w-100" disabled={loading}>
                    {loading ? "Registrando..." : "Crear cuenta"}
                </button>
            </form>

            <div className="text-center mt-3">
                <span className="text-muted me-1">¿Ya tienes cuenta?</span>
                <Link to="/login">Inicia sesión</Link>
            </div>
        </div>
    );
}
