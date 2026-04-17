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
      <section className="panel auth-panel">
        <div>
          <h1>短信验证码登录</h1>
          <p>后端默认不返回验证码，本页面用于直接联调真实登录流程。</p>
        </div>
        <form className="form-grid" onSubmit={handleSubmit}>
          <label>
            <span>手机号</span>
            <input value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="11 位手机号" />
          </label>
          <label>
            <span>验证码</span>
            <input value={code} onChange={(event) => setCode(event.target.value)} placeholder="6 位验证码" />
          </label>
          <div className="inline-actions">
            <button type="button" onClick={handleSendSms} disabled={loading}>
              发送验证码
            </button>
            <button type="submit" disabled={loading}>
              登录
            </button>
          </div>
        </form>
        <p className="feedback">{message}</p>
      </section>
    </div>
  );
}
