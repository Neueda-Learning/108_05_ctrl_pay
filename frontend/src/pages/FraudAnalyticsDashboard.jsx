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
  Security as SecurityIcon,
  Warning as WarningIcon,
  Gavel as GavelIcon,
  TrendingDown as TrendingDownIcon,
} from '@mui/icons-material';

function FraudAnalyticsDashboard() {
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
      const fraud = await dashboardAPI.getFraud();
      setData(fraud);
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

  const mockRiskData = [
    { name: 'Approved', value: data?.totalFraudChecks - data?.fraudDetectedCount - data?.suspiciousTransactions || 0, fill: '#10b981' },
    { name: 'Suspicious', value: data?.suspiciousTransactions || 0, fill: '#d946ef' },
    { name: 'Rejected', value: data?.fraudDetectedCount || 0, fill: '#ef4444' },
  ];

  const mockRulesData = [
    { name: 'Rule A', triggered: 245, effective: 98 },
    { name: 'Rule B', triggered: 221, effective: 95 },
    { name: 'Rule C', triggered: 229, effective: 92 },
    { name: 'Rule D', triggered: 200, effective: 88 },
    { name: 'Rule E', triggered: 150, effective: 85 },
  ];

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
          Fraud & Risk Analytics
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Fraud detection metrics and risk assessment
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
            title="Total Fraud Checks"
            value={data?.totalFraudChecks?.toLocaleString() || '0'}
            icon={SecurityIcon}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatsCard
            title="Fraud Detected"
            value={data?.fraudDetectedCount?.toLocaleString() || '0'}
            icon={WarningIcon}
            color="error"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatsCard
            title="Prevention Rate"
            value={`${(data?.fraudPreventionPercentage || 0).toFixed(1)}%`}
            icon={TrendingDownIcon}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatsCard
            title="Suspicious"
            value={data?.suspiciousTransactions?.toLocaleString() || '0'}
            icon={GavelIcon}
          />
        </Grid>
      </Grid>

      {/* Charts */}
      <Grid container spacing={3}>
        {/* Fraud Decision Distribution */}
        <Grid item xs={12} lg={8}>
          <ChartCard title="Fraud Assessment Decisions" height="400px">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={mockRiskData}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, value }) => `${name}: ${value}`}
                  outerRadius={100}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {mockRiskData.map((entry, index) => (
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

        {/* Summary Stats */}
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
                Assessment Summary
              </Typography>
              {[
                { label: 'Approved', value: data?.totalFraudChecks - data?.fraudDetectedCount - data?.suspiciousTransactions || 0, color: '#10b981' },
                { label: 'Suspicious', value: data?.suspiciousTransactions || 0, color: '#d946ef' },
                { label: 'Rejected', value: data?.fraudDetectedCount || 0, color: '#ef4444' },
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

export default FraudAnalyticsDashboard;

