import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

export const dashboardAPI = {
  /**
   * Get platform overview dashboard statistics
   */
  getOverview: async () => {
    const response = await axios.get(`${API_BASE_URL}/dashboard/overview`);
    return response.data;
  },

  /**
   * Get transaction dashboard statistics
   */
  getTransactions: async () => {
    const response = await axios.get(`${API_BASE_URL}/dashboard/transactions`);
    return response.data;
  },

  /**
   * Get fraud and risk dashboard statistics
   */
  getFraud: async () => {
    const response = await axios.get(`${API_BASE_URL}/dashboard/fraud`);
    return response.data;
  },

  /**
   * Get customer analytics dashboard statistics
   */
  getCustomerAnalytics: async () => {
    const response = await axios.get(`${API_BASE_URL}/dashboard/customers`);
    return response.data;
  },

  /**
   * Get bulk payment analytics dashboard statistics
   */
  getBulkPaymentAnalytics: async () => {
    const response = await axios.get(`${API_BASE_URL}/dashboard/bulk-payments`);
    return response.data;
  },

  /**
   * Get ML model dashboard statistics
   */
  getMLModel: async () => {
    const response = await axios.get(`${API_BASE_URL}/dashboard/ml`);
    return response.data;
  },

  /**
   * Get compliance dashboard statistics
   */
  getCompliance: async () => {
    const response = await axios.get(`${API_BASE_URL}/dashboard/compliance`);
    return response.data;
  },
};

