import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/home.css";

const API = "http://localhost:8080";

const getToken = () =>
    localStorage.getItem("token") || localStorage.getItem("admin_token") || "";
const authHeaders = () =>
    getToken() ? { Authorization: `Bearer ${getToken()}` } : {};

async function apiGet(path) {
    const r = await fetch(`${API}${path}`, { headers: { ...authHeaders() } });
    if (!r.ok) throw new Error(`GET ${path} -> ${r.status}`);
    return r.json();
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

export default function AdminCanciones() {
    const navigate = useNavigate();
    const role = decodeRole();
    const isAdmin = role.includes("ADMIN");

    const [songs, setSongs] = useState([]);

    useEffect(() => {
        if (!isAdmin) {
            navigate("/home", { replace: true });
            return;
        }
        apiGet("/api/canciones")
            .then(setSongs)
            .catch(() => setSongs([]));
    }, [isAdmin, navigate]);

    return (
        <div className="app-shell">
            <aside className="sidebar">
                <div className="brand">🎧 SyncUp</div>
                <div>
                    <button className="navbtn" onClick={() => navigate("/home")}>
                        <span style={{ width: 20, textAlign: "center" }}>🏠</span> Home
                    </button>
                    <button className="navbtn active">
                        <span style={{ width: 20, textAlign: "center" }}>🎵</span> Admin · Canciones
                    </button>
                    <button className="navbtn" onClick={() => navigate("/admin/usuarios")}>
                        <span style={{ width: 20, textAlign: "center" }}>👥</span> Admin · Usuarios
                    </button>
                </div>
            </aside>

            <main className="main">
                <div className="topbar">
                    <div className="pill">‹</div>
                    <div className="pill">›</div>
                    <button className="profile" onClick={() => navigate("/home")}>Volver</button>
                </div>

                <section className="section">
                    <h2>Admin · Canciones</h2>
                    <div className="card">
                        <div className="card-muted">
                            Aquí podrás cargar CSV, crear/editar/eliminar canciones (pendiente UI).
                        </div>
                    </div>
                </section>

                <section className="section">
                    <h2>Catálogo actual</h2>
                    <div className="row-scroll">
                        {songs.map((s) => (
                            <div className="card" key={s.id}>
                                <div className="card-title" style={{ color: "#ff4d4d" }}>{s.titulo}</div>
                                <div className="card-muted">{s.artista} · {s.genero} · {s.anio}</div>
                                <button className="btn btn-sm btn-outline-light" disabled>Editar (pronto)</button>
                            </div>
                        ))}
                        {songs.length === 0 && (
                            <div className="card"><div className="card-muted">No hay canciones.</div></div>
                        )}
                    </div>
                </section>
            </main>
        </div>
    );
}
