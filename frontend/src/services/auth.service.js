import { apiClient } from './apiClient';

export const authService = {
    login: (username, password) =>
        apiClient.post('/api/usuarios/login?username='+encodeURIComponent(username)+'&password='+encodeURIComponent(password)),
    register: (username, password, nombre) =>
        apiClient.post('/api/usuarios/registrar?username='+encodeURIComponent(username)+'&password='+encodeURIComponent(password)+'&nombre='+encodeURIComponent(nombre)),
};
