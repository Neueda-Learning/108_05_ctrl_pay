import React from 'react';
import { alpha } from '@mui/material/styles';
import {
  Box,
  Card,
  CardContent,
  Typography,
  useTheme,
  Skeleton,
} from '@mui/material';

/**
 * Reusable component for displaying statistics cards on dashboards.
 */
function StatsCard({
  title,
  value,
  subtitle = '',
  icon: Icon = null,
  color = 'primary',
  loading = false,
  trend = null,
}) {
  const theme = useTheme();
  const custom = theme.customTokens;

  if (loading) {
    return (
      <Card
        sx={{
          background: custom.gridRow,
          backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
          border: `1px solid ${alpha(custom.border, 0.6)}`,
          borderRadius: '16px',
          transition: 'all 0.3s cubic-bezier(0.4,0,0.2,1)',
          height: '100%',
        }}
      >
        <CardContent>
          <Skeleton variant="text" width="60%" />
          <Skeleton variant="text" width="80%" sx={{ mt: 1 }} />
          <Skeleton variant="rectangular" height={40} sx={{ mt: 2 }} />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card
      sx={{
        background: custom.gridRow,
        backdropFilter: 'blur(20px)',
        WebkitBackdropFilter: 'blur(20px)',
        border: `1px solid ${alpha(custom.border, 0.6)}`,
        borderRadius: '16px',
        transition: 'all 0.3s cubic-bezier(0.4,0,0.2,1)',
        height: '100%',
        '&:hover': {
          border: `1px solid ${alpha(custom.brand.main, 0.8)}`,
          boxShadow: `0 8px 32px ${alpha(custom.brand.main, 0.15)}`,
          transform: 'translateY(-4px)',
        },
      }}
    >
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <Box>
            <Typography
              variant="body2"
              sx={{
                color: custom.text.muted,
                fontWeight: 600,
                fontSize: '0.75rem',
                letterSpacing: 1.2,
                textTransform: 'uppercase',
                mb: 1,
              }}
            >
              {title}
            </Typography>
            <Typography
              variant="h4"
              sx={{
                color: custom.text.primary,
                fontWeight: 700,
                mb: subtitle ? 0.5 : 0,
              }}
            >
              {value}
            </Typography>
            {subtitle && (
              <Typography
                variant="caption"
                sx={{
                  color: custom.text.muted,
                  fontSize: '0.8rem',
                }}
              >
                {subtitle}
              </Typography>
            )}
          </Box>
          {Icon && (
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: 48,
                height: 48,
                borderRadius: '12px',
                background: alpha(custom.brand.main, 0.15),
              }}
            >
              <Icon
                sx={{
                  color: custom.brand.main,
                  fontSize: '1.5rem',
                }}
              />
            </Box>
          )}
        </Box>
        {trend && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mt: 2 }}>
            <Typography
              variant="caption"
              sx={{
                color: trend > 0 ? theme.palette.success.main : theme.palette.error.main,
                fontWeight: 600,
              }}
            >
              {trend > 0 ? '↑' : '↓'} {Math.abs(trend)}%
            </Typography>
            <Typography
              variant="caption"
              sx={{
                color: custom.text.muted,
              }}
            >
              vs last period
            </Typography>
          </Box>
        )}
      </CardContent>
    </Card>
  );
}

export default StatsCard;

