import React, { useEffect, useState } from 'react';
import { alpha } from '@mui/material/styles';
import {
  Box,
  Grid,
  Alert,
  CircularProgress,
  Typography,
  useTheme,
} from '@mui/material';
import {
  TrendingUp as TrendingUpIcon,
  PaymentOutlined as PaymentIcon,
  CheckCircle as CheckCircleIcon,
  ErrorOutlined as ErrorIcon,
  People as PeopleIcon,
  Security as SecurityIcon,
  Storage as StorageIcon,
} from '@mui/icons-material';
import {
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import StatsCard from '../components/StatsCard';
import ChartCard from '../components/ChartCard';
import { dashboardAPI } from '../services/dashboardAPI';

function PlatformOverviewDashboard() {
  const theme = useTheme();
  const custom = theme.customTokens;

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [data, setData] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);
      const overview = await dashboardAPI.getOverview();
      setData(overview);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load dashboard');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  const mockSuccessVsFailureData = [
    { name: 'Successful', value: data?.successfulTransactions || 0, fill: '#10b981' },
    { name: 'Failed', value: data?.failedTransactions || 0, fill: '#ef4444' },
    { name: 'Pending', value: data?.pendingTransactions || 0, fill: '#f59e0b' },
  ];

  const mockTransactionTrendData = [
    { date: 'Mon', transactions: 120, volume: 45000 },
    { date: 'Tue', transactions: 140, volume: 52000 },
    { date: 'Wed', transactions: 110, volume: 48000 },
    { date: 'Thu', transactions: 160, volume: 61000 },
    { date: 'Fri', transactions: 190, volume: 75000 },
    { date: 'Sat', transactions: 130, volume: 51000 },
    { date: 'Sun', transactions: 100, volume: 42000 },
  ];

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
          Platform Overview
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Real-time platform statistics and performance metrics
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={4} lg={2.4}>
          <StatsCard
            title="Total Transactions"
            value={data?.totalTransactions?.toLocaleString() || '0'}
            icon={PaymentIcon}
            color="primary"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={4} lg={2.4}>
          <StatsCard
            title="Success Rate"
            value={`${(data?.transactionSuccessRate || 0).toFixed(1)}%`}
            icon={CheckCircleIcon}
            color="success"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={4} lg={2.4}>
          <StatsCard
            title="Active Customers"
            value={data?.activeCustomers?.toLocaleString() || '0'}
            icon={PeopleIcon}
            color="info"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={4} lg={2.4}>
          <StatsCard
            title="Fraud Detected"
            value={data?.fraudDetectedCount?.toLocaleString() || '0'}
            icon={SecurityIcon}
            color="warning"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={4} lg={2.4}>
          <StatsCard
            title="System Health"
            value={data?.systemHealth || 'OK'}
            icon={StorageIcon}
            color={
              data?.systemHealth === 'OK'
                ? 'success'
                : data?.systemHealth === 'WARNING'
                ? 'warning'
                : 'error'
            }
          />
        </Grid>
      </Grid>

      {/* Charts */}
      <Grid container spacing={3}>
        {/* Transaction Trend Chart */}
        <Grid item xs={12} lg={8}>
          <ChartCard title="Transaction Trend (This Week)" height="400px">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={mockTransactionTrendData}>
                <CartesianGrid strokeDasharray="3 3" stroke={alpha(custom.border, 0.5)} />
                <XAxis dataKey="date" stroke={custom.text.muted} />
                <YAxis stroke={custom.text.muted} />
                <RechartsTooltip
                  contentStyle={{
                    background: custom.drawer,
                    border: `1px solid ${custom.border}`,
                    borderRadius: '8px',
                  }}
                />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="transactions"
                  stroke={custom.brand.main}
                  strokeWidth={2}
                  dot={{ fill: custom.brand.main }}
                />
              </LineChart>
            </ResponsiveContainer>
          </ChartCard>
        </Grid>

        {/* Success vs Failure Pie Chart */}
        <Grid item xs={12} lg={4}>
          <ChartCard title="Transaction Status" height="400px">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={mockSuccessVsFailureData}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, value }) => `${name}: ${value}`}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {mockSuccessVsFailureData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.fill} />
                  ))}
                </Pie>
                <RechartsTooltip
                  contentStyle={{
                    background: custom.drawer,
                    border: `1px solid ${custom.border}`,
                    borderRadius: '8px',
                  }}
                />
              </PieChart>
            </ResponsiveContainer>
          </ChartCard>
        </Grid>
      </Grid>
    </Box>
  );
}

export default PlatformOverviewDashboard;

