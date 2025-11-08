const BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

export async function http(method, url, { body, token, headers } = {}) {
    const res = await fetch(`${BASE_URL}${url}`, {
        method,
        headers: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
            ...(headers || {}),
        },
        body: body ? JSON.stringify(body) : undefined,
    });

    // intenta parsear JSON si hay contenido
    let data = null;
    const text = await res.text();
    if (text) {
        try { data = JSON.parse(text); } catch { data = text; }
    }

    if (!res.ok) {
        const msg = (data && (data.error || data.message)) || `HTTP ${res.status}`;
        throw new Error(msg);
    }

    return data;
}
