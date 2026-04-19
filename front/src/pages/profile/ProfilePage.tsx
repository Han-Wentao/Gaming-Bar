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
          <span className="eyebrow">Identity</span>
          <h2>个人资料</h2>
          <p>这里强调的是身份观感。除了表单本身，也给用户一个更完整的资料预览区域。</p>
        </div>
        <div className="hero-stats">
          <article className="metric-card">
            <span className="muted">昵称长度</span>
            <strong>{nickname.length}</strong>
            <small>建议 2 到 20 字符</small>
          </article>
          <article className="metric-card">
            <span className="muted">头像链接</span>
            <strong>{avatar ? "已设置" : "空"}</strong>
            <small>支持 http/https</small>
          </article>
          <article className="metric-card">
            <span className="muted">当前账号</span>
            <strong>{auth.user?.id ?? "--"}</strong>
            <small>服务端身份为准</small>
          </article>
        </div>
      </div>

      <section className="profile-layout">
        <article className="panel">
          <div className="section-head">
            <div>
              <h3>资料编辑</h3>
              <p>提交后会同步刷新本地认证缓存，避免资料展示滞后。</p>
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
            <span className="eyebrow">Preview</span>
            <h3>个人卡片预览</h3>
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
              <span>{avatar || "未设置头像 URL"}</span>
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
