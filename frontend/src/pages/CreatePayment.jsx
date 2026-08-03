import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Grid,
  CircularProgress,
  Alert,
  Stepper,
  Step,
  StepLabel,
  MenuItem,
} from '@mui/material';
import { useForm, Controller } from 'react-hook-form';
import { paymentAPI } from '../services/api';
import { toast } from 'react-toastify';

const steps = ['Payment Details', 'Review', 'Confirmation'];

function CreatePayment() {
  const navigate = useNavigate();
  const { control, handleSubmit, watch, formState: { errors } } = useForm({
    defaultValues: {
      sourceAccount: '',
      destinationAccount: '',
      amount: '',
      currency: 'USD',
    },
  });
  const [activeStep, setActiveStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [paymentId, setPaymentId] = useState(null);

  const formValues = watch();

  const onSubmit = async (data) => {
    if (activeStep === 0) {
      setActiveStep(1);
      return;
    }

    if (activeStep === 1) {
      setActiveStep(2);
      return;
    }

    try {
      setLoading(true);
      const response = await paymentAPI.createPayment({
        sourceAccount: data.sourceAccount,
        destinationAccount: data.destinationAccount,
        amount: parseFloat(data.amount),
        currency: data.currency,
        idempotencyKey: `payment-${Date.now()}`,
      });

      setPaymentId(response.data.id);
      toast.success('Payment created successfully! Redirecting to processing...');

      setTimeout(() => {
        navigate(`/payment/process/${response.data.id}`);
      }, 1500);
    } catch (error) {
      console.error('Error creating payment:', error);
      toast.error(error.response?.data?.message || 'Error creating payment');
      setActiveStep(0);
    } finally {
      setLoading(false);
    }
  };

  const handleBack = () => {
    if (activeStep > 0) setActiveStep(activeStep - 1);
  };

  const isValidEmail = (email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>
        Create New Payment
      </Typography>

      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Stepper activeStep={activeStep} sx={{ mb: 4 }}>
                {steps.map((label) => (
                  <Step key={label}>
                    <StepLabel>{label}</StepLabel>
                  </Step>
                ))}
              </Stepper>

              {paymentId && (
                <Alert severity="success" sx={{ mb: 3 }}>
                  ✓ Payment {paymentId} created successfully! Redirecting...
                </Alert>
              )}

              <form onSubmit={handleSubmit(onSubmit)}>
                {/* Step 1: Payment Details */}
                {activeStep === 0 && (
                  <Box sx={{ space: 2 }}>
                    <Grid container spacing={2}>
                      <Grid item xs={12}>
                        <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 2 }}>
                          Payment Information
                        </Typography>
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <Controller
                          name="sourceAccount"
                          control={control}
                          rules={{ required: 'Source account is required' }}
                          render={({ field }) => (
                            <TextField
                              {...field}
                              label="From Account"
                              fullWidth
                              placeholder="12-digit account number"
                              error={!!errors.sourceAccount}
                              helperText={errors.sourceAccount?.message}
                            />
                          )}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <Controller
                          name="destinationAccount"
                          control={control}
                          rules={{ required: 'Destination account is required' }}
                          render={({ field }) => (
                            <TextField
                              {...field}
                              label="To Account"
                              fullWidth
                              placeholder="12-digit account number"
                              error={!!errors.destinationAccount}
                              helperText={errors.destinationAccount?.message}
                            />
                          )}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <Controller
                          name="amount"
                          control={control}
                          rules={{ required: 'Amount is required' }}
                          render={({ field }) => (
                            <TextField
                              {...field}
                              label="Amount"
                              type="number"
                              fullWidth
                              inputProps={{ step: '0.01', min: '0' }}
                              error={!!errors.amount}
                              helperText={errors.amount?.message}
                            />
                          )}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <Controller
                          name="currency"
                          control={control}
                          render={({ field }) => (
                            <TextField
                              {...field}
                              label="Currency"
                              select
                              fullWidth
                            >
                              {['USD', 'EUR', 'GBP', 'JPY', 'AUD'].map((option) => (
                                <MenuItem key={option} value={option}>
                                  {option}
                                </MenuItem>
                              ))}
                            </TextField>
                          )}
                        />
                      </Grid>
                    </Grid>
                  </Box>
                )}

                {/* Step 2: Review */}
                {activeStep === 1 && (
                  <Box>
                    <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 2 }}>
                      Review Payment Details
                    </Typography>
                    <Box sx={{ backgroundColor: '#f5f5f5', p: 2, borderRadius: 1, mb: 3 }}>
                      <Grid container spacing={2}>
                        <Grid item xs={6}>
                          <Typography variant="body2" color="textSecondary">
                            From Account
                          </Typography>
                          <Typography variant="body1" sx={{ fontWeight: 600 }}>
                            {formValues.sourceAccount}
                          </Typography>
                        </Grid>
                        <Grid item xs={6}>
                          <Typography variant="body2" color="textSecondary">
                            To Account
                          </Typography>
                          <Typography variant="body1" sx={{ fontWeight: 600 }}>
                            {formValues.destinationAccount}
                          </Typography>
                        </Grid>
                        <Grid item xs={6}>
                          <Typography variant="body2" color="textSecondary">
                            Amount
                          </Typography>
                          <Typography variant="h6" sx={{ fontWeight: 600 }}>
                            {formValues.amount} {formValues.currency}
                          </Typography>
                        </Grid>
                        <Grid item xs={6}>
                          <Typography variant="body2" color="textSecondary">
                            Status
                          </Typography>
                          <Typography variant="body1" sx={{ fontWeight: 600, color: 'warning.main' }}>
                            Pending
                          </Typography>
                        </Grid>
                      </Grid>
                    </Box>
                  </Box>
                )}

                {/* Step 3: Confirmation */}
                {activeStep === 2 && (
                  <Box sx={{ textAlign: 'center', py: 3 }}>
                    <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                      Confirm and Submit
                    </Typography>
                    <Typography variant="body2" color="textSecondary" sx={{ mb: 3 }}>
                      Click submit to create the payment
                    </Typography>
                  </Box>
                )}

                {/* Buttons */}
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 4 }}>
                  <Button
                    variant="outlined"
                    onClick={handleBack}
                    disabled={activeStep === 0 || loading}
                  >
                    Back
                  </Button>
                  <Box sx={{ display: 'flex', gap: 1 }}>
                    <Button
                      variant="outlined"
                      onClick={() => navigate('/payments')}
                      disabled={loading}
                    >
                      Cancel
                    </Button>
                    <Button
                      type="submit"
                      variant="contained"
                      disabled={loading}
                    >
                      {loading ? <CircularProgress size={24} /> : (activeStep === steps.length - 1 ? 'Submit' : 'Next')}
                    </Button>
                  </Box>
                </Box>
              </form>
            </CardContent>
          </Card>
        </Grid>

        {/* Summary */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Summary
              </Typography>
              <Box sx={{ space: 1 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2">Status:</Typography>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    {activeStep === 0 ? 'Editing' : activeStep === 1 ? 'Review' : 'Submitting'}
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2">Step:</Typography>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    {activeStep + 1} of {steps.length}
                  </Typography>
                </Box>
                <Typography variant="caption" color="textSecondary">
                  {steps[activeStep]}
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}

export default CreatePayment;
