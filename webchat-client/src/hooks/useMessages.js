import { useState, useEffect, useCallback, useRef } from 'react'
import ws from '../services/websocketService'
import { chatService } from '../services/chatService'
import { WS_MESSAGE_TYPES } from '../config/constants'

const formatTime = (ts) =>
  ts ? new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''

function useMessages(chatMode, targetId, myPhone) {
  const [messages, setMessages] = useState([])
  const [members,  setMembers]  = useState([])

  const prevTargetRef = useRef(null)
  const prevModeRef   = useRef(null)

  const addMessage = useCallback(
    (msg) => setMessages((prev) => [...prev, msg]),
    []
  )

  // Group TEXT handler
  const onGroupText = useCallback(
    (msg) => {
      if (chatMode !== 'group' || msg.groupId !== targetId) return

      setMessages((prev) => {
        const isMine = msg.sender === myPhone

        if (isMine) {
          // Reconcile optimistic message: update the pending entry in-place
          const optIdx = prev.findIndex(
            (m) => m.sender === myPhone && m.content === msg.content && !m.read
          )
          if (optIdx !== -1) {
            const updated = [...prev]
            updated[optIdx] = {
              ...updated[optIdx],
              id:   msg.messageId ?? updated[optIdx].id,
              time: formatTime(msg.timestamp),
              read: true,
            }
            return updated
          }
        }

        // Deduplicate by messageId
        if (msg.messageId && prev.some((m) => m.id === msg.messageId)) return prev

        return [
          ...prev,
          {
            id:      msg.messageId ?? Date.now(),
            content: msg.content,
            sender:  msg.sender,
            sent:    isMine,
            time:    formatTime(msg.timestamp),
            read:    true,
          },
        ]
      })
    },
    [chatMode, targetId, myPhone]
  )

  // DM handler
  const onDirectMessage = useCallback(
    (msg) => {
      if (chatMode !== 'dm') return
      const isMine      = msg.sender === myPhone
      const counterpart = isMine ? msg.recipientId : msg.sender
      if (counterpart !== targetId) return // not our thread

      setMessages((prev) => {
        // Deduplicate: skip if we already stored this messageId
        if (msg.messageId && prev.some((m) => m.id === msg.messageId)) return prev
        return [
          ...prev,
          {
            id:      msg.messageId ?? Date.now(),
            content: msg.content,
            sender:  msg.sender,
            sent:    isMine,
            time:    formatTime(msg.timestamp),
            read:    true,
          },
        ]
      })
    },
    [chatMode, targetId, myPhone]
  )

  // JOIN / LEAVE system messages
  const onMemberEvent = useCallback(
    (msg) => {
      if (chatMode !== 'group' || msg.groupId !== targetId) return
      addMessage({ id: Date.now(), content: msg.content, system: true, time: formatTime(msg.timestamp) })
      chatService
        .listUsers(targetId)
        .then((res) => setMembers(Array.isArray(res.data) ? res.data : []))
        .catch(() => {})
    },
    [chatMode, targetId, addMessage]
  )

  // Load history and register listeners when target changes
  useEffect(() => {
    const prevTarget = prevTargetRef.current
    const prevMode   = prevModeRef.current

    if (prevTarget && prevTarget !== targetId) {
      if (prevMode === 'group') chatService.leaveGroup(prevTarget)
      setMessages([])
      setMembers([])
    }

    if (!targetId) {
      prevTargetRef.current = null
      prevModeRef.current   = null
      return
    }

    prevTargetRef.current = targetId
    prevModeRef.current   = chatMode

    const mapHistory = (h) => ({
      id:      h.messageId ?? h.id,
      content: h.content,
      sender:  h.sender,
      sent:    h.sender === myPhone,
      time:    formatTime(h.timestamp),
      read:    true,
    })

    if (chatMode === 'group') {
      chatService.joinGroup(targetId)
      chatService
        .listUsers(targetId)
        .then((res) => setMembers(Array.isArray(res.data) ? res.data : []))
        .catch(() => {})
      chatService
        .getGroupHistory(targetId)
        .then((history) => { if (Array.isArray(history)) setMessages(history.map(mapHistory)) })
        .catch(() => {})
    } else if (chatMode === 'dm') {
      chatService
        .getDmHistory(targetId)
        .then((history) => { if (Array.isArray(history)) setMessages(history.map(mapHistory)) })
        .catch(() => {})
    }

    ws.on(WS_MESSAGE_TYPES.TEXT,           onGroupText)
    ws.on(WS_MESSAGE_TYPES.DIRECT_MESSAGE, onDirectMessage)
    ws.on(WS_MESSAGE_TYPES.JOIN,           onMemberEvent)
    ws.on(WS_MESSAGE_TYPES.LEAVE,          onMemberEvent)

    return () => {
      ws.off(WS_MESSAGE_TYPES.TEXT,           onGroupText)
      ws.off(WS_MESSAGE_TYPES.DIRECT_MESSAGE, onDirectMessage)
      ws.off(WS_MESSAGE_TYPES.JOIN,           onMemberEvent)
      ws.off(WS_MESSAGE_TYPES.LEAVE,          onMemberEvent)
    }
  }, [targetId, chatMode, onGroupText, onDirectMessage, onMemberEvent, myPhone])

  // Send
  const sendMessage = useCallback(
    (content) => {
      if (!content.trim() || !targetId || !ws.isConnected) return

      if (chatMode === 'group') {
        chatService.sendMessage(targetId, content)
        // Optimistic insert — server echo reconciles the messageId
        addMessage({
          id:      Date.now(),
          content,
          sender:  myPhone,
          sent:    true,
          time:    formatTime(Date.now()),
          read:    false,
        })
      } else {
        // DM — do NOT add optimistically; wait for server echo (deduplication in onDirectMessage)
        ws.send(WS_MESSAGE_TYPES.DIRECT_MESSAGE, { recipientId: targetId, content })
      }
    },
    [chatMode, targetId, myPhone, addMessage]
  )

  return { messages, members, sendMessage }
}

export default useMessages
