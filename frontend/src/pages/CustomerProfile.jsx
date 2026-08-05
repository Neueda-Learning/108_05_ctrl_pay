import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Grid,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { Controller, useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { accountAPI, currencyAPI } from '../services/api';
import { useCustomer } from '../context/CustomerContext';

const getTodayDateString = () => new Date().toISOString().split('T')[0];

function CustomerProfile() {
  const { customerId, customer, accounts, loadCustomer, loadingCustomer, clearCustomer } = useCustomer();
  const [customerIdInput, setCustomerIdInput] = useState(customerId || '');
  const [submitting, setSubmitting] = useState(false);
  const [currencies, setCurrencies] = useState([]);
  const [loadingCurrencies, setLoadingCurrencies] = useState(true);

  useEffect(() => {
    setCustomerIdInput(customerId || '');
  }, [customerId]);

  // Load currencies on component mount
  useEffect(() => {
    const loadCurrencies = async () => {
      try {
        setLoadingCurrencies(true);
        const response = await currencyAPI.getCurrencies();
        setCurrencies(response.data || []);
      } catch (error) {
        console.error('Error loading currencies:', error);
        toast.error('Failed to load available currencies');
        setCurrencies([]);
      } finally {
        setLoadingCurrencies(false);
      }
    };

    loadCurrencies();
  }, []);

  const accountForm = useForm({
    defaultValues: {
      accountNumber: '',
      accountName: '',
      accountBalance: '0',
      accountOpeningDate: getTodayDateString(),
      currency: 'INR',
      ifscCode: '',
      accountLocation: '',
      bankName: '',
      accountPin: '',
    },
  });

  const handleLoadCustomer = async () => {
    try {
      await loadCustomer(customerIdInput);
    } catch (error) {
      toast.error(error.response?.data?.message || error.message || 'Unable to load customer');
    }
  };

  const onSubmit = async (values) => {
    if (!customer?.customerId) {
      toast.error('Load and validate a customer ID before creating an account');
      return;
    }

    try {
      setSubmitting(true);
      await accountAPI.createAccount(customer.customerId, {
        accountNumber: values.accountNumber,
        accountName: values.accountName,
        accountBalance: parseFloat(values.accountBalance),
        accountOpeningDate: values.accountOpeningDate,
        currency: values.currency.toUpperCase(),
        ifscCode: values.ifscCode.toUpperCase(),
        accountLocation: values.accountLocation,
        bankName: values.bankName,
        accountPin: values.accountPin,
      });

      toast.success('Account added successfully');
      accountForm.reset({
        accountNumber: '',
        accountName: '',
        accountBalance: '0',
        accountOpeningDate: getTodayDateString(),
        currency: 'INR',
        ifscCode: '',
        accountLocation: '',
        bankName: '',
        accountPin: '',
      });
      await loadCustomer(customer.customerId);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Unable to add account');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
          Create New Account
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Enter a Customer ID, validate it with the backend, and create only customer-provided account fields.
        </Typography>
      </Box>

      <Grid container spacing={3}>
        <Grid item xs={12} md={5}>
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Customer Lookup
              </Typography>
              <Stack spacing={2}>
                <TextField
                  label="Customer ID"
                  value={customerIdInput}
                  onChange={(e) => setCustomerIdInput(e.target.value)}
                  fullWidth
                />
                <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                  <Button variant="contained" onClick={handleLoadCustomer} disabled={loadingCustomer}>
                    {loadingCustomer ? 'Validating...' : 'Validate Customer'}
                  </Button>
                  <Button variant="outlined" onClick={clearCustomer}>
                    Clear
                  </Button>
                </Box>
                {customer && (
                  <Alert severity="success">
                    Loaded {customer.name} ({customer.customerId}) with {accounts.length} account(s).
                  </Alert>
                )}
              </Stack>
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Existing Accounts
              </Typography>
              {accounts.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No accounts loaded for the selected customer.
                </Typography>
              ) : (
                <Stack spacing={1.5}>
                  {accounts.map((account) => (
                    <Box key={account.accountId || account.id} sx={{ p: 1.5, border: '1px solid #e0e0e0', borderRadius: 1 }}>
                      <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                        {account.accountName}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {account.bankName} | {account.ifscCode}
                      </Typography>
                      <Typography variant="body2">
                        {account.currency} {account.accountBalance}
                      </Typography>
                    </Box>
                  ))}
                </Stack>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={7}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Account Details
              </Typography>

              <form onSubmit={accountForm.handleSubmit(onSubmit)}>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6}>
                    <Controller
                      name="accountNumber"
                      control={accountForm.control}
                      rules={{
                        required: 'Account number is required',
                        pattern: {
                          value: /^[0-9]{12}$/,
                          message: 'Account number must be exactly 12 digits',
                        },
                      }}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          label="Account Number"
                          placeholder="12-digit number"
                          fullWidth
                          error={!!accountForm.formState.errors.accountNumber}
                          helperText={accountForm.formState.errors.accountNumber?.message}
                        />
                      )}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Controller
                      name="accountName"
                      control={accountForm.control}
                      rules={{ required: 'Account name is required' }}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          label="Account Name"
                          fullWidth
                          error={!!accountForm.formState.errors.accountName}
                          helperText={accountForm.formState.errors.accountName?.message}
                        />
                      )}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Controller
                      name="accountBalance"
                      control={accountForm.control}
                      rules={{
                        required: 'Opening balance is required',
                        min: {
                          value: 0,
                          message: 'Opening balance cannot be negative',
                        },
                      }}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          label="Opening Balance"
                          type="number"
                          placeholder="0.00"
                          inputProps={{ min: '0', step: '0.01' }}
                          fullWidth
                          error={!!accountForm.formState.errors.accountBalance}
                          helperText={accountForm.formState.errors.accountBalance?.message}
                        />
                      )}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Controller
                      name="accountOpeningDate"
                      control={accountForm.control}
                      rules={{
                        required: 'Account opening date is required',
                        validate: (value) => value <= getTodayDateString() || 'Account opening date cannot be in the future',
                      }}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          label="Account Opening Date"
                          type="date"
                          fullWidth
                          InputLabelProps={{ shrink: true }}
                          inputProps={{ max: getTodayDateString() }}
                          helperText={accountForm.formState.errors.accountOpeningDate?.message || 'Defaults to today'}
                          error={!!accountForm.formState.errors.accountOpeningDate}
                        />
                      )}
                    />
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <Controller
                      name="currency"
                      control={accountForm.control}
                      render={({ field }) => (
                        <TextField {...field} label="Currency" select fullWidth disabled={loadingCurrencies}>
                          {loadingCurrencies ? (
                            <MenuItem disabled>Loading currencies...</MenuItem>
                          ) : currencies.length > 0 ? (
                            currencies.map((currency) => (
                              <MenuItem key={currency.code} value={currency.code}>
                                {currency.name} ({currency.code})
                              </MenuItem>
                            ))
                          ) : (
                            <MenuItem disabled>No currencies available</MenuItem>
                          )}
                        </TextField>
                      )}
                    />
                  </Grid>
                  <Grid item xs={12} sm={8}>
                    <Controller
                      name="ifscCode"
                      control={accountForm.control}
                      rules={{
                        required: 'IFSC code is required',
                        pattern: {
                          value: /^[A-Za-z]{4}0[A-Za-z0-9]{6}$/,
                          message: 'Invalid IFSC format',
                        },
                      }}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          label="IFSC Code"
                          fullWidth
                          error={!!accountForm.formState.errors.ifscCode}
                          helperText={accountForm.formState.errors.ifscCode?.message}
                        />
                      )}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Controller
                      name="bankName"
                      control={accountForm.control}
                      rules={{ required: 'Bank name is required' }}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          label="Bank Name"
                          fullWidth
                          error={!!accountForm.formState.errors.bankName}
                          helperText={accountForm.formState.errors.bankName?.message}
                        />
                      )}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Controller
                      name="accountLocation"
                      control={accountForm.control}
                      rules={{ required: 'Account location is required' }}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          label="Account Location"
                          fullWidth
                          error={!!accountForm.formState.errors.accountLocation}
                          helperText={accountForm.formState.errors.accountLocation?.message}
                        />
                      )}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Controller
                      name="accountPin"
                      control={accountForm.control}
                      rules={{
                        required: 'Account PIN is required',
                        pattern: {
                          value: /^[0-9]{4,6}$/,
                          message: 'PIN must be 4 to 6 digits',
                        },
                      }}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          label="Account PIN"
                          type="password"
                          fullWidth
                          error={!!accountForm.formState.errors.accountPin}
                          helperText={accountForm.formState.errors.accountPin?.message}
                        />
                      )}
                    />
                  </Grid>
                  <Grid item xs={12}>
                    <Button type="submit" variant="contained" disabled={submitting || !customer}>
                      {submitting ? <CircularProgress size={20} /> : 'Create Account'}
                    </Button>
                  </Grid>
                </Grid>
              </form>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}

export default CustomerProfile;

