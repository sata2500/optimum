import { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import {
  Play, Pause, RotateCcw, SkipForward,
  Settings as SettingsIcon, X, Check,
  BrainCircuit, Coffee, Moon
} from 'lucide-react';
import { pomodoroService } from '../services/pomodoroService';
import type { PomodoroSettings, PomodoroSessionRecord } from '../services/pomodoroService';
import { useToast } from './Toast';

type Phase = 'work' | 'short-break' | 'long-break';

const PHASE = {
  work: {
    label: 'Odaklanma',
    emoji: '🧠',
    icon: BrainCircuit,
    gradId: 'pom-work',
    c1: '#ef4444',
    c2: '#f97316',
    color: '#f97316',
    bg: 'rgba(249,115,22,0.08)',
  },
  'short-break': {
    label: 'Kısa Mola',
    emoji: '☕',
    icon: Coffee,
    gradId: 'pom-break',
    c1: '#06b6d4',
    c2: '#8b5cf6',
    color: '#06b6d4',
    bg: 'rgba(6,182,212,0.08)',
  },
  'long-break': {
    label: 'Uzun Mola',
    emoji: '🌙',
    icon: Moon,
    gradId: 'pom-long',
    c1: '#8b5cf6',
    c2: '#6366f1',
    color: '#8b5cf6',
    bg: 'rgba(139,92,246,0.08)',
  },
} as const;

const fmt = (s: number) =>
  `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;

const notify = async (title: string, body: string) => {
  try {
    if ('Notification' in window && Notification.permission === 'granted') {
      let sent = false;
      if ('serviceWorker' in navigator && navigator.serviceWorker.controller) {
        try {
          const reg = await navigator.serviceWorker.ready;
          await reg.showNotification(title, { body });
          sent = true;
        } catch (swErr) {
          console.warn('SW pomodoro notification failed:', swErr);
        }
      }
      if (!sent) {
        new Notification(title, { body });
      }
    }
  } catch { /* silent */ }
};

const RING_R = 100;
const CIRC = 2 * Math.PI * RING_R;

interface SliderRowProps {
  label: string;
  value: number;
  min: number;
  max: number;
  unit: string;
  color: string;
  onChange: (v: number) => void;
}

function SliderRow({ label, value, min, max, unit, color, onChange }: SliderRowProps) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <label style={{ fontSize: '0.82rem', fontWeight: '600', color: 'var(--color-text-secondary)' }}>
          {label}
        </label>
        <span style={{
          fontSize: '0.88rem', fontWeight: '800', fontFamily: 'Outfit',
          color, background: `${color}18`,
          padding: '2px 10px', borderRadius: '12px',
          border: `1px solid ${color}30`,
        }}>
          {value} {unit}
        </span>
      </div>
      <input
        type="range"
        className="range-input"
        min={min}
        max={max}
        value={value}
        onChange={e => onChange(Number(e.target.value))}
      />
    </div>
  );
}

export default function Pomodoro() {
  const toast = useToast();

  const [settings, setSettings] = useState<PomodoroSettings>(() => pomodoroService.getSettings());
  const [draft, setDraft] = useState<PomodoroSettings>(() => pomodoroService.getSettings());
  const [phase, setPhase] = useState<Phase>('work');
  const [timeLeft, setTimeLeft] = useState(() => pomodoroService.getSettings().workMinutes * 60);
  const [isRunning, setIsRunning] = useState(false);
  const [completedCycles, setCompletedCycles] = useState(0);
  const [showSettings, setShowSettings] = useState(false);
  const [todayStat, setTodayStat] = useState<PomodoroSessionRecord | null>(
    () => pomodoroService.getSessionToday()
  );

  // Refs to avoid stale closures in interval/timeout callbacks
  const stateRef = useRef({ phase, settings, completedCycles });
  useEffect(() => { stateRef.current = { phase, settings, completedCycles }; }, [phase, settings, completedCycles]);

  const totalTime = useMemo(() => {
    if (phase === 'work') return settings.workMinutes * 60;
    if (phase === 'short-break') return settings.shortBreakMinutes * 60;
    return settings.longBreakMinutes * 60;
  }, [phase, settings]);

  const progress = totalTime > 0 ? (totalTime - timeLeft) / totalTime : 0;
  const cfg = PHASE[phase];

  // ── Phase End Logic ─────────────────────────────────────────────
  const handlePhaseEnd = useCallback(() => {
    const { phase: p, settings: s, completedCycles: cc } = stateRef.current;

    if (p === 'work') {
      const newCC = cc + 1;
      pomodoroService.recordCompletedPomodoro(s.workMinutes);
      setCompletedCycles(newCC);
      setTodayStat(pomodoroService.getSessionToday());

      const goLong = newCC % s.cyclesBeforeLong === 0;

      if (goLong) {
        notify('🎉 Pomodoro Tamamlandı!', 'Uzun mola zamanı. İyi dinlenmeler!');
        toast.success(`🎉 ${s.cyclesBeforeLong} döngü tamamlandı! Uzun mola hak ettiniz.`);
        setPhase('long-break');
        setTimeLeft(s.longBreakMinutes * 60);
        if (s.autoStartBreak) setTimeout(() => setIsRunning(true), 200);
      } else {
        notify('✓ Pomodoro Tamamlandı!', 'Kısa mola zamanı. Nefes alın!');
        toast.info('✓ Pomodoro bitti. Kısa mola zamanı!');
        setPhase('short-break');
        setTimeLeft(s.shortBreakMinutes * 60);
        if (s.autoStartBreak) setTimeout(() => setIsRunning(true), 200);
      }
    } else {
      notify('⏰ Mola Bitti!', 'Odaklanma zamanı. Haydi!');
      toast.info('⏰ Mola bitti. Odaklanma zamanı!');
      setPhase('work');
      setTimeLeft(s.workMinutes * 60);
      if (s.autoStartWork) setTimeout(() => setIsRunning(true), 200);
    }
  }, [toast]);

  const handlePhaseEndRef = useRef(handlePhaseEnd);
  useEffect(() => { handlePhaseEndRef.current = handlePhaseEnd; }, [handlePhaseEnd]);

  // ── Timer Interval ───────────────────────────────────────────────
  useEffect(() => {
    if (!isRunning) return;
    const id = setInterval(() => {
      setTimeLeft(prev => {
        if (prev <= 1) {
          clearInterval(id);
          setIsRunning(false);
          setTimeout(() => handlePhaseEndRef.current(), 50);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(id);
  }, [isRunning]);

  // ── Controls ─────────────────────────────────────────────────────
  const handlePlayPause = () => setIsRunning(p => !p);

  const handleReset = () => {
    setIsRunning(false);
    const s = stateRef.current.settings;
    if (phase === 'work') setTimeLeft(s.workMinutes * 60);
    else if (phase === 'short-break') setTimeLeft(s.shortBreakMinutes * 60);
    else setTimeLeft(s.longBreakMinutes * 60);
  };

  const handleSkip = () => {
    setIsRunning(false);
    setTimeout(() => handlePhaseEndRef.current(), 50);
  };

  const handleSaveSettings = () => {
    pomodoroService.saveSettings(draft);
    setSettings(draft);
    setIsRunning(false);
    setPhase('work');
    setTimeLeft(draft.workMinutes * 60);
    setCompletedCycles(0);
    setShowSettings(false);
    toast.success('Pomodoro ayarları kaydedildi.');
  };

  // ── Cycle Dots ──────────────────────────────────────────────────
  const dotsCount = settings.cyclesBeforeLong;
  const filledDots = completedCycles % dotsCount;
  const isCurrentlyInWork = phase === 'work';
  const longBreaksDone = Math.floor(completedCycles / dotsCount);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px', maxWidth: '500px', margin: '0 auto' }}>

      {/* Header */}
      <div>
        <h1 style={{ fontSize: '1.8rem', fontWeight: '800', fontFamily: 'Outfit', color: '#fff' }}>
          Pomodoro Zamanlayıcı
        </h1>
        <p style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', marginTop: '4px' }}>
          Odaklanma ve mola döngülerinizi yönetin.
        </p>
      </div>

      {/* Main Timer Card */}
      <div
        className="glass-panel"
        style={{
          padding: '32px 24px',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: '24px',
          borderColor: `${cfg.color}30`,
          background: cfg.bg,
          transition: 'background 0.5s ease, border-color 0.5s ease',
        }}
      >
        {/* Phase label */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <span style={{ fontSize: '1.5rem' }}>{cfg.emoji}</span>
          <span style={{
            fontSize: '1rem', fontWeight: '700', color: cfg.color,
            fontFamily: 'Outfit', letterSpacing: '0.05em',
          }}>
            {cfg.label}
          </span>
          <span style={{
            fontSize: '0.78rem', color: 'var(--color-text-muted)', fontWeight: '600',
            background: 'rgba(255,255,255,0.05)',
            padding: '1px 8px', borderRadius: '10px',
          }}>
            {phase === 'work' ? `${filledDots + 1}/${dotsCount}` : 'Mola'}
          </span>
        </div>

        {/* Ring Timer */}
        <div style={{ position: 'relative', width: '260px', height: '260px', flexShrink: 0 }}>
          <svg width="260" height="260" viewBox="0 0 260 260">
            <defs>
              {Object.entries(PHASE).map(([key, p]) => (
                <linearGradient key={key} id={p.gradId} x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stopColor={p.c1}/>
                  <stop offset="100%" stopColor={p.c2}/>
                </linearGradient>
              ))}
              <filter id="pom-glow">
                <feGaussianBlur stdDeviation="6" result="blur"/>
                <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
              </filter>
            </defs>

            {/* Background track */}
            <circle cx="130" cy="130" r={RING_R} fill="none"
              stroke="rgba(255,255,255,0.06)" strokeWidth="18"
              transform="rotate(-90 130 130)"
            />

            {/* Progress ring */}
            <circle cx="130" cy="130" r={RING_R} fill="none"
              stroke={`url(#${cfg.gradId})`}
              strokeWidth="18"
              strokeLinecap="round"
              strokeDasharray={CIRC}
              strokeDashoffset={CIRC * (1 - progress)}
              transform="rotate(-90 130 130)"
              filter="url(#pom-glow)"
              style={{ transition: 'stroke-dashoffset 0.9s linear, stroke 0.5s ease' }}
            />
          </svg>

          {/* Center text */}
          <div style={{
            position: 'absolute', inset: 0,
            display: 'flex', flexDirection: 'column',
            alignItems: 'center', justifyContent: 'center',
            gap: '4px', pointerEvents: 'none',
          }}>
            <span style={{
              fontSize: '3.4rem', fontWeight: '800',
              fontFamily: 'Outfit, monospace',
              color: '#fff', lineHeight: 1,
              letterSpacing: '-0.02em',
              textShadow: `0 0 30px ${cfg.color}50`,
            }}>
              {fmt(timeLeft)}
            </span>
            <span style={{
              fontSize: '0.72rem', color: 'var(--color-text-muted)',
              fontWeight: '700', textTransform: 'uppercase', letterSpacing: '0.08em',
            }}>
              {isRunning ? 'devam ediyor' : timeLeft === 0 ? 'tamamlandı ✓' : 'hazır'}
            </span>
          </div>
        </div>

        {/* Cycle dots */}
        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          {Array.from({ length: dotsCount }, (_, i) => {
            const isFilled = i < filledDots;
            const isCurrent = isCurrentlyInWork && i === filledDots && isRunning;
            return (
              <div key={i} style={{
                width: isCurrent ? '16px' : '10px',
                height: isCurrent ? '16px' : '10px',
                borderRadius: '50%',
                background: isFilled ? cfg.color : 'rgba(255,255,255,0.08)',
                border: isCurrent ? `2px solid ${cfg.color}` : '2px solid transparent',
                boxShadow: isFilled ? `0 0 10px ${cfg.color}60` : 'none',
                transition: 'all 0.35s ease',
              }} />
            );
          })}
          {longBreaksDone > 0 && (
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginLeft: '6px' }}>
              × {longBreaksDone} uzun mola
            </span>
          )}
        </div>

        {/* Control Buttons */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
          {/* Reset */}
          <button
            onClick={handleReset}
            aria-label="Sıfırla"
            style={{
              width: '52px', height: '52px', borderRadius: '50%',
              background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.1)',
              color: 'var(--color-text-secondary)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              cursor: 'pointer', transition: 'all 0.2s ease',
            }}
            onMouseEnter={e => { e.currentTarget.style.background = 'rgba(255,255,255,0.12)'; e.currentTarget.style.color = '#fff'; }}
            onMouseLeave={e => { e.currentTarget.style.background = 'rgba(255,255,255,0.06)'; e.currentTarget.style.color = 'var(--color-text-secondary)'; }}
          >
            <RotateCcw size={20} />
          </button>

          {/* Play / Pause */}
          <button
            onClick={handlePlayPause}
            aria-label={isRunning ? 'Duraklat' : 'Başlat'}
            style={{
              width: '78px', height: '78px', borderRadius: '50%',
              background: `linear-gradient(135deg, ${cfg.c1}, ${cfg.c2})`,
              border: 'none', color: '#fff',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              cursor: 'pointer',
              boxShadow: `0 0 28px ${cfg.color}50, 0 6px 20px rgba(0,0,0,0.4)`,
              transition: 'all 0.25s ease',
            }}
            onMouseEnter={e => { e.currentTarget.style.transform = 'scale(1.07)'; }}
            onMouseLeave={e => { e.currentTarget.style.transform = 'scale(1)'; }}
          >
            {isRunning
              ? <Pause size={30} />
              : <Play size={30} style={{ marginLeft: '4px' }} />
            }
          </button>

          {/* Skip */}
          <button
            onClick={handleSkip}
            aria-label="Sonraki faza atla"
            style={{
              width: '52px', height: '52px', borderRadius: '50%',
              background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.1)',
              color: 'var(--color-text-secondary)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              cursor: 'pointer', transition: 'all 0.2s ease',
            }}
            onMouseEnter={e => { e.currentTarget.style.background = 'rgba(255,255,255,0.12)'; e.currentTarget.style.color = '#fff'; }}
            onMouseLeave={e => { e.currentTarget.style.background = 'rgba(255,255,255,0.06)'; e.currentTarget.style.color = 'var(--color-text-secondary)'; }}
          >
            <SkipForward size={20} />
          </button>
        </div>

        {/* Settings Toggle */}
        <button
          onClick={() => { setShowSettings(p => !p); setDraft(settings); }}
          style={{
            display: 'flex', alignItems: 'center', gap: '6px',
            padding: '6px 16px',
            background: 'none',
            border: '1px solid var(--color-border)',
            borderRadius: '20px',
            color: 'var(--color-text-secondary)',
            cursor: 'pointer',
            fontSize: '0.8rem', fontWeight: '600',
            transition: 'all 0.2s ease',
          }}
          onMouseEnter={e => { e.currentTarget.style.borderColor = cfg.color; e.currentTarget.style.color = cfg.color; }}
          onMouseLeave={e => { e.currentTarget.style.borderColor = 'var(--color-border)'; e.currentTarget.style.color = 'var(--color-text-secondary)'; }}
        >
          {showSettings ? <X size={13} /> : <SettingsIcon size={13} />}
          {showSettings ? 'Kapat' : 'Ayarlar'}
        </button>

        {/* Settings Panel */}
        {showSettings && (
          <div style={{
            width: '100%',
            borderTop: '1px solid var(--color-border)',
            paddingTop: '20px',
            display: 'flex',
            flexDirection: 'column',
            gap: '14px',
            animation: 'slide-up 0.25s ease',
          }}>
            <SliderRow label="Odaklanma Süresi"  value={draft.workMinutes}         min={5}  max={90} unit="dk"      color={cfg.color} onChange={v => setDraft(p => ({ ...p, workMinutes: v }))} />
            <SliderRow label="Kısa Mola"          value={draft.shortBreakMinutes}   min={1}  max={30} unit="dk"      color={cfg.color} onChange={v => setDraft(p => ({ ...p, shortBreakMinutes: v }))} />
            <SliderRow label="Uzun Mola"           value={draft.longBreakMinutes}    min={5}  max={60} unit="dk"      color={cfg.color} onChange={v => setDraft(p => ({ ...p, longBreakMinutes: v }))} />
            <SliderRow label="Uzun Mola Öncesi"   value={draft.cyclesBeforeLong}    min={1}  max={8}  unit="döngü"   color={cfg.color} onChange={v => setDraft(p => ({ ...p, cyclesBeforeLong: v }))} />
            <SliderRow label="Hedef (0 = ∞)"       value={draft.totalSessions}       min={0}  max={20} unit="pomodoro" color={cfg.color} onChange={v => setDraft(p => ({ ...p, totalSessions: v }))} />

            <div style={{ borderTop: '1px solid var(--color-border)', paddingTop: '12px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {([
                { key: 'autoStartBreak', label: 'Molayı Otomatik Başlat', desc: 'Pomodoro bitince mola otomatik başlar' },
                { key: 'autoStartWork',  label: 'Çalışmayı Otomatik Başlat', desc: 'Mola bitince çalışma otomatik başlar' },
              ] as const).map(({ key, label, desc }) => (
                <div key={key} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '16px' }}>
                  <div>
                    <strong style={{ fontSize: '0.83rem', color: 'var(--color-text-primary)' }}>{label}</strong>
                    <p style={{ fontSize: '0.73rem', color: 'var(--color-text-muted)', marginTop: '1px' }}>{desc}</p>
                  </div>
                  <label className="toggle-switch">
                    <input type="checkbox" checked={draft[key]} onChange={e => setDraft(p => ({ ...p, [key]: e.target.checked }))} />
                    <span className="toggle-track" />
                  </label>
                </div>
              ))}
            </div>

            <button
              className="btn btn-primary"
              onClick={handleSaveSettings}
              style={{ width: '100%', padding: '12px', borderRadius: '14px' }}
            >
              <Check size={15} />
              Kaydet & Sıfırla
            </button>
          </div>
        )}
      </div>

      {/* Today's Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '12px' }}>
        {[
          { label: 'Bugün Tamamlanan', value: todayStat?.completedPomodoros ?? 0, unit: 'pomodoro', color: cfg.color },
          { label: 'Odaklanma Süresi', value: todayStat?.totalFocusMinutes ?? 0,  unit: 'dakika',   color: '#06b6d4' },
          { label: 'Bu Oturum',        value: completedCycles,                       unit: 'döngü',    color: '#8b5cf6' },
        ].map(stat => (
          <div key={stat.label} style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--color-border)',
            borderRadius: '14px',
            padding: '16px 10px',
            textAlign: 'center',
            transition: 'all 0.3s ease',
          }}>
            <div style={{ fontSize: '1.8rem', fontWeight: '800', fontFamily: 'Outfit', color: stat.color, lineHeight: 1 }}>
              {stat.value}
            </div>
            <div style={{ fontSize: '0.68rem', color: 'var(--color-text-muted)', fontWeight: '700', textTransform: 'uppercase', letterSpacing: '0.05em', marginTop: '3px' }}>
              {stat.unit}
            </div>
            <div style={{ fontSize: '0.72rem', color: 'var(--color-text-secondary)', marginTop: '5px', lineHeight: 1.3 }}>
              {stat.label}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
