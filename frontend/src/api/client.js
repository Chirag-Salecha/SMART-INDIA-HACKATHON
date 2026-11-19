import axios from 'axios'

// Base client for the Spring Boot backend.
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Base client for the FastAPI AI service (used once AI features are wired in).
export const aiClient = axios.create({
  baseURL: import.meta.env.VITE_AI_API_BASE_URL || 'http://localhost:8000/api',
  headers: {
    'Content-Type': 'application/json',
  },
})
