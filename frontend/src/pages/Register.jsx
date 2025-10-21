import React, { useState } from "react";

export default function Register() {
  const [username, setUsername] = useState("");
  const [nombre, setNombre] = useState("");
  const [password, setPassword] = useState("");
  const [mensaje, setMensaje] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Construimos la URL con parámetros
    const url = `http://localhost:8080/api/usuarios/register?username=${username}&password=${password}&nombre=${nombre}`;

    try {
      const response = await fetch(url, {
        method: "POST",
      });

      if (response.ok) {
        const text = await response.text();
        setMensaje(text);
        setUsername("");
        setNombre("");
        setPassword("");
      } else {
        setMensaje("❌ Error al registrar el usuario");
      }
    } catch (error) {
      console.error(error);
      setMensaje("🚨 No se pudo conectar con el servidor");
    }
  };

  return (
    <div style={{ maxWidth: "400px", margin: "2rem auto" }}>
      <h2>Registro de Usuario</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Nombre completo</label><br />
          <input
            type="text"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            required
          />
        </div>

        <div>
          <label>Nombre de usuario</label><br />
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
          Registrarse
        </button>
      </form>

      {mensaje && (
        <p style={{ marginTop: "1rem", fontWeight: "bold" }}>{mensaje}</p>
      )}
    </div>
  );
}
