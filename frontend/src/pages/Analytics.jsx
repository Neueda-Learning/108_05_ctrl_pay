import React, { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Grid,
  MenuItem,
  TextField,
  Typography,
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import { format } from 'date-fns';
import { customerAPI } from '../services/api';
import { useCustomer } from '../context/CustomerContext';
import StatusBadge from '../components/StatusBadge';

const GRID_SX = {
  border: 'none',
  background: 'transparent',
  '& .MuiDataGrid-columnHeaders': {
    background:    '#1E293B',
    borderBottom:  '1px solid rgba(255,255,255,0.08)',
    borderRadius:  0,
    minHeight:     '48px !important',
  },
  '& .MuiDataGrid-columnHeaderTitle': {
    color:         '#94A3B8',
    fontWeight:    700,
    fontSize:      '0.68rem',
    letterSpacing: 0.9,
    textTransform: 'uppercase',
  },
  '& .MuiDataGrid-columnSeparator': { color: 'rgba(255,255,255,0.08)' },
  '& .MuiDataGrid-row': {
    background:  '#0F172A',
    transition:  'background 0.2s ease',
    '&:hover':   { background: 'rgba(99,102,241,0.08)' },
    '&.Mui-selected': {
      background: 'rgba(99,102,241,0.12)',
      '&:hover':  { background: 'rgba(99,102,241,0.16)' },
    },
  },
  '& .MuiDataGrid-cell': {
    color:        '#CBD5E1',
    fontSize:     '0.875rem',
    borderBottom: '1px solid rgba(255,255,255,0.04)',
    display:      'flex',
    alignItems:   'center',
  },
  '& .MuiDataGrid-footerContainer': {
    background:  '#1E293B',
    borderTop:   '1px solid rgba(255,255,255,0.08)',
  },
  '& .MuiTablePagination-root, & .MuiTablePagination-selectLabel, & .MuiTablePagination-displayedRows': {
    color:    '#94A3B8',
    fontSize: '0.8rem',
  },
  '& .MuiTablePagination-select': { color: '#CBD5E1' },
  '& .MuiDataGrid-iconButtonContainer .MuiIconButton-root, & .MuiTablePagination-actions .MuiIconButton-root': {
    color:     '#64748B',
    '&:hover': { background: 'rgba(99,102,241,0.1)', color: '#818CF8' },
    '&.Mui-disabled': { color: '#1E293B' },
  },
  '& .MuiDataGrid-overlay': { background: 'rgba(11,17,32,0.85)', color: '#94A3B8' },
  '& .MuiDataGrid-virtualScroller': { background: 'transparent' },
  '& .MuiDataGrid-withBorderColor': { borderColor: 'rgba(255,255,255,0.05)' },
};

function Analytics() {
  const { currentCustomer, customerAccounts, loading, error, selectCustomer, clearErrorForField } = useCustomer();

  const [customerIdInput, setCustomerIdInput] = useState(currentCustomer?.customerId || '');
  const [filters, setFilters] = useState({
    status: '',
    account: '',
    dateFrom: '',
    dateTo: '',
  });
  const [paymentLoading, setPaymentLoading] = useState(false);
  const [paymentError, setPaymentError] = useState('');
  const [payments, setPayments] = useState([]);

  // Sync input with current customer
  useEffect(() => {
    if (currentCustomer?.customerId) {
      setCustomerIdInput(currentCustomer.customerId);
    }
  }, [currentCustomer?.customerId]);

  const accountOptions = useMemo(() => (Array.isArray(customerAccounts) ? customerAccounts : []), [customerAccounts]);

  const getAccountValue = (account) => (
    account?.accountNumber
      || account?.sourceAccount
      || account?.accountId
      || account?.id
      || ''
  );

  const getAccountLabel = (account) => `${account?.accountName || 'Account'}${getAccountValue(account) ? ` (${getAccountValue(account)})` : ''}`;

  /**
   * Fetch payments for current customer with filters
   */
  const fetchCustomerPayments = async (customerId, currentFilters = filters) => {
    try {
      setPaymentLoading(true);
      setPaymentError('');

      const params = {
        ...(currentFilters.status && { status: currentFilters.status }),
        ...(currentFilters.account && { account: currentFilters.account }),
        ...(currentFilters.dateFrom && { 'date-from': currentFilters.dateFrom }),
        ...(currentFilters.dateTo && { 'date-to': currentFilters.dateTo }),
      };

      const response = await customerAPI.getCustomerPayments(customerId, params);
      const rows = Array.isArray(response.data)
        ? response.data
        : Array.isArray(response.data?.content)
          ? response.data.content
          : [];

      setPayments(rows.map((payment, index) => ({
        id: payment.id || payment.paymentId || index,
        ...payment,
      })));
    } catch (fetchError) {
      setPaymentError(fetchError.response?.data?.message || fetchError.message || 'Unable to load payment statistics');
      setPayments([]);
    } finally {
      setPaymentLoading(false);
    }
  };

  /**
   * Load customer dashboard - switches customer if ID different
   */
  const handleLoadDashboard = async () => {
    try {
      setPaymentError('');

      // If customer already loaded and ID matches, just fetch payments
      if (currentCustomer?.customerId === customerIdInput) {
        await fetchCustomerPayments(customerIdInput, filters);
        return;
      }

      // Otherwise, select new customer (which clears old and loads new)
      await selectCustomer(customerIdInput);
      // After customer loaded, fetch payments
      await fetchCustomerPayments(customerIdInput, filters);
    } catch (loadError) {
      setPaymentError(loadError.response?.data?.message || loadError.message || 'Unable to load customer');
      setPayments([]);
    }
  };

  /**
   * Apply filters to current customer's payments
   */
  const handleApplyFilters = async () => {
    const customerId = currentCustomer?.customerId || customerIdInput;
    if (!customerId) {
      setPaymentError('Enter a Customer ID before applying filters');
      return;
    }
    await fetchCustomerPayments(customerId, filters);
  };

  const summary = useMemo(() => {
    const total      = payments.length;
    const successful = payments.filter((p) => p.status === 'COMPLETED').length;
    const failed     = payments.filter((p) => p.status === 'FAILED').length;
    const pending    = total - successful - failed;
    return { total, successful, failed, pending };
  }, [payments]);

  const metricCards = [
    {
      label:          'Total Payments',
      value:          summary.total,
      icon:           '💳',
      gradient:       'linear-gradient(135deg, #6366F1 0%, #8B5CF6 100%)',
      borderGradient: 'rgba(99,102,241,0.55) 0%, rgba(139,92,246,0.30) 100%',
      glow:           'rgba(99,102,241,0.22)',
      valueColor:     '#818CF8',
    },
    {
      label:          'Successful',
      value:          summary.successful,
      icon:           '✓',
      gradient:       'linear-gradient(135deg, #10B981 0%, #34D399 100%)',
      borderGradient: 'rgba(16,185,129,0.55) 0%, rgba(52,211,153,0.30) 100%',
      glow:           'rgba(16,185,129,0.22)',
      valueColor:     '#34D399',
    },
    {
      label:          'Failed / Fraud',
      value:          summary.failed,
      icon:           '⚠',
      gradient:       'linear-gradient(135deg, #EF4444 0%, #F97316 100%)',
      borderGradient: 'rgba(239,68,68,0.55) 0%, rgba(249,115,22,0.30) 100%',
      glow:           'rgba(239,68,68,0.22)',
      valueColor:     '#F87171',
    },
    {
      label:          'Pending / Other',
      value:          summary.pending,
      icon:           '◷',
      gradient:       'linear-gradient(135deg, #F59E0B 0%, #FBBF24 100%)',
      borderGradient: 'rgba(245,158,11,0.55) 0%, rgba(251,191,36,0.30) 100%',
      glow:           'rgba(245,158,11,0.22)',
      valueColor:     '#FCD34D',
    },
  ];

  const columns = [
    { field: 'id', headerName: 'Payment ID', width: 140 },
    {
      field: 'sourceAccount',
      headerName: 'From Account',
      width: 180,
      renderCell: (params) => params.row.sourceAccount || params.row.source_account || params.row.accountId || '',
    },
    {
      field: 'destinationAccount',
      headerName: 'To Account',
      width: 180,
      renderCell: (params) => params.row.destinationAccount || params.row.destination_account || '',
    },
    {
      field: 'amount',
      headerName: 'Amount',
      width: 130,
      renderCell: (params) => `${params.row.amount} ${params.row.currency || ''}`,
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 140,
      renderCell: (params) => <StatusBadge status={params.value} />,
    },
    {
      field: 'createdAt',
      headerName: 'Created At',
      width: 180,
      renderCell: (params) => (params.value ? format(new Date(params.value), 'MMM dd, yyyy HH:mm') : 'N/A'),
    },
  ];

  const isLoading = loading.customer || paymentLoading;
  const displayError = error.customer || paymentError;

  return (
    <Box>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
          Statistics Dashboard
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Load a customer ID to inspect successful payments, failed payments, payment details, and filtered results.
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
                placeholder="Enter customer ID to load payment records"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <Button variant="contained" onClick={handleLoadDashboard} disabled={isLoading} fullWidth>
                {isLoading ? 'Loading...' : 'Load Statistics'}
              </Button>
            </Grid>
          </Grid>
          {displayError && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {displayError}
            </Alert>
          )}
          {currentCustomer && (
            <Alert severity="success" sx={{ mt: 2 }}>
              Showing payment data for {currentCustomer.name} ({currentCustomer.customerId}).
            </Alert>
          )}
        </CardContent>
      </Card>

      <Grid container spacing={3} sx={{ mb: 3 }}>
        {metricCards.map((card) => (
          <Grid item xs={12} sm={6} md={3} key={card.label}>
            <Card
              sx={{
                background:
                  `linear-gradient(rgba(11,17,32,0.9), rgba(11,17,32,0.9)) padding-box,` +
                  `linear-gradient(135deg, ${card.borderGradient}) border-box`,
                border: '1px solid transparent',
                boxShadow: `0 20px 40px rgba(0,0,0,0.25), 0 0 30px ${card.glow}`,
                '&:hover': {
                  transform: 'translateY(-6px)',
                  boxShadow: `0 28px 50px rgba(0,0,0,0.35), 0 0 40px ${card.glow}`,
                  background:
                    `linear-gradient(rgba(11,17,32,0.95), rgba(11,17,32,0.95)) padding-box,` +
                    `linear-gradient(135deg, ${card.borderGradient}) border-box`,
                },
              }}
            >
              <CardContent sx={{ p: 2.5 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                  <Typography
                    variant="caption"
                    sx={{ color: '#94A3B8', fontWeight: 600, letterSpacing: 0.6, textTransform: 'uppercase', fontSize: '0.68rem' }}
                  >
                    {card.label}
                  </Typography>
                  <Box
                    sx={{
                      width: 38, height: 38, borderRadius: '10px',
                      background: card.gradient,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: '1rem', fontWeight: 700, color: '#fff',
                      boxShadow: `0 4px 16px ${card.glow}`,
                      flexShrink: 0,
                    }}
                  >
                    {card.icon}
                  </Box>
                </Box>
                <Typography
                  variant="h3"
                  sx={{ fontWeight: 800, color: card.valueColor, letterSpacing: -1, lineHeight: 1 }}
                >
                  {card.value}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
            Filters
          </Typography>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                label="Status"
                select
                fullWidth
                value={filters.status}
                onChange={(e) => setFilters({ ...filters, status: e.target.value })}
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="CREATED">Created</MenuItem>
                <MenuItem value="VALIDATED">Validated</MenuItem>
                <MenuItem value="SENT">Sent</MenuItem>
                <MenuItem value="COMPLETED">Completed</MenuItem>
                <MenuItem value="FAILED">Failed</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                label="Account"
                select
                fullWidth
                value={filters.account}
                onChange={(e) => setFilters({ ...filters, account: e.target.value })}
              >
                <MenuItem value="">All Accounts</MenuItem>
                {accountOptions.map((account) => {
                  const value = getAccountValue(account);
                  return (
                    <MenuItem key={value || account.accountName} value={value}>
                      {getAccountLabel(account)}
                    </MenuItem>
                  );
                })}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                label="From Date"
                type="date"
                fullWidth
                value={filters.dateFrom}
                onChange={(e) => setFilters({ ...filters, dateFrom: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                label="To Date"
                type="date"
                fullWidth
                value={filters.dateTo}
                onChange={(e) => setFilters({ ...filters, dateTo: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12}>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                <Button variant="contained" onClick={handleApplyFilters} disabled={paymentLoading}>
                  Apply Filters
                </Button>
                <Button
                  variant="outlined"
                  onClick={() => setFilters({ status: '', account: '', dateFrom: '', dateTo: '' })}
                >
                  Clear Filters
                </Button>
              </Box>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
            Payment Details
          </Typography>
          {paymentLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
              <CircularProgress />
            </Box>
          ) : (
            <DataGrid
              autoHeight
              rows={payments}
              columns={columns}
              pageSizeOptions={[10, 25, 50]}
              initialState={{ pagination: { paginationModel: { pageSize: 10, page: 0 } } }}
              disableRowSelectionOnClick
              sx={GRID_SX}
            />
          )}
        </CardContent>
      </Card>
    </Box>
  );
}

export default Analytics;

