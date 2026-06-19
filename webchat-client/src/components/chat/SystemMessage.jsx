import React from 'react'

/**
 * SystemMessage — inline notice for join/leave/server events.
 */
function SystemMessage({ content }) {
  return (
    <div style={{
      textAlign:  'center',
      fontSize:   '0.73rem',
      color:      'var(--text-muted)',
      padding:    '3px 0',
      fontStyle:  'italic',
    }}>
      {content}
    </div>
  )
}

export default SystemMessage
