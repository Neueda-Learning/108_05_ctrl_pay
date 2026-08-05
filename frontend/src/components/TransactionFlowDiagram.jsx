import React from 'react';
import { alpha } from '@mui/material/styles';
import {
  Box,
  Card,
  CardContent,
  Typography,
  useTheme,
  useMediaQuery,
  Grid,
} from '@mui/material';
import {
  PersonAdd,
  CheckCircle,
  CreditCard,
  Payment,
  Analytics,
  ArrowForward,
  ArrowDownward,
} from '@mui/icons-material';

function TransactionFlowDiagram() {
  const theme = useTheme();
  const custom = theme.customTokens;
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  const steps = [
    {
      number: '1',
      title: 'Create Customer Profile',
      description: 'Collect customer details and create a profile',
      icon: PersonAdd,
      color: '#3B82F6',
      bgColor: 'rgba(59, 130, 246, 0.1)',
    },
    {
      number: '2',
      title: 'Validate Customer ID',
      description: 'Verify the customer ID exists before proceeding',
      icon: CheckCircle,
      color: '#10B981',
      bgColor: 'rgba(16, 185, 129, 0.1)',
    },
    {
      number: '3',
      title: 'Create Account',
      description: 'Link a new account to the validated customer',
      icon: CreditCard,
      color: '#F59E0B',
      bgColor: 'rgba(245, 158, 11, 0.1)',
    },
    {
      number: '4',
      title: 'Make Payment',
      description: 'Use the account to initiate a payment transaction',
      icon: Payment,
      color: '#8B5CF6',
      bgColor: 'rgba(139, 92, 246, 0.1)',
    },
    {
      number: '5',
      title: 'View Statistics',
      description: 'Monitor and analyze payment history and trends',
      icon: Analytics,
      color: '#EC4899',
      bgColor: 'rgba(236, 72, 153, 0.1)',
    },
  ];

  const StepCard = ({ step, isLast }) => {
    const Icon = step.icon;
    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
        <Card
          sx={{
            width: '100%',
            maxWidth: 200,
            background: step.bgColor,
            border: `2px solid ${step.color}`,
            borderRadius: '12px',
            transition: 'all 0.3s ease',
            '&:hover': {
              transform: 'translateY(-4px)',
              boxShadow: `0 8px 20px ${alpha(step.color, 0.3)}`,
            },
          }}
        >
          <CardContent sx={{ textAlign: 'center', p: 2 }}>
            <Box
              sx={{
                width: 56,
                height: 56,
                borderRadius: '50%',
                background: step.color,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                mx: 'auto',
                mb: 1.5,
              }}
            >
              <Typography sx={{ color: '#FFFFFF', fontWeight: 700, fontSize: '1.5rem' }}>
                {step.number}
              </Typography>
            </Box>
            <Icon sx={{ color: step.color, fontSize: 32, mb: 1 }} />
            <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 0.5 }}>
              {step.title}
            </Typography>
            <Typography variant="caption" sx={{ color: custom.text.muted }}>
              {step.description}
            </Typography>
          </CardContent>
        </Card>

        {!isLast && (
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              py: 1,
            }}
          >
            {isMobile ? (
              <ArrowDownward sx={{ color: custom.brand.main, fontSize: 28 }} />
            ) : (
              <ArrowForward sx={{ color: custom.brand.main, fontSize: 28 }} />
            )}
          </Box>
        )}
      </Box>
    );
  };

  return (
    <Card
      sx={{
        background: `linear-gradient(135deg, ${alpha(custom.brand.main, 0.05)} 0%, ${alpha(custom.brand.accent, 0.03)} 100%)`,
        border: `1px solid ${alpha(custom.border, 0.6)}`,
        borderRadius: '16px',
      }}
    >
      <CardContent sx={{ p: 3 }}>
        {/* Header */}
        <Box sx={{ mb: 4, textAlign: 'center' }}>
          <Typography
            variant="h5"
            sx={{
              fontWeight: 700,
              mb: 1,
              background: `linear-gradient(135deg, ${custom.brand.main} 0%, ${custom.brand.accent} 100%)`,
              backgroundClip: 'text',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
            }}
          >
            Transaction Lifecycle
          </Typography>
          <Typography variant="body2" sx={{ color: custom.text.muted }}>
            Follow these 5 simple steps to process payments successfully
          </Typography>
        </Box>

        {/* Flow Diagram */}
        {isMobile ? (
          // Stack vertically on mobile
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {steps.map((step, index) => (
              <Box key={step.number}>
                <StepCard step={step} isLast={index === steps.length - 1} />
              </Box>
            ))}
          </Box>
        ) : (
          // Horizontal layout on desktop
          <Box
            sx={{
              display: 'flex',
              gap: 2,
              alignItems: 'center',
              justifyContent: 'center',
              overflowX: 'auto',
              pb: 1,
              px: 1,
            }}
          >
            {steps.map((step, index) => (
              <Box
                key={step.number}
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 2,
                  flexShrink: 0,
                  minWidth: 'max-content',
                }}
              >
                <StepCard step={step} isLast={index === steps.length - 1} />
              </Box>
            ))}
          </Box>
        )}

        {/* Footer Info */}
        <Box
          sx={{
            mt: 4,
            p: 2,
            borderRadius: '12px',
            background: alpha(custom.brand.main, 0.08),
            border: `1px solid ${alpha(custom.brand.main, 0.2)}`,
          }}
        >
          <Typography variant="body2" sx={{ color: custom.text.secondary, textAlign: 'center' }}>
            💡 <strong>Tip:</strong> Each step must be completed in order for the transaction flow to work correctly.
            Start by creating a customer profile, then validate the ID before proceeding with account and payment creation.
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
}

export default TransactionFlowDiagram;

