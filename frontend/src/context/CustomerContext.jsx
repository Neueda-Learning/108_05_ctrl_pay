import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { accountAPI, customerAPI } from '../services/api';

const CustomerContext = createContext(null);

export function CustomerProvider({ children }) {
  // Primary state
  const [currentCustomer, setCurrentCustomer] = useState(null);
  const [customerAccounts, setCustomerAccounts] = useState([]);

  // Loading states - granular control
  const [loading, setLoading] = useState({
    customer: false,
    accounts: false,
    creating: false,
  });

  // Error states - per section
  const [error, setError] = useState({
    customer: null,
    accounts: null,
    creating: null,
  });

  // Legacy fields for backward compatibility
  const customerId = currentCustomer?.customerId || '';
  const customer = currentCustomer;
  const accounts = customerAccounts;

  /**
   * Internal: Clear all customer context
   */
  const _clearAll = useCallback(() => {
    setCurrentCustomer(null);
    setCustomerAccounts([]);
    setError({ customer: null, accounts: null, creating: null });
  }, []);

  /**
   * Load customer by ID - fetches customer + accounts
   */
  const loadCustomer = useCallback(async (inputCustomerId) => {
    const normalizedCustomerId = String(inputCustomerId || '').trim();
    if (!normalizedCustomerId) {
      const errorMsg = 'Customer ID is required';
      setError((prev) => ({ ...prev, customer: errorMsg }));
      throw new Error(errorMsg);
    }

    setLoading((prev) => ({ ...prev, customer: true, accounts: true }));
    setError((prev) => ({ ...prev, customer: null, accounts: null }));

    try {
      // Fetch customer details
      const customerRes = await customerAPI.getCustomer(normalizedCustomerId);
      setCurrentCustomer(customerRes.data);

      // Fetch customer accounts
      let accountsData = [];
      try {
        const accountsRes = await accountAPI.listAccountsByCustomer(normalizedCustomerId);
        accountsData = Array.isArray(accountsRes.data) ? accountsRes.data : [];
      } catch (accountError) {
        console.error('Failed to load accounts:', accountError);
        setError((prev) => ({ ...prev, accounts: accountError.message }));
        accountsData = [];
      }

      setCustomerAccounts(accountsData);
      return { customer: customerRes.data, accounts: accountsData };
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || 'Failed to load customer';
      setError((prev) => ({ ...prev, customer: errorMsg }));
      setCurrentCustomer(null);
      setCustomerAccounts([]);
      throw err;
    } finally {
      setLoading((prev) => ({ ...prev, customer: false, accounts: false }));
    }
  }, []);

  /**
   * Select a different customer - clears old, loads new
   * Use this when switching customers
   */
  const selectCustomer = useCallback(
    async (inputCustomerId) => {
      _clearAll();
      return loadCustomer(inputCustomerId);
    },
    [loadCustomer, _clearAll]
  );

  /**
   * Refresh only the accounts for current customer
   * Use after creating a new account
   */
  const refreshAccounts = useCallback(async () => {
    if (!currentCustomer?.customerId) {
      return;
    }

    setLoading((prev) => ({ ...prev, accounts: true }));
    setError((prev) => ({ ...prev, accounts: null }));

    try {
      const accountsRes = await accountAPI.listAccountsByCustomer(currentCustomer.customerId);
      const accountsData = Array.isArray(accountsRes.data) ? accountsRes.data : [];
      setCustomerAccounts(accountsData);
      return accountsData;
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || 'Failed to load accounts';
      setError((prev) => ({ ...prev, accounts: errorMsg }));
      throw err;
    } finally {
      setLoading((prev) => ({ ...prev, accounts: false }));
    }
  }, [currentCustomer?.customerId]);

  /**
   * Refresh current customer details
   */
  const refreshCurrentCustomer = useCallback(async () => {
    if (!currentCustomer?.customerId) {
      return;
    }

    return loadCustomer(currentCustomer.customerId);
  }, [currentCustomer?.customerId, loadCustomer]);

  /**
   * Clear customer context
   */
  const clearCustomer = useCallback(() => {
    _clearAll();
  }, [_clearAll]);

  /**
   * Set error for a specific section
   */
  const setErrorForField = useCallback((field, message) => {
    setError((prev) => ({ ...prev, [field]: message }));
  }, []);

  /**
   * Clear error for a specific section
   */
  const clearErrorForField = useCallback((field) => {
    setError((prev) => ({ ...prev, [field]: null }));
  }, []);

  /**
   * Mark creating state (for payments, accounts, etc)
   */
  const setCreating = useCallback((isCreating) => {
    setLoading((prev) => ({ ...prev, creating: isCreating }));
  }, []);

  const value = useMemo(
    () => ({
      // Primary state
      currentCustomer,
      customerAccounts,
      loading,
      error,

      // Legacy fields for backward compatibility
      customerId,
      customer,
      accounts,
      loadingCustomer: loading.customer,

      // Methods
      loadCustomer,
      selectCustomer,
      refreshAccounts,
      refreshCurrentCustomer,
      clearCustomer,
      setErrorForField,
      clearErrorForField,
      setCreating,

      // Keep for backward compatibility but deprecated
      setCustomerId: (_id) => {
        console.warn('setCustomerId is deprecated, use loadCustomer() instead');
      },
    }),
    [
      currentCustomer,
      customerAccounts,
      loading,
      error,
      customerId,
      customer,
      accounts,
      loadCustomer,
      selectCustomer,
      refreshAccounts,
      refreshCurrentCustomer,
      clearCustomer,
      setErrorForField,
      clearErrorForField,
      setCreating,
    ]
  );

  return <CustomerContext.Provider value={value}>{children}</CustomerContext.Provider>;
}

export function useCustomer() {
  const context = useContext(CustomerContext);
  if (!context) {
    throw new Error('useCustomer must be used within CustomerProvider');
  }
  return context;
}

