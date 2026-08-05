import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { alpha } from '@mui/material/styles';

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
  Tooltip,
  Avatar,
  useMediaQuery,
  useTheme,
} from '@mui/material';

import {
  Dashboard          as DashboardIcon,
  PersonAdd          as PersonAddIcon,
  CreditCard         as CreditCardIcon,
  AccountBalanceWallet as WalletIcon,
  TrendingUp         as TrendingUpIcon,
  ReceiptLong        as ReceiptIcon,
  ManageAccounts     as ManageAccountsIcon,
  Menu               as MenuIcon,
  ChevronLeft        as ChevronLeftIcon,
  Logout             as LogoutIcon,
} from '@mui/icons-material';

import { useCustomer } from '../context/CustomerContext';
import ThemeToggle from './ThemeToggle';


/* ─── constants ──────────────────────────────────────────────────── */
const DRAWER_FULL = 272;
const DRAWER_MINI = 72;

const menuSections = [
  {
    label: 'OVERVIEW',
    items: [
      { label: 'Home',            icon: DashboardIcon,      path: '/'                },
    ],
  },
  {
    label: 'CUSTOMERS',
    items: [
      { label: 'Create Customer', icon: PersonAddIcon,      path: '/customers/new'   },
      { label: 'Create Account',  icon: CreditCardIcon,     path: '/accounts/new'    },
    ],
  },
  {
    label: 'PAYMENTS',
    items: [
      { label: 'Make Payment',    icon: WalletIcon,         path: '/payments/create' },
      { label: 'Payment History', icon: ReceiptIcon,        path: '/payments'        },
    ],
  },
  {
    label: 'ANALYTICS',
    items: [
      { label: 'Statistics',      icon: TrendingUpIcon,     path: '/statistics'      },
    ],
  },
  {
    label: 'SYSTEM',
    items: [
      { label: 'Rules',           icon: ManageAccountsIcon, path: '/rules'           },
    ],
  },
];

/* ─── component ──────────────────────────────────────────────────── */
function Layout({ children }) {

  const [open, setOpen] = useState(true);

  const location = useLocation();
  const navigate = useNavigate();
  const theme    = useTheme();
  const custom   = theme.customTokens;
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  const { customerId, customer, clearCustomer } = useCustomer();

  const drawerWidth = open ? DRAWER_FULL : DRAWER_MINI;

  const handleDrawerToggle  = () => setOpen(prev => !prev);
  const handleClearCustomer = () => { clearCustomer(); navigate('/'); };


  /* ── nav item ───────────────────────────────────────────────────── */
  const NavItem = ({ item }) => {
    const isActive = location.pathname === item.path;
    const Icon     = item.icon;

    const inner = (
      <ListItem
        button
        component={Link}
        to={item.path}
        sx={{
          mx:             '8px',
          mb:             '4px',
          width:          'calc(100% - 16px)',
          borderRadius:   '12px',
          minHeight:      48,
          px:             open ? 1.5 : '14px',
          py:             '10px',
          justifyContent: open ? 'flex-start' : 'center',
          position:       'relative',
          overflow:       'hidden',
          transition:     'all 0.25s cubic-bezier(0.4,0,0.2,1)',

          background: isActive
            ? `linear-gradient(135deg, ${custom.brand.main} 0%, ${custom.brand.accent} 100%)`
            : 'transparent',

          boxShadow: isActive
            ? `0 4px 20px ${alpha(custom.brand.main, 0.35)}, inset 0 1px 0 ${alpha(custom.text.primary, 0.12)}`
            : 'none',

          '&::before': isActive ? {
            content:      '""',
            position:     'absolute',
            inset:        0,
            background:   'linear-gradient(90deg, transparent 0%, rgba(255,255,255,0.06) 50%, transparent 100%)',
            pointerEvents:'none',
          } : {},

          '& .MuiListItemIcon-root': {
            color:      isActive ? '#FFFFFF' : custom.text.muted,
            minWidth:   open ? 38 : 'unset',
            transition: 'color 0.25s ease',
          },

          '& .MuiListItemText-primary': {
            color:         isActive ? '#FFFFFF' : custom.text.secondary,
            fontWeight:    isActive ? 700 : 500,
            fontSize:      '0.875rem',
            letterSpacing: 0.2,
            transition:    'color 0.25s ease',
            whiteSpace:    'nowrap',
            overflow:      'hidden',
          },

          '&:hover': {
            background: isActive
              ? `linear-gradient(135deg, ${custom.brand.main} 0%, ${custom.brand.accent} 100%)`
              : custom.hoverOverlay,
            transform: 'translateX(3px)',
            '& .MuiListItemIcon-root': { color: isActive ? '#FFFFFF' : custom.brand.accent },
            '& .MuiListItemText-primary': { color: isActive ? '#FFFFFF' : custom.text.primary },
          },
        }}
      >
        <ListItemIcon><Icon fontSize="small" /></ListItemIcon>
        <ListItemText
          primary={item.label}
          sx={{
            opacity:    open ? 1 : 0,
            maxWidth:   open ? 200 : 0,
            transition: 'opacity 0.2s ease, max-width 0.25s ease',
            overflow:   'hidden',
            ml:         0,
          }}
        />
      </ListItem>
    );

    return open ? inner : (
      <Tooltip title={item.label} placement="right" arrow>{inner}</Tooltip>
    );
  };


  /* ── drawer content ─────────────────────────────────────────────── */
  const DrawerContent = (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', pt: 1.5, pb: 1.5 }}>

      {menuSections.map((section, si) => (
        <Box key={section.label}>

          {open ? (
            <Typography
              variant="caption"
              sx={{
                display:       'block',
                px:            3,
                py:            '6px',
                color:         custom.text.muted,
                fontWeight:    700,
                fontSize:      '0.62rem',
                letterSpacing: 1.5,
                userSelect:    'none',
                mt:            si === 0 ? 0 : 1.5,
              }}
            >
              {section.label}
            </Typography>
          ) : (
            si > 0 && (
              <Divider sx={{ borderColor: alpha(custom.border, 0.55), mx: 1.5, my: 1 }} />
            )
          )}

          <List disablePadding>
            {section.items.map(item => <NavItem key={item.path} item={item} />)}
          </List>

        </Box>
      ))}


      {/* spacer */}
      <Box sx={{ flexGrow: 1 }} />


      <Divider sx={{ borderColor: alpha(custom.border, 0.75), mx: 1.5, mb: 1.5 }} />


      {/* user profile */}
      <Box sx={{ px: 1 }}>
        <Box
          sx={{
            display:        'flex',
            alignItems:     'center',
            gap:            1.5,
            p:              1.5,
            borderRadius:   '12px',
            background:     custom.mutedOverlay,
            border:         `1px solid ${alpha(custom.border, 0.8)}`,
            transition:     'background 0.2s ease',
            justifyContent: open ? 'flex-start' : 'center',
            '&:hover':      { background: alpha(custom.text.primary, 0.08) },
          }}
        >
          <Avatar
            sx={{
              width:      36,
              height:     36,
              flexShrink: 0,
              background: `linear-gradient(135deg, ${custom.brand.main} 0%, ${custom.brand.accent} 100%)`,
              fontSize:   '0.9rem',
              fontWeight: 700,
              boxShadow:  `0 2px 10px ${alpha(custom.brand.main, 0.5)}`,
            }}
          >
            {customer?.name?.[0]?.toUpperCase() || 'U'}
          </Avatar>

          {open && (
            <>
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography
                  variant="body2"
                  sx={{
                    fontWeight:   600,
                    color:        custom.text.primary,
                    whiteSpace:   'nowrap',
                    overflow:     'hidden',
                    textOverflow: 'ellipsis',
                    fontSize:     '0.825rem',
                  }}
                >
                  {customer?.name || 'Guest User'}
                </Typography>
                <Typography variant="caption" sx={{ color: custom.text.muted, fontSize: '0.7rem' }}>
                  {customerId ? `ID: ${customerId}` : 'No customer selected'}
                </Typography>
              </Box>

              {customerId && (
                <Tooltip title="Clear Customer">
                  <IconButton
                    size="small"
                    onClick={handleClearCustomer}
                    sx={{
                      color:      custom.text.muted,
                      flexShrink: 0,
                      '&:hover':  { color: theme.palette.error.main, background: alpha(theme.palette.error.main, 0.12) },
                    }}
                  >
                    <LogoutIcon sx={{ fontSize: 16 }} />
                  </IconButton>
                </Tooltip>
              )}
            </>
          )}
        </Box>
      </Box>

    </Box>
  );


  /* ── render ─────────────────────────────────────────────────────── */
  return (

    <Box
      sx={{
        display:    'flex',
        height:     '100vh',
        overflow:   'hidden',
        position:   'relative',
        background: 'var(--theme-app-gradient)',
      }}
    >

      {/* glow orb — top-left */}
      <Box sx={{
        position: 'fixed', top: '-15%', left: '-10%',
        width: 700, height: 700, borderRadius: '50%',
        pointerEvents: 'none', zIndex: 0,
        background: `radial-gradient(circle, ${alpha(custom.brand.main, 0.16)} 0%, transparent 65%)`,
        filter: 'blur(60px)',
      }} />

      {/* glow orb — bottom-right */}
      <Box sx={{
        position: 'fixed', bottom: '-20%', right: '-12%',
        width: 900, height: 900, borderRadius: '50%',
        pointerEvents: 'none', zIndex: 0,
        background: `radial-gradient(circle, ${alpha(custom.brand.accent, 0.14)} 0%, transparent 65%)`,
        filter: 'blur(80px)',
      }} />

      {/* glow orb — center cyan */}
      <Box sx={{
        position: 'fixed', top: '45%', left: '38%',
        width: 500, height: 500, borderRadius: '50%',
        pointerEvents: 'none', zIndex: 0,
        background: `radial-gradient(circle, ${alpha(custom.brand.main, 0.08)} 0%, transparent 65%)`,
        filter: 'blur(70px)',
      }} />


      {/* ── AppBar ──────────────────────────────────────────────────── */}
      <AppBar
        position="fixed"
        sx={{
          zIndex:               (t) => t.zIndex.drawer + 1,
          background:           custom.appBar,
          backdropFilter:       'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
          borderBottom:         `1px solid ${alpha(custom.border, 0.82)}`,
          boxShadow:            `0 4px 24px ${alpha('#000000', 0.32)}, inset 0 -1px 0 ${alpha(custom.brand.main, 0.2)}`,
        }}
      >
        <Toolbar>

          <IconButton
            color="inherit"
            onClick={handleDrawerToggle}
            edge="start"
            sx={{
              mr:         2,
              color:      custom.text.primary,
              transition: 'transform 0.3s cubic-bezier(0.4,0,0.2,1)',
              transform:  open ? 'rotate(0deg)' : 'rotate(180deg)',
            }}
          >
            {open ? <ChevronLeftIcon /> : <MenuIcon />}
          </IconButton>

          <Typography
            variant="h6"
            sx={{
              flexGrow: 1,
              fontWeight: 800,
              letterSpacing: 1,
              fontSize: '1.3rem',
              color: custom.text.primary,
            }}
          >
            Ctrl-Pay
          </Typography>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>

            <ThemeToggle />

            <Typography variant="body2" sx={{ color: alpha(custom.text.primary, 0.78) }}>
              {customerId ? `Customer: ${customerId}` : 'No customer selected'}
            </Typography>

            {customer && (
              <Typography variant="body2" sx={{ color: alpha(custom.text.primary, 0.62) }}>
                {customer.name}
              </Typography>
            )}

            {customerId && (
              <Button variant="outlined" color="inherit" size="small" onClick={handleClearCustomer}>
                Clear Customer
              </Button>
            )}

          </Box>

        </Toolbar>
      </AppBar>


      {/* ── Sidebar ─────────────────────────────────────────────────── */}
      <Drawer
        variant={isMobile ? 'temporary' : 'permanent'}
        open={isMobile ? open : true}
        onClose={handleDrawerToggle}
        sx={{
          width:      drawerWidth,
          flexShrink: 0,
          transition: 'width 0.3s cubic-bezier(0.4,0,0.2,1)',

          '& .MuiDrawer-paper': {
            width:                drawerWidth,
            boxSizing:            'border-box',
            marginTop:            '64px',
            height:               'calc(100vh - 64px)',
            overflowX:            'hidden',
            overflowY:            'auto',
            scrollbarWidth:       'thin',
            scrollbarColor:       `${alpha(custom.brand.main, 0.65)} ${alpha(custom.background.secondary, 0.55)}`,

            '&::-webkit-scrollbar': {
              width: '8px',
            },
            '&::-webkit-scrollbar-track': {
              background: alpha(custom.background.secondary, 0.55),
              borderRadius: '10px',
            },
            '&::-webkit-scrollbar-thumb': {
              background: alpha(custom.brand.main, 0.65),
              borderRadius: '10px',
            },
            '&::-webkit-scrollbar-thumb:hover': {
              background: alpha(custom.brand.accent, 0.8),
            },

            background:           custom.drawer,
            backdropFilter:       'blur(24px)',
            WebkitBackdropFilter: 'blur(24px)',

            borderRight:  open ? `1px solid ${alpha(custom.border, 0.85)}` : 'none',
            borderRadius: '0 16px 16px 0',
            boxShadow:    `4px 0 32px ${alpha('#000000', 0.24)}, inset -1px 0 0 ${alpha(custom.brand.main, 0.14)}`,

            transition:   'width 0.3s cubic-bezier(0.4,0,0.2,1)',
          },
        }}
      >
        {DrawerContent}
      </Drawer>


      {/* ── Main content ────────────────────────────────────────────── */}
      <Box
        component="main"
        sx={{
          flexGrow:        1,
          p:               3,
          mt:              '64px',
          overflowY:       'auto',
          background:      'transparent',
          transition:      'all 0.3s cubic-bezier(0.4,0,0.2,1)',
          backgroundImage: `radial-gradient(${alpha(custom.brand.main, 0.12)} 1px, transparent 1px)`,
          backgroundSize:  '40px 40px',
        }}
      >
        {children}
      </Box>

    </Box>
  );
}

export default Layout;