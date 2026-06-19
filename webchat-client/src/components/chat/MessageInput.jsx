import React, { useRef } from 'react'

/**
 * MessageInput — the input bar at the bottom of the chat panel.
 * Auto-resizes textarea. Enter = send, Shift+Enter = newline.
 */
function MessageInput({ value, onChange, onSend, placeholder, disabled }) {
  const textareaRef = useRef(null)

  const handleChange = (e) => {
    onChange(e.target.value)
    const el = textareaRef.current
    if (el) { el.style.height = 'auto'; el.style.height = el.scrollHeight + 'px' }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      onSend()
    }
  }

  return (
    <div className="chat-input-bar">
      <div className="input-actions">
        <button className="icon-btn" title="Attach file">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" />
          </svg>
        </button>
        <button className="icon-btn" title="Emoji">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="10" />
            <path d="M8 14s1.5 2 4 2 4-2 4-2" />
            <line x1="9" y1="9" x2="9.01" y2="9" />
            <line x1="15" y1="9" x2="15.01" y2="9" />
          </svg>
        </button>
      </div>

      <div className="msg-input-wrap">
        <textarea
          ref={textareaRef}
          id="msg-input"
          className="msg-input"
          placeholder={placeholder}
          value={value}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          rows={1}
          disabled={disabled}
        />
      </div>

      <button
        id="send-btn"
        className="btn-send"
        onClick={onSend}
        title="Send"
        disabled={!value.trim() || disabled}
      >
        <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
          <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
        </svg>
      </button>
    </div>
  )
}

export default MessageInput
