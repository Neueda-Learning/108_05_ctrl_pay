import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert, Box, Button, Card, CardContent, Chip, CircularProgress,
  Dialog, DialogActions, DialogContent, DialogTitle, Divider,
  Grid, Table, TableBody, TableCell, TableContainer, TableHead,
  TableRow, TextField, Typography,
} from '@mui/material';
import { format } from 'date-fns';
import { adminFraudAPI } from '../services/api';

// ── helpers ─────────────────────────────────────────────────────────────────
const riskColor = (level) => {
  switch ((level || '').toUpperCase()) {
    case 'CRITICAL': return '#EF4444';
    case 'HIGH':     return '#F59E0B';
    case 'MEDIUM':   return '#3B82F6';
    default:         return '#10B981';
  }
};

const fmt = (dt) => dt ? format(new Date(dt), 'MMM d, yyyy HH:mm') : '—';
const fmtAmount = (a, c) => a != null ? `${Number(a).toLocaleString()} ${c || ''}` : '—';
const fmtScore = (s) => s != null ? `${Number(s).toFixed(1)}%` : '—';

// ── sub-component: stat card ─────────────────────────────────────────────────
const StatCard = ({ label, value, color }) => (
  <Card sx={{ flex: 1, minWidth: 140 }}>
    <CardContent sx={{ textAlign: 'center', py: 2 }}>
      <Typography variant="h4" sx={{ color, fontWeight: 700 }}>{value}</Typography>
      <Typography variant="body2" sx={{ color: '#94A3B8', mt: 0.5 }}>{label}</Typography>
    </CardContent>
  </Card>
);

// ── sub-component: detail dialog ──────────────────────────────────────────────
const FraudDetailDialog = ({ open, paymentId, onClose, onAction }) => {
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(false);
  const [notes, setNotes] = useState('');
  const [reviewer, setReviewer] = useState('admin');
  const [acting, setActing] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!open || !paymentId) return;
    setLoading(true);
    setError('');
    adminFraudAPI.getPaymentFraudDetail(paymentId)
      .then(r => setDetail(r.data))
      .catch(e => setError(e.response?.data?.error || 'Failed to load details'))
      .finally(() => setLoading(false));
  }, [open, paymentId]);

  const handleApprove = async () => {
    setActing(true);
    try {
      await adminFraudAPI.approvePayment(paymentId, { reviewedBy: reviewer, notes });
      onAction('approved', paymentId);
      onClose();
    } catch (e) {
      setError(e.response?.data?.error || 'Approval failed');
    } finally { setActing(false); }
  };

  const handleReject = async () => {
    if (!notes.trim()) { setError('Please provide a rejection reason'); return; }
    setActing(true);
    try {
      await adminFraudAPI.rejectPayment(paymentId, { reviewedBy: reviewer, notes });
      onAction('rejected', paymentId);
      onClose();
    } catch (e) {
      setError(e.response?.data?.error || 'Rejection failed');
    } finally { setActing(false); }
  };

  const p = detail?.payment;
  const a = detail?.fraudAssessment;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ color: '#E2E8F0', borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
        🔍 Fraud Investigation — Payment #{paymentId}
      </DialogTitle>
      <DialogContent sx={{ mt: 2 }}>
        {loading && <Box display="flex" justifyContent="center"><CircularProgress /></Box>}
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {detail && (
          <Grid container spacing={2}>
            {/* Payment info */}
            <Grid item xs={12} md={6}>
              <Typography variant="subtitle2" sx={{ color: '#94A3B8', mb: 1 }}>Payment Details</Typography>
              <Box sx={{ background: 'rgba(255,255,255,0.04)', borderRadius: 2, p: 2 }}>
                <Typography variant="body2">Amount: <strong>{fmtAmount(p?.amount, p?.currency)}</strong></Typography>
                <Typography variant="body2">From: <code>{p?.sourceAccount}</code></Typography>
                <Typography variant="body2">To: <code>{p?.destinationAccount}</code></Typography>
                <Typography variant="body2">Status: <Chip label={p?.status} size="small" sx={{ ml: 1, fontSize: '0.7rem' }} /></Typography>
                <Typography variant="body2">Created: {fmt(p?.createdAt)}</Typography>
              </Box>
            </Grid>
            {/* Fraud assessment */}
            <Grid item xs={12} md={6}>
              <Typography variant="subtitle2" sx={{ color: '#94A3B8', mb: 1 }}>Fraud Analysis</Typography>
              <Box sx={{ background: 'rgba(255,255,255,0.04)', borderRadius: 2, p: 2 }}>
                {a ? (
                  <>
                    <Typography variant="body2">Hybrid Score: <strong style={{ color: riskColor(a.riskLevel) }}>{fmtScore(a.hybridFraudScore)}</strong></Typography>
                    <Typography variant="body2">Rule Score: {fmtScore(a.ruleEngineScore)}</Typography>
                    <Typography variant="body2">ML Score: {fmtScore(a.mlFraudProbability)}</Typography>
                    <Typography variant="body2">Risk Level: <strong style={{ color: riskColor(a.riskLevel) }}>{a.riskLevel}</strong></Typography>
                    <Typography variant="body2" sx={{ mt: 1, fontSize: '0.78rem', color: '#94A3B8' }}>{a.explanation}</Typography>
                  </>
                ) : <Typography variant="body2" sx={{ color: '#64748B' }}>No assessment available</Typography>}
              </Box>
            </Grid>
            {/* Account info */}
            {detail.sourceAccount && (
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" sx={{ color: '#94A3B8', mb: 1 }}>Source Account</Typography>
                <Box sx={{ background: 'rgba(255,255,255,0.04)', borderRadius: 2, p: 2 }}>
                  <Typography variant="body2">{detail.sourceAccount.accountName}</Typography>
                  <Typography variant="body2">{detail.sourceAccount.bankName}</Typography>
                  <Typography variant="body2">Balance: {fmtAmount(detail.sourceAccount.accountBalance, detail.sourceAccount.currency)}</Typography>
                  <Typography variant="body2">Status: <Chip label={detail.sourceAccount.accountStatus} size="small" sx={{ ml: 1, fontSize: '0.7rem' }} /></Typography>
                </Box>
              </Grid>
            )}
            {/* Reviewer fields */}
            <Grid item xs={12}>
              <Divider sx={{ my: 1, borderColor: 'rgba(255,255,255,0.08)' }} />
              <Typography variant="subtitle2" sx={{ color: '#94A3B8', mb: 1 }}>Reviewer Decision</Typography>
              <TextField
                label="Reviewer Name"
                value={reviewer}
                onChange={e => setReviewer(e.target.value)}
                size="small"
                sx={{ mr: 2, width: 200 }}
              />
              <TextField
                label="Notes / Reason"
                value={notes}
                onChange={e => setNotes(e.target.value)}
                multiline
                rows={2}
                fullWidth
                sx={{ mt: 1 }}
                placeholder="Required for rejection. Optional for approval."
              />
            </Grid>
          </Grid>
        )}
      </DialogContent>
      <DialogActions sx={{ p: 2, borderTop: '1px solid rgba(255,255,255,0.08)' }}>
        <Button onClick={onClose} disabled={acting}>Cancel</Button>
        <Button
          variant="outlined"
          color="error"
          onClick={handleReject}
          disabled={acting || !detail}
          sx={{ mr: 1 }}
        >
          {acting ? <CircularProgress size={16} /> : '🚫 Reject'}
        </Button>
        <Button
          variant="contained"
          color="success"
          onClick={handleApprove}
          disabled={acting || !detail}
          sx={{ background: 'linear-gradient(135deg,#10B981,#059669)' }}
        >
          {acting ? <CircularProgress size={16} /> : '✅ Approve'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

// ── main page ─────────────────────────────────────────────────────────────────
export default function ReviewHaltedPayments() {
  const [stats, setStats] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedPaymentId, setSelectedPaymentId] = useState(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [actionMsg, setActionMsg] = useState('');

  const loadData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [statsRes, txRes] = await Promise.all([
        adminFraudAPI.getStats(),
        adminFraudAPI.getSuspiciousTransactions({ limit: 50 }),
      ]);
      setStats(statsRes.data);
      setTransactions(txRes.data.transactions || []);
    } catch (e) {
      setError(e.response?.data?.error || 'Failed to load fraud data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  const openDetail = (paymentId) => {
    setSelectedPaymentId(paymentId);
    setDialogOpen(true);
  };

  const handleAction = (action, paymentId) => {
    setActionMsg(`Payment #${paymentId} has been ${action}.`);
    loadData();
    setTimeout(() => setActionMsg(''), 5000);
  };

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box>
          <Typography variant="h4" sx={{ color: '#E2E8F0', fontWeight: 700 }}>⏸️ Review Halted Payments</Typography>
          <Typography variant="body2" sx={{ color: '#64748B', mt: 0.5 }}>
            Review and manage suspicious payments flagged by the fraud detection engine
          </Typography>
        </Box>
        <Button variant="outlined" onClick={loadData} disabled={loading}>
          🔄 Refresh
        </Button>
      </Box>

      {/* Action feedback */}
      {actionMsg && <Alert severity="success" sx={{ mb: 2 }}>{actionMsg}</Alert>}
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {/* Stats row */}
      {stats && (
        <Box display="flex" gap={2} mb={3} flexWrap="wrap">
          <StatCard label="Pending Review" value={stats.pendingReview ?? 0} color="#F59E0B" />
          <StatCard label="Awaiting Review" value={stats.paymentsAwaitingReview ?? 0} color="#F59E0B" />
          <StatCard label="Approved" value={stats.totalApproved ?? 0} color="#10B981" />
          <StatCard label="Rejected" value={stats.totalRejected ?? 0} color="#EF4444" />
          <StatCard label="Total Assessed" value={stats.totalAssessed ?? 0} color="#6366F1" />
          <StatCard label="Fraud Rate" value={stats.fraudRate ?? '0%'} color="#EF4444" />
        </Box>
      )}

      {/* Transactions table */}
      <Card>
        <CardContent sx={{ p: 0 }}>
          <Box sx={{ p: 2, borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
            <Typography variant="h6" sx={{ color: '#E2E8F0' }}>
              ⚠️ Suspicious Transactions ({transactions.length})
            </Typography>
          </Box>

          {loading ? (
            <Box display="flex" justifyContent="center" p={4}>
              <CircularProgress />
            </Box>
          ) : transactions.length === 0 ? (
            <Box textAlign="center" p={4}>
              <Typography sx={{ color: '#64748B' }}>✅ No suspicious transactions pending review</Typography>
            </Box>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Payment ID</TableCell>
                    <TableCell>Amount</TableCell>
                    <TableCell>Source → Destination</TableCell>
                    <TableCell>Fraud Score</TableCell>
                    <TableCell>Risk Level</TableCell>
                    <TableCell>Triggered Rules</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Created</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {transactions.map((tx) => {
                    const triggered = (() => {
                      try { return JSON.parse(tx.triggeredRules || '[]'); } catch { return []; }
                    })();
                    return (
                      <TableRow key={tx.paymentId}>
                        <TableCell>
                          <Typography variant="body2" sx={{ color: '#6366F1', fontWeight: 600 }}>
                            #{tx.paymentId}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontWeight: 600 }}>
                            {fmtAmount(tx.amount, tx.currency)}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="caption" sx={{ color: '#94A3B8', display: 'block' }}>
                            {tx.sourceAccount}
                          </Typography>
                          <Typography variant="caption" sx={{ color: '#64748B' }}>
                            → {tx.destinationAccount}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" sx={{ color: riskColor(tx.riskLevel), fontWeight: 700 }}>
                            {fmtScore(tx.fraudScore)}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={tx.riskLevel || 'UNKNOWN'}
                            size="small"
                            sx={{
                              background: riskColor(tx.riskLevel) + '20',
                              color: riskColor(tx.riskLevel),
                              fontWeight: 700,
                              fontSize: '0.7rem',
                            }}
                          />
                        </TableCell>
                        <TableCell>
                          <Box display="flex" flexWrap="wrap" gap={0.5}>
                            {triggered.slice(0, 2).map(r => (
                              <Chip key={r} label={r} size="small" sx={{ fontSize: '0.65rem', background: 'rgba(239,68,68,0.15)', color: '#FCA5A5' }} />
                            ))}
                            {triggered.length > 2 && (
                              <Chip label={`+${triggered.length - 2}`} size="small" sx={{ fontSize: '0.65rem' }} />
                            )}
                          </Box>
                        </TableCell>
                        <TableCell>
                          <Chip label={tx.paymentStatus} size="small"
                            sx={{ background: '#F59E0B20', color: '#F59E0B', fontSize: '0.7rem' }} />
                        </TableCell>
                        <TableCell>
                          <Typography variant="caption" sx={{ color: '#64748B' }}>
                            {fmt(tx.createdAt)}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Button
                            size="small"
                            variant="outlined"
                            onClick={() => openDetail(tx.paymentId)}
                            sx={{ fontSize: '0.75rem' }}
                          >
                            Investigate
                          </Button>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Detail Dialog */}
      <FraudDetailDialog
        open={dialogOpen}
        paymentId={selectedPaymentId}
        onClose={() => setDialogOpen(false)}
        onAction={handleAction}
      />
    </Box>
  );
}

