import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import "../../styles/home.css";

const API = "http://localhost:8080";

/* ===== Helpers Auth / Fetch ===== */
const getToken = () =>
    localStorage.getItem("token") || localStorage.getItem("admin_token") || "";

const authHeaders = () => (getToken() ? { Authorization: `Bearer ${getToken()}` } : {});

async function apiGet(path) {
    const r = await fetch(`${API}${path}`, { headers: { ...authHeaders() } });
    const ct = r.headers.get("content-type") || "";
    if (!r.ok) throw new Error(`GET ${path} -> ${r.status}`);
    return ct.includes("application/json") ? r.json() : r.text();
}

function decodeToken() {
    try {
        const t = getToken();
        if (!t) return {};
        const [, payload] = t.split(".");
        const data = JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
        return {
            username: data.sub || data.username || null,
            role: data.rol || data.role || null,
        };
    } catch {
        return {};
    }
}

/* ====== UI ====== */
export default function MetricsPage() {
    const navigate = useNavigate();

    // Datos de sesión
    const [auth, setAuth] = useState(() => decodeToken());
    const username = auth.username;
    const role = auth.role || "";

    const [open, setOpen] = useState(false);
    const ref = useRef(null);

    // Estados de datos
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [descargasPorDia, setDescargasPorDia] = useState({});
    const [topExportadores, setTopExportadores] = useState([]);
    const [topArtistas, setTopArtistas] = useState([]);
    const [topGeneros, setTopGeneros] = useState([]);

    useEffect(() => {
        const handleStorage = () => setAuth(decodeToken());
        const closeOnOutside = (e) => {
            if (ref.current && !ref.current.contains(e.target)) setOpen(false);
        };
        window.addEventListener("storage", handleStorage);
        document.addEventListener("mousedown", closeOnOutside);
        return () => {
            window.removeEventListener("storage", handleStorage);
            document.removeEventListener("mousedown", closeOnOutside);
        };
    }, []);

    // Cargar métricas
    useEffect(() => {
        const fetchMetrics = async () => {
            try {
                setLoading(true);
                setError("");

                const [dia, exportadores, artistas, generos] = await Promise.all([
                    apiGet("/api/metricas/descargas-favoritos/dia"),
                    apiGet("/api/metricas/usuarios/top-exportadores?limit=10"),
                    apiGet("/api/metricas/favoritos/top-artistas?limit=10"),
                    apiGet("/api/metricas/favoritos/top-generos?limit=10"),
                ]);

                setDescargasPorDia(dia || {});
                setTopExportadores(exportadores || []);
                setTopArtistas(artistas || []);
                setTopGeneros(generos || []);
            } catch (err) {
                setError(`Error al cargar métricas: ${err.message}`);
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        fetchMetrics();
    }, []);

    // Cerrar sesión
    const handleLogout = async () => {
        try {
            await fetch(`${API}/api/usuarios/logout`, {
                method: "POST",
                headers: { ...authHeaders() },
            });
        } catch {}
        localStorage.removeItem("token");
        localStorage.removeItem("admin_token");
        navigate("/login", { replace: true });
    };

    if (loading) {
        return (
            <div className="app-shell">
                <aside className="sidebar">
                    <div className="brand">🎧 SyncUp</div>
                </aside>
                <main className="main">
                    <div style={{ padding: "40px", textAlign: "center", color: "#fff" }}>
                        <h2>📊 Cargando métricas...</h2>
                    </div>
                </main>
            </div>
        );
    }

    return (
        <div className="app-shell">
            {/* Sidebar */}
            <aside className="sidebar">
                <div className="brand">🎧 SyncUp</div>
                <div>
                    <button className="navbtn active">
                        <span style={{ width: 20, textAlign: "center" }}>📊</span>
                        Métricas
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

                    {/* Botón perfil que abre dropdown */}
                    <button
                        className="profile"
                        onClick={() => setOpen((v) => !v)}
                        aria-haspopup="menu"
                        aria-expanded={open}
                        style={{ marginLeft: 8 }}
                    >
                        👤 {username || "Admin"}
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
                                    color: "#fff", // ← CAMBIO: #bbb → #fff
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

                {/* Contenido principal */}
                <section className="section">
                    <h2>📊 Métricas del Sistema</h2>

                    {error && (
                        <div
                            className="alert alert-danger"
                            style={{
                                background: "#3d1414",
                                color: "#ffd7d7",
                                padding: "10px 12px",
                                borderRadius: 8,
                                marginBottom: 20,
                            }}
                        >
                            {error}
                        </div>
                    )}

                    {/* Card: Descargas por día */}
                    <div className="card" style={{ maxWidth: 820, margin: "0 auto 15px" }}>
                        <h3
                            style={{
                                marginTop: 0,
                                marginBottom: 10,
                                textAlign: "center",
                                color: "#ff4d4d",
                            }}
                        >
                            📥 Descargas de Favoritos por Día
                        </h3>

                        {Object.keys(descargasPorDia).length > 0 ? (
                            <div style={{ overflowX: "auto" }}>
                                <table
                                    style={{
                                        width: "100%",
                                        borderCollapse: "collapse",
                                        fontSize: 14,
                                    }}
                                >
                                    <thead>
                                        <tr
                                            style={{
                                                borderBottom: "1px solid rgba(255,255,255,0.2)",
                                            }}
                                        >
                                            <th
                                                style={{
                                                    textAlign: "left",
                                                    padding: "10px 8px",
                                                    color: "#fff", // ← CAMBIO: #bbb → #fff
                                                }}
                                            >
                                                Fecha
                                            </th>
                                            <th
                                                style={{
                                                    textAlign: "right",
                                                    padding: "10px 8px",
                                                    color: "#fff", // ← CAMBIO: #bbb → #fff
                                                }}
                                            >
                                                Total
                                            </th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {Object.entries(descargasPorDia).map(([fecha, total]) => (
                                            <tr
                                                key={fecha}
                                                style={{
                                                    borderBottom: "1px solid rgba(255,255,255,0.08)",
                                                }}
                                            >
                                                <td style={{ padding: "8px", color: "#fff" }}>{fecha}</td>
                                                <td style={{ textAlign: "right", padding: "8px", color: "#fff" }}>
                                                    {total}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : (
                            <p style={{ color: "#aaa", textAlign: "center", margin: 0 }}>
                                Sin datos disponibles
                            </p>
                        )}
                    </div>

                    {/* Card: Top Exportadores */}
                    <div className="card" style={{ maxWidth: 820, margin: "0 auto 15px" }}>
                        <h3
                            style={{
                                marginTop: 0,
                                marginBottom: 10,
                                textAlign: "center",
                                color: "#ff4d4d",
                            }}
                        >
                            🏆 Top Exportadores
                        </h3>

                        {topExportadores.length > 0 ? (
                            <div style={{ overflowX: "auto" }}>
                                <table
                                    style={{
                                        width: "100%",
                                        borderCollapse: "collapse",
                                        fontSize: 14,
                                    }}
                                >
                                    <thead>
                                        <tr
                                            style={{
                                                borderBottom: "1px solid rgba(255,255,255,0.2)",
                                            }}
                                        >
                                            <th
                                                style={{
                                                    textAlign: "left",
                                                    padding: "10px 8px",
                                                    color: "#fff", // ← CAMBIO: #bbb → #fff
                                                }}
                                            >
                                                Usuario
                                            </th>
                                            <th
                                                style={{
                                                    textAlign: "right",
                                                    padding: "10px 8px",
                                                    color: "#fff", // ← CAMBIO: #bbb → #fff
                                                }}
                                            >
                                                Exportaciones
                                            </th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {topExportadores.map((item, idx) => (
                                            <tr
                                                key={idx}
                                                style={{
                                                    borderBottom: "1px solid rgba(255,255,255,0.08)",
                                                }}
                                            >
                                                <td style={{ padding: "8px", color: "#fff" }}>{item.username}</td>
                                                <td style={{ textAlign: "right", padding: "8px", color: "#fff" }}>
                                                    {item.total}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : (
                            <p style={{ color: "#aaa", textAlign: "center", margin: 0 }}>
                                Sin datos disponibles
                            </p>
                        )}
                    </div>

                    {/* Card: Top Artistas */}
                    <div className="card" style={{ maxWidth: 820, margin: "0 auto 15px" }}>
                        <h3
                            style={{
                                marginTop: 0,
                                marginBottom: 10,
                                textAlign: "center",
                                color: "#ff4d4d",
                            }}
                        >
                            🎤 Top Artistas
                        </h3>

                        {topArtistas.length > 0 ? (
                            <div style={{ overflowX: "auto" }}>
                                <table
                                    style={{
                                        width: "100%",
                                        borderCollapse: "collapse",
                                        fontSize: 14,
                                    }}
                                >
                                    <thead>
                                        <tr
                                            style={{
                                                borderBottom: "1px solid rgba(255,255,255,0.2)",
                                            }}
                                        >
                                            <th
                                                style={{
                                                    textAlign: "left",
                                                    padding: "10px 8px",
                                                    color: "#fff", // ← CAMBIO: #bbb → #fff
                                                }}
                                            >
                                                Artista
                                            </th>
                                            <th
                                                style={{
                                                    textAlign: "right",
                                                    padding: "10px 8px",
                                                    color: "#fff", // ← CAMBIO: #bbb → #fff
                                                }}
                                            >
                                                Apariciones
                                            </th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {topArtistas.map((item, idx) => (
                                            <tr
                                                key={idx}
                                                style={{
                                                    borderBottom: "1px solid rgba(255,255,255,0.08)",
                                                }}
                                            >
                                                <td style={{ padding: "8px", color: "#fff" }}>{item.artista}</td>
                                                <td style={{ textAlign: "right", padding: "8px", color: "#fff" }}>
                                                    {item.total}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : (
                            <p style={{ color: "#aaa", textAlign: "center", margin: 0 }}>
                                Sin datos disponibles
                            </p>
                        )}
                    </div>

                    {/* Card: Top Géneros */}
                    <div className="card" style={{ maxWidth: 820, margin: "0 auto 15px" }}>
                        <h3
                            style={{
                                marginTop: 0,
                                marginBottom: 10,
                                textAlign: "center",
                                color: "#ff4d4d",
                            }}
                        >
                            🎧 Top Géneros
                        </h3>

                        {topGeneros.length > 0 ? (
                            <div style={{ overflowX: "auto" }}>
                                <table
                                    style={{
                                        width: "100%",
                                        borderCollapse: "collapse",
                                        fontSize: 14,
                                    }}
                                >
                                    <thead>
                                        <tr
                                            style={{
                                                borderBottom: "1px solid rgba(255,255,255,0.2)",
                                            }}
                                        >
                                            <th
                                                style={{
                                                    textAlign: "left",
                                                    padding: "10px 8px",
                                                    color: "#fff", // ← CAMBIO: #bbb → #fff
                                                }}
                                            >
                                                Género
                                            </th>
                                            <th
                                                style={{
                                                    textAlign: "right",
                                                    padding: "10px 8px",
                                                    color: "#fff", // ← CAMBIO: #bbb → #fff
                                                }}
                                            >
                                                Apariciones
                                            </th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {topGeneros.map((item, idx) => (
                                            <tr
                                                key={idx}
                                                style={{
                                                    borderBottom: "1px solid rgba(255,255,255,0.08)",
                                                }}
                                            >
                                                <td style={{ padding: "8px", color: "#fff" }}>{item.genero}</td>
                                                <td style={{ textAlign: "right", padding: "8px", color: "#fff" }}>
                                                    {item.total}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : (
                            <p style={{ color: "#aaa", textAlign: "center", margin: 0 }}>
                                Sin datos disponibles
                            </p>
                        )}
                    </div>
                </section>
            </main>
        </div>
    );
}