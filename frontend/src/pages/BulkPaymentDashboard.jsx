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
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  ResponsiveContainer,
} from 'recharts';
import StatsCard from '../components/StatsCard';
import ChartCard from '../components/ChartCard';
import { dashboardAPI } from '../services/dashboardAPI';
import {
  BatchPrediction as BatchPredictionIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon,
  Undo as UndoIcon,
} from '@mui/icons-material';

function BulkPaymentDashboard() {
  const theme = useTheme();
  const custom = theme.customTokens;

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
          Bulk Payment Analytics
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Bulk payment batch performance and success metrics
        </Typography>
      </Box>

      <Alert severity="info" sx={{ mb: 3 }}>
        Bulk payment analytics will be available once bulk payment batches are created in the system. Currently, no bulk payment data is available.
      </Alert>

      {/* Stats Cards - All Zeros */}
      <Grid container spacing={3}>
        <Grid item xs={12} sm={6} md={3}>
          <StatsCard
            title="Total Batches"
            value="0"
            icon={BatchPredictionIcon}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatsCard
            title="Successful Payments"
            value="0"
            icon={CheckCircleIcon}
            color="success"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatsCard
            title="Failed Payments"
            value="0"
            icon={ErrorIcon}
            color="error"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatsCard
            title="Rollbacks"
            value="0"
            icon={UndoIcon}
          />
        </Grid>
      </Grid>
    </Box>
  );
}

export default BulkPaymentDashboard;

