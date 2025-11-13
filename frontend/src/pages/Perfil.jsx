// src/pages/Perfil.jsx
import React, { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/home.css";

const API = "http://localhost:8080";

/* ===== Helpers Auth / Fetch ===== */
const getToken = () =>
    localStorage.getItem("token") || localStorage.getItem("admin_token") || "";

const authHeaders = () => (getToken() ? { Authorization: `Bearer ${getToken()}` } : {});

async function apiPost(path) {
    const r = await fetch(`${API}${path}`, { method: "POST", headers: { ...authHeaders() } });
    return r.ok;
}
async function apiPut(path, body) {
    const r = await fetch(`${API}${path}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json", ...authHeaders() },
        body: body ? JSON.stringify(body) : undefined,
    });
    const ct = r.headers.get("content-type") || "";
    if (!r.ok) throw new Error(`PUT ${path} -> ${r.status}`);
    return ct.includes("application/json") ? r.json() : r.text();
}
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
export default function Perfil() {
    const navigate = useNavigate();

    // Datos de sesión para el botón Perfil (igual estilo que Home)
    const [auth, setAuth] = useState(() => decodeToken());
    const username = auth.username;
    const role = auth.role || "";

    const [open, setOpen] = useState(false);
    const ref = useRef(null);

    // Datos visibles del usuario (nombre mostrado en el backend)
    const [nombreActual, setNombreActual] = useState("");

    // ===== Estados para formularios =====
    // Actualizar nombre
    const [newName, setNewName] = useState("");
    const [nameMsg, setNameMsg] = useState("");
    const [nameErr, setNameErr] = useState("");
    const [nameLoading, setNameLoading] = useState(false);

    // Cambiar contraseña (mínimo 3)
    const [pwd, setPwd] = useState({ nueva: "", confirmar: "" });
    const [pwdMsg, setPwdMsg] = useState("");
    const [pwdErr, setPwdErr] = useState("");
    const [pwdLoading, setPwdLoading] = useState(false);

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

    // Prefill de nombre desde /api/usuarios/sesion
    useEffect(() => {
        let alive = true;
        apiGet("/api/usuarios/sesion")
            .then((u) => {
                if (!alive) return;
                const n = (u && (u.nombre || u.name)) || "";
                setNombreActual(n);
            })
            .catch(() => {})
            .finally(() => {});
        return () => {
            alive = false;
        };
    }, []);

    // Cerrar sesión (mismo flujo que en Home)
    const handleLogout = async () => {
        try {
            await apiPost("/api/usuarios/logout");
        } catch {}
        localStorage.removeItem("token");
        localStorage.removeItem("admin_token");
        navigate("/login", { replace: true });
    };

    // ===== Handlers =====
    const submitUpdateName = async (e) => {
        e.preventDefault();
        setNameMsg("");
        setNameErr("");

        const valor = newName.trim();
        if (!valor) {
            setNameErr("Debes escribir un nombre.");
            return;
        }
        if (!username) {
            setNameErr("No se pudo identificar al usuario actual.");
            return;
        }

        setNameLoading(true);
        try {
            const path = `/api/usuarios/${encodeURIComponent(
                username
            )}/actualizar-nombre?nuevoNombre=${encodeURIComponent(valor)}`;
            const resp = await apiPut(path);
            const msg =
                typeof resp === "string"
                    ? resp
                    : resp?.mensaje || "✅ Nombre actualizado correctamente.";
            setNameMsg(msg);
            setNombreActual(valor);
            setNewName("");
        } catch (e2) {
            setNameErr("No se pudo actualizar el nombre.");
        } finally {
            setNameLoading(false);
        }
    };

    const submitChangePassword = async (e) => {
        e.preventDefault();
        setPwdMsg("");
        setPwdErr("");

        const a = String(pwd.nueva || "").trim();
        const b = String(pwd.confirmar || "").trim();
        if (!a || !b) {
            setPwdErr("Debes completar ambos campos.");
            return;
        }
        if (a.length < 3) {
            setPwdErr("La contraseña debe tener al menos 3 caracteres.");
            return;
        }
        if (a !== b) {
            setPwdErr("Las contraseñas no coinciden.");
            return;
        }
        if (!username) {
            setPwdErr("No se pudo identificar al usuario actual.");
            return;
        }

        setPwdLoading(true);
        try {
            const path = `/api/usuarios/${encodeURIComponent(
                username
            )}/cambiar-password?nuevaPassword=${encodeURIComponent(a)}`;
            const resp = await apiPut(path);
            const msg =
                typeof resp === "string"
                    ? resp
                    : resp?.mensaje || "✅ Contraseña actualizada correctamente.";
            setPwdMsg(msg);
            setPwd({ nueva: "", confirmar: "" });
        } catch (e2) {
            setPwdErr("No se pudo cambiar la contraseña.");
        } finally {
            setPwdLoading(false);
        }
    };

    return (
        <div className="app-shell">
            {/* Sidebar con el logo; SIN el botón Home */}
            <aside className="sidebar">
                <div className="brand">🎧 SyncUp</div>
                <div>
                    <button className="navbtn active">
                        <span style={{ width: 20, textAlign: "center" }}>👤</span>
                        Perfil
                    </button>
                </div>
            </aside>

            <main className="main">
                {/* Topbar con botón Volver y dropdown de perfil */}
                <div className="topbar" ref={ref} style={{ position: "relative" }}>
                    <div className="pill">‹</div>
                    <div className="pill">›</div>

                    {/* Volver a Home */}
                    <button className="profile" onClick={() => navigate("/home")}>
                        ← Volver
                    </button>

                    {/* Botón perfil que abre dropdown con Cerrar sesión */}
                    <button
                        className="profile"
                        onClick={() => setOpen((v) => !v)}
                        aria-haspopup="menu"
                        aria-expanded={open}
                        style={{ marginLeft: 8 }}
                    >
                        👤 {username || "Perfil"}
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

                {/* Contenido principal del perfil */}
                <section className="section">
                    <h2>Perfil</h2>

                    {/* ===== Resumen de usuario (fondos compactos por campo) ===== */}
                    <div style={{ maxWidth: 820, margin: "0 auto 16px", display: "grid", gap: 12, gridTemplateColumns: "1fr 1fr auto" }}>
                        {/* Usuario */}
                        <div
                            style={{
                                background: "rgba(255,255,255,0.06)",
                                border: "1px solid rgba(255,255,255,0.08)",
                                borderRadius: 10,
                                padding: "10px 12px",
                                minWidth: 0,
                            }}
                        >
                            <div className="card-muted" style={{ marginBottom: 4 }}>
                                Usuario
                            </div>
                            <div style={{ fontWeight: 700, color: "#fff", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                                {username || "—"}
                            </div>
                        </div>

                        {/* Nombre */}
                        <div
                            style={{
                                background: "rgba(255,255,255,0.06)",
                                border: "1px solid rgba(255,255,255,0.08)",
                                borderRadius: 10,
                                padding: "10px 12px",
                                minWidth: 0,
                            }}
                        >
                            <div className="card-muted" style={{ marginBottom: 4 }}>
                                Nombre
                            </div>
                            <div style={{ fontWeight: 700, color: "#fff", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                                {nombreActual || "—"}
                            </div>
                        </div>

                        {/* Rol */}
                        <div
                            style={{
                                background: "rgba(255,255,255,0.06)",
                                border: "1px solid rgba(255,255,255,0.08)",
                                borderRadius: 10,
                                padding: "10px 12px",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "space-between",
                                gap: 10,
                            }}
                        >
                            <div className="card-muted">Rol</div>
                            <span
                                style={{
                                    background: "rgba(255,255,255,0.08)",
                                    padding: "2px 8px",
                                    borderRadius: 999,
                                    fontSize: 12,
                                    color: "#ddd",
                                    whiteSpace: "nowrap",
                                }}
                            >
                {(role || "USER").toUpperCase()}
              </span>
                        </div>
                    </div>

                    {/* Card: Actualizar nombre */}
                    <div className="card" style={{ maxWidth: 820, margin: "0 auto 15px", maxHeight: 200 }}>
                        <h3
                            style={{
                                marginTop: 0,
                                marginBottom: 10,
                                textAlign: "center",
                                color: "#ff4d4d",
                            }}
                        >
                            Actualizar nombre
                        </h3>

                        {nameMsg && (
                            <div
                                className="alert alert-success"
                                style={{
                                    background: "#143d2b",
                                    color: "#b7ffd7",
                                    padding: "8px 10px",
                                    borderRadius: 8,
                                    marginBottom: 10,
                                    textAlign: "center",
                                }}
                            >
                                {nameMsg}
                            </div>
                        )}
                        {nameErr && (
                            <div
                                className="alert alert-danger"
                                style={{
                                    background: "#3d1414",
                                    color: "#ffd7d7",
                                    padding: "8px 10px",
                                    borderRadius: 8,
                                    marginBottom: 10,
                                    textAlign: "center",
                                }}
                            >
                                {nameErr}
                            </div>
                        )}

                        <form
                            onSubmit={submitUpdateName}
                            style={{
                                display: "grid",
                                gridTemplateColumns: "1fr auto",
                                gap: 12,
                                alignItems: "end",
                            }}
                        >
                            <div>
                                <label className="card-muted" style={{ display: "block", marginBottom: 6 }}>
                                    Nuevo nombre
                                </label>
                                <input
                                    className="select"
                                    value={newName}
                                    onChange={(e) => setNewName(e.target.value)}
                                />
                            </div>
                            <div>
                                <button className="btn btn-sm btn-outline-light" type="submit" disabled={nameLoading}>
                                    {nameLoading ? "Guardando..." : "Guardar"}
                                </button>
                            </div>
                        </form>

                        <div className="card-muted" style={{ marginTop: 8, fontSize: 12 }}>
                            * Este cambio solo actualiza tu nombre visible. Tu usuario no cambia.
                        </div>
                    </div>

                    {/* Card: Cambiar contraseña (mínimo 3 caracteres) */}
                    <div className="card" style={{ maxWidth: 820, margin: "0 auto", maxHeight: 240 }}>
                        <h3
                            style={{
                                marginTop: 0,
                                marginBottom: 10,
                                textAlign: "center",
                                color: "#ff4d4d",
                            }}
                        >
                            Cambiar contraseña
                        </h3>

                        {pwdMsg && (
                            <div
                                className="alert alert-success"
                                style={{
                                    background: "#143d2b",
                                    color: "#b7ffd7",
                                    padding: "8px 10px",
                                    borderRadius: 8,
                                    marginBottom: 10,
                                    textAlign: "center",
                                }}
                            >
                                {pwdMsg}
                            </div>
                        )}
                        {pwdErr && (
                            <div
                                className="alert alert-danger"
                                style={{
                                    background: "#3d1414",
                                    color: "#ffd7d7",
                                    padding: "8px 10px",
                                    borderRadius: 8,
                                    marginBottom: 10,
                                    textAlign: "center",
                                }}
                            >
                                {pwdErr}
                            </div>
                        )}

                        <form
                            onSubmit={submitChangePassword}
                            style={{
                                display: "grid",
                                gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
                                gap: 12,
                                alignItems: "end",
                            }}
                        >
                            <div>
                                <label className="card-muted" style={{ display: "block", marginBottom: 6 }}>
                                    Nueva contraseña
                                </label>
                                <input
                                    type="password"
                                    className="select"
                                    value={pwd.nueva}
                                    onChange={(e) => setPwd((p) => ({ ...p, nueva: e.target.value }))}
                                    placeholder="Mínimo 3 caracteres"
                                />
                            </div>
                            <div>
                                <label className="card-muted" style={{ display: "block", marginBottom: 6 }}>
                                    Confirmar contraseña
                                </label>
                                <input
                                    type="password"
                                    className="select"
                                    value={pwd.confirmar}
                                    onChange={(e) => setPwd((p) => ({ ...p, confirmar: e.target.value }))}
                                    placeholder="Repite tu contraseña"
                                />
                            </div>
                            <div style={{ gridColumn: "1 / -1", display: "flex", justifyContent: "center" }}>
                                <button className="btn btn-sm btn-outline-light" type="submit" disabled={pwdLoading}>
                                    {pwdLoading ? "Guardando..." : "Guardar"}
                                </button>
                            </div>
                        </form>

                        <div className="card-muted" style={{ marginTop: 8, fontSize: 12 }}>
                            * Se actualizará inmediatamente. Vuelve a iniciar sesión si tu sesión expira.
                        </div>
                    </div>
                </section>
            </main>
        </div>
    );
}
