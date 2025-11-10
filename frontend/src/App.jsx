// App.jsx
import AppRouter from './Routes/AppRouter';
import { AuthProvider } from './context/AuthContext'; // <-- importa tu provider

export default function App() {
    return (
        <AuthProvider>
            <AppRouter />
        </AuthProvider>
    );
}
