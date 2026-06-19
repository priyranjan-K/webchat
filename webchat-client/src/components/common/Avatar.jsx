import React from 'react'

/**
 * Avatar — reusable circular avatar showing the first letter of a name.
 */
function Avatar({ name = '?', size = 46, online = false, style = {} }) {
  const initial = String(name).charAt(0).toUpperCase()

  return (
    <div className="avatar" style={{ flexShrink: 0 }}>
      <div
        className="avatar-img"
        style={{ width: size, height: size, fontSize: size * 0.38, ...style }}
        aria-label={name}
      >
        {initial}
      </div>
      {online && <div className="online-dot" />}
    </div>
  )
}

export default Avatar
