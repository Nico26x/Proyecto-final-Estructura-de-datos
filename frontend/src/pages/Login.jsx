// src/pages/Login.jsx
import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

const API_URL = process.env.REACT_APP_API_URL || "http://localhost:8080";

export default function Login() {
    const navigate = useNavigate();
    const [form, setForm] = useState({ username: "", password: "" });
    const [loading, setLoading] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");

    const handleChange = (e) => {
        setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setErrorMsg("");
        setLoading(true);

        try {
            const url = `${API_URL}/api/usuarios/login?username=${encodeURIComponent(
                form.username
            )}&password=${encodeURIComponent(form.password)}`;

            const res = await fetch(url, {
                method: "POST",
                headers: { "Content-Type": "application/json", Accept: "application/json" },
            });

            if (!res.ok) {
                const body = await res.json().catch(() => ({}));
                const msg =
                    body?.error ||
                    body?.mensaje ||
                    `Error de autenticación (HTTP ${res.status})`;
                throw new Error(msg);
            }

            const data = await res.json();
            // guarda token y (opcional) usuario
            if (data?.token) localStorage.setItem("token", data.token);
            if (data?.usuario) localStorage.setItem("usuario", JSON.stringify(data.usuario));

            navigate("/"); // redirige a Home (ajusta la ruta si quieres)
        } catch (err) {
            setErrorMsg(err.message || "No se pudo iniciar sesión");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container py-5">
            <div className="row justify-content-center">
                <div className="col-12 col-sm-10 col-md-6 col-lg-5">
                    <h1 className="display-6 mb-4">Iniciar sesión</h1>

                    <form onSubmit={handleSubmit} noValidate>
                        <div className="mb-3">
                            <label className="form-label">Usuario</label>
                            <input
                                type="text"
                                name="username"
                                className="form-control"
                                value={form.username}
                                onChange={handleChange}
                                autoComplete="username"
                                required
                            />
                        </div>

                        <div className="mb-3">
                            <label className="form-label">Contraseña</label>
                            <input
                                type="password"
                                name="password"
                                className="form-control"
                                value={form.password}
                                onChange={handleChange}
                                autoComplete="current-password"
                                required
                            />
                        </div>

                        {errorMsg && (
                            <div className="alert alert-danger py-2" role="alert">
                                {errorMsg}
                            </div>
                        )}

                        <button
                            type="submit"
                            className="btn btn-primary w-100"
                            disabled={loading}
                        >
                            {loading ? "Entrando..." : "Entrar"}
                        </button>
                    </form>

                    {/* ⬇️ Enlace a registro agregado */}
                    <div className="text-center mt-3">
                        <span className="text-muted me-1">¿No tienes cuenta?</span>
                        <Link to="/register">Regístrate</Link>
                    </div>
                </div>
            </div>
        </div>
    );
}
