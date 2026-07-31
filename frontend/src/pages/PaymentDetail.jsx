import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Grid,
  Typography,
  CircularProgress,
  Button,
  Chip,
  Divider,
  Timeline,
  TimelineItem,
  TimelineSeparator,
  TimelineConnector,
  TimelineContent,
  TimelineDot,
  TimelineOppositeContent,
} from '@mui/material';
import { CheckCircle, Error as ErrorIcon } from '@mui/icons-material';
import { paymentAPI } from '../services/api';
import { format } from 'date-fns';

const statusColors = {
  CREATED: 'default',
  VALIDATED: 'info',
  SENT: 'warning',
  COMPLETED: 'success',
  FAILED: 'error',
};

function PaymentDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [payment, setPayment] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);

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
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Status History
              </Typography>
              {history && history.length > 0 ? (
                <Timeline position="alternate">
                  {history.map((item, idx) => (
                    <TimelineItem key={idx}>
                      <TimelineOppositeContent color="textSecondary">
                        {format(new Date(item.timestamp), 'MMM dd, HH:mm')}
                      </TimelineOppositeContent>
                      <TimelineSeparator>
                        <TimelineDot
                          sx={{
                            bgcolor: item.newStatus === 'COMPLETED' ? 'success.main' : 'info.main',
                          }}
                        >
                          <CheckCircle />
                        </TimelineDot>
                        {idx < history.length - 1 && <TimelineConnector />}
                      </TimelineSeparator>
                      <TimelineContent>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          {item.oldStatus} → {item.newStatus}
                        </Typography>
                      </TimelineContent>
                    </TimelineItem>
                  ))}
                </Timeline>
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
                <Button variant="outlined" fullWidth>
                  Mark Failed
                </Button>
              </Box>
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
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}

export default PaymentDetail;

