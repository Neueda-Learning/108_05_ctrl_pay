import { alpha, createTheme } from '@mui/material/styles';

export const THEME_MODES = {
  light: {
    brand: {
      main: '#E50914',
      accent: '#FF3131',
      glow: 'rgba(255,49,49,0.5)',
    },
    background: {
      main: '#FFFFFF',
      secondary: '#FFF5F5',
      card: '#FFFFFF',
    },
    text: {
      primary: '#111111',
      secondary: '#555555',
      muted: '#888888',
    },
    border: '#FFD6D6',
  },
  dark: {
    brand: {
      main: '#FF003C',
      accent: '#FF1744',
      glow: 'rgba(255,0,60,0.8)',
    },
    background: {
      main: '#090909',
      secondary: '#151515',
      card: '#1E1E1E',
    },
    text: {
      primary: '#FFFFFF',
      secondary: '#D0D0D0',
      muted: '#8A8A8A',
    },
    border: '#55000F',
  },
};

const themeTransition = 'background-color 0.4s ease, color 0.4s ease, border-color 0.4s ease, box-shadow 0.4s ease';

export function createAppTheme(mode = 'dark') {
  const isDark = mode === 'dark';
  const tokens = THEME_MODES[isDark ? 'dark' : 'light'];

  const theme = createTheme({
    palette: {
      mode,
      primary: {
        main: tokens.brand.main,
        light: tokens.brand.accent,
      },
      secondary: {
        main: tokens.brand.accent,
      },
      background: {
        default: tokens.background.main,
        paper: tokens.background.card,
      },
      text: {
        primary: tokens.text.primary,
        secondary: tokens.text.secondary,
      },
      divider: tokens.border,
      success: {
        main: '#10B981',
        light: '#34D399',
      },
      warning: {
        main: '#F59E0B',
        light: '#FCD34D',
      },
      error: {
        main: '#EF4444',
        light: '#F87171',
      },
      info: {
        main: '#06B6D4',
        light: '#22D3EE',
      },
    },
    shape: {
      borderRadius: 10,
    },
    typography: {
      fontFamily: '"Segoe UI", "Helvetica", "Arial", sans-serif',
      h1: { fontWeight: 700, fontSize: '2rem' },
      h2: { fontWeight: 700, fontSize: '1.75rem' },
      h3: { fontWeight: 700, fontSize: '1.5rem' },
      h4: { fontWeight: 700, fontSize: '1.25rem' },
      h5: { fontWeight: 700, fontSize: '1rem' },
      h6: { fontWeight: 700, fontSize: '0.875rem' },
    },
    components: {
      MuiCssBaseline: {
        styleOverrides: {
          ':root': {
            '--theme-brand-main': tokens.brand.main,
            '--theme-brand-accent': tokens.brand.accent,
            '--theme-brand-glow': tokens.brand.glow,
            '--theme-bg-main': tokens.background.main,
            '--theme-bg-secondary': tokens.background.secondary,
            '--theme-bg-card': tokens.background.card,
            '--theme-text-primary': tokens.text.primary,
            '--theme-text-secondary': tokens.text.secondary,
            '--theme-text-muted': tokens.text.muted,
            '--theme-border': tokens.border,
            '--theme-app-gradient': isDark
              ? 'linear-gradient(135deg, #090909 0%, #151515 55%, #090909 100%)'
              : 'linear-gradient(135deg, #FFFFFF 0%, #FFF5F5 55%, #FFFFFF 100%)',
          },
          '*, *::before, *::after': {
            transition: themeTransition,
          },
          html: {
            backgroundColor: tokens.background.main,
          },
          body: {
            margin: 0,
            minHeight: '100vh',
            backgroundColor: tokens.background.main,
            color: tokens.text.primary,
            transition: themeTransition,
          },
          '#root': {
            minHeight: '100vh',
            backgroundColor: tokens.background.main,
          },
        },
      },
      MuiButton: {
        styleOverrides: {
          root: {
            textTransform: 'none',
            fontWeight: 600,
            fontSize: '0.875rem',
            borderRadius: 12,
            padding: '10px 22px',
            letterSpacing: 0.3,
            transition: themeTransition,
          },
          contained: {
            background: `linear-gradient(135deg, ${tokens.brand.main} 0%, ${tokens.brand.accent} 100%)`,
            color: '#FFFFFF',
            border: `1px solid ${alpha(tokens.brand.accent, 0.35)}`,
            boxShadow: `0 8px 24px ${alpha(tokens.brand.main, 0.25)}`,
            '&:hover': {
              background: `linear-gradient(135deg, ${tokens.brand.accent} 0%, ${tokens.brand.main} 100%)`,
              boxShadow: `0 12px 30px ${alpha(tokens.brand.main, 0.32)}`,
            },
          },
          outlined: {
            borderColor: alpha(tokens.brand.main, 0.4),
            color: tokens.text.primary,
            '&:hover': {
              borderColor: tokens.brand.main,
              backgroundColor: alpha(tokens.brand.main, 0.08),
            },
          },
        },
      },
      MuiCard: {
        styleOverrides: {
          root: {
            backgroundColor: tokens.background.card,
            border: `1px solid ${tokens.border}`,
            boxShadow: isDark
              ? '0 18px 30px rgba(0,0,0,0.28)'
              : '0 10px 24px rgba(229,9,20,0.08)',
            transition: themeTransition,
          },
        },
      },
      MuiPaper: {
        styleOverrides: {
          root: {
            backgroundImage: 'none',
          },
        },
      },
      MuiTableContainer: {
        styleOverrides: {
          root: {
            backgroundColor: tokens.background.card,
            borderRadius: 16,
            border: `1px solid ${tokens.border}`,
          },
        },
      },
      MuiTableHead: {
        styleOverrides: {
          root: {
            '& .MuiTableCell-root': {
              backgroundColor: tokens.background.secondary,
              color: tokens.text.secondary,
              borderBottom: `1px solid ${tokens.border}`,
            },
          },
        },
      },
      MuiTableRow: {
        styleOverrides: {
          root: {
            backgroundColor: tokens.background.card,
            '&:hover': {
              backgroundColor: alpha(tokens.brand.main, 0.08),
            },
          },
        },
      },
      MuiTableCell: {
        styleOverrides: {
          root: {
            color: tokens.text.secondary,
            borderBottom: `1px solid ${alpha(tokens.border, 0.6)}`,
          },
        },
      },
      MuiDialog: {
        styleOverrides: {
          paper: {
            backgroundColor: tokens.background.card,
            border: `1px solid ${tokens.border}`,
          },
        },
      },
    },
  });

  theme.customTokens = {
    ...tokens,
    transition: themeTransition,
    appBar: alpha(tokens.background.main, isDark ? 0.82 : 0.9),
    drawer: alpha(tokens.background.secondary, isDark ? 0.88 : 0.94),
    mutedOverlay: alpha(tokens.text.primary, isDark ? 0.06 : 0.04),
    hoverOverlay: alpha(tokens.brand.main, 0.12),
    activeOverlay: alpha(tokens.brand.main, 0.2),
    gridHeader: tokens.background.secondary,
    gridRow: tokens.background.card,
    gridOverlay: alpha(tokens.background.secondary, isDark ? 0.88 : 0.95),
  };

  return theme;
}
