import React, { useState, useEffect, useRef } from 'react';
import {
  Box,
  Card,
  CardContent,
  Grid,
  Typography,
  Button,
  TextField,
  Tabs,
  Tab,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  FormControlLabel,
  Switch,
  Slider,
  Alert,
  Chip,
  LinearProgress,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  PlayArrow,
  Pause,
  Stop,
  SkipPrevious,
  SkipNext,
  Speed,
  BugReport,
  History,
  Timeline as TimelineIcon,
  Refresh,
  CheckCircle,
  Error as ErrorIcon,
  Schedule,
} from '@mui/icons-material';

import { paymentAPI } from '../services/api';
import StatusBadge from '../components/StatusBadge';
import {
  ReplayEngine,
  SimulationEngine,
  FAILURE_TYPES,
  calculateDurations,
  formatDuration,
} from '../services/simulatorEngine';
import './simulator.css';

export default function LifecycleSimulator() {
  const [activeTab, setActiveTab] = useState(0);

  // Shared / Audit State
  const [paymentIdInput, setPaymentIdInput] = useState('1');
  const [loadingPayment, setLoadingPayment] = useState(false);
  const [payment, setPayment] = useState(null);
  const [rawHistory, setRawHistory] = useState([]);
  const [timedHistory, setTimedHistory] = useState([]);
  const [errorMsg, setErrorMsg] = useState('');

  // Replay State
  const [replayState, setReplayState] = useState({
    currentIndex: -1,
    totalSteps: 0,
    currentEvent: null,
    historySoFar: [],
    currentStatus: 'IDLE',
    isPlaying: false,
  });
  const [replaySpeed, setReplaySpeed] = useState(1);
  const replayEngineRef = useRef(null);

  // Simulation State
  const [simConfig, setSimConfig] = useState({
    sourceAccount: '100000000001',
    destinationAccount: '200000000002',
    amount: 250.00,
    currency: 'USD',
    failureType: 'NONE',
    maxRetries: 3,
    retryDelayMs: 1500,
    exponentialBackoff: true,
    speed: 1,
  });

  const [simState, setSimState] = useState({
    status: 'IDLE',
    retryCount: 0,
    maxRetries: 3,
    logs: [],
    isRunning: false,
  });

  const [simOutcome, setSimOutcome] = useState(null);
  const simEngineRef = useRef(null);
  const logEndRef = useRef(null);

  // Auto-scroll simulation log
  useEffect(() => {
    logEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [simState.logs]);

  // Load audit data
  const handleLoadPayment = async () => {
    if (!paymentIdInput) return;
    try {
      setLoadingPayment(true);
      setErrorMsg('');

      const [pRes, hRes] = await Promise.all([
        paymentAPI.getPayment(paymentIdInput),
        paymentAPI.getPaymentHistory(paymentIdInput),
      ]);

      const pData = pRes.data;
      const hData = Array.isArray(hRes.data) ? hRes.data : [];

      setPayment(pData);
      setRawHistory(hData);

      const computed = calculateDurations(hData);
      setTimedHistory(computed);

      // Initialize Replay Engine
      if (replayEngineRef.current) {
        replayEngineRef.current.stop();
      }

      const engine = new ReplayEngine(
        hData,
        (state) => setReplayState(state),
        () => {}
      );
      replayEngineRef.current = engine;
      setReplayState({
        currentIndex: -1,
        totalSteps: computed.length,
        currentEvent: null,
        historySoFar: [],
        currentStatus: 'IDLE',
        isPlaying: false,
      });

    } catch (err) {
      setErrorMsg(err.response?.data?.message || err.message || 'Payment not found');
      setPayment(null);
      setRawHistory([]);
      setTimedHistory([]);
    } finally {
      setLoadingPayment(false);
    }
  };

  useEffect(() => {
    handleLoadPayment();
  }, []);

  // Cleanup engines on unmount
  useEffect(() => {
    return () => {
      if (replayEngineRef.current) replayEngineRef.current.stop();
      if (simEngineRef.current) simEngineRef.current.stop();
    };
  }, []);

  /* ── Replay Handlers ────────────────────────────────────────────── */
  const handleReplayStart = () => {
    if (replayEngineRef.current) {
      replayEngineRef.current.setSpeed(replaySpeed);
      replayEngineRef.current.start();
    }
  };

  const handleReplayPause = () => {
    if (replayEngineRef.current) replayEngineRef.current.pause();
  };

  const handleReplayStop = () => {
    if (replayEngineRef.current) replayEngineRef.current.stop();
  };

  const handleReplayStepForward = () => {
    if (replayEngineRef.current) replayEngineRef.current.stepForward();
  };

  const handleReplayStepBackward = () => {
    if (replayEngineRef.current) replayEngineRef.current.stepBackward();
  };

  const handleReplaySpeedChange = (speed) => {
    setReplaySpeed(speed);
    if (replayEngineRef.current) replayEngineRef.current.setSpeed(speed);
  };

  /* ── Simulation Handlers ────────────────────────────────────────── */
  const handleStartSimulation = () => {
    setSimOutcome(null);

    const engine = new SimulationEngine(
      simConfig,
      (event, allLogs) => {
        setSimState((prev) => ({ ...prev, logs: allLogs }));
      },
      (state) => {
        setSimState(state);
      },
      (outcome) => {
        setSimOutcome(outcome);
      }
    );

    simEngineRef.current = engine;
    engine.start();
  };

  const handleStopSimulation = () => {
    if (simEngineRef.current) {
      simEngineRef.current.stop();
    }
  };

  return (
    <Box className="simulator-container">
      {/* Header */}
      <Box sx={{ mb: 1 }}>
        <Typography variant="h4" sx={{ fontWeight: 800, mb: 0.5 }}>
          Payment Lifecycle & Replay Simulator
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Analyze historical audit timelines, replay events step-by-step, or run failure injection sandbox simulations.
        </Typography>
      </Box>

      {/* Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs
          value={activeTab}
          onChange={(e, v) => setActiveTab(v)}
          sx={{
            '& .MuiTab-root': { textTransform: 'none', fontWeight: 700, fontSize: '0.95rem' },
          }}
        >
          <Tab icon={<TimelineIcon />} iconPosition="start" label="Visual Timeline" />
          <Tab icon={<History />} iconPosition="start" label="Replay Payment" />
          <Tab icon={<BugReport />} iconPosition="start" label="Simulation Sandbox Mode" />
        </Tabs>
      </Box>

      {/* TAB 0 & 1: Payment Loader Bar */}
      {activeTab !== 2 && (
        <Card>
          <CardContent sx={{ py: 2 }}>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} sm={6} md={4}>
                <TextField
                  label="Payment ID"
                  size="small"
                  value={paymentIdInput}
                  onChange={(e) => setPaymentIdInput(e.target.value)}
                  fullWidth
                  placeholder="Enter Payment ID (e.g. 1)"
                />
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Button
                  variant="contained"
                  onClick={handleLoadPayment}
                  disabled={loadingPayment}
                  startIcon={<Refresh />}
                >
                  {loadingPayment ? 'Loading...' : 'Load Audit Trail'}
                </Button>
              </Grid>
            </Grid>

            {errorMsg && <Alert severity="error" sx={{ mt: 2 }}>{errorMsg}</Alert>}

            {payment && (
              <Box sx={{ mt: 2, display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
                <Typography variant="body2" sx={{ fontWeight: 700 }}>
                  Payment #{payment.id}
                </Typography>
                <Chip label={`${payment.amount} ${payment.currency}`} size="small" variant="outlined" />
                <Typography variant="caption" color="text.secondary">
                  Source: {payment.sourceAccount} → Dest: {payment.destinationAccount}
                </Typography>
                <StatusBadge status={payment.status} />
              </Box>
            )}
          </CardContent>
        </Card>
      )}

      {/* ═════════════════════════════════════════════════════════════
         TAB 0: VISUAL TIMELINE
         ═════════════════════════════════════════════════════════════ */}
      {activeTab === 0 && (
        <Grid container spacing={3}>
          <Grid item xs={12} md={8}>
            <Card>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                  Historical State Transition Timeline
                </Typography>

                {timedHistory.length === 0 ? (
                  <Typography variant="body2" color="text.secondary">
                    No transition history available for this payment.
                  </Typography>
                ) : (
                  <Box className="timeline-tree">
                    {timedHistory.map((item, idx) => (
                      <Box key={idx} className="timeline-node-item active">
                        <Box
                          className="timeline-dot"
                          sx={{
                            background: item.newStatus === 'COMPLETED' ? '#10B981' : item.newStatus === 'FAILED' ? '#EF4444' : '#FF003C',
                            color: '#FFF',
                          }}
                        >
                          {idx + 1}
                        </Box>
                        <Box className="timeline-content-card">
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                              {item.oldStatus ? `${item.oldStatus} → ${item.newStatus}` : item.newStatus}
                            </Typography>
                            <StatusBadge status={item.newStatus} />
                          </Box>

                          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
                            Timestamp: {new Date(item.timestamp).toLocaleString()} {item.triggeredBy ? `(Triggered by ${item.triggeredBy})` : ''}
                          </Typography>

                          {idx > 0 && (
                            <Box className="timeline-duration-badge">
                              <Schedule sx={{ fontSize: 12 }} />
                              Transition Duration: {item.formattedDuration}
                            </Box>
                          )}

                          {item.errorCode && (
                            <Alert severity="error" sx={{ mt: 1.5, py: 0, fontSize: '0.8rem' }}>
                              [{item.errorCode}] {item.errorMessage}
                            </Alert>
                          )}
                        </Box>
                      </Box>
                    ))}
                  </Box>
                )}
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                  Timeline Summary
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  This timeline shows the exact sequence of state transitions recorded by the audit system.
                </Typography>

                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                  <Box>
                    <Typography variant="caption" color="text.secondary">Total Transitions</Typography>
                    <Typography variant="body1" sx={{ fontWeight: 700 }}>{timedHistory.length}</Typography>
                  </Box>
                  <Box>
                    <Typography variant="caption" color="text.secondary">Initial State</Typography>
                    <Typography variant="body1" sx={{ fontWeight: 700 }}>{timedHistory[0]?.newStatus || 'CREATED'}</Typography>
                  </Box>
                  <Box>
                    <Typography variant="caption" color="text.secondary">Final Outcome State</Typography>
                    <Typography variant="body1" sx={{ fontWeight: 700 }}>{payment?.status || 'N/A'}</Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* ═════════════════════════════════════════════════════════════
         TAB 1: REPLAY PAYMENT
         ═════════════════════════════════════════════════════════════ */}
      {activeTab === 1 && (
        <Grid container spacing={3}>
          <Grid item xs={12}>
            {/* Playback Controls */}
            <Box className="simulator-control-bar" sx={{ bgcolor: 'background.paper' }}>
              <Box className="simulator-btn-group">
                {!replayState.isPlaying ? (
                  <button className="simulator-btn simulator-btn-primary" onClick={handleReplayStart} disabled={timedHistory.length === 0}>
                    <PlayArrow fontSize="small" /> Play Replay
                  </button>
                ) : (
                  <button className="simulator-btn simulator-btn-secondary" onClick={handleReplayPause}>
                    <Pause fontSize="small" /> Pause
                  </button>
                )}

                <button className="simulator-btn simulator-btn-secondary" onClick={handleReplayStop} disabled={replayState.currentIndex === -1}>
                  <Stop fontSize="small" /> Stop
                </button>

                <button className="simulator-btn simulator-btn-secondary" onClick={handleReplayStepBackward} disabled={replayState.currentIndex <= 0}>
                  <SkipPrevious fontSize="small" /> Step Prev
                </button>

                <button className="simulator-btn simulator-btn-secondary" onClick={handleReplayStepForward} disabled={replayState.currentIndex >= timedHistory.length - 1}>
                  <SkipNext fontSize="small" /> Step Next
                </button>
              </Box>

              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>Speed:</Typography>
                {[0.5, 1, 2, 5].map((spd) => (
                  <Chip
                    key={spd}
                    label={`${spd}x`}
                    size="small"
                    color={replaySpeed === spd ? 'primary' : 'default'}
                    onClick={() => handleReplaySpeedChange(spd)}
                    clickable
                  />
                ))}
              </Box>
            </Box>
          </Grid>

          <Grid item xs={12} md={8}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                  <Typography variant="h6" sx={{ fontWeight: 700 }}>
                    Replay Progress (Step {replayState.currentIndex + 1} of {timedHistory.length})
                  </Typography>
                  {replayState.currentStatus !== 'IDLE' && (
                    <StatusBadge status={replayState.currentStatus} />
                  )}
                </Box>

                <LinearProgress
                  variant="determinate"
                  value={timedHistory.length > 0 ? ((replayState.currentIndex + 1) / timedHistory.length) * 100 : 0}
                  sx={{ height: 8, borderRadius: 4, mb: 3 }}
                />

                {/* Animated Replay Steps */}
                <Box className="timeline-tree">
                  {timedHistory.slice(0, replayState.currentIndex + 1).map((item, idx) => (
                    <Box key={idx} className={`timeline-node-item ${idx === replayState.currentIndex ? 'active' : ''}`}>
                      <Box className="timeline-dot" sx={{ background: '#FF003C', color: '#FFF' }}>
                        {idx + 1}
                      </Box>
                      <Box className="timeline-content-card">
                        <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                          {item.oldStatus ? `${item.oldStatus} → ${item.newStatus}` : item.newStatus}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {new Date(item.timestamp).toLocaleString()}
                        </Typography>
                        {item.formattedDuration && (
                          <Box className="timeline-duration-badge">
                            Duration: {item.formattedDuration}
                          </Box>
                        )}
                      </Box>
                    </Box>
                  ))}
                </Box>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
                  Replay Safety Guarantee
                </Typography>
                <Alert severity="info">
                  Replay mode parses stored audit logs. It simulates playback in memory and <strong>never executes backend state changes or API updates</strong>.
                </Alert>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* ═════════════════════════════════════════════════════════════
         TAB 2: SIMULATION SANDBOX MODE
         ═════════════════════════════════════════════════════════════ */}
      {activeTab === 2 && (
        <Grid container spacing={3}>
          {/* Config Form */}
          <Grid item xs={12} lg={4}>
            <Card>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                  Simulation Controls
                </Typography>

                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  <FormControl fullWidth size="small">
                    <InputLabel>Injected Failure Mode</InputLabel>
                    <Select
                      value={simConfig.failureType}
                      label="Injected Failure Mode"
                      onChange={(e) => setSimConfig({ ...simConfig, failureType: e.target.value })}
                      disabled={simState.isRunning}
                    >
                      {Object.values(FAILURE_TYPES).map((f) => (
                        <MenuItem key={f.id} value={f.id}>
                          {f.label}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>

                  <TextField
                    label="Max Retry Attempts"
                    type="number"
                    size="small"
                    value={simConfig.maxRetries}
                    onChange={(e) => setSimConfig({ ...simConfig, maxRetries: parseInt(e.target.value, 10) || 0 })}
                    disabled={simState.isRunning}
                  />

                  <TextField
                    label="Initial Retry Delay (ms)"
                    type="number"
                    size="small"
                    value={simConfig.retryDelayMs}
                    onChange={(e) => setSimConfig({ ...simConfig, retryDelayMs: parseInt(e.target.value, 10) || 500 })}
                    disabled={simState.isRunning}
                  />

                  <FormControlLabel
                    control={
                      <Switch
                        checked={simConfig.exponentialBackoff}
                        onChange={(e) => setSimConfig({ ...simConfig, exponentialBackoff: e.target.checked })}
                        disabled={simState.isRunning}
                      />
                    }
                    label="Exponential Backoff"
                  />

                  <Box>
                    <Typography variant="caption" color="text.secondary">Simulation Speed: {simConfig.speed}x</Typography>
                    <Slider
                      value={simConfig.speed}
                      min={0.5}
                      max={5}
                      step={0.5}
                      onChange={(e, v) => setSimConfig({ ...simConfig, speed: v })}
                      disabled={simState.isRunning}
                    />
                  </Box>

                  {!simState.isRunning ? (
                    <Button variant="contained" color="primary" onClick={handleStartSimulation} startIcon={<PlayArrow />}>
                      Run Sandbox Simulation
                    </Button>
                  ) : (
                    <Button variant="contained" color="error" onClick={handleStopSimulation} startIcon={<Stop />}>
                      Stop Simulation
                    </Button>
                  )}
                </Box>
              </CardContent>
            </Card>
          </Grid>

          {/* Real-time Sandbox Display */}
          <Grid item xs={12} lg={8}>
            <Card sx={{ mb: 3 }}>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Typography variant="h6" sx={{ fontWeight: 700 }}>
                    Live Lifecycle Simulation State
                  </Typography>
                  <StatusBadge status={simState.status} />
                </Box>

                {/* Progress bar */}
                <LinearProgress
                  variant="determinate"
                  value={
                    simState.status === 'CREATED' ? 25 :
                    simState.status === 'VALIDATED' ? 50 :
                    simState.status === 'SENT' ? 75 :
                    simState.status === 'COMPLETED' || simState.status === 'FAILED' ? 100 : 0
                  }
                  color={simState.status === 'FAILED' ? 'error' : 'primary'}
                  sx={{ height: 8, borderRadius: 4, mb: 3 }}
                />

                {/* Simulation Logs Stream */}
                <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                  Execution Event Stream
                </Typography>
                <Box className="simulator-log-container">
                  {simState.logs.length === 0 ? (
                    <Typography variant="caption" color="text.secondary">
                      Press "Run Sandbox Simulation" to start state execution...
                    </Typography>
                  ) : (
                    simState.logs.map((log) => (
                      <Box key={log.id} className="simulator-log-row">
                        <span className="simulator-log-time">{new Date(log.timestamp).toLocaleTimeString()}</span>
                        <span className={`simulator-log-badge ${log.level}`}>{log.stage}</span>
                        <span className="simulator-log-msg">{log.message}</span>
                      </Box>
                    ))
                  )}
                  <div ref={logEndRef} />
                </Box>
              </CardContent>
            </Card>

            {/* Simulation Final Outcome */}
            {simOutcome && (
              <Card sx={{ border: simOutcome.success ? '1px solid #10B981' : '1px solid #EF4444' }}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1 }}>
                    {simOutcome.success ? (
                      <CheckCircle sx={{ color: '#10B981', fontSize: 32 }} />
                    ) : (
                      <ErrorIcon sx={{ color: '#EF4444', fontSize: 32 }} />
                    )}
                    <Typography variant="h6" sx={{ fontWeight: 700 }}>
                      Simulation Outcome: {simOutcome.success ? 'SUCCESS' : 'FAILED'}
                    </Typography>
                  </Box>

                  <Grid container spacing={2} sx={{ mt: 1 }}>
                    <Grid item xs={6} sm={3}>
                      <Typography variant="caption" color="text.secondary">Total Execution Duration</Typography>
                      <Typography variant="body1" sx={{ fontWeight: 700 }}>{simOutcome.formattedTotalDuration}</Typography>
                    </Grid>
                    <Grid item xs={6} sm={3}>
                      <Typography variant="caption" color="text.secondary">Retries Attempted</Typography>
                      <Typography variant="body1" sx={{ fontWeight: 700 }}>{simOutcome.retryAttempts}</Typography>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <Typography variant="caption" color="text.secondary">Final Error Status</Typography>
                      <Typography variant="body1" sx={{ fontWeight: 700, color: simOutcome.success ? '#10B981' : '#EF4444' }}>
                        {simOutcome.errorCode ? `[${simOutcome.errorCode}] ${simOutcome.errorMessage}` : 'None (Completed Cleanly)'}
                      </Typography>
                    </Grid>
                  </Grid>
                </CardContent>
              </Card>
            )}
          </Grid>
        </Grid>
      )}
    </Box>
  );
}
