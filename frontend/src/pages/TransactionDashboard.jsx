import React, { useEffect, useState } from 'react';
import { alpha } from '@mui/material/styles';
import {
  Box,
  Grid,
  Alert,
  CircularProgress,
  Typography,
  useTheme,
  Card,
} from '@mui/material';
import {
  BarChart,
  Bar,
  AreaChart,
  Area,
  PieChart,
  Pie,
  Cell,
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
import {
  TrendingUp as TrendingUpIcon,
  PaymentOutlined as PaymentIcon,
  AttachMoney as MoneyIcon,
  CalendarToday as CalendarIcon,
} from '@mui/icons-material';

function TransactionDashboard() {
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
      const transactions = await dashboardAPI.getTransactions();
      setData(transactions);
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

  const mockDailyData = [
    { date: 'Mon', count: 120, volume: 45000 },
    { date: 'Tue', count: 140, volume: 52000 },
    { date: 'Wed', count: 110, volume: 48000 },
    { date: 'Thu', count: 160, volume: 61000 },
    { date: 'Fri', count: 190, volume: 75000 },
    { date: 'Sat', count: 130, volume: 51000 },
    { date: 'Sun', count: 100, volume: 42000 },
  ];

  const mockStatusData = [
    { name: 'Completed', value: data?.transactionsByStatus?.COMPLETED || 0, fill: '#10b981' },
    { name: 'Failed', value: data?.transactionsByStatus?.FAILED || 0, fill: '#ef4444' },
    { name: 'Pending', value: (data?.transactionsByStatus?.CREATED || 0) + (data?.transactionsByStatus?.VALIDATED || 0) + (data?.transactionsByStatus?.SENT || 0), fill: '#f59e0b' },
    { name: 'Suspicious', value: data?.transactionsByStatus?.SUSPICIOUS || 0, fill: '#d946ef' },
  ];

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
          Transaction Analytics
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Detailed transaction metrics and trends
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={3}>
          <StatsCard
            title="Total Transactions"
            value={data?.totalTransactionCount?.toLocaleString() || '0'}
            icon={PaymentIcon}
          />
        </Grid>
{/*         <Grid item xs={12} sm={6} md={3}> */}
{/*           <StatsCard */}
{/*             title="Total Volume" */}
{/*             value={`$${(data?.transactionVolume?.toLocaleString() || 0)}`} */}
{/*             icon={MoneyIcon} */}
{/*           /> */}
{/*         </Grid> */}
{/*         <Grid item xs={12} sm={6} md={3}> */}
{/*           <StatsCard */}
{/*             title="Average Value" */}
{/*             value={`$${(data?.averageTransactionValue?.toLocaleString() || 0)}`} */}
{/*             icon={TrendingUpIcon} */}
{/*           /> */}
{/*         </Grid> */}
{/*         <Grid item xs={12} sm={6} md={3}> */}
{/*           <StatsCard */}
{/*             title="This Month" */}
{/*             value={Math.random().toFixed(0) * 1000} */}
{/*             icon={CalendarIcon} */}
{/*           /> */}
{/*         </Grid> */}
      </Grid>

      {/* Charts */}
      <Grid container spacing={3}>
        {/* Status Distribution */}
        <Grid item xs={12} lg={8}>
          <ChartCard title="Transaction Status Distribution" height="400px">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={mockStatusData}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, value }) => `${name}: ${value}`}
                  outerRadius={100}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {mockStatusData.map((entry, index) => (
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

        {/* Stats Summary Side Panel */}
        <Grid item xs={12} lg={4}>
          <Box
            sx={{
              display: 'flex',
              flexDirection: 'column',
              gap: 2,
              height: '400px',
            }}
          >
            <Card
              sx={{
                background: custom.gridRow,
                backdropFilter: 'blur(20px)',
                WebkitBackdropFilter: 'blur(20px)',
                border: `1px solid ${alpha(custom.border, 0.6)}`,
                borderRadius: '16px',
                flex: 1,
                p: 2,
              }}
            >
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2, color: custom.text.primary }}>
                Status Breakdown
              </Typography>
              {[
                { label: 'Completed', value: data?.transactionsByStatus?.COMPLETED || 0, color: '#10b981' },
                { label: 'Failed', value: data?.transactionsByStatus?.FAILED || 0, color: '#ef4444' },
                { label: 'Pending', value: (data?.transactionsByStatus?.CREATED || 0) + (data?.transactionsByStatus?.VALIDATED || 0) + (data?.transactionsByStatus?.SENT || 0), color: '#f59e0b' },
                { label: 'Suspicious', value: data?.transactionsByStatus?.SUSPICIOUS || 0, color: '#d946ef' },
              ].map((item) => (
                <Box key={item.label} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Box sx={{ width: 12, height: 12, borderRadius: '2px', background: item.color }} />
                    <Typography variant="body2" sx={{ color: custom.text.secondary }}>
                      {item.label}
                    </Typography>
                  </Box>
                  <Typography variant="h6" sx={{ color: custom.brand.main, fontWeight: 700 }}>
                    {item.value.toLocaleString()}
                  </Typography>
                </Box>
              ))}
            </Card>
          </Box>
        </Grid>
      </Grid>
    </Box>
  );
}

export default TransactionDashboard;

