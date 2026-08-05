import React, { useState, useRef } from 'react';
import { Box, Button, Card, CardContent, Stepper, Step, StepLabel, Tab, Tabs, Alert, CircularProgress, Typography, TextField, useTheme, FormControl, InputLabel, Select, MenuItem, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Dialog, DialogTitle, DialogContent, DialogActions } from '@mui/material';
import { alpha } from '@mui/material/styles';
import { toast } from 'react-toastify';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import api from '../services/api';
import { useCustomer } from '../context/CustomerContext';
import BulkPaymentResults from '../components/BulkPaymentResults';
import './BulkPayments.css';

const BulkPayments = () => {
  const theme = useTheme();
  const custom = theme.customTokens;
  const { customerAccounts, customerId } = useCustomer();
  const [mode, setMode] = useState('upload'); // 'upload' or 'manual'
  const [activeStep, setActiveStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [csvValidationResult, setCsvValidationResult] = useState(null);
  const [batchResult, setBatchResult] = useState(null);
  const [selectedSourceAccount, setSelectedSourceAccount] = useState(''); // From dropdown

  // PIN Authentication
  const [pinDialogOpen, setPinDialogOpen] = useState(false);
  const [pinInput, setPinInput] = useState('');
  const [pendingSubmit, setPendingSubmit] = useState(null); // 'csv' or 'manual'

  // Upload mode state
  const [selectedFile, setSelectedFile] = useState(null);
  const fileInputRef = useRef(null);

   // Manual mode state - simplified to only destination and amount
   const [manualItems, setManualItems] = useState([{ destination: '', amount: '' }]);

   // Get source account details for currency
   const getSourceAccountCurrency = () => {
     if (!selectedSourceAccount || !customerAccounts) return 'USD';
     const account = customerAccounts.find(a => a.accountNumber === selectedSourceAccount);
     return account?.currency || 'USD';
   };

  // Sample CSV reference
  const sampleCSVContent = `destinationAccount,amount
987654321001,1000
987654321002,500
987654321003,2000`;

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

    // Check if source account is selected
    if (!selectedSourceAccount) {
      toast.error('Please select a source account above');
      return;
    }

    // Ask for PIN
    setPendingSubmit('csv');
    setPinDialogOpen(true);
  };

  // ================== MANUAL MODE ==================

  const handlePinSubmit = async () => {
    if (!pinInput) {
      toast.error('Please enter your PIN');
      return;
    }

    setPinDialogOpen(false);
    setLoading(true);

    try {
       if (pendingSubmit === 'csv') {
         // Process CSV submission
         const text = await selectedFile.text();
         const lines = text.split('\n').slice(1); // Skip header
         const items = lines
           .filter(line => line.trim())
           .map(line => {
             const [dest, amount] = line.split(',').map(v => v.trim());
             return {
               destinationAccount: dest,
               amount: parseFloat(amount)
               // Currency will be set to source account currency by backend
             };
           });

         const response = await api.post('/bulk-payments', {
           sourceAccount: selectedSourceAccount,
           pin: pinInput,
           items,
           idempotencyKey: `upload-${Date.now()}`
         });

        setBatchResult(response.data);
        toast.success(`Bulk payment batch created: ${response.data.batchReference}`);
        setActiveStep(2);
      } else if (pendingSubmit === 'manual') {
        // Process manual submission
        const items = manualItems.map(item => ({
          destinationAccount: item.destination,
          amount: parseFloat(item.amount)
        }));

        const response = await api.post('/bulk-payments', {
          sourceAccount: selectedSourceAccount,
          pin: pinInput,
          items,
          idempotencyKey: `manual-${Date.now()}`
        });

        setBatchResult(response.data);
        toast.success(`Bulk payment batch created: ${response.data.batchReference}`);
        setActiveStep(2);
      }
    } catch (error) {
      toast.error('Error creating batch: ' + (error.response?.data?.message || error.message));
      console.error('Batch creation error:', error);
    } finally {
      setLoading(false);
      setPinInput('');
      setPendingSubmit(null);
    }
  };

  const handleAddRow = () => {
    setManualItems([...manualItems, { destination: '', amount: '' }]);
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
    if (!selectedSourceAccount) {
      toast.error('Please select a source account from the dropdown above');
      return false;
    }
    if (manualItems.length === 0) {
      toast.error('Please add at least one payment item');
      return false;
    }

    for (let i = 0; i < manualItems.length; i++) {
      const item = manualItems[i];
      if (!item.destination || !item.amount) {
        toast.error(`Row ${i + 1}: Please fill in all required fields (destination account and amount)`);
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
    }

    if (selectedSourceAccount === manualItems[0].destination) {
      toast.error('Source and destination accounts cannot be the same');
      return false;
    }

    return true;
  };

  const submitManualPayments = async () => {
    if (!validateManualItems()) return;

    // Ask for PIN before processing
    setPendingSubmit('manual');
    setPinDialogOpen(true);
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

      {/* Source Account Selector - Same for both modes */}
      <Card sx={{ mb: 4, border: `2px solid ${alpha(custom.brand.main, 0.3)}` }}>
        <CardContent>
          {!customerId ? (
            <Alert severity="warning" sx={{ mb: 2 }}>
              Please select a customer first to view available accounts. Go to <strong>Create Customer</strong> in the sidebar.
            </Alert>
          ) : null}

          <FormControl fullWidth sx={{ minWidth: 300 }} disabled={!customerId}>
            <InputLabel id="source-account-label">Select Source Account</InputLabel>
            <Select
              labelId="source-account-label"
              id="source-account-select"
              value={selectedSourceAccount}
              label="Select Source Account"
              onChange={(e) => setSelectedSourceAccount(e.target.value)}
              sx={{
                '& .MuiOutlinedInput-root': {
                  '&:hover fieldset': {
                    borderColor: custom.brand.main,
                  },
                  '&.Mui-focused fieldset': {
                    borderColor: custom.brand.main,
                  },
                },
              }}
            >
              <MenuItem value="">
                <em>-- Choose an account --</em>
              </MenuItem>
              {customerId && customerAccounts && Array.isArray(customerAccounts) && customerAccounts.length > 0 ? (
                customerAccounts.map((account) => (
                  <MenuItem key={account.accountNumber} value={account.accountNumber}>
                    {account.accountNumber} - {account.accountName || 'No Name'} ({account.currency})
                  </MenuItem>
                ))
              ) : customerId ? (
                <MenuItem disabled>
                  No accounts found for this customer
                </MenuItem>
              ) : (
                <MenuItem disabled>
                  Select a customer to view accounts
                </MenuItem>
              )}
            </Select>
            {selectedSourceAccount && (
              <Box sx={{ mt: 2, p: 1.5, backgroundColor: alpha(custom.brand.main, 0.08), borderRadius: 1, borderLeft: `3px solid ${custom.brand.main}` }}>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  Selected: <strong>{selectedSourceAccount}</strong>
                </Typography>
              </Box>
            )}
          </FormControl>
        </CardContent>
      </Card>

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
          {!selectedSourceAccount && (
            <Alert severity="info" sx={{ mb: 3 }}>
              <strong>Select a source account above to proceed with bulk payments.</strong>
            </Alert>
          )}

          {/* Mode Selection */}
          <Card sx={{ mb: 3, opacity: selectedSourceAccount ? 1 : 0.6 }}>
            <CardContent>
              <Tabs
                value={mode === 'upload' ? 0 : 1}
                onChange={(e, newValue) => {
                  if (selectedSourceAccount) {
                    setMode(newValue === 0 ? 'upload' : 'manual');
                    setActiveStep(0);
                    setCsvValidationResult(null);
                  }
                }}
                disabled={!selectedSourceAccount}
              >
                <Tab label="CSV Upload" disabled={!selectedSourceAccount} />
                <Tab label="Manual Entry" disabled={!selectedSourceAccount} />
              </Tabs>
            </CardContent>
          </Card>

          {selectedSourceAccount && (
            <>
          {/* Upload Mode */}
          {mode === 'upload' && (
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Upload CSV File
                </Typography>

                <Box sx={{
                  mb: 3,
                  p: 2,
                  border: `2px dashed ${custom.brand.main}`,
                  borderRadius: 2,
                  textAlign: 'center',
                  cursor: 'pointer',
                  backgroundColor: alpha(custom.brand.main, 0.05),
                  transition: 'all 0.3s ease',
                  '&:hover': {
                    backgroundColor: alpha(custom.brand.main, 0.1),
                    borderColor: custom.brand.light,
                  }
                }}
                  onClick={() => fileInputRef.current?.click()}
                >
                  <CloudUploadIcon sx={{ fontSize: 48, color: custom.brand.main, mb: 1 }} />
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
                  <Box sx={{
                    backgroundColor: alpha(custom.gridHeader, 0.7),
                    border: `1px solid ${custom.border}`,
                    borderRadius: 1,
                    overflow: 'auto',
                    maxHeight: 150,
                    p: 2,
                    fontFamily: 'monospace',
                    fontSize: '0.85rem',
                    color: custom.text.primary
                   }}>
{`destinationAccount,amount
987654321001,1000
987654321002,500`}
                   </Box>
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

                 <Alert severity="info" sx={{ mb: 3 }}>
                   Source account has been selected from the dropdown above. Add payment destinations in the table below.
                 </Alert>

                  <Typography variant="subtitle2" gutterBottom sx={{ mb: 2 }}>
                    Payment Items
                  </Typography>

                 <TableContainer component={Paper} sx={{
                   mb: 2,
                   backgroundColor: custom.gridRow,
                   border: `1px solid ${alpha(custom.border, 0.5)}`
                 }}>
                   <Table>
                      <TableHead>
                        <TableRow sx={{ backgroundColor: custom.gridHeader }}>
                          <TableCell sx={{ fontWeight: 700, color: custom.text.secondary }}>Destination Account</TableCell>
                          <TableCell sx={{ fontWeight: 700, color: custom.text.secondary }}>Amount</TableCell>
                          <TableCell align="center" sx={{ fontWeight: 700, color: custom.text.secondary }}>Action</TableCell>
                        </TableRow>
                      </TableHead>
                     <TableBody>
                       {manualItems.map((item, index) => (
                         <TableRow
                           key={index}
                           sx={{
                             '&:hover': {
                               backgroundColor: alpha(custom.brand.main, 0.05)
                             },
                             borderBottom: `1px solid ${alpha(custom.border, 0.3)}`
                           }}
                         >
                           <TableCell>
                             <TextField
                               size="small"
                               placeholder="12 digits"
                               value={item.destination}
                               onChange={(e) => handleItemChange(index, 'destination', e.target.value)}
                               fullWidth
                               sx={{
                                 '& .MuiOutlinedInput-root': {
                                   '&:hover fieldset': {
                                     borderColor: custom.brand.main,
                                   },
                                   '&.Mui-focused fieldset': {
                                     borderColor: custom.brand.main,
                                   },
                                 },
                               }}
                             />
                           </TableCell>
                           <TableCell>
                             <TextField
                               size="small"
                               type="number"
                               step="0.01"
                               placeholder="0.00"
                               value={item.amount}
                               onChange={(e) => handleItemChange(index, 'amount', e.target.value)}
                               fullWidth
                               sx={{
                                 '& .MuiOutlinedInput-root': {
                                   '&:hover fieldset': {
                                     borderColor: custom.brand.main,
                                   },
                                   '&.Mui-focused fieldset': {
                                     borderColor: custom.brand.main,
                                   },
                                 },
                               }}
                             />
                            </TableCell>
                            <TableCell align="center">
                              <Button
                                size="small"
                                color="error"
                                onClick={() => handleRemoveRow(index)}
                                disabled={manualItems.length === 1}
                                sx={{
                                  '&:hover': {
                                    backgroundColor: alpha('#f44336', 0.1),
                                  }
                                }}
                              >
                                <DeleteIcon fontSize="small" />
                              </Button>
                            </TableCell>
                         </TableRow>
                       ))}
                     </TableBody>
                   </Table>
                 </TableContainer>

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
                       setManualItems([{ destination: '', amount: '' }]);
                     }}
                   >
                     Clear All
                   </Button>
                 </Box>
              </CardContent>
            </Card>
          )}
            </>
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

      {/* PIN Verification Dialog */}
      <Dialog
        open={pinDialogOpen}
        onClose={() => {
          if (!loading) {
            setPinDialogOpen(false);
            setPinInput('');
          }
        }}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Enter Account PIN</DialogTitle>
        <DialogContent sx={{ pt: 2 }}>
          <Typography variant="body2" sx={{ mb: 2 }}>
            Please enter your 4-6 digit PIN to authorize this bulk payment transaction
          </Typography>
          <TextField
            autoFocus
            fullWidth
            label="PIN"
            type="password"
            value={pinInput}
            onChange={(e) => setPinInput(e.target.value)}
            placeholder="Enter 4-6 digits"
            inputProps={{ maxLength: 6, pattern: '[0-9]*' }}
            disabled={loading}
            onKeyPress={(e) => {
              if (e.key === 'Enter' && !loading) {
                handlePinSubmit();
              }
            }}
          />
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button
            onClick={() => {
              setPinDialogOpen(false);
              setPinInput('');
            }}
            disabled={loading}
          >
            Cancel
          </Button>
          <Button
            onClick={handlePinSubmit}
            variant="contained"
            disabled={loading || !pinInput}
            startIcon={loading ? <CircularProgress size={20} /> : undefined}
          >
            {loading ? 'Processing...' : 'Authorize'}
          </Button>
        </DialogActions>
      </Dialog>
    </div>
  );
};

export default BulkPayments;

