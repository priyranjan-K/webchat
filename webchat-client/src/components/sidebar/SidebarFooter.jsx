import React from 'react'
import Avatar from '../common/Avatar'

/**
 * SidebarFooter — logged-in user profile + sign-out button.
 */
function SidebarFooter({ displayName, isConnected, onLogout }) {
  const myInitial = displayName?.charAt(0)?.toUpperCase() || '?'

  return (
    <div className="sidebar-footer">
      <div className="user-profile">
        <Avatar name={myInitial} size={36} />
        <div className="user-info">
          <p>{displayName || 'You'}</p>
          <p className="user-status">
            {isConnected ? '● Online' : '○ Reconnecting…'}
          </p>
        </div>
      </div>
      <button id="logout-btn" className="btn-logout" onClick={onLogout} title="Sign out">
        Sign out
      </button>
    </div>
  )
}

export default SidebarFooter
