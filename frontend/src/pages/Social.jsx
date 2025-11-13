import React, { useState, useEffect, useCallback } from "react";
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

    const token = getActiveToken();
    const username = getUsernameFromToken();

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
                        onClick={() => window.history.back()}
                    >
                        ← Volver
                    </button>

                    {/* Botón de perfil */}
                    <button
                        className="profile"
                        onClick={() => alert('Abrir menú de perfil')} // Placeholder para abrir el menú de perfil
                    >
                        👤 {username || "Perfil"}
                    </button>
                </div>

                <section className="section">
                    <h1>Bienvenido a la página Social</h1>
                    <p>¡Aquí podrás interactuar con otros usuarios y compartir contenido!</p>
                    <div style={{ display: "flex", gap: 8, alignItems: "center", margin: "12px 0" }}>
                        <label htmlFor="limite">Número de sugerencias:</label>
                        <input
                            type="number"
                            id="limite"
                            value={limite}
                            onChange={(e) => setLimite(Number(e.target.value || 0))}
                            min="1"
                            style={{ width: 80 }}
                        />
                        <button onClick={obtenerSugerencias} disabled={loading}>
                            {loading ? "Cargando..." : "Obtener sugerencias"}
                        </button>
                    </div>

                    {error && <div className="error" style={{ color: "#ffb3b3" }}>{error}</div>}
                    {mensaje && <div className="mensaje" style={{ color: "#4caf50" }}>{mensaje}</div>} {/* Mostrar mensaje de éxito */}

                    <div style={{ marginTop: 12 }}>
                        <h2>Usuarios sugeridos:</h2>
                        {sugerencias.length === 0 && !loading && !error && (
                            <div className="card-muted">No hay sugerencias por ahora.</div>
                        )}
                        {sugerencias.length > 0 && (
                            <ul>
                                {sugerencias.map((u, i) => (
                                    <li key={`${u}-${i}`}>
                                        {u}
                                        <button
                                            onClick={() => toggleSeguir(u)}
                                            style={{
                                                backgroundColor: seguido.has(u) ? "red" : "green",
                                                color: "white",
                                                padding: "5px",
                                            }}
                                        >
                                            Seguir
                                        </button>
                                        {seguido.has(u) && (
                                            <button
                                                onClick={() => toggleDejarDeSeguir(u)}
                                                style={{
                                                    backgroundColor: "gray",
                                                    color: "white",
                                                    padding: "5px",
                                                    marginLeft: "10px",
                                                }}
                                            >
                                                Dejar de seguir
                                            </button>
                                        )}
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>

                    {/* Mostrar lista de usuarios seguidos */}
                    <div style={{ marginTop: 12 }}>
                        <h2>Usuarios seguidos:</h2>
                        {seguido.size === 0 && (
                            <div className="card-muted">No estás siguiendo a nadie.</div>
                        )}
                        {seguido.size > 0 && (
                            <ul>
                                {Array.from(seguido).map((usuario, i) => (
                                    <li key={`${usuario}-${i}`}>{usuario}</li>
                                ))}
                            </ul>
                        )}
                    </div>
                </section>
            </main>
        </div>
    );
};

export default Social;
