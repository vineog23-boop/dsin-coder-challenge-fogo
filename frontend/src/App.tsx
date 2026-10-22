import { Navigate, NavLink, Outlet, Route, Routes } from 'react-router'
import AgentListPage from './pages/AgentListPage'
import EquipmentListPage from './pages/EquipmentListPage'

function Layout() {
  return (
    <>
      <header className="app-header">
        <span className="app-brand">F.O.G.O.</span>
        <nav aria-label="Principal">
          <NavLink to="/equipamentos">Equipamentos</NavLink>
          <NavLink to="/agentes">Agentes</NavLink>
        </nav>
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
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/equipamentos" replace />} />
        <Route path="equipamentos" element={<EquipmentListPage />} />
        <Route path="agentes" element={<AgentListPage />} />
        <Route path="*" element={<Navigate to="/equipamentos" replace />} />
      </Route>
    </Routes>
  )
}
