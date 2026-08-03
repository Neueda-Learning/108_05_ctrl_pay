import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
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

// Create professional theme
const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
      light: '#42a5f5',
      dark: '#1565c0',
    },
    secondary: {
      main: '#dc004e',
      light: '#ff5c8d',
      dark: '#9a0036',
    },
    success: {
      main: '#2e7d32',
      light: '#4caf50',
      lighter: '#c8e6c9',
      dark: '#1b5e20',
    },
    warning: {
      main: '#f57c00',
      light: '#ffb74d',
      lighter: '#ffe0b2',
      dark: '#e65100',
    },
    error: {
      main: '#d32f2f',
      light: '#ef5350',
      lighter: '#ffcdd2',
      dark: '#c62828',
    },
    info: {
      main: '#1976d2',
      light: '#42a5f5',
      lighter: '#bbdefb',
      dark: '#1565c0',
    },
    background: {
      default: '#f5f5f5',
      paper: '#ffffff',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
    h1: { fontWeight: 600, fontSize: '2rem' },
    h2: { fontWeight: 600, fontSize: '1.75rem' },
    h3: { fontWeight: 600, fontSize: '1.5rem' },
    h4: { fontWeight: 600, fontSize: '1.25rem' },
    h5: { fontWeight: 600, fontSize: '1rem' },
    h6: { fontWeight: 600, fontSize: '0.875rem' },
  },
  shape: {
    borderRadius: 8,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 600,
          fontSize: '0.95rem',
          padding: '10px 20px',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
          transition: 'box-shadow 0.3s ease',
          '&:hover': {
            boxShadow: '0 4px 16px rgba(0,0,0,0.15)',
          },
        },
      },
    },
  },
});

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Router>
        <Layout>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/payments" element={<PaymentsList />} />
            <Route path="/payments/create" element={<CreatePayment />} />
            <Route path="/payment/process/:id" element={<PaymentProcessing />} />
            <Route path="/payments/:id" element={<PaymentDetail />} />
            <Route path="/rules" element={<RulesManagement />} />
            <Route path="/analytics" element={<Analytics />} />
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
        theme="light"
      />
    </ThemeProvider>
  );
}

export default App;

