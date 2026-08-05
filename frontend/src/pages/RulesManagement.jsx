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
import { rulesAPI } from '../services/api';
import { toast } from 'react-toastify';
import StatusBadge from '../components/StatusBadge';

const RULE_TYPES = [
  { id: 'CURRENCY_WHITELIST', name: 'Currency Whitelist', icon: '💱' },
  { id: 'AMOUNT_RANGE', name: 'Amount Range', icon: '💰' },
  { id: 'ACCOUNT_FORMAT', name: 'Account Format', icon: '📋' },
  { id: 'ACCOUNT_DIFFERENCE', name: 'Account Difference', icon: '👥' },
  { id: 'MOCK_SUFFICIENT_FUNDS', name: 'Mock Sufficient Funds', icon: '🏦' },
];

function RulesManagement() {
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingRule, setEditingRule] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    type: '',
    enabled: true,
    definition: {},
  });

  useEffect(() => {
    fetchRules();
  }, []);

  const fetchRules = async () => {
    try {
      setLoading(true);
      const response = await rulesAPI.listRules();
      setRules(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error('Error fetching rules:', error);
      toast.error('Failed to load validation rules');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (rule = null) => {
    if (rule) {
      setEditingRule(rule);
      setFormData({
        name: rule.name,
        description: rule.description || '',
        ruleType: rule.ruleType,
        severity: rule.severity || 'MEDIUM',
        orderOfExecution: rule.orderOfExecution || 1,
        ruleDefinition: rule.ruleDefinition || {},
      });
    } else {
      setEditingRule(null);
      setFormData({
        name: '',
        description: '',
        ruleType: '',
        severity: 'MEDIUM',
        orderOfExecution: 1,
        ruleDefinition: {},
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
       if (!formData.name || !formData.ruleType) {
         toast.error('Please fill in all required fields');
         return;
       }

       const requestData = {
         name: formData.name,
         description: formData.description,
         ruleType: formData.ruleType,
         severity: formData.severity || 'MEDIUM',
         orderOfExecution: formData.orderOfExecution || 1,
         ruleDefinition: formData.ruleDefinition || {},
       };

       if (editingRule) {
         await rulesAPI.updateRule(editingRule.id, requestData);
         toast.success('Rule updated successfully');
       } else {
         await rulesAPI.createRule(requestData);
         toast.success('Rule created successfully');
       }

       handleCloseDialog();
       fetchRules();
     } catch (error) {
       console.error('Error saving rule:', error);
       toast.error(error.response?.data?.message || 'Error saving rule');
     }
   };

  const handleDeleteRule = async (ruleId) => {
    if (window.confirm('Are you sure you want to delete this rule?')) {
      try {
        setRules(rules.filter((r) => r.id !== ruleId));
        toast.success('Rule deleted successfully');
      } catch (error) {
        console.error('Error deleting rule:', error);
        toast.error('Error deleting rule');
      }
    }
  };

  const handleToggleRule = async (ruleId) => {
    try {
      await rulesAPI.toggleRule(ruleId);
      fetchRules();
      toast.success('Rule toggled successfully');
    } catch (error) {
      console.error('Error toggling rule:', error);
      toast.error('Error toggling rule');
    }
  };

  const getRuleTypeLabel = (typeId) => {
    const rule = RULE_TYPES.find((r) => r.id === typeId);
    return rule ? `${rule.icon} ${rule.name}` : typeId;
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
          Validation Rules Management
        </Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => handleOpenDialog()}
        >
          New Rule
        </Button>
      </Box>

      <Alert severity="info" sx={{ mb: 3 }}>
        📋 Manage payment validation rules. These rules are applied to all payments during the validation phase.
      </Alert>

      <Card>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell sx={{ fontWeight: 700 }}>Rule Name</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Type</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Description</TableCell>
                <TableCell align="center" sx={{ fontWeight: 700 }}>Status</TableCell>
                <TableCell align="center" sx={{ fontWeight: 700 }}>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rules.length > 0 ? (
                 rules.map((rule) => (
                   <TableRow key={rule.id} hover>
                     <TableCell sx={{ fontWeight: 600 }}>{rule.name}</TableCell>
                     <TableCell>{getRuleTypeLabel(rule.ruleType)}</TableCell>
                     <TableCell>{rule.description || '-'}</TableCell>
                     <TableCell align="center">
                       <StatusBadge status={rule.isActive ? 'ACTIVE' : 'INACTIVE'} label={rule.isActive ? 'Active' : 'Inactive'} />
                     </TableCell>
                    <TableCell align="center">
                      <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
                        <Button
                          size="small"
                          variant="outlined"
                          startIcon={<PowerIcon />}
                          onClick={() => handleToggleRule(rule.id)}
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
                  <TableCell colSpan={5} align="center" sx={{ py: 4 }}>
                    <Typography color="textSecondary">No rules found. Create your first rule.</Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {editingRule ? 'Edit Rule' : 'Create New Rule'}
        </DialogTitle>
        <DialogContent sx={{ pt: 2 }}>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                label="Rule Name"
                fullWidth
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="e.g., USD Currency Only"
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
                 placeholder="Optional description of this rule"
               />
             </Grid>
             <Grid item xs={12}>
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
             <Grid item xs={12}>
               <TextField
                 label="Order of Execution"
                 type="number"
                 fullWidth
                 value={formData.orderOfExecution || 1}
                 onChange={(e) => setFormData({ ...formData, orderOfExecution: parseInt(e.target.value) })}
                 inputProps={{ min: 1 }}
               />
             </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleSaveRule}
          >
            Save Rule
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default RulesManagement;

