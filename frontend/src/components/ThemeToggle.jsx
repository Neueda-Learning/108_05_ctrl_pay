import React from 'react';
import { alpha } from '@mui/material/styles';
import { Box, IconButton, Tooltip, useTheme } from '@mui/material';
import { DarkModeRounded, LightModeRounded } from '@mui/icons-material';
import { useAppTheme } from '../context/ThemeContext';

function ToggleIcon({ active, icon, label }) {
  const theme = useTheme();
  const custom = theme.customTokens;

  return (
    <Tooltip title={label} arrow>
      <Box
        sx={{
          width: 34,
          height: 34,
          borderRadius: '50%',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: active ? custom.brand.main : custom.text.muted,
          backgroundColor: active ? alpha(custom.brand.main, 0.12) : 'transparent',
          boxShadow: active ? `0 0 18px ${custom.brand.glow}` : 'none',
          transform: `rotate(${active ? '0deg' : '-180deg'}) scale(${active ? 1 : 0.92})`,
          transition: custom.transition,
          zIndex: 1,
        }}
      >
        {icon}
      </Box>
    </Tooltip>
  );
}

export default function ThemeToggle() {
  const theme = useTheme();
  const custom = theme.customTokens;
  const { mode, toggleTheme } = useAppTheme();
  const isDark = mode === 'dark';

  return (
    <IconButton
      onClick={toggleTheme}
      aria-label={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
      sx={{
        p: 0.5,
        borderRadius: '999px',
        border: `1px solid ${custom.border}`,
        backgroundColor: custom.background.card,
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      <Box
        sx={{
          position: 'absolute',
          width: 34,
          height: 34,
          borderRadius: '50%',
          background: `radial-gradient(circle, ${alpha(custom.brand.main, 0.24)} 0%, transparent 70%)`,
          transform: `translateX(${isDark ? '34px' : '0px'})`,
          transition: custom.transition,
          pointerEvents: 'none',
        }}
      />

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, px: 0.5 }}>
        <ToggleIcon
          active={!isDark}
          icon={<LightModeRounded sx={{ fontSize: 18 }} />}
          label="Light mode"
        />
        <ToggleIcon
          active={isDark}
          icon={<DarkModeRounded sx={{ fontSize: 18 }} />}
          label="Dark mode"
        />
      </Box>
    </IconButton>
  );
}
