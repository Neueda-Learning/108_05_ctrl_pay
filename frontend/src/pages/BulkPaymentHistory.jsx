import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
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
  useTheme,
  Grid,
  Chip,
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import { Add, Search, Visibility } from '@mui/icons-material';
import api from '../services/api';
import { format } from 'date-fns';

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

const getStatusColor = (status) => {
  switch (status) {
    case 'COMPLETED':
      return 'success';
    case 'PARTIALLY_COMPLETED':
      return 'warning';
    case 'FAILED':
      return 'error';
    case 'PROCESSING':
    case 'VALIDATING':
      return 'info';
    default:
      return 'default';
  }
};

function BulkPaymentHistory() {
  const theme = useTheme();
  const custom = theme.customTokens;
  const navigate = useNavigate();

  const [batches, setBatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [paginationModel, setPaginationModel] = useState({ pageSize: 10, page: 0 });
  const [filters, setFilters] = useState({
    batchReference: '',
    sourceAccount: '',
    status: '',
    dateFrom: '',
    dateTo: '',
  });

  useEffect(() => {
    fetchBatches();
  }, [paginationModel, filters]);

  const fetchBatches = async () => {
    try {
      setLoading(true);

      const params = {
        limit: paginationModel.pageSize,
        offset: paginationModel.page * paginationModel.pageSize,
        ...(filters.batchReference && { 'batch-reference': filters.batchReference }),
        ...(filters.sourceAccount && { 'source-account': filters.sourceAccount }),
        ...(filters.status && { status: filters.status }),
        ...(filters.dateFrom && { 'date-from': filters.dateFrom }),
        ...(filters.dateTo && { 'date-to': filters.dateTo }),
      };

      const response = await api.get('/bulk-payments/history', { params });
      const data = Array.isArray(response.data) ? response.data : [];

      setBatches(data.map((batch, idx) => ({
        id: batch.batchId || idx,
        ...batch,
      })));
    } catch (error) {
      console.error('Error fetching bulk payment history:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (field) => (event) => {
    setFilters(prev => ({
      ...prev,
      [field]: event.target.value
    }));
    setPaginationModel({ pageSize: 10, page: 0 });
  };

  const handleViewDetails = (batchId) => {
    navigate(`/bulk-payments/${batchId}`);
  };

  const columns = [
    { field: 'batchReference', headerName: 'Batch Reference', width: 140 },
    { field: 'sourceAccount', headerName: 'Source Account', width: 140 },
    {
      field: 'totalTransactions',
      headerName: 'Total Transactions',
      width: 130,
      type: 'number',
      align: 'center',
    },
    {
      field: 'successfulCount',
      headerName: 'Successful',
      width: 110,
      type: 'number',
      align: 'center',
      renderCell: (params) => (
        <Chip
          label={params.value}
          size="small"
          color="success"
          variant="outlined"
        />
      ),
    },
    {
      field: 'failedCount',
      headerName: 'Failed',
      width: 100,
      type: 'number',
      align: 'center',
      renderCell: (params) => (
        <Chip
          label={params.value}
          size="small"
          color="error"
          variant="outlined"
        />
      ),
    },
    {
      field: 'totalAmount',
      headerName: 'Total Amount',
      width: 130,
      renderCell: (params) => params.value ? `${parseFloat(params.value).toFixed(2)}` : '0.00',
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 140,
      renderCell: (params) => (
        <Chip
          label={params.value}
          size="small"
          color={getStatusColor(params.value)}
          variant="outlined"
        />
      ),
    },
    {
      field: 'createdAt',
      headerName: 'Created Date',
      width: 160,
      renderCell: (params) => params.value ? format(new Date(params.value), 'MMM dd, yyyy HH:mm') : 'N/A',
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 100,
      sortable: false,
      renderCell: (params) => (
        <Button
          size="small"
          startIcon={<Visibility />}
          onClick={() => handleViewDetails(params.row.id)}
          sx={{ textTransform: 'none' }}
        >
          View
        </Button>
      ),
    },
  ];

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
            Bulk Payment History
          </Typography>
          <Typography variant="body2" color="text.secondary">
            View and manage all bulk payment batches
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => navigate('/payments/bulk')}
        >
          New Bulk Payment
        </Button>
      </Box>

      {/* Filters */}
      <Card sx={{ mb: 3, border: `1px solid ${alpha(custom.border, 0.5)}` }}>
        <CardContent>
          <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 2 }}>
            Filters
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                fullWidth
                size="small"
                label="Batch Reference"
                value={filters.batchReference}
                onChange={handleFilterChange('batchReference')}
                placeholder="e.g., BP202608051234"
                InputProps={{
                  startAdornment: <Search style={{ marginRight: 8, opacity: 0.5 }} />,
                }}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                fullWidth
                size="small"
                label="Source Account"
                value={filters.sourceAccount}
                onChange={handleFilterChange('sourceAccount')}
                placeholder="12-digit account"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                fullWidth
                size="small"
                select
                label="Status"
                value={filters.status}
                onChange={handleFilterChange('status')}
              >
                <MenuItem value="">All Statuses</MenuItem>
                <MenuItem value="CREATED">Created</MenuItem>
                <MenuItem value="VALIDATING">Validating</MenuItem>
                <MenuItem value="VALIDATED">Validated</MenuItem>
                <MenuItem value="PROCESSING">Processing</MenuItem>
                <MenuItem value="COMPLETED">Completed</MenuItem>
                <MenuItem value="PARTIALLY_COMPLETED">Partially Completed</MenuItem>
                <MenuItem value="FAILED">Failed</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Button
                fullWidth
                variant="outlined"
                onClick={() => {
                  setFilters({
                    batchReference: '',
                    sourceAccount: '',
                    status: '',
                    dateFrom: '',
                    dateTo: '',
                  });
                  setPaginationModel({ pageSize: 10, page: 0 });
                }}
              >
                Clear Filters
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Table */}
      <Card sx={{ border: `1px solid ${alpha(custom.border, 0.5)}` }}>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 400 }}>
            <CircularProgress />
          </Box>
        ) : (
          <Box sx={{ height: 600, width: '100%' }}>
            <DataGrid
              rows={batches}
              columns={columns}
              paginationModel={paginationModel}
              onPaginationModelChange={setPaginationModel}
              pageSizeOptions={[5, 10, 25, 50]}
              sx={getGridSx(theme)}
              disableSelectionOnClick
              density="compact"
            />
          </Box>
        )}
      </Card>
    </Box>
  );
}

export default BulkPaymentHistory;

