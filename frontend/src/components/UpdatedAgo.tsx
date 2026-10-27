import { useEffect, useState } from 'react'

/** "atualizado há X s", contado a cada segundo a partir da última resposta boa. */
export default function UpdatedAgo({ updatedAt }: { updatedAt: number | null }) {
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [])

  if (updatedAt === null) return null
  const seconds = Math.max(0, Math.round((now - updatedAt) / 1000))
  return <span className="muted">atualizado há {seconds} s</span>
}
