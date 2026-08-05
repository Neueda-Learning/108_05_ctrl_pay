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
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
} from '@mui/material';
import StatsCard from '../components/StatsCard';
import { dashboardAPI } from '../services/dashboardAPI';
import {
  Gavel as GavelIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon,
  Assessment as AssessmentIcon,
  Visibility as VisibilityIcon,
  Edit as EditIcon,
} from '@mui/icons-material';
import { format } from 'date-fns';

function ComplianceDashboard() {
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
      const compliance = await dashboardAPI.getCompliance();
      setData(compliance);
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

  // Mock audit events for table
  const mockAuditEvents = [
    {
      id: 1,
      eventType: 'Fraud Review',
      description: 'Manual fraud review completed',
      performedBy: 'admin@ctrl-pay.com',
      timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000),
    },
    {
      id: 2,
      eventType: 'Rule Change',
      description: 'Fraud rule updated: Max transaction amount',
      performedBy: 'admin@ctrl-pay.com',
      timestamp: new Date(Date.now() - 6 * 60 * 60 * 1000),
    },
    {
      id: 3,
      eventType: 'Validation Failure',
      description: 'Payment validation failed: Amount exceeds limit',
      performedBy: 'SYSTEM',
      timestamp: new Date(Date.now() - 12 * 60 * 60 * 1000),
    },
    {
      id: 4,
      eventType: 'Admin Action',
      description: 'Payment manually approved by admin',
      performedBy: 'admin@ctrl-pay.com',
      timestamp: new Date(Date.now() - 24 * 60 * 60 * 1000),
    },
  ];

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
          Compliance Dashboard
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Audit logs, rule changes, and compliance metrics
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={4}>
          <StatsCard
            title="Pending Reviews"
            value={data?.pendingReviews?.toLocaleString() || '0'}
            icon={GavelIcon}
            color="warning"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <StatsCard
            title="Manual Reviews"
            value={data?.manualFraudReviews?.toLocaleString() || '0'}
            icon={VisibilityIcon}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <StatsCard
            title="Audit Events"
            value={data?.auditEvents?.toLocaleString() || '0'}
            icon={CheckCircleIcon}
          />
        </Grid>
      </Grid>

      {/* Activity Summary */}
      <Box sx={{ mt: 4 }}>
        <Alert severity="info">
          Additional compliance metrics (validation failures, rule changes, audit trails) require implementation of custom repository queries and audit logging enhancements.
        </Alert>
      </Box>
    </Box>
  );
}

export default ComplianceDashboard;

