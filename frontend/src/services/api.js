import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
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

  // Get payment history
  getPaymentHistory: (id) => apiClient.get(`/payments/${id}/history`),

  // Get payment validations
  getPaymentValidations: (id) => apiClient.get(`/payments/${id}/validations`),

  // Get complete audit trail
  getPaymentAudit: (id) => apiClient.get(`/payments/${id}/audit`),
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

// Health API
export const healthAPI = {
  // Get health status
  getHealth: () => apiClient.get('/actuator/health/liveness'),

  // Get readiness
  getReadiness: () => apiClient.get('/actuator/health/readiness'),

  // Get info
  getInfo: () => apiClient.get('/actuator/info'),
};

export default apiClient;

