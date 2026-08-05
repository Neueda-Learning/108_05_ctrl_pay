import React from 'react';
import { Box } from '@mui/material';

/* ─── per-status config ─────────────────────────────────────── */
const CONFIG = {
  COMPLETED:  { bg: 'rgba(16,185,129,0.12)',  border: 'rgba(16,185,129,0.40)',  color: '#34D399', dot: '#10B981', glow: 'rgba(16,185,129,0.35)' },
  FAILED:     { bg: 'rgba(239,68,68,0.12)',   border: 'rgba(239,68,68,0.40)',   color: '#F87171', dot: '#EF4444', glow: 'rgba(239,68,68,0.35)'  },
  CREATED:    { bg: 'rgba(99,102,241,0.12)',  border: 'rgba(99,102,241,0.40)',  color: '#818CF8', dot: '#6366F1', glow: 'rgba(99,102,241,0.30)' },
  VALIDATED:  { bg: 'rgba(6,182,212,0.12)',   border: 'rgba(6,182,212,0.40)',   color: '#22D3EE', dot: '#06B6D4', glow: 'rgba(6,182,212,0.30)'  },
  SENT:       { bg: 'rgba(245,158,11,0.12)',  border: 'rgba(245,158,11,0.40)',  color: '#FCD34D', dot: '#F59E0B', glow: 'rgba(245,158,11,0.30)' },
  PENDING:    { bg: 'rgba(245,158,11,0.12)',  border: 'rgba(245,158,11,0.40)',  color: '#FCD34D', dot: '#F59E0B', glow: 'rgba(245,158,11,0.30)' },
  ACTIVE:     { bg: 'rgba(16,185,129,0.12)',  border: 'rgba(16,185,129,0.40)',  color: '#34D399', dot: '#10B981', glow: 'rgba(16,185,129,0.35)' },
  INACTIVE:   { bg: 'rgba(100,116,139,0.10)', border: 'rgba(100,116,139,0.30)', color: '#94A3B8', dot: '#64748B', glow: null                   },
};

const DEFAULT = { bg: 'rgba(100,116,139,0.10)', border: 'rgba(100,116,139,0.25)', color: '#94A3B8', dot: '#64748B', glow: null };

/* ─── component ─────────────────────────────────────────────── */
export default function StatusBadge({ status, label }) {
  const cfg  = CONFIG[status?.toUpperCase()] || DEFAULT;
  const text = label || status || '—';

  return (
    <Box
      component="span"
      sx={{
        display:       'inline-flex',
        alignItems:    'center',
        gap:           '5px',
        px:            1.5,
        py:            '4px',
        borderRadius:  '20px',
        background:    cfg.bg,
        border:        `1px solid ${cfg.border}`,
        color:         cfg.color,
        fontSize:      '0.68rem',
        fontWeight:    700,
        letterSpacing: 0.7,
        textTransform: 'uppercase',
        boxShadow:     cfg.glow ? `0 0 12px ${cfg.glow}` : 'none',
        whiteSpace:    'nowrap',
        userSelect:    'none',
      }}
    >
      <Box
        component="span"
        sx={{
          width:        6,
          height:       6,
          borderRadius: '50%',
          flexShrink:   0,
          background:   cfg.dot,
          boxShadow:    cfg.glow ? `0 0 6px ${cfg.dot}` : 'none',
        }}
      />
      {text}
    </Box>
  );
}
