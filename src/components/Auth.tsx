import { useState } from 'react';
import { supabase, isSupabaseConfigured } from '../services/supabaseClient';
import { LogOut, ShieldAlert, CheckCircle } from 'lucide-react';
import { useToast } from './Toast';

interface AuthProps {
  user: any;
  onLogout: () => void;
}

export default function Auth({ user, onLogout }: AuthProps) {
  const toast = useToast();
  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = async () => {
    if (!isSupabaseConfigured || !supabase) {
      toast.error('Supabase bağlantısı henüz yapılandırılmadı.');
      return;
    }

    setIsLoading(true);
    try {
      const { error } = await supabase.auth.signInWithOAuth({
        provider: 'google',
        options: {
          redirectTo: window.location.origin
        }
      });
      if (error) throw error;
    } catch (err: any) {
      console.error('Login error:', err.message);
      toast.error(`Giriş başlatılamadı: ${err.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = async () => {
    if (!supabase) return;
    try {
      const { error } = await supabase.auth.signOut();
      if (error) throw error;
      onLogout();
      toast.success('Oturum başarıyla kapatıldı.');
    } catch (err: any) {
      toast.error(`Oturum kapatılamadı: ${err.message}`);
    }
  };

  // Google Icon SVG
  const GoogleIcon = () => (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ marginRight: '8px' }}>
      <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
      <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
      <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z" fill="#FBBC05"/>
      <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z" fill="#EA4335"/>
    </svg>
  );

  if (!isSupabaseConfigured) {
    return (
      <div 
        className="glass-panel" 
        style={{ 
          padding: '16px 20px', 
          border: '1px solid rgba(234, 179, 8, 0.25)', 
          background: 'rgba(234, 179, 8, 0.02)',
          display: 'flex',
          flexDirection: 'column',
          gap: '12px'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#eab308' }}>
          <ShieldAlert size={18} />
          <strong style={{ fontSize: '0.85rem' }}>Bulut Senkronizasyonu Pasif (Local Mode)</strong>
        </div>
        <p style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', lineHeight: '1.4', margin: 0 }}>
          Bulut yedekleme ve Google ile Giriş için Supabase bilgilerini girmeniz gerekir. 
          Vercel panelinde veya projenin <code>.env</code> dosyasında <code>VITE_SUPABASE_URL</code> ve <code>VITE_SUPABASE_ANON_KEY</code> tanımlandığında bu özellik otomatik aktif olacaktır.
        </p>
      </div>
    );
  }

  if (user) {
    const avatarUrl = user.user_metadata?.avatar_url;
    const name = user.user_metadata?.full_name || user.email;

    return (
      <div 
        className="glass-panel" 
        style={{ 
          padding: '16px 20px', 
          border: '1px solid rgba(34, 197, 94, 0.25)', 
          background: 'rgba(34, 197, 94, 0.02)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '16px'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {avatarUrl ? (
            <img 
              src={avatarUrl} 
              alt="Avatar" 
              style={{ width: '40px', height: '40px', borderRadius: '50%', border: '2px solid #22c55e' }} 
            />
          ) : (
            <div style={{ width: '40px', height: '40px', borderRadius: '50%', background: '#22c55e', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold' }}>
              {(name || 'U')[0].toUpperCase()}
            </div>
          )}
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <strong style={{ fontSize: '0.9rem', color: '#fff' }}>{name}</strong>
              <span style={{ display: 'inline-flex' }} title="Bulut Bağlantısı Aktif">
                <CheckCircle size={14} color="#22c55e" />
              </span>
            </div>
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)' }}>
              {user.email} (Bulut Senkronize)
            </span>
          </div>
        </div>

        <button 
          onClick={handleLogout} 
          className="btn btn-secondary" 
          style={{ padding: '8px 16px', borderRadius: '10px', fontSize: '0.8rem', borderColor: 'rgba(239, 68, 68, 0.25)', color: '#ef4444' }}
        >
          <LogOut size={14} style={{ marginRight: '6px' }} />
          Oturumu Kapat
        </button>
      </div>
    );
  }

  return (
    <div 
      className="glass-panel" 
      style={{ 
        padding: '16px 20px', 
        border: '1px solid var(--color-border)', 
        background: 'rgba(255, 255, 255, 0.01)',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: '16px'
      }}
    >
      <div>
        <strong style={{ fontSize: '0.9rem', color: '#fff', display: 'block' }}>Bulut Yedekleme & Senkronizasyon</strong>
        <span style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)' }}>
          Verilerinizi güvenceye almak için Google hesabınızla giriş yapın
        </span>
      </div>

      <button 
        onClick={handleLogin} 
        disabled={isLoading}
        className="btn btn-secondary" 
        style={{ 
          padding: '10px 20px', 
          borderRadius: '12px', 
          fontSize: '0.85rem', 
          background: '#fff', 
          color: '#000', 
          border: 'none',
          fontWeight: '700'
        }}
      >
        <GoogleIcon />
        {isLoading ? 'Giriş yapılıyor...' : 'Google ile Giriş Yap'}
      </button>
    </div>
  );
}
