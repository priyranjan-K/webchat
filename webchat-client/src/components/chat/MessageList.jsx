import React, { useEffect, useRef } from 'react'
import MessageBubble from '../MessageBubble'
import SystemMessage from './SystemMessage'

/**
 * MessageList — scrollable area showing all messages for the active chat.
 */
function MessageList({ messages, chatName }) {
  const endRef = useRef(null)

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  return (
    <div className="messages-area">
      {messages.length === 0 && (
        <div style={{
          textAlign:  'center',
          color:      'var(--text-muted)',
          fontSize:   '0.85rem',
          marginTop:  '40px',
        }}>
          Start the conversation in <strong style={{ color: 'var(--accent)' }}>{chatName}</strong> 👋
        </div>
      )}

      {messages.map((msg, i) =>
        msg.system
          ? <SystemMessage key={msg.id ?? i} content={msg.content} />
          : <MessageBubble
              key={msg.id ?? i}
              content={msg.content}
              sent={msg.sent}
              time={msg.time}
              senderInitial={msg.sender?.charAt(0)?.toUpperCase() || '?'}
              read={msg.read}
            />
      )}

      <div ref={endRef} />
    </div>
  )
}

export default MessageList
