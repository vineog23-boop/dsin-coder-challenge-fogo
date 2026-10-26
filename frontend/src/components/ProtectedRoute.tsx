import { Navigate, Outlet, useLocation } from 'react-router'
import { useSession } from '../lib/session/session'

/** Sem sessão, manda ao login guardando a rota pedida para voltar a ela depois. */
export default function ProtectedRoute() {
  const { session } = useSession()
  const location = useLocation()

  if (!session) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}
