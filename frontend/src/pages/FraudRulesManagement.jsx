import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Grid,
  Typography,
  Button,
  CircularProgress,
  Chip,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Switch,
  FormControlLabel,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  MenuItem,
} from '@mui/material';
import {
  Add,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Power as PowerIcon,
} from '@mui/icons-material';
import { fraudRulesAPI } from '../services/api';
import { toast } from 'react-toastify';
import StatusBadge from '../components/StatusBadge';

const RULE_TYPES = [
  { id: 'THRESHOLD', name: 'Threshold', icon: '📊' },
  { id: 'PATTERN', name: 'Pattern', icon: '🔵' },
  { id: 'BEHAVIOR', name: 'Behavior', icon: '👤' },
  { id: 'VELOCITY', name: 'Velocity', icon: '⚡' },
  { id: 'ANOMALY', name: 'Anomaly', icon: '⚠️' },
];

function FraudRulesManagement() {
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingRule, setEditingRule] = useState(null);
  const [formData, setFormData] = useState({
    ruleName: '',
    ruleType: '',
    description: '',
    severity: 'MEDIUM',
    orderOfExecution: 100,
    weight: 1.0,
    ruleDefinitionJson: '',
    triggeringConditionsJson: '',
  });

  useEffect(() => {
    fetchRules();
  }, []);

  const fetchRules = async () => {
    try {
      setLoading(true);
      const response = await fraudRulesAPI.listRules();
      console.log('Fraud Rules API Response:', response);

      // Handle response - backend returns { rules: [], total: 0, activeCount: 0 }
      const rulesData = response.data?.rules || response.data || [];
      console.log('Extracted rules data:', rulesData);

      if (Array.isArray(rulesData)) {
        setRules(rulesData);
        if (rulesData.length === 0) {
          toast.info('No fraud rules found. Create your first fraud rule.');
        }
      } else {
        console.error('Rules data is not an array:', rulesData);
        setRules([]);
        toast.warning('Unexpected response format from server');
      }
    } catch (error) {
      console.error('Error fetching fraud rules:', error);
      console.error('Error response:', error.response?.data);
      toast.error(error.response?.data?.message || 'Failed to load fraud rules');
      setRules([]);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (rule = null) => {
    if (rule) {
      setEditingRule(rule);
      setFormData({
        ruleName: rule.ruleName,
        ruleType: rule.ruleType,
        description: rule.description || '',
        severity: rule.severity || 'MEDIUM',
        orderOfExecution: rule.orderOfExecution || 100,
        weight: rule.weight || 1.0,
        ruleDefinitionJson: rule.ruleDefinitionJson || '',
        triggeringConditionsJson: rule.triggeringConditionsJson || '',
      });
    } else {
      setEditingRule(null);
      setFormData({
        ruleName: '',
        ruleType: '',
        description: '',
        severity: 'MEDIUM',
        orderOfExecution: 100,
        weight: 1.0,
        ruleDefinitionJson: '',
        triggeringConditionsJson: '',
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingRule(null);
  };

  const handleSaveRule = async () => {
    try {
      if (!formData.ruleName || !formData.ruleType) {
        toast.error('Please fill in all required fields');
        return;
      }

      const requestData = {
        ruleName: formData.ruleName,
        ruleType: formData.ruleType,
        description: formData.description,
        severity: formData.severity || 'MEDIUM',
        orderOfExecution: parseInt(formData.orderOfExecution) || 100,
        weight: parseFloat(formData.weight) || 1.0,
        ruleDefinitionJson: formData.ruleDefinitionJson,
        triggeringConditionsJson: formData.triggeringConditionsJson,
      };

      if (editingRule) {
        await fraudRulesAPI.updateRule(editingRule.id, requestData);
        toast.success('Fraud rule updated successfully');
      } else {
        await fraudRulesAPI.createRule(requestData);
        toast.success('Fraud rule created successfully');
      }

      handleCloseDialog();
      fetchRules();
    } catch (error) {
      console.error('Error saving fraud rule:', error);
      toast.error(error.response?.data?.message || 'Error saving fraud rule');
    }
  };

  const handleDeleteRule = async (ruleId) => {
    if (window.confirm('Are you sure you want to delete this fraud rule?')) {
      try {
        await fraudRulesAPI.deleteRule(ruleId);
        setRules(rules.filter((r) => r.id !== ruleId));
        toast.success('Fraud rule deleted successfully');
      } catch (error) {
        console.error('Error deleting fraud rule:', error);
        toast.error('Error deleting fraud rule');
      }
    }
  };

  const handleToggleRule = async (ruleId) => {
    try {
      await fraudRulesAPI.toggleRule(ruleId);
      fetchRules();
      toast.success('Fraud rule toggled successfully');
    } catch (error) {
      console.error('Error toggling fraud rule:', error);
      toast.error('Error toggling fraud rule');
    }
  };

  const getRuleTypeLabel = (typeId) => {
    const rule = RULE_TYPES.find((r) => r.id === typeId);
    return rule ? `${rule.icon} ${rule.name}` : typeId;
  };

  const getSeverityColor = (severity) => {
    switch (severity) {
      case 'CRITICAL':
        return 'error';
      case 'HIGH':
        return 'warning';
      case 'MEDIUM':
        return 'info';
      case 'LOW':
        return 'success';
      default:
        return 'default';
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          🛡️ Fraud Rules Management
        </Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => handleOpenDialog()}
        >
          New Fraud Rule
        </Button>
      </Box>

      <Alert severity="info" sx={{ mb: 3 }}>
        🎯 Manage fraud detection rules. These rules are applied to payments to detect suspicious activity. You can enable/disable rules dynamically without redeploying code.
      </Alert>

      <Card>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow sx={{ backgroundColor: '#f5f5f5' }}>
                <TableCell sx={{ fontWeight: 700 }}>Rule Name</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Type</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Description</TableCell>
                <TableCell sx={{ fontWeight: 700 }} align="center">Severity</TableCell>
                <TableCell sx={{ fontWeight: 700 }} align="center">Order</TableCell>
                <TableCell sx={{ fontWeight: 700 }} align="center">Weight</TableCell>
                <TableCell sx={{ fontWeight: 700 }} align="center">Status</TableCell>
                <TableCell sx={{ fontWeight: 700 }} align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rules.length > 0 ? (
                rules.map((rule) => (
                  <TableRow key={rule.id} hover>
                    <TableCell sx={{ fontWeight: 600 }}>{rule.ruleName}</TableCell>
                    <TableCell>{getRuleTypeLabel(rule.ruleType)}</TableCell>
                    <TableCell>{rule.description || '-'}</TableCell>
                    <TableCell align="center">
                      <Chip
                        label={rule.severity}
                        size="small"
                        color={getSeverityColor(rule.severity)}
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell align="center">{rule.orderOfExecution}</TableCell>
                    <TableCell align="center">{rule.weight ? rule.weight.toFixed(2) : '1.00'}</TableCell>
                    <TableCell align="center">
                      <StatusBadge
                        status={rule.isActive ? 'ACTIVE' : 'INACTIVE'}
                        label={rule.isActive ? 'Active' : 'Inactive'}
                      />
                    </TableCell>
                    <TableCell align="center">
                      <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
                        <Button
                          size="small"
                          variant="outlined"
                          startIcon={<PowerIcon />}
                          onClick={() => handleToggleRule(rule.id)}
                          title={rule.isActive ? 'Deactivate' : 'Activate'}
                        >
                          Toggle
                        </Button>
                        <Button
                          size="small"
                          variant="outlined"
                          startIcon={<EditIcon />}
                          onClick={() => handleOpenDialog(rule)}
                        >
                          Edit
                        </Button>
                        <Button
                          size="small"
                          variant="outlined"
                          color="error"
                          startIcon={<DeleteIcon />}
                          onClick={() => handleDeleteRule(rule.id)}
                        >
                          Delete
                        </Button>
                      </Box>
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={8} align="center" sx={{ py: 4 }}>
                    <Typography color="textSecondary">No fraud rules found. Create your first rule.</Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>

      {/* Create/Edit Dialog */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {editingRule ? '✏️ Edit Fraud Rule' : '🆕 Create New Fraud Rule'}
        </DialogTitle>
        <DialogContent sx={{ pt: 2 }}>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                label="Rule Name"
                fullWidth
                value={formData.ruleName}
                onChange={(e) => setFormData({ ...formData, ruleName: e.target.value })}
                placeholder="e.g., LARGE_TRANSACTION_RULE"
                disabled={!!editingRule}
                helperText={editingRule ? 'Rule name cannot be changed after creation' : ''}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Rule Type"
                select
                fullWidth
                value={formData.ruleType}
                onChange={(e) => setFormData({ ...formData, ruleType: e.target.value })}
              >
                {RULE_TYPES.map((type) => (
                  <MenuItem key={type.id} value={type.id}>
                    {type.icon} {type.name}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Description"
                fullWidth
                multiline
                rows={3}
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="Describe what this rule detects and why it's important"
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                label="Severity"
                select
                fullWidth
                value={formData.severity || 'MEDIUM'}
                onChange={(e) => setFormData({ ...formData, severity: e.target.value })}
              >
                <MenuItem value="LOW">Low</MenuItem>
                <MenuItem value="MEDIUM">Medium</MenuItem>
                <MenuItem value="HIGH">High</MenuItem>
                <MenuItem value="CRITICAL">Critical</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={6}>
              <TextField
                label="Execution Order"
                type="number"
                fullWidth
                value={formData.orderOfExecution}
                onChange={(e) => setFormData({ ...formData, orderOfExecution: e.target.value })}
                inputProps={{ min: 1, max: 9999 }}
                helperText="Lower numbers execute first"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Weight"
                type="number"
                fullWidth
                value={formData.weight}
                onChange={(e) => setFormData({ ...formData, weight: e.target.value })}
                inputProps={{ min: 0.1, max: 10, step: 0.1 }}
                helperText="Weight in fraud score calculation (0.1 - 10.0)"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Rule Definition (JSON)"
                fullWidth
                multiline
                rows={2}
                value={formData.ruleDefinitionJson}
                onChange={(e) => setFormData({ ...formData, ruleDefinitionJson: e.target.value })}
                placeholder='{"threshold": 5000, "condition": "gt"}'
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Triggering Conditions (JSON)"
                fullWidth
                multiline
                rows={2}
                value={formData.triggeringConditionsJson}
                onChange={(e) => setFormData({ ...formData, triggeringConditionsJson: e.target.value })}
                placeholder='{"field": "amount", "operator": ">=", "value": 5000}'
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button variant="contained" onClick={handleSaveRule}>
            {editingRule ? 'Update Rule' : 'Create Rule'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default FraudRulesManagement;

