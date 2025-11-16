import React, { useState, useEffect, useCallback, useRef } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/home.css";

const API_URL = "http://localhost:8080/api/usuarios";

function getActiveToken() {
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

function getUsernameFromToken() {
    const t = getActiveToken();
    if (!t) return "";
    const data = parseJwt(t);
    return data?.sub || data?.username || "";
}

const Social = () => {
    const [sugerencias, setSugerencias] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [limite, setLimite] = useState(5);
    const [seguido, setSeguido] = useState(new Set());
    const [mensaje, setMensaje] = useState("");
    const [open, setOpen] = useState(false);

    const token = getActiveToken();
    const username = getUsernameFromToken();
    const role = parseJwt(token)?.rol || "";
    const navigate = useNavigate();

    const ref = useRef(null);

    const obtenerSugerencias = useCallback(async () => {
        if (!token || !username) {
            setError("🚫 No estás autenticado o falta el username en el token.");
            setSugerencias([]);
            return;
        }

        setLoading(true);
        setError("");
        setMensaje("");

        try {
            const url = `${API_URL}/${encodeURIComponent(username)}/sugerir-usuarios?limite=${encodeURIComponent(limite)}`;
            const resp = await fetch(url, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            });

            if (resp.status === 204) {
                setSugerencias([]);
                return;
            }

            if (!resp.ok) {
                setError("❌ No se pudieron obtener sugerencias.");
                setSugerencias([]);
                return;
            }

            const data = await resp.json();
            setSugerencias(Array.isArray(data) ? data : []);
        } catch (e) {
            setError("❌ Error en la solicitud.");
            setSugerencias([]);
        } finally {
            setLoading(false);
        }
    }, [token, username, limite]);

    const obtenerSeguidos = useCallback(async () => {
        if (!token || !username) return;

        try {
            const resp = await fetch(`${API_URL}/${encodeURIComponent(username)}/seguidos`, {
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            });

            if (resp.ok) {
                const seguidos = await resp.json();
                setSeguido(new Set(seguidos));
            }
        } catch (e) {
            setError("❌ Error al obtener usuarios seguidos.");
        }
    }, [token, username]);

    useEffect(() => {
        obtenerSugerencias();
        obtenerSeguidos();
    }, [obtenerSugerencias, obtenerSeguidos]);

    const toggleSeguir = async (usuario) => {
        if (!token || !username) return;

        try {
            const resp = await fetch(`${API_URL}/seguir`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify({
                    username,
                    destino: usuario,
                }),
            });

            if (resp.ok) {
                const newSeguidos = new Set(seguido);
                newSeguidos.add(usuario);
                setSeguido(newSeguidos);
                setMensaje(`✅ Ahora sigues a ${usuario}`);
                setTimeout(() => setMensaje(""), 3000);
            } else {
                setError("❌ No se pudo realizar la acción.");
            }
        } catch (e) {
            setError("❌ Error en la solicitud.");
        }
    };

    const toggleDejarDeSeguir = async (usuario) => {
        if (!token || !username) return;

        try {
            const resp = await fetch(`${API_URL}/dejar-seguir`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify({
                    username,
                    destino: usuario,
                }),
            });

            if (resp.ok) {
                const newSeguidos = new Set(seguido);
                newSeguidos.delete(usuario);
                setSeguido(newSeguidos);
                setMensaje(`🗑️ Has dejado de seguir a ${usuario}`);
                setTimeout(() => setMensaje(""), 3000);
            } else {
                setError("❌ No se pudo realizar la acción.");
            }
        } catch (e) {
            setError("❌ Error en la solicitud.");
        }
    };

    const handleLogout = async () => {
        try {
            await fetch(`${API_URL}/logout`, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            });
        } catch (e) {
            console.error("Error al cerrar sesión:", e);
        }
        localStorage.removeItem("token");
        localStorage.removeItem("admin_token");
        navigate("/login", { replace: true });
    };

    useEffect(() => {
        const handler = (e) => {
            if (ref.current && !ref.current.contains(e.target)) {
                setOpen(false);
            }
        };
        document.addEventListener("mousedown", handler);
        return () => document.removeEventListener("mousedown", handler);
    }, []);

    return (
        <div className="app-shell">
            {/* Sidebar */}
            <aside className="sidebar">
                <div className="brand">🎧 SyncUp</div>
                <div>
                    <button className="navbtn active">
                        <span style={{ width: 20, textAlign: "center" }}>👥</span>
                        Social
                    </button>
                </div>
            </aside>

            <main className="main">
                {/* Topbar */}
                <div className="topbar" ref={ref} style={{ position: "relative" }}>
                    <div className="pill">‹</div>
                    <div className="pill">›</div>

                    {/* Volver a Home */}
                    <button className="profile" onClick={() => navigate("/home")}>
                        ← Volver
                    </button>

                    {/* Botón perfil */}
                    <button
                        className="profile"
                        onClick={() => setOpen((v) => !v)}
                        aria-haspopup="menu"
                        aria-expanded={open}
                        style={{ marginLeft: 8 }}
                    >
                        👤 {username || "Social"}
                    </button>

                    {open && (
                        <div
                            role="menu"
                            className="dropdown"
                            style={{
                                position: "absolute",
                                right: 0,
                                top: "calc(100% + 8px)",
                                background: "rgba(255,255,255,0.06)",
                                border: "1px solid rgba(255,255,255,0.1)",
                                backdropFilter: "blur(6px)",
                                borderRadius: 8,
                                padding: 8,
                                minWidth: 220,
                                boxShadow: "0 6px 24px rgba(0,0,0,0.25)",
                                zIndex: 10,
                            }}
                        >
                            <div
                                style={{
                                    padding: "8px 10px",
                                    color: "#bbb",
                                    fontSize: 13,
                                    borderBottom: "1px solid rgba(255,255,255,0.08)",
                                    marginBottom: 6,
                                    display: "flex",
                                    justifyContent: "space-between",
                                    gap: 8,
                                }}
                            >
                                <span>
                                    Sesión: <b>{username || "—"}</b>
                                </span>
                                {role && (
                                    <span
                                        style={{
                                            background: "rgba(255,255,255,0.08)",
                                            padding: "2px 8px",
                                            borderRadius: 999,
                                            fontSize: 11,
                                            color: "#ddd",
                                        }}
                                    >
                                        {role}
                                    </span>
                                )}
                            </div>

                            <button
                                className="btn btn-sm btn-outline-light"
                                style={{ width: "100%" }}
                                onClick={() => {
                                    setOpen(false);
                                    handleLogout();
                                }}
                            >
                                🚪 Cerrar sesión
                            </button>
                        </div>
                    )}
                </div>

                {/* Contenido */}
                <section className="section">
                    <h2>Social</h2>

                    {error && (
                        <div
                            className="alert alert-danger"
                            style={{
                                background: "#3d1414",
                                color: "#ffd7d7",
                                padding: "8px 10px",
                                borderRadius: 8,
                                marginBottom: 20,
                                textAlign: "center",
                            }}
                        >
                            {error}
                        </div>
                    )}

                    {mensaje && (
                        <div
                            className="alert alert-success"
                            style={{
                                background: "#143d2b",
                                color: "#b7ffd7",
                                padding: "8px 10px",
                                borderRadius: 8,
                                marginBottom: 20,
                                textAlign: "center",
                            }}
                        >
                            {mensaje}
                        </div>
                    )}

                    {/* Card: Obtener sugerencias */}
                    <div className="card" style={{ maxWidth: 820, margin: "0 auto 15px", height: "auto" }}>
                        <h3
                            style={{
                                marginTop: 0,
                                marginBottom: 10,
                                textAlign: "center",
                                color: "#ff4d4d",
                            }}
                        >
                            Obtener Sugerencias
                        </h3>

                        <form
                            onSubmit={(e) => {
                                e.preventDefault();
                                obtenerSugerencias();
                            }}
                            style={{
                                display: "grid",
                                gridTemplateColumns: "1fr auto",
                                gap: 12,
                                alignItems: "end",
                            }}
                        >
                            <div>
                                <label className="card-muted" style={{ display: "block", marginBottom: 6 }}>
                                    Cantidad de sugerencias
                                </label>
                                <input
                                    type="number"
                                    className="select"
                                    value={limite}
                                    onChange={(e) => setLimite(Math.max(1, Number(e.target.value) || 1))}
                                    min="1"
                                    max="50"
                                />
                            </div>
                            <div>
                                <button className="btn btn-sm btn-outline-light" type="submit" disabled={loading}>
                                    {loading ? "Cargando..." : "Obtener"}
                                </button>
                            </div>
                        </form>
                    </div>

                    {/* Card: Usuarios sugeridos */}
                    {sugerencias.length > 0 && (
                        <div className="card" style={{ maxWidth: 820, margin: "0 auto 15px", height: "auto" }}>
                            <h3
                                style={{
                                    marginTop: 0,
                                    marginBottom: 10,
                                    textAlign: "center",
                                    color: "#ff4d4d",
                                }}
                            >
                                👥 Usuarios Sugeridos ({sugerencias.length})
                            </h3>

                            <div style={{ display: "grid", gap: 12 }}>
                                {sugerencias.map((usuario, idx) => (
                                    <div
                                        key={idx}
                                        style={{
                                            display: "flex",
                                            justifyContent: "space-between",
                                            alignItems: "center",
                                            background: "rgba(255,255,255,0.03)",
                                            border: "1px solid rgba(255,255,255,0.08)",
                                            borderRadius: 8,
                                            padding: "10px 12px",
                                        }}
                                    >
                                        <div style={{ fontWeight: 600, color: "#fff" }}>{usuario}</div>
                                        <button
                                            className="btn btn-sm btn-outline-light"
                                            onClick={() =>
                                                seguido.has(usuario)
                                                    ? toggleDejarDeSeguir(usuario)
                                                    : toggleSeguir(usuario)
                                            }
                                            style={{
                                                background: seguido.has(usuario)
                                                    ? "rgba(255,255,255,0.1)"
                                                    : undefined,
                                            }}
                                        >
                                            {seguido.has(usuario) ? "✓ Siguiendo" : "+ Seguir"}
                                        </button>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* Card: Usuarios seguidos */}
                    <div className="card" style={{ maxWidth: 820, margin: "0 auto", height: "auto" }}>
                        <h3
                            style={{
                                marginTop: 0,
                                marginBottom: 10,
                                textAlign: "center",
                                color: "#ff4d4d",
                            }}
                        >
                            ⭐ Siguiendo ({seguido.size})
                        </h3>

                        {seguido.size > 0 ? (
                            <div style={{ display: "grid", gap: 8 }}>
                                {Array.from(seguido).map((usuario, idx) => (
                                    <div
                                        key={idx}
                                        style={{
                                            display: "flex",
                                            justifyContent: "space-between",
                                            alignItems: "center",
                                            background: "rgba(255,255,255,0.03)",
                                            border: "1px solid rgba(255,255,255,0.08)",
                                            borderRadius: 8,
                                            padding: "10px 12px",
                                        }}
                                    >
                                        <div style={{ fontWeight: 600, color: "#fff" }}>{usuario}</div>
                                        <button
                                            className="btn btn-sm btn-outline-light"
                                            onClick={() => toggleDejarDeSeguir(usuario)}
                                        >
                                            Dejar de seguir
                                        </button>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <p className="card-muted" style={{ textAlign: "center", margin: 0 }}>
                                No estás siguiendo a nadie aún.
                            </p>
                        )}
                    </div>
                </section>
            </main>
        </div>
    );
};

export default Social;
