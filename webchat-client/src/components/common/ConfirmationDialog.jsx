import React, { useEffect, useRef } from 'react'
import '../../styles/ConfirmationDialog.css'

function ConfirmationDialog({
  isOpen,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  type = 'danger', // 'danger' | 'warning' | 'info' | 'success'
  isAlert = false, // if true, only show one button (e.g. replacing window.alert)
  onConfirm,
  onCancel,
}) {
  const modalRef = useRef(null)

  // Keydown and focus management
  useEffect(() => {
    if (!isOpen) return

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        if (onCancel) {
          onCancel()
        } else {
          onConfirm()
        }
      } else if (e.key === 'Enter') {
        onConfirm()
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    
    // Set focus on the dialog box itself to prevent focus leaking
    if (modalRef.current) {
      modalRef.current.focus()
    }

    return () => {
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [isOpen, onConfirm, onCancel])

  if (!isOpen) return null

  // Retrieve matching icon
  const getIcon = () => {
    switch (type) {
      case 'danger':
        return <span className="dialog-icon-wrapper icon-danger">⚠️</span>
      case 'warning':
        return <span className="dialog-icon-wrapper icon-warning">🔔</span>
      case 'success':
        return <span className="dialog-icon-wrapper icon-success">✓</span>
      case 'info':
      default:
        return <span className="dialog-icon-wrapper icon-info">ℹ️</span>
    }
  }

  const handleBackdropClick = (e) => {
    if (e.target.classList.contains('dialog-backdrop')) {
      if (onCancel) {
        onCancel()
      } else {
        onConfirm()
      }
    }
  }

  return (
    <div className="dialog-backdrop" onClick={handleBackdropClick} role="presentation">
      <div
        className={`dialog-box type-${type}`}
        ref={modalRef}
        tabIndex="-1"
        role="dialog"
        aria-modal="true"
        aria-labelledby="dialog-title"
        aria-describedby="dialog-description"
      >
        <div className="dialog-header">
          {getIcon()}
          <h3 id="dialog-title" className="dialog-title">{title}</h3>
        </div>
        <div className="dialog-body">
          <p id="dialog-description" className="dialog-message">{message}</p>
        </div>
        <div className="dialog-footer">
          {!isAlert && (
            <button 
              type="button" 
              className="dialog-btn btn-cancel" 
              onClick={onCancel}
              aria-label={cancelLabel}
            >
              {cancelLabel}
            </button>
          )}
          <button 
            type="button" 
            className={`dialog-btn btn-confirm btn-${type}`} 
            onClick={onConfirm}
            aria-label={confirmLabel}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}

export default ConfirmationDialog
