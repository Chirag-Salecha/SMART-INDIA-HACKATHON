import { apiClient } from './client.js'

export const getDashboardStats = () => apiClient.get('/dashboard/stats').then((res) => res.data)

export const getDashboardComplianceStats = () =>
  apiClient.get('/dashboard/compliance-stats').then((res) => res.data)
