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
  LinearProgress,
  useTheme,
} from '@mui/material';
import { CheckCircle, Error as ErrorIcon, Security as SecurityIcon } from '@mui/icons-material';
import { paymentAPI, adminFraudAPI } from '../services/api';
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
  SUSPICIOUS: 'warning',
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
  const [fraudDetail, setFraudDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [downloadingReceipt, setDownloadingReceipt] = useState(false);
  const [receiptError, setReceiptError] = useState('');
  const [fraudAction, setFraudAction] = useState('');

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

      // Load fraud details if available
      try {
        const fraudRes = await adminFraudAPI.getPaymentFraudDetail(id);
        setFraudDetail(fraudRes.data);
      } catch {
        // No fraud assessment — normal for valid payments
      }
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
  const isSuspicious = payment?.status === 'SUSPICIOUS';

  // Construct complete history with synthesized SENT→COMPLETED transition
  const getCompleteHistory = () => {
    if (!history || history.length === 0) return [];

    let displayHistory = [...history];

    // If payment is COMPLETED but last history entry is SENT, add synthetic SENT→COMPLETED transition
    if (payment?.status === 'COMPLETED' && displayHistory.length > 0) {
      const lastEntry = displayHistory[displayHistory.length - 1];
      if (lastEntry.newStatus === 'SENT') {
        // Add synthesized SENT→COMPLETED transition with timestamp
        displayHistory.push({
          oldStatus: 'SENT',
          newStatus: 'COMPLETED',
          timestamp: payment.updatedAt || new Date().toISOString(),
        });
      }
    }

    return displayHistory;
  };

  const handleFraudApprove = async () => {
    try {
      setFraudAction('approving');
      await adminFraudAPI.approvePayment(id, { reviewedBy: 'admin', notes: 'Approved via payment detail view' });
      toast.success('Payment approved — transitioning to VALIDATED');
      fetchPaymentDetails();
    } catch (e) {
      toast.error(e.response?.data?.error || 'Approval failed');
    } finally { setFraudAction(''); }
  };

  const handleFraudReject = async () => {
    try {
      setFraudAction('rejecting');
      await adminFraudAPI.rejectPayment(id, { reviewedBy: 'admin', notes: 'Rejected via payment detail view — fraud confirmed' });
      toast.success('Payment rejected — moved to FAILED');
      fetchPaymentDetails();
    } catch (e) {
      toast.error(e.response?.data?.error || 'Rejection failed');
    } finally { setFraudAction(''); }
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

          {/* Fraud Assessment Card — shown if assessment exists */}
          {fraudDetail?.fraudAssessment && (
            <Card sx={{ mb: 3, border: isSuspicious ? '1px solid rgba(245,158,11,0.4)' : undefined }}>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                  <SecurityIcon sx={{ color: '#F59E0B' }} />
                  <Typography variant="h6" sx={{ fontWeight: 700 }}>Fraud Assessment</Typography>
                  <Chip
                    label={fraudDetail.fraudAssessment.decision}
                    size="small"
                    sx={{
                      ml: 'auto',
                      background: fraudDetail.fraudAssessment.decision === 'APPROVED'
                        ? 'rgba(16,185,129,0.15)' : 'rgba(245,158,11,0.15)',
                      color: fraudDetail.fraudAssessment.decision === 'APPROVED' ? '#10B981' : '#F59E0B',
                      fontWeight: 700,
                    }}
                  />
                </Box>
                <Grid container spacing={2}>
                  <Grid item xs={6} sm={3}>
                    <Typography variant="caption" color="textSecondary">Hybrid Score</Typography>
                    <Typography variant="h6" sx={{ fontWeight: 700, color: '#F59E0B' }}>
                      {Number(fraudDetail.fraudAssessment.hybridFraudScore).toFixed(1)}%
                    </Typography>
                  </Grid>
                  <Grid item xs={6} sm={3}>
                    <Typography variant="caption" color="textSecondary">Rule Score</Typography>
                    <Typography variant="body1" sx={{ fontWeight: 600 }}>
                      {Number(fraudDetail.fraudAssessment.ruleEngineScore).toFixed(1)}%
                    </Typography>
                  </Grid>
                  <Grid item xs={6} sm={3}>
                    <Typography variant="caption" color="textSecondary">ML Score</Typography>
                    <Typography variant="body1" sx={{ fontWeight: 600 }}>
                      {Number(fraudDetail.fraudAssessment.mlFraudProbability).toFixed(1)}%
                    </Typography>
                  </Grid>
                  <Grid item xs={6} sm={3}>
                    <Typography variant="caption" color="textSecondary">Risk Level</Typography>
                    <Typography variant="body1" sx={{ fontWeight: 600, color: '#F59E0B' }}>
                      {fraudDetail.fraudAssessment.riskLevel}
                    </Typography>
                  </Grid>
                  <Grid item xs={12}>
                    <Box sx={{ mt: 1 }}>
                      <Typography variant="caption" color="textSecondary">Fraud Score</Typography>
                      <LinearProgress
                        variant="determinate"
                        value={Math.min(100, Number(fraudDetail.fraudAssessment.hybridFraudScore))}
                        sx={{
                          mt: 0.5, height: 8, borderRadius: 4,
                          bgcolor: 'rgba(255,255,255,0.08)',
                          '& .MuiLinearProgress-bar': {
                            background: Number(fraudDetail.fraudAssessment.hybridFraudScore) >= 70
                              ? 'linear-gradient(90deg,#F59E0B,#EF4444)'
                              : 'linear-gradient(90deg,#10B981,#06B6D4)',
                          }
                        }}
                      />
                    </Box>
                  </Grid>
                  {fraudDetail.fraudAssessment.explanation && (
                    <Grid item xs={12}>
                      <Typography variant="caption" color="textSecondary">Explanation</Typography>
                      <Typography variant="body2" sx={{ mt: 0.5, color: '#94A3B8', fontSize: '0.8rem' }}>
                        {fraudDetail.fraudAssessment.explanation}
                      </Typography>
                    </Grid>
                  )}
                  {fraudDetail.fraudAssessment.reviewedBy && (
                    <Grid item xs={12}>
                      <Divider sx={{ my: 1 }} />
                      <Typography variant="caption" color="textSecondary">
                        Reviewed by <strong>{fraudDetail.fraudAssessment.reviewedBy}</strong>
                        {fraudDetail.fraudAssessment.reviewedAt && ` on ${format(new Date(fraudDetail.fraudAssessment.reviewedAt), 'MMM d, yyyy')}`}
                      </Typography>
                      {fraudDetail.fraudAssessment.reviewerNotes && (
                        <Typography variant="body2" sx={{ color: '#94A3B8', mt: 0.5 }}>
                          {fraudDetail.fraudAssessment.reviewerNotes}
                        </Typography>
                      )}
                    </Grid>
                  )}
                </Grid>
              </CardContent>
            </Card>
          )}

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
              {(() => {
                const completeHistory = getCompleteHistory();
                return completeHistory && completeHistory.length > 0 ? (
                  <Stack spacing={2}>
                    {completeHistory.map((item, idx) => (
                      <Box
                        key={idx}
                        sx={{
                          display: 'flex',
                          gap: 2,
                          pb: idx < completeHistory.length - 1 ? 2 : 0,
                          borderBottom: idx < completeHistory.length - 1
                            ? isHighRisk
                              ? '1px solid rgba(239,68,68,0.25)'
                              : `1px solid ${alpha(theme.palette.primary.main, 0.2)}`
                            : 'none',
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
                          {idx < completeHistory.length - 1 && (
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
                );
              })()}
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
                {/* Fraud review actions for SUSPICIOUS payments */}
                {isSuspicious && (
                  <Alert severity="warning" sx={{ mb: 1, fontSize: '0.8rem' }}>
                    ⚠️ This payment is pending fraud review.
                  </Alert>
                )}
                {isSuspicious && (
                  <Button
                    variant="contained"
                    fullWidth
                    color="success"
                    onClick={handleFraudApprove}
                    disabled={!!fraudAction}
                    sx={{ background: 'linear-gradient(135deg,#10B981,#059669)' }}
                  >
                    {fraudAction === 'approving' ? <CircularProgress size={18} /> : '✅ Approve Payment'}
                  </Button>
                )}
                {isSuspicious && (
                  <Button
                    variant="outlined"
                    fullWidth
                    color="error"
                    onClick={handleFraudReject}
                    disabled={!!fraudAction}
                  >
                    {fraudAction === 'rejecting' ? <CircularProgress size={18} /> : '🚫 Reject Payment'}
                  </Button>
                )}
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

