import { FormEvent, useEffect, useState } from "react";
import { getMe } from "../../api/modules/auth";
import { updateProfile } from "../../api/modules/user";
import { setAuthState, useAuthState } from "../../store/auth-store";

export function ProfilePage() {
  const auth = useAuthState();
  const [nickname, setNickname] = useState(auth.user?.nickname ?? "");
  const [avatar, setAvatar] = useState(auth.user?.avatar ?? "");
  const [message, setMessage] = useState("");

  useEffect(() => {
    getMe()
      .then((user) => {
        setNickname(user.nickname);
        setAvatar(user.avatar);
        setAuthState({ token: auth.token, refreshToken: auth.refreshToken, user });
      })
      .catch((error: Error) => setMessage(error.message));
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      const user = await updateProfile({ nickname, avatar });
      setAuthState({ token: auth.token, refreshToken: auth.refreshToken, user });
      setMessage("Profile updated.");
    } catch (error) {
      setMessage((error as Error).message);
    }
  }

  return (
    <section className="page-stack">
      <div className="page-hero hero-panel">
        <div className="hero-copy">
          <span className="eyebrow">Profile</span>
          <h2>Account Details</h2>
          <p>Changes here sync back into the active session and are reflected anywhere your profile is shown.</p>
        </div>
        <div className="hero-stats">
          <article className="metric-card">
            <span className="muted">Nickname Length</span>
            <strong>{nickname.length}</strong>
            <small>Recommended: 2 to 20 characters.</small>
          </article>
          <article className="metric-card">
            <span className="muted">Avatar</span>
            <strong>{avatar ? "Configured" : "Not Set"}</strong>
            <small>Supports `http` and `https` links.</small>
          </article>
          <article className="metric-card">
            <span className="muted">Account ID</span>
            <strong>{auth.user?.id ?? "--"}</strong>
            <small>Uses the latest value returned by the backend.</small>
          </article>
        </div>
      </div>

      <section className="profile-layout">
        <article className="panel">
          <div className="section-head">
            <div>
              <h3>Edit Profile</h3>
              <p>Nickname and avatar changes will appear in room membership and chat history views.</p>
            </div>
          </div>

          <form className="form-grid" onSubmit={handleSubmit}>
            <label>
              <span className="label-caption">Nickname</span>
              <input value={nickname} onChange={(event) => setNickname(event.target.value)} />
            </label>

            <label>
              <span className="label-caption">Avatar URL</span>
              <input value={avatar} onChange={(event) => setAvatar(event.target.value)} placeholder="https://..." />
            </label>

            <div className="inline-actions">
              <button type="submit">Save Changes</button>
            </div>
          </form>

          {message ? <p className="feedback">{message}</p> : null}
        </article>

        <aside className="panel">
          <div className="hero-copy">
            <span className="eyebrow">Preview</span>
            <h3>Current Snapshot</h3>
          </div>

          <ul className="simple-list">
            <li>
              <strong>Nickname</strong>
              <span>{nickname || "Not provided"}</span>
            </li>
            <li>
              <strong>Phone</strong>
              <span>{auth.user?.phone ?? "Unavailable"}</span>
            </li>
            <li>
              <strong>Avatar URL</strong>
              <span>{avatar || "Not provided"}</span>
            </li>
            <li>
              <strong>Credit Score</strong>
              <span>{auth.user?.credit_score ?? "--"}</span>
            </li>
          </ul>
        </aside>
      </section>
    </section>
  );
}
