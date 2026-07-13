import { X, AlertTriangle, ShieldAlert } from 'lucide-react';

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export default function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'Onayla',
  cancelLabel = 'İptal',
  danger = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  if (!open) return null;

  const IconComp = danger ? ShieldAlert : AlertTriangle;
  const iconColor = danger ? '#ef4444' : '#eab308';

  return (
    <div
      className="modal-overlay animate-scale-in"
      style={{ zIndex: 2000 }}
      onClick={onCancel}
    >
      <div
        className="modal-content"
        style={{ maxWidth: '420px' }}
        onClick={e => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
      >
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div
              style={{
                width: '38px',
                height: '38px',
                borderRadius: '10px',
                background: danger ? 'rgba(239,68,68,0.12)' : 'rgba(234,179,8,0.12)',
                border: `1px solid ${danger ? 'rgba(239,68,68,0.25)' : 'rgba(234,179,8,0.25)'}`,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              <IconComp size={18} color={iconColor} />
            </div>
            <h3 id="confirm-dialog-title" style={{ fontSize: '1.05rem', fontFamily: 'Outfit', fontWeight: '700' }}>
              {title}
            </h3>
          </div>
          <button
            className="btn btn-secondary"
            onClick={onCancel}
            aria-label="Kapat"
            style={{ padding: '6px', borderRadius: '50%' }}
          >
            <X size={14} />
          </button>
        </div>

        <div className="modal-body">
          <p style={{ fontSize: '0.9rem', color: 'var(--color-text-secondary)', lineHeight: 1.65 }}>
            {message}
          </p>
        </div>

        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onCancel} style={{ padding: '10px 20px' }}>
            {cancelLabel}
          </button>
          <button
            className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`}
            onClick={onConfirm}
            style={{ padding: '10px 20px' }}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
