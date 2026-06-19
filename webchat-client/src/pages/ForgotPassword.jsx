import React, { useState, useMemo } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { authService } from '../services/authService'
import '../styles/Auth.css'

function getStrength(pw) {
  if (!pw) return { score: 0, label: '', cls: '' }
  let score = 0
  if (pw.length >= 8)           score++
  if (/[A-Z]/.test(pw))         score++
  if (/[0-9]/.test(pw))         score++
  if (/[^A-Za-z0-9]/.test(pw)) score++
  const map    = ['', 'weak', 'fair', 'good', 'strong']
  const labels = ['', 'Weak', 'Fair', 'Good', 'Strong 🔒']
  return { score, label: labels[score], cls: map[score] }
}

function ForgotPassword() {
  const [phoneNumber, setPhoneNumber] = useState('')
  const [otp, setOtp]                 = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [showPass, setShowPass]       = useState(false)
  const [error, setError]             = useState('')
  const [success, setSuccess]         = useState('')
  const [loading, setLoading]         = useState(false)
  const [step, setStep]               = useState(1) // 1: Request, 2: Reset

  const navigate = useNavigate()
  const strength = useMemo(() => getStrength(newPassword), [newPassword])

  const handleRequestOTP = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)
    try {
      const res = await authService.requestPasswordReset(phoneNumber.trim())
      setSuccess(res.message || 'Verification code sent!')
      setStep(2)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to request password reset code.')
    } finally {
      setLoading(false)
    }
  }

  const handleResetPassword = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    if (strength.score < 2) {
      setError('Please choose a stronger password.')
      return
    }
    setLoading(true)
    try {
      await authService.resetPassword(phoneNumber.trim(), otp.trim(), newPassword)
      setSuccess('Password updated successfully! Redirecting to login…')
      setTimeout(() => {
        navigate('/login')
      }, 2500)
    } catch (err) {
      setError(err.response?.data?.message || 'Password reset failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        {/* Brand Header */}
        <div className="auth-brand">
          <Link to="/home" style={{ textDecoration: 'none' }}>
            <div className="auth-logo">💬</div>
          </Link>
          <h1>Reset Password</h1>
          <p>Retrieve access to your WebChat account</p>
        </div>

        {error && <div className="error-message" role="alert">⚠️ {error}</div>}
        {success && <div className="success-message" role="alert">✓ {success}</div>}

        {step === 1 ? (
          <form onSubmit={handleRequestOTP} className="auth-form" autoComplete="off">
            <div className="field">
              <label htmlFor="reset-phone">Phone Number</label>
              <div className="field-input-wrap">
                <input
                  id="reset-phone"
                  type="tel"
                  placeholder="+91 98765 43210"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  required
                  disabled={loading}
                  autoComplete="tel"
                />
              </div>
            </div>

            <button
              id="reset-request-submit"
              type="submit"
              className="btn-auth"
              disabled={loading || !phoneNumber.trim()}
            >
              {loading ? <><span className="spinner" /> Requesting Code…</> : '→ Send Verification Code'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleResetPassword} className="auth-form" autoComplete="off">
            <div className="field">
              <label htmlFor="reset-otp">Verification Code</label>
              <div className="field-input-wrap">
                <input
                  id="reset-otp"
                  type="text"
                  placeholder="Enter 6-digit code (e.g. 123456)"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value)}
                  required
                  disabled={loading}
                />
              </div>
              <span className="field-hint" style={{ color: 'var(--teal)' }}>
                💡 Tip: Use mock code 123456 for testing.
              </span>
            </div>

            <div className="field">
              <label htmlFor="reset-password">New Password</label>
              <div className="field-input-wrap">
                <input
                  id="reset-password"
                  type={showPass ? 'text' : 'password'}
                  placeholder="Create a strong password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                  disabled={loading}
                  autoComplete="new-password"
                />
                <button
                  type="button"
                  className="eye-toggle"
                  onClick={() => setShowPass(p => !p)}
                  aria-label={showPass ? 'Hide password' : 'Show password'}
                  tabIndex={-1}
                >
                  {showPass ? '🙈' : '👁️'}
                </button>
              </div>
              {newPassword && (
                <>
                  <div className="strength-bar-wrap">
                    {[1,2,3,4].map(i => (
                      <div
                        key={i}
                        className={`strength-segment ${i <= strength.score ? `active-${strength.cls}` : ''}`}
                      />
                    ))}
                  </div>
                  <span className={`strength-label ${strength.cls}`}>{strength.label}</span>
                </>
              )}
            </div>

            <button
              id="reset-submit"
              type="submit"
              className="btn-auth"
              disabled={loading || !otp.trim() || !newPassword}
            >
              {loading ? <><span className="spinner" /> Resetting Password…</> : '→ Confirm New Password'}
            </button>
          </form>
        )}

        <div className="auth-footer">
          Remember your password? <Link to="/login">Sign in</Link>
        </div>
      </div>
    </div>
  )
}

export default ForgotPassword
