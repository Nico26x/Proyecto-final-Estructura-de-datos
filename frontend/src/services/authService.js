// src/services/authService.js
const API_URL = "http://localhost:8080/api/usuarios";

export async function registerUser(username, password, nombre) {
  const response = await fetch(`${API_URL}/register?username=${username}&password=${password}&nombre=${nombre}`, {
    method: "POST",
  });
  const data = await response.text();
  return data;
}

export async function loginUser(username, password) {
  const response = await fetch(`${API_URL}/login?username=${username}&password=${password}`, {
    method: "POST",
  });
  if (!response.ok) {
    throw new Error("Error en la petición");
  }
  return await response.json();  // si no encuentra usuario, devuelve null
}
