import { useMemo, useState, useEffect } from 'react';
import { supabase, isSupabaseConfigured } from '../services/supabaseClient';
import type { User } from '@supabase/supabase-js';
import { storageService } from '../services/storageService';
import type { Category, TimeLog, AppSettings } from '../services/storageService';
import { calculateStreak, isLogProductive } from '../utils/productivityUtils';

import { 
  Award, Flame, TrendingUp, User as UserIcon, Mail, Trophy, Zap, LogOut, CheckCircle, ShieldAlert, Edit3
} from 'lucide-react';

import { useToast } from './Toast';

interface ProfileProps {
  categories: Category[];
  logs: TimeLog[];
  user: User | null;
  onLogout: () => void;
  onBackToDashboard?: () => void;
  onSettingsChange?: (newSettings: AppSettings) => void;
}

export default function Profile({ categories, logs, user, onLogout, onBackToDashboard, onSettingsChange }: ProfileProps) {
  const toast = useToast();
  const settings = useMemo(() => storageService.getSettings(), []);

  const [username, setUsername] = useState(settings.userName || 'Kullanıcı');
  const [isLoggingIn, setIsLoggingIn] = useState(false);

  // Profile Edit States
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editName, setEditName] = useState(user?.user_metadata?.full_name || settings.userName || '');
  const [editAvatarUrl, setEditAvatarUrl] = useState(user?.user_metadata?.avatar_url || '');
  const [editEmail, setEditEmail] = useState(user?.email || '');

  useEffect(() => {
    if (user) {
      setEditName(user.user_metadata?.full_name || settings.userName || '');
      setEditAvatarUrl(user.user_metadata?.avatar_url || '');
      setEditEmail(user.email || '');
    }
  }, [user, settings.userName]);

  const handleLogin = async () => {
    if (!isSupabaseConfigured || !supabase) {
      toast.error('Supabase bağlantısı henüz yapılandırılmadı.');
      return;
    }

    setIsLoggingIn(true);
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
      setIsLoggingIn(false);
    }
  };

  const handleSaveUsername = () => {
    const trimmed = username.trim();
    if (!trimmed) {
      toast.error('Kullanıcı adı boş olamaz.');
      return;
    }
    const newSettings = { ...settings, userName: trimmed };
    storageService.saveSettings(newSettings);
    onSettingsChange?.(newSettings);
    toast.success('Kullanıcı adı başarıyla güncellendi.');
  };

  const handleSaveProfile = async () => {
    if (!editName.trim()) {
      toast.error('İsim alanı boş bırakılamaz.');
      return;
    }
    
    try {
      if (user && supabase) {
        const { error } = await supabase.auth.updateUser({
          data: {
            full_name: editName.trim(),
            avatar_url: editAvatarUrl.trim()
          }
        });
        if (error) throw error;
      }
      
      const newSettings = { ...settings, userName: editName.trim() };
      storageService.saveSettings(newSettings);
      onSettingsChange?.(newSettings);
      
      toast.success('Profil bilgileri başarıyla güncellendi.');
      setIsEditModalOpen(false);
    } catch (err: any) {
      console.error(err);
      toast.error(`Profil güncellenemedi: ${err.message}`);
    }
  };

  // --- 1. STREAK CALCULATION LOGIC ---
  const currentStreak = useMemo(() => {
    return calculateStreak(logs, categories, settings);
  }, [logs, categories, settings]);

  // --- 1b. TODAY PRODUCTIVITY STATS & GOALS ---
  const todayProductiveStats = useMemo(() => {
    const todayStr = new Date().toISOString().split('T')[0];
    const todayLogs = logs.filter(l => l.date === todayStr);
    const intervalMinutes = settings.intervalMinutes;

    let totalProdMins = 0;
    const categoryProdMins: { [catId: string]: number } = {};

    todayLogs.forEach(l => {
      const isProductive = isLogProductive(l, categories);
      const mins = l.durationMinutes || intervalMinutes;
      if (isProductive) {
        totalProdMins += mins;
        categoryProdMins[l.categoryId] = (categoryProdMins[l.categoryId] || 0) + mins;
      }
    });

    const totalProdHours = totalProdMins / 60;
    return {
      totalProdHours,
      categoryHours: Object.fromEntries(
        Object.entries(categoryProdMins).map(([catId, mins]) => [catId, mins / 60])
      )
    };
  }, [logs, categories, settings.intervalMinutes]);


  // --- 2. POMODORO STATISTICS ---
  const pomodoroSessions = useMemo(() => {
    try {
      const raw = localStorage.getItem('optimum_pomodoro_sessions');
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  }, []);

  const totalPomodorosCompleted = useMemo(() => {
    return pomodoroSessions.reduce((acc: number, curr: any) => acc + (curr.completedPomodoros || 0), 0);
  }, [pomodoroSessions]);

  // --- 3. BADGES & ACHIEVEMENTS ---
  const { totalHours, productiveHours, avgEfficiency, hasHighEfficiencyDay } = useMemo(() => {
    if (logs.length === 0) return { totalHours: 0, productiveHours: 0, avgEfficiency: 0, hasHighEfficiencyDay: false };
    
    const intervalMinutes = settings.intervalMinutes;
    let totalMin = 0;
    let prodMin = 0;
    
    // To check if there's any single day with >= 85% efficiency
    const dailyMinutes: { [date: string]: { total: number; productive: number } } = {};

    logs.forEach(l => {
      const mins = l.durationMinutes || intervalMinutes;
      totalMin += mins;
      
      const isProductive = isLogProductive(l, categories);
      if (isProductive) {
        prodMin += mins;
      }

      if (!dailyMinutes[l.date]) {
        dailyMinutes[l.date] = { total: 0, productive: 0 };
      }
      dailyMinutes[l.date].total += mins;
      if (isProductive) {
        dailyMinutes[l.date].productive += mins;
      }
    });


    const hasHighEfficiencyDay = Object.values(dailyMinutes).some(day => {
      if (day.total === 0) return false;
      const score = Math.round((day.productive / day.total) * 100);
      return score >= 85;
    });

    const totalHours = totalMin / 60;
    const productiveHours = prodMin / 60;
    const avgEfficiency = totalMin > 0 ? Math.round((prodMin / totalMin) * 100) : 0;

    return { totalHours, productiveHours, avgEfficiency, hasHighEfficiencyDay };
  }, [logs, categories, settings]);

  const BADGES = useMemo(() => [
    {
      id: 'first_log',
      name: 'İlk Adım',
      desc: 'Uygulamada ilk zaman kaydını başarıyla eklediniz.',
      icon: '🎯',
      color: '#38bdf8',
      unlocked: logs.length > 0
    },
    {
      id: 'streak_3',
      name: 'İstikrar Abidesi',
      desc: 'En az 3 günlük üretkenlik serisi (streak) yakaladınız.',
      icon: '🔥',
      color: '#fb923c',
      unlocked: currentStreak >= 3
    },
    {
      id: 'pomodoro_complete',
      name: 'Odaklanma Ustası',
      desc: 'En az 1 Pomodoro odaklanma seansını başarıyla tamamladınız.',
      icon: '⏰',
      color: '#ec4899',
      unlocked: totalPomodorosCompleted > 0
    },
    {
      id: 'high_efficiency',
      name: 'Verimlilik Canavarı',
      desc: 'Tek bir günde %85 veya üzeri üretkenlik oranına ulaştınız.',
      icon: '⚡',
      color: '#eab308',
      unlocked: hasHighEfficiencyDay
    },
    {
      id: 'time_conqueror',
      name: 'Zaman Fatihi',
      desc: 'Toplamda 50 saatten fazla zaman kaydı tuttunuz.',
      icon: '👑',
      color: '#a855f7',
      unlocked: totalHours >= 50
    }
  ], [logs.length, currentStreak, totalPomodorosCompleted, hasHighEfficiencyDay, totalHours]);

  const unlockedCount = useMemo(() => BADGES.filter(b => b.unlocked).length, [BADGES]);

  const handleLogout = async () => {
    if (!supabase) return;
    try {
      const { error } = await supabase.auth.signOut();
      if (error) throw error;
      onLogout();
      toast.success('Oturum kapatıldı. Yerel modda devam ediliyor.');
    } catch (err: any) {
      toast.error(`Oturum kapatılamadı: ${err.message}`);
    }
  };

  const avatarUrl = user?.user_metadata?.avatar_url;
  const name = user?.user_metadata?.full_name || settings.userName || 'Kullanıcı';

  // Google Icon SVG
  const GoogleIcon = () => (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ marginRight: '8px' }}>
      <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
      <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
      <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z" fill="#FBBC05"/>
      <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z" fill="#EA4335"/>
    </svg>
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {onBackToDashboard && (
        <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
          <button 
            type="button" 
            className="btn btn-secondary" 
            onClick={onBackToDashboard}
            style={{ 
              padding: '6px 14px', 
              fontSize: '0.82rem', 
              borderRadius: '8px', 
              display: 'flex', 
              alignItems: 'center', 
              gap: '6px',
              border: '1px solid rgba(255,255,255,0.08)'
            }}
          >
            ← Zaman Paneline Dön
          </button>
        </div>
      )}

      {/* 1. Header Profile Box */}
      {!isSupabaseConfigured ? (
        <div 
          className="glass-panel" 
          style={{ 
            padding: '20px 24px', 
            border: '1px solid rgba(234, 179, 8, 0.25)', 
            background: 'rgba(234, 179, 8, 0.02)',
            display: 'flex',
            flexDirection: 'column',
            gap: '12px'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#eab308' }}>
            <ShieldAlert size={18} />
            <strong style={{ fontSize: '0.95rem', fontFamily: 'Outfit' }}>Bulut Senkronizasyonu Pasif (Yerel Mod)</strong>
          </div>
          <p style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', lineHeight: '1.4', margin: 0 }}>
            Bulut yedekleme ve Google ile Giriş için Supabase bilgilerini girmeniz gerekir. 
            Vercel panelinde veya projenin <code>.env</code> dosyasında <code>VITE_SUPABASE_URL</code> ve <code>VITE_SUPABASE_ANON_KEY</code> tanımlandığında bu özellik otomatik aktif olacaktır.
          </p>
        </div>
      ) : user ? (
        <div 
          className="glass-panel" 
          style={{ 
            padding: '24px', 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '20px'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px', flexWrap: 'wrap' }}>
            {avatarUrl ? (
              <img 
                src={avatarUrl} 
                alt="Avatar" 
                style={{ 
                  width: '64px', 
                  height: '64px', 
                  borderRadius: '50%', 
                  border: '2px solid var(--color-primary)', 
                  boxShadow: '0 0 12px var(--color-primary-glow)',
                  objectFit: 'cover'
                }} 
              />
            ) : (
              <div 
                style={{ 
                  width: '64px', 
                  height: '64px', 
                  borderRadius: '50%', 
                  background: 'linear-gradient(135deg, #38bdf8 0%, #8b5cf6 100%)', 
                  color: '#fff', 
                  display: 'flex', 
                  alignItems: 'center', 
                  justifyContent: 'center', 
                  fontWeight: '800', 
                  fontSize: '1.8rem',
                  border: '2px solid rgba(255,255,255,0.1)',
                  fontFamily: 'Outfit, sans-serif'
                }}
              >
                {(name || 'K')[0].toUpperCase()}
              </div>
            )}

            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <h2 style={{ fontSize: '1.4rem', fontWeight: '800', fontFamily: 'Outfit', margin: 0 }}>{name}</h2>
                <span 
                  style={{ 
                    background: 'rgba(34,197,94,0.1)', 
                    border: '1px solid rgba(34,197,94,0.3)', 
                    color: '#22c55e', 
                    fontSize: '0.65rem', 
                    padding: '2px 8px', 
                    borderRadius: '10px', 
                    fontWeight: '700',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px'
                  }}
                  title="Tüm kayıtlarınız buluta güvenle yedekleniyor."
                >
                  <CheckCircle size={10} />
                  Bulut Yedek Aktif
                </span>
              </div>
              
              <p style={{ fontSize: '0.82rem', color: 'var(--color-text-secondary)', display: 'flex', alignItems: 'center', gap: '6px', margin: 0 }}>
                <Mail size={14} />
                {user.email}
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
            <button
              type="button"
              onClick={() => setIsEditModalOpen(true)}
              className="btn btn-secondary"
              style={{
                padding: '10px 20px',
                borderRadius: '12px',
                fontSize: '0.85rem',
                borderColor: 'var(--color-primary-glow)',
                color: 'var(--color-primary)',
                display: 'flex',
                alignItems: 'center',
                gap: '6px'
              }}
            >
              <Edit3 size={14} />
              Düzenle
            </button>
            <button 
              type="button"
              onClick={handleLogout} 
              className="btn btn-secondary" 
              style={{ 
                padding: '10px 20px', 
                borderRadius: '12px', 
                fontSize: '0.85rem', 
                borderColor: 'rgba(239, 68, 68, 0.25)', 
                color: '#ef4444',
                display: 'flex',
                alignItems: 'center',
                gap: '6px'
              }}
            >
              <LogOut size={14} />
              Oturumu Kapat
            </button>
          </div>
        </div>
      ) : (
        <div 
          className="glass-panel" 
          style={{ 
            padding: '24px', 
            display: 'flex', 
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '16px'
          }}
        >
          <div>
            <strong style={{ fontSize: '0.95rem', color: '#fff', display: 'block', fontFamily: 'Outfit' }}>Bulut Yedekleme & Senkronizasyon</strong>
            <span style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)' }}>
              Verilerinizi buluta yedeklemek ve kaybolmasını önlemek için Google hesabınızla giriş yapın.
            </span>
          </div>

          <button 
            type="button"
            onClick={handleLogin} 
            disabled={isLoggingIn}
            className="btn btn-secondary" 
            style={{ 
              padding: '10px 20px', 
              borderRadius: '12px', 
              fontSize: '0.85rem', 
              background: '#fff', 
              color: '#000', 
              border: 'none',
              fontWeight: '700',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              opacity: isLoggingIn ? 0.7 : 1,
              cursor: isLoggingIn ? 'not-allowed' : 'pointer'
            }}
          >
            <GoogleIcon />
            {isLoggingIn ? 'Giriş yapılıyor...' : 'Google ile Giriş Yap'}
          </button>
        </div>
      )}

      {!user && (
        <div 
          className="glass-panel" 
          style={{ 
            padding: '20px 24px', 
            display: 'flex', 
            flexDirection: 'column', 
            gap: '14px'
          }}
        >
          <h3 style={{ fontSize: '1rem', fontWeight: '700', fontFamily: 'Outfit', margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <UserIcon size={16} color="var(--color-primary)" />
            Kullanıcı Bilgileri
          </h3>
          
          <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-end', flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', flex: 1, minWidth: '200px' }}>
              <label style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', fontWeight: '600' }}>
                Kullanıcı Adı
              </label>
              <input
                type="text"
                value={username}
                onChange={e => setUsername(e.target.value)}
                placeholder="İsminiz..."
                maxLength={25}
                style={{ 
                  padding: '8px 12px', 
                  borderRadius: '8px', 
                  background: 'rgba(255,255,255,0.02)', 
                  border: '1px solid var(--color-border)', 
                  color: '#fff',
                  fontSize: '0.85rem',
                  outline: 'none'
                }}
              />
            </div>
            <button 
              type="button"
              onClick={handleSaveUsername}
              style={{ 
                padding: '9px 18px', 
                borderRadius: '8px', 
                fontSize: '0.82rem', 
                fontWeight: 'bold',
                background: 'var(--color-primary)',
                color: '#fff',
                border: 'none',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                height: '38px',
                transition: 'background 0.2s'
              }}
            >
              Kaydet
            </button>
          </div>
        </div>
      )}

      {/* 2. Gamification Widget Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '16px' }}>
        
        {/* Streak Flame Card */}
        <div 
          className="glass-panel" 
          style={{ 
            padding: '24px', 
            display: 'flex', 
            alignItems: 'center', 
            gap: '20px',
            background: currentStreak > 0 ? 'rgba(251, 146, 60, 0.03)' : 'rgba(255,255,255,0.01)',
            borderColor: currentStreak > 0 ? 'rgba(251, 146, 60, 0.2)' : 'var(--color-border)'
          }}
        >
          <div 
            style={{ 
              width: '54px', 
              height: '54px', 
              borderRadius: '14px', 
              background: currentStreak > 0 ? 'rgba(251, 146, 60, 0.12)' : 'rgba(255,255,255,0.02)',
              border: `1px solid ${currentStreak > 0 ? 'rgba(251, 146, 60, 0.3)' : 'var(--color-border)'}`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <Flame size={28} color={currentStreak > 0 ? '#fb923c' : 'var(--color-text-muted)'} />
          </div>
          <div>
            <h4 style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Kesintisiz Seri</h4>
            <h3 style={{ fontSize: '1.35rem', fontWeight: '800', fontFamily: 'Outfit', margin: '2px 0' }}>{currentStreak} Günlük Seri</h3>
            <p style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', margin: 0 }}>
              {currentStreak > 0 ? 'Harika gidiyorsun, zinciri kırma! 🔥' : 'Hedefine ulaşarak bir seri başlat!'}
            </p>
          </div>
        </div>

        {/* Medals Summary Card */}
        <div 
          className="glass-panel" 
          style={{ 
            padding: '24px', 
            display: 'flex', 
            alignItems: 'center', 
            gap: '20px',
            background: unlockedCount > 0 ? 'rgba(168, 85, 247, 0.03)' : 'rgba(255,255,255,0.01)',
            borderColor: unlockedCount > 0 ? 'rgba(168, 85, 247, 0.2)' : 'var(--color-border)'
          }}
        >
          <div 
            style={{ 
              width: '54px', 
              height: '54px', 
              borderRadius: '14px', 
              background: unlockedCount > 0 ? 'rgba(168, 85, 247, 0.12)' : 'rgba(255,255,255,0.02)',
              border: `1px solid ${unlockedCount > 0 ? 'rgba(168, 85, 247, 0.3)' : 'var(--color-border)'}`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <Trophy size={26} color={unlockedCount > 0 ? '#a855f7' : 'var(--color-text-muted)'} />
          </div>
          <div>
            <h4 style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Kazanılan Başarılar</h4>
            <h3 style={{ fontSize: '1.35rem', fontWeight: '800', fontFamily: 'Outfit', margin: '2px 0' }}>{unlockedCount} / {BADGES.length} Madalya</h3>
            <p style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', margin: 0 }}>
              Üretkenlik hedeflerini tamamla, madalyaları topla.
            </p>
          </div>
        </div>

      </div>

      {/* 2b. Daily Productive Target & Category Targets (Moved from Dashboard) */}
      <div 
        className="glass-panel" 
        style={{ 
          padding: '20px 24px', 
          display: 'flex', 
          flexDirection: 'column', 
          gap: '12px',
          borderColor: currentStreak > 0 ? 'rgba(251, 146, 60, 0.25)' : 'var(--color-border)',
          background: currentStreak > 0 ? 'rgba(251, 146, 60, 0.02)' : 'rgba(255, 255, 255, 0.01)'
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
          <h3 style={{ fontSize: '1.05rem', fontFamily: 'Outfit', margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Zap size={18} color="#eab308" fill="#eab308" />
            Günlük Üretkenlik Hedefi
          </h3>
          <div style={{ fontSize: '0.82rem', color: 'var(--color-text-secondary)' }}>
            Bugün: <strong>{todayProductiveStats.totalProdHours.toFixed(1)}</strong> / {settings.dailyProductiveTargetHours || 4} Saat
          </div>
        </div>

        {/* Total Productive progress bar */}
        {(() => {
          const target = settings.dailyProductiveTargetHours || 4;
          const ratio = Math.min(100, Math.round((todayProductiveStats.totalProdHours / target) * 100));
          return (
            <div style={{ width: '100%' }}>
              <div style={{ width: '100%', height: '8px', background: 'rgba(255,255,255,0.03)', borderRadius: '4px', overflow: 'hidden', border: '1px solid var(--color-border)' }}>
                <div 
                  style={{ 
                    width: `${ratio}%`, 
                    height: '100%', 
                    background: 'linear-gradient(90deg, var(--color-primary) 0%, #22c55e 100%)', 
                    borderRadius: '4px',
                    transition: 'width 0.4s ease'
                  }} 
                />
              </div>
            </div>
          );
        })()}

        {/* Category Targets Mini Bars */}
        {(() => {
          const targets = settings.categoryTargets || {};
          const activeTargets = Object.entries(targets).filter(([_, targetVal]) => targetVal > 0);
          if (activeTargets.length === 0) return null;

          return (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px 14px', marginTop: '4px', borderTop: '1px solid var(--color-border)', paddingTop: '10px' }}>
              {activeTargets.map(([catId, targetVal]) => {
                const cat = categories.find(c => c.id === catId);
                if (!cat) return null;
                const hoursLogged = todayProductiveStats.categoryHours[catId] || 0;
                const ratio = Math.min(100, Math.round((hoursLogged / targetVal) * 100));

                return (
                  <div key={catId} style={{ display: 'flex', flexDirection: 'column', gap: '3px', flex: '1 1 120px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.72rem' }}>
                      <span style={{ color: 'var(--color-text-secondary)' }}>{cat.name}</span>
                      <strong style={{ color: '#fff' }}>{hoursLogged.toFixed(1)}/{targetVal}s</strong>
                    </div>
                    {/* Micro bar */}
                    <div style={{ width: '100%', height: '4px', background: 'rgba(255,255,255,0.02)', borderRadius: '2px', overflow: 'hidden' }}>
                      <div 
                        style={{ 
                          width: `${ratio}%`, 
                          height: '100%', 
                          background: cat.color, 
                          borderRadius: '2px',
                          transition: 'width 0.4s ease'
                        }} 
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          );
        })()}
      </div>

      {/* 3. Detailed Statistics Row */}
      <div className="glass-panel" style={{ padding: '24px' }}>
        <h3 style={{ fontSize: '1.05rem', fontFamily: 'Outfit', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <TrendingUp size={18} color="var(--color-primary)" />
          Zaman İstatistikleri
        </h3>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px' }}>
          
          <div style={{ background: 'rgba(255,255,255,0.01)', border: '1px solid var(--color-border)', borderRadius: '12px', padding: '16px' }}>
            <span style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', display: 'block' }}>Toplam Kayıt</span>
            <span style={{ fontSize: '1.5rem', fontWeight: '800', fontFamily: 'Outfit', color: '#fff', display: 'block', marginTop: '4px' }}>
              {logs.length} Dilim
            </span>
          </div>

          <div style={{ background: 'rgba(255,255,255,0.01)', border: '1px solid var(--color-border)', borderRadius: '12px', padding: '16px' }}>
            <span style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', display: 'block' }}>Toplam Süre</span>
            <span style={{ fontSize: '1.5rem', fontWeight: '800', fontFamily: 'Outfit', color: '#fff', display: 'block', marginTop: '4px' }}>
              {totalHours.toFixed(1)} Saat
            </span>
          </div>

          <div style={{ background: 'rgba(255,255,255,0.01)', border: '1px solid var(--color-border)', borderRadius: '12px', padding: '16px' }}>
            <span style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', display: 'block' }}>Üretken Çalışma</span>
            <span style={{ fontSize: '1.5rem', fontWeight: '800', fontFamily: 'Outfit', color: 'var(--color-primary)', display: 'block', marginTop: '4px' }}>
              {productiveHours.toFixed(1)} Saat
            </span>
          </div>

          <div style={{ background: 'rgba(255,255,255,0.01)', border: '1px solid var(--color-border)', borderRadius: '12px', padding: '16px' }}>
            <span style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', display: 'block' }}>Ortalama Verimlilik</span>
            <span style={{ fontSize: '1.5rem', fontWeight: '800', fontFamily: 'Outfit', color: '#22c55e', display: 'block', marginTop: '4px' }}>
              %{avgEfficiency}
            </span>
          </div>

        </div>
      </div>

      {/* 4. Badges/Achievements Listing */}
      <div className="glass-panel" style={{ padding: '24px' }}>
        <h3 style={{ fontSize: '1.05rem', fontFamily: 'Outfit', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Award size={18} color="var(--color-primary)" />
          Başarı Rozetleri & Madalyalar
        </h3>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {BADGES.map(badge => (
            <div 
              key={badge.id}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '16px',
                padding: '16px',
                borderRadius: '16px',
                border: `1px solid ${badge.unlocked ? `${badge.color}30` : 'var(--color-border)'}`,
                background: badge.unlocked ? `${badge.color}04` : 'rgba(255,255,255,0.01)',
                opacity: badge.unlocked ? 1 : 0.45,
                transition: 'all 0.25s ease'
              }}
            >
              {/* Badge Icon circle */}
              <div 
                style={{ 
                  width: '50px', 
                  height: '50px', 
                  borderRadius: '50%', 
                  background: badge.unlocked ? `${badge.color}15` : 'rgba(255,255,255,0.02)',
                  border: `2.5px solid ${badge.unlocked ? badge.color : 'rgba(255,255,255,0.1)'}`,
                  boxShadow: badge.unlocked ? `0 0 10px ${badge.color}25` : 'none',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '1.5rem',
                  flexShrink: 0
                }}
              >
                {badge.icon}
              </div>

              <div style={{ flex: 1 }}>
                <h4 style={{ fontSize: '0.9rem', fontWeight: '700', color: badge.unlocked ? '#fff' : 'var(--color-text-secondary)', margin: 0 }}>
                  {badge.name}
                </h4>
                <p style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', margin: '2px 0 0 0' }}>
                  {badge.desc}
                </p>
              </div>

              {badge.unlocked ? (
                <span style={{ fontSize: '0.72rem', background: `${badge.color}22`, color: badge.color, border: `1px solid ${badge.color}50`, padding: '3px 8px', borderRadius: '8px', fontWeight: 'bold' }}>
                  Açıldı
                </span>
              ) : (
                <span style={{ fontSize: '0.72rem', background: 'rgba(255,255,255,0.02)', color: 'var(--color-text-muted)', border: '1px solid var(--color-border)', padding: '3px 8px', borderRadius: '8px' }}>
                  Kilitli
                </span>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Profil Düzenleme Modalı */}
      {isEditModalOpen && (
        <div 
          className="modal-backdrop"
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: 'rgba(15, 23, 42, 0.85)',
            backdropFilter: 'blur(8px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '20px'
          }}
        >
          <div 
            className="glass-panel animate-scale-in"
            style={{
              width: '100%',
              maxWidth: '420px',
              padding: '28px',
              display: 'flex',
              flexDirection: 'column',
              gap: '20px',
              border: '1px solid rgba(255,255,255,0.08)'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: '800', fontFamily: 'Outfit, sans-serif', color: '#fff', margin: 0 }}>
                Profili Düzenle
              </h3>
              <button
                type="button"
                onClick={() => setIsEditModalOpen(false)}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--color-text-muted)',
                  cursor: 'pointer',
                  fontSize: '1.1rem',
                  padding: '4px'
                }}
              >
                ✕
              </button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '8px' }}>
                {editAvatarUrl ? (
                  <img 
                    src={editAvatarUrl} 
                    alt="Preview" 
                    style={{ width: '80px', height: '80px', borderRadius: '50%', objectFit: 'cover', border: '2.5px solid var(--color-primary)', boxShadow: '0 0 12px var(--color-primary-glow)' }} 
                  />
                ) : (
                  <div style={{ width: '80px', height: '80px', borderRadius: '50%', background: 'linear-gradient(135deg, #38bdf8 0%, #8b5cf6 100%)', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '2rem', fontWeight: 'bold' }}>
                    {(editName || 'K')[0].toUpperCase()}
                  </div>
                )}
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <label style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', fontWeight: '600' }}>
                  Kullanıcı Adı
                </label>
                <input
                  type="text"
                  value={editName}
                  onChange={e => setEditName(e.target.value)}
                  placeholder="Adınız ve soyadınız..."
                  maxLength={30}
                  style={{
                    padding: '10px 12px',
                    borderRadius: '8px',
                    background: 'rgba(255,255,255,0.03)',
                    border: '1px solid var(--color-border)',
                    color: '#fff',
                    outline: 'none',
                    fontSize: '0.85rem'
                  }}
                />
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <label style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', fontWeight: '600' }}>
                  E-posta (Google ile Giriş)
                </label>
                <input
                  type="email"
                  value={editEmail}
                  disabled
                  style={{
                    padding: '10px 12px',
                    borderRadius: '8px',
                    background: 'rgba(255,255,255,0.01)',
                    border: '1px solid rgba(255,255,255,0.04)',
                    color: 'var(--color-text-muted)',
                    cursor: 'not-allowed',
                    fontSize: '0.85rem'
                  }}
                />
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <label style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', fontWeight: '600' }}>
                  Profil Fotoğrafı URL
                </label>
                <input
                  type="text"
                  value={editAvatarUrl}
                  onChange={e => setEditAvatarUrl(e.target.value)}
                  placeholder="Görsel bağlantısı yapıştırın (https://...)"
                  style={{
                    padding: '10px 12px',
                    borderRadius: '8px',
                    background: 'rgba(255,255,255,0.03)',
                    border: '1px solid var(--color-border)',
                    color: '#fff',
                    outline: 'none',
                    fontSize: '0.85rem'
                  }}
                />
              </div>
            </div>

            <div style={{ display: 'flex', gap: '12px', marginTop: '8px' }}>
              <button
                type="button"
                onClick={() => setIsEditModalOpen(false)}
                className="btn btn-secondary"
                style={{ flex: 1, padding: '10px 0', borderRadius: '10px', fontSize: '0.85rem' }}
              >
                İptal
              </button>
              <button
                type="button"
                onClick={handleSaveProfile}
                className="btn btn-primary"
                style={{ flex: 1, padding: '10px 0', borderRadius: '10px', fontSize: '0.85rem', border: 'none' }}
              >
                Kaydet
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
