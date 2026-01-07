import { get } from '../utils/request'

export interface DashboardStats {
  agentCount: number
  knowledgeBaseCount: number
  workflowCount: number
  pluginCount: number
}

export const getDashboardStats = () => {
  return get<DashboardStats>('/dashboard/stats')
}
