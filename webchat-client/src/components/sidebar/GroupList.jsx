import React from 'react'
import Avatar from '../common/Avatar'

/**
 * GroupList — renders the list of chat groups in the sidebar.
 *
 * Each row has an X button that:
 *   - For the group creator  → deletes the group entirely
 *   - For other members      → leaves the group (others keep chatting)
 *
 * @param {Object[]} groups         - array of ChatGroup objects
 * @param {string}   activeGroupId  - currently selected group ID
 * @param {string}   userPhone      - logged-in user's phone number
 * @param {Function} onSelect       - called when a group row is clicked
 * @param {Function} onRemove       - called with (group) when X is clicked
 * @param {Object}   [unreadCounts] - map of unread message counts
 */
function GroupList({ groups, activeGroupId, userPhone, onSelect, onRemove, unreadCounts = {} }) {
  if (groups.length === 0) {
    return (
      <div style={{ padding: '20px 16px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.83rem' }}>
        No groups yet. Create one with ✏️
      </div>
    )
  }

  return (
    <>
      {groups.map(group => {
        const isMine      = group.creator === userPhone
        const memberCount = group.members?.length ?? 0
        const isActive    = activeGroupId === group.groupId

        return (
          <div
            key={group.groupId}
            className={`contact-item ${isActive ? 'active' : ''} group-list-item`}
            onClick={() => onSelect(group)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === 'Enter' && onSelect(group)}
          >
            <Avatar
              name={group.groupName}
              size={46}
              style={{
                background: isMine
                  ? 'linear-gradient(135deg,var(--accent),var(--teal))'
                  : 'linear-gradient(135deg,#4a5568,#718096)',
              }}
            />

            <div className="contact-info">
              <div className="contact-top" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', minWidth: 0 }}>
                  <span className="contact-name" style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {group.groupName}
                  </span>
                  {unreadCounts[group.groupId] > 0 && (
                    <span className="unread-badge">{unreadCounts[group.groupId]}</span>
                  )}
                </div>
              </div>
              <div className="contact-preview">
                <span className="preview-text">
                  👥 {memberCount} member{memberCount !== 1 ? 's' : ''}
                  {isMine ? ' · you created this' : ` · by ${group.creator}`}
                </span>
              </div>
            </div>

            {/* Remove / Leave button */}
            <button
              className="item-remove-btn"
              title={isMine ? 'Delete group' : 'Leave group'}
              onClick={(e) => {
                e.stopPropagation() // don't open the chat
                onRemove(group)
              }}
              aria-label={isMine ? `Delete group ${group.groupName}` : `Leave group ${group.groupName}`}
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

export default GroupList
