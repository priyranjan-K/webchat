import React, { useState, useEffect } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Home          from './pages/Home'
import Login         from './pages/Login'
import Signup        from './pages/Signup'
import ForgotPassword from './pages/ForgotPassword'
import Dashboard     from './pages/Dashboard'
import ErrorBoundary from './components/ErrorBoundary'
import { STORAGE_KEYS } from './config/constants'
import './index.css'

/**
 * Redirects unauthenticated users to the login page.
 */
function PrivateRoute({ isAuthenticated, children }) {
  return isAuthenticated ? children : <Navigate to="/login" replace />
}

/**
 * Redirects already-authenticated users away from auth pages (login, signup, etc.).
 */
function PublicRoute({ isAuthenticated, children }) {
  return isAuthenticated ? <Navigate to="/chat" replace /> : children
}

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [loading,         setLoading]         = useState(true)

  useEffect(() => {
    const token = sessionStorage.getItem(STORAGE_KEYS.AUTH_TOKEN)
    setIsAuthenticated(!!token)
    setLoading(false)
  }, [])

  if (loading) {
    return (
      <div className="app-loading">
        <div className="brand-logo">💬</div>
        <p>Loading WebChat…</p>
      </div>
    )
  }

  return (
    <ErrorBoundary>
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <Routes>
          {/* ── Public: home & auth pages ── */}
          <Route path="/home"   element={<Home isAuthenticated={isAuthenticated} />} />
          <Route path="/login"  element={<PublicRoute isAuthenticated={isAuthenticated}><Login  setIsAuthenticated={setIsAuthenticated} /></PublicRoute>} />
          <Route path="/signup" element={<PublicRoute isAuthenticated={isAuthenticated}><Signup setIsAuthenticated={setIsAuthenticated} /></PublicRoute>} />
          <Route path="/forgot-password" element={<PublicRoute isAuthenticated={isAuthenticated}><ForgotPassword /></PublicRoute>} />

          {/* ── Private: chat ── */}
          <Route path="/chat"                    element={<PrivateRoute isAuthenticated={isAuthenticated}><Dashboard setIsAuthenticated={setIsAuthenticated} /></PrivateRoute>} />
          <Route path="/chat/group/:groupId"     element={<PrivateRoute isAuthenticated={isAuthenticated}><Dashboard setIsAuthenticated={setIsAuthenticated} /></PrivateRoute>} />
          <Route path="/chat/dm/:recipientPhone" element={<PrivateRoute isAuthenticated={isAuthenticated}><Dashboard setIsAuthenticated={setIsAuthenticated} /></PrivateRoute>} />

          {/* ── Redirects ── */}
          <Route path="/"  element={<Navigate to={isAuthenticated ? '/chat' : '/login'} replace />} />
          <Route path="*"  element={<Navigate to={isAuthenticated ? '/chat' : '/login'} replace />} />
        </Routes>
      </BrowserRouter>
    </ErrorBoundary>
  )
}

export default App
