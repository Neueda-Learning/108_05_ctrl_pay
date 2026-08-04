import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { accountAPI, authAPI, customerAPI } from '../services/api';

const SESSION_STORAGE_KEY = 'ctrlpay_session';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [customer, setCustomer] = useState(null);
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);

  const persistSession = useCallback((session) => {
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
  }, []);

  const clearSession = useCallback(() => {
    localStorage.removeItem(SESSION_STORAGE_KEY);
  }, []);

  const refreshCustomerData = useCallback(async (customerId) => {
    const [customerRes, accountsRes] = await Promise.all([
      customerAPI.getCustomer(customerId),
      accountAPI.listAccountsByCustomer(customerId),
    ]);

    setCustomer(customerRes.data);
    setAccounts(accountsRes.data || []);
  }, []);

  const login = useCallback(async ({ phoneNumber, dob }) => {
    const loginRes = await authAPI.login({ phoneNumber, dob });
    const { customerId, customer: customerPayload, accounts: accountPayload } = loginRes.data;

    if (customerPayload && accountPayload) {
      setCustomer(customerPayload);
      setAccounts(accountPayload);
    } else {
      await refreshCustomerData(customerId);
    }

    persistSession({ customerId, phoneNumber });
  }, [persistSession, refreshCustomerData]);

  const register = useCallback(async (profileData) => {
    const created = await customerAPI.createCustomer(profileData);
    return created.data;
  }, []);

  const logout = useCallback(async () => {
    try {
      await authAPI.logout();
    } catch (error) {
      // Frontend still clears local session if backend logout endpoint is unavailable.
    }

    clearSession();
    setCustomer(null);
    setAccounts([]);
  }, [clearSession]);

  const addAccount = useCallback(async (accountData) => {
    if (!customer?.customerId) {
      throw new Error('Customer session missing');
    }

    await accountAPI.createAccount(customer.customerId, accountData);
    await refreshCustomerData(customer.customerId);
  }, [customer, refreshCustomerData]);

  useEffect(() => {
    let cancelled = false;

    const bootstrap = async () => {
      try {
        const storedSession = localStorage.getItem(SESSION_STORAGE_KEY);
        if (!storedSession) {
          return;
        }

        const parsed = JSON.parse(storedSession);
        if (!parsed?.customerId) {
          clearSession();
          return;
        }

        await refreshCustomerData(parsed.customerId);
      } catch (error) {
        clearSession();
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    bootstrap();

    return () => {
      cancelled = true;
    };
  }, [clearSession, refreshCustomerData]);

  useEffect(() => {
    if (!loading) {
      return;
    }

    // No session scenario still needs to release the loading gate.
    const timeout = setTimeout(() => setLoading(false), 0);
    return () => clearTimeout(timeout);
  }, [loading]);

  const value = useMemo(() => ({
    customer,
    accounts,
    loading,
    isAuthenticated: Boolean(customer),
    login,
    register,
    logout,
    addAccount,
    refreshCustomerData,
  }), [accounts, addAccount, customer, loading, login, logout, refreshCustomerData, register]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}

