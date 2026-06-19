import React from 'react'

/**
 * MessageBubble — renders a single chat message
 * @param {string}  content   - message text
 * @param {boolean} sent      - true = from current user
 * @param {string}  time      - display time string
 * @param {string}  senderInitial - first letter for recv avatar
 * @param {boolean} read      - show double blue tick
 */
function MessageBubble({ content, sent, time, senderInitial = '?', read = false }) {
  return (
    <div className={`msg-row ${sent ? 'sent' : 'recv'}`}>
      {!sent && (
        <div className="msg-avatar">{senderInitial}</div>
      )}
      <div className="msg-bubble-wrap">
        <div className="msg-bubble">{content}</div>
        <div className="msg-meta">
          <span>{time}</span>
          {sent && (
            <span className="msg-tick">{read ? '✓✓' : '✓'}</span>
          )}
        </div>
      </div>
    </div>
  )
}

export default MessageBubble
