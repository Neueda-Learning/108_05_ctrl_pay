import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { alpha, useTheme } from '@mui/material/styles';
import { processMessage, getSuggestedPrompts } from '../services/copilotEngine';
import './copilot.css';

/* ═══════════════════════════════════════════════════════════════════
   AICopilot.jsx — Payment Intelligence Center Floating Copilot
   ═══════════════════════════════════════════════════════════════════ */

/* ── Markdown-light inline renderer (bold only) ──────────────────── */
function renderInline(text) {
  if (!text) return null;
  const parts = text.split(/(\*\*[^*]+\*\*)/g);
  return parts.map((p, i) =>
    p.startsWith('**') && p.endsWith('**')
      ? <strong key={i}>{p.slice(2, -2)}</strong>
      : p
  );
}

/* ── Timestamp helper ────────────────────────────────────────────── */
function fmtTime(date) {
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function fmtTimestamp(ts) {
  if (!ts) return '';
  const d = new Date(ts);
  return isNaN(d.getTime()) ? ts : d.toLocaleString([], { dateStyle: 'short', timeStyle: 'short' });
}

/* ═══════════════════════════════════════════════════════════════════
   RICH RESPONSE RENDERERS
   ═══════════════════════════════════════════════════════════════════ */

/* ── Text ────────────────────────────────────────────────────────── */
function TextResponse({ content }) {
  return <div>{renderInline(content)}</div>;
}

/* ── Error ───────────────────────────────────────────────────────── */
function ErrorResponse({ content, colors }) {
  return (
    <div style={{
      padding: '10px 14px',
      borderRadius: 10,
      background: 'rgba(239,68,68,0.08)',
      borderLeft: '3px solid #EF4444',
      fontSize: '0.82rem',
      color: colors.textPrimary,
    }}>
      ⚠️ {renderInline(content)}
    </div>
  );
}

/* ── Help ─────────────────────────────────────────────────────────── */
function HelpResponse({ content, colors }) {
  return (
    <div>
      <div style={{ marginBottom: 6 }}>{renderInline(content.greeting)}</div>
      <div className="copilot-help-list">
        {content.capabilities.map((c, i) => (
          <div key={i} className="copilot-help-item">
            <span className="copilot-help-icon">{c.icon}</span>
            <div>
              <div className="copilot-help-label" style={{ color: colors.textPrimary }}>{c.label}</div>
              <div className="copilot-help-example">{c.example}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

/* ── Payment Detail ──────────────────────────────────────────────── */
function PaymentDetailResponse({ content, colors, onNavigate }) {
  const p = content;
  const fields = [
    { label: 'ID',          value: `#${p.id}` },
    { label: 'Status',      value: p.status, isStatus: true },
    { label: 'Amount',      value: `${p.amount} ${p.currency}` },
    { label: 'Source',      value: p.sourceAccount },
    { label: 'Destination', value: p.destinationAccount },
    { label: 'Created',     value: fmtTimestamp(p.createdAt) },
  ];
  if (p.errorCode) {
    fields.push({ label: 'Error', value: p.errorCode });
  }
  if (p.fraudProbability != null) {
    fields.push({ label: 'Fraud Risk', value: `${Number(p.fraudProbability).toFixed(1)}%` });
  }

  return (
    <div>
      <div style={{ fontWeight: 700, fontSize: '0.88rem', marginBottom: 4 }}>
        Payment #{p.id}
        <span className={`copilot-status copilot-status-${p.status}`} style={{ marginLeft: 8 }}>
          {p.status}
        </span>
      </div>
      <div className="copilot-detail-grid">
        {fields.map((f, i) => (
          <div key={i} className="copilot-detail-item" style={{ background: alpha(colors.textPrimary, 0.04) }}>
            <div className="copilot-detail-label">{f.label}</div>
            <div className="copilot-detail-value" style={{ color: colors.textPrimary }}>
              {f.isStatus ? (
                <span className={`copilot-status copilot-status-${f.value}`}>{f.value}</span>
              ) : f.value}
            </div>
          </div>
        ))}
      </div>
      <button
        onClick={() => onNavigate(`/payments/${p.id}`)}
        style={{
          marginTop: 10,
          width: '100%',
          padding: '8px',
          borderRadius: 10,
          border: `1px solid ${alpha(colors.brandMain, 0.3)}`,
          background: alpha(colors.brandMain, 0.06),
          color: colors.brandMain,
          fontWeight: 600,
          fontSize: '0.78rem',
          cursor: 'pointer',
          transition: 'background 0.2s',
        }}
      >
        View Full Details →
      </button>
    </div>
  );
}

/* ── Payment Table ───────────────────────────────────────────────── */
function PaymentTableResponse({ content, colors, onNavigate }) {
  const { payments, filterDescription } = content;
  if (!payments || payments.length === 0) {
    return <div>{renderInline(filterDescription)} No payments found matching your criteria.</div>;
  }

  return (
    <div>
      <div style={{ fontSize: '0.82rem', marginBottom: 6 }}>{renderInline(filterDescription)}</div>
      <div style={{ overflowX: 'auto' }}>
        <table className="copilot-payment-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Status</th>
              <th>Amount</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            {payments.slice(0, 10).map(p => (
              <tr key={p.id} onClick={() => onNavigate(`/payments/${p.id}`)}>
                <td style={{ fontWeight: 700, color: colors.brandMain }}>#{p.id}</td>
                <td><span className={`copilot-status copilot-status-${p.status}`}>{p.status}</span></td>
                <td>{p.amount} {p.currency}</td>
                <td style={{ fontSize: '0.72rem', opacity: 0.6 }}>{fmtTimestamp(p.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {payments.length > 10 && (
        <div style={{ fontSize: '0.72rem', opacity: 0.5, marginTop: 6, textAlign: 'center' }}>
          Showing 10 of {payments.length} results
        </div>
      )}
    </div>
  );
}

/* ── Dashboard Insights ──────────────────────────────────────────── */
function DashboardInsightsResponse({ content, colors }) {
  const { successRate, distribution, volume, fraud } = content;
  const sr = successRate || {};
  const dist = distribution || {};
  const vol = volume || {};

  const insights = [
    { value: sr.total_payments ?? '—', label: 'Total Payments', color: colors.brandMain },
    { value: sr.success_rate_percent ?? '—', label: 'Success Rate', color: '#10B981' },
    { value: sr.failed ?? '—', label: 'Failed', color: '#EF4444' },
    { value: sr.pending ?? '—', label: 'Pending', color: '#F59E0B' },
  ];

  // Build a narrative summary
  const total = sr.total_payments || 0;
  const completed = sr.completed || 0;
  const failed = sr.failed || 0;
  const pending = sr.pending || 0;

  let narrative = `Your system has processed **${total} payments** in total.`;
  if (total > 0) {
    narrative += ` The success rate is **${sr.success_rate_percent || '0%'}**, with **${completed}** completed, **${failed}** failed, and **${pending}** still in progress.`;
  }

  if (fraud?.pendingReview > 0) {
    narrative += ` ⚠️ There are **${fraud.pendingReview}** transactions pending fraud review.`;
  }

  return (
    <div>
      <div style={{ fontSize: '0.82rem', lineHeight: 1.6, marginBottom: 4 }}>
        {renderInline(narrative)}
      </div>
      <div className="copilot-insights-grid">
        {insights.map((ins, i) => (
          <div key={i} className="copilot-insight-card" style={{ background: alpha(ins.color, 0.08) }}>
            <div className="copilot-insight-value" style={{ color: ins.color }}>{ins.value}</div>
            <div className="copilot-insight-label">{ins.label}</div>
          </div>
        ))}
      </div>
      {/* Status distribution breakdown */}
      {dist && (
        <div style={{ marginTop: 10, fontSize: '0.78rem', opacity: 0.7 }}>
          <div style={{ fontWeight: 700, marginBottom: 4, opacity: 1 }}>Status Breakdown</div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
            {['CREATED', 'VALIDATED', 'SENT', 'COMPLETED', 'FAILED'].map(s => (
              <span key={s} className={`copilot-status copilot-status-${s}`}>
                {s}: {dist[s] ?? 0}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

/* ── Risk Assessment ─────────────────────────────────────────────── */
function RiskAssessmentResponse({ content, colors }) {
  const { payment, risk } = content;
  const score = payment.fraudProbability != null ? Number(payment.fraudProbability) : 0;
  const circumference = 2 * Math.PI * 54; // radius = 54
  const offset = circumference - (score / 100) * circumference;

  return (
    <div>
      <div style={{ fontWeight: 700, fontSize: '0.88rem', marginBottom: 4 }}>
        Risk Assessment — Payment #{payment.id}
      </div>
      <div className={`copilot-risk-gauge copilot-risk-level-${risk.level.toLowerCase()}`}>
        <div className="copilot-risk-circle">
          <svg viewBox="0 0 120 120" width="120" height="120">
            <circle cx="60" cy="60" r="54" className="copilot-risk-circle-bg" stroke={risk.color} />
            <circle
              cx="60" cy="60" r="54"
              className="copilot-risk-circle-fill"
              stroke={risk.color}
              strokeDasharray={circumference}
              strokeDashoffset={offset}
            />
          </svg>
          <div className="copilot-risk-center">
            <div className="copilot-risk-score" style={{ color: risk.color }}>
              {score.toFixed(0)}%
            </div>
            <div className="copilot-risk-label" style={{ color: risk.color }}>{risk.label}</div>
          </div>
        </div>
        <div className="copilot-risk-desc" style={{ color: colors.textPrimary }}>{risk.description}</div>
        <ul className="copilot-risk-recs">
          {risk.recommendations.map((rec, i) => (
            <li key={i} style={{ background: alpha(risk.color, 0.06), color: colors.textPrimary }}>{rec}</li>
          ))}
        </ul>
      </div>
    </div>
  );
}

/* ── Audit Timeline ──────────────────────────────────────────────── */
function AuditTrailResponse({ content, colors }) {
  const { payment, history, validations } = content;
  const statusColors = {
    CREATED: '#06B6D4',
    VALIDATED: '#6366F1',
    SENT: '#F59E0B',
    COMPLETED: '#10B981',
    FAILED: '#EF4444',
    SUSPICIOUS: '#F59E0B',
  };

  return (
    <div>
      <div style={{ fontWeight: 700, fontSize: '0.88rem', marginBottom: 8 }}>
        Audit Trail — Payment #{payment.id}
        <span className={`copilot-status copilot-status-${payment.status}`} style={{ marginLeft: 8 }}>
          {payment.status}
        </span>
      </div>

      {(!history || history.length === 0) ? (
        <div style={{ fontSize: '0.82rem', opacity: 0.6 }}>No status transitions recorded yet.</div>
      ) : (
        <div className="copilot-timeline">
          {history.map((h, i) => {
            const isLast = i === history.length - 1;
            const statusKey = h.newStatus || '';
            return (
              <div key={i} className="copilot-timeline-item">
                <div
                  className={`copilot-timeline-node ${isLast ? 'current' : ''}`}
                  style={{ borderColor: statusColors[statusKey] || '#FF003C', background: isLast ? (statusColors[statusKey] || '#FF003C') : colors.bgCard }}
                />
                <div className="copilot-timeline-status" style={{ color: statusColors[statusKey] || colors.textPrimary }}>
                  {h.oldStatus ? `${h.oldStatus} → ${h.newStatus}` : h.newStatus}
                </div>
                <div className="copilot-timeline-meta">
                  {fmtTimestamp(h.timestamp)} {h.triggeredBy ? `• by ${h.triggeredBy}` : ''}
                </div>
                {h.errorCode && (
                  <div className="copilot-timeline-error" style={{ background: alpha('#EF4444', 0.06), color: colors.textPrimary }}>
                    <strong>{h.errorCode}</strong>: {h.errorMessage}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {validations && validations.length > 0 && (
        <div style={{ marginTop: 12 }}>
          <div style={{ fontWeight: 700, fontSize: '0.8rem', marginBottom: 4 }}>Validation Results</div>
          {validations.map((v, i) => (
            <div key={i} style={{
              display: 'flex', alignItems: 'center', gap: 8,
              padding: '5px 10px', borderRadius: 8, marginBottom: 3,
              background: v.passed ? alpha('#10B981', 0.06) : alpha('#EF4444', 0.06),
              fontSize: '0.76rem',
            }}>
              <span>{v.passed ? '✅' : '❌'}</span>
              <span style={{ fontWeight: 600 }}>{v.ruleName || `Rule #${v.validationRuleId}`}</span>
              {!v.passed && v.errorMessage && (
                <span style={{ opacity: 0.6, marginLeft: 'auto', fontSize: '0.7rem' }}>{v.errorMessage}</span>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

/* ── Failure Explanation ─────────────────────────────────────────── */
function FailureExplanationResponse({ content, colors }) {
  const { payment, errorInfo, history, failedValidations } = content;

  return (
    <div>
      <div style={{ fontWeight: 700, fontSize: '0.88rem', marginBottom: 6 }}>
        Why Payment #{payment.id} Failed
      </div>

      <div className="copilot-failure-card" style={{ background: alpha('#EF4444', 0.06) }}>
        <div className="copilot-failure-title" style={{ color: '#EF4444' }}>
          ❌ {errorInfo.title}
        </div>
        <div className="copilot-failure-explain" style={{ color: colors.textPrimary }}>
          {errorInfo.explanation}
        </div>
        <div className="copilot-failure-remediation" style={{ background: alpha('#10B981', 0.06), color: colors.textPrimary }}>
          💡 <strong>How to fix:</strong> {errorInfo.remediation}
        </div>
      </div>

      {failedValidations && failedValidations.length > 0 && (
        <div className="copilot-failure-validations">
          <div style={{ fontWeight: 700, fontSize: '0.8rem', marginBottom: 4, marginTop: 8 }}>
            Failed Validation Rules
          </div>
          {failedValidations.map((v, i) => (
            <div key={i} className="copilot-failure-val-item" style={{ background: alpha('#EF4444', 0.06), color: colors.textPrimary }}>
              <span>❌</span>
              <div>
                <div style={{ fontWeight: 600 }}>{v.ruleName || `Rule #${v.validationRuleId}`}</div>
                {v.errorMessage && <div style={{ fontSize: '0.72rem', opacity: 0.65 }}>{v.errorMessage}</div>}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Compact timeline */}
      {history && history.length > 0 && (
        <div style={{ marginTop: 10 }}>
          <div style={{ fontWeight: 700, fontSize: '0.78rem', opacity: 0.7, marginBottom: 4 }}>
            Payment Journey
          </div>
          <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
            {history.map((h, i) => (
              <span key={i} className={`copilot-status copilot-status-${h.newStatus}`}>
                {h.newStatus}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}


/* ═══════════════════════════════════════════════════════════════════
   RESPONSE RENDERER  — dispatches to the right component
   ═══════════════════════════════════════════════════════════════════ */

function ResponseRenderer({ response, colors, onNavigate }) {
  switch (response.type) {
    case 'text':
      return <TextResponse content={response.content} />;
    case 'error':
      return <ErrorResponse content={response.content} colors={colors} />;
    case 'help':
      return <HelpResponse content={response.content} colors={colors} />;
    case 'payment-detail':
      return <PaymentDetailResponse content={response.content} colors={colors} onNavigate={onNavigate} />;
    case 'payment-table':
      return <PaymentTableResponse content={response.content} colors={colors} onNavigate={onNavigate} />;
    case 'dashboard-insights':
      return <DashboardInsightsResponse content={response.content} colors={colors} />;
    case 'risk-assessment':
      return <RiskAssessmentResponse content={response.content} colors={colors} />;
    case 'audit-trail':
      return <AuditTrailResponse content={response.content} colors={colors} />;
    case 'failure-explanation':
      return <FailureExplanationResponse content={response.content} colors={colors} />;
    default:
      return <TextResponse content={JSON.stringify(response.content)} />;
  }
}


/* ═══════════════════════════════════════════════════════════════════
   MAIN COPILOT COMPONENT
   ═══════════════════════════════════════════════════════════════════ */

export default function AICopilot() {
  const theme = useTheme();
  const custom = theme.customTokens;
  const navigate = useNavigate();

  const colors = {
    textPrimary: custom.text.primary,
    textSecondary: custom.text.secondary,
    textMuted: custom.text.muted,
    bgCard: custom.background.card,
    bgSecondary: custom.background.secondary,
    brandMain: custom.brand.main,
    brandAccent: custom.brand.accent,
    border: custom.border,
  };

  const isDark = theme.palette.mode === 'dark';

  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [showSuggestions, setShowSuggestions] = useState(true);

  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  // Auto-scroll to bottom
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  // Focus input when opened
  useEffect(() => {
    if (open) {
      setTimeout(() => inputRef.current?.focus(), 350);
    }
  }, [open]);

  // Ctrl+K shortcut
  useEffect(() => {
    const handler = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        setOpen(prev => !prev);
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  // Extract a recent payment ID from conversation for suggestions
  const lastPaymentId = messages.reduce((acc, m) => {
    if (m.role === 'bot' && m.response?.content) {
      const c = m.response.content;
      if (c.id) return c.id;
      if (c.payment?.id) return c.payment.id;
      if (c.payments?.[0]?.id) return c.payments[0].id;
    }
    return acc;
  }, null);

  const suggestions = getSuggestedPrompts(lastPaymentId);

  const handleSend = useCallback(async (text) => {
    const msg = (text || input).trim();
    if (!msg || loading) return;

    const userMsg = { role: 'user', text: msg, time: new Date() };
    setMessages(prev => [...prev, userMsg]);
    setInput('');
    setLoading(true);
    setShowSuggestions(false);

    try {
      const response = await processMessage(msg);
      const botMsg = { role: 'bot', response, time: new Date() };
      setMessages(prev => [...prev, botMsg]);
    } catch (err) {
      const errMsg = {
        role: 'bot',
        response: { type: 'error', content: `Something went wrong: ${err.message}` },
        time: new Date(),
      };
      setMessages(prev => [...prev, errMsg]);
    } finally {
      setLoading(false);
      setShowSuggestions(true);
    }
  }, [input, loading]);

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleNavigate = (path) => {
    navigate(path);
    setOpen(false);
  };

  // Panel styles (glassmorphism)
  const panelStyle = {
    background: isDark
      ? alpha(custom.background.card, 0.85)
      : alpha(custom.background.card, 0.92),
    backdropFilter: 'blur(28px)',
    WebkitBackdropFilter: 'blur(28px)',
    border: `1px solid ${alpha(custom.border, 0.6)}`,
    boxShadow: isDark
      ? `0 24px 64px rgba(0,0,0,0.55), 0 0 0 1px ${alpha(custom.brand.main, 0.08)}`
      : `0 20px 50px rgba(0,0,0,0.12), 0 0 0 1px ${alpha(custom.brand.main, 0.06)}`,
  };

  const headerStyle = {
    background: isDark
      ? `linear-gradient(135deg, ${alpha(custom.brand.main, 0.12)} 0%, ${alpha(custom.background.secondary, 0.9)} 100%)`
      : `linear-gradient(135deg, ${alpha(custom.brand.main, 0.06)} 0%, ${alpha(custom.background.secondary, 0.95)} 100%)`,
    borderBottom: `1px solid ${alpha(custom.border, 0.5)}`,
  };

  const inputAreaStyle = {
    background: isDark
      ? alpha(custom.background.secondary, 0.6)
      : alpha(custom.background.secondary, 0.8),
    borderTop: `1px solid ${alpha(custom.border, 0.4)}`,
  };

  const inputStyle = {
    background: isDark ? alpha(custom.text.primary, 0.06) : alpha(custom.text.primary, 0.04),
    color: custom.text.primary,
  };

  const chipStyle = {
    background: isDark ? alpha(custom.brand.main, 0.08) : alpha(custom.brand.main, 0.06),
    color: custom.text.secondary,
    border: `1px solid ${alpha(custom.brand.main, 0.12)}`,
  };

  const botBubbleStyle = {
    background: isDark ? alpha(custom.text.primary, 0.05) : alpha(custom.text.primary, 0.04),
    color: custom.text.primary,
  };

  return (
    <>
      {/* ── Panel ──────────────────────────────────────────────────── */}
      {open && (
        <div className="copilot-panel" style={panelStyle} id="copilot-panel">
          {/* Header */}
          <div className="copilot-header" style={headerStyle}>
            <div className="copilot-header-avatar">🤖</div>
            <div className="copilot-header-info">
              <div className="copilot-header-title" style={{ color: custom.text.primary }}>
                Payment Intelligence
              </div>
              <div className="copilot-header-subtitle" style={{ color: custom.text.muted }}>
                AI Copilot • Ctrl+K
              </div>
            </div>
            <button
              className="copilot-header-close"
              onClick={() => setOpen(false)}
              style={{ color: custom.text.muted }}
              aria-label="Close copilot"
            >
              ✕
            </button>
          </div>

          {/* Messages */}
          <div className="copilot-messages" id="copilot-messages">
            {messages.length === 0 && !loading && (
              <div style={{
                textAlign: 'center',
                padding: '32px 16px',
                opacity: 0.5,
                fontSize: '0.82rem',
              }}>
                <div style={{ fontSize: '2rem', marginBottom: 8 }}>🧠</div>
                <div style={{ fontWeight: 600 }}>Payment Intelligence Center</div>
                <div style={{ marginTop: 4, fontSize: '0.75rem' }}>
                  Ask me about payments, failures, risk scores, or insights.
                </div>
              </div>
            )}

            {messages.map((msg, i) => (
              <div key={i} className={`copilot-msg ${msg.role}`}>
                <div className="copilot-msg-bubble" style={msg.role === 'bot' ? botBubbleStyle : undefined}>
                  {msg.role === 'user' ? (
                    msg.text
                  ) : (
                    <ResponseRenderer
                      response={msg.response}
                      colors={colors}
                      onNavigate={handleNavigate}
                    />
                  )}
                </div>
                <div className="copilot-msg-time" style={{ color: custom.text.muted }}>
                  {fmtTime(msg.time)}
                </div>
              </div>
            ))}

            {loading && (
              <div className="copilot-typing" style={botBubbleStyle}>
                <div className="copilot-typing-dot" />
                <div className="copilot-typing-dot" />
                <div className="copilot-typing-dot" />
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Suggestions */}
          {showSuggestions && messages.length < 3 && (
            <div className="copilot-suggestions" style={{ borderTop: `1px solid ${alpha(custom.border, 0.3)}` }}>
              {suggestions.slice(0, 4).map((s, i) => (
                <button
                  key={i}
                  className="copilot-suggestion-chip"
                  style={chipStyle}
                  onClick={() => handleSend(s)}
                >
                  {s}
                </button>
              ))}
            </div>
          )}

          {/* Input */}
          <div className="copilot-input-area" style={inputAreaStyle}>
            <input
              ref={inputRef}
              className="copilot-input"
              style={inputStyle}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask about payments..."
              disabled={loading}
              id="copilot-input"
            />
            <button
              className="copilot-send-btn"
              onClick={() => handleSend()}
              disabled={loading || !input.trim()}
              aria-label="Send message"
              id="copilot-send"
            >
              ↑
            </button>
          </div>
        </div>
      )}

      {/* ── FAB ────────────────────────────────────────────────────── */}
      <button
        className={`copilot-fab ${open ? 'open' : ''}`}
        onClick={() => setOpen(prev => !prev)}
        aria-label="Toggle AI Copilot"
        id="copilot-fab"
      >
        {open ? '✕' : '🧠'}
        {!open && messages.length === 0 && (
          <span className="copilot-fab-badge">AI</span>
        )}
      </button>
    </>
  );
}
