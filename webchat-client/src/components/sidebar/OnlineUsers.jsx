import React from 'react'
import Avatar from '../common/Avatar'

/**
 * OnlineUsers — shows all registered users (contacts) and their online status.
 *
 * Each row has an X button to delete the DM history with that user.
 *
 * @param {Object[]} users               - array of UserDto objects
 * @param {string}   myPhone             - logged-in user's phone number
 * @param {string[]} onlinePhoneNumbers  - list of currently online phone numbers
 * @param {string}   activeDmPhone       - currently open DM phone number
 * @param {Function} onSelectUser        - called with phoneNumber when a row is clicked
 * @param {Function} onRemoveDm          - called with phoneNumber when X is clicked
 */
function OnlineUsers({ users, myPhone, onlinePhoneNumbers = [], activeDmPhone, onSelectUser, onRemoveDm, unreadCounts = {} }) {
  const others = users.filter(user => user.phoneNumber !== myPhone)

  if (others.length === 0) {
    return (
      <div style={{ padding: '20px 16px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.83rem' }}>
        No contacts yet. Add someone with ➕
      </div>
    )
  }

  return (
    <>
      {others.map(user => {
        const isOnline   = onlinePhoneNumbers.includes(user.phoneNumber)
        const isActive   = activeDmPhone === user.phoneNumber

        return (
          <div
            key={user.phoneNumber}
            className={`contact-item ${isActive ? 'active' : ''} group-list-item`}
            onClick={() => onSelectUser(user.phoneNumber)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === 'Enter' && onSelectUser(user.phoneNumber)}
          >
            <Avatar name={user.displayName || user.phoneNumber} size={46} online={isOnline} />

            <div className="contact-info">
              <div className="contact-top" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', minWidth: 0 }}>
                  <span className="contact-name" style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {user.displayName || user.phoneNumber}
                  </span>
                  {unreadCounts[user.phoneNumber] > 0 && (
                    <span className="unread-badge">{unreadCounts[user.phoneNumber]}</span>
                  )}
                </div>
                <span className="contact-time" style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                  {user.phoneNumber}
                </span>
              </div>
              <div className="contact-preview">
                <span className="preview-text" style={{ color: isOnline ? 'var(--success)' : 'var(--text-muted)', fontSize: '0.78rem' }}>
                  {isOnline ? '● Online' : '○ Offline'}
                </span>
                <span style={{
                  background: isOnline ? 'var(--teal)' : 'var(--border)',
                  color: isOnline ? '#000' : 'var(--text-primary)',
                  fontSize: '0.65rem',
                  fontWeight: 700,
                  padding: '1px 6px',
                  borderRadius: 'var(--radius-full)',
                }}>
                  Chat
                </span>
              </div>
            </div>

            {/* Remove DM button */}
            <button
              className="item-remove-btn"
              title="Remove contact & chat history"
              onClick={(e) => {
                e.stopPropagation() // don't open the DM
                onRemoveDm(user.phoneNumber)
              }}
              aria-label={`Remove chat with ${user.displayName || user.phoneNumber}`}
            >
              <svg viewBox="0 0 24 24" width="10" height="10" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>
        )
      })}
    </>
  )
}

export default OnlineUsers
