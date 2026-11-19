import { apiClient } from './client.js'

export const getComplianceAssessment = (bidderId) =>
  apiClient.get(`/bidders/${bidderId}/compliance`).then((res) => res.data)

export const getRiskAssessment = (bidderId) =>
  apiClient.get(`/bidders/${bidderId}/risk`).then((res) => res.data)

export const getBidderAuditTrail = (bidderId) =>
  apiClient.get(`/bidders/${bidderId}/audit`).then((res) => res.data)
