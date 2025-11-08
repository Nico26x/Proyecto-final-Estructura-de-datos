export const initialAuthState = { token: null, user: null };

export function authReducer(state, action) {
    switch (action.type) {
        case 'LOGIN_SUCCESS':
            return { token: action.payload.token, user: action.payload.usuario };
        case 'LOGOUT':
            return { token: null, user: null };
        default:
            return state;
    }
}
