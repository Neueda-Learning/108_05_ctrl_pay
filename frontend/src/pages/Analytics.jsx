import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Grid,
  Typography,
  CircularProgress,
} from '@mui/material';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { analyticsAPI } from '../services/api';

function Analytics() {
  const [loading, setLoading] = useState(true);
  const [analytics, setAnalytics] = useState({
    successRate: 0,
    statusDistribution: {},
    volume: 0,
  });

  useEffect(() => {
    fetchAnalytics();
  }, []);

  const fetchAnalytics = async () => {
    try {
      setLoading(true);
      const [successRes, distRes, volRes] = await Promise.all([
        analyticsAPI.getSuccessRate(),
        analyticsAPI.getStatusDistribution(),
        analyticsAPI.getVolume(),
      ]);

      setAnalytics({
        successRate: parseFloat(successRes.data.success_rate_percent),
        statusDistribution: distRes.data,
        volume: successRes.data.total_payments,
      });
    } catch (error) {
      console.error('Error fetching analytics:', error);
    } finally {
      setLoading(false);
    }
  };

  const chartData = [
    { date: 'Mon', volume: 120, success: 100, failure: 20 },
    { date: 'Tue', volume: 150, success: 130, failure: 20 },
    { date: 'Wed', volume: 140, success: 120, failure: 20 },
    { date: 'Thu', volume: 180, success: 160, failure: 20 },
    { date: 'Fri', volume: 200, success: 180, failure: 20 },
    { date: 'Sat', volume: 90, success: 81, failure: 9 },
    { date: 'Sun', volume: 70, success: 63, failure: 7 },
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
      <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>
        Analytics & Reports
      </Typography>

      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Total Volume
              </Typography>
              <Typography variant="h4" sx={{ fontWeight: 700 }}>
                {analytics.volume.toLocaleString()}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Success Rate
              </Typography>
              <Typography variant="h4" sx={{ fontWeight: 700, color: 'success.main' }}>
                {analytics.successRate.toFixed(1)}%
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Completed
              </Typography>
              <Typography variant="h4" sx={{ fontWeight: 700 }}>
                {analytics.statusDistribution.COMPLETED || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Failed
              </Typography>
              <Typography variant="h4" sx={{ fontWeight: 700, color: 'error.main' }}>
                {analytics.statusDistribution.FAILED || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Payment Volume Trend
              </Typography>
              <ResponsiveContainer width="100%" height={400}>
                <BarChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="date" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="volume" fill="#1976d2" />
                  <Bar dataKey="success" fill="#2e7d32" />
                  <Bar dataKey="failure" fill="#d32f2f" />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}

export default Analytics;

