import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { alpha } from '@mui/material/styles';
import {
  Alert,
  Box,
  Card,
  CardContent,
  Grid,
  Typography,
  CircularProgress,
  Button,
  Chip,
  Divider,
  Stack,
  useTheme,
} from '@mui/material';
import { CheckCircle, Error as ErrorIcon } from '@mui/icons-material';
import { paymentAPI } from '../services/api';
import { format } from 'date-fns';
import { toast } from 'react-toastify';
import { useCustomer } from '../context/CustomerContext';
import {
  extractReceiptDownloadError,
  isSuccessfulPaymentStatus,
  saveReceiptPdf,
} from '../utils/receiptDownload';

const statusColors = {
  CREATED: 'default',
  VALIDATED: 'info',
  SENT: 'warning',
  COMPLETED: 'success',
  FAILED: 'error',
};

function PaymentDetail() {
  const theme = useTheme();
  const { id } = useParams();
  const navigate = useNavigate();
  const { customerId } = useCustomer();
  const [payment, setPayment] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [downloadingReceipt, setDownloadingReceipt] = useState(false);
  const [receiptError, setReceiptError] = useState('');

  useEffect(() => {
    fetchPaymentDetails();
  }, [id]);

  const fetchPaymentDetails = async () => {
    try {
      setLoading(true);
      const [paymentRes, historyRes] = await Promise.all([
        paymentAPI.getPayment(id),
        paymentAPI.getPaymentHistory(id),
      ]);

      setPayment(paymentRes.data);
      setHistory(Array.isArray(historyRes.data) ? historyRes.data : []);

    } catch (error) {
      console.error('Error fetching payment details:', error);
    } finally {
      setLoading(false);
    }
  };

  const fraudProbability = payment?.fraudProbability != null && !Number.isNaN(Number(payment.fraudProbability))
    ? Number(payment.fraudProbability).toFixed(2)
    : null;
  const isHighRisk = fraudProbability !== null && Number(fraudProbability) >= 80;
  const handleDownloadReceipt = async () => {
    if (!customerId) {
      setReceiptError('Load a customer profile before downloading a receipt.');
      return;
    }

    try {
      setDownloadingReceipt(true);
      setReceiptError('');

      const response = await paymentAPI.downloadReceipt(customerId, id);
      saveReceiptPdf(response, payment?.id || id);
      toast.success('Receipt downloaded successfully');
    } catch (error) {
      const message = await extractReceiptDownloadError(error, 'Unable to download receipt');
      setReceiptError(message);
      toast.error(message);
    } finally {
      setDownloadingReceipt(false);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!payment) {
    return (
      <Box>
        <Typography>Payment not found</Typography>
        <Button onClick={() => navigate('/payments')}>Back to Payments</Button>
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Payment Details
        </Typography>
        <Button variant="outlined" onClick={() => navigate('/payments')}>
          Back
        </Button>
      </Box>

      <Grid container spacing={3}>
        <Grid item xs={12} lg={8}>
          {/* Payment Information */}
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Payment Information
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={6}>
                  <Typography variant="body2" color="textSecondary">
                    Payment ID
                  </Typography>
                  <Typography variant="body1" sx={{ fontWeight: 600 }}>
                    {payment.id}
                  </Typography>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Typography variant="body2" color="textSecondary">
                    Status
                  </Typography>
                  <Chip
                    label={payment.status}
                    color={statusColors[payment.status] || 'default'}
                    variant="outlined"
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Typography variant="body2" color="textSecondary">
                    From Account
                  </Typography>
                  <Typography variant="body1" sx={{ fontWeight: 600 }}>
                    {payment.sourceAccount}
                  </Typography>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Typography variant="body2" color="textSecondary">
                    To Account
                  </Typography>
                  <Typography variant="body1" sx={{ fontWeight: 600 }}>
                    {payment.destinationAccount}
                  </Typography>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Typography variant="body2" color="textSecondary">
                    Amount
                  </Typography>
                  <Typography variant="h6" sx={{ fontWeight: 600 }}>
                    {payment.amount} {payment.currency}
                  </Typography>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Typography variant="body2" color="textSecondary">
                    Created
                  </Typography>
                  <Typography variant="body1">
                    {format(new Date(payment.createdAt), 'MMM dd, yyyy HH:mm:ss')}
                  </Typography>
                </Grid>
              </Grid>
            </CardContent>
          </Card>

          {/* Status Timeline */}
          <Card sx={isHighRisk ? { border: '1px solid rgba(239,68,68,0.35)', backgroundColor: 'rgba(239,68,68,0.04)' } : undefined}>
            <CardContent>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6" sx={{ fontWeight: 700 }}>
                  Status History
                </Typography>
                {isHighRisk && (
                  <Chip label="High Risk Transaction" size="small" color="error" variant="outlined" />
                )}
              </Box>
              {history && history.length > 0 ? (
                <Stack spacing={2}>
                  {history.map((item, idx) => (
                    <Box
                      key={idx}
                      sx={{
                        display: 'flex',
                        gap: 2,
                        pb: idx < history.length - 1 ? 2 : 0,
<<<<<<< Updated upstream
                        borderBottom: idx < history.length - 1
                          ? isHighRisk
                            ? '1px solid rgba(239,68,68,0.25)'
                            : '1px solid rgba(99,102,241,0.15)'
                          : 'none',
=======
                        borderBottom: idx < history.length - 1 ? `1px solid ${alpha(theme.palette.primary.main, 0.2)}` : 'none',
>>>>>>> Stashed changes
                      }}
                    >
                      <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                        <Box
                          sx={{
                            width: 40,
                            height: 40,
                            borderRadius: '50%',
                            backgroundColor: item.newStatus === 'COMPLETED' ? 'success.lighter' : 'info.lighter',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: item.newStatus === 'COMPLETED' ? 'success.main' : 'info.main',
                          }}
                        >
                          <CheckCircle sx={{ fontSize: 24 }} />
                        </Box>
                        {idx < history.length - 1 && (
                          <Box
                            sx={{
                              width: 2,
                              height: 20,
                              backgroundColor: alpha(theme.palette.primary.main, 0.35),
                              mt: 1,
                            }}
                          />
                        )}
                      </Box>
                      <Box sx={{ pt: 0.5, flex: 1 }}>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          {item.oldStatus} → {item.newStatus}
                        </Typography>
                        <Typography variant="caption" color="textSecondary">
                          {format(new Date(item.timestamp), 'MMM dd, yyyy HH:mm:ss')}
                        </Typography>
                      </Box>
                    </Box>
                  ))}
                </Stack>
              ) : (
                <Typography variant="body2" color="textSecondary">
                  No status history available
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Sidebar */}
        <Grid item xs={12} lg={4}>
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Quick Actions
              </Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                {isSuccessfulPaymentStatus(payment.status) && (
                  <Button
                    variant="contained"
                    fullWidth
                    onClick={handleDownloadReceipt}
                    disabled={downloadingReceipt}
                  >
                    {downloadingReceipt ? <CircularProgress size={20} color="inherit" /> : 'Download Receipt'}
                  </Button>
                )}
                {payment.status === 'CREATED' && (
                  <Button variant="contained" fullWidth>
                    Validate
                  </Button>
                )}
                {payment.status === 'VALIDATED' && (
                  <Button variant="contained" fullWidth>
                    Send
                  </Button>
                )}
                {payment.status === 'SENT' && (
                  <Button variant="contained" fullWidth>
                    Complete
                  </Button>
                )}
                {payment.status === 'FAILED' && (
                  <Button variant="contained" fullWidth>
                    Retry
                  </Button>
                )}
              </Box>

              {receiptError && (
                <Alert severity="error" sx={{ mt: 2 }}>
                  {receiptError}
                </Alert>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Details
              </Typography>
              <Box sx={{ space: 1 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2">Idempotency Key:</Typography>
                  <Typography variant="body2" sx={{ fontWeight: 500 }}>
                    {payment.idempotencyKey || 'N/A'}
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2">Last Updated:</Typography>
                  <Typography variant="body2" sx={{ fontWeight: 500 }}>
                    {format(new Date(payment.updatedAt), 'MMM dd, yyyy')}
                  </Typography>
                </Box>
                {payment.status === 'COMPLETED' && (
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
                    <Typography variant="body2">Fraud Probability:</Typography>
                    <Typography variant="body2" sx={{ fontWeight: 500 }}>
                      {fraudProbability !== null ? `${fraudProbability}%` : 'N/A'}
                    </Typography>
                  </Box>
                )}
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}

export default PaymentDetail;

