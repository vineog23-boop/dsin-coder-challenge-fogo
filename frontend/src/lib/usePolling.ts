import { useEffect, useState } from 'react'
import { ApiError } from './api/client'

export type PollingState<T> = {
  data: T | null
  error: string | null
  /** Instante (ms) da última resposta boa; com erro, os dados anteriores continuam valendo. */
  updatedAt: number | null
}

/**
 * Chama `fetcher` agora e de novo a cada `intervalMs` depois de cada resposta (sem empilhar chamadas).
 * Para com a aba oculta e retoma na hora quando ela volta. `fetcher` deve ser estável (`useCallback`):
 * trocar o fetcher (ex.: filtro novo) reinicia o ciclo.
 */
export function usePolling<T>(fetcher: () => Promise<T>, intervalMs: number): PollingState<T> {
  const [state, setState] = useState<PollingState<T>>({ data: null, error: null, updatedAt: null })

  useEffect(() => {
    let active = true
    let timer: ReturnType<typeof setTimeout> | undefined

    async function run() {
      clearTimeout(timer)
      try {
        const data = await fetcher()
        if (active) setState({ data, error: null, updatedAt: Date.now() })
      } catch (e) {
        if (active) {
          const message = e instanceof ApiError ? e.message : 'Falha ao atualizar.'
          setState((current) => ({ ...current, error: message }))
        }
      } finally {
        if (active && !document.hidden) timer = setTimeout(run, intervalMs)
      }
    }

    function onVisibilityChange() {
      if (document.hidden) {
        clearTimeout(timer)
      } else {
        void run()
      }
    }

    void run()
    document.addEventListener('visibilitychange', onVisibilityChange)
    return () => {
      active = false
      clearTimeout(timer)
      document.removeEventListener('visibilitychange', onVisibilityChange)
    }
  }, [fetcher, intervalMs])

  return state
}
