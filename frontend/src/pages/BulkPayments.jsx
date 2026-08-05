import React, { useState, useRef } from 'react';
import { Box, Button, Card, CardContent, Stepper, Step, StepLabel, Tab, Tabs, Alert, CircularProgress, Typography, TextField } from '@mui/material';
import { toast } from 'react-toastify';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import api from '../services/api';
import BulkPaymentResults from '../components/BulkPaymentResults';
import './BulkPayments.css';

const BulkPayments = () => {
  const [mode, setMode] = useState('upload'); // 'upload' or 'manual'
  const [activeStep, setActiveStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [csvValidationResult, setCsvValidationResult] = useState(null);
  const [batchResult, setBatchResult] = useState(null);

  // Upload mode state
  const [selectedFile, setSelectedFile] = useState(null);
  const fileInputRef = useRef(null);

  // Manual mode state
  const [sourceAccount, setSourceAccount] = useState('');
  const [manualItems, setManualItems] = useState([{ destination: '', amount: '', currency: 'USD', description: '' }]);

  // Sample CSV reference
  const sampleCSVContent = `destinationAccount,amount,currency,description
987654321001,1000,USD,Rent payment
987654321002,500,USD,Invoice payment
987654321003,2000,EUR,Vendor payment`;

  // ================== UPLOAD MODE ==================

  const handleFileSelect = (event) => {
    const file = event.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      setCsvValidationResult(null);
    }
  };

const validateCSV = async () => {
    if (!selectedFile) {
      toast.error('Please select a CSV file');
      return;
    }

    setLoading(true);
    try {
      const formData = new FormData();
      formData.append('file', selectedFile);

      const response = await api.post('/bulk-payments/validate-csv', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      setCsvValidationResult(response.data);

      if (response.data.isValid) {
        toast.success(`CSV is valid! ${response.data.validRecords} records ready for processing`);
        setActiveStep(1);
      } else {
        toast.warning(`CSV has ${response.data.invalidRecords} errors`);
      }
    } catch (error) {
      toast.error('Error validating CSV: ' + (error.response?.data?.message || error.message));
      console.error('CSV validation error:', error);
    } finally {
      setLoading(false);
    }
  };

  const downloadSampleCSV = () => {
    const element = document.createElement('a');
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(sampleCSVContent));
    element.setAttribute('download', 'bulk_payment_sample.csv');
    element.style.display = 'none';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  };

  const proceedWithCSV = async () => {
    if (!csvValidationResult?.isValid) {
      toast.error('Please fix CSV errors before proceeding');
      return;
    }

    // Read CSV and send to backend
    setLoading(true);
    try {
      const text = await selectedFile.text();
      const lines = text.split('\n').slice(1); // Skip header
      const items = lines
        .filter(line => line.trim())
        .map(line => {
          const [dest, amount, currency, description] = line.split(',').map(v => v.trim());
          return {
            destinationAccount: dest,
            amount: parseFloat(amount),
            currency: currency,
            description: description
          };
        });

      // Get source account (TODO: from authenticated user)
      const sourceAccount = prompt('Enter source account number:');
      if (!sourceAccount) return;

      const response = await api.post('/bulk-payments', {
        sourceAccount,
        items,
        idempotencyKey: `upload-${Date.now()}`
      });

      setBatchResult(response.data);
      toast.success(`Bulk payment batch created: ${response.data.batchReference}`);
      setActiveStep(2);
    } catch (error) {
      toast.error('Error creating batch: ' + (error.response?.data?.message || error.message));
      console.error('Batch creation error:', error);
    } finally {
      setLoading(false);
    }
  };

  // ================== MANUAL MODE ==================

  const handleAddRow = () => {
    setManualItems([...manualItems, { destination: '', amount: '', currency: 'USD', description: '' }]);
  };

  const handleRemoveRow = (index) => {
    setManualItems(manualItems.filter((_, i) => i !== index));
  };

  const handleItemChange = (index, field, value) => {
    const newItems = [...manualItems];
    newItems[index][field] = value;
    setManualItems(newItems);
  };

  const validateManualItems = () => {
    if (!sourceAccount) {
      toast.error('Please enter source account');
      return false;
    }
    if (manualItems.length === 0) {
      toast.error('Please add at least one payment item');
      return false;
    }

    for (let i = 0; i < manualItems.length; i++) {
      const item = manualItems[i];
      if (!item.destination || !item.amount || !item.currency) {
        toast.error(`Row ${i + 1}: Please fill in all required fields`);
        return false;
      }
      if (!/^\d{12}$/.test(item.destination)) {
        toast.error(`Row ${i + 1}: Destination account must be 12 digits`);
        return false;
      }
      if (isNaN(item.amount) || parseFloat(item.amount) <= 0) {
        toast.error(`Row ${i + 1}: Amount must be a positive number`);
        return false;
      }
      if (!/^[A-Z]{3}$/.test(item.currency)) {
        toast.error(`Row ${i + 1}: Currency must be 3 uppercase letters (e.g., USD)`);
        return false;
      }
    }

    if (sourceAccount === manualItems[0].destination) {
      toast.error('Source and destination accounts cannot be the same');
      return false;
    }

    return true;
  };

  const submitManualPayments = async () => {
    if (!validateManualItems()) return;

    setLoading(true);
    try {
      const items = manualItems.map(item => ({
        destinationAccount: item.destination,
        amount: parseFloat(item.amount),
        currency: item.currency,
        description: item.description || null
      }));

      const response = await api.post('/bulk-payments', {
        sourceAccount,
        items,
        idempotencyKey: `manual-${Date.now()}`
      });

      setBatchResult(response.data);
      toast.success(`Bulk payment batch created: ${response.data.batchReference}`);
      setActiveStep(2);
    } catch (error) {
      toast.error('Error creating batch: ' + (error.response?.data?.message || error.message));
      console.error('Batch creation error:', error);
    } finally {
      setLoading(false);
    }
  };

  // ================== RESULTS VIEW ==================

  const handleRetry = async () => {
    if (!batchResult) return;
    // TODO: Implement retry logic for failed transactions
    toast.info('Retry functionality coming soon');
  };

  return (
    <div className="bulk-payments-container">
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" gutterBottom>
          Bulk Payments
        </Typography>
        <Typography variant="body2" color="textSecondary">
          Process multiple payments in a single batch operation
        </Typography>
      </Box>

      <Stepper activeStep={activeStep} sx={{ mb: 4 }}>
        <Step>
          <StepLabel>{mode === 'upload' ? 'Upload CSV' : 'Enter Payments'}</StepLabel>
        </Step>
        <Step>
          <StepLabel>Validate</StepLabel>
        </Step>
        <Step>
          <StepLabel>Results</StepLabel>
        </Step>
      </Stepper>

      {activeStep === 0 && (
        <Box>
          {/* Mode Selection */}
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Tabs value={mode === 'upload' ? 0 : 1} onChange={(e, newValue) => {
                setMode(newValue === 0 ? 'upload' : 'manual');
                setActiveStep(0);
                setCsvValidationResult(null);
              }}>
                <Tab label="CSV Upload" />
                <Tab label="Manual Entry" />
              </Tabs>
            </CardContent>
          </Card>

          {/* Upload Mode */}
          {mode === 'upload' && (
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Upload CSV File
                </Typography>

                <Box sx={{ mb: 3, p: 2, border: '2px dashed #ccc', borderRadius: 2, textAlign: 'center', cursor: 'pointer' }}
                  onClick={() => fileInputRef.current?.click()}
                >
                  <CloudUploadIcon sx={{ fontSize: 48, color: '#1976d2', mb: 1 }} />
                  <Typography>
                    Click to upload or drag and drop
                  </Typography>
                  <Typography variant="caption" color="textSecondary">
                    CSV files only
                  </Typography>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept=".csv"
                    hidden
                    onChange={handleFileSelect}
                  />
                </Box>

                {selectedFile && (
                  <Alert severity="info" sx={{ mb: 2 }}>
                    Selected file: {selectedFile.name} ({(selectedFile.size / 1024).toFixed(2)} KB)
                  </Alert>
                )}

                <Box sx={{ mb: 3 }}>
                  <Typography variant="subtitle2" gutterBottom>
                    CSV Format (Required Columns):
                  </Typography>
                  <pre style={{ backgroundColor: '#f5f5f5', p: 1, borderRadius: 1, overflow: 'auto', maxHeight: 150 }}>
{`destinationAccount,amount,currency,description
987654321001,1000,USD,Rent payment
987654321002,500,USD,Invoice payment`}
                  </pre>
                </Box>

                <Box sx={{ display: 'flex', gap: 2 }}>
                  <Button
                    variant="contained"
                    color="primary"
                    onClick={validateCSV}
                    disabled={!selectedFile || loading}
                    startIcon={loading ? <CircularProgress size={20} /> : undefined}
                  >
                    Validate CSV
                  </Button>
                  <Button
                    variant="outlined"
                    onClick={downloadSampleCSV}
                  >
                    Download Sample
                  </Button>
                </Box>
              </CardContent>
            </Card>
          )}

          {/* Manual Mode */}
          {mode === 'manual' && (
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Manual Payment Entry
                </Typography>

                <TextField
                  fullWidth
                  label="Source Account Number"
                  placeholder="12-digit account number"
                  value={sourceAccount}
                  onChange={(e) => setSourceAccount(e.target.value)}
                  sx={{ mb: 3 }}
                  inputProps={{ pattern: '[0-9]{12}' }}
                />

                <Typography variant="subtitle2" gutterBottom sx={{ mt: 3, mb: 2 }}>
                  Payment Items
                </Typography>

                <Box sx={{ overflowX: 'auto', mb: 2 }}>
                  <table className="manual-entry-table">
                    <thead>
                      <tr>
                        <th>Destination Account</th>
                        <th>Amount</th>
                        <th>Currency</th>
                        <th>Description</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {manualItems.map((item, index) => (
                        <tr key={index}>
                          <td>
                            <TextField
                              size="small"
                              placeholder="12 digits"
                              value={item.destination}
                              onChange={(e) => handleItemChange(index, 'destination', e.target.value)}
                              fullWidth
                            />
                          </td>
                          <td>
                            <TextField
                              size="small"
                              type="number"
                              step="0.01"
                              placeholder="0.00"
                              value={item.amount}
                              onChange={(e) => handleItemChange(index, 'amount', e.target.value)}
                              fullWidth
                            />
                          </td>
                          <td>
                            <TextField
                              size="small"
                              placeholder="USD"
                              value={item.currency}
                              onChange={(e) => handleItemChange(index, 'currency', e.target.value.toUpperCase())}
                              fullWidth
                            />
                          </td>
                          <td>
                            <TextField
                              size="small"
                              placeholder="Optional"
                              value={item.description}
                              onChange={(e) => handleItemChange(index, 'description', e.target.value)}
                              fullWidth
                            />
                          </td>
                          <td>
                            <Button
                              size="small"
                              color="error"
                              onClick={() => handleRemoveRow(index)}
                              disabled={manualItems.length === 1}
                            >
                              <DeleteIcon fontSize="small" />
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </Box>

                <Button
                  variant="outlined"
                  size="small"
                  startIcon={<AddIcon />}
                  onClick={handleAddRow}
                  sx={{ mb: 3 }}
                >
                  Add Row
                </Button>

                <Box sx={{ display: 'flex', gap: 2 }}>
                  <Button
                    variant="contained"
                    color="primary"
                    onClick={submitManualPayments}
                    disabled={loading}
                    startIcon={loading ? <CircularProgress size={20} /> : undefined}
                  >
                    Submit Payments
                  </Button>
                  <Button
                    variant="outlined"
                    onClick={() => {
                      setSourceAccount('');
                      setManualItems([{ destination: '', amount: '', currency: 'USD', description: '' }]);
                    }}
                  >
                    Clear All
                  </Button>
                </Box>
              </CardContent>
            </Card>
          )}
        </Box>
      )}

      {activeStep === 1 && mode === 'upload' && (
        <Box>
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                CSV Validation Result
              </Typography>

              {csvValidationResult && (
                <>
                  <Alert severity={csvValidationResult.isValid ? 'success' : 'warning'} sx={{ mb: 2 }}>
                    Total Records: {csvValidationResult.totalRecords} |
                    Valid: {csvValidationResult.validRecords} |
                    Invalid: {csvValidationResult.invalidRecords}
                  </Alert>

                  {csvValidationResult.errors.length > 0 && (
                    <Box>
                      <Typography variant="subtitle2" color="error" gutterBottom>
                        Errors Found:
                      </Typography>
                      <Box sx={{ maxHeight: 300, overflow: 'auto', mb: 2 }}>
                        {csvValidationResult.errors.map((error, idx) => (
                          <Alert key={idx} severity="error" sx={{ mb: 1 }}>
                            Row {error.rowNumber}: {error.errorMessage}
                          </Alert>
                        ))}
                      </Box>
                    </Box>
                  )}
                </>
              )}

              <Box sx={{ display: 'flex', gap: 2 }}>
                <Button
                  variant="contained"
                  color="primary"
                  onClick={proceedWithCSV}
                  disabled={!csvValidationResult?.isValid || loading}
                  startIcon={loading ? <CircularProgress size={20} /> : undefined}
                >
                  Proceed with Processing
                </Button>
                <Button
                  variant="outlined"
                  onClick={() => {
                    setActiveStep(0);
                    setCsvValidationResult(null);
                  }}
                >
                  Back
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Box>
      )}

      {activeStep === 2 && batchResult && (
        <BulkPaymentResults batch={batchResult} onRetry={handleRetry} />
      )}
    </div>
  );
};

export default BulkPayments;

