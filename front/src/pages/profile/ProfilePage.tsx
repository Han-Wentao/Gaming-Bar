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
        setAuthState({ token: auth.token, user });
      })
      .catch((error: Error) => setMessage(error.message));
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      const user = await updateProfile({ nickname, avatar });
      setAuthState({ token: auth.token, user });
      setMessage("资料已更新。");
    } catch (error) {
      setMessage((error as Error).message);
    }
  }

  return (
    <section className="panel">
      <div className="section-head">
        <div>
          <h2>个人资料</h2>
          <p>昵称 2-20 字符，头像允许空字符串或 http/https 地址。</p>
        </div>
      </div>
      <form className="form-grid" onSubmit={handleSubmit}>
        <label>
          <span>昵称</span>
          <input value={nickname} onChange={(event) => setNickname(event.target.value)} />
        </label>
        <label>
          <span>头像 URL</span>
          <input value={avatar} onChange={(event) => setAvatar(event.target.value)} placeholder="https://..." />
        </label>
        <div className="inline-actions">
          <button type="submit">保存</button>
        </div>
      </form>
      {message ? <p className="feedback">{message}</p> : null}
    </section>
  );
}
