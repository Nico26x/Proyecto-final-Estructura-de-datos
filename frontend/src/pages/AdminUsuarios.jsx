// src/pages/AdminUsuarios.jsx
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/home.css";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";

const API = "http://localhost:8080";

// ===== Utils JWT / Auth =====
function getToken() {
    return localStorage.getItem("admin_token") || localStorage.getItem("token") || "";
}
function bearer() {
    const t = getToken();
    return t ? { Authorization: `Bearer ${t}` } : {};
}
function decodeRole() {
    try {
        const t = getToken();
        if (!t) return "";
        const [, payload] = t.split(".");
        const data = JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
        return (data.rol || data.role || "").toUpperCase();
    } catch {
        return "";
    }
}
function decodeUsername() {
    try {
        const t = getToken();
        if (!t) return "";
        const [, payload] = t.split(".");
        const data = JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
        return data.sub || data.username || "";
    } catch {
        return "";
    }
}

// ===== Minimal API helpers =====
async function apiGet(path) {
    const r = await fetch(`${API}${path}`, { headers: { ...bearer() } });
    const ct = r.headers.get("content-type") || "";
    if (!r.ok) throw new Error(`${r.status}`);
    return ct.includes("application/json") ? r.json() : r.text();
}
async function apiDelete(path, { timeoutMs = 15000 } = {}) {
    const ctrl = new AbortController();
    const id = setTimeout(() => ctrl.abort("timeout"), timeoutMs);

    try {
        const r = await fetch(`${API}${path}`, {
            method: "DELETE",
            headers: { ...bearer() },
            signal: ctrl.signal,
        });
        const ct = r.headers.get("content-type") || "";
        const body = ct.includes("application/json") ? await r.json() : await r.text();

        if (!r.ok) {
            const msg = typeof body === "string" ? body : (body?.message || body?.error || `HTTP ${r.status}`);
            throw new Error(msg);
        }
        return body;
    } finally {
        clearTimeout(id);
    }
}

export default function AdminUsuarios() {
    const navigate = useNavigate();
    const role = decodeRole();
    const isAdmin = role.includes("ADMIN");
    const me = decodeUsername();

    // ===== Guard =====
    useEffect(() => {
        if (!isAdmin) navigate("/home", { replace: true });
    }, [isAdmin, navigate]);

    // ===== State =====
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [err, setErr] = useState("");
    const [info, setInfo] = useState("");

    const [filter, setFilter] = useState("");
    const [busyUser, setBusyUser] = useState(""); // username en eliminación

    // ===== Data Load =====
    const loadUsers = async () => {
        setLoading(true);
        setErr("");
        setInfo("");
        try {
            // GET /api/usuarios/listar (requiere Authorization Bearer y rol ADMIN)
            const list = await apiGet("/api/usuarios/listar");
            const arr = Array.isArray(list)
                ? list
                : Array.isArray(list?.data)
                    ? list.data
                    : [];
            setUsers(arr);
        } catch (e) {
            setErr("No se pudieron cargar los usuarios. Verifica el token ADMIN y el backend.");
            setUsers([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (isAdmin) loadUsers();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isAdmin]);

    const filtered = useMemo(() => {
        const q = filter.trim().toLowerCase();
        if (!q) return users;
        return users.filter((u) => {
            const username = (u.username || u.user || "").toLowerCase();
            const nombre = (u.nombre || u.name || "").toLowerCase();
            const rol = (String(u.rol || u.role || "")).toLowerCase();
            return username.includes(q) || nombre.includes(q) || rol.includes(q);
        });
    }, [users, filter]);

    // ===== Delete handler con SweetAlert2 =====
    const onDelete = async (username) => {
        if (!username) return;
        if (username === me) {
            await Swal.fire({
                icon: "info",
                title: "Acción no permitida",
                text: "No puedes eliminar tu propio usuario mientras estás logueado como ADMIN.",
                confirmButtonText: "Entendido",
                background: "rgba(20,20,20,0.98)",
                color: "#fff",
                confirmButtonColor: "#ff4d4d",
            });
            return;
        }

        let confirmed = false;
        try {
            const res = await Swal.fire({
                title: `¿Eliminar a "${username}"?`,
                text: "Esta acción no se puede deshacer.",
                icon: "warning",
                showCancelButton: true,
                confirmButtonText: "Sí, eliminar",
                cancelButtonText: "Cancelar",
                reverseButtons: true,
                background: "rgba(20,20,20,0.98)",
                color: "#fff",
                confirmButtonColor: "#ff4d4d",
                cancelButtonColor: "#666",
            });
            confirmed = res.isConfirmed;
        } catch {
            confirmed = window.confirm(`¿Eliminar al usuario "${username}"?`);
        }
        if (!confirmed) return;

        setBusyUser(username);
        setErr("");
        setInfo("");

        // Mostrar loader SIN await; cerrar en finally
        Swal.fire({
            title: "Eliminando…",
            html: "Procesando la solicitud",
            didOpen: () => Swal.showLoading(),
            allowOutsideClick: false,
            allowEscapeKey: false,
            background: "rgba(20,20,20,0.98)",
            color: "#fff",
        });

        try {
            const resp = await apiDelete(`/api/usuarios/eliminar?username=${encodeURIComponent(username)}`);
            const msg = typeof resp === "string" ? resp : (resp?.mensaje || "Operación realizada.");
            setInfo(msg);

            Swal.close();
            await Swal.fire({
                icon: "success",
                title: "Usuario eliminado",
                text: msg.replace(/^✅\s*/i, ""),
                timer: 1600,
                showConfirmButton: false,
                background: "rgba(20,20,20,0.98)",
                color: "#fff",
            });

            await loadUsers();
        } catch (e) {
            Swal.close();
            setErr("No se pudo eliminar el usuario. Verifica rol ADMIN y que el usuario exista.");
            await Swal.fire({
                icon: "error",
                title: "Error",
                text: "No se pudo eliminar el usuario. Verifica rol ADMIN y que el usuario exista.",
                confirmButtonText: "Cerrar",
                background: "rgba(20,20,20,0.98)",
                color: "#fff",
                confirmButtonColor: "#ff4d4d",
            });
        } finally {
            setBusyUser("");
        }
    };

    return (
        <div className="app-shell">
            {/* Sidebar (coincide con estilo general) */}
            <aside className="sidebar">
                <div className="brand">🎧 SyncUp</div>
                <div>
                    <button className="navbtn" onClick={() => navigate("/home")}>
                        <span style={{ width: 20, textAlign: "center" }}>🏠</span> Home
                    </button>
                    <button className="navbtn" onClick={() => navigate("/admin/canciones")}>
                        <span style={{ width: 20, textAlign: "center" }}>🎵</span> Admin · Canciones
                    </button>
                    <button className="navbtn active">
                        <span style={{ width: 20, textAlign: "center" }}>👥</span> Admin · Usuarios
                    </button>
                </div>
            </aside>

            <main className="main" style={{ paddingBottom: 140 }}>
                {/* Topbar compacta como en otras páginas */}
                <div className="topbar">
                    <div className="pill">‹</div>
                    <div className="pill">›</div>
                    <button className="profile" onClick={() => navigate("/home")}>
                        Volver
                    </button>
                </div>

                {/* ======== Encabezado ======== */}
                <section className="section">
                    <h2>Admin · Usuarios</h2>
                </section>

                {/* ======== Acciones/Listado ======== */}
                <section className="section">
                    <div className="card" style={{ maxWidth: 980, margin: "0 auto" }}>
                        {/* Barra de acciones */}
                        <div
                            style={{
                                display: "grid",
                                gridTemplateColumns: "1fr auto",
                                gap: 10,
                                marginBottom: 12,
                                alignItems: "center",
                            }}
                        >
                            <input
                                className="select"
                                placeholder="Filtrar por usuario, nombre o rol…"
                                value={filter}
                                onChange={(e) => setFilter(e.target.value)}
                            />
                            <div style={{ display: "flex", gap: 8 }}>
                                <button
                                    className="btn btn-sm btn-outline-light"
                                    onClick={loadUsers}
                                    disabled={loading}
                                    title="Refrescar lista"
                                >
                                    {loading ? "Cargando..." : "Refrescar"}
                                </button>
                            </div>
                        </div>

                        {/* Mensajes (persisten en tarjeta además del SweetAlert) */}
                        {err && (
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
                                {err}
                            </div>
                        )}
                        {info && (
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
                                {info}
                            </div>
                        )}

                        {/* Tabla / Lista */}
                        <div
                            style={{
                                width: "100%",
                                overflowX: "auto",
                                borderTop: "1px solid rgba(255,255,255,0.06)",
                            }}
                        >
                            <table style={{ width: "100%", borderCollapse: "collapse" }}>
                                <thead>
                                <tr className="card-muted" style={{ textAlign: "left" }}>
                                    <th style={{ padding: "10px 8px" }}>Usuario</th>
                                    <th style={{ padding: "10px 8px" }}>Nombre</th>
                                    <th style={{ padding: "10px 8px" }}>Rol</th>
                                    <th style={{ padding: "10px 8px", textAlign: "right" }}>Acciones</th>
                                </tr>
                                </thead>
                                <tbody>
                                {loading && (
                                    <tr>
                                        <td colSpan={4} className="card-muted" style={{ padding: 12 }}>
                                            Cargando usuarios…
                                        </td>
                                    </tr>
                                )}

                                {!loading && filtered.length === 0 && (
                                    <tr>
                                        <td colSpan={4} className="card-muted" style={{ padding: 12 }}>
                                            No hay usuarios para mostrar.
                                        </td>
                                    </tr>
                                )}

                                {!loading &&
                                    filtered.map((u, i) => {
                                        const username = u.username || u.user || "";
                                        const nombre = u.nombre || u.name || "—";
                                        const rol = String(u.rol || u.role || "").toUpperCase() || "USER";
                                        const isMe = username && username === me;

                                        return (
                                            <tr
                                                key={`${username}-${i}`}
                                                style={{
                                                    borderTop: "1px solid rgba(255,255,255,0.06)",
                                                }}
                                            >
                                                {/* Texto blanco para username y nombre */}
                                                <td style={{ padding: "10px 8px", fontWeight: 600, color: "#fff" }}>
                                                    {username}
                                                </td>
                                                <td style={{ padding: "10px 8px", color: "#fff" }}>
                                                    {nombre}
                                                </td>
                                                <td style={{ padding: "10px 8px" }}>
                            <span
                                style={{
                                    background: "rgba(255,255,255,0.08)",
                                    padding: "2px 8px",
                                    borderRadius: 999,
                                    fontSize: 12,
                                    color: "#ddd",
                                }}
                            >
                              {rol}
                            </span>
                                                </td>
                                                <td style={{ padding: "10px 8px" }}>
                                                    <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
                                                        <button
                                                            className="btn btn-sm btn-outline-light"
                                                            style={{ opacity: isMe ? 0.5 : 1 }}
                                                            onClick={() => onDelete(username)}
                                                            disabled={busyUser === username || isMe}
                                                            title={
                                                                isMe
                                                                    ? "No puedes eliminar tu propio usuario activo."
                                                                    : "Eliminar usuario"
                                                            }
                                                        >
                                                            {busyUser === username ? "Eliminando..." : "Eliminar"}
                                                        </button>
                                                    </div>
                                                </td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        </div>

                        {/* Pie con totales */}
                        <div
                            className="card-muted"
                            style={{ marginTop: 10, display: "flex", justifyContent: "space-between" }}
                        >
                            <span>Total: <b>{users.length}</b></span>
                            <span>Filtrados: <b>{filtered.length}</b></span>
                        </div>
                    </div>
                </section>
            </main>
        </div>
    );
}
