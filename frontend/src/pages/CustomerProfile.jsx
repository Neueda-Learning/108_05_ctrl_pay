import React, { useMemo, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Grid,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { Controller, useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { useAuth } from '../context/AuthContext';

function CustomerProfile() {
  const { customer, accounts, addAccount, refreshCustomerData } = useAuth();
  const [submitting, setSubmitting] = useState(false);

  const accountForm = useForm({
    defaultValues: {
      accountName: '',
      accountBalance: '0',
      currency: 'INR',
      accountOpeningDate: new Date().toISOString().split('T')[0],
      ifscCode: '',
      accountLocation: '',
      bankName: '',
      accountPin: '',
    },
  });

  const statusColor = useMemo(() => {
    if (!customer?.customerAccountStatus) {
      return 'default';
    }

    return customer.customerAccountStatus === 'ACTIVE' ? 'success' : 'warning';
  }, [customer]);

  const onSubmit = async (values) => {
    try {
      setSubmitting(true);
      await addAccount({
        ...values,
        accountBalance: parseFloat(values.accountBalance),
        currency: values.currency.toUpperCase(),
        ifscCode: values.ifscCode.toUpperCase(),
      });
      toast.success('Account added successfully');
      accountForm.reset({
        accountName: '',
        accountBalance: '0',
        currency: 'INR',
        accountOpeningDate: new Date().toISOString().split('T')[0],
        ifscCode: '',
        accountLocation: '',
        bankName: '',
        accountPin: '',
      });
    } catch (error) {
      toast.error(error.response?.data?.message || 'Unable to add account');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>
            My Profile
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Live customer and account data synced from backend.
          </Typography>
        </Box>
        <Button variant="outlined" onClick={() => refreshCustomerData(customer.customerId)}>
          Refresh
        </Button>
      </Box>

      <Grid container spacing={3}>
        <Grid item xs={12} md={5}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Customer Details
              </Typography>
              <Stack spacing={1.5}>
                <Typography variant="body2"><strong>Name:</strong> {customer?.name}</Typography>
                <Typography variant="body2"><strong>Phone:</strong> {customer?.phoneNumber}</Typography>
                <Typography variant="body2"><strong>DOB:</strong> {customer?.dob}</Typography>
                <Typography variant="body2"><strong>PAN:</strong> {customer?.panNumber}</Typography>
                <Typography variant="body2"><strong>Country:</strong> {customer?.country}</Typography>
                <Typography variant="body2">
                  <strong>Status:</strong>{' '}
                  <Chip size="small" color={statusColor} label={customer?.customerAccountStatus || 'UNKNOWN'} />
                </Typography>
              </Stack>
            </CardContent>
          </Card>

          <Card sx={{ mt: 3 }}>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Accounts ({accounts.length})
              </Typography>
              {accounts.length === 0 ? (
                <Typography variant="body2" color="text.secondary">No accounts yet. Add your first account.</Typography>
              ) : (
                <Stack spacing={1.5}>
                  {accounts.map((account) => (
                    <Box key={account.accountId} sx={{ p: 1.5, border: '1px solid #e0e0e0', borderRadius: 1 }}>
                      <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>{account.accountName}</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {account.bankName} | {account.ifscCode}
                      </Typography>
                      <Typography variant="body2">
                        {account.currency} {account.accountBalance} | {account.accountStatus}
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
                Add Account
              </Typography>

              <form onSubmit={accountForm.handleSubmit(onSubmit)}>
                <Grid container spacing={2}>
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
                      rules={{ required: 'Balance is required' }}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          label="Opening Balance"
                          type="number"
                          inputProps={{ min: '0', step: '0.01' }}
                          fullWidth
                          error={!!accountForm.formState.errors.accountBalance}
                          helperText={accountForm.formState.errors.accountBalance?.message}
                        />
                      )}
                    />
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <Controller
                      name="currency"
                      control={accountForm.control}
                      render={({ field }) => (
                        <TextField {...field} label="Currency" select fullWidth>
                          {['INR', 'USD', 'EUR', 'GBP'].map((currency) => (
                            <MenuItem key={currency} value={currency}>{currency}</MenuItem>
                          ))}
                        </TextField>
                      )}
                    />
                  </Grid>
                  <Grid item xs={12} sm={8}>
                    <Controller
                      name="accountOpeningDate"
                      control={accountForm.control}
                      rules={{ required: 'Opening date is required' }}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          type="date"
                          label="Account Opening Date"
                          InputLabelProps={{ shrink: true }}
                          fullWidth
                          error={!!accountForm.formState.errors.accountOpeningDate}
                          helperText={accountForm.formState.errors.accountOpeningDate?.message}
                        />
                      )}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
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
                    <Button type="submit" variant="contained" disabled={submitting}>
                      {submitting ? <CircularProgress size={20} /> : 'Add Account'}
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

