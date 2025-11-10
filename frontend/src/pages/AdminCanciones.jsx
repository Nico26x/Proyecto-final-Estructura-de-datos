// src/pages/AdminCanciones.jsx
import { useState } from "react";
import { useNavigate } from "react-router-dom";

const API = "http://localhost:8080";

// Tokens: usuario normal o admin
const getToken = () =>
    localStorage.getItem("admin_token") || localStorage.getItem("token") || "";
const authHeaders = () =>
    getToken()
        ? { Authorization: `Bearer ${getToken()}`, "Content-Type": "application/json" }
        : { "Content-Type": "application/json" };

async function apiPostJson(path, body) {
    const r = await fetch(`${API}${path}`, {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify(body),
    });
    if (!r.ok) throw new Error(`POST ${path} -> ${r.status}`);
    try {
        return await r.json();
    } catch {
        return true;
    }
}
async function apiDelete(path) {
    const r = await fetch(`${API}${path}`, {
        method: "DELETE",
        headers: getToken() ? { Authorization: `Bearer ${getToken()}` } : {},
    });
    if (!r.ok) throw new Error(`DELETE ${path} -> ${r.status}`);
    const ct = r.headers.get("content-type") || "";
    return ct.includes("application/json") ? r.json() : r.text();
}

// === Sidebar compacta con estilo de Home ===
function AdminSidebar({ active = "canciones" }) {
    const nav = useNavigate();
    const go = (to) => () => nav(to);

    return (
        <aside className="sidebar">
            <div className="brand">🎧 SyncUp</div>

            <div style={{ marginBottom: 10 }}>
                <button className="navbtn" onClick={go("/home")}>
                    <span style={{ width: 20, textAlign: "center" }}>🏠</span>
                    Home
                </button>
            </div>

            <div className="card-muted" style={{ margin: "8px 0 6px", opacity: 0.8 }}>
                Admin
            </div>
            <div>
                <button
                    className={`navbtn ${active === "canciones" ? "active" : ""}`}
                    onClick={go("/admin/canciones")}
                >
                    <span style={{ width: 20, textAlign: "center" }}>🎵</span>
                    Canciones
                </button>
                <button
                    className={`navbtn ${active === "usuarios" ? "active" : ""}`}
                    onClick={go("/admin/usuarios")}
                >
                    <span style={{ width: 20, textAlign: "center" }}>👥</span>
                    Usuarios
                </button>
            </div>
        </aside>
    );
}

export default function AdminCanciones() {
    // ====== Formulario AGREGAR ======
    const [addMsg, setAddMsg] = useState("");
    const [addErr, setAddErr] = useState("");
    const [form, setForm] = useState({
        id: "",
        titulo: "",
        artista: "",
        genero: "",
        anio: "",
        duracion: "",
        fileName: "",
    });
    const setField = (e) => {
        const { name, value } = e.target;
        setForm((p) => ({ ...p, [name]: value }));
    };

    const onSubmitAdd = async (e) => {
        e.preventDefault();
        setAddMsg("");
        setAddErr("");

        if (!form.titulo.trim() || !form.artista.trim() || !form.fileName.trim()) {
            setAddErr("Título, Artista y fileName son obligatorios.");
            return;
        }
        if (!form.anio || isNaN(Number(form.anio))) {
            setAddErr("Año debe ser un número.");
            return;
        }
        if (!form.duracion || isNaN(Number(form.duracion))) {
            setAddErr("Duración debe ser un número (minutos.decimales).");
            return;
        }

        try {
            const payload = {
                id: form.id?.trim() || undefined,
                titulo: form.titulo.trim(),
                artista: form.artista.trim(),
                genero: form.genero.trim(),
                anio: Number(form.anio),
                duracion: Number(String(form.duracion).replace(",", ".")),
                fileName: form.fileName.trim(),
            };

            const resp = await apiPostJson("/api/canciones", payload);
            const okMsg =
                typeof resp === "string" ? resp : "✅ Canción agregada correctamente.";
            setAddMsg(okMsg);
            setForm({
                id: "",
                titulo: "",
                artista: "",
                genero: "",
                anio: "",
                duracion: "",
                fileName: "",
            });
        } catch {
            setAddErr(
                "No se pudo agregar la canción. Verifica el token de admin y el backend."
            );
        }
    };

    // ====== Formulario ELIMINAR ======
    const [delId, setDelId] = useState("");
    const [delMsg, setDelMsg] = useState("");
    const [delErr, setDelErr] = useState("");

    const onSubmitDel = async (e) => {
        e.preventDefault();
        setDelMsg("");
        setDelErr("");

        const id = delId.trim();
        if (!id) {
            setDelErr("Debes ingresar un ID.");
            return;
        }

        try {
            const resp = await apiDelete(`/api/canciones/${encodeURIComponent(id)}`);
            const msg =
                typeof resp === "string" ? resp : "🗑️ Canción eliminada (si existía).";
            setDelMsg(msg);
            setDelId("");
        } catch {
            setDelErr(
                "No se pudo eliminar la canción. Verifica el ID y el token de admin."
            );
        }
    };

    return (
        <div className="app-shell">
            <AdminSidebar active="canciones" />

            <main
                className="main"
                style={{
                    paddingBottom: 140,
                    display: "grid",
                    gridTemplateRows: "auto auto",
                    gap: 18,
                }}
            >
                {/* ====== Card: Agregar canción ====== */}
                <section className="section" style={{ display: "grid", placeItems: "center" }}>
                    <div style={{ width: "100%", maxWidth: 960 }}>
                        <h2 style={{ textAlign: "center", marginBottom: 12 }}>
                            Administrador · Canciones
                        </h2>

                        <div className="card" style={{ maxWidth: 900, margin: "0 auto" }}>
                            {/* Título (ahora rojo) */}
                            <h3
                                style={{
                                    marginTop: 0,
                                    marginBottom: 10,
                                    textAlign: "center",
                                    color: "#ff4d4d",
                                }}
                            >
                                Agregar canción
                            </h3>

                            {addMsg && (
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
                                    {addMsg}
                                </div>
                            )}
                            {addErr && (
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
                                    {addErr}
                                </div>
                            )}

                            <form
                                onSubmit={onSubmitAdd}
                                style={{
                                    display: "grid",
                                    gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
                                    gap: 16,
                                    alignItems: "start",
                                }}
                            >
                                <div>
                                    <label className="card-muted" style={{ display: "block", marginBottom: 6 }}>
                                        ID
                                    </label>
                                    <input className="select" name="id" value={form.id} onChange={setField} />
                                </div>

                                <div>
                                    <label className="card-muted" style={{ display: "block", marginBottom: 6 }}>
                                        Título *
                                    </label>
                                    <input className="select" name="titulo" value={form.titulo} onChange={setField} required />
                                </div>

                                <div>
                                    <label className="card-muted" style={{ display: "block", marginBottom: 6 }}>
                                        Artista *
                                    </label>
                                    <input className="select" name="artista" value={form.artista} onChange={setField} required />
                                </div>

                                <div>
                                    <label className="card-muted" style={{ display: "block", marginBottom: 6 }}>
                                        Género
                                    </label>
                                    <input className="select" name="genero" value={form.genero} onChange={setField} />
                                </div>

                                <div>
                                    <label className="card-muted" style={{ display: "block", marginBottom: 6 }}>
                                        Año *
                                    </label>
                                    <input className="select" name="anio" value={form.anio} onChange={setField} required />
                                </div>

                                <div>
                                    <label className="card-muted" style={{ display: "block", marginBottom: 6 }}>
                                        Duración (min) *
                                    </label>
                                    <input className="select" name="duracion" value={form.duracion} onChange={setField} required />
                                </div>

                                <div style={{ gridColumn: "1 / -1" }}>
                                    <label className="card-muted" style={{ display: "block", marginBottom: 6 }}>
                                        fileName (*.mp3) *
                                    </label>
                                    <input className="select" name="fileName" value={form.fileName} onChange={setField} required />
                                </div>

                                <div style={{ gridColumn: "1 / -1", display: "flex", justifyContent: "center", marginTop: 4 }}>
                                    <button type="submit" className="btn btn-sm btn-outline-light">Guardar</button>
                                </div>
                            </form>

                            {/* Instrucciones (abajo) */}
                            <div
                                className="card-muted"
                                style={{ marginTop: 14, textAlign: "center" }}
                            >
                                1) Copia el <b>.mp3</b> a <code>front/public/music/</code>.<br />
                                2) Registra los datos aquí (el <code>fileName</code> debe coincidir con el nombre real del archivo).
                            </div>
                        </div>
                    </div>
                </section>

                {/* ====== Card: Eliminar canción por ID ====== */}
                <section className="section" style={{ display: "grid", placeItems: "center" }}>
                    <div style={{ width: "100%", maxWidth: 720 }}>
                        <div className="card" style={{ margin: "0 auto" }}>
                            <h3
                                style={{
                                    marginTop: 0,
                                    marginBottom: 10,
                                    textAlign: "center",
                                    color: "#ff4d4d", // título en rojo acento
                                }}
                            >
                                Eliminar canción por ID
                            </h3>

                            {delMsg && (
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
                                    {delMsg}
                                </div>
                            )}
                            {delErr && (
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
                                    {delErr}
                                </div>
                            )}

                            <form
                                onSubmit={onSubmitDel}
                                style={{
                                    display: "grid",
                                    gridTemplateColumns: "1fr auto",
                                    gap: 12,
                                    alignItems: "end",
                                }}
                            >
                                <div>
                                    <label className="card-muted" style={{ display: "block", marginBottom: 6 }}>
                                        ID de la canción
                                    </label>
                                    <input
                                        className="select"
                                        value={delId}
                                        onChange={(e) => setDelId(e.target.value)}
                                        placeholder="Ej: 3"
                                    />
                                </div>

                                <div>
                                    <button type="submit" className="btn btn-sm btn-outline-light" style={{ whiteSpace: "nowrap" }}>
                                        Eliminar
                                    </button>
                                </div>
                            </form>

                            <div className="card-muted" style={{ marginTop: 8, fontSize: 12 }}>
                                * Esta acción elimina la canción del archivo <code>canciones.txt</code> del backend (no borra el .mp3 del front).
                            </div>
                        </div>
                    </div>
                </section>
            </main>
        </div>
    );
}
