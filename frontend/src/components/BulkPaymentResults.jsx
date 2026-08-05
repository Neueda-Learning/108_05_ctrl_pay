import React, { useEffect, useState } from 'react';
import { Box, Button, Card, CardContent, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography, Dialog, DialogTitle, DialogContent, Chip, Alert, useTheme } from '@mui/material';
import { alpha } from '@mui/material/styles';
import { toast } from 'react-toastify';
import DownloadIcon from '@mui/icons-material/Download';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import RefreshIcon from '@mui/icons-material/Refresh';
import api from '../services/api';
import '../pages/BulkPayments.css';

const BulkPaymentResults = ({ batch, onRetry }) => {
  const theme = useTheme();
  const custom = theme.customTokens;
  const [expandedRow, setExpandedRow] = useState(null);
  const [currentBatch, setCurrentBatch] = useState(batch);
  const [loading, setLoading] = useState(false);
  const [selectedTransactions, setSelectedTransactions] = useState([]);
  const [errorDetailsOpen, setErrorDetailsOpen] = useState(false);
  const [selectedErrorDetails, setSelectedErrorDetails] = useState(null);

  // Poll for progress updates
  useEffect(() => {
    if (currentBatch.status === 'PROCESSING' || currentBatch.status === 'VALIDATING') {
      const interval = setInterval(() => {
        pollBatchProgress();
      }, 2000);
      return () => clearInterval(interval);
    }
  }, [currentBatch.status]);

  const pollBatchProgress = async () => {
    try {
      const response = await api.get(`/bulk-payments/${currentBatch.batchId}/progress`);
      // Update UI with progress
      console.log('Progress:', response.data);
    } catch (error) {
      console.error('Error polling progress:', error);
    }
  };

  const downloadResultsCSV = () => {
    let csv = 'Line,Account,Amount,Currency,Status,Error\\n';

    currentBatch.transactionResults.forEach(result => {
      const status = result.status;
      const error = result.failureReason ? `"${result.failureReason}"` : '';
      csv += `${result.lineNumber},"${result.destinationAccount}",${result.amount},"${result.currency}","${status}",${error}\\n`;
    });

    const element = document.createElement('a');
    element.setAttribute('href', 'data:text/csv;charset=utf-8,' + encodeURIComponent(csv));
    element.setAttribute('download', `bulk_payment_${currentBatch.batchReference}_results.csv`);
    element.style.display = 'none';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
    toast.success('Results downloaded');
  };

  const handleRetryFailed = async () => {
    if (!window.confirm('Retry all failed transactions?')) return;

    setLoading(true);
    try {
      const failedItems = currentBatch.transactionResults.filter(r => r.status === 'FAILED');
      // TODO: Call API endpoint to retry failed transactions
      toast.success(`Retrying ${failedItems.length} failed transactions`);
    } catch (error) {
      toast.error('Error retrying transactions: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const toggleRowExpand = (lineNumber) => {
    setExpandedRow(expandedRow === lineNumber ? null : lineNumber);
  };

  const showErrorDetails = (result) => {
    setSelectedErrorDetails(result);
    setErrorDetailsOpen(true);
  };

  const isProcessing = currentBatch.status === 'PROCESSING' || currentBatch.status === 'VALIDATING';
  const completionPercent = (currentBatch.successfulCount + currentBatch.failedCount) / currentBatch.totalTransactions * 100;

  return (
    <div className="bulk-results-container">
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h5" gutterBottom>
            Batch: {currentBatch.batchReference}
          </Typography>

          <Alert severity={
            currentBatch.status === 'COMPLETED' ? 'success' :
            currentBatch.status === 'PARTIALLY_COMPLETED' ? 'warning' :
            currentBatch.status === 'FAILED' ? 'error' : 'info'
          } sx={{ mb: 2 }}>
            Status: <strong>{currentBatch.status}</strong> |
            Timestamp: {new Date(currentBatch.createdAt).toLocaleString()}
          </Alert>

          {/* Summary Cards */}
          <Box className="results-summary">
            <Box className="result-card info">
              <h3>Total</h3>
              <div className="value">{currentBatch.totalTransactions}</div>
            </Box>
            <Box className="result-card success">
              <h3>Successful</h3>
              <div className="value">{currentBatch.successfulCount}</div>
            </Box>
            <Box className="result-card error">
              <h3>Failed</h3>
              <div className="value">{currentBatch.failedCount}</div>
            </Box>
            <Box className="result-card info">
              <h3>Amount</h3>
              <div className="value" style={{ fontSize: '20px' }}>{currentBatch.totalAmount} {currentBatch.transactionResults[0]?.currency}</div>
            </Box>
          </Box>

       {/* Progress Bar */}
           {isProcessing && (
             <Box sx={{ mb: 3 }}>
               <Typography variant="body2" gutterBottom>
                 Processing: {Math.round(completionPercent)}%
               </Typography>
               <Box sx={{ width: '100%', backgroundColor: alpha(custom.border, 0.3), height: 8, borderRadius: 4, overflow: 'hidden' }}>
                 <Box sx={{ width: `${completionPercent}%`, backgroundColor: custom.brand.main, height: '100%', transition: 'width 0.3s' }} />
               </Box>
             </Box>
           )}

          {/* Action Buttons */}
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
            <Button
              variant="outlined"
              startIcon={<DownloadIcon />}
              onClick={downloadResultsCSV}
            >
              Download Results CSV
            </Button>

            {currentBatch.failedCount > 0 && (
              <Button
                variant="contained"
                color="warning"
                startIcon={<RefreshIcon />}
                onClick={handleRetryFailed}
                disabled={loading || isProcessing}
              >
                Retry Failed ({currentBatch.failedCount})
              </Button>
            )}

            {isProcessing && (
              <Button
                variant="outlined"
                startIcon={<RefreshIcon />}
                onClick={() => pollBatchProgress()}
                disabled={loading}
              >
                Refresh
              </Button>
            )}
          </Box>
        </CardContent>
      </Card>

      {/* Successful Transactions */}
      {currentBatch.successfulCount > 0 && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h6" sx={{ mb: 2, color: '#4caf50' }}>
              ✓ Successful Transactions ({currentBatch.successfulCount})
            </Typography>

            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow sx={{ backgroundColor: '#e8f5e9' }}>
                    <TableCell>Line</TableCell>
                    <TableCell>Payment ID</TableCell>
                    <TableCell>Account</TableCell>
                    <TableCell align="right">Amount</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Fraud Score</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {currentBatch.transactionResults
                    .filter(r => r.status === 'SUCCESS')
                    .map((result) => (
                      <TableRow key={result.lineNumber} hover>
                        <TableCell>{result.lineNumber}</TableCell>
                        <TableCell>{result.paymentId}</TableCell>
                        <TableCell>{result.destinationAccount}</TableCell>
                        <TableCell align="right">{result.amount} {result.currency}</TableCell>
                        <TableCell>
                          <Chip label="SUCCESS" size="small" color="success" variant="outlined" />
                        </TableCell>
                        <TableCell>
                          {result.fraudScore && (
                            <Chip
                              label={`${result.fraudScore}% - ${result.fraudDecision}`}
                              size="small"
                              variant="outlined"
                              color={result.fraudScore > 70 ? 'warning' : 'default'}
                            />
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      )}

      {/* Failed Transactions */}
      {currentBatch.failedCount > 0 && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h6" sx={{ mb: 2, color: '#f44336' }}>
              ✗ Failed Transactions ({currentBatch.failedCount})
            </Typography>

            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow sx={{ backgroundColor: alpha(theme.palette.error.main, 0.1) }}>
                    <TableCell>Line</TableCell>
                    <TableCell>Account</TableCell>
                    <TableCell align="right">Amount</TableCell>
                    <TableCell>Error Code</TableCell>
                    <TableCell>Error Message</TableCell>
                    <TableCell>Details</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {currentBatch.transactionResults
                    .filter(r => r.status === 'FAILED')
                    .map((result) => (
                      <TableRow key={result.lineNumber} hover>
                        <TableCell>{result.lineNumber}</TableCell>
                        <TableCell>{result.destinationAccount}</TableCell>
                        <TableCell align="right">{result.amount} {result.currency}</TableCell>
                        <TableCell>
                          <Chip label={result.errorCode} size="small" variant="outlined" />
                        </TableCell>
                        <TableCell sx={{ maxWidth: 300, wordBreak: 'break-word' }}>
                          {result.failureReason}
                        </TableCell>
                        <TableCell>
                          <Button
                            size="small"
                            onClick={() => showErrorDetails(result)}
                          >
                            View
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      )}

      {/* Error Details Dialog */}
      <Dialog open={errorDetailsOpen} onClose={() => setErrorDetailsOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Error Details - Line {selectedErrorDetails?.lineNumber}</DialogTitle>
        <DialogContent>
          {selectedErrorDetails && (
            <Box sx={{ mt: 2 }}>
              <Typography variant="body2"><strong>Account:</strong> {selectedErrorDetails.destinationAccount}</Typography>
              <Typography variant="body2"><strong>Amount:</strong> {selectedErrorDetails.amount} {selectedErrorDetails.currency}</Typography>
              <Typography variant="body2"><strong>Error Code:</strong> {selectedErrorDetails.errorCode}</Typography>
              <Typography variant="body2" sx={{ mt: 2 }}><strong>Error Message:</strong></Typography>
              <Typography variant="body2" sx={{ p: 1, backgroundColor: custom.mutedOverlay, borderRadius: 1, wordBreak: 'break-word', border: `1px solid ${alpha(custom.border, 0.5)}` }}>
                {selectedErrorDetails.failureReason}
              </Typography>

              {selectedErrorDetails.validationErrors && (
                <>
                  <Typography variant="body2" sx={{ mt: 2 }}><strong>Validation Errors:</strong></Typography>
                  <pre style={{ fontSize: '12px', backgroundColor: custom.mutedOverlay, padding: '8px', borderRadius: '4px', maxHeight: 200, overflow: 'auto', border: `1px solid ${alpha(custom.border, 0.5)}` }}>
                    {selectedErrorDetails.validationErrors}
                  </pre>
                </>
              )}
            </Box>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default BulkPaymentResults;

