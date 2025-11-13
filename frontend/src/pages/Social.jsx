import React, { useState, useEffect, useCallback } from "react";

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
    const [sugerencias, setSugerencias] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [limite, setLimite] = useState(5);

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

            // Manejo explícito de 204 No Content
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
            // data debería ser string[] (usernames)
            setSugerencias(Array.isArray(data) ? data : []);
        } catch (e) {
            setError("❌ Error en la solicitud.");
            setSugerencias([]);
        } finally {
            setLoading(false);
        }
    }, [token, username, limite]);

    useEffect(() => {
        obtenerSugerencias();
    }, [obtenerSugerencias]);

    return (
        <div className="social-page">
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

            <div style={{ marginTop: 12 }}>
                <h2>Usuarios sugeridos:</h2>
                {sugerencias.length === 0 && !loading && !error && (
                    <div className="card-muted">No hay sugerencias por ahora.</div>
                )}
                {sugerencias.length > 0 && (
                    <ul>
                        {sugerencias.map((u, i) => (
                            <li key={`${u}-${i}`}>{u}</li>
                        ))}
                    </ul>
                )}
            </div>
        </div>
    );
};

export default Social;
