const BASE_URL = process.env.REACT_APP_API_URL;

export const apiClient = {
    get: async (path, token) => {
        const res = await fetch(`${BASE_URL}${path}`, {
            headers: {
                'Accept': 'application/json',
                ...(token ? { Authorization: `Bearer ${token}` } : {})
            }
        });
        if (!res.ok) throw new Error(res.status);
        return res.json();
    },
    post: async (path, body, token) => {
        const res = await fetch(`${BASE_URL}${path}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...(token ? { Authorization: `Bearer ${token}` } : {})
            },
            body: JSON.stringify(body)
        });
        if (!res.ok) throw new Error(res.status);
        return res.json();
    },
    del: async (path, token) => {
        const res = await fetch(`${BASE_URL}${path}`, {
            method: 'DELETE',
            headers: {
                ...(token ? { Authorization: `Bearer ${token}` } : {})
            }
        });
        if (!res.ok) throw new Error(res.status);
        return res.text();
    }
};
