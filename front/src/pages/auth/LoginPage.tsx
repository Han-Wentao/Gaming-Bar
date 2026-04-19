import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { login, sendSms } from "../../api/modules/auth";
import { setAuthState } from "../../store/auth-store";

export function LoginPage() {
  const navigate = useNavigate();
  const [phone, setPhone] = useState("13812345678");
  const [code, setCode] = useState("");
  const [message, setMessage] = useState("输入手机号后先发送验证码，再完成登录。");
  const [loading, setLoading] = useState(false);

  async function handleSendSms() {
    try {
      setLoading(true);
      const result = await sendSms({ phone });
      setMessage(`验证码已发送，有效期 ${result.expires_in} 秒。`);
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
      setAuthState({ token: result.token, user: result.user });
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
          <span className="eyebrow">GamingBar Entrance</span>
          <h1>不是一张普通登录页，而是组局入口。</h1>
          <p>
            从验证码登录开始，整个界面就切换到更偏「实时房间控制台」的气质。
            重点是让用户一眼看懂状态，也愿意继续往下点。
          </p>

          <div className="showcase-grid">
            <article className="showcase-card">
              <h3>即时房间</h3>
              <p>快速判断空位、状态和当前是否已经在房内。</p>
            </article>
            <article className="showcase-card">
              <h3>动态反馈</h3>
              <p>按钮、卡片、列表和聊天区都带有轻量过渡与悬浮节奏。</p>
            </article>
            <article className="showcase-card">
              <h3>更强身份感</h3>
              <p>登录、认证、成员角色和个人资料都做了更清晰的视觉区分。</p>
            </article>
            <article className="showcase-card">
              <h3>低依赖实现</h3>
              <p>全部基于现有 React 和 CSS，不额外引入 UI 包。</p>
            </article>
          </div>
        </section>

        <section className="auth-panel panel">
          <div className="hero-copy">
            <span className="eyebrow">Secure Sign In</span>
            <h2>短信验证码登录</h2>
            <p>前端会在启动时回源校验登录态，伪造本地存储已经不能直接穿透进入页面。</p>
          </div>

          <form className="form-grid" onSubmit={handleSubmit}>
            <label>
              <span className="label-caption">手机号</span>
              <input value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="请输入 11 位手机号" />
            </label>

            <label>
              <span className="label-caption">验证码</span>
              <input value={code} onChange={(event) => setCode(event.target.value)} placeholder="请输入 6 位验证码" />
            </label>

            <div className="auth-actions">
              <button type="button" className="ghost-button" onClick={handleSendSms} disabled={loading}>
                发送验证码
              </button>
              <button type="submit" disabled={loading}>
                登录进入
              </button>
            </div>
          </form>

          <p className="feedback">{message}</p>
        </section>
      </div>
    </div>
  );
}
