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
  Chip,
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import { format } from 'date-fns';
import { customerAPI } from '../services/api';
import { useCustomer } from '../context/CustomerContext';

const statusColors = {
  CREATED: 'default',
  VALIDATED: 'info',
  SENT: 'warning',
  COMPLETED: 'success',
  FAILED: 'error',
};

function Analytics() {
  const { customerId, customer, accounts, loadCustomer, loadingCustomer } = useCustomer();
  const [customerIdInput, setCustomerIdInput] = useState(customerId || '');
  const [filters, setFilters] = useState({
    status: '',
    account: '',
    dateFrom: '',
    dateTo: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [payments, setPayments] = useState([]);

  useEffect(() => {
    setCustomerIdInput(customerId || '');
  }, [customerId]);

  const accountOptions = useMemo(() => (Array.isArray(accounts) ? accounts : []), [accounts]);

  const getAccountValue = (account) => (
    account?.accountNumber
      || account?.sourceAccount
      || account?.accountId
      || account?.id
      || ''
  );

  const getAccountLabel = (account) => `${account?.accountName || 'Account'}${getAccountValue(account) ? ` (${getAccountValue(account)})` : ''}`;

  const fetchCustomerPayments = async (customerId, currentFilters = filters) => {
    try {
      setLoading(true);
      setError('');

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
      setError(fetchError.response?.data?.message || fetchError.message || 'Unable to load payment statistics');
      setPayments([]);
    } finally {
      setLoading(false);
    }
  };

  const handleLoadDashboard = async () => {
    try {
      const loadedCustomer = await loadCustomer(customerIdInput);
      const customerId = loadedCustomer?.customer?.customerId || customerIdInput;
      await fetchCustomerPayments(customerId, filters);
    } catch (loadError) {
      setError(loadError.response?.data?.message || loadError.message || 'Unable to load customer');
      setPayments([]);
    }
  };

  const handleApplyFilters = async () => {
    const customerId = customer?.customerId || customerIdInput;
    if (!customerId) {
      setError('Enter a Customer ID before applying filters');
      return;
    }
    await fetchCustomerPayments(customerId, filters);
  };

  const summary = useMemo(() => {
    const total = payments.length;
    const successful = payments.filter((payment) => payment.status === 'COMPLETED').length;
    const failed = payments.filter((payment) => payment.status === 'FAILED').length;
    const pending = total - successful - failed;
    return { total, successful, failed, pending };
  }, [payments]);

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
      width: 130,
      renderCell: (params) => (
        <Chip
          size="small"
          label={params.value}
          color={statusColors[params.value] || 'default'}
          variant="outlined"
        />
      ),
    },
    {
      field: 'createdAt',
      headerName: 'Created At',
      width: 180,
      renderCell: (params) => (params.value ? format(new Date(params.value), 'MMM dd, yyyy HH:mm') : 'N/A'),
    },
  ];

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
              <Button variant="contained" onClick={handleLoadDashboard} disabled={loadingCustomer || loading} fullWidth>
                {(loadingCustomer || loading) ? 'Loading...' : 'Load Statistics'}
              </Button>
            </Grid>
          </Grid>
          {error && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {error}
            </Alert>
          )}
          {customer && (
            <Alert severity="success" sx={{ mt: 2 }}>
              Showing payment data for {customer.name} ({customer.customerId}).
            </Alert>
          )}
        </CardContent>
      </Card>

      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" gutterBottom>
                Total Payments
              </Typography>
              <Typography variant="h4" sx={{ fontWeight: 700 }}>
                {summary.total}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" gutterBottom>
                Successful Payments
              </Typography>
              <Typography variant="h4" sx={{ fontWeight: 700, color: 'success.main' }}>
                {summary.successful}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" gutterBottom>
                Failed Payments
              </Typography>
              <Typography variant="h4" sx={{ fontWeight: 700, color: 'error.main' }}>
                {summary.failed}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" gutterBottom>
                Pending / Other
              </Typography>
              <Typography variant="h4" sx={{ fontWeight: 700, color: 'warning.main' }}>
                {summary.pending}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
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
                <Button variant="contained" onClick={handleApplyFilters} disabled={loading}>
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
          {loading ? (
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
              sx={{ border: 'none' }}
            />
          )}
        </CardContent>
      </Card>
    </Box>
  );
}

export default Analytics;

