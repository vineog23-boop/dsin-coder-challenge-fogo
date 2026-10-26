import { Navigate, NavLink, Outlet, Route, Routes } from 'react-router'
import ProtectedRoute from './components/ProtectedRoute'
import { useSession } from './lib/session/session'
import AgentListPage from './pages/AgentListPage'
import EquipmentDetailPage from './pages/EquipmentDetailPage'
import EquipmentFormPage from './pages/EquipmentFormPage'
import EquipmentListPage from './pages/EquipmentListPage'
import LoginPage from './pages/LoginPage'

const ROLE_LABEL = { MANAGER: 'Gestor', OPERATOR: 'Operador' } as const

function Layout() {
  const { session, logout } = useSession()

  return (
    <>
      <header className="app-header">
        <span className="app-brand">F.O.G.O.</span>
        <nav aria-label="Principal">
          <NavLink to="/equipamentos">Equipamentos</NavLink>
          <NavLink to="/agentes">Agentes</NavLink>
        </nav>
        {session && (
          <div className="app-user">
            <span>
              {session.username} · {ROLE_LABEL[session.role]}
            </span>
            <button type="button" onClick={logout}>
              Sair
            </button>
          </div>
        )}
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route index element={<Navigate to="/equipamentos" replace />} />
          <Route path="equipamentos" element={<EquipmentListPage />} />
          <Route path="equipamentos/novo" element={<EquipmentFormPage />} />
          <Route path="equipamentos/:id" element={<EquipmentDetailPage />} />
          <Route path="equipamentos/:id/editar" element={<EquipmentFormPage />} />
          <Route path="agentes" element={<AgentListPage />} />
          <Route path="*" element={<Navigate to="/equipamentos" replace />} />
        </Route>
      </Route>
    </Routes>
  )
}
