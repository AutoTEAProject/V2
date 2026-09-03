import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from './AuthContext'

const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID

export function LoginPage() {
  const { loginWithGoogleIdToken } = useAuth()
  const navigate = useNavigate()
  const buttonRef = useRef<HTMLDivElement>(null)
  const initializedRef = useRef(false)
  const [loginError, setLoginError] = useState<string | null>(null)

  useEffect(() => {
    if (!clientId || initializedRef.current) return

    let cancelled = false

    const setup = () => {
      if (cancelled || !window.google || !buttonRef.current || initializedRef.current) return
      initializedRef.current = true
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: (response) => {
          loginWithGoogleIdToken(response.credential)
            .then(() => navigate('/', { replace: true }))
            .catch((e: unknown) => {
              setLoginError(e instanceof ApiError ? e.message : '로그인에 실패했습니다.')
            })
        },
      })
      window.google.accounts.id.renderButton(buttonRef.current, {
        theme: 'outline',
        size: 'large',
      })
    }

    if (window.google) {
      setup()
      return
    }
    const interval = window.setInterval(() => {
      if (window.google) {
        window.clearInterval(interval)
        setup()
      }
    }, 100)
    return () => {
      cancelled = true
      window.clearInterval(interval)
    }
  }, [loginWithGoogleIdToken, navigate])

  return (
    <div className="login-page">
      <div className="login-card">
        <h1>AutoTEA</h1>
        <p>Google 계정으로 로그인하세요.</p>
        <div ref={buttonRef} className="google-button-slot" />
        {!clientId && (
          <p className="error-text">
            VITE_GOOGLE_CLIENT_ID가 설정되지 않았습니다. frontend/.env를 확인하세요.
          </p>
        )}
        {loginError && <p className="error-text">{loginError}</p>}
      </div>
    </div>
  )
}
