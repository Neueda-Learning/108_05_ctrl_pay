import React from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Step,
  Stepper,
  StepLabel,
  StepConnector,
  CircularProgress,
  Alert,
  Grid,
} from '@mui/material';
import {
  CheckCircle,
  Error as ErrorIcon,
  HourglassEmpty,
  Refresh,
} from '@mui/icons-material';
import { styled } from '@mui/material/styles';

// Styled components for better visual presentation
const StyledStepConnector = styled(StepConnector)(({ theme }) => ({
  '& .MuiStepConnector-line': {
    borderColor: theme.palette.divider,
  },
  '&.Mui-active .MuiStepConnector-line': {
    borderColor: theme.palette.primary.main,
  },
  '&.Mui-completed .MuiStepConnector-line': {
    borderColor: theme.palette.success.main,
  },
}));

const StyledStepIconBox = styled(Box)(({ theme }) => ({
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  width: 50,
  height: 50,
  borderRadius: '50%',
  backgroundColor: theme.palette.grey[200],
  position: 'relative',
}));

/**
 * PaymentStatusFlow Component
 *
 * Visualizes the payment processing workflow with status progression
 * Shows: Created → Validating → Validated → Sending → Sent → Completing → Completed
 *
 * Props:
 * - currentStatus: Current payment status (CREATED, VALIDATED, SENT, COMPLETED, FAILED)
 * - processingPhase: Current processing phase (creating, validating, sending, completing)
 * - errorDetails: Error information if failed
 * - retryCount: Number of retries attempted
 * - maxRetries: Maximum retries allowed
 */
function PaymentStatusFlow({
  currentStatus,
  processingPhase,
  errorDetails,
  retryCount = 0,
  maxRetries = 3,
}) {
  // Define the workflow steps
  const steps = [
    {
      id: 'create',
      label: 'Create',
      description: 'Payment Created',
      completedStatus: 'CREATED',
      processingPhase: 'creating'
    },
    {
      id: 'validate',
      label: 'Validate',
      description: 'Running Validations',
      completedStatus: 'VALIDATED',
      processingPhase: 'validating'
    },
    {
      id: 'send',
      label: 'Send',
      description: 'Sending to Gateway',
      completedStatus: 'SENT',
      processingPhase: 'sending'
    },
    {
      id: 'complete',
      label: 'Complete',
      description: 'Payment Complete',
      completedStatus: 'COMPLETED',
      processingPhase: 'completing'
    },
  ];

  // Determine which steps are completed
  const statusToStepMap = {
    CREATED: 0,
    VALIDATED: 1,
    SENT: 2,
    COMPLETED: 3,
    FAILED: -1,
  };

  const activeStepIndex = statusToStepMap[currentStatus] ?? -1;

  // Get icon for each step
  const getStepIcon = (stepIndex) => {
    if (currentStatus === 'FAILED') {
      return (
        <StyledStepIconBox sx={{ bgcolor: 'error.lighter', color: 'error.main' }}>
          <ErrorIcon sx={{ fontSize: 28 }} />
        </StyledStepIconBox>
      );
    }

    if (stepIndex < activeStepIndex) {
      // Completed step
      return (
        <StyledStepIconBox sx={{ bgcolor: 'success.lighter', color: 'success.main' }}>
          <CheckCircle sx={{ fontSize: 28 }} />
        </StyledStepIconBox>
      );
    }

    if (stepIndex === activeStepIndex) {
      if (processingPhase === steps[stepIndex]?.processingPhase) {
        // Currently processing
        return (
          <StyledStepIconBox sx={{ bgcolor: 'primary.lighter', color: 'primary.main' }}>
            <CircularProgress size={28} />
          </StyledStepIconBox>
        );
      }
      // Just reached this step
      return (
        <StyledStepIconBox sx={{ bgcolor: 'primary.lighter', color: 'primary.main' }}>
          <CheckCircle sx={{ fontSize: 28 }} />
        </StyledStepIconBox>
      );
    }

    // Pending step
    return (
      <StyledStepIconBox sx={{ bgcolor: 'grey.200', color: 'grey.500' }}>
        <HourglassEmpty sx={{ fontSize: 28 }} />
      </StyledStepIconBox>
    );
  };

  return (
    <Card sx={{ mb: 3 }}>
      <CardContent>
        <Box sx={{ mb: 3 }}>
          <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
            Payment Processing Status
          </Typography>
          <Typography variant="body2" color="textSecondary">
            Real-time progress tracking of your payment
          </Typography>
        </Box>

        {/* Main Stepper */}
        <Box sx={{ mb: 4 }}>
          <Stepper
            activeStep={activeStepIndex}
            connector={<StyledStepConnector />}
            sx={{ mb: 2 }}
          >
            {steps.map((step, index) => (
              <Step key={step.id} completed={index < activeStepIndex}>
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                  {getStepIcon(index)}
                  <StepLabel
                    sx={{
                      '& .MuiStepLabel-label': {
                        marginTop: 1,
                        textAlign: 'center',
                      },
                    }}
                  >
                    <Typography variant="body2" sx={{ fontWeight: 600 }}>
                      {step.label}
                    </Typography>
                  </StepLabel>
                  <Typography
                    variant="caption"
                    color="textSecondary"
                    sx={{ textAlign: 'center', mt: 0.5 }}
                  >
                    {step.description}
                  </Typography>
                </Box>
              </Step>
            ))}
          </Stepper>
        </Box>

        {/* Status Messages */}
        <Box sx={{ mt: 3 }}>
          {currentStatus === 'COMPLETED' && (
            <Alert severity="success" sx={{ mb: 2 }}>
              ✓ Payment completed successfully! Your transaction has been processed.
            </Alert>
          )}

          {currentStatus === 'FAILED' && (
            <Alert severity="error" sx={{ mb: 2 }}>
              ✗ Payment failed: {errorDetails?.message || 'An error occurred'}
              {errorDetails?.code && ` (Code: ${errorDetails.code})`}
            </Alert>
          )}

          {processingPhase && currentStatus !== 'COMPLETED' && currentStatus !== 'FAILED' && (
            <Alert severity="info" sx={{ mb: 2 }}>
              ⏳ Currently {steps[activeStepIndex]?.description?.toLowerCase() || 'processing'}...
              Please wait while we process your payment.
            </Alert>
          )}

          {currentStatus === 'FAILED' && retryCount > 0 && (
            <Alert severity="warning" sx={{ mb: 2 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Refresh sx={{ fontSize: 20 }} />
                Retry attempt {retryCount} of {maxRetries}
              </Box>
            </Alert>
          )}
        </Box>

        {/* Additional Info */}
        <Box
          sx={{
            mt: 3,
            p: 2,
            backgroundColor: 'background.default',
            borderRadius: 1,
            border: '1px solid',
            borderColor: 'divider',
          }}
        >
          <Grid container spacing={2}>
            <Grid item xs={6}>
              <Typography variant="caption" color="textSecondary">
                Current Status
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                {currentStatus}
              </Typography>
            </Grid>
            <Grid item xs={6}>
              <Typography variant="caption" color="textSecondary">
                Processing Phase
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                {processingPhase || 'Idle'}
              </Typography>
            </Grid>
          </Grid>
        </Box>
      </CardContent>
    </Card>
  );
}

export default PaymentStatusFlow;



