/**
 * Application-wide constants for the WebChat client.
 *
 * Import from here instead of repeating magic strings across services and hooks.
 */

// ── Storage Keys ────────────────────────────────────────────────────────────

export const STORAGE_KEYS = Object.freeze({
  AUTH_TOKEN:   'authToken',    // sessionStorage
  USER_PHONE:   'userPhone',    // sessionStorage
  DISPLAY_NAME: 'displayName', // sessionStorage
})

// ── API ─────────────────────────────────────────────────────────────────────

/** Base path for all REST API calls (proxied by Vite to the backend). */
export const API_BASE_URL = '/api'

/** Endpoint paths that are publicly accessible (no JWT required). */
export const PUBLIC_AUTH_ENDPOINTS = Object.freeze([
  '/auth/login',
  '/auth/signup',
  '/auth/forgot-password',
  '/auth/public-key',
])

// ── WebSocket ────────────────────────────────────────────────────────────────

/**
 * Exponential back-off delays (ms) for WebSocket reconnection attempts.
 * After the last entry is reached subsequent retries reuse the final value.
 */
export const WS_RECONNECT_DELAYS = Object.freeze([1000, 2000, 4000, 8000, 15000])

/** Maximum milliseconds to wait for a one-shot WebSocket response before timing out. */
export const WS_RESPONSE_TIMEOUT_MS = 10_000

// ── WebSocket Message Types ───────────────────────────────────────────────────

export const WS_MESSAGE_TYPES = Object.freeze({
  REGISTER:            'REGISTER',
  JOIN:                'JOIN',
  LEAVE:               'LEAVE',
  CREATE_GROUP:        'CREATE_GROUP',
  TEXT:                'TEXT',
  DIRECT_MESSAGE:      'DIRECT_MESSAGE',
  LIST_GROUPS:         'LIST_GROUPS',
  LIST_USERS:          'LIST_USERS',
  LIST_ONLINE_USERS:   'LIST_ONLINE_USERS',
  DELETE_GROUP:        'DELETE_GROUP',
  ADD_GROUP_MEMBER:    'ADD_GROUP_MEMBER',
  REMOVE_GROUP_MEMBER: 'REMOVE_GROUP_MEMBER',
  PROMOTE_MEMBER:      'PROMOTE_MEMBER',
  DEMOTE_MEMBER:       'DEMOTE_MEMBER',
  DELETE_DM:           'DELETE_DM',
  GROUP_INVITATION:    'GROUP_INVITATION',
  GROUP_KICK:          'GROUP_KICK',
})
