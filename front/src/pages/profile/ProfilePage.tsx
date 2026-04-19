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
    <section className="page-stack">
      <div className="page-hero hero-panel">
        <div className="hero-copy">
          <span className="eyebrow">个人资料</span>
          <h2>账号信息</h2>
          <p>在这里修改昵称和头像链接，保存后会立即同步到当前登录状态。</p>
        </div>
        <div className="hero-stats">
          <article className="metric-card">
            <span className="muted">昵称长度</span>
            <strong>{nickname.length}</strong>
            <small>建议 2 到 20 个字符</small>
          </article>
          <article className="metric-card">
            <span className="muted">头像链接</span>
            <strong>{avatar ? "已设置" : "未设置"}</strong>
            <small>支持 http / https</small>
          </article>
          <article className="metric-card">
            <span className="muted">账号 ID</span>
            <strong>{auth.user?.id ?? "--"}</strong>
            <small>以服务端返回为准</small>
          </article>
        </div>
      </div>

      <section className="profile-layout">
        <article className="panel">
          <div className="section-head">
            <div>
              <h3>编辑资料</h3>
              <p>昵称和头像更新后，房间内看到的身份信息也会同步刷新。</p>
            </div>
          </div>

          <form className="form-grid" onSubmit={handleSubmit}>
            <label>
              <span className="label-caption">昵称</span>
              <input value={nickname} onChange={(event) => setNickname(event.target.value)} />
            </label>

            <label>
              <span className="label-caption">头像 URL</span>
              <input value={avatar} onChange={(event) => setAvatar(event.target.value)} placeholder="https://..." />
            </label>

            <div className="inline-actions">
              <button type="submit">保存资料</button>
            </div>
          </form>

          {message ? <p className="feedback">{message}</p> : null}
        </article>

        <aside className="panel">
          <div className="hero-copy">
            <span className="eyebrow">资料预览</span>
            <h3>当前展示</h3>
          </div>

          <ul className="simple-list">
            <li>
              <strong>昵称</strong>
              <span>{nickname || "未填写昵称"}</span>
            </li>
            <li>
              <strong>手机号</strong>
              <span>{auth.user?.phone ?? "未获取"}</span>
            </li>
            <li>
              <strong>头像链接</strong>
              <span>{avatar || "未设置头像链接"}</span>
            </li>
            <li>
              <strong>信用分</strong>
              <span>{auth.user?.credit_score ?? "--"}</span>
            </li>
          </ul>
        </aside>
      </section>
    </section>
  );
}
