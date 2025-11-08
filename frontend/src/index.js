// src/index.js
import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";

// Bootstrap global (si lo usas)
import "bootstrap/dist/css/bootstrap.min.css";
// Tus estilos globales opcionales
import "./styles/index.css";

import App from "./App";
// Si tienes contexto de auth
import { AuthProvider } from "./context/AuthContext";

ReactDOM.createRoot(document.getElementById("root")).render(
    <React.StrictMode>
        <BrowserRouter>
            <AuthProvider>
                <App />
            </AuthProvider>
        </BrowserRouter>
    </React.StrictMode>
);
