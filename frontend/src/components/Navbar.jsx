import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
    const { token, user, logout } = useAuth();
    const navigate = useNavigate();

    const onLogout = () => {
        logout();
        navigate('/login');
    };

    return (
        <nav className="navbar navbar-expand-lg bg-light">
            <div className="container">
                <Link className="navbar-brand" to="/">🎵 Plataforma</Link>
                <div className="ms-auto">
                    {token ? (
                        <div className="d-flex align-items-center gap-2">
                            <span className="text-muted small">{user?.username}</span>
                            <button className="btn btn-outline-secondary btn-sm" onClick={onLogout}>Salir</button>
                        </div>
                    ) : (
                        <div className="d-flex gap-2">
                            <Link className="btn btn-outline-primary btn-sm" to="/login">Login</Link>
                            <Link className="btn btn-primary btn-sm" to="/registro">Registro</Link>
                        </div>
                    )}
                </div>
            </div>
        </nav>
    );
}
