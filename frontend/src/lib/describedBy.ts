/** Id do texto que descreve o campo: o erro, se houver; senão a dica. Combina com os ids do `Field`. */
export function describedBy(id: string, error?: string, hint?: string): string | undefined {
  if (error) return `${id}-error`
  return hint ? `${id}-hint` : undefined
}
