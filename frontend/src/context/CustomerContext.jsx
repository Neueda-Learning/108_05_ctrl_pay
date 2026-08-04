import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { accountAPI, customerAPI } from '../services/api';

const CustomerContext = createContext(null);

export function CustomerProvider({ children }) {
  const [customerId, setCustomerId] = useState('');
  const [customer, setCustomer] = useState(null);
  const [accounts, setAccounts] = useState([]);
  const [loadingCustomer, setLoadingCustomer] = useState(false);

  const loadCustomer = useCallback(async (inputCustomerId) => {
    const normalizedCustomerId = String(inputCustomerId || '').trim();
    if (!normalizedCustomerId) {
      throw new Error('Customer ID is required');
    }

    setLoadingCustomer(true);
    try {
      const customerRes = await customerAPI.getCustomer(normalizedCustomerId);
      let accountsData = [];

      try {
        const accountsRes = await accountAPI.listAccountsByCustomer(normalizedCustomerId);
        accountsData = Array.isArray(accountsRes.data) ? accountsRes.data : [];
      } catch {
        accountsData = [];
      }

      setCustomerId(normalizedCustomerId);
      setCustomer(customerRes.data);
      setAccounts(accountsData);
      return { customer: customerRes.data, accounts: accountsData };
    } finally {
      setLoadingCustomer(false);
    }
  }, []);

  const refreshCurrentCustomer = useCallback(async () => {
    if (!customerId) {
      return;
    }

    return loadCustomer(customerId);
  }, [customerId, loadCustomer]);

  const clearCustomer = useCallback(() => {
    setCustomerId('');
    setCustomer(null);
    setAccounts([]);
  }, []);

  const value = useMemo(() => ({
    customerId,
    customer,
    accounts,
    loadingCustomer,
    setCustomerId,
    loadCustomer,
    refreshCurrentCustomer,
    clearCustomer,
  }), [accounts, clearCustomer, customer, customerId, loadCustomer, loadingCustomer, refreshCurrentCustomer]);

  return <CustomerContext.Provider value={value}>{children}</CustomerContext.Provider>;
}

export function useCustomer() {
  const context = useContext(CustomerContext);
  if (!context) {
    throw new Error('useCustomer must be used within CustomerProvider');
  }
  return context;
}

