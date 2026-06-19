import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { authService } from '../services/authService'
import { STORAGE_KEYS } from '../config/constants'
import '../styles/Auth.css'

function Login({ setIsAuthenticated }) {
  const [phoneNumber, setPhoneNumber] = useState('')
  const [password, setPassword]       = useState('')
  const [showPass, setShowPass]       = useState(false)
  const [error, setError]             = useState('')
  const [loading, setLoading]         = useState(false)
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await authService.login(phoneNumber, password)
      sessionStorage.setItem(STORAGE_KEYS.AUTH_TOKEN,   res.token)
      sessionStorage.setItem(STORAGE_KEYS.USER_PHONE,   res.phoneNumber   || phoneNumber)
      sessionStorage.setItem(STORAGE_KEYS.DISPLAY_NAME, res.displayName   || 'User')
      setIsAuthenticated(true)
      navigate('/chat')
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed. Check your credentials.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        {/* Brand */}
        <div className="auth-brand">
          <Link to="/home" style={{ textDecoration: 'none' }}>
            <div className="auth-logo">💬</div>
          </Link>
          <h1>WebChat</h1>
          <p>Welcome back — sign in to continue</p>
        </div>

        <form onSubmit={handleSubmit} className="auth-form" autoComplete="off">
          {error && <div className="error-message" role="alert">⚠️ {error}</div>}

          {/* Phone */}
          <div className="field">
            <label htmlFor="login-phone">Phone Number</label>
            <div className="field-input-wrap">
              <input
                id="login-phone"
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
            <label htmlFor="login-password">Password</label>
            <div className="field-input-wrap">
              <input
                id="login-password"
                type={showPass ? 'text' : 'password'}
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                disabled={loading}
                autoComplete="current-password"
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
            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '2px' }}>
              <Link to="/forgot-password" style={{ fontSize: '0.78rem', color: 'var(--accent)', textDecoration: 'none', fontWeight: 500 }}>
                Forgot Password?
              </Link>
            </div>
          </div>

          <button
            id="login-submit"
            type="submit"
            className="btn-auth"
            disabled={loading}
          >
            {loading ? <><span className="spinner" /> Signing in…</> : '→ Sign In'}
          </button>
        </form>

        <div className="auth-footer">
          Don't have an account? <Link to="/signup">Create one</Link>
        </div>
      </div>
    </div>
  )
}

export default Login
