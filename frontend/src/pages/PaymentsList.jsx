import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { alpha } from '@mui/material/styles';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  CircularProgress,
  MenuItem,
  InputAdornment,
  Chip,
  useTheme,
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import { Add, Search } from '@mui/icons-material';
import { paymentAPI } from '../services/api';
import { format } from 'date-fns';
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
    '& .high-risk-row': {
      backgroundColor: 'rgba(239,68,68,0.08)',
      '&:hover': { backgroundColor: 'rgba(239,68,68,0.14)' },
    },
  };
};

function PaymentsList() {
  const theme = useTheme();
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

      // Convert datetime-local format (YYYY-MM-DDTHH:mm) to ISO 8601 format (YYYY-MM-DDTHH:mm:ss)
      const formatDateToISO8601 = (dateString) => {
        if (!dateString) return '';
        // datetime-local format: "2026-07-31T10:30"
        // ISO 8601 format: "2026-07-31T10:30:00"
        return dateString.includes('T') ? dateString + ':00' : dateString;
      };

      const params = {
        limit: paginationModel.pageSize,
        offset: paginationModel.page * paginationModel.pageSize,
        ...(filters.status && { status: filters.status }),
        ...(filters.currency && { currency: filters.currency }),
        ...(filters.account && { account: filters.account }),
        ...(filters.dateFrom && { 'date-from': formatDateToISO8601(filters.dateFrom) }),
        ...(filters.dateTo && { 'date-to': formatDateToISO8601(filters.dateTo) }),
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
      width: 140,
      renderCell: (params) => <StatusBadge status={params.value} />,
    },
    {
      field: 'risk',
      headerName: 'Risk',
      width: 190,
      sortable: false,
      renderCell: (params) => {
        const isHighRisk = Boolean(params.row.highRisk);
        const probability = params.row.fraudProbability;
        if (!isHighRisk) {
          return <Typography variant="caption" color="text.secondary">Normal</Typography>;
        }

        return (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Chip label="High Risk" size="small" color="error" variant="outlined" />
            <Typography variant="caption" sx={{ color: '#FCA5A5', fontWeight: 600 }}>
              {typeof probability === 'number' ? `${probability.toFixed(2)}%` : '>=80%'}
            </Typography>
          </Box>
        );
      },
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
      <Card sx={{ overflow: 'hidden', p: 0 }}>
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
            getRowClassName={(params) => (params.row.highRisk ? 'high-risk-row' : '')}
            sx={{ height: 600, ...getGridSx(theme) }}
          />
        )}
      </Card>
    </Box>
  );
}

export default PaymentsList;

