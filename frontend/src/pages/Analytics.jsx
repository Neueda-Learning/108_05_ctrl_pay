import React, { useEffect, useMemo, useState } from 'react';
import { alpha } from '@mui/material/styles';
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
  useTheme,
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import { format } from 'date-fns';
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip as RechartsTooltip } from 'recharts';
import { customerAPI } from '../services/api';
import { useCustomer } from '../context/CustomerContext';
import StatusBadge from '../components/StatusBadge';

const getGridSx = (theme) => {
  const custom = theme.customTokens;

  return {
    border: 'none',
    background: 'transparent',
    '& .MuiDataGrid-columnHeaders': {
      background: custom.gridHeader,
      borderBottom: `1px solid ${alpha(custom.border, 0.75)}`,
      borderRadius: 0,
      minHeight: '48px !important',
    },
    '& .MuiDataGrid-columnHeaderTitle': {
      color: custom.text.secondary,
      fontWeight: 700,
      fontSize: '0.68rem',
      letterSpacing: 0.9,
      textTransform: 'uppercase',
    },
    '& .MuiDataGrid-columnSeparator': { color: alpha(custom.border, 0.75) },
    '& .MuiDataGrid-row': {
      background: custom.gridRow,
      transition: custom.transition,
      '&:hover': { background: alpha(custom.brand.main, 0.08) },
      '&.Mui-selected': {
        background: alpha(custom.brand.main, 0.12),
        '&:hover': { background: alpha(custom.brand.main, 0.16) },
      },
    },
    '& .MuiDataGrid-cell': {
      color: custom.text.secondary,
      fontSize: '0.875rem',
      borderBottom: `1px solid ${alpha(custom.border, 0.45)}`,
      display: 'flex',
      alignItems: 'center',
    },
    '& .MuiDataGrid-footerContainer': {
      background: custom.gridHeader,
      borderTop: `1px solid ${alpha(custom.border, 0.75)}`,
    },
    '& .MuiTablePagination-root, & .MuiTablePagination-selectLabel, & .MuiTablePagination-displayedRows': {
      color: custom.text.secondary,
      fontSize: '0.8rem',
    },
    '& .MuiTablePagination-select': { color: custom.text.primary },
    '& .MuiDataGrid-iconButtonContainer .MuiIconButton-root, & .MuiTablePagination-actions .MuiIconButton-root': {
      color: custom.text.muted,
      '&:hover': { background: alpha(custom.brand.main, 0.1), color: custom.brand.accent },
      '&.Mui-disabled': { color: alpha(custom.text.muted, 0.5) },
    },
    '& .MuiDataGrid-overlay': { background: custom.gridOverlay, color: custom.text.secondary },
    '& .MuiDataGrid-virtualScroller': { background: 'transparent' },
    '& .MuiDataGrid-withBorderColor': { borderColor: alpha(custom.border, 0.6) },
  };
};

function Analytics() {

  const { currentCustomer, customerAccounts, loading, error, selectCustomer } = useCustomer();

  const [customerIdInput, setCustomerIdInput] = useState(currentCustomer?.customerId || '');

  const theme = useTheme();
  const custom = theme.customTokens;

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

  const pieData = useMemo(() => ([
    {
      name: 'Successful',
      value: summary.successful,
      color: '#10B981',
      glow: alpha('#10B981', 0.45),
    },
    {
      name: 'Failed',
      value: summary.failed,
      color: '#EF4444',
      glow: alpha('#EF4444', 0.45),
    },
    {
      name: 'Pending / Other',
      value: summary.pending,
      color: '#F59E0B',
      glow: alpha('#F59E0B', 0.4),
    },
  ]), [summary.successful, summary.failed, summary.pending]);

  const chartHasData = pieData.some((slice) => slice.value > 0);

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
                  `linear-gradient(${alpha(custom.background.card, 0.9)}, ${alpha(custom.background.card, 0.9)}) padding-box,` +
                  `linear-gradient(135deg, ${card.borderGradient}) border-box`,
                border: '1px solid transparent',
                boxShadow: `0 20px 40px rgba(0,0,0,0.25), 0 0 30px ${card.glow}`,
                '&:hover': {
                  transform: 'translateY(-6px)',
                  boxShadow: `0 28px 50px rgba(0,0,0,0.35), 0 0 40px ${card.glow}`,
                  background:
                    `linear-gradient(${alpha(custom.background.card, 0.96)}, ${alpha(custom.background.card, 0.96)}) padding-box,` +
                    `linear-gradient(135deg, ${card.borderGradient}) border-box`,
                },
              }}
            >
              <CardContent sx={{ p: 2.5 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                  <Typography
                    variant="caption"
                    sx={{ color: custom.text.secondary, fontWeight: 600, letterSpacing: 0.6, textTransform: 'uppercase', fontSize: '0.68rem' }}
                  >
                    {card.label}
                  </Typography>
                  <Box
                    sx={{
                      width: 38, height: 38, borderRadius: '10px',
                      background: card.gradient,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: '1rem', fontWeight: 700, color: '#FFFFFF',
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
            Payment Distribution
          </Typography>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} md={7}>
              <Box
                sx={{
                  height: 320,
                  borderRadius: 3,
                  border: `1px solid ${alpha(custom.border, 0.7)}`,
                  background:
                    `radial-gradient(circle at 15% 20%, ${alpha(custom.brand.main, 0.12)} 0%, transparent 55%),` +
                    `radial-gradient(circle at 85% 80%, ${alpha(custom.brand.accent, 0.12)} 0%, transparent 50%)`,
                  p: 1,
                }}
              >
                {chartHasData ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={pieData}
                        dataKey="value"
                        nameKey="name"
                        cx="50%"
                        cy="50%"
                        innerRadius={76}
                        outerRadius={118}
                        paddingAngle={4}
                        cornerRadius={10}
                        isAnimationActive
                        animationDuration={850}
                        animationEasing="ease-out"
                        labelLine={false}
                        label={({ percent }) => `${Math.round(percent * 100)}%`}
                      >
                        {pieData.map((entry) => (
                          <Cell key={entry.name} fill={entry.color} stroke={alpha(entry.color, 0.95)} />
                        ))}
                      </Pie>
                      <RechartsTooltip
                        formatter={(value, name) => [value, name]}
                        contentStyle={{
                          borderRadius: 12,
                          border: `1px solid ${alpha(custom.border, 0.9)}`,
                          background: alpha(custom.background.card, 0.96),
                          color: custom.text.primary,
                        }}
                        itemStyle={{ color: custom.text.primary }}
                        labelStyle={{ color: custom.text.secondary }}
                      />
                    </PieChart>
                  </ResponsiveContainer>
                ) : (
                  <Box sx={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <Typography variant="body2" color="text.secondary">
                      No payment data available yet.
                    </Typography>
                  </Box>
                )}
              </Box>
            </Grid>

            <Grid item xs={12} md={5}>
              <Box sx={{ display: 'grid', gap: 1.25 }}>
                {pieData.map((slice) => (
                  <Box
                    key={slice.name}
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      px: 1.5,
                      py: 1.25,
                      borderRadius: 2,
                      border: `1px solid ${alpha(slice.color, 0.45)}`,
                      backgroundColor: alpha(slice.color, 0.12),
                      boxShadow: `0 0 18px ${slice.glow}`,
                    }}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Box
                        sx={{
                          width: 10,
                          height: 10,
                          borderRadius: '50%',
                          bgcolor: slice.color,
                          boxShadow: `0 0 10px ${slice.glow}`,
                        }}
                      />
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        {slice.name}
                      </Typography>
                    </Box>
                    <Typography variant="body2" sx={{ fontWeight: 700 }}>
                      {slice.value}
                    </Typography>
                  </Box>
                ))}
                <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5 }}>
                  Total payments: {summary.total}
                </Typography>
              </Box>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

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
              sx={getGridSx(theme)}
            />
          )}
        </CardContent>
      </Card>
    </Box>
  );
}

export default Analytics;

