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
import AuthPage from './pages/AuthPage';
import CustomerProfile from './pages/CustomerProfile';
import { CustomerProvider } from './context/CustomerContext';

// Create professional theme
const theme = createTheme({
  palette: {
    mode: 'dark',
    primary: {
      main: '#6366F1',
      light: '#818CF8',
      dark: '#4F46E5',
    },
    secondary: {
      main: '#8B5CF6',
      light: '#A78BFA',
      dark: '#7C3AED',
    },
    success: {
      main: '#10B981',
      light: '#34D399',
      lighter: '#064E3B',
      dark: '#059669',
    },
    warning: {
      main: '#F59E0B',
      light: '#FCD34D',
      lighter: '#451A03',
      dark: '#D97706',
    },
    error: {
      main: '#EF4444',
      light: '#F87171',
      lighter: '#450A0A',
      dark: '#DC2626',
    },
    info: {
      main: '#06B6D4',
      light: '#22D3EE',
      lighter: '#164E63',
      dark: '#0891B2',
    },
    background: {
      default: '#070B18',
      paper: '#0F172A',
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
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          background: 'linear-gradient(135deg, #070B18 0%, #111827 50%, #1E1B4B 100%)',
          minHeight: '100vh',
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform:  'none',
          fontWeight:     600,
          fontSize:       '0.875rem',
          borderRadius:   '12px',
          padding:        '10px 22px',
          letterSpacing:  0.3,
          transition:     'all 0.25s cubic-bezier(0.4,0,0.2,1)',
          position:       'relative',
          overflow:       'hidden',

          /* shimmer sweep on hover */
          '&::after': {
            content:    '""',
            position:   'absolute',
            inset:      0,
            background: 'linear-gradient(105deg, transparent 40%, rgba(255,255,255,0.12) 50%, transparent 60%)',
            transform:  'translateX(-100%)',
            transition: 'transform 0.45s ease',
          },
          '&:hover::after': {
            transform: 'translateX(100%)',
          },
        },

        /* ── contained → gradient primary ────────────────── */
        contained: {
          background:    'linear-gradient(135deg, #6366F1 0%, #8B5CF6 100%)',
          border:        '1px solid rgba(139,92,246,0.4)',
          color:         '#fff',
          boxShadow:     '0 4px 20px rgba(99,102,241,0.4), inset 0 1px 0 rgba(255,255,255,0.15)',

          '&:hover': {
            background:  'linear-gradient(135deg, #4F46E5 0%, #7C3AED 100%)',
            boxShadow:   '0 8px 30px rgba(99,102,241,0.55), 0 0 0 3px rgba(99,102,241,0.18), inset 0 1px 0 rgba(255,255,255,0.18)',
            transform:   'translateY(-2px)',
          },

          '&:active': {
            transform:   'translateY(0px)',
            boxShadow:   '0 4px 15px rgba(99,102,241,0.4)',
          },

          '&.Mui-disabled': {
            background:  'rgba(99,102,241,0.25)',
            color:       'rgba(255,255,255,0.4)',
            boxShadow:   'none',
            border:      '1px solid rgba(99,102,241,0.15)',
          },
        },

        /* ── outlined → glass secondary ──────────────────── */
        outlined: {
          background:  'rgba(255,255,255,0.05)',
          border:      '1px solid rgba(255,255,255,0.15)',
          color:       '#CBD5E1',
          backdropFilter: 'blur(12px)',
          WebkitBackdropFilter: 'blur(12px)',
          boxShadow:   '0 2px 8px rgba(0,0,0,0.2), inset 0 1px 0 rgba(255,255,255,0.06)',

          '&:hover': {
            background:  'rgba(99,102,241,0.12)',
            border:      '1px solid rgba(99,102,241,0.45)',
            color:       '#E0E7FF',
            boxShadow:   '0 4px 16px rgba(0,0,0,0.25), 0 0 0 3px rgba(99,102,241,0.1), inset 0 1px 0 rgba(255,255,255,0.08)',
            transform:   'translateY(-2px)',
          },

          '&:active': {
            transform:  'translateY(0px)',
          },

          '&.Mui-disabled': {
            background: 'rgba(255,255,255,0.02)',
            border:     '1px solid rgba(255,255,255,0.06)',
            color:      'rgba(255,255,255,0.2)',
          },
        },

        /* ── text → subtle ghost ──────────────────────────── */
        text: {
          color:   '#94A3B8',
          padding: '8px 14px',

          '&:hover': {
            background: 'rgba(99,102,241,0.1)',
            color:      '#C7D2FE',
          },
        },

        /* size overrides */
        sizeSmall: {
          fontSize:     '0.8rem',
          padding:      '6px 14px',
          borderRadius: '10px',
        },
        sizeLarge: {
          fontSize:     '1rem',
          padding:      '13px 28px',
          borderRadius: '14px',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: '20px',
          background:
            'linear-gradient(rgba(11,17,32,0.85), rgba(11,17,32,0.85)) padding-box,' +
            'linear-gradient(135deg, rgba(99,102,241,0.25) 0%, rgba(139,92,246,0.12) 100%) border-box',
          border: '1px solid transparent',
          backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
          boxShadow: '0 20px 40px rgba(0,0,0,0.25)',
          transition: 'transform 0.3s cubic-bezier(0.4,0,0.2,1), box-shadow 0.3s cubic-bezier(0.4,0,0.2,1), background 0.3s ease',
          backgroundImage: 'none',
          '&:hover': {
            transform: 'translateY(-4px)',
            boxShadow: '0 28px 50px rgba(0,0,0,0.35), 0 0 24px rgba(99,102,241,0.12)',
            background:
              'linear-gradient(rgba(11,17,32,0.92), rgba(11,17,32,0.92)) padding-box,' +
              'linear-gradient(135deg, rgba(99,102,241,0.45) 0%, rgba(139,92,246,0.28) 100%) border-box',
          },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
        },
      },
    },
    MuiTableContainer: {
      styleOverrides: {
        root: {
          background:   '#111827',
          borderRadius: '16px',
          overflow:     'hidden',
          border:       '1px solid rgba(255,255,255,0.06)',
        },
      },
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          '& .MuiTableCell-root': {
            background:     '#1E293B',
            color:          '#94A3B8',
            fontWeight:     700,
            fontSize:       '0.68rem',
            letterSpacing:  0.9,
            textTransform:  'uppercase',
            borderBottom:   '1px solid rgba(255,255,255,0.08)',
            padding:        '14px 20px',
            whiteSpace:     'nowrap',
          },
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: {
          background:  '#0F172A',
          transition:  'background 0.2s ease',
          '&:hover':   { background: 'rgba(99,102,241,0.08)' },
          '&:last-child .MuiTableCell-root': { borderBottom: 'none' },
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottom: '1px solid rgba(255,255,255,0.05)',
          color:        '#CBD5E1',
          padding:      '14px 20px',
          fontSize:     '0.875rem',
        },
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: {
          background: 'rgba(15,23,42,0.92)',
          border: '1px solid rgba(255,255,255,0.12)',
          backdropFilter: 'blur(24px)',
          WebkitBackdropFilter: 'blur(24px)',
        },
      },
    },
  },
});

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
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
              <Route path="/statistics" element={<Analytics />} />
              <Route path="/analytics" element={<Navigate to="/statistics" replace />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </Layout>
        </Router>
      </CustomerProvider>
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
        theme="dark"
      />
    </ThemeProvider>
  );
}


export default App;

