import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { login, sendSms } from "../../api/modules/auth";
import { setAuthState } from "../../store/auth-store";

export function LoginPage() {
  const navigate = useNavigate();
  const [phone, setPhone] = useState("13812345678");
  const [code, setCode] = useState("");
  const [message, setMessage] = useState("Enter your phone number, request a code, then sign in.");
  const [loading, setLoading] = useState(false);

  async function handleSendSms() {
    try {
      setLoading(true);
      const result = await sendSms({ phone });
      setMessage(`Verification code sent. It will expire in ${result.expires_in} seconds.`);
    } catch (error) {
      setMessage((error as Error).message);
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      setLoading(true);
      const result = await login({ phone, code });
      setAuthState({ token: result.token, refreshToken: result.refresh_token, user: result.user });
      navigate("/rooms");
    } catch (error) {
      setMessage((error as Error).message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-grid">
        <section className="auth-showcase panel">
          <span className="eyebrow">Welcome</span>
          <h1>Start Your Next Match</h1>
          <p>Browse open rooms, find teammates fast, and switch into realtime chat as soon as you join.</p>

          <div className="hero-rail">
            <span className="hero-chip">Discover Rooms</span>
            <span className="hero-chip">Realtime Chat</span>
            <span className="hero-chip">Create Sessions</span>
          </div>

          <div className="auth-summary">
            <article className="auth-summary-card">
              <strong>Room Lobby</strong>
              <p>See active rooms, owners, and current player counts before joining.</p>
            </article>
            <article className="auth-summary-card">
              <strong>Team Coordination</strong>
              <p>Room members can load history and continue conversations over the realtime channel.</p>
            </article>
          </div>
        </section>

        <section className="auth-panel panel">
          <div className="hero-copy">
            <span className="eyebrow">SMS Sign In</span>
            <h2>Verify And Continue</h2>
            <p>Login now returns both an access token and a refresh token so the session can rotate safely.</p>
          </div>

          <form className="form-grid" onSubmit={handleSubmit}>
            <label>
              <span className="label-caption">Phone</span>
              <input value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="Enter an 11-digit phone number" />
            </label>

            <label>
              <span className="label-caption">Code</span>
              <input value={code} onChange={(event) => setCode(event.target.value)} placeholder="Enter the 6-digit code" />
            </label>

            <div className="auth-actions">
              <button type="button" className="ghost-button" onClick={() => void handleSendSms()} disabled={loading}>
                Send Code
              </button>
              <button type="submit" disabled={loading}>
                Sign In
              </button>
            </div>
          </form>

          <p className="feedback">{message}</p>
        </section>
      </div>
    </div>
  );
}
