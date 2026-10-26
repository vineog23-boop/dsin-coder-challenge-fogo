export type CepAddress = {
  street: string
  neighborhood: string
  city: string
  state: string
}

/**
 * Consulta o ViaCEP direto do navegador (o serviço libera CORS). Devolve `null` se o CEP não existe
 * ou o serviço falhar: o preenchimento manual continua valendo.
 */
export async function lookupCep(zipCode: string): Promise<CepAddress | null> {
  const digits = zipCode.replace(/\D/g, '')
  if (digits.length !== 8) return null
  try {
    const response = await fetch(`https://viacep.com.br/ws/${digits}/json/`)
    if (!response.ok) return null
    const data = (await response.json()) as {
      erro?: boolean | string
      logradouro?: string
      bairro?: string
      localidade?: string
      uf?: string
    }
    if (data.erro) return null
    return {
      street: data.logradouro ?? '',
      neighborhood: data.bairro ?? '',
      city: data.localidade ?? '',
      state: data.uf ?? '',
    }
  } catch {
    return null
  }
}
