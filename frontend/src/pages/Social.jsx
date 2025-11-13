import React, { useState, useEffect, useCallback, useRef } from "react";
import { useNavigate } from "react-router-dom"; // Importamos el hook useNavigate
import "../styles/home.css";  // Importar el CSS de Home para mantener el diseño

const API_URL = "http://localhost:8080/api/usuarios";

// Helper para leer y decodificar el JWT y sacar el username
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
    const [sugerencias, setSugerencias] = useState([]); // Usuarios sugeridos
    const [loading, setLoading] = useState(false); // Cargando
    const [error, setError] = useState(""); // Error
    const [limite, setLimite] = useState(5); // Limite de sugerencias
    const [seguido, setSeguido] = useState(new Set()); // Usuarios seguidos
    const [mensaje, setMensaje] = useState(""); // Mensaje para mostrar al seguir o dejar de seguir
    const [open, setOpen] = useState(false); // Controlar la visibilidad del menú desplegable

    const token = getActiveToken();
    const username = getUsernameFromToken();
    const role = parseJwt(token)?.rol || ""; // Obtener el rol del token
    const navigate = useNavigate(); // Usamos el hook useNavigate

    const ref = useRef(null);

    const obtenerSugerencias = useCallback(async () => {
        if (!token || !username) {
            setError("🚫 No estás autenticado o falta el username en el token.");
            setSugerencias([]);
            return;
        }

        setLoading(true);
        setError("");

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
                setSeguido(new Set(seguidos)); // Guardamos los seguidos en el set
            }
        } catch (e) {
            setError("❌ Error al obtener usuarios seguidos.");
        }
    }, [token, username]);

    useEffect(() => {
        obtenerSugerencias();
        obtenerSeguidos();
    }, [obtenerSugerencias, obtenerSeguidos]);

    // Seguir usuario
    const toggleSeguir = async (usuario) => {
        if (!token || !username) return;

        console.log(`Intentando seguir a ${usuario}`);

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

            const data = await resp.json();
            console.log(data); // Verifica que data sea un objeto

            if (resp.ok) {
                const newSeguidos = new Set(seguido);
                newSeguidos.add(usuario);  // Agregar siempre al set

                setSeguido(newSeguidos);
                setMensaje(`✅ Ahora sigues a ${usuario}`); // Mostrar mensaje de éxito
            } else {
                setError("❌ No se pudo realizar la acción.");
                console.error("Error al realizar la acción: ", data); // Verifica la respuesta
            }
        } catch (e) {
            setError("❌ Error en la solicitud.");
            console.error("Error de solicitud: ", e);
        }
    };

    // Dejar de seguir usuario
    const toggleDejarDeSeguir = async (usuario) => {
        if (!token || !username) return;

        console.log(`Intentando dejar de seguir a ${usuario}`);

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

            const data = await resp.json();
            console.log(data);

            if (resp.ok) {
                const newSeguidos = new Set(seguido);
                newSeguidos.delete(usuario);  // Eliminar del set si la acción es exitosa

                setSeguido(newSeguidos);
                setMensaje(`🗑️ Has dejado de seguir a ${usuario}`); // Mostrar mensaje de éxito
            } else {
                setError("❌ No se pudo realizar la acción.");
                console.error("Error al realizar la acción: ", data); // Verifica la respuesta
            }
        } catch (e) {
            setError("❌ Error en la solicitud.");
            console.error("Error de solicitud: ", e);
        }
    };

    // Cerrar sesión
    const handleLogout = async () => {
        try {
            // Realizamos la llamada para cerrar sesión en el backend si es necesario
            await fetch(`${API_URL}/logout`, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            });
        } catch (e) {
            console.error("Error al cerrar sesión:", e);
        }
        // Eliminar tokens y redirigir al login
        localStorage.removeItem("token");
        localStorage.removeItem("admin_token");
        navigate("/login", { replace: true });  // Redirigimos al login
    };

    // Detectar clics fuera del menú
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
            {/* Barra lateral */}
            <aside className="sidebar">
                <div className="brand">🎧 SyncUp</div>
                <div>
                    <button className="navbtn active">
                        <span style={{ width: 20, textAlign: "center" }}>👥</span>
                        Social
                    </button>
                </div>
            </aside>

            {/* Contenido de la página */}
            <main className="main">
                <div className="topbar">
                    <button
                        className="profile"
                        onClick={() => setOpen((prev) => !prev)} // Cambia el estado de 'open' para mostrar/ocultar el menú
                    >
                        👤 {username || "Perfil"}
                    </button>

                    {/* Menú desplegable */}
                    {open && (
                        <div
                            ref={ref} // Necesitamos el ref para cerrar el menú si se hace clic fuera
                            className="dropdown-menu"
                            style={{
                                position: "absolute",
                                right: 0,
                                top: "100%",
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
                            {/* Mostrar Admin si es admin */}
                            {role === "admin" && (
                                <button
                                    className="btn btn-sm btn-outline-light"
                                    onClick={() => navigate("/admin")}
                                >
                                    Admin
                                </button>
                            )}

                            {/* Mostrar Editar perfil si no es admin */}
                            {role !== "admin" && (
                                <button
                                    className="btn btn-sm btn-outline-light"
                                    onClick={() => navigate("/perfil/editar")}
                                >
                                    Editar perfil
                                </button>
                            )}

                            {/* Cerrar sesión */}
                            <button
                                className="btn btn-sm btn-outline-light"
                                onClick={handleLogout}
                            >
                                🚪 Cerrar sesión
                            </button>
                        </div>
                    )}
                </div>

                <section className="section">
                    <h1>Bienvenido a la página Social</h1>
                    <p>¡Aquí podrás interactuar con otros usuarios y compartir contenido!</p>
                    {/* Caja de formulario para sugerencias */}
                    <div className="card" style={{ padding: "16px", display: "flex", flexDirection: "column", gap: "16px", maxHeight: 160, maxWidth: 400 }}>
                        <h3 style={{ color: "white", marginBottom: "12px" }}>Número de sugerencias</h3>

                        <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                            <label htmlFor="limite" style={{ color: "var(--muted)", fontSize: "16px", flex: "0 0 auto" }}>
                                Limite:
                            </label>

                            <input
                                type="number"
                                id="limite"
                                value={limite}
                                onChange={(e) => setLimite(Number(e.target.value || 0))}
                                min="1"
                                style={{
                                    padding: "10px",
                                    borderRadius: "8px",
                                    border: "1px solid var(--panel)",
                                    backgroundColor: "var(--panel)",
                                    color: "white",
                                    fontSize: "16px",
                                    width: "80px",
                                    textAlign: "center",
                                    transition: "border-color 0.3s ease",
                                }}
                            />

                            <button
                                onClick={obtenerSugerencias}
                                disabled={loading}
                                style={{
                                    padding: "10px 16px",
                                    borderRadius: "8px",
                                    backgroundColor: "var(--accent)",
                                    color: "white",
                                    fontSize: "16px",
                                    border: "none",
                                    cursor: loading ? "not-allowed" : "pointer",
                                    transition: "background-color 0.3s ease",
                                }}
                            >
                                {loading ? "Cargando..." : "Obtener sugerencias"}
                            </button>
                        </div>
                    </div>

                    {error && <div className="error" style={{ color: "#ffb3b3" }}>{error}</div>}
                    {mensaje && <div className="mensaje" style={{ color: "#4caf50" }}>{mensaje}</div>} {/* Mostrar mensaje de éxito */}

                    <div style={{ marginTop: 12 }}>
                        <h2>Usuarios sugeridos:</h2>
                        {sugerencias.length === 0 && !loading && !error && (
                            <div className="card-muted">No hay sugerencias por ahora.</div>
                        )}
                        {sugerencias.length > 0 && (
                            <div className="row-scroll">
                                {sugerencias.map((u, i) => (
                                    <div className="card" key={`${u}-${i}`} style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                                        <h3 style={{ color: 'white' }}>{u}</h3> {/* Nombre de usuario en blanco */}
                                        <div style={{ display: 'flex', gap: '10px' }}>
                                            {/* Botón "Seguir" solo si no se sigue al usuario */}
                                            {!seguido.has(u) ? (
                                                <button
                                                    onClick={() => toggleSeguir(u)}
                                                    style={{
                                                        backgroundColor: "green",
                                                        color: "white",
                                                        padding: "8px 12px",
                                                        borderRadius: "8px",
                                                        cursor: "pointer",
                                                        transition: "background-color 0.3s ease",
                                                    }}
                                                >
                                                    Seguir
                                                </button>
                                            ) : (
                                                <button
                                                    onClick={() => toggleDejarDeSeguir(u)}
                                                    style={{
                                                        backgroundColor: "gray",
                                                        color: "white",
                                                        padding: "8px 12px",
                                                        borderRadius: "8px",
                                                    }}
                                                >
                                                    Dejar de seguir
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* Mostrar lista de usuarios seguidos */}
                    <div style={{ marginTop: 12 }}>
                        <h2>Usuarios seguidos:</h2>
                        {seguido.size === 0 ? (
                            <div className="card-muted" style={{ padding: '16px', textAlign: 'center' }}>
                                No estás siguiendo a nadie.
                            </div>
                        ) : (
                            <div className="row-scroll">
                                {Array.from(seguido).map((usuario, i) => (
                                    <div className="card" key={`${usuario}-${i}`} style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                                        <h3 style={{ color: 'white' }}>{usuario}</h3> {/* Solo el nombre del usuario */}
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>


                </section>
            </main>
        </div>
    );
};

export default Social;
