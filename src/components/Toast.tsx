import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import type { ReactNode } from 'react';
import { CheckCircle, AlertCircle, Info, AlertTriangle, X } from 'lucide-react';

type ToastType = 'success' | 'error' | 'info' | 'warning';

interface ToastData {
  id: string;
  message: string;
  type: ToastType;
}

interface ToastContextValue {
  success: (message: string) => void;
  error: (message: string) => void;
  info: (message: string) => void;
  warning: (message: string) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be inside ToastProvider');
  return ctx;
}

const CONFIGS: Record<ToastType, { icon: React.ElementType; color: string; border: string }> = {
  success: { icon: CheckCircle,   color: '#22c55e', border: 'rgba(34,197,94,0.35)' },
  error:   { icon: AlertCircle,   color: '#ef4444', border: 'rgba(239,68,68,0.35)' },
  info:    { icon: Info,          color: '#06b6d4', border: 'rgba(6,182,212,0.35)' },
  warning: { icon: AlertTriangle, color: '#eab308', border: 'rgba(234,179,8,0.35)' },
};

function ToastItem({ item, onRemove }: { item: ToastData; onRemove: () => void }) {
  const [show, setShow] = useState(false);
  const cfg = CONFIGS[item.type];
  const Icon = cfg.icon;

  useEffect(() => {
    const t1 = setTimeout(() => setShow(true), 10);
    const t2 = setTimeout(() => { setShow(false); setTimeout(onRemove, 350); }, 3800);
    return () => { clearTimeout(t1); clearTimeout(t2); };
  }, [onRemove]);

  const handleClose = () => { setShow(false); setTimeout(onRemove, 350); };

  return (
    <div
      role="alert"
      style={{
        display: 'flex',
        alignItems: 'flex-start',
        gap: '12px',
        padding: '14px 16px',
        background: 'linear-gradient(135deg, #0d1526, #111827)',
        border: `1px solid ${cfg.border}`,
        borderLeft: `4px solid ${cfg.color}`,
        borderRadius: '14px',
        boxShadow: '0 8px 32px rgba(0,0,0,0.6), 0 0 0 1px rgba(255,255,255,0.03)',
        minWidth: '280px',
        maxWidth: '380px',
        transform: show ? 'translateX(0) scale(1)' : 'translateX(110%) scale(0.95)',
        opacity: show ? 1 : 0,
        transition: 'all 0.35s cubic-bezier(0.16, 1, 0.3, 1)',
        pointerEvents: 'all',
      }}
    >
      <Icon size={18} color={cfg.color} style={{ flexShrink: 0, marginTop: '1px' }} />
      <span style={{ flex: 1, fontSize: '0.875rem', fontWeight: '500', color: '#f1f5f9', lineHeight: 1.5 }}>
        {item.message}
      </span>
      <button
        onClick={handleClose}
        aria-label="Kapat"
        style={{
          background: 'none', border: 'none', color: '#64748b', cursor: 'pointer',
          padding: '2px', display: 'flex', borderRadius: '4px', flexShrink: 0,
          transition: 'color 0.15s',
        }}
        onMouseEnter={e => ((e.currentTarget as HTMLButtonElement).style.color = '#94a3b8')}
        onMouseLeave={e => ((e.currentTarget as HTMLButtonElement).style.color = '#64748b')}
      >
        <X size={14} />
      </button>
    </div>
  );
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastData[]>([]);

  const remove = useCallback((id: string) => setToasts(p => p.filter(t => t.id !== id)), []);

  const add = useCallback((message: string, type: ToastType) => {
    const id = `toast_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;
    setToasts(p => [...p.slice(-4), { id, message, type }]);
  }, []);

  const success = useCallback((m: string) => add(m, 'success'), [add]);
  const error   = useCallback((m: string) => add(m, 'error'),   [add]);
  const info    = useCallback((m: string) => add(m, 'info'),    [add]);
  const warning = useCallback((m: string) => add(m, 'warning'), [add]);

  return (
    <ToastContext.Provider value={{ success, error, info, warning }}>
      {children}
      <div
        aria-live="polite"
        aria-atomic="false"
        style={{
          position: 'fixed',
          bottom: '88px',
          right: '20px',
          zIndex: 9999,
          display: 'flex',
          flexDirection: 'column',
          gap: '10px',
          alignItems: 'flex-end',
          pointerEvents: 'none',
        }}
      >
        {toasts.map(t => <ToastItem key={t.id} item={t} onRemove={() => remove(t.id)} />)}
      </div>
    </ToastContext.Provider>
  );
}
