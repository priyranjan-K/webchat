import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import ChatHeader  from './chat/ChatHeader'
import MessageList from './chat/MessageList'
import MessageInput from './chat/MessageInput'
import useMessages from '../hooks/useMessages'
import ws from '../services/websocketService'

/**
 * ChatPanel — orchestrates the center chat area.
 * Delegates message logic to useMessages hook.
 * Supports both group chat and direct message (DM) modes.
 *
 * Props:
 *   chatMode    'group' | 'dm'
 *   group       ChatGroup object (group mode)
 *   dmPhone     recipient phone (dm mode)
 *   myPhone     current user's phone
 */
function ChatPanel({ chatMode, group, dmPhone, myPhone, allUsers, resolveName, showInfo, onToggleInfo }) {
  const [input, setInput] = useState('')

  const targetId  = chatMode === 'group' ? group?.groupId : dmPhone
  const chatName  = chatMode === 'group' ? group?.groupName : resolveName(dmPhone)
  const hasLeft   = chatMode === 'group' && group && !group.members?.includes(myPhone)

  const { messages, members, sendMessage } = useMessages(chatMode, targetId, myPhone)

  const otherMembersNames = Array.isArray(members)
    ? members
        .filter(m => (m?.phoneNumber || m) !== myPhone)
        .map(m => m?.displayName || resolveName(m))
    : []

  const handleSend = () => {
    if (!input.trim()) return
    sendMessage(input.trim())
    setInput('')
  }

  // ─── Empty state ───────────────────────────────────────────────
  if (!targetId) {
    return (
      <div className="chat-empty">
        <div className="empty-icon">💬</div>
        <h3>Start a conversation</h3>
        <p>Pick a group from the sidebar or message someone directly from the People tab</p>
      </div>
    )
  }

  return (
    <>
      <ChatHeader
        chatMode={chatMode}
        group={group}
        recipientPhone={dmPhone}
        recipientName={resolveName(dmPhone)}
        memberCount={members.length}
        memberNames={otherMembersNames}
        showInfo={showInfo}
        onToggleInfo={onToggleInfo}
      />

      <MessageList
        messages={messages}
        chatName={chatName}
      />

      <MessageInput
        value={input}
        onChange={setInput}
        onSend={handleSend}
        placeholder={
          hasLeft 
            ? 'You cannot send messages because you have left this group.' 
            : ws.isConnected 
              ? `Message ${chatName}…` 
              : 'Reconnecting…'
        }
        disabled={!ws.isConnected || hasLeft}
      />
    </>
  )
}

export default ChatPanel
