import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import {
  Box,
  AppBar,
  Toolbar,
  Drawer,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Typography,
  Divider,
  Button,
  IconButton,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import {
  Dashboard as DashboardIcon,
  PersonAdd as PersonAddIcon,
  CreditCard as CreditCardIcon,
  Payment as PaymentIcon,
  Settings as SettingsIcon,
  BarChart as AnalyticsIcon,
  Menu as MenuIcon,
  Close as CloseIcon,
} from '@mui/icons-material';
import { useCustomer } from '../context/CustomerContext';

const DRAWER_WIDTH = 280;

const menuItems = [
  { label: 'Home', icon: <DashboardIcon />, path: '/' },
  { label: 'Create Customer', icon: <PersonAddIcon />, path: '/customers/new' },
  { label: 'Create Account', icon: <CreditCardIcon />, path: '/accounts/new' },
  { label: 'Make Payment', icon: <PaymentIcon />, path: '/payments/create' },
  { label: 'Statistics', icon: <AnalyticsIcon />, path: '/statistics' },
  { label: 'Payments', icon: <PaymentIcon />, path: '/payments' },
  { label: 'Rules Management', icon: <SettingsIcon />, path: '/rules' },
];

function Layout({ children }) {
  const [open, setOpen] = useState(true);
  const location = useLocation();
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const { customerId, customer, clearCustomer } = useCustomer();

  const handleDrawerToggle = () => {
    setOpen(!open);
  };

  const handleClearCustomer = () => {
    clearCustomer();
    navigate('/');
  };

  return (
    <Box sx={{ display: 'flex', height: '100vh' }}>
      {/* AppBar */}
      <AppBar
        position="fixed"
        sx={{
          zIndex: (theme) => theme.zIndex.drawer + 1,
          backgroundColor: '#1976d2',
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="toggle drawer"
            onClick={handleDrawerToggle}
            edge="start"
            sx={{ mr: 2 }}
          >
            {open ? <CloseIcon /> : <MenuIcon />}
          </IconButton>
          <Typography
            variant="h6"
            sx={{
              flexGrow: 1,
              fontWeight: 700,
              fontSize: '1.3rem',
              letterSpacing: 0.5,
            }}
          >
            💳 Ctrl-Pay
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
            <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.85)' }}>
              {customerId ? `Customer: ${customerId}` : 'No customer selected'}
            </Typography>
            {customer && (
              <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.7)' }}>
                {customer.name}
              </Typography>
            )}
            {customerId && (
              <Button color="inherit" variant="outlined" size="small" onClick={handleClearCustomer}>
                Clear Customer
              </Button>
            )}
          </Box>
        </Toolbar>
      </AppBar>

      {/* Drawer */}
      <Drawer
        variant={isMobile ? 'temporary' : 'permanent'}
        open={open}
        onClose={handleDrawerToggle}
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: DRAWER_WIDTH,
            boxSizing: 'border-box',
            backgroundColor: '#fafafa',
            borderRight: '1px solid #e0e0e0',
            marginTop: '64px',
            height: 'calc(100vh - 64px)',
          },
        }}
      >
        <List>
          {menuItems.map((item) => (
            <ListItem
              button
              key={item.label}
              component={Link}
              to={item.path}
              selected={location.pathname === item.path}
              sx={{
                mx: 1,
                my: 0.5,
                borderRadius: 1,
                '&.Mui-selected': {
                  backgroundColor: 'rgba(25, 118, 210, 0.08)',
                  color: 'primary.main',
                  '& .MuiListItemIcon-root': {
                    color: 'primary.main',
                  },
                },
                '&:hover': {
                  backgroundColor: 'rgba(0, 0, 0, 0.04)',
                },
              }}
            >
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.label} />
            </ListItem>
          ))}
        </List>
        <Divider sx={{ my: 2 }} />
        <Box sx={{ px: 2, py: 1 }}>
          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
            Customer-ID flow enabled
          </Typography>
        </Box>
      </Drawer>

      {/* Main Content */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: '100%',
          marginTop: '64px',
          overflowY: 'auto',
          backgroundColor: '#f5f5f5',
        }}
      >
        {children}
      </Box>
    </Box>
  );
}

export default Layout;

