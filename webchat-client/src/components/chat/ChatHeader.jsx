import React from 'react'
import Avatar from '../common/Avatar'

/**
 * ChatHeader — top bar of the chat panel.
 * Supports both group chats and direct messages.
 */
function ChatHeader({ chatMode, group, recipientPhone, recipientName, memberCount, memberNames, showInfo, onToggleInfo }) {
  if (chatMode === 'dm') {
    return (
      <div className="chat-header">
        <div className="chat-contact">
          <Avatar name={recipientName} size={40} online />
          <div>
            <div className="chat-contact-name">{recipientName}</div>
            <div className="chat-contact-status" style={{ color: 'var(--success)' }}>
              ● Direct Message
            </div>
          </div>
        </div>
        <div className="chat-header-actions" style={{ display: 'flex', gap: '8px' }}>
          <button className="icon-btn" title="Call">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
            </svg>
          </button>
          <button 
            className={`icon-btn ${showInfo ? 'active' : ''}`} 
            title="Toggle Info" 
            onClick={onToggleInfo}
          >
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12.01" y2="8" />
            </svg>
          </button>
        </div>
      </div>
    )
  }

  const subtitle = [
    `👥 ${memberCount} member${memberCount !== 1 ? 's' : ''}`,
    memberNames?.length > 0 ? memberNames.slice(0, 2).join(', ') + (memberNames.length > 2 ? ' …' : '') : null,
  ].filter(Boolean).join(' · ')

  return (
    <div className="chat-header">
      <div className="chat-contact">
        <Avatar
          name={group?.groupName || 'G'}
          size={40}
          style={{ background: 'linear-gradient(135deg,var(--accent),var(--teal))' }}
        />
        <div>
          <div className="chat-contact-name">{group?.groupName}</div>
          <div className="chat-contact-status">{subtitle}</div>
        </div>
      </div>
      <div className="chat-header-actions" style={{ display: 'flex', gap: '8px' }}>
        <button className="icon-btn" title="Group members">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
            <circle cx="9" cy="7" r="4" />
            <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
            <path d="M16 3.13a4 4 0 0 1 0 7.75" />
          </svg>
        </button>
        <button 
          className={`icon-btn ${showInfo ? 'active' : ''}`} 
          title="Toggle Info" 
          onClick={onToggleInfo}
        >
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="16" x2="12" y2="12" />
            <line x1="12" y1="8" x2="12.01" y2="8" />
          </svg>
        </button>
      </div>
    </div>
  )
}

export default ChatHeader
