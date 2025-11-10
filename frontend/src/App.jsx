// App.jsx
import { useEffect } from "react";
import AppRouter from './Routes/AppRouter';
import { AuthProvider } from './context/AuthContext'; // <-- importa tu provider

// Limpia tokens al iniciar la app una sola vez por carga (evita auto-login)
const PURGE_ON_BOOT = true;

export default function App() {
    useEffect(() => {
        if (PURGE_ON_BOOT && !sessionStorage.getItem("purged")) {
            localStorage.removeItem("token");
            localStorage.removeItem("admin_token");
            sessionStorage.setItem("purged", "1");
        }
    }, []);

    return (
        <AuthProvider>
            <AppRouter />
        </AuthProvider>
    );
}
