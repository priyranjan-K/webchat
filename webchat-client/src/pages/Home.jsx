import React from 'react'
import { Link, Navigate } from 'react-router-dom'
import '../styles/Home.css'

function Home({ isAuthenticated }) {
  // If already authenticated, redirect directly to the chat dashboard
  if (isAuthenticated) {
    return <Navigate to="/chat" replace />
  }

  return (
    <div className="home-page">
      {/* Glow Blobs */}
      <div className="home-glow home-glow-1"></div>
      <div className="home-glow home-glow-2"></div>

      <header className="home-header">
        <div className="home-brand">
          <span className="brand-logo">💬</span>
          <span className="brand-name">WebChat</span>
        </div>
        <nav className="home-nav">
          <Link to="/login" className="nav-btn nav-login">Sign In</Link>
          <Link to="/signup" className="nav-btn nav-signup btn-glow">Get Started</Link>
        </nav>
      </header>

      <main className="home-hero-section">
        <div className="hero-content">
          <h1 className="hero-title">
            Connect Instantly. <br />
            <span className="accent-text">Chat Securely.</span>
          </h1>
          <p className="hero-subtitle">
            A state-of-the-art real-time messaging application designed for fast, seamless, and reliable communication.
          </p>
          <div className="hero-actions">
            <Link to="/signup" className="hero-btn btn-primary">Start Chatting Now</Link>
            <Link to="/login" className="hero-btn btn-secondary">Have an account? Login</Link>
          </div>
        </div>

        <div className="features-grid">
          <div className="feature-card">
            <div className="feature-icon">⚡</div>
            <h3>Real-time Delivery</h3>
            <p>Experience zero-latency communication powered by ultra-fast WebSockets.</p>
          </div>
          <div className="feature-card">
            <div className="feature-icon">🔒</div>
            <h3>Stateful JWT Security</h3>
            <p>Enterprise-grade security with session-revocation on the database during logout.</p>
          </div>
          <div className="feature-card">
            <div className="feature-icon">👥</div>
            <h3>Contact Search</h3>
            <p>Add friends by phone number, message contacts directly, and see live connection states.</p>
          </div>
        </div>
      </main>

      <footer className="home-footer">
        <p>&copy; {new Date().getFullYear()} WebChat. Created with love &amp; premium aesthetics.</p>
      </footer>
    </div>
  )
}

export default Home
