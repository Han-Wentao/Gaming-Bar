import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { login, sendSms } from "../../api/modules/auth";
import { setAuthState } from "../../store/auth-store";

export function LoginPage() {
  const navigate = useNavigate();
  const [phone, setPhone] = useState("13812345678");
  const [code, setCode] = useState("");
  const [message, setMessage] = useState("请输入手机号并获取验证码后登录。");
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
          <span className="eyebrow">欢迎使用</span>
          <h1>开始你的组局</h1>
          <p>快速进入房间大厅，查看可加入的房间，并和队友保持实时沟通。</p>

          <div className="hero-rail">
            <span className="hero-chip">快速找房</span>
            <span className="hero-chip">即时沟通</span>
            <span className="hero-chip">创建房间</span>
          </div>

          <div className="auth-summary">
            <article className="auth-summary-card">
              <strong>房间大厅</strong>
              <p>随时查看可加入的房间和当前人数。</p>
            </article>
            <article className="auth-summary-card">
              <strong>成员聊天</strong>
              <p>加入房间后即可查看消息并继续沟通。</p>
            </article>
          </div>
        </section>

        <section className="auth-panel panel">
          <div className="hero-copy">
            <span className="eyebrow">短信登录</span>
            <h2>验证码登录</h2>
            <p>登录后将自动验证会话状态，确保访问页面和接口时身份一致。</p>
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
                立即登录
              </button>
            </div>
          </form>

          <p className="feedback">{message}</p>
        </section>
      </div>
    </div>
  );
}
