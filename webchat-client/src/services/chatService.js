import ws from './websocketService'
import apiClient from './authService'
import { WS_MESSAGE_TYPES, WS_RESPONSE_TIMEOUT_MS } from '../config/constants'

/**
 * Registers a one-shot listener for a WebSocket message type.
 * Cleans up after the first matching message or after the timeout.
 *
 * @param {string} type - the WS message type to wait for
 * @returns {Promise<Object>} resolves with the server response
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
  listGroups() {
    const promise = once(WS_MESSAGE_TYPES.LIST_GROUPS)
    ws.send(WS_MESSAGE_TYPES.LIST_GROUPS)
    return promise
  },

  createGroup(name) {
    const promise = once(WS_MESSAGE_TYPES.CREATE_GROUP)
    ws.send(WS_MESSAGE_TYPES.CREATE_GROUP, { content: name })
    return promise
  },

  joinGroup(groupId) {
    ws.send(WS_MESSAGE_TYPES.JOIN, { groupId })
  },

  leaveGroup(groupId) {
    ws.send(WS_MESSAGE_TYPES.LEAVE, { groupId })
  },

  sendMessage(groupId, content) {
    ws.send(WS_MESSAGE_TYPES.TEXT, { groupId, content })
  },

  listUsers(groupId) {
    const promise = once(WS_MESSAGE_TYPES.LIST_USERS)
    ws.send(WS_MESSAGE_TYPES.LIST_USERS, { groupId })
    return promise
  },

  async getAllUsers() {
    const { data } = await apiClient.get('/contacts')
    return data
  },

  async addContact(phone) {
    const { data } = await apiClient.post(`/contacts/add?phone=${encodeURIComponent(phone)}`)
    return data
  },

  async getDmHistory(counterpart) {
    const { data } = await apiClient.get(`/messages/dm/${counterpart}`)
    return data
  },

  async getGroupHistory(groupId) {
    const { data } = await apiClient.get(`/messages/group/${groupId}`)
    return data
  },
}

export default chatService
