import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Grid,
  TextField,
  Typography,
} from '@mui/material';
import { Controller, useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { customerAPI } from '../services/api';
import { useCustomer } from '../context/CustomerContext';

function AuthPage() {
  const navigate = useNavigate();
  const { setCustomerId } = useCustomer();
  const [loading, setLoading] = useState(false);
  const [createdCustomerId, setCreatedCustomerId] = useState('');

  const { control, handleSubmit, reset, formState: { errors } } = useForm({
    defaultValues: {
      name: '',
      dob: '',
      phoneNumber: '',
      panNumber: '',
      country: '',
    },
  });

  const handleCreateCustomer = async (values) => {
    try {
      setLoading(true);
      const response = await customerAPI.createCustomer({
        ...values,
        panNumber: values.panNumber.toUpperCase(),
      });

      const customerId = response.data?.customerId || response.data?.id || response.data?.customer_id;
      if (customerId) {
        setCreatedCustomerId(customerId);
        setCustomerId(customerId);
      }

      toast.success('Customer profile created successfully');
      reset();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Could not create customer profile');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
          Create New Customer Profile
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Enter customer-provided details only. The backend should generate the customer identifier and manage all system fields.
        </Typography>
      </Box>

      {createdCustomerId && (
        <Alert severity="success" sx={{ mb: 3 }}>
          Customer profile created. New Customer ID: <strong>{createdCustomerId}</strong>
          <Box sx={{ mt: 1 }}>
            <Button variant="contained" size="small" onClick={() => navigate('/accounts/new')}>
              Create Account for This Customer
            </Button>
          </Box>
        </Alert>
      )}

      <Card sx={{ maxWidth: 900 }}>
        <CardContent sx={{ p: 4 }}>
          <form onSubmit={handleSubmit(handleCreateCustomer)}>
            <Grid container spacing={2}>
              <Grid item xs={12} md={6}>
                <Controller
                  name="name"
                  control={control}
                  rules={{ required: 'Name is required' }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Full Name"
                      fullWidth
                      error={!!errors.name}
                      helperText={errors.name?.message}
                    />
                  )}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <Controller
                  name="dob"
                  control={control}
                  rules={{ required: 'Date of birth is required' }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      type="date"
                      label="Date of Birth"
                      InputLabelProps={{ shrink: true }}
                      fullWidth
                      error={!!errors.dob}
                      helperText={errors.dob?.message}
                    />
                  )}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <Controller
                  name="phoneNumber"
                  control={control}
                  rules={{
                    required: 'Phone number is required',
                    pattern: {
                      value: /^[0-9+() -]{7,20}$/,
                      message: 'Invalid phone number format',
                    },
                  }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Phone Number"
                      fullWidth
                      error={!!errors.phoneNumber}
                      helperText={errors.phoneNumber?.message}
                    />
                  )}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <Controller
                  name="panNumber"
                  control={control}
                  rules={{
                    required: 'PAN is required',
                    pattern: {
                      value: /^[A-Za-z]{5}[0-9]{4}[A-Za-z]{1}$/,
                      message: 'Invalid PAN format',
                    },
                  }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="PAN Number"
                      fullWidth
                      inputProps={{ style: { textTransform: 'uppercase' } }}
                      error={!!errors.panNumber}
                      helperText={errors.panNumber?.message}
                    />
                  )}
                />
              </Grid>
              <Grid item xs={12}>
                <Controller
                  name="country"
                  control={control}
                  rules={{ required: 'Country is required' }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Country"
                      fullWidth
                      error={!!errors.country}
                      helperText={errors.country?.message}
                    />
                  )}
                />
              </Grid>
              <Grid item xs={12}>
                <Button type="submit" variant="contained" disabled={loading}>
                  {loading ? <CircularProgress size={20} /> : 'Create Customer Profile'}
                </Button>
              </Grid>
            </Grid>
          </form>
        </CardContent>
      </Card>
    </Box>
  );
}

export default AuthPage;

