import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function Layout() {
  const { user, logout } = useAuth()

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="app-header-left">
          <span className="app-title">AutoTEA</span>
          <nav className="app-nav">
            <NavLink to="/" end>
              프로젝트
            </NavLink>
          </nav>
        </div>
        <div className="app-header-right">
          {user?.pictureUrl && <img className="user-avatar" src={user.pictureUrl} alt="" />}
          <span className="user-name">{user?.displayName}</span>
          <button type="button" className="btn btn-ghost" onClick={logout}>
            로그아웃
          </button>
        </div>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  )
}
