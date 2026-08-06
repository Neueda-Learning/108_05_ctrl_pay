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
  Grid,
  CircularProgress,
  Alert,
  Stepper,
  Step,
  StepLabel,
  MenuItem,
  Divider,
} from '@mui/material';
import { useForm, Controller } from 'react-hook-form';
import { paymentAPI, accountAPI, currencyAPI } from '../services/api';
import { toast } from 'react-toastify';
import { useCustomer } from '../context/CustomerContext';
import PinCodeInput from '../components/PinCodeInput';

const steps = [
  'Verify Destination Account',
  'Currency & Amount',
  'Review & Authenticate',
];

function CreatePayment() {
  const navigate = useNavigate();

  // Load customer and accounts from context
  const { customer, accounts } = useCustomer();

  // Form management
  const {
    control,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm({
    defaultValues: {
      sourceAccount: '',
      destinationAccount: '',
      amount: '',
      pin: '',
    },
  });

  // UI State
  const [activeStep, setActiveStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [paymentId, setPaymentId] = useState(null);

  // Source account state
  const [accountOptions, setAccountOptions] = useState([]);
  const [selectedSourceAccount, setSelectedSourceAccount] = useState(null);

  // Destination account state after verification
  const [destinationAccountData, setDestinationAccountData] = useState(null);
  const [verifyingDestination, setVerifyingDestination] = useState(false);
  const [destinationVerified, setDestinationVerified] = useState(false);

  // Currency conversion state
  const [sourceCurrency, setSourceCurrency] = useState(null);
  const [destinationCurrency, setDestinationCurrency] = useState(null);
  const [convertedAmount, setConvertedAmount] = useState(null);
  const [exchangeRate, setExchangeRate] = useState(null);
  const [convertingCurrency, setConvertingCurrency] = useState(false);

  const formValues = watch();

  // Populate source account options from customer accounts
  useEffect(() => {
    if (accounts && accounts.length > 0) {
      setAccountOptions(accounts);
    } else {
      setAccountOptions([]);
      setSelectedSourceAccount(null);
    }
  }, [accounts]);

  // Set source currency when source account is selected
  useEffect(() => {
    if (selectedSourceAccount) {
      setSourceCurrency(selectedSourceAccount.currency);
    }
  }, [selectedSourceAccount]);

  // Helper: Get account display value
  const getAccountValue = (account) => {
    return account.accountNumber;
  };

  // Helper: Get account display label
  const getAccountLabel = (account) => {
    return `${account.accountNumber} - ${account.accountName || 'Account'}`;
  };

  /**
   * STEP 1: Verify destination account exists
   * Call backend to validate destination account number
   */
  const verifyDestinationAccount = async () => {
    if (!formValues.destinationAccount) {
      toast.error('Please enter a destination account number');
      return;
    }

    try {
      setVerifyingDestination(true);

      const response = await accountAPI.getAccountByNumber(
        formValues.destinationAccount
      );

      setDestinationAccountData(response.data);
      setDestinationCurrency(response.data.currency);
      setDestinationVerified(true);

      toast.success('Destination account verified!');

      // Move to next step
      setTimeout(() => setActiveStep(1), 500);
    } catch (error) {
      console.error('Error verifying destination account:', error);
      setDestinationVerified(false);
      toast.error(
        error.response?.data?.message ||
          'Destination account not found. Please enter a valid account number.'
      );
    } finally {
      setVerifyingDestination(false);
    }
  };

  /**
   * STEP 2: Convert currency when amount changes
   */
  useEffect(() => {
    const convertCurrency = async () => {
      if (
        !formValues.amount ||
        formValues.amount <= 0 ||
        !sourceCurrency ||
        !destinationCurrency
      ) {
        setConvertedAmount(null);
        setExchangeRate(null);
        return;
      }

      try {
        setConvertingCurrency(true);

        const response = await currencyAPI.convert({
          amount: parseFloat(formValues.amount),
          sourceCurrency: sourceCurrency,
          destinationCurrency: destinationCurrency,
        });

        setExchangeRate(response.data.exchangeRate);
        setConvertedAmount(response.data.convertedAmount);
      } catch (error) {
        console.error('Error converting currency:', error);
        setExchangeRate(null);
        setConvertedAmount(null);
      } finally {
        setConvertingCurrency(false);
      }
    };

    convertCurrency();
  }, [formValues.amount, sourceCurrency, destinationCurrency]);

  /**
   * Handle form submission
   */
  const onSubmit = async (data) => {
    // Validate customer is loaded
    if (!customer) {
      toast.error('Please load a customer first by going to the Dashboard');
      return;
    }

    // Validate source account is selected
    if (!data.sourceAccount) {
      toast.error('Please select a source account');
      return;
    }

    // Step 1: Verify destination (if not already verified)
    if (activeStep === 0) {
      await verifyDestinationAccount();
      return;
    }

    // Step 2: Move to review
    if (activeStep === 1) {
      // Validate amount
      if (!data.amount || parseFloat(data.amount) <= 0) {
        toast.error('Please enter a valid amount');
        return;
      }

      // Validate conversion happened
      if (!convertedAmount) {
        toast.error('Unable to convert currency. Please try again.');
        return;
      }

      setActiveStep(2);
      return;
    }

    // Step 3: Submit payment
    if (activeStep === 2) {
      // Validate PIN
      if (!data.pin) {
        toast.error('Please enter your PIN');
        return;
      }

      try {
        setLoading(true);

        // Create payment with exchange rate information
        const response = await paymentAPI.createPayment({
          sourceAccount: data.sourceAccount,
          destinationAccount: data.destinationAccount,
          amount: parseFloat(data.amount),
          currency: sourceCurrency,
          sourceCurrency: sourceCurrency,
          destinationCurrency: destinationCurrency,
          sourceAmount: parseFloat(data.amount),
          destinationAmount: convertedAmount,
          exchangeRate: exchangeRate,
          idempotencyKey: `payment-${Date.now()}`,
          pin: data.pin,
        });

        setPaymentId(response.data.id);

        toast.success(
          'Payment created successfully! Redirecting to processing...'
        );

        setTimeout(() => {
          navigate(`/payment/process/${response.data.id}`);
        }, 1500);
      } catch (error) {
        console.error('Error creating payment:', error);

        // Show specific error messages
        if (error.response?.status === 401) {
          toast.error('PIN verification failed. Please check your PIN and try again.');
        } else {
          toast.error(
            error.response?.data?.message || 'Error creating payment'
          );
        }

        setActiveStep(0);
      } finally {
        setLoading(false);
      }
    }
  };

  const handleBack = () => {
    if (activeStep > 0) {
      setActiveStep(activeStep - 1);
    }
  };


  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>
        Create New Payment
      </Typography>

      {/* Customer Load Alert */}
      {!customer && (
        <Alert severity="warning" sx={{ mb: 3 }}>
          ⚠️ No customer loaded. Please go to the Dashboard and load a customer
          first.
        </Alert>
      )}

      {/* Customer Info Alert */}
      {customer && (
        <Alert severity="success" sx={{ mb: 3 }}>
          ✓ Customer: <strong>{customer.name}</strong> ({customer.customerId}) -
          {' '}
          {accountOptions.length} account(s) available
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              {/* Stepper */}
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

                {/* STEP 1: DESTINATION ACCOUNT VERIFICATION */}
                {activeStep === 0 && (
                  <Box>
                    <Typography
                      variant="subtitle1"
                      sx={{ fontWeight: 600, mb: 3 }}
                    >
                      Payment Information
                    </Typography>

                    <Grid container spacing={2}>
                      {/* Source Account Selection (PRESERVED FROM EXISTING) */}
                      <Grid item xs={12} sm={6}>
                        <Controller
                          name="sourceAccount"
                          control={control}
                          rules={{
                            required: 'Source account is required',
                          }}
                          render={({ field }) => (
                            <TextField
                              {...field}
                              label="From Account"
                              select
                              fullWidth
                              disabled={
                                accountOptions.length === 0 || !customer
                              }
                              error={!!errors.sourceAccount}
                              helperText={
                                !customer
                                  ? 'Load a customer first'
                                  : accountOptions.length === 0
                                  ? 'No accounts available for this customer'
                                  : errors.sourceAccount?.message
                              }
                              onChange={(e) => {
                                field.onChange(e);

                                const selected = accountOptions.find(
                                  (acc) =>
                                    getAccountValue(acc) === e.target.value
                                );

                                setSelectedSourceAccount(selected);
                              }}
                            >
                              <MenuItem value="">
                                Select a customer account
                              </MenuItem>

                              {accountOptions.map((account) => {
                                const value = getAccountValue(account);

                                return (
                                  <MenuItem
                                    key={value || account.accountName}
                                    value={value}
                                  >
                                    {getAccountLabel(account)}
                                  </MenuItem>
                                );
                              })}
                            </TextField>
                          )}
                        />
                      </Grid>

                      {/* Source Account Info */}
                      {selectedSourceAccount && (
                        <Grid item xs={12} sm={6}>
                          <Box
                            sx={{
                              p: 2,
                              border: (theme) => `1px solid ${theme.palette.divider}`,
                              borderRadius: 1,
                              backgroundColor: (theme) => theme.customTokens.background.secondary,
                            }}
                          >
                            <Typography variant="caption" color="textSecondary">
                              Source Account Currency
                            </Typography>
                            <Typography variant="h6" sx={{ fontWeight: 700 }}>
                              {selectedSourceAccount.currency}
                            </Typography>
                            <Typography variant="body2" color="textSecondary">
                              {selectedSourceAccount.bankName}
                            </Typography>
                          </Box>
                        </Grid>
                      )}

                      {/* Destination Account Input */}
                      <Grid item xs={12} sm={6}>
                        <Controller
                          name="destinationAccount"
                          control={control}
                          rules={{
                            required:
                              'Destination account is required',
                            pattern: {
                              value: /^[0-9]{12}$/,
                              message:
                                'Account number must be exactly 12 digits',
                            },
                          }}
                          render={({ field }) => (
                            <TextField
                              {...field}
                              label="To Account"
                              fullWidth
                              placeholder="12-digit account number"
                              error={!!errors.destinationAccount}
                              helperText={
                                errors.destinationAccount?.message
                              }
                              disabled={verifyingDestination}
                            />
                          )}
                        />
                      </Grid>

                      {/* Destination verification state */}
                      <Grid item xs={12} sm={6}>
                        {verifyingDestination && (
                          <Box sx={{ p: 2, textAlign: 'center' }}>
                            <CircularProgress size={22} />
                            <Typography variant="caption" display="block" sx={{ mt: 1 }}>
                              Verifying destination account...
                            </Typography>
                          </Box>
                        )}

                        {!verifyingDestination && destinationVerified && (
                          <Box
                            sx={{
                              p: 2,
                              border: (theme) => `1px solid ${theme.palette.success.main}`,
                              borderRadius: 1,
                              backgroundColor: (theme) => alpha(theme.palette.success.main, 0.14),
                            }}
                          >
                            <Typography variant="caption" sx={{ color: 'success.main' }}>
                              ✓ Destination Account Verified
                            </Typography>
                            <Typography
                              variant="h6"
                              sx={{ fontWeight: 700, color: 'success.main' }}
                            >
                              {destinationAccountData?.currency}
                            </Typography>
                            <Typography variant="body2" color="textSecondary">
                              {destinationAccountData?.bankName}
                            </Typography>
                          </Box>
                        )}

                        {!verifyingDestination && !destinationVerified && (
                          <Typography variant="caption" color="text.secondary">
                            
                          </Typography>
                        )}
                      </Grid>
                    </Grid>

                  </Box>
                )}



                {/* STEP 2: CURRENCY & AMOUNT */}
                {activeStep === 1 && (
                  <Box>
                    <Typography
                      variant="subtitle1"
                      sx={{ fontWeight: 600, mb: 3 }}
                    >
                      Currency & Amount
                    </Typography>

                    <Grid container spacing={2}>
                      {/* Source Currency Info */}
                      <Grid item xs={12} sm={6}>
                        <Box
                          sx={{
                            p: 2,
                            border: (theme) => `1px solid ${theme.palette.divider}`,
                            borderRadius: 1,
                            backgroundColor: (theme) => theme.customTokens.background.secondary,
                            mb: 2,
                          }}
                        >
                          <Typography
                            variant="caption"
                            color="textSecondary"
                          >
                            From Account
                          </Typography>
                          <Typography variant="body2">
                            {selectedSourceAccount?.accountNumber}
                          </Typography>
                          <Typography
                            variant="caption"
                            color="textSecondary"
                          >
                            Currency
                          </Typography>
                          <Typography variant="h6" sx={{ fontWeight: 700 }}>
                            {sourceCurrency}
                          </Typography>
                        </Box>

                        {/* Amount Input */}
                        <Controller
                          name="amount"
                          control={control}
                          rules={{
                            required: 'Amount is required',
                            validate: (value) =>
                              parseFloat(value) > 0 ||
                              'Amount must be greater than 0',
                          }}
                          render={({ field }) => (
                            <TextField
                              {...field}
                              label="Amount to Send"
                              type="number"
                              fullWidth
                              inputProps={{
                                step: '0.01',
                                min: '0',
                              }}
                              error={!!errors.amount}
                              helperText={errors.amount?.message}
                            />
                          )}
                        />
                      </Grid>

                      {/* Destination Currency Info & Conversion */}
                      <Grid item xs={12} sm={6}>
                        <Box
                          sx={{
                            p: 2,
                            border: (theme) => `1px solid ${theme.palette.divider}`,
                            borderRadius: 1,
                            backgroundColor: (theme) => theme.customTokens.background.secondary,
                            mb: 2,
                          }}
                        >
                          <Typography
                            variant="caption"
                            color="textSecondary"
                          >
                            To Account
                          </Typography>
                          <Typography variant="body2">
                            {formValues.destinationAccount}
                          </Typography>
                          <Typography
                            variant="caption"
                            color="textSecondary"
                          >
                            Currency
                          </Typography>
                          <Typography variant="h6" sx={{ fontWeight: 700 }}>
                            {destinationCurrency}
                          </Typography>
                        </Box>

                        {/* Conversion Result */}
                        {formValues.amount && convertingCurrency && (
                          <Box sx={{ p: 2, textAlign: 'center' }}>
                            <CircularProgress size={24} />
                          </Box>
                        )}

                        {formValues.amount && convertedAmount && !convertingCurrency && (
                          <Box
                            sx={{
                              p: 2,
                              border: (theme) => `1px solid ${theme.palette.info.main}`,
                              borderRadius: 1,
                              backgroundColor: (theme) => alpha(theme.palette.info.main, 0.14),
                            }}
                          >
                            <Typography
                              variant="caption"
                              sx={{ color: 'info.main' }}
                            >
                              Exchange Rate
                            </Typography>
                            <Typography
                              variant="body2"
                              sx={{
                                fontWeight: 600,
                                color: 'info.main',
                                mb: 1,
                              }}
                            >
                              1 {sourceCurrency} ={' '}
                              {parseFloat(exchangeRate).toFixed(4)}{' '}
                              {destinationCurrency}
                            </Typography>

                            <Divider sx={{ my: 1 }} />

                            <Typography
                              variant="caption"
                              sx={{ color: 'info.main' }}
                            >
                              Receiver Gets
                            </Typography>
                            <Typography
                              variant="h6"
                              sx={{
                                fontWeight: 700,
                                color: 'info.main',
                              }}
                            >
                              {parseFloat(convertedAmount).toFixed(2)}{' '}
                              {destinationCurrency}
                            </Typography>
                          </Box>
                        )}
                      </Grid>
                    </Grid>
                  </Box>

                )}



                {/* STEP 3: REVIEW & AUTHENTICATE */}
                {activeStep === 2 && (
                  <Box>
                    <Typography
                      variant="subtitle1"
                      sx={{ fontWeight: 600, mb: 3 }}
                    >
                      Review & Confirm Payment
                    </Typography>

                    {/* Payment Details Review */}
                    <Box
                      sx={{
                        p: 2,
                        border: (theme) => `1px solid ${theme.palette.divider}`,
                        borderRadius: 1,
                        mb: 3,
                      }}
                    >
                      <Grid container spacing={2}>
                        <Grid item xs={12} sm={6}>
                          <Typography
                            variant="caption"
                            color="textSecondary"
                          >
                            From Account
                          </Typography>
                          <Typography variant="body1">
                            {getAccountLabel(selectedSourceAccount)}
                          </Typography>
                        </Grid>
                        <Grid item xs={12} sm={6}>
                          <Typography
                            variant="caption"
                            color="textSecondary"
                          >
                            To Account
                          </Typography>
                          <Typography variant="body1">
                            {formValues.destinationAccount}
                          </Typography>
                        </Grid>

                        <Grid item xs={12} sm={6}>
                          <Typography
                            variant="caption"
                            color="textSecondary"
                          >
                            Account Holder Name
                          </Typography>
                          <Typography variant="body1" sx={{ fontWeight: 500 }}>
                            {destinationAccountData?.accountName || '-'}
                          </Typography>
                        </Grid>

                        <Grid item xs={12}>
                          <Divider />
                        </Grid>

                        <Grid item xs={12} sm={6}>
                          <Typography
                            variant="caption"
                            color="textSecondary"
                          >
                            You Send
                          </Typography>
                          <Typography variant="h6" sx={{ fontWeight: 700 }}>
                            {parseFloat(formValues.amount).toFixed(2)}{' '}
                            {sourceCurrency}
                          </Typography>
                        </Grid>
                        <Grid item xs={12} sm={6}>
                          <Typography
                            variant="caption"
                            color="textSecondary"
                          >
                            They Receive
                          </Typography>
                          <Typography variant="h6" sx={{ fontWeight: 700 }}>
                            {parseFloat(convertedAmount).toFixed(2)}{' '}
                            {destinationCurrency}
                          </Typography>
                        </Grid>

                        <Grid item xs={12}>
                          <Divider />
                        </Grid>

                        <Grid item xs={12}>
                          <Typography
                            variant="caption"
                            color="textSecondary"
                          >
                            Exchange Rate
                          </Typography>
                          <Typography variant="body2">
                            1 {sourceCurrency} ={' '}
                            {parseFloat(exchangeRate).toFixed(4)}{' '}
                            {destinationCurrency}
                          </Typography>
                        </Grid>
                      </Grid>
                    </Box>

                    {/* PIN Authentication */}
                    <Typography
                      variant="subtitle2"
                      sx={{ fontWeight: 600, mb: 2 }}
                    >
                      Account Authentication
                    </Typography>

                    <Controller
                      name="pin"
                      control={control}
                      rules={{
                        required: 'PIN is required',
                        pattern: {
                          value: /^[0-9]{4}$/,
                          message: 'PIN must be exactly 4 digits',
                        },
                      }}
                      render={({ field }) => (
                        <Box sx={{ mb: 2 }}>
                          <PinCodeInput
                            label="Enter Account PIN"
                            value={field.value}
                            onChange={field.onChange}
                            error={!!errors.pin}
                            helperText={errors.pin?.message || 'Enter 4-digit PIN'}
                          />
                        </Box>
                        
                      )}
                    />

                    <Alert severity="info">
                      ℹ️ Your PIN verifies this is an authorized transaction.
                      Never share your PIN with anyone.
                    </Alert>
                  </Box>

                )}



                {/* Navigation Buttons */}
                <Box
                  sx={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    mt: 4,
                  }}
                >
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
                      disabled={
                        loading ||
                        !customer ||
                        (activeStep === 0 && accountOptions.length === 0)
                      }
                    >
                      {loading ? (
                        <CircularProgress size={24} />
                      ) : activeStep === steps.length - 1 ? (
                        'Submit Payment'
                      ) : (
                        'Next'
                      )}
                    </Button>
                  </Box>
                </Box>
              </form>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}


export default CreatePayment;