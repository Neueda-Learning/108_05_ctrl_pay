import React, { useMemo, useRef } from 'react';
import { Box, FormHelperText, TextField, Typography } from '@mui/material';

function PinCodeInput({
  label = 'PIN',
  value = '',
  onChange,
  error = false,
  helperText,
  disabled = false,
  autoFocus = false,
}) {
  const refs = useRef([]);

  const digits = useMemo(() => {
    const normalized = String(value || '').replace(/\D/g, '').slice(0, 4);
    return [0, 1, 2, 3].map((index) => normalized[index] || '');
  }, [value]);

  const emitValue = (nextDigits) => {
    const pin = nextDigits.join('').replace(/\D/g, '').slice(0, 4);
    onChange(pin);
  };

  const handleChange = (index, nextValue) => {
    const sanitized = String(nextValue || '').replace(/\D/g, '');

    // Support paste/autofill into any box.
    if (sanitized.length > 1) {
      const merged = [...digits];
      for (let i = 0; i < sanitized.length && index + i < 4; i += 1) {
        merged[index + i] = sanitized[i];
      }
      emitValue(merged);

      const nextFocus = Math.min(index + sanitized.length, 3);
      refs.current[nextFocus]?.focus();
      return;
    }

    const merged = [...digits];
    merged[index] = sanitized;
    emitValue(merged);

    if (sanitized && index < 3) {
      refs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index, event) => {
    if (event.key === 'Backspace' && !digits[index] && index > 0) {
      refs.current[index - 1]?.focus();
    }

    if (event.key === 'ArrowLeft' && index > 0) {
      refs.current[index - 1]?.focus();
    }

    if (event.key === 'ArrowRight' && index < 3) {
      refs.current[index + 1]?.focus();
    }
  };

  return (
    <Box>
      <Typography variant="body2" sx={{ mb: 1 }}>
        {label}
      </Typography>

      <Box sx={{ display: 'flex', gap: 1 }}>
        {[0, 1, 2, 3].map((index) => (
          <TextField
            key={index}
            inputRef={(element) => {
              refs.current[index] = element;
            }}
            value={digits[index]}
            onChange={(event) => handleChange(index, event.target.value)}
            onKeyDown={(event) => handleKeyDown(index, event)}
            type="password"
            autoFocus={autoFocus && index === 0}
            disabled={disabled}
            error={error}
            sx={{ width: 56 }}
            inputProps={{
              maxLength: 1,
              inputMode: 'numeric',
              pattern: '[0-9]*',
              style: { textAlign: 'center', fontSize: '1.2rem', fontWeight: 700 },
            }}
          />
        ))}
      </Box>

      {helperText ? (
        <FormHelperText error={error} sx={{ ml: 0, mt: 1 }}>
          {helperText}
        </FormHelperText>
      ) : null}
    </Box>
  );
}

export default PinCodeInput;
