import React, { useState } from "react";
import Login from "./pages/Login";
import Register from "./pages/Register";

function App() {
  const [pagina, setPagina] = useState("login");

  return (
    <div>
      <nav style={{ margin: "1rem" }}>
        <button onClick={() => setPagina("login")}>Iniciar Sesión</button>
        <button onClick={() => setPagina("register")}>Registrarse</button>
      </nav>

      {pagina === "login" ? <Login /> : <Register />}
    </div>
  );
}

export default App;
