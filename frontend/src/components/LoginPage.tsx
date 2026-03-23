import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { authAPI } from '../api/api';
import './LoginPage.css';

export default function LoginPage() {
  const [email, setEmail]       = useState('admin@gmail.com');
  const [password, setPassword] = useState('admin123');
  const [error, setError]       = useState('');
  const [loading, setLoading]   = useState(false);
  const { login } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await authAPI.login(email, password);

      if (response && response.token) {
        login(response.token, {
          name: response.name,
          email: response.email,
          role: response.role as 'ADMIN' | 'USER',
        });
      } else {
        setError('Invalid email or password.');
      }
    } catch (err: unknown) {
      console.error('Login error:', err);
      const msg = err instanceof Error ? err.message : String(err);

      if (msg.toLowerCase().includes('failed to fetch') || msg.toLowerCase().includes('networkerror')) {
        setError('Cannot reach the server. Is the backend running on port 8080?');
      } else if (msg.includes('401') || msg.includes('403')) {
        setError('Wrong email or password.');
      } else {
        setError(`Login failed: ${msg}`);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        {/* Header */}
        <div className="login-header">
          <div className="login-logo">🎯</div>
          <h1>Advanced Task Manager</h1>
          <p>Sign in to your account</p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="login-form">
          {error && <div className="login-error">❌ {error}</div>}

          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="admin@gmail.com"
              required
              autoComplete="email"
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
              autoComplete="current-password"
            />
          </div>

          <button type="submit" className="login-btn" disabled={loading}>
            {loading ? '⏳ Signing in…' : '🔐 Sign In'}
          </button>
        </form>

        {/* Hint */}
        <div className="login-hint">
          <p>🔑 Default Admin credentials:</p>
          <code>admin@gmail.com / admin123</code>
        </div>
      </div>
    </div>
  );
}

