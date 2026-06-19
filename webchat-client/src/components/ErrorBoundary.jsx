import React from 'react'

/**
 * ErrorBoundary — catches unhandled render errors in the component tree.
 *
 * Prevents a single component crash from taking down the entire application.
 * Wrap around major subtrees (e.g. Dashboard, route roots) for coverage.
 *
 * Usage:
 * ```jsx
 * <ErrorBoundary>
 *   <MyComponent />
 * </ErrorBoundary>
 * ```
 */
class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, info) {
    // In production, send this to your error tracking service (e.g. Sentry)
    console.error('[ErrorBoundary] Caught error:', error, info)
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null })
    window.location.href = '/'
  }

  render() {
    if (!this.state.hasError) return this.props.children

    return (
      <div style={{
        display: 'flex', flexDirection: 'column', alignItems: 'center',
        justifyContent: 'center', height: '100vh', gap: '16px',
        background: 'var(--bg-primary, #0d1117)', color: 'var(--text-primary, #e6edf3)',
        fontFamily: 'Inter, sans-serif',
      }}>
        <div style={{ fontSize: '3rem' }}>⚠️</div>
        <h2 style={{ margin: 0 }}>Something went wrong</h2>
        <p style={{ color: 'var(--text-secondary, #8b949e)', margin: 0, maxWidth: '400px', textAlign: 'center' }}>
          An unexpected error occurred. Please reload the page.
        </p>
        <button
          onClick={this.handleReset}
          style={{
            marginTop: '8px', padding: '10px 24px',
            background: 'var(--accent, #2563eb)', color: '#fff',
            border: 'none', borderRadius: '8px', cursor: 'pointer',
            fontSize: '0.95rem', fontWeight: 600,
          }}
        >
          Go to Home
        </button>
      </div>
    )
  }
}

export default ErrorBoundary
