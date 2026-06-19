/**
 * chatService.js
 *
 * Higher-level helpers that wrap WebSocket messages as Promises and
 * REST calls for history retrieval.
 *
 * WebSocket request/response pairs are resolved via a one-shot listener
 * with a configurable timeout to prevent listener leaks when the server
 * does not reply.
 */
import ws from './websocketService'
import apiClient from './authService'
import { WS_MESSAGE_TYPES, WS_RESPONSE_TIMEOUT_MS } from '../config/constants'

/**
 * Registers a one-shot listener for a WebSocket message type.
 * Automatically cleans up after the first matching message arrives,
 * or after {@link WS_RESPONSE_TIMEOUT_MS} ms (whichever comes first).
 *
 * @param {string} type - the WS message type to wait for
 * @returns {Promise<Object>} resolves with the server response message
 */
const once = (type) =>
  new Promise((resolve, reject) => {
    let timeoutId

    const handler = (data) => {
      clearTimeout(timeoutId)
      ws.off(type, handler)
      resolve(data)
    }

    timeoutId = setTimeout(() => {
      ws.off(type, handler)
      reject(new Error(`WebSocket response timeout for type: ${type}`))
    }, WS_RESPONSE_TIMEOUT_MS)

    ws.on(type, handler)
  })

export const chatService = {
  /**
   * Fetches all active groups from the server.
   * @returns {Promise<Object>} server response containing group list in `.data`
   */
  listGroups() {
    const promise = once(WS_MESSAGE_TYPES.LIST_GROUPS)
    ws.send(WS_MESSAGE_TYPES.LIST_GROUPS)
    return promise
  },

  /**
   * Creates a new named group.
   * @param {string} name - human-readable group name
   * @returns {Promise<Object>} server response containing the created group in `.data`
   */
  createGroup(name) {
    const promise = once(WS_MESSAGE_TYPES.CREATE_GROUP)
    ws.send(WS_MESSAGE_TYPES.CREATE_GROUP, { content: name })
    return promise
  },

  /**
   * Joins a group room (begins receiving group messages).
   * @param {string} groupId
   */
  joinGroup(groupId) {
    ws.send(WS_MESSAGE_TYPES.JOIN, { groupId })
  },

  /**
   * Leaves a group room (stops receiving group messages).
   * @param {string} groupId
   */
  leaveGroup(groupId) {
    ws.send(WS_MESSAGE_TYPES.LEAVE, { groupId })
  },

  /**
   * Sends a text message to a group.
   * @param {string} groupId
   * @param {string} content
   */
  sendMessage(groupId, content) {
    ws.send(WS_MESSAGE_TYPES.TEXT, { groupId, content })
  },

  /**
   * Lists members of a specific group.
   * @param {string} groupId
   * @returns {Promise<Object>} server response with member list in `.data`
   */
  listUsers(groupId) {
    const promise = once(WS_MESSAGE_TYPES.LIST_USERS)
    ws.send(WS_MESSAGE_TYPES.LIST_USERS, { groupId })
    return promise
  },

  /**
   * Fetches the current user's contact list (REST).
   * @returns {Promise<UserDto[]>}
   */
  async getAllUsers() {
    const { data } = await apiClient.get('/contacts')
    return data
  },

  /**
   * Adds a contact by phone number (REST).
   * @param {string} phone
   * @returns {Promise<string>} server confirmation message
   */
  async addContact(phone) {
    const { data } = await apiClient.post(`/contacts/add?phone=${encodeURIComponent(phone)}`)
    return data
  },

  /**
   * Fetches the DM history between the current user and a counterpart (REST).
   * @param {string} counterpart - the other user's phone number
   * @returns {Promise<MessageEntity[]>}
   */
  async getDmHistory(counterpart) {
    const { data } = await apiClient.get(`/messages/dm/${counterpart}`)
    return data
  },

  /**
   * Fetches the message history for a group (REST).
   * @param {string} groupId
   * @returns {Promise<MessageEntity[]>}
   */
  async getGroupHistory(groupId) {
    const { data } = await apiClient.get(`/messages/group/${groupId}`)
    return data
  },
}

export default chatService
