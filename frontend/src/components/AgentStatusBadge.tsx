import type { AgentStatus } from '../lib/api/agents'
import { AGENT_STATUS_LABEL } from '../lib/labels'

export default function AgentStatusBadge({ status }: { status: AgentStatus }) {
  return <span className={`badge badge-agent-${status.toLowerCase()}`}>{AGENT_STATUS_LABEL[status]}</span>
}
