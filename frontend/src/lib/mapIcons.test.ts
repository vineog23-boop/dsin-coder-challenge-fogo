import { describe, expect, it } from 'vitest'
import { AGENT_STATUSES, AGENT_TYPES } from './api/agents'
import { EQUIPMENT_STATUSES, EQUIPMENT_TYPES } from './api/equipments'
import { AGENT_STATUS_COLOR, EQUIPMENT_STATUS_COLOR, agentGlyph, agentIcon, equipmentGlyph, equipmentIcon } from './mapIcons'

describe('mapIcons', () => {
  it.each(EQUIPMENT_TYPES)('deveDevolverSimboloQuandoTipoE%s', (type) => {
    const glyph = equipmentGlyph(type)

    expect(glyph).toBeTruthy()
    expect(glyph.trim()).not.toBe('')
  })

  it('deveDevolverSimbolosDiferentesQuandoTiposSaoDeGruposDiferentes', () => {
    const radar = equipmentGlyph('RADAR')
    const camera = equipmentGlyph('SURVEILLANCE_CAMERA')
    const scooter = equipmentGlyph('ELECTRIC_SCOOTER')

    expect(new Set([radar, camera, scooter]).size).toBe(3)
  })

  it('deveTerCorQuandoQualquerSituacaoDeEquipamento', () => {
    for (const status of EQUIPMENT_STATUSES) {
      expect(EQUIPMENT_STATUS_COLOR[status]).toMatch(/^#[0-9a-f]{6}$/i)
    }
    expect(EQUIPMENT_STATUS_COLOR.MALFUNCTION).not.toBe(EQUIPMENT_STATUS_COLOR.OPERATIONAL)
  })

  it('deveUsarSimboloECorDaSituacaoQuandoCriaIconeDeEquipamento', () => {
    const icon = equipmentIcon('RADAR', 'MALFUNCTION')

    const html = String(icon.options.html)
    expect(html).toContain(equipmentGlyph('RADAR'))
    expect(html).toContain(EQUIPMENT_STATUS_COLOR.MALFUNCTION)
    expect(html).not.toContain(EQUIPMENT_STATUS_COLOR.OPERATIONAL)
  })

  it('deveUsarCorDaSituacaoQuandoCriaIconeDeAgente', () => {
    for (const status of AGENT_STATUSES) {
      expect(String(agentIcon('TRAFFIC_AGENT', status).options.html)).toContain(AGENT_STATUS_COLOR[status])
    }
    expect(AGENT_STATUS_COLOR.AVAILABLE).not.toBe(AGENT_STATUS_COLOR.ON_OCCURRENCE)
  })

  it('deveUsarSimboloDiferentePorTipoQuandoCriaIconeDeAgente', () => {
    const glyphs = AGENT_TYPES.map((type) => agentGlyph(type))
    expect(new Set(glyphs).size).toBe(AGENT_TYPES.length)
    for (const type of AGENT_TYPES) {
      expect(String(agentIcon(type, 'AVAILABLE').options.html)).toContain(agentGlyph(type))
    }
  })
})
