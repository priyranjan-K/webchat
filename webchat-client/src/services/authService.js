import axios from 'axios'
import {
  API_BASE_URL,
  PUBLIC_AUTH_ENDPOINTS,
  STORAGE_KEYS,
} from '../config/constants'
import ws from './websocketService'

/**
 * Pre-configured Axios instance for all REST calls.
 * Automatically attaches the JWT Bearer token from sessionStorage.
 */
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ── Request interceptor — attach JWT ────────────────────────────────────────

apiClient.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(STORAGE_KEYS.AUTH_TOKEN)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ── Response interceptor — auto-logout on 401 / 403 ────────────────────────

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    if (status === 401 || status === 403) {
      const url = error.config?.url ?? ''
      const isPublic = PUBLIC_AUTH_ENDPOINTS.some((path) => url.includes(path))
      if (!isPublic) {
        authService.logout()
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

// ── Auth service ─────────────────────────────────────────────────────────────

/**
 * Authentication service — wraps all auth-related REST calls.
 */
export const authService = {
  /**
   * Registers a new user.
   * @param {string} phoneNumber
   * @param {string} password
   * @param {string} displayName
   * @returns {Promise<AuthResponse>}
   */
  signup: async (phoneNumber, password, displayName) => {
    const { data } = await apiClient.post('/auth/signup', {
      phoneNumber,
      password,
      displayName,
    })
    return data
  },

  /**
   * Authenticates an existing user.
   * @param {string} phoneNumber
   * @param {string} password
   * @returns {Promise<AuthResponse>}
   */
  login: async (phoneNumber, password) => {
    const { data } = await apiClient.post('/auth/login', { phoneNumber, password })
    return data
  },

  /**
   * Logs the current user out.
   * Calls the server to revoke the JWT, disconnects WebSocket,
   * and clears all local auth state.
   */
  logout: async () => {
    try {
      await apiClient.post('/auth/logout')
    } catch {
      // Ignore token-expiry or network failures on logout
    }
    ws.disconnect()
    sessionStorage.removeItem(STORAGE_KEYS.AUTH_TOKEN)
    sessionStorage.removeItem(STORAGE_KEYS.USER_PHONE)
    sessionStorage.removeItem(STORAGE_KEYS.DISPLAY_NAME)
  },

  /**
   * Initiates the forgot-password flow by requesting an OTP.
   * @param {string} phoneNumber
   * @returns {Promise<AuthResponse>}
   */
  requestPasswordReset: async (phoneNumber) => {
    const { data } = await apiClient.post(
      `/auth/forgot-password/request?phoneNumber=${encodeURIComponent(phoneNumber)}`
    )
    return data
  },

  /**
   * Completes a password reset with an OTP and new password.
   * @param {string} phoneNumber
   * @param {string} otp
   * @param {string} newPassword
   * @returns {Promise<AuthResponse>}
   */
  resetPassword: async (phoneNumber, otp, newPassword) => {
    const { data } = await apiClient.post('/auth/forgot-password/reset', {
      phoneNumber,
      otp,
      newPassword,
    })
    return data
  },
}

export default apiClient
