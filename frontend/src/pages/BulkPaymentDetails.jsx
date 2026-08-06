import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { alpha } from '@mui/material/styles';
import {
  Box,
  Card,
  CardContent,
  Grid,
  Typography,
  Button,
  CircularProgress,
  Tabs,
  Tab,
  Alert,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  useTheme,
  Divider,
} from '@mui/material';
import { ArrowBack, Visibility } from '@mui/icons-material';
import api from '../services/api';
import { format } from 'date-fns';

const getStatusColor = (status) => {
  switch (status) {
    case 'SUCCESS':
    case 'COMPLETED':
      return 'success';
    case 'PARTIALLY_COMPLETED':
      return 'warning';
    case 'FAILED':
    case 'PENDING':
      return 'error';
    case 'PROCESSING':
    case 'VALIDATING':
    case 'VALIDATED':
    case 'SENT':
      return 'info';
    default:
      return 'default';
  }
};

function BulkPaymentDetails() {
  const theme = useTheme();
  const custom = theme.customTokens;
  const { batchId } = useParams();
  const navigate = useNavigate();

  const [batch, setBatch] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState(0);
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    fetchBatchDetails();
    // Set up polling if batch is still processing
    const interval = setInterval(() => {
      if (batch?.batchStatus === 'PROCESSING' || batch?.batchStatus === 'VALIDATING') {
        fetchBatchDetailsWithoutLoading();
      }
    }, 3000);
    return () => clearInterval(interval);
  }, [batchId, batch?.batchStatus]);

  const fetchBatchDetails = async () => {
    try {
      setLoading(true);
      const response = await api.get(`/bulk-payments/${batchId}/status`);
      setBatch(response.data);
    } catch (error) {
      console.error('Error fetching batch details:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchBatchDetailsWithoutLoading = async () => {
    try {
      const response = await api.get(`/bulk-payments/${batchId}/status`);
      setBatch(response.data);
    } catch (error) {
      console.error('Error refreshing batch details:', error);
    }
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    await fetchBatchDetails();
    setRefreshing(false);
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!batch) {
    return (
      <Box>
        <Alert severity="error">Batch not found</Alert>
        <Button onClick={() => navigate('/payments/bulk-history')} sx={{ mt: 2 }}>
          Back to History
        </Button>
      </Box>
    );
  }

  const progressPercent = batch.progressPercentage || 0;
  const isProcessing = batch.batchStatus === 'PROCESSING' || batch.batchStatus === 'VALIDATING';

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 3, display: 'flex', alignItems: 'center', gap: 2 }}>
        <Button
          startIcon={<ArrowBack />}
          onClick={() => navigate('/payments/bulk-history')}
          sx={{ textTransform: 'none' }}
        >
          Back to History
        </Button>
        <Box flex={1}>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>
            Batch: {batch.batchReference}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            Created on {format(new Date(batch.createdAt), 'MMM dd, yyyy HH:mm:ss')}
          </Typography>
        </Box>
        <Button
          variant="outlined"
          onClick={handleRefresh}
          disabled={refreshing}
        >
          {refreshing ? 'Refreshing...' : 'Refresh'}
        </Button>
      </Box>

      {/* Section 1: Batch Summary */}
      <Card sx={{ mb: 3, border: `1px solid ${alpha(custom.border, 0.5)}` }}>
        <CardContent>
          <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>
            Batch Summary
          </Typography>

          <Grid container spacing={3} sx={{ mb: 3 }}>
            <Grid item xs={12} sm={6} md={3}>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Batch Reference
                </Typography>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  {batch.batchReference}
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Source Account
                </Typography>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  {batch.sourceAccount}
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Status
                </Typography>
                <Chip
                  label={batch.batchStatus}
                  size="small"
                  color={getStatusColor(batch.batchStatus)}
                  variant="outlined"
                  sx={{ mt: 0.5 }}
                />
              </Box>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Total Amount
                </Typography>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  {parseFloat(batch.totalAmount).toFixed(2)}
                </Typography>
              </Box>
            </Grid>
          </Grid>

          <Divider sx={{ my: 3 }} />

          {/* Progress */}
          {isProcessing && (
            <Box sx={{ mb: 3 }}>
              <Typography variant="body2" sx={{ fontWeight: 600, mb: 1 }}>
                Processing Progress: {progressPercent}%
              </Typography>
              <Box
                sx={{
                  width: '100%',
                  height: 8,
                  backgroundColor: alpha(custom.border, 0.3),
                  borderRadius: 4,
                  overflow: 'hidden',
                }}
              >
                <Box
                  sx={{
                    width: `${progressPercent}%`,
                    height: '100%',
                    backgroundColor: custom.brand.main,
                    transition: 'width 0.3s ease',
                  }}
                />
              </Box>
            </Box>
          )}

          {/* Stats Grid */}
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6} md={3}>
              <Box sx={{ p: 2, backgroundColor: alpha(custom.brand.main, 0.08), borderRadius: 1 }}>
                <Typography variant="caption" color="text.secondary">
                  Total Transactions
                </Typography>
                <Typography variant="h6" sx={{ fontWeight: 700, color: custom.brand.main }}>
                  {batch.totalTransactions}
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Box sx={{ p: 2, backgroundColor: alpha('#10b981', 0.08), borderRadius: 1 }}>
                <Typography variant="caption" color="text.secondary">
                  Completed
                </Typography>
                <Typography variant="h6" sx={{ fontWeight: 700, color: '#10b981' }}>
                  {batch.completedCount}
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Box sx={{ p: 2, backgroundColor: alpha('#ef4444', 0.08), borderRadius: 1 }}>
                <Typography variant="caption" color="text.secondary">
                  Failed
                </Typography>
                <Typography variant="h6" sx={{ fontWeight: 700, color: '#ef4444' }}>
                  {batch.failedCount}
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Box sx={{ p: 2, backgroundColor: alpha('#f59e0b', 0.08), borderRadius: 1 }}>
                <Typography variant="caption" color="text.secondary">
                  Processing
                </Typography>
                <Typography variant="h6" sx={{ fontWeight: 700, color: '#f59e0b' }}>
                  {batch.processingCount}
                </Typography>
              </Box>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Section 2: Individual Payments */}
      <Card sx={{ border: `1px solid ${alpha(custom.border, 0.5)}` }}>
        <CardContent>
          <Tabs
            value={activeTab}
            onChange={(e, newValue) => setActiveTab(newValue)}
            sx={{ mb: 3 }}
          >
            <Tab label={`All Payments (${batch.payments?.length || 0})`} />
            <Tab label={`Successful (${batch.payments?.filter(p => p.status === 'SUCCESS').length || 0})`} />
            <Tab label={`Failed (${batch.payments?.filter(p => p.status === 'FAILED').length || 0})`} />
          </Tabs>

          <TableContainer component={Paper} sx={{ backgroundColor: 'transparent' }}>
            <Table size="small">
              <TableHead sx={{ backgroundColor: custom.gridHeader }}>
                <TableRow>
                  <TableCell sx={{ fontWeight: 700, fontSize: '0.75rem' }}>Line</TableCell>
                  <TableCell sx={{ fontWeight: 700, fontSize: '0.75rem' }}>Payment ID</TableCell>
                  <TableCell sx={{ fontWeight: 700, fontSize: '0.75rem' }}>Destination Account</TableCell>
                  <TableCell sx={{ fontWeight: 700, fontSize: '0.75rem' }} align="right">Amount</TableCell>
                  <TableCell sx={{ fontWeight: 700, fontSize: '0.75rem' }}>Currency</TableCell>
                  <TableCell sx={{ fontWeight: 700, fontSize: '0.75rem' }}>Status</TableCell>
                  <TableCell sx={{ fontWeight: 700, fontSize: '0.75rem' }}>Failure Reason</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(batch.payments || [])
                  .filter((payment) => {
                    if (activeTab === 0) return true;
                    if (activeTab === 1) return payment.status === 'SUCCESS';
                    if (activeTab === 2) return payment.status === 'FAILED';
                    return true;
                  })
                  .map((payment) => (
                    <TableRow
                      key={payment.lineNumber}
                      sx={{
                        backgroundColor: custom.gridRow,
                        '&:hover': { backgroundColor: alpha(custom.brand.main, 0.08) },
                      }}
                    >
                      <TableCell>{payment.lineNumber}</TableCell>
                      <TableCell>
                        {payment.paymentId ? (
                          <Button
                            size="small"
                            variant="text"
                            endIcon={<Visibility />}
                            onClick={() => navigate(`/payments/${payment.paymentId}`)}
                            sx={{ textTransform: 'none', fontSize: '0.75rem' }}
                          >
                            {payment.paymentId}
                          </Button>
                        ) : (
                          'N/A'
                        )}
                      </TableCell>
                      <TableCell>{payment.destinationAccount}</TableCell>
                      <TableCell align="right">{parseFloat(payment.amount).toFixed(2)}</TableCell>
                      <TableCell>{payment.currency}</TableCell>
                      <TableCell>
                        <Chip
                          label={payment.status}
                          size="small"
                          color={getStatusColor(payment.status)}
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell sx={{ fontSize: '0.75rem', color: 'error.main' }}>
                        {payment.failureReason || '-'}
                      </TableCell>
                    </TableRow>
                  ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    </Box>
  );
}

export default BulkPaymentDetails;

