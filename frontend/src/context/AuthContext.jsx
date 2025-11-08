import { createContext, useContext, useEffect, useState } from "react";

const AuthContext = createContext(null);

// Hook para consumir el contexto
export function useAuth() {
    return useContext(AuthContext);
}

// 👉 Provider (exportación con nombre)
export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(null);

    // Carga inicial desde storage (si ya había sesión)
    useEffect(() => {
        try {
            const t = localStorage.getItem("token");
            const u = localStorage.getItem("user");
            if (t) setToken(t);
            if (u) setUser(JSON.parse(u));
        } catch (_) {}
    }, []);

    const login = (userObj, jwt) => {
        setUser(userObj);
        setToken(jwt);
        localStorage.setItem("token", jwt);
        localStorage.setItem("user", JSON.stringify(userObj));
    };

    const logout = () => {
        setUser(null);
        setToken(null);
        localStorage.removeItem("token");
        localStorage.removeItem("user");
    };

    const value = {
        user,
        token,
        isAuthenticated: !!token,
        login,
        logout,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// También lo exportamos por defecto, por si importas sin llaves.
export default AuthProvider;
