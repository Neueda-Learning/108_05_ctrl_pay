import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

const ROOT_API_BASE_URL = (process.env.REACT_APP_API_URL || 'http://localhost:8080/api').replace(/\/api$/, '');
const rootApiClient = axios.create({
  baseURL: ROOT_API_BASE_URL,
});

const FRAUD_API_BASE_URL = process.env.REACT_APP_FRAUD_API_URL || 'http://localhost:5000';
const fraudApiClient = axios.create({
  baseURL: FRAUD_API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Payment API
export const paymentAPI = {
  // Create payment
  createPayment: (data) => apiClient.post('/payments', data),

  // Get single payment
  getPayment: (id) => apiClient.get(`/payments/${id}`),

  // List payments with filters
  listPayments: (params) => apiClient.get('/payments', { params }),

  // Validate payment
  validatePayment: (id) => apiClient.post(`/payments/${id}/validate`),

  // Send payment
  sendPayment: (id) => apiClient.post(`/payments/${id}/send`),

  // Complete payment
  completePayment: (id) => apiClient.post(`/payments/${id}/complete`),

  // Fail payment
  failPayment: (id, data) => apiClient.post(`/payments/${id}/fail`, data),

  // Get payment status history (audit trail)
  getPaymentHistory: (id) => apiClient.get(`/payments/${id}/audit/status-history`),

  // Get payment validations
  getPaymentValidations: (id) => apiClient.get(`/payments/${id}/audit/validations`),

  // Get complete audit trail
  getPaymentAudit: (id) => apiClient.get(`/payments/${id}/audit`),

  // Download payment receipt PDF for an authorized customer
  downloadReceipt: (customerId, id) => apiClient.get(
    `/customers/${customerId}/payments/${id}/receipt`,
    { responseType: 'blob' }
  ),
};

// Validation Rules API
export const rulesAPI = {
  // List rules
  listRules: () => apiClient.get('/admin/validation-rules'),

  // Get rule details
  getRule: (id) => apiClient.get(`/admin/validation-rules/${id}`),

  // Create rule
  createRule: (data) => apiClient.post('/admin/validation-rules', data),

  // Update rule
  updateRule: (id, data) => apiClient.put(`/admin/validation-rules/${id}`, data),

  // Toggle rule status
  toggleRule: (id) => apiClient.patch(`/admin/validation-rules/${id}/toggle`),

  // Test rule
  testRule: (id, data) => apiClient.post(`/admin/validation-rules/${id}/test-dry-run`, data),
};

// Analytics API
export const analyticsAPI = {
  // Get success rate
  getSuccessRate: () => apiClient.get('/analytics/success-rate'),

  // Get status distribution
  getStatusDistribution: () => apiClient.get('/analytics/status-distribution'),

  // Get volume statistics
  getVolume: () => apiClient.get('/analytics/volume'),

  // Get trend data
  getTrends: () => apiClient.get('/analytics/trends'),
};

// Customer API
export const customerAPI = {
  createCustomer: (data) => apiClient.post('/customers', data),
  getCustomer: (customerId) => apiClient.get(`/customers/${customerId}`),
  getCustomerPayments: (customerId, params) => apiClient.get(`/customers/${customerId}/payments`, { params }),
  getCustomerStats: (customerId, params) => apiClient.get(`/customers/${customerId}/statistics`, { params }),
};

// Account API
export const accountAPI = {
  createAccount: (customerId, data) => apiClient.post(`/customers/${customerId}/accounts`, data),
  listAccountsByCustomer: (customerId) => apiClient.get(`/customers/${customerId}/accounts`),
  getAccountById: (accountId) => apiClient.get(`/accounts/${accountId}`),
  getAccountByNumber: (accountNumber) => apiClient.get(`/accounts/by-number/${accountNumber}`),
};

// Currency API
export const currencyAPI = {
  getCurrencies: () => apiClient.get('/currencies'),
  convert: (data) => apiClient.post('/convert', data),
};

// Fraud API (ML service)
export const fraudAPI = {
  predict: (data) => fraudApiClient.post('/predict-json', data),
};

// Admin Fraud Dashboard API
export const adminFraudAPI = {
  // Get suspicious transactions pending review
  getSuspiciousTransactions: (params) => apiClient.get('/admin/fraud/transactions', { params }),

  // Get detailed fraud investigation for a payment
  getPaymentFraudDetail: (id) => apiClient.get(`/admin/fraud/payment/${id}`),

  // Approve a suspicious payment
  approvePayment: (id, data) => apiClient.post(`/admin/fraud/payment/${id}/approve`, data),

  // Reject a suspicious payment
  rejectPayment: (id, data) => apiClient.post(`/admin/fraud/payment/${id}/reject`, data),

  // Get fraud statistics
  getStats: () => apiClient.get('/admin/fraud/stats'),

  // Get pending review queue
  getPending: (params) => apiClient.get('/admin/fraud/pending', { params }),

  // Refresh account risk status
  refreshAccountRisk: (accountNumber) => apiClient.post(`/admin/fraud/account/${accountNumber}/refresh-risk`),
};


// Health API
export const healthAPI = {
  // Get health status
  getHealth: () => rootApiClient.get('/actuator/health/liveness'),

  // Get readiness
  getReadiness: () => rootApiClient.get('/actuator/health/readiness'),

  // Get info
  getInfo: () => rootApiClient.get('/actuator/info'),
};

export default apiClient;
