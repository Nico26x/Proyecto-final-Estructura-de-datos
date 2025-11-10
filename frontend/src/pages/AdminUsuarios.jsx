import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/home.css";

function decodeRole() {
    try {
        const t = localStorage.getItem("token") || localStorage.getItem("admin_token") || "";
        if (!t) return "";
        const [, payload] = t.split(".");
        const data = JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
        return (data.rol || data.role || "").toUpperCase();
    } catch {
        return "";
    }
}

export default function AdminUsuarios() {
    const navigate = useNavigate();
    const role = decodeRole();
    const isAdmin = role.includes("ADMIN");

    useEffect(() => {
        if (!isAdmin) navigate("/home", { replace: true });
    }, [isAdmin, navigate]);

    return (
        <div className="app-shell">
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

            <main className="main">
                <div className="topbar">
                    <div className="pill">‹</div>
                    <div className="pill">›</div>
                    <button className="profile" onClick={() => navigate("/home")}>Volver</button>
                </div>

                <section className="section">
                    <h2>Admin · Usuarios</h2>
                    <div className="card">
                        <div className="card-muted">
                            Aquí podrás listar/eliminar usuarios, ver métricas, etc. (pendiente UI).
                        </div>
                    </div>
                </section>
            </main>
        </div>
    );
}
