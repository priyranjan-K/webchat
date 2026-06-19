/**
 * WebSocketService
 *
 * Manages the single persistent WebSocket connection to the server.
 * Supports typed event listeners, automatic exponential back-off reconnection,
 * and JWT authentication via query parameter (needed for the WS handshake).
 */
import { STORAGE_KEYS, WS_RECONNECT_DELAYS } from '../config/constants'

class WebSocketService {
  constructor() {
    /** @type {WebSocket|null} */
    this.ws = null

    /** Whether the WebSocket is currently connected and ready. */
    this.isConnected = false

    /** The logged-in user's phone number, used as the sender identity. */
    this.sender = null

    /** @type {Object.<string, Function[]>} Typed event listener registry. */
    this.listeners = {}

    this._reconnectAttempt = 0
    this._reconnectTimer   = null
    this._intentionalClose = false
  }

  // ── Public API ──────────────────────────────────────────────────────────────

  /**
   * Opens a WebSocket connection for the given user.
   * Call this once after login.
   *
   * @param {string} sender - the logged-in user's phone number
   */
  connect(sender) {
    this.sender           = sender
    this._intentionalClose = false
    this._doConnect()
  }

  /**
   * Gracefully closes the WebSocket and cancels any pending reconnects.
   * Call this on logout.
   */
  disconnect() {
    this._intentionalClose = true
    clearTimeout(this._reconnectTimer)
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.isConnected = false
    this.sender      = null
  }

  /**
   * Sends a typed message to the server.
   *
   * @param {string} type   - WebSocket message type (see WS_MESSAGE_TYPES)
   * @param {Object} fields - additional fields to merge into the message payload
   * @returns {boolean} {@code true} if the message was sent, {@code false} if not connected
   */
  send(type, fields = {}) {
    if (!this.isConnected || !this.ws || this.ws.readyState !== WebSocket.OPEN) return false
    const msg = {
      type,
      sender:    this.sender,
      timestamp: Date.now(),
      ...fields,
    }
    try {
      this.ws.send(JSON.stringify(msg))
      return true
    } catch (err) {
      console.error('Failed to send WebSocket message:', err)
      return false
    }
  }

  /**
   * Registers a listener for a specific message type.
   *
   * @param {string}   type     - message type to listen for (or '_connected', '_disconnected')
   * @param {Function} callback - invoked with the parsed message object
   */
  on(type, callback) {
    if (!this.listeners[type]) this.listeners[type] = []
    this.listeners[type].push(callback)
  }

  /**
   * Removes a previously registered listener.
   *
   * @param {string}   type     - message type
   * @param {Function} callback - the exact callback reference to remove
   */
  off(type, callback) {
    if (!this.listeners[type]) return
    this.listeners[type] = this.listeners[type].filter((cb) => cb !== callback)
  }

  // ── Internal ────────────────────────────────────────────────────────────────

  _doConnect() {
    const token    = sessionStorage.getItem(STORAGE_KEYS.AUTH_TOKEN)
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const url      = `${protocol}//${window.location.host}/ws/chat${token ? `?token=${token}` : ''}`

    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      this._reconnectAttempt = 0
      
      // Ensure connection transitions to OPEN before sending registration
      const establishSession = () => {
        if (!this.ws) return
        if (this.ws.readyState === WebSocket.OPEN) {
          this.isConnected = true
          this.send('REGISTER')
          this._emit('_connected', {})
        } else if (this.ws.readyState === WebSocket.CONNECTING) {
          setTimeout(establishSession, 50)
        }
      }
      establishSession()
    }

    this.ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        this._emit(msg.type, msg)
      } catch {
        // Ignore non-JSON frames (e.g. server health pings)
      }
    }

    this.ws.onerror = () => {
      this.isConnected = false
    }

    this.ws.onclose = () => {
      this.isConnected = false
      this._emit('_disconnected', {})
      if (!this._intentionalClose) {
        this._scheduleReconnect()
      }
    }
  }

  _scheduleReconnect() {
    const delay = WS_RECONNECT_DELAYS[
      Math.min(this._reconnectAttempt, WS_RECONNECT_DELAYS.length - 1)
    ]
    this._reconnectAttempt++
    this._reconnectTimer = setTimeout(() => this._doConnect(), delay)
  }

  _emit(type, data) {
    ;(this.listeners[type] ?? []).forEach((cb) => cb(data))
  }
}

/** Singleton instance shared across the entire application. */
export default new WebSocketService()
