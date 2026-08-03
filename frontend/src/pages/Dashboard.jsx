import React, { useState, useEffect } from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  LinearProgress,
  CircularProgress,
  Button,
} from '@mui/material';
import {
  TrendingUp,
  CheckCircle,
  Error as ErrorIcon,
  HourglassEmpty,
} from '@mui/icons-material';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import { analyticsAPI, paymentAPI } from '../services/api';

const COLORS = ['#2e7d32', '#d32f2f', '#f57c00', '#1976d2'];

function StatCard({ icon: Icon, title, value, change, color }) {
  return (
    <Card sx={{ height: '100%', position: 'relative', overflow: 'visible' }}>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <Box>
            <Typography color="textSecondary" gutterBottom variant="subtitle2">
              {title}
            </Typography>
            <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
              {value}
            </Typography>
            {change && (
              <Typography variant="body2" sx={{ color: change > 0 ? 'success.main' : 'error.main' }}>
                {change > 0 ? '↑' : '↓'} {Math.abs(change)}% vs last month
              </Typography>
            )}
          </Box>
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 60,
              height: 60,
              borderRadius: '50%',
              backgroundColor: `${color}15`,
            }}
          >
            <Icon sx={{ fontSize: 32, color }} />
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
}

function Dashboard() {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    successRate: 0,
    totalPayments: 0,
    completed: 0,
    failed: 0,
    distribution: [],
  });

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      const [successRes, distRes] = await Promise.all([
        analyticsAPI.getSuccessRate(),
        analyticsAPI.getStatusDistribution(),
      ]);

      const successData = successRes.data;
      const distData = distRes.data;

      setStats({
        successRate: parseFloat(successData.success_rate_percent),
        totalPayments: successData.total_payments,
        completed: successData.completed,
        failed: successData.failed,
        distribution: [
          { name: 'Completed', value: successData.completed },
          { name: 'Failed', value: successData.failed },
          { name: 'Pending', value: successData.pending },
        ],
      });
    } catch (error) {
      console.error('Error fetching dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  // Mock chart data
  const chartData = [
    { date: 'Mon', payments: 120, completed: 100, failed: 20 },
    { date: 'Tue', payments: 150, completed: 130, failed: 20 },
    { date: 'Wed', payments: 140, completed: 120, failed: 20 },
    { date: 'Thu', payments: 180, completed: 160, failed: 20 },
    { date: 'Fri', payments: 200, completed: 180, failed: 20 },
    { date: 'Sat', payments: 90, completed: 81, failed: 9 },
    { date: 'Sun', payments: 70, completed: 63, failed: 7 },
  ];

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
          Dashboard
        </Typography>
        <Typography variant="body2" color="textSecondary">
          Welcome to Ctrl-Pay Payment Processing System
        </Typography>
      </Box>

      {/* KPI Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            icon={CheckCircle}
            title="Total Payments"
            value={stats.totalPayments.toLocaleString()}
            change={12}
            color="#2e7d32"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            icon={TrendingUp}
            title="Success Rate"
            value={`${stats.successRate.toFixed(1)}%`}
            change={5}
            color="#1976d2"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            icon={CheckCircle}
            title="Completed"
            value={stats.completed.toLocaleString()}
            change={8}
            color="#2e7d32"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            icon={ErrorIcon}
            title="Failed"
            value={stats.failed.toLocaleString()}
            change={-3}
            color="#d32f2f"
          />
        </Grid>
      </Grid>

      {/* Charts */}
      <Grid container spacing={3}>
        <Grid item xs={12} lg={8}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Payment Trend (Last 7 Days)
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="date" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Line
                    type="monotone"
                    dataKey="completed"
                    stroke="#2e7d32"
                    strokeWidth={2}
                    dot={{ fill: '#2e7d32' }}
                  />
                  <Line
                    type="monotone"
                    dataKey="failed"
                    stroke="#d32f2f"
                    strokeWidth={2}
                    dot={{ fill: '#d32f2f' }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} lg={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Status Distribution
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={stats.distribution}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, value }) => `${name}: ${value}`}
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {stats.distribution.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Recent Activity
              </Typography>
              <Box sx={{ space: 2 }}>
                <Box sx={{ mb: 2 }}>
                  <Typography variant="body2" sx={{ mb: 1 }}>
                    Payment Creation Rate
                  </Typography>
                  <LinearProgress variant="determinate" value={75} />
                </Box>
                <Box sx={{ mb: 2 }}>
                  <Typography variant="body2" sx={{ mb: 1 }}>
                    Validation Success Rate
                  </Typography>
                  <LinearProgress variant="determinate" value={90} sx={{ '& .MuiLinearProgress-bar': { backgroundColor: '#2e7d32' } }} />
                </Box>
                <Box>
                  <Typography variant="body2" sx={{ mb: 1 }}>
                    Processing Efficiency
                  </Typography>
                  <LinearProgress variant="determinate" value={85} sx={{ '& .MuiLinearProgress-bar': { backgroundColor: '#1976d2' } }} />
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Quick Actions */}
      <Box sx={{ mt: 4, display: 'flex', gap: 2 }}>
        <Button variant="contained" color="primary" size="large">
          Create Payment
        </Button>
        <Button variant="outlined" color="primary" size="large">
          View Reports
        </Button>
      </Box>
    </Box>
  );
}

export default Dashboard;

