import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Chip,
  CircularProgress,
  Grid,
  MenuItem,
  InputAdornment,
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import { Add, Search, FilterList } from '@mui/icons-material';
import { paymentAPI } from '../services/api';
import { format } from 'date-fns';

const statusColors = {
  CREATED: 'default',
  VALIDATED: 'info',
  SENT: 'warning',
  COMPLETED: 'success',
  FAILED: 'error',
};

function PaymentsList() {
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [paginationModel, setPaginationModel] = useState({ pageSize: 10, page: 0 });
  const [filters, setFilters] = useState({
    status: '',
    currency: '',
    account: '',
    dateFrom: '',
    dateTo: '',
  });

  useEffect(() => {
    fetchPayments();
  }, [paginationModel, filters]);

  const fetchPayments = async () => {
    try {
      setLoading(true);
      const params = {
        limit: paginationModel.pageSize,
        offset: paginationModel.page * paginationModel.pageSize,
        ...(filters.status && { status: filters.status }),
        ...(filters.currency && { currency: filters.currency }),
        ...(filters.account && { account: filters.account }),
        ...(filters.dateFrom && { 'date-from': filters.dateFrom }),
        ...(filters.dateTo && { 'date-to': filters.dateTo }),
      };

      const response = await paymentAPI.listPayments(params);
      const data = Array.isArray(response.data) ? response.data : [];

      setPayments(data.map((p, idx) => ({
        id: p.id || idx,
        ...p,
      })));
    } catch (error) {
      console.error('Error fetching payments:', error);
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    { field: 'id', headerName: 'ID', width: 100 },
    { field: 'sourceAccount', headerName: 'From Account', width: 140 },
    { field: 'destinationAccount', headerName: 'To Account', width: 140 },
    {
      field: 'amount',
      headerName: 'Amount',
      width: 120,
      renderCell: (params) => `${params.value} ${params.row.currency}`,
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 120,
      renderCell: (params) => (
        <Chip
          label={params.value}
          size="small"
          color={statusColors[params.value] || 'default'}
          variant="outlined"
        />
      ),
    },
    {
      field: 'createdAt',
      headerName: 'Created',
      width: 180,
      renderCell: (params) => format(new Date(params.value), 'MMM dd, yyyy HH:mm'),
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 120,
      sortable: false,
      renderCell: (params) => (
        <Button
          component={Link}
          to={`/payments/${params.row.id}`}
          size="small"
          variant="outlined"
        >
          View
        </Button>
      ),
    },
  ];

  return (
    <Box>
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Payments
        </Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          component={Link}
          to="/payments/create"
        >
          New Payment
        </Button>
      </Box>

      {/* Filters */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'flex-end' }}>
            <TextField
              label="Status"
              select
              size="small"
              value={filters.status}
              onChange={(e) => {
                setFilters({ ...filters, status: e.target.value });
                setPaginationModel({ ...paginationModel, page: 0 });
              }}
              sx={{ minWidth: 120 }}
            >
              <MenuItem value="">All</MenuItem>
              <MenuItem value="CREATED">Created</MenuItem>
              <MenuItem value="VALIDATED">Validated</MenuItem>
              <MenuItem value="SENT">Sent</MenuItem>
              <MenuItem value="COMPLETED">Completed</MenuItem>
              <MenuItem value="FAILED">Failed</MenuItem>
            </TextField>

            <TextField
              label="Currency"
              size="small"
              value={filters.currency}
              onChange={(e) => {
                setFilters({ ...filters, currency: e.target.value });
                setPaginationModel({ ...paginationModel, page: 0 });
              }}
              sx={{ minWidth: 100 }}
            />

            <TextField
              label="Account"
              size="small"
              value={filters.account}
              onChange={(e) => {
                setFilters({ ...filters, account: e.target.value });
                setPaginationModel({ ...paginationModel, page: 0 });
              }}
              sx={{ minWidth: 150 }}
              InputProps={{
                startAdornment: <InputAdornment position="start"><Search fontSize="small" /></InputAdornment>,
              }}
            />

            <TextField
              label="From Date"
              type="datetime-local"
              size="small"
              value={filters.dateFrom}
              onChange={(e) => {
                setFilters({ ...filters, dateFrom: e.target.value });
                setPaginationModel({ ...paginationModel, page: 0 });
              }}
              InputLabelProps={{ shrink: true }}
              sx={{ minWidth: 200 }}
            />

            <TextField
              label="To Date"
              type="datetime-local"
              size="small"
              value={filters.dateTo}
              onChange={(e) => {
                setFilters({ ...filters, dateTo: e.target.value });
                setPaginationModel({ ...paginationModel, page: 0 });
              }}
              InputLabelProps={{ shrink: true }}
              sx={{ minWidth: 200 }}
            />

            <Button
              variant="outlined"
              size="small"
              onClick={() => {
                setFilters({
                  status: '',
                  currency: '',
                  account: '',
                  dateFrom: '',
                  dateTo: '',
                });
                setPaginationModel({ ...paginationModel, page: 0 });
              }}
            >
              Clear
            </Button>
          </Box>
        </CardContent>
      </Card>

      {/* Data Table */}
      <Card>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : (
          <DataGrid
            rows={payments}
            columns={columns}
            paginationModel={paginationModel}
            onPaginationModelChange={setPaginationModel}
            pageSizeOptions={[10, 25, 50]}
            sx={{ height: 600, border: 'none' }}
          />
        )}
      </Card>
    </Box>
  );
}

export default PaymentsList;

