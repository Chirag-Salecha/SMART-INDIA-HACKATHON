// Mirrors the enums defined on the backend (com.sih.bidcompliance.entity).
// Kept here as plain arrays so form <select> inputs stay in sync with the API.

export const TENDER_STATUSES = ['DRAFT', 'OPEN', 'UNDER_REVIEW', 'COMPLETED']

export const DOCUMENT_TYPES = [
  'GST',
  'PAN',
  'UDYAM',
  'INCOME_TAX',
  'OEM_AUTHORIZATION',
  'MAKE_IN_INDIA',
  'EPFO',
  'ESIC',
  'STARTUP_INDIA',
  'NSIC',
  'OTHER',
]
