import axios from 'axios';

const api = axios.create({
  baseURL: '', // Vite proxy routes /artworks, /auth, /users → localhost:8080
  timeout: 30000,
});

// Attach JWT token to every request automatically
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('artsight_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Redirect to login on 401 — but NOT for /auth/** endpoints (e.g. wrong password returns 401)
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const url = err.config?.url ?? '';
    if (err.response?.status === 401 && !url.startsWith('/auth/')) {
      localStorage.removeItem('artsight_token');
      window.location.href = '/auth';
    }
    return Promise.reject(err);
  }
);

export default api;
