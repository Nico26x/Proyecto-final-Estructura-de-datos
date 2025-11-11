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

// ⬇️ Añadimos el botón “Similares” en cada tarjeta
function SectionRow({ title, items, onPick, onShowSimilares, showSimButton = true }) {
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
                        <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                            <button
                                className="btn btn-sm btn-outline-light"
                                onClick={() => onPick(s)}
                            >
                                Reproducir
                            </button>
                            {showSimButton && (
                                <button
                                    className="btn btn-sm btn-outline-light"
                                    onClick={() => onShowSimilares?.(s)}
                                >
                                    Similares
                                </button>
                            )}
                        </div>
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

    // 👉 Autocompletar
    const [suggestions, setSuggestions] = useState([]);
    const [showSugg, setShowSugg] = useState(false);
    const [searchLoading, setSearchLoading] = useState(false);

    // 👉 Búsqueda avanzada
    const [adv, setAdv] = useState({
        titulo: "",
        artista: "",
        genero: "",
        anioFrom: "",
        anioTo: "",
        op: "AND",
    });
    const [advLoading, setAdvLoading] = useState(false);
    const [advError, setAdvError] = useState("");
    const [advResults, setAdvResults] = useState([]);
    const [advSearched, setAdvSearched] = useState(false);

    // 👉 NUEVO: Similares (caché por canción)
    const [simMap, setSimMap] = useState({}); // id(str) -> array de canciones
    const [simFor, setSimFor] = useState(null); // { id, titulo }
    const [simLoading, setSimLoading] = useState(false);
    const [simError, setSimError] = useState("");

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

    // Toggle favorito
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

    // 🔎 Autocompletar (debounce)
    useEffect(() => {
        const q = query.trim();
        if (q.length < 2) {
            setSuggestions([]);
            setShowSugg(false);
            setSearchLoading(false);
            return;
        }
        let alive = true;
        setSearchLoading(true);
        const timer = setTimeout(() => {
            apiGet(`/api/canciones/autocompletar?prefijo=${encodeURIComponent(q)}`)
                .then((list) => {
                    if (!alive) return;
                    if (Array.isArray(list)) {
                        setSuggestions(list.slice(0, 8));
                        setShowSugg(true);
                    } else {
                        setSuggestions([]);
                        setShowSugg(false);
                    }
                })
                .catch(() => {
                    if (!alive) return;
                    setSuggestions([]);
                    setShowSugg(false);
                })
                .finally(() => alive && setSearchLoading(false));
        }, 300);
        return () => {
            alive = false;
            clearTimeout(timer);
        };
    }, [query]);

    const handlePickSuggestion = (text) => {
        setQuery(text);
        setShowSugg(false);
    };

    // 🔍 Búsqueda avanzada
    const onAdvChange = (e) => {
        const { name, value } = e.target;
        setAdv((p) => ({ ...p, [name]: value }));
    };

    const onAdvSearch = async (e) => {
        e?.preventDefault?.();
        setAdvError("");
        setAdvLoading(true);
        setAdvSearched(false);
        setAdvResults([]);

        const params = new URLSearchParams();
        if (adv.titulo.trim()) params.append("titulo", adv.titulo.trim());
        if (adv.artista.trim()) params.append("artista", adv.artista.trim());
        if (adv.genero.trim()) params.append("genero", adv.genero.trim());
        if (String(adv.anioFrom).trim()) params.append("anioFrom", Number(adv.anioFrom));
        if (String(adv.anioTo).trim()) params.append("anioTo", Number(adv.anioTo));
        if (adv.op) params.append("op", adv.op);

        try {
            const list = await apiGet(`/api/canciones/buscar/avanzado?${params.toString()}`);
            setAdvResults(Array.isArray(list) ? list : []);
            setAdvSearched(true);
        } catch {
            setAdvError("No se pudo realizar la búsqueda avanzada.");
        } finally {
            setAdvLoading(false);
        }
    };

    // 🎵 NUEVO: handler para “Similares”
    const handleShowSimilares = async (song) => {
        if (!song) return;
        const id = idToStr(song.id);
        setSimError("");
        setSimFor({ id, titulo: song.titulo || "" });

        // si ya está en caché, no refetch
        if (simMap[id]) return;

        setSimLoading(true);
        try {
            const list = await apiGet(`/api/canciones/${encodeURIComponent(id)}/similares?limite=8`);
            setSimMap((prev) => ({ ...prev, [id]: Array.isArray(list) ? list : [] }));
        } catch {
            setSimError("No se pudieron cargar canciones similares.");
        } finally {
            setSimLoading(false);
        }
    };

    // 🔒 Logout handler
    const handleLogout = async () => {
        try {
            await apiPost("/api/usuarios/logout");
        } catch {}
        localStorage.removeItem("token");
        localStorage.removeItem("admin_token");
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
                            onShowSimilares={handleShowSimilares}
                            showSimButton
                        />
                        <SectionRow
                            title="Catálogo"
                            items={songs}
                            onPick={setCurrent}
                            onShowSimilares={handleShowSimilares}
                            showSimButton
                        />
                    </>
                )}

                {tab === "buscar" && (
                    <>
                        {/* Búsqueda simple + autocompletar */}
                        <section className="section">
                            <h2>Buscar</h2>
                            <div style={{ position: "relative", width: "100%", maxWidth: 420, marginLeft: 12 }}>
                                <input
                                    className="select"
                                    style={{ width: "100%" }}
                                    placeholder="Busca por título, artista o género…"
                                    value={query}
                                    onChange={(e) => setQuery(e.target.value)}
                                    onFocus={() => suggestions.length && setShowSugg(true)}
                                    onBlur={() => setTimeout(() => setShowSugg(false), 120)}
                                />
                                {showSugg && suggestions.length > 0 && (
                                    <div
                                        className="dropdown"
                                        style={{
                                            position: "absolute",
                                            left: 0,
                                            top: "calc(100% + 6px)",
                                            width: "100%",
                                            background: "rgba(255,255,255,0.06)",
                                            border: "1px solid rgba(255,255,255,0.1)",
                                            backdropFilter: "blur(6px)",
                                            borderRadius: 8,
                                            padding: 6,
                                            boxShadow: "0 6px 24px rgba(0,0,0,0.25)",
                                            zIndex: 5,
                                        }}
                                    >
                                        {suggestions.map((sug, i) => (
                                            <button
                                                key={`${sug}-${i}`}
                                                className="btn btn-sm btn-outline-light"
                                                style={{ width: "100%", textAlign: "left", marginBottom: 4 }}
                                                onMouseDown={(e) => e.preventDefault()}
                                                onClick={() => handlePickSuggestion(sug)}
                                            >
                                                {sug}
                                            </button>
                                        ))}
                                    </div>
                                )}
                            </div>
                            {searchLoading && (
                                <div className="card" style={{ marginTop: 12, maxWidth: 420, marginLeft: 12 }}>
                                    <div className="card-muted">Buscando…</div>
                                </div>
                            )}
                        </section>

                        {query.trim().length > 0 && (
                            <SectionRow
                                title="Resultados"
                                items={filtered}
                                onPick={setCurrent}
                                onShowSimilares={handleShowSimilares}
                                showSimButton
                            />
                        )}

                        {/* ======= Búsqueda avanzada ======= */}
                        <section className="section">
                            <h2>Búsqueda avanzada</h2>
                            <div className="card" style={{ maxWidth: 900 }}>
                                <form
                                    onSubmit={onAdvSearch}
                                    style={{
                                        display: "grid",
                                        gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
                                        gap: 12,
                                        alignItems: "end",
                                    }}
                                >
                                    <div>
                                        <label className="card-muted" style={{ display: "block", marginBottom: 6 }}>
                                            Título
                                        </label>
                                        <input
                                            className="select"
                                            name="titulo"
                                            value={adv.titulo}
                                            onChange={onAdvChange}
                                        />
                                    </div>

                                    <div>
                                        <label className="card-muted" style={{ display: "block", marginBottom: 6 }}>
                                            Artista
                                        </label>
                                        <input
                                            className="select"
                                            name="artista"
                                            value={adv.artista}
                                            onChange={onAdvChange}
                                        />
                                    </div>

                                    <div>
                                        <label className="card-muted" style={{ display: "block", marginBottom: 6 }}>
                                            Género
                                        </label>
                                        <input
                                            className="select"
                                            name="genero"
                                            value={adv.genero}
                                            onChange={onAdvChange}
                                        />
                                    </div>

                                    <div>
                                        <label className="card-muted" style={{ display: "block", marginBottom: 6 }}>
                                            Año desde
                                        </label>
                                        <input
                                            className="select"
                                            name="anioFrom"
                                            value={adv.anioFrom}
                                            onChange={onAdvChange}
                                        />
                                    </div>

                                    <div>
                                        <label className="card-muted" style={{ display: "block", marginBottom: 6 }}>
                                            Año hasta
                                        </label>
                                        <input
                                            className="select"
                                            name="anioTo"
                                            value={adv.anioTo}
                                            onChange={onAdvChange}
                                        />
                                    </div>

                                    <div>
                                        <label className="card-muted" style={{ display: "block", marginBottom: 6 }}>
                                            Operador
                                        </label>
                                        <select
                                            className="select"
                                            name="op"
                                            value={adv.op}
                                            onChange={onAdvChange}
                                        >
                                            <option value="AND">AND (todas)</option>
                                            <option value="OR">OR (cualquiera)</option>
                                        </select>
                                    </div>

                                    <div style={{ gridColumn: "1 / -1", display: "flex", justifyContent: "center" }}>
                                        <button type="submit" className="btn btn-sm btn-outline-light">
                                            {advLoading ? "Buscando..." : "Buscar"}
                                        </button>
                                    </div>
                                </form>

                                {advError && (
                                    <div
                                        className="alert alert-danger"
                                        style={{
                                            background: "#3d1414",
                                            color: "#ffd7d7",
                                            padding: "8px 10px",
                                            borderRadius: 8,
                                            marginTop: 10,
                                            textAlign: "center",
                                        }}
                                    >
                                        {advError}
                                    </div>
                                )}
                            </div>
                        </section>

                        {advSearched && (
                            <div style={{ marginTop: 28 }}>
                                <SectionRow
                                    title="Resultados avanzados"
                                    items={advResults}
                                    onPick={setCurrent}
                                    onShowSimilares={handleShowSimilares}
                                    showSimButton
                                />
                            </div>
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
                            onShowSimilares={handleShowSimilares}
                            showSimButton
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
                            onShowSimilares={handleShowSimilares}
                            showSimButton
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

                {/* ======= NUEVO: Sección de similares seleccionada ======= */}
                {simFor && (
                    <div style={{ marginTop: 28 }}>
                        {simError && (
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
                                {simError}
                            </div>
                        )}
                        {simLoading ? (
                            <section className="section">
                                <h2>Similares a “{simFor.titulo}”</h2>
                                <div className="card">
                                    <div className="card-muted">Cargando similares…</div>
                                </div>
                            </section>
                        ) : (
                            <SectionRow
                                title={`Similares a “${simFor.titulo}”`}
                                items={simMap[simFor.id] || []}
                                onPick={setCurrent}
                                onShowSimilares={handleShowSimilares}
                                showSimButton
                            />
                        )}
                    </div>
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
