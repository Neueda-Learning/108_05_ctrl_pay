import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  CircularProgress,
  Grid,
  Typography,
  Tabs,
  Tab,
  Alert,
  List,
  ListItem,
  ListItemText,
  Divider,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  useTheme,
} from '@mui/material';
import { alpha } from '@mui/material/styles';
import {
  AccountBalance as AccountIcon,
  Security as SecurityIcon,
  TrendingUp as StatsIcon,
  History as HistoryIcon,
  Storage as BulkIcon,
  Person as PersonIcon,
} from '@mui/icons-material';
import { format } from 'date-fns';
import { useCustomer } from '../context/CustomerContext';
import { profileAPI } from '../services/api';

function Profile() {
  const theme = useTheme();
  const custom = theme.customTokens;
  const { currentCustomer } = useCustomer();
  const [activeTab, setActiveTab] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Profile data
  const [profile, setProfile] = useState(null);
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [statistics, setStatistics] = useState(null);
  const [riskInfo, setRiskInfo] = useState(null);
  const [bulkPayments, setBulkPayments] = useState([]);

  // ✓ Helper for loading state and error handling
  const loadProfileData = async () => {
    if (!currentCustomer?.customerId) return;

    setLoading(true);
    setError(null);

    try {
      const [profileRes, accountsRes, transactionsRes, statsRes, riskRes, bulkRes] = await Promise.all([
        profileAPI.getCustomerProfile(currentCustomer.customerId),
        profileAPI.getAccounts(currentCustomer.customerId),
        profileAPI.getTransactions(currentCustomer.customerId, { page: 0, pageSize: 10 }),
        profileAPI.getPaymentStatistics(currentCustomer.customerId),
        profileAPI.getRiskInformation(currentCustomer.customerId),
        profileAPI.getBulkPayments(currentCustomer.customerId, { page: 0, pageSize: 5 }),
      ]);

      setProfile(profileRes.data);
      setAccounts(accountsRes.data || []);
      setTransactions(transactionsRes.data || []);
      setStatistics(statsRes.data);
      setRiskInfo(riskRes.data);
      setBulkPayments(bulkRes.data || []);
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || 'Failed to load profile data';
      setError(errorMsg);
      console.error('Error loading profile:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (currentCustomer?.customerId) {
      loadProfileData();
    }
  }, [currentCustomer?.customerId]);

  if (!currentCustomer?.customerId) {
    return (
      <Box sx={{ p: 4 }}>
        <Alert severity="info">
          Please select or create a customer first to view their profile.
        </Alert>
      </Box>
    );
  }

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '400px' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ p: 4 }}>
        <Alert severity="error">{error}</Alert>
      </Box>
    );
  }

  // =========================================================================
  // SECTION 1: PERSONAL INFORMATION
  // =========================================================================
  const PersonalInfoContent = () => (
    <Card sx={{ mb: 3 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 3, display: 'flex', alignItems: 'center', gap: 1 }}>
          <PersonIcon /> Customer Personal Information
        </Typography>
        <Grid container spacing={3}>
          <Grid item xs={12} sm={6}>
            <Box sx={{ mb: 2 }}>
              <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                Customer ID
              </Typography>
              <Typography variant="body1" sx={{ fontWeight: 600 }}>
                {profile?.customerId}
              </Typography>
            </Box>
            <Box sx={{ mb: 2 }}>
              <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                Full Name
              </Typography>
              <Typography variant="body1" sx={{ fontWeight: 600 }}>
                {profile?.name}
              </Typography>
            </Box>
            <Box sx={{ mb: 2 }}>
              <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                Email
              </Typography>
              <Typography variant="body1" sx={{ fontWeight: 600 }}>
                {profile?.panNumber || 'N/A'}
              </Typography>
            </Box>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Box sx={{ mb: 2 }}>
              <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                Phone Number
              </Typography>
              <Typography variant="body1" sx={{ fontWeight: 600 }}>
                {profile?.phoneNumber}
              </Typography>
            </Box>
            <Box sx={{ mb: 2 }}>
              <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                Country
              </Typography>
              <Typography variant="body1" sx={{ fontWeight: 600 }}>
                {profile?.country}
              </Typography>
            </Box>
            <Box sx={{ mb: 2 }}>
              <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                Account Status
              </Typography>
              <Chip
                label={profile?.status}
                size="small"
                color={profile?.status === 'ACTIVE' ? 'success' : 'default'}
                variant="outlined"
                sx={{ mt: 0.5 }}
              />
            </Box>
          </Grid>
          <Grid item xs={12}>
            <Divider />
          </Grid>
          <Grid item xs={12} sm={6}>
            <Box>
              <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                Date Joined
              </Typography>
              <Typography variant="body2">
                {profile?.profileCreated
                  ? format(new Date(profile.profileCreated), 'dd-MMM-yyyy HH:mm')
                  : 'N/A'}
              </Typography>
            </Box>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Box>
              <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                Last Updated
              </Typography>
              <Typography variant="body2">
                {profile?.lastUpdated
                  ? format(new Date(profile.lastUpdated), 'dd-MMM-yyyy HH:mm')
                  : 'N/A'}
              </Typography>
            </Box>
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );

  // =========================================================================
  // SECTION 2: ACCOUNT SUMMARY
  // =========================================================================
  const AccountSummaryContent = () => (
    <Box>
      <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
        <AccountIcon /> My Accounts
      </Typography>
      {accounts.length === 0 ? (
        <Alert severity="info">No accounts found for this customer.</Alert>
      ) : (
        <Grid container spacing={2}>
          {accounts.map((account) => (
            <Grid item xs={12} key={account.accountId}>
              <Card sx={{ background: alpha(custom.brand.main, 0.05) }}>
                <CardContent>
                  <Grid container spacing={2}>
                    <Grid item xs={12} sm={6} md={3}>
                      <Box>
                        <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                          Account Number
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 600, mt: 0.5 }}>
                          {account.accountNumber}
                        </Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={12} sm={6} md={3}>
                      <Box>
                        <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                          Account Type
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 600, mt: 0.5 }}>
                          {account.accountType || 'SAVINGS'}
                        </Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={12} sm={6} md={3}>
                      <Box>
                        <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                          Currency
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 600, mt: 0.5 }}>
                          {account.currency}
                        </Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={12} sm={6} md={3}>
                      <Box>
                        <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                          Status
                        </Typography>
                        <Chip
                          label={account.status}
                          size="small"
                          color={account.status === 'ACTIVE' ? 'success' : 'default'}
                          variant="outlined"
                          sx={{ mt: 0.5 }}
                        />
                      </Box>
                    </Grid>
                    <Grid item xs={12}>
                      <Divider />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <Box>
                        <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                          Current Balance
                        </Typography>
                        <Typography variant="h6" sx={{ fontWeight: 600, mt: 0.5 }}>
                          {account.balance} {account.currency}
                        </Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <Box>
                        <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                          Available Balance
                        </Typography>
                        <Typography variant="h6" sx={{ fontWeight: 600, mt: 0.5 }}>
                          {account.availableBalance} {account.currency}
                        </Typography>
                      </Box>
                    </Grid>
                  </Grid>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}
    </Box>
  );

  // =========================================================================
  // SECTION 3: TRANSACTION HISTORY
  // =========================================================================
  const TransactionHistoryContent = () => (
    <Box>
      <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
        <HistoryIcon /> Transaction History
      </Typography>
      {transactions.length === 0 ? (
        <Alert severity="info">No transactions found.</Alert>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow sx={{ background: custom.gridHeader }}>
                <TableCell>Transaction ID</TableCell>
                <TableCell>From Account</TableCell>
                <TableCell>To Account</TableCell>
                <TableCell align="right">Amount</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Date</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {transactions.map((tx) => (
                <TableRow key={tx.transactionId}>
                  <TableCell>{tx.transactionId}</TableCell>
                  <TableCell>{tx.sourceAccount}</TableCell>
                  <TableCell>{tx.destinationAccount}</TableCell>
                  <TableCell align="right">
                    {tx.amount} {tx.currency}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={tx.status}
                      size="small"
                      color={
                        tx.status === 'COMPLETED'
                          ? 'success'
                          : tx.status === 'FAILED'
                          ? 'error'
                          : 'default'
                      }
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell>{format(new Date(tx.transactionDate), 'dd-MMM-yyyy HH:mm')}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );

  // =========================================================================
  // SECTION 4: PAYMENT STATISTICS
  // =========================================================================
  const PaymentStatisticsContent = () => (
    <Box>
      <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
        <StatsIcon /> Payment Statistics
      </Typography>
      {statistics ? (
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6} md={4}>
            <Card sx={{ background: alpha(custom.brand.main, 0.08) }}>
              <CardContent sx={{ textAlign: 'center' }}>
                <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                  Total Payments
                </Typography>
                <Typography variant="h4" sx={{ fontWeight: 600, mt: 1 }}>
                  {statistics.totalPayments}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={4}>
            <Card sx={{ background: alpha('success.main', 0.08) }}>
              <CardContent sx={{ textAlign: 'center' }}>
                <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                  Successful Payments
                </Typography>
                <Typography variant="h4" sx={{ fontWeight: 600, mt: 1, color: 'success.main' }}>
                  {statistics.successfulPayments}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={4}>
            <Card sx={{ background: alpha('error.main', 0.08) }}>
              <CardContent sx={{ textAlign: 'center' }}>
                <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                  Failed Payments
                </Typography>
                <Typography variant="h4" sx={{ fontWeight: 600, mt: 1, color: 'error.main' }}>
                  {statistics.failedPayments}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={4}>
            <Card sx={{ background: alpha('warning.main', 0.08) }}>
              <CardContent sx={{ textAlign: 'center' }}>
                <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                  Rejected Payments
                </Typography>
                <Typography variant="h4" sx={{ fontWeight: 600, mt: 1, color: 'warning.main' }}>
                  {statistics.rejectedPayments}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={4}>
            <Card sx={{ background: alpha(custom.brand.main, 0.08) }}>
              <CardContent sx={{ textAlign: 'center' }}>
                <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                  Total Amount
                </Typography>
                <Typography variant="h6" sx={{ fontWeight: 600, mt: 1 }}>
                  {statistics.totalAmount}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={4}>
            <Card sx={{ background: alpha(custom.brand.main, 0.08) }}>
              <CardContent sx={{ textAlign: 'center' }}>
                <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                  Average Transaction
                </Typography>
                <Typography variant="h6" sx={{ fontWeight: 600, mt: 1 }}>
                  {statistics.averageTransactionAmount}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      ) : (
        <Alert severity="info">No statistics available.</Alert>
      )}
    </Box>
  );

  // =========================================================================
  // SECTION 5: SECURITY & RISK INFORMATION
  // =========================================================================
  const SecurityRiskContent = () => (
    <Box>
      <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
        <SecurityIcon /> Security Overview
      </Typography>
      {riskInfo ? (
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ background: alpha(custom.brand.main, 0.08) }}>
              <CardContent sx={{ textAlign: 'center' }}>
                <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                  Account Risk Level
                </Typography>
                <Chip
                  label={riskInfo.riskLevel}
                  size="medium"
                  color={
                    riskInfo.riskLevel === 'LOW'
                      ? 'success'
                      : riskInfo.riskLevel === 'MEDIUM'
                      ? 'warning'
                      : 'error'
                  }
                  variant="outlined"
                  sx={{ mt: 1 }}
                />
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ background: alpha('warning.main', 0.08) }}>
              <CardContent sx={{ textAlign: 'center' }}>
                <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                  Fraud Flags
                </Typography>
                <Typography variant="h4" sx={{ fontWeight: 600, mt: 1, color: 'warning.main' }}>
                  {riskInfo.fraudFlags}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ background: alpha('error.main', 0.08) }}>
              <CardContent sx={{ textAlign: 'center' }}>
                <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                  Rejected Payments
                </Typography>
                <Typography variant="h4" sx={{ fontWeight: 600, mt: 1, color: 'error.main' }}>
                  {riskInfo.rejectedTransactions}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ background: alpha(custom.brand.main, 0.08) }}>
              <CardContent>
                <Typography variant="caption" sx={{ color: custom.text.secondary }}>
                  Last Suspicious Activity
                </Typography>
                <Typography variant="body2" sx={{ mt: 1 }}>
                  {riskInfo.lastSuspiciousActivity
                    ? format(new Date(riskInfo.lastSuspiciousActivity), 'dd-MMM-yyyy')
                    : 'None'}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      ) : (
        <Alert severity="info">No risk information available.</Alert>
      )}
    </Box>
  );

  // =========================================================================
  // SECTION 6: BULK PAYMENT HISTORY
  // =========================================================================
  const BulkPaymentContent = () => (
    <Box>
      <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
        <BulkIcon /> Bulk Payments
      </Typography>
      {bulkPayments.length === 0 ? (
        <Alert severity="info">No bulk payment batches found.</Alert>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow sx={{ background: custom.gridHeader }}>
                <TableCell>Batch ID</TableCell>
                <TableCell>Reference</TableCell>
                <TableCell align="center">Total</TableCell>
                <TableCell align="center">Successful</TableCell>
                <TableCell align="center">Failed</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Created</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {bulkPayments.map((batch) => (
                <TableRow key={batch.batchId}>
                  <TableCell>{batch.batchId}</TableCell>
                  <TableCell>{batch.batchReference}</TableCell>
                  <TableCell align="center">{batch.totalTransactions}</TableCell>
                  <TableCell align="center">
                    <Chip
                      label={batch.successfulTransactions}
                      size="small"
                      color="success"
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell align="center">
                    <Chip
                      label={batch.failedTransactions}
                      size="small"
                      color="error"
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={batch.status}
                      size="small"
                      color={batch.status === 'COMPLETED' ? 'success' : 'default'}
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell>{format(new Date(batch.createdAt), 'dd-MMM-yyyy HH:mm')}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" sx={{ mb: 3, fontWeight: 600 }}>
        Customer Profile
      </Typography>

      <Card>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={activeTab} onChange={(e, newVal) => setActiveTab(newVal)}>
            <Tab label="Personal Info" />
            <Tab label="Accounts" />
            <Tab label="Transactions" />
            <Tab label="Statistics" />
            <Tab label="Security" />
            <Tab label="Bulk Payments" />
          </Tabs>
        </Box>

        <CardContent sx={{ pt: 3 }}>
          {activeTab === 0 && <PersonalInfoContent />}
          {activeTab === 1 && <AccountSummaryContent />}
          {activeTab === 2 && <TransactionHistoryContent />}
          {activeTab === 3 && <PaymentStatisticsContent />}
          {activeTab === 4 && <SecurityRiskContent />}
          {activeTab === 5 && <BulkPaymentContent />}
        </CardContent>
      </Card>
    </Box>
  );
}

export default Profile;

