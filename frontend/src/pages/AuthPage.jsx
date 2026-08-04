import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Grid,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import { useForm, Controller } from 'react-hook-form';
import { toast } from 'react-toastify';
import { useAuth } from '../context/AuthContext';

function AuthPage() {
  const [tab, setTab] = useState(0);
  const [loading, setLoading] = useState(false);
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const redirectPath = location.state?.from?.pathname || '/';

  const loginForm = useForm({
    defaultValues: {
      phoneNumber: '',
      dob: '',
    },
  });

  const registerForm = useForm({
    defaultValues: {
      name: '',
      dob: '',
      phoneNumber: '',
      panNumber: '',
      country: '',
    },
  });

  const handleLogin = async (values) => {
    try {
      setLoading(true);
      await login(values);
      toast.success('Login successful');
      navigate(redirectPath, { replace: true });
    } catch (error) {
      toast.error(error.response?.data?.message || 'Login failed. Please verify phone number and DOB.');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (values) => {
    try {
      setLoading(true);
      await register({
        ...values,
        panNumber: values.panNumber.toUpperCase(),
      });

      toast.success('Profile created successfully. You can login now.');
      setTab(0);
      loginForm.setValue('phoneNumber', values.phoneNumber);
      loginForm.setValue('dob', values.dob);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Could not create customer profile.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', p: 2, backgroundColor: '#f5f7fb' }}>
      <Card sx={{ width: '100%', maxWidth: 760 }}>
        <CardContent sx={{ p: 4 }}>
          <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
            Ctrl-Pay
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Register as a new customer or login as a returning user.
          </Typography>

          <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ mb: 3 }}>
            <Tab label="Login" />
            <Tab label="Register" />
          </Tabs>

          {tab === 0 && (
            <form onSubmit={loginForm.handleSubmit(handleLogin)}>
              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <Alert severity="info">Username = phone number, Password = DOB</Alert>
                </Grid>
                <Grid item xs={12}>
                  <Controller
                    name="phoneNumber"
                    control={loginForm.control}
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
                        error={!!loginForm.formState.errors.phoneNumber}
                        helperText={loginForm.formState.errors.phoneNumber?.message}
                      />
                    )}
                  />
                </Grid>
                <Grid item xs={12}>
                  <Controller
                    name="dob"
                    control={loginForm.control}
                    rules={{ required: 'DOB is required' }}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        type="date"
                        label="Date of Birth"
                        InputLabelProps={{ shrink: true }}
                        fullWidth
                        error={!!loginForm.formState.errors.dob}
                        helperText={loginForm.formState.errors.dob?.message}
                      />
                    )}
                  />
                </Grid>
                <Grid item xs={12}>
                  <Button type="submit" variant="contained" fullWidth disabled={loading}>
                    {loading ? <CircularProgress size={22} /> : 'Login'}
                  </Button>
                </Grid>
              </Grid>
            </form>
          )}

          {tab === 1 && (
            <form onSubmit={registerForm.handleSubmit(handleRegister)}>
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <Controller
                    name="name"
                    control={registerForm.control}
                    rules={{ required: 'Name is required' }}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Full Name"
                        fullWidth
                        error={!!registerForm.formState.errors.name}
                        helperText={registerForm.formState.errors.name?.message}
                      />
                    )}
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <Controller
                    name="dob"
                    control={registerForm.control}
                    rules={{ required: 'DOB is required' }}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        type="date"
                        label="Date of Birth"
                        InputLabelProps={{ shrink: true }}
                        fullWidth
                        error={!!registerForm.formState.errors.dob}
                        helperText={registerForm.formState.errors.dob?.message}
                      />
                    )}
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <Controller
                    name="phoneNumber"
                    control={registerForm.control}
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
                        error={!!registerForm.formState.errors.phoneNumber}
                        helperText={registerForm.formState.errors.phoneNumber?.message}
                      />
                    )}
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <Controller
                    name="panNumber"
                    control={registerForm.control}
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
                        error={!!registerForm.formState.errors.panNumber}
                        helperText={registerForm.formState.errors.panNumber?.message}
                      />
                    )}
                  />
                </Grid>
                <Grid item xs={12}>
                  <Controller
                    name="country"
                    control={registerForm.control}
                    rules={{ required: 'Country is required' }}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Country"
                        fullWidth
                        error={!!registerForm.formState.errors.country}
                        helperText={registerForm.formState.errors.country?.message}
                      />
                    )}
                  />
                </Grid>
                <Grid item xs={12}>
                  <Button type="submit" variant="contained" fullWidth disabled={loading}>
                    {loading ? <CircularProgress size={22} /> : 'Create Profile'}
                  </Button>
                </Grid>
              </Grid>
            </form>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}

export default AuthPage;

