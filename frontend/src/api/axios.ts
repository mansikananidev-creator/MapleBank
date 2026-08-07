import axios from "axios";

// VITE_API_URL lets a deployed frontend (e.g. on Vercel) point at the deployed
// backend instead of localhost - set it in that host's environment variables.
const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8081/api'
})

api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token')
    if(token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

export default api