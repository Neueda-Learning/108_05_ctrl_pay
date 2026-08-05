import React, { useState, useEffect } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { alpha } from '@mui/material/styles';
import {
  Box,
  Button,
  Card,
  CardContent,
  Grid,
  Alert,
  TextField,
  Typography,
  Chip,
  useTheme,
} from '@mui/material';
import { AccountCircle, AddCard, Analytics, ArrowForward, PersonAdd, Security } from '@mui/icons-material';
import { useCustomer } from '../context/CustomerContext';
import { adminFraudAPI } from '../services/api';
import TransactionFlowDiagram from '../components/TransactionFlowDiagram';

const actionCards = [
  {
    title: 'Create New Customer Profile',
    description: 'Collect only customer-provided details and let the backend create the customer identifier.',
    icon: PersonAdd,
    path: '/customers/new',
    buttonLabel: 'Create Customer',
  },
  {
    title: 'Create New Account',
    description: 'Enter a Customer ID, validate it with the backend, and create an account from customer-owned data.',
    icon: AddCard,
    path: '/accounts/new',
    buttonLabel: 'Create Account',
  },
  {
    title: 'Make Payment',
    description: 'Pick a customer account and continue with the existing payment processing flow.',
    icon: AccountCircle,
    path: '/payments/create',
    buttonLabel: 'Start Payment',
  },
  {
    title: 'Show Statistics Dashboard',
    description: 'Load customer payment records, view success/failure counts, and apply filters.',
    icon: Analytics,
    path: '/statistics',
    buttonLabel: 'Open Dashboard',
  },
];

function Dashboard() {
  const theme = useTheme();
  const navigate = useNavigate();
  const { customer, accounts, loadCustomer, loadingCustomer, clearCustomer } = useCustomer();
  const [customerIdInput, setCustomerIdInput] = useState('');
  const [error, setError] = useState('');
  const [fraudStats, setFraudStats] = useState(null);

  const handleLoadCustomer = async () => {
    try {
      setError('');
      await loadCustomer(customerIdInput);
    } catch (loadError) {
      setError(loadError.response?.data?.message || loadError.message || 'Unable to load customer');
    }
  };

  // Load fraud stats on mount
  useEffect(() => {
    adminFraudAPI.getStats()
      .then(r => setFraudStats(r.data))
      .catch(() => {}); // graceful — fraud service may not be ready
  }, []);

  const handleClear = () => {
    setCustomerIdInput('');
    setError('');
    clearCustomer();
  };

  const handleOpen = (path) => {
    navigate(path);
  };

  return (
    <Box>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
          Ctrl-Pay Home
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Customer-ID driven onboarding, account creation, payments, and reporting.
        </Typography>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} md={8}>
              <TextField
                label="Customer ID"
                value={customerIdInput}
                onChange={(e) => setCustomerIdInput(e.target.value)}
                fullWidth
                placeholder="Enter an existing customer ID to load their data"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                <Button variant="contained" onClick={handleLoadCustomer} disabled={loadingCustomer}>
                  {loadingCustomer ? 'Loading...' : 'Load Customer'}
                </Button>
                <Button variant="outlined" onClick={handleClear}>
                  Clear
                </Button>
              </Box>
            </Grid>
          </Grid>

          {error && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {error}
            </Alert>
          )}

          {customer && (
            <Alert severity="success" sx={{ mt: 2 }}>
              Loaded customer <strong>{customer.name}</strong> ({customer.customerId}) with {accounts.length} account(s).
            </Alert>
          )}
        </CardContent>
      </Card>

      <Grid container spacing={3} sx={{ mb: 3 }}>
        {actionCards.map(({ title, description, icon: Icon, path, buttonLabel }) => (
          <Grid item xs={12} sm={6} lg={3} key={title}>
            <Card sx={{ height: '100%' }}>
              <CardContent sx={{ height: '100%', display: 'flex', flexDirection: 'column', gap: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                  <Box
                    sx={{
                      width: 44,
                      height: 44,
                      borderRadius: '50%',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      backgroundColor: alpha(theme.palette.primary.main, 0.15),
                      color: 'primary.main',
                    }}
                  >
                    <Icon />
                  </Box>
                  <Typography variant="h6" sx={{ fontWeight: 700 }}>
                    {title}
                  </Typography>
                </Box>
                <Typography variant="body2" color="text.secondary" sx={{ flexGrow: 1 }}>
                  {description}
                </Typography>
                <Button
                  variant="outlined"
                  endIcon={<ArrowForward />}
                  onClick={() => handleOpen(path)}
                >
                  {buttonLabel}
                </Button>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* Fraud Monitoring Widget */}
      {fraudStats && (
        <Card sx={{ mb: 3, border: fraudStats.pendingReview > 0 ? '1px solid rgba(245,158,11,0.35)' : undefined }}>
          <CardContent>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Security sx={{ color: fraudStats.pendingReview > 0 ? '#F59E0B' : '#10B981' }} />
                <Typography variant="h6" sx={{ fontWeight: 700 }}>Fraud Monitoring</Typography>
                {fraudStats.pendingReview > 0 && (
                  <Chip
                    label={`${fraudStats.pendingReview} pending`}
                    size="small"
                    sx={{ background: 'rgba(245,158,11,0.15)', color: '#F59E0B', fontWeight: 700 }}
                  />
                )}
              </Box>
              <Button size="small" variant="outlined" onClick={() => navigate('/fraud')}>
                Open Fraud Dashboard →
              </Button>
            </Box>
            <Grid container spacing={2}>
              {[
                { label: 'Pending Review', value: fraudStats.pendingReview ?? 0, color: '#F59E0B' },
                { label: 'Approved Today', value: fraudStats.totalApproved ?? 0, color: '#10B981' },
                { label: 'Rejected', value: fraudStats.totalRejected ?? 0, color: '#EF4444' },
                { label: 'Fraud Rate', value: fraudStats.fraudRate ?? '0%', color: '#6366F1' },
              ].map(({ label, value, color }) => (
                <Grid item xs={6} sm={3} key={label}>
                  <Box sx={{ textAlign: 'center', p: 1, background: 'rgba(255,255,255,0.03)', borderRadius: 2 }}>
                    <Typography variant="h5" sx={{ fontWeight: 700, color }}>{value}</Typography>
                    <Typography variant="caption" sx={{ color: '#64748B' }}>{label}</Typography>
                  </Box>
                </Grid>
              ))}
            </Grid>
          </CardContent>
        </Card>
      )}

      {/* Transaction Lifecycle Flow Diagram */}
      <Box sx={{ mt: 3 }}>
        <TransactionFlowDiagram />
      </Box>

      {customer && (
        <Box sx={{ mt: 3 }}>
          <Button component={RouterLink} to="/accounts/new" variant="contained">
            Add Another Account for {customer.name}
          </Button>
        </Box>
      )}
    </Box>
  );
}

export default Dashboard;

