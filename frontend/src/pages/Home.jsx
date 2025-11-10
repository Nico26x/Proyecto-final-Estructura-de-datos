// src/pages/Home.jsx
import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/home.css";

const API = "http://localhost:8080";

// ----- Helpers de auth y fetch -----
const getToken = () =>
    localStorage.getItem("token") || localStorage.getItem("admin_token") || "";
const authHeaders = () =>
    getToken() ? { Authorization: `Bearer ${getToken()}` } : {};

async function apiGet(path) {
    const r = await fetch(`${API}${path}`, { headers: { ...authHeaders() } });
    if (!r.ok) throw new Error(`GET ${path} -> ${r.status}`);
    return r.json();
}
async function apiPost(path) {
    const r = await fetch(`${API}${path}`, {
        method: "POST",
        headers: { ...authHeaders() },
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
        headers: { ...authHeaders() },
    });
    if (!r.ok) throw new Error(`DELETE ${path} -> ${r.status}`);
    try {
        return await r.json();
    } catch {
        return true;
    }
}

function decodeToken() {
    try {
        const t = getToken();
        if (!t) return {};
        const [, payload] = t.split(".");
        const data = JSON.parse(
            atob(payload.replace(/-/g, "+").replace(/_/g, "/"))
        );
        return {
            username: data.sub || data.username || null,
            role: data.rol || data.role || null,
        };
    } catch {
        return {};
    }
}

const idToStr = (id) => (id == null ? "" : String(id));

// =================== UI ===================
function Sidebar({ tab, setTab }) {
    const items = [
        { id: "home", label: "Home", icon: "🏠" },
        { id: "buscar", label: "Buscar", icon: "🔎" },
        { id: "favoritos", label: "Favoritos", icon: "❤️" },
        { id: "descubrimiento", label: "Descubrimiento", icon: "🧭" },
        { id: "social", label: "Social", icon: "👥" },
    ];
    return (
        <aside className="sidebar">
            <div className="brand">🎧 SyncUp</div>
            <div>
                {items.map((i) => (
                    <button
                        key={i.id}
                        className={`navbtn ${tab === i.id ? "active" : ""}`}
                        onClick={() => setTab(i.id)}
                    >
                        <span style={{ width: 20, textAlign: "center" }}>{i.icon}</span>
                        {i.label}
                    </button>
                ))}
            </div>
        </aside>
    );
}

function TopBar({ username, role, isAdmin, onGoAdminCanciones, onGoAdminUsuarios, onLogout }) {
    const [open, setOpen] = useState(false);
    const [adminOpen, setAdminOpen] = useState(false);
    const ref = useRef(null);

    // Cerrar al hacer click fuera
    useEffect(() => {
        const handler = (e) => {
            if (ref.current && !ref.current.contains(e.target)) {
                setOpen(false);
                setAdminOpen(false);
            }
        };
        document.addEventListener("mousedown", handler);
        return () => document.removeEventListener("mousedown", handler);
    }, []);

    return (
        <div className="topbar" ref={ref} style={{ position: "relative" }}>
            <div className="pill">‹</div>
            <div className="pill">›</div>

            <button
                className="profile"
                onClick={() => setOpen((v) => !v)}
                aria-haspopup="menu"
                aria-expanded={open}
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
                        minWidth: 200,
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

                    {/* Bloque Admin (solo si es admin) */}
                    {isAdmin && (
                        <div
                            style={{
                                marginBottom: 8,
                                borderBottom: "1px solid rgba(255,255,255,0.08)",
                                paddingBottom: 8,
                            }}
                        >
                            <button
                                className="btn btn-sm btn-outline-light"
                                style={{ width: "100%", marginBottom: 6 }}
                                onClick={() => setAdminOpen((v) => !v)}
                            >
                                🔧 Admin
                            </button>

                            {adminOpen && (
                                <div style={{ display: "grid", gap: 6 }}>
                                    <button
                                        className="btn btn-sm btn-outline-light"
                                        onClick={() => {
                                            onGoAdminCanciones?.();
                                            setOpen(false);
                                            setAdminOpen(false);
                                        }}
                                    >
                                        🎵 Canciones
                                    </button>
                                    <button
                                        className="btn btn-sm btn-outline-light"
                                        onClick={() => {
                                            onGoAdminUsuarios?.();
                                            setOpen(false);
                                            setAdminOpen(false);
                                        }}
                                    >
                                        👥 Usuarios
                                    </button>
                                </div>
                            )}
                        </div>
                    )}

                    <button
                        className="btn btn-sm btn-outline-light"
                        style={{ width: "100%" }}
                        onClick={async () => {
                            setOpen(false);
                            setAdminOpen(false);
                            if (onLogout) await onLogout();
                        }}
                    >
                        🚪 Cerrar sesión
                    </button>
                </div>
            )}
        </div>
    );
}

function SectionRow({ title, items, onPick }) {
    return (
        <section className="section">
            <h2>{title}</h2>
            <div className="row-scroll">
                {items.map((s) => (
                    <div key={s.id} className="card">
                        <div className="card-title" style={{ color: "#ff4d4d" }}>
                            {s.titulo}
                        </div>
                        <div className="card-muted">
                            {s.artista} · {s.genero} · {s.anio}
                        </div>
                        <button
                            className="btn btn-sm btn-outline-light"
                            onClick={() => onPick(s)}
                        >
                            Reproducir
                        </button>
                    </div>
                ))}
                {items.length === 0 && (
                    <div className="card">
                        <div className="card-muted">No hay elementos para mostrar.</div>
                    </div>
                )}
            </div>
        </section>
    );
}

function PlayerBar({ song, onPrev, onNext, onToggleFav, isFav }) {
    const audioRef = useRef(null);
    const [playing, setPlaying] = useState(false);
    const [pos, setPos] = useState(0);
    const [dur, setDur] = useState(0);
    const [vol, setVol] = useState(0.8);

    useEffect(() => {
        if (!audioRef.current) return;
        if (song?.fileName) {
            audioRef.current.src = `/music/${song.fileName}`;
            audioRef.current.pause();
            setPlaying(false);
            setPos(0);
        } else {
            audioRef.current.removeAttribute("src");
            audioRef.current.load();
            setPlaying(false);
            setPos(0);
        }
    }, [song]);

    const togglePlay = () => {
        if (!audioRef.current?.src) return;
        if (playing) {
            audioRef.current.pause();
            setPlaying(false);
        } else {
            audioRef.current
                .play()
                .then(() => setPlaying(true))
                .catch(() => {});
        }
    };
    const onTimeUpdate = () => setPos(audioRef.current?.currentTime || 0);
    const onLoaded = () => setDur(audioRef.current?.duration || 0);
    const seek = (e) => {
        const v = Number(e.target.value);
        if (!Number.isNaN(v) && audioRef.current) {
            audioRef.current.currentTime = v;
            setPos(v);
        }
    };
    const changeVol = (e) => {
        const v = Number(e.target.value);
        setVol(v);
        if (audioRef.current) audioRef.current.volume = v;
    };
    const fmt = (secs) => {
        const s = Math.floor(secs % 60)
            .toString()
            .padStart(2, "0");
        const m = Math.floor(secs / 60).toString();
        return `${m}:${s}`;
    };

    return (
        <>
            <div
                className="playerbar"
                style={{ height: 110, paddingTop: 14, paddingBottom: 14 }}
            >
                <div className="trackmeta">
                    <div>
                        <div style={{ fontWeight: 700 }}>{song?.titulo || "—"}</div>
                        <div className="card-muted" style={{ fontSize: 12 }}>
                            {song?.artista || ""}
                            {song?.genero ? ` · ${song.genero}` : ""}
                        </div>
                    </div>
                    {song && (
                        <button
                            className="heart"
                            onClick={onToggleFav}
                            title="Agregar/Quitar de favoritos"
                            style={{ color: isFav ? "#ff4d4d" : "#aaa" }}
                        >
                            {isFav ? "♥" : "♡"}
                        </button>
                    )}
                </div>

                <div className="controls">
                    <button className="ctrl" onClick={onPrev} title="Anterior">
                        ⏮
                    </button>
                    <button
                        className="ctrl play"
                        onClick={togglePlay}
                        title={playing ? "Pausar" : "Reproducir"}
                    >
                        {playing ? "⏸" : "▶"}
                    </button>
                    <button className="ctrl" onClick={onNext} title="Siguiente">
                        ⏭
                    </button>
                    <div className="line" style={{ width: 520 }}>
                        <span className="time">{fmt(pos)}</span>
                        <input
                            className="range"
                            type="range"
                            min={0}
                            max={dur || 0}
                            step="1"
                            value={pos}
                            onChange={seek}
                        />
                        <span className="time">{fmt(dur || 0)}</span>
                    </div>
                </div>

                <div className="line">
                    <span className="time">🔊</span>
                    <input
                        className="range"
                        type="range"
                        min="0"
                        max="1"
                        step="0.01"
                        value={vol}
                        onChange={changeVol}
                        style={{ width: 110 }}
                    />
                </div>
            </div>

            <audio
                ref={audioRef}
                onTimeUpdate={onTimeUpdate}
                onLoadedMetadata={onLoaded}
                preload="metadata"
            />
        </>
    );
}

export default function Home() {
    const navigate = useNavigate();

    // 🔄 Auth reactivo
    const [auth, setAuth] = useState(() => decodeToken());
    const username = auth.username;
    const role = auth.role || "";
    const isAdmin = String(role).toUpperCase().includes("ADMIN");

    const [tab, setTab] = useState("home");

    const [songs, setSongs] = useState([]);
    const [discover, setDiscover] = useState([]);
    const [current, setCurrent] = useState(null);

    // Guardamos favoritos como Set<string> para búsquedas O(1)
    const [favSet, setFavSet] = useState(() => new Set());

    const [query, setQuery] = useState("");

    // 🔄 Escuchar cambios de token en storage / foco / visibilidad
    useEffect(() => {
        const handleStorage = () => {
            setAuth(decodeToken());
        };
        const refresh = () => setAuth(decodeToken());

        window.addEventListener("storage", handleStorage);
        window.addEventListener("focus", refresh);
        document.addEventListener("visibilitychange", refresh);

        return () => {
            window.removeEventListener("storage", handleStorage);
            window.removeEventListener("focus", refresh);
            document.removeEventListener("visibilitychange", refresh);
        };
    }, []);

    // Cargar catálogo
    useEffect(() => {
        apiGet("/api/canciones")
            .then((list) => {
                setSongs(list);
                if (!current && list.length) setCurrent(list[0]); // sin autoplay
            })
            .catch(() => setSongs([]));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // Cargar favoritos y descubrimiento (con token)
    useEffect(() => {
        if (!username) return;

        // Favoritos del usuario
        apiGet(`/api/usuarios/${encodeURIComponent(username)}/favoritos`)
            .then((list) => {
                const ids = new Set(list.map((c) => idToStr(c.id)));
                setFavSet(ids);
            })
            .catch(() => setFavSet(new Set()));

        // Descubrimiento
        apiGet(`/api/usuarios/${encodeURIComponent(username)}/descubrimiento?size=12`)
            .then((list) => Array.isArray(list) && setDiscover(list))
            .catch(() => setDiscover([]));
    }, [username]);

    // ¿La canción actual está en favoritos?
    const isFav = current ? favSet.has(idToStr(current.id)) : false;

    // Toggle favorito → llama a los endpoints del UsuarioController con token
    const toggleFav = async () => {
        if (!username || !current) return;

        const songId = encodeURIComponent(idToStr(current.id));
        const base = `/api/usuarios/${encodeURIComponent(username)}/favoritos`;

        try {
            if (favSet.has(idToStr(current.id))) {
                await apiDelete(`${base}/eliminar?idCancion=${songId}`);
                setFavSet((prev) => {
                    const next = new Set(prev);
                    next.delete(idToStr(current.id));
                    return next;
                });
            } else {
                await apiPost(`${base}/agregar?idCancion=${songId}`);
                setFavSet((prev) => new Set(prev).add(idToStr(current.id)));
            }
        } catch (e) {
            console.error("Favoritos error:", e);
        }
    };

    const next = () => {
        if (!songs.length || !current) return;
        const idx = songs.findIndex((s) => idToStr(s.id) === idToStr(current.id));
        const nextIdx = (idx + 1) % songs.length;
        setCurrent(songs[nextIdx]);
    };
    const prev = () => {
        if (!songs.length || !current) return;
        const idx = songs.findIndex((s) => idToStr(s.id) === idToStr(current.id));
        const prevIdx = (idx - 1 + songs.length) % songs.length;
        setCurrent(songs[prevIdx]);
    };

    const filtered =
        query
            ? songs.filter((s) =>
                `${s.titulo} ${s.artista} ${s.genero}`
                    .toLowerCase()
                    .includes(query.toLowerCase())
            )
            : [];

    const favSongs = songs.filter((s) => favSet.has(idToStr(s.id)));

    // 🔒 Logout handler centralizado
    const handleLogout = async () => {
        try {
            await apiPost("/api/usuarios/logout");
        } catch {}
        localStorage.removeItem("token");
        localStorage.removeItem("admin_token"); // importante para admin
        setAuth({});
        window.location.href = "/login";
    };

    return (
        <div className="app-shell">
            <Sidebar tab={tab} setTab={setTab} />
            <main className="main">
                <TopBar
                    username={username}
                    role={role}
                    isAdmin={isAdmin}
                    onGoAdminCanciones={() => navigate("/admin/canciones")}
                    onGoAdminUsuarios={() => navigate("/admin/usuarios")}
                    onLogout={handleLogout}
                />

                {tab === "home" && (
                    <>
                        <section className="section">
                            <h2>¡Bienvenido{username ? `, ${username}` : ""}!</h2>
                        </section>
                        <SectionRow
                            title="Reproducido recientemente"
                            items={songs.slice(0, 8)}
                            onPick={setCurrent}
                        />
                        <SectionRow title="Catálogo" items={songs} onPick={setCurrent} />
                    </>
                )}

                {tab === "buscar" && (
                    <>
                        <section className="section">
                            <h2>Buscar</h2>
                            <input
                                className="select"
                                style={{ width: "100%", maxWidth: 420, marginLeft: 12 }}
                                placeholder="Busca por título, artista o género…"
                                value={query}
                                onChange={(e) => setQuery(e.target.value)}
                            />
                        </section>
                        {query.trim().length > 0 && (
                            <SectionRow title="Resultados" items={filtered} onPick={setCurrent} />
                        )}
                    </>
                )}

                {tab === "favoritos" && (
                    <>
                        <section className="section">
                            <h2>Favoritos</h2>
                        </section>
                        <SectionRow
                            title="Tus canciones favoritas"
                            items={favSongs}
                            onPick={setCurrent}
                        />
                    </>
                )}

                {tab === "descubrimiento" && (
                    <>
                        <section className="section">
                            <h2>Descubrimiento</h2>
                        </section>
                        <SectionRow
                            title="Recomendado para ti"
                            items={discover}
                            onPick={setCurrent}
                        />
                    </>
                )}

                {tab === "social" && (
                    <>
                        <section className="section">
                            <h2>Social</h2>
                            <div className="card">
                                <div className="card-muted">
                                    Aquí irán “Seguir / Dejar de seguir / Seguidos / Sugerencias”.
                                </div>
                            </div>
                        </section>
                    </>
                )}

                <PlayerBar
                    song={current}
                    onPrev={prev}
                    onNext={next}
                    onToggleFav={toggleFav}
                    isFav={isFav}
                />
            </main>
        </div>
    );
}
