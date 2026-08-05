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
  CardContent,
} from '@mui/material';
import {
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  Radar,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
} from 'recharts';
import StatsCard from '../components/StatsCard';
import ChartCard from '../components/ChartCard';
import { dashboardAPI } from '../services/dashboardAPI';
import {
  SmartToy as SmartToyIcon,
  TrendingUp as TrendingUpIcon,
  Speed as SpeedIcon,
  Info as InfoIcon,
} from '@mui/icons-material';

function MLModelDashboard() {
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
      const model = await dashboardAPI.getMLModel();
      setData(model);
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

  const metricsData = [
    { name: 'Accuracy', value: data?.accuracy || 0 },
    { name: 'Precision', value: data?.precision || 0 },
    { name: 'Recall', value: data?.recall || 0 },
    { name: 'F1 Score', value: data?.f1Score || 0 },
    { name: 'AUC', value: data?.aucScore || 0 },
  ];

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
          ML Model Dashboard
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Active model performance and metrics
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Model Info */}
      <Card
        sx={{
          background: custom.gridRow,
          backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
          border: `1px solid ${alpha(custom.border, 0.6)}`,
          borderRadius: '16px',
          mb: 4,
        }}
      >
        <CardContent>
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6} md={3}>
              <Box>
                <Typography variant="caption" sx={{ color: custom.text.muted, textTransform: 'uppercase' }}>
                  Model Name
                </Typography>
                <Typography variant="h6" sx={{ color: custom.text.primary, fontWeight: 700 }}>
                  {data?.activeModelName || 'N/A'}
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Box>
                <Typography variant="caption" sx={{ color: custom.text.muted, textTransform: 'uppercase' }}>
                  Version
                </Typography>
                <Typography variant="h6" sx={{ color: custom.text.primary, fontWeight: 700 }}>
                  {data?.modelVersion || 'N/A'}
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Box>
                <Typography variant="caption" sx={{ color: custom.text.muted, textTransform: 'uppercase' }}>
                  Training Dataset
                </Typography>
                <Typography variant="h6" sx={{ color: custom.text.primary, fontWeight: 700 }}>
                  {data?.trainingDataset || 'N/A'}
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Box>
                <Typography variant="caption" sx={{ color: custom.text.muted, textTransform: 'uppercase' }}>
                  Last Updated
                </Typography>
                <Typography variant="h6" sx={{ color: custom.text.primary, fontWeight: 700 }}>
                  {data?.lastUpdated ? new Date(data.lastUpdated).toLocaleDateString() : 'N/A'}
                </Typography>
              </Box>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Performance Stats */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={2.4}>
          <StatsCard
            title="Total Predictions"
            value={data?.totalPredictions?.toLocaleString() || '0'}
            icon={SmartToyIcon}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={2.4}>
          <StatsCard
            title="Accuracy"
            value={`${(data?.accuracy || 0).toFixed(2)}%`}
            icon={TrendingUpIcon}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={2.4}>
          <StatsCard
            title="Precision"
            value={`${(data?.precision || 0).toFixed(2)}%`}
            icon={InfoIcon}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={2.4}>
          <StatsCard
            title="Recall"
            value={`${(data?.recall || 0).toFixed(2)}%`}
            icon={TrendingUpIcon}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={2.4}>
          <StatsCard
            title="Latency"
            value={`${(data?.averagePredictionLatencyMs || 0).toFixed(1)}ms`}
            icon={SpeedIcon}
          />
        </Grid>
      </Grid>

      {/* Performance Radar */}
      <Grid container spacing={3}>
        <Grid item xs={12} lg={6}>
          <ChartCard title="Performance Metrics" height="400px">
            <ResponsiveContainer width="100%" height="100%">
              <RadarChart data={metricsData}>
                <PolarGrid stroke={alpha(custom.border, 0.5)} />
                <PolarAngleAxis dataKey="name" stroke={custom.text.muted} />
                <PolarRadiusAxis stroke={custom.text.muted} />
                <Radar
                  name="Score"
                  dataKey="value"
                  stroke={custom.brand.main}
                  fill={custom.brand.main}
                  fillOpacity={0.6}
                />
                <RechartsTooltip
                  contentStyle={{
                    background: custom.drawer,
                    border: `1px solid ${custom.border}`,
                    borderRadius: '8px',
                  }}
                />
              </RadarChart>
            </ResponsiveContainer>
          </ChartCard>
        </Grid>

        {/* Model Details */}
        <Grid item xs={12} lg={6}>
          <Card
            sx={{
              background: custom.gridRow,
              backdropFilter: 'blur(20px)',
              WebkitBackdropFilter: 'blur(20px)',
              border: `1px solid ${alpha(custom.border, 0.6)}`,
              borderRadius: '16px',
              height: '400px',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
            }}
          >
            <CardContent>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                {[
                  { label: 'F1 Score', value: `${(data?.f1Score || 0).toFixed(4)}` },
                  { label: 'AUC Score', value: `${(data?.aucScore || 0).toFixed(4)}` },
                  { label: 'Accuracy', value: `${(data?.accuracy || 0).toFixed(2)}%` },
                  { label: 'Precision', value: `${(data?.precision || 0).toFixed(2)}%` },
                  { label: 'Recall', value: `${(data?.recall || 0).toFixed(2)}%` },
                ].map((item) => (
                  <Box key={item.label} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="body2" sx={{ color: custom.text.secondary }}>
                      {item.label}
                    </Typography>
                    <Typography variant="h6" sx={{ color: custom.brand.main, fontWeight: 700 }}>
                      {item.value}
                    </Typography>
                  </Box>
                ))}
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}

export default MLModelDashboard;

