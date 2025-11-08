// Routes/AppRouter.jsx
import { Routes, Route, Navigate } from "react-router-dom";
import Home from "../pages/Home";
import Login from "../pages/Login";
import Register from "../pages/Register";
import { useAuth } from "../context/AuthContext";

function PrivateRoute({ children }) {
    const { isAuthenticated } = useAuth(); // p.ej. !!localStorage.getItem('token')
    return isAuthenticated ? children : <Navigate to="/login" replace />;
}

export default function AppRouter() {
    return (
        <Routes>
            {/* si entras a /, manda a /login */}
            <Route path="/" element={<Navigate to="/login" replace />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route
                path="/home"
                element={
                    <PrivateRoute>
                        <Home />
                    </PrivateRoute>
                }
            />
            <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
    );
}
