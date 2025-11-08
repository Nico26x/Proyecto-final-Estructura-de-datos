import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { registerUser } from "../api/auth";

export default function Register() {
    const navigate = useNavigate();
    const [form, setForm] = useState({ username: "", password: "", nombre: "" });
    const [loading, setLoading] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");

    const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

    const onSubmit = async (e) => {
        e.preventDefault();
        setErrorMsg("");
        setLoading(true);
        try {
            // Llama al API
            const result = await registerUser(form);

            // Compatibilidad: si el API devuelve string (modo antiguo) o { ok, message } (modo nuevo)
            let ok = false;
            let message = "";

            if (typeof result === "object" && result !== null && "ok" in result) {
                ok = !!result.ok;
                message = result.message || "";
            } else if (typeof result === "string") {
                message = result;
                // Si contiene señales de fallo conocidas, lo tratamos como error
                const esDuplicado =
                    result.includes("El usuario ya existe") || result.includes("⚠️");
                ok = !esDuplicado;
            } else {
                // Cualquier forma desconocida la tratamos como error
                ok = false;
            }

            // Nueva lógica solicitada:
            if (!ok) {
                setErrorMsg(message || "El usuario ya existe");
                return; // NO navegar
            }

            // Si todo bien, navegar a login
            navigate("/login");
        } catch (err) {
            setErrorMsg("Error al registrar. Intenta de nuevo.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container py-4" style={{ maxWidth: 480 }}>
            <h1 className="h4 mb-3">Crear cuenta</h1>

            {errorMsg && (
                <div className="alert alert-danger" role="alert">
                    {errorMsg}
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
                        required
                        autoComplete="username"
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
                        autoComplete="name"
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
                        required
                        autoComplete="new-password"
                    />
                </div>

                <button className="btn btn-primary w-100" disabled={loading}>
                    {loading ? "Creando..." : "Registrarme"}
                </button>
            </form>
        </div>
    );
}
