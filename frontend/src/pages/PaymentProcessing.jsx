import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Grid,
  Typography,
  Button,
  CircularProgress,
  Alert,
} from '@mui/material';
import { Refresh, CheckCircle, Error as ErrorIcon } from '@mui/icons-material';
import { paymentAPI } from '../services/api';
import { toast } from 'react-toastify';
import PaymentStatusFlow from '../components/PaymentStatusFlow';
import { useCustomer } from '../context/CustomerContext';
import {
  extractReceiptDownloadError,
  isSuccessfulPaymentStatus,
  saveReceiptPdf,
} from '../utils/receiptDownload';

/**
 * PaymentProcessing Component
 *
 * Handles the automated payment processing workflow:
 * 1. Payment is already created
 * 2. Auto-transitions through: VALIDATING → VALIDATED → SENDING → SENT → COMPLETING → COMPLETED
 * 3. Handles failures with retry logic
 * 4. Provides real-time status updates via polling
 */
function PaymentProcessing() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { customerId } = useCustomer();

  // State management
  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [processingPhase, setProcessingPhase] = useState(null);
  const [errorDetails, setErrorDetails] = useState(null);
  const [retryCount, setRetryCount] = useState(0);
  const [autoProcessing, setAutoProcessing] = useState(true);
  const [pollingInterval, setPollingInterval] = useState(null);
  const [downloadingReceipt, setDownloadingReceipt] = useState(false);
  const [receiptError, setReceiptError] = useState('');

  // Configuration
  const MAX_RETRIES = 3;
  const POLLING_INTERVAL = 2000; // 2 seconds
  const AUTO_TRANSITION_DELAY = 1500; // Delay before auto-transitioning to next step

  /**
   * Fetch current payment status
   */
  const fetchPaymentStatus = useCallback(async () => {
    try {
      const response = await paymentAPI.getPayment(id);
      return response.data;
    } catch (error) {
      console.error('Error fetching payment status:', error);
      throw error;
    }
  }, [id]);

  /**
   * Initialize payment processing
   */
  const initializeProcessing = useCallback(async () => {
    try {
      const paymentData = await fetchPaymentStatus();
      setPayment(paymentData);
      setLoading(false);

      // If payment is in terminal state, stop processing
      if (paymentData.status === 'COMPLETED' || paymentData.status === 'FAILED') {
        setAutoProcessing(false);
        if (paymentData.status === 'FAILED') {
          setErrorDetails({
            code: paymentData.errorCode,
            message: paymentData.errorMessage,
          });
        }
      }
    } catch (error) {
      setLoading(false);
      setErrorDetails({ message: 'Failed to fetch payment status' });
    }
  }, [fetchPaymentStatus]);

  /**
   * Process payment through workflow states
   *
   * NOTE: Backend scheduler automatically handles all state transitions:
   * CREATED → VALIDATED → SENT → COMPLETED/FAILED
   *
   * Frontend now only monitors status and updates the UI accordingly.
   * No manual API calls to validate/send/complete endpoints.
   */
  const processPaymentWorkflow = useCallback(async (currentPayment) => {
    try {
      let paymentData = currentPayment || (await fetchPaymentStatus());
      setPayment(paymentData);

      // Update UI phase based on current payment status
      if (paymentData.status === 'CREATED') {
        setProcessingPhase('validating');
      } else if (paymentData.status === 'VALIDATED') {
        setProcessingPhase('sending');
      } else if (paymentData.status === 'SENT') {
        setProcessingPhase('completing');
      } else if (paymentData.status === 'COMPLETED') {
        setProcessingPhase(null);
        setAutoProcessing(false);
        toast.success('Payment completed successfully!');
      } else if (paymentData.status === 'FAILED') {
        setProcessingPhase(null);
        setAutoProcessing(false);
        if (!errorDetails) {
          setErrorDetails({
            code: paymentData.errorCode,
            message: paymentData.errorMessage,
          });
        }
      }
    } catch (error) {
      console.error('Workflow error:', error);
      setErrorDetails({ message: 'An error occurred during processing' });
      setAutoProcessing(false);
    }
  }, [fetchPaymentStatus, errorDetails]);

  /**
   * Handle manual retry
   */
  const handleManualRetry = async () => {
    setRetryCount(0);
    setErrorDetails(null);
    setProcessingPhase(null);

    // Reset to previous validation state to retry send/complete
    if (payment?.status === 'FAILED') {
      // Determine what was being attempted
      const historyResponse = await paymentAPI.getPaymentHistory(id);
      const history = Array.isArray(historyResponse.data)
        ? historyResponse.data
        : [];

      if (history.length > 0) {
        const lastStatus = history[history.length - 1]?.oldStatus;
        if (lastStatus === 'VALIDATED') {
          setPayment((prev) => ({ ...prev, status: 'VALIDATED', errorCode: null, errorMessage: null }));
          processPaymentWorkflow({ ...payment, status: 'VALIDATED', errorCode: null, errorMessage: null });
        } else if (lastStatus === 'SENT') {
          setPayment((prev) => ({ ...prev, status: 'SENT', errorCode: null, errorMessage: null }));
          processPaymentWorkflow({ ...payment, status: 'SENT', errorCode: null, errorMessage: null });
        }
      }
    }
  };

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

  /**
   * Initial effect: Load payment and start processing
   */
  useEffect(() => {
    initializeProcessing();
  }, [initializeProcessing]);

  /**
   * Auto-process workflow when payment is loaded
   */
  useEffect(() => {
    if (payment && autoProcessing) {
      processPaymentWorkflow(payment);
    }
  }, [payment, autoProcessing, processPaymentWorkflow]);

  /**
   * Polling effect: Check payment status periodically
   */
  useEffect(() => {
    if (!autoProcessing) return;

    const interval = setInterval(async () => {
      try {
        const updatedPayment = await fetchPaymentStatus();
        setPayment(updatedPayment);

        // If payment reached terminal state, update workflow to show correct UI
        if (updatedPayment.status === 'COMPLETED' || updatedPayment.status === 'FAILED') {
          setAutoProcessing(false);
          // CRITICAL: Call workflow to update processingPhase and show correct UI
          // This ensures the spinning loader stops and shows completion/failure state
          await processPaymentWorkflow(updatedPayment);
        }
      } catch (error) {
        console.error('Polling error:', error);
      }
    }, POLLING_INTERVAL);

    setPollingInterval(interval);

    return () => clearInterval(interval);
  }, [autoProcessing, fetchPaymentStatus, processPaymentWorkflow]);

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
        <Alert severity="error" sx={{ mb: 2 }}>
          Payment not found
        </Alert>
        <Button onClick={() => navigate('/payments')}>Back to Payments List</Button>
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Payment Processing
        </Typography>
        <Button variant="outlined" onClick={() => navigate('/payments')}>
          Back to Payments
        </Button>
      </Box>

      <Grid container spacing={3}>
        {/* Main Content */}
        <Grid item xs={12} lg={8}>
          {/* Status Flow */}
          <PaymentStatusFlow
            currentStatus={payment.status}
            processingPhase={processingPhase}
            errorDetails={errorDetails}
            retryCount={retryCount}
            maxRetries={MAX_RETRIES}
          />

          {/* Payment Details Card */}
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Payment Details
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
                    Amount
                  </Typography>
                  <Typography variant="h6" sx={{ fontWeight: 600 }}>
                    {payment.amount} {payment.currency}
                  </Typography>
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
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* Sidebar */}
        <Grid item xs={12} lg={4}>
          {/* Status Card */}
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Current Status
              </Typography>
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  p: 3,
                  backgroundColor:
                    payment.status === 'COMPLETED'
                      ? 'success.lighter'
                      : payment.status === 'FAILED'
                      ? 'error.lighter'
                      : 'info.lighter',
                  borderRadius: 2,
                  mb: 2,
                }}
              >
                {isSuccessfulPaymentStatus(payment.status) && (
                  <Box sx={{ textAlign: 'center', color: 'success.main' }}>
                    <CheckCircle sx={{ fontSize: 48, mb: 1 }} />
                    <Typography variant="h6" sx={{ fontWeight: 700 }}>
                      {payment.status}
                    </Typography>
                  </Box>
                )}
                {payment.status === 'FAILED' && (
                  <Box sx={{ textAlign: 'center', color: 'error.main' }}>
                    <ErrorIcon sx={{ fontSize: 48, mb: 1 }} />
                    <Typography variant="h6" sx={{ fontWeight: 700 }}>
                      {payment.status}
                    </Typography>
                  </Box>
                )}
                {!(isSuccessfulPaymentStatus(payment.status) || payment.status === 'FAILED') && (
                  <Box sx={{ textAlign: 'center', color: 'info.main' }}>
                    <CircularProgress size={48} sx={{ mb: 1 }} />
                    <Typography variant="h6" sx={{ fontWeight: 700 }}>
                      {processingPhase ? processingPhase.toUpperCase() : payment.status}
                    </Typography>
                  </Box>
                )}
              </Box>

              {/* Action Buttons */}
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                {payment.status === 'FAILED' && (
                  <>
                    <Button
                      variant="contained"
                      color="primary"
                      onClick={handleManualRetry}
                      startIcon={<Refresh />}
                      fullWidth
                    >
                      Retry Payment
                    </Button>
                    <Button
                      variant="outlined"
                      onClick={() => navigate('/payments')}
                      fullWidth
                    >
                      Back to List
                    </Button>
                  </>
                )}
                {isSuccessfulPaymentStatus(payment.status) && (
                  <>
                    <Button
                      variant="contained"
                      color="success"
                      onClick={handleDownloadReceipt}
                      disabled={downloadingReceipt}
                      fullWidth
                    >
                      {downloadingReceipt ? <CircularProgress size={20} color="inherit" /> : 'Download Receipt'}
                    </Button>
                    <Button
                      variant="outlined"
                      onClick={() => navigate('/payments')}
                      fullWidth
                    >
                      Back to List
                    </Button>
                  </>
                )}
              </Box>

              {receiptError && (
                <Alert severity="error" sx={{ mt: 2 }}>
                  {receiptError}
                </Alert>
              )}
            </CardContent>
          </Card>

          {/* Info Card section removed - was displaying Processing Info, Retry Attempts, Processing Status, and Error Details */}
        </Grid>
      </Grid>
    </Box>
  );
}

export default PaymentProcessing;
