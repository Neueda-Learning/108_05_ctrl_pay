import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import PaymentsList from './pages/PaymentsList';
import PaymentDetail from './pages/PaymentDetail';
import PaymentProcessing from './pages/PaymentProcessing';
import CreatePayment from './pages/CreatePayment';
import RulesManagement from './pages/RulesManagement';
import Analytics from './pages/Analytics';
import AuthPage from './pages/AuthPage';
import CustomerProfile from './pages/CustomerProfile';
import FraudDashboard from './pages/FraudDashboard';
import FraudRulesManagement from './pages/FraudRulesManagement';
import { CustomerProvider } from './context/CustomerContext';
import { AppThemeProvider, useAppTheme } from './context/ThemeContext';

function AppShell() {
  const { mode } = useAppTheme();

  return (
    <CustomerProvider>
      <Router>
        <Layout>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/customers/new" element={<AuthPage />} />
            <Route path="/accounts/new" element={<CustomerProfile />} />
            <Route path="/payments" element={<PaymentsList />} />
            <Route path="/payments/create" element={<CreatePayment />} />
            <Route path="/payment/process/:id" element={<PaymentProcessing />} />
            <Route path="/payments/:id" element={<PaymentDetail />} />
            <Route path="/rules" element={<RulesManagement />} />
            <Route path="/fraud-rules" element={<FraudRulesManagement />} />
            <Route path="/statistics" element={<Analytics />} />
            <Route path="/analytics" element={<Navigate to="/statistics" replace />} />
            <Route path="/fraud" element={<FraudDashboard />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Layout>
      </Router>
      <ToastContainer
        position="bottom-right"
        autoClose={5000}
        hideProgressBar={false}
        newestOnTop={false}
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
        theme={mode}
      />
    </CustomerProvider>
  );
}

function App() {
  return (
    <AppThemeProvider>
      <AppShell />
    </AppThemeProvider>
  );
}

export default App;
