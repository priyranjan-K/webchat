import React, { useState, useMemo } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { authService } from '../services/authService'
import { STORAGE_KEYS } from '../config/constants'
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

function Signup({ setIsAuthenticated }) {
  const [displayName, setDisplayName] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [password,    setPassword]    = useState('')
  const [confirmPw,   setConfirmPw]   = useState('')
  const [showPass,    setShowPass]    = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)
  const [error,       setError]       = useState('')
  const [loading,     setLoading]     = useState(false)
  const navigate = useNavigate()

  const strength = useMemo(() => getStrength(password), [password])
  const pwMatch  = confirmPw && password !== confirmPw

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (password !== confirmPw) { setError('Passwords do not match.'); return }
    if (strength.score < 2)     { setError('Please choose a stronger password.'); return }
    setError('')
    setLoading(true)
    try {
      const res = await authService.signup(phoneNumber, password, displayName)
      sessionStorage.setItem(STORAGE_KEYS.AUTH_TOKEN,  res.token)
      sessionStorage.setItem(STORAGE_KEYS.USER_PHONE,  res.phoneNumber  || phoneNumber)
      sessionStorage.setItem(STORAGE_KEYS.DISPLAY_NAME, res.displayName  || displayName)
      setIsAuthenticated(true)
      navigate('/chat')
    } catch (err) {
      setError(err.response?.data?.message || 'Signup failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      {/* Slightly tighter card just for signup — fits 4 fields in viewport */}
      <div className="auth-card" style={{ padding: '28px 36px 24px' }}>
        {/* Horizontal brand — saves ~70px vs stacked logo */}
        <div className="auth-brand compact">
          <Link to="/home" style={{ textDecoration: 'none' }}>
            <div className="auth-logo">💬</div>
          </Link>
          <div className="brand-text">
            <h1>WebChat</h1>
            <p>Create your account in seconds</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="auth-form compact-form" autoComplete="off">
          {error && <div className="error-message" role="alert">⚠️ {error}</div>}

          {/* Display Name */}
          <div className="field">
            <label htmlFor="signup-name">Display Name</label>
            <div className="field-input-wrap">
              <input
                id="signup-name"
                type="text"
                placeholder="How should we call you?"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                required
                disabled={loading}
                autoComplete="name"
              />
            </div>
          </div>

          {/* Phone */}
          <div className="field">
            <label htmlFor="signup-phone">Phone Number</label>
            <div className="field-input-wrap">
              <input
                id="signup-phone"
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

          {/* Password */}
          <div className="field">
            <label htmlFor="signup-password">Password</label>
            <div className="field-input-wrap">
              <input
                id="signup-password"
                type={showPass ? 'text' : 'password'}
                placeholder="Create a strong password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
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
            {password && (
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

          {/* Confirm Password */}
          <div className="field">
            <label htmlFor="signup-confirm">Confirm Password</label>
            <div className="field-input-wrap">
              <input
                id="signup-confirm"
                type={showConfirm ? 'text' : 'password'}
                placeholder="Repeat your password"
                value={confirmPw}
                onChange={(e) => setConfirmPw(e.target.value)}
                required
                disabled={loading}
                autoComplete="new-password"
                style={pwMatch ? { borderColor: 'var(--error)' } : {}}
              />
              <button
                type="button"
                className="eye-toggle"
                onClick={() => setShowConfirm(p => !p)}
                aria-label="Toggle confirm password visibility"
                tabIndex={-1}
              >
                {showConfirm ? '🙈' : '👁️'}
              </button>
            </div>
            {pwMatch && <span className="field-hint">✗ Passwords don't match</span>}
          </div>

          <button
            id="signup-submit"
            type="submit"
            className="btn-auth"
            disabled={loading || !!pwMatch}
          >
            {loading ? <><span className="spinner" /> Creating account…</> : '→ Create Account'}
          </button>
        </form>

        <div className="auth-footer compact-footer">
          Already have an account? <Link to="/login">Sign in</Link>
        </div>
      </div>
    </div>
  )
}

export default Signup
