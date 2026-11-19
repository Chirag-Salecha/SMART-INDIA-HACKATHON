import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import './Login.css'

export default function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = (e) => {
    e.preventDefault()
    // Mock login for the MVP milestone - any non-empty credentials are accepted.
    // Real authentication will be added once the backend supports it.
    login(username || 'officer.sharma')
    navigate('/dashboard')
  }

  return (
    <div className="login-screen">
      <div className="login-card">
        <div className="login-mark" aria-hidden="true">
          <svg width="34" height="34" viewBox="0 0 34 34" fill="none">
            <rect x="1" y="1" width="32" height="32" stroke="#b78a2e" strokeWidth="1.5" />
            <path d="M9 22V12l8-5 8 5v10" stroke="#101c31" strokeWidth="1.5" fill="none" />
            <path d="M9 22h16" stroke="#101c31" strokeWidth="1.5" />
            <path d="M13 22v-6M17 22v-6M21 22v-6" stroke="#101c31" strokeWidth="1.5" />
          </svg>
        </div>

        <p className="login-eyebrow">Smart Automation &middot; SIH 2026</p>
        <h1 className="login-title">Bid Compliance Verification Platform</h1>
        <p className="login-subtitle">
          Sign in to review bidder submissions and compliance status.
        </p>

        <form className="login-form" onSubmit={handleSubmit}>
          <label className="login-field">
            <span>Officer ID</span>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="officer.sharma"
              autoComplete="username"
            />
          </label>

          <label className="login-field">
            <span>Password</span>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              autoComplete="current-password"
            />
          </label>

          <button type="submit" className="login-submit">
            Sign in
          </button>
        </form>

        <p className="login-note">
          Demo build — enter any Officer ID to continue. Authentication will be
          enforced in a later milestone.
        </p>
      </div>
    </div>
  )
}
