import React, { useState } from "react";

export default function Login() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [mensaje, setMensaje] = useState("");
  const [usuario, setUsuario] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();

    const url = `http://localhost:8080/api/usuarios/login?username=${username}&password=${password}`;

    try {
      const response = await fetch(url, { method: "POST" });

      if (response.ok) {
        const data = await response.json(); // el backend devuelve un Usuario
        if (data) {
          setUsuario(data);
          setMensaje("✅ Inicio de sesión exitoso");
        } else {
          setMensaje("❌ Usuario o contraseña incorrectos");
        }
      } else {
        setMensaje("❌ Error al iniciar sesión");
      }
    } catch (error) {
      console.error(error);
      setMensaje("🚨 No se pudo conectar con el servidor");
    }
  };

  return (
    <div style={{ maxWidth: "400px", margin: "2rem auto" }}>
      <h2>Iniciar Sesión</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Usuario</label><br />
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
        </div>

        <div>
          <label>Contraseña</label><br />
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>

        <button type="submit" style={{ marginTop: "1rem" }}>
          Ingresar
        </button>
      </form>

      {mensaje && (
        <p style={{ marginTop: "1rem", fontWeight: "bold" }}>{mensaje}</p>
      )}

      {usuario && (
        <div style={{ marginTop: "1rem", background: "#f3f3f3", padding: "1rem", borderRadius: "8px" }}>
          <h3>👋 Bienvenido, {usuario.nombre}!</h3>
          <p>Usuario: {usuario.username}</p>
        </div>
      )}
    </div>
  );
}
