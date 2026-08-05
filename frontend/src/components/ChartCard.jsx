import React from 'react';
import { alpha } from '@mui/material/styles';
import {
  Box,
  Card,
  CardContent,
  Typography,
  useTheme,
  CircularProgress,
} from '@mui/material';

/**
 * Reusable component for displaying chart containers on dashboards.
 */
function ChartCard({
  title,
  subtitle = '',
  children,
  loading = false,
  height = '400px',
}) {
  const theme = useTheme();
  const custom = theme.customTokens;

  return (
    <Card
      sx={{
        background: custom.gridRow,
        backdropFilter: 'blur(20px)',
        WebkitBackdropFilter: 'blur(20px)',
        border: `1px solid ${alpha(custom.border, 0.6)}`,
        borderRadius: '16px',
        transition: 'all 0.3s cubic-bezier(0.4,0,0.2,1)',
        height,
        display: 'flex',
        flexDirection: 'column',
        '&:hover': {
          border: `1px solid ${alpha(custom.brand.main, 0.8)}`,
          boxShadow: `0 8px 32px ${alpha(custom.brand.main, 0.15)}`,
        },
      }}
    >
      <CardContent sx={{ py: 2 }}>
        <Typography
          variant="body2"
          sx={{
            color: custom.text.muted,
            fontWeight: 600,
            fontSize: '0.75rem',
            letterSpacing: 1.2,
            textTransform: 'uppercase',
            mb: 0.5,
          }}
        >
          {title}
        </Typography>
        {subtitle && (
          <Typography
            variant="caption"
            sx={{
              color: custom.text.muted,
              fontSize: '0.8rem',
              mb: 1,
            }}
          >
            {subtitle}
          </Typography>
        )}
      </CardContent>

      <Box
        sx={{
          flex: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          px: 2,
          pb: 2,
          minHeight: 0,
        }}
      >
        {loading ? (
          <CircularProgress />
        ) : (
          <Box sx={{ width: '100%', height: '100%' }}>
            {children}
          </Box>
        )}
      </Box>
    </Card>
  );
}

export default ChartCard;

