// src/context/AuthContext.jsx
import { createContext, useEffect, useMemo, useState } from "react";
import { loginUser, getSession, registerUser } from "../api/auth";

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [token, setToken] = useState(() => localStorage.getItem("token") || "");
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(!!token);

    useEffect(() => {
        async function bootstrap() {
            if (!token) return;
            try {
                const { data } = await getSession(token);
                setUser(data);
            } catch {
                localStorage.removeItem("token");
                setToken("");
                setUser(null);
            } finally {
                setLoading(false);
            }
        }
        bootstrap();
    }, [token]);

    async function login(username, password) {
        const { data } = await loginUser(username, password);
        const tk = data?.token;
        if (tk) {
            localStorage.setItem("token", tk);
            setToken(tk);
            // opcional: también podrías setear user aquí si lo devuelves en login
        }
        return data;
    }

    async function register(payload) {
        // payload: { username, password, nombre }
        const { data } = await registerUser(payload);
        return data;
    }

    function logout() {
        localStorage.removeItem("token");
        setToken("");
        setUser(null);
    }

    const value = useMemo(
        () => ({ token, user, loading, login, register, logout }),
        [token, user, loading]
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// default export del contexto (por si lo importas como default)
export default AuthContext;
