import { useMemo } from 'react';
import { supabase } from '../services/supabaseClient';
import { storageService } from '../services/storageService';
import type { Category, TimeLog } from '../services/storageService';
import { calculateStreak } from '../utils/streakUtils';
import { 
  Award, Flame, TrendingUp, User, Mail, ShieldCheck, Trophy 
} from 'lucide-react';

import { useToast } from './Toast';

interface ProfileProps {
  categories: Category[];
  logs: TimeLog[];
  user: any;
  onLogout: () => void;
  onBackToDashboard?: () => void;
}

export default function Profile({ categories, logs, user, onLogout, onBackToDashboard }: ProfileProps) {
  const toast = useToast();
  const settings = useMemo(() => storageService.getSettings(), []);

  // --- 1. STREAK CALCULATION LOGIC ---
  const currentStreak = useMemo(() => {
    return calculateStreak(logs, categories, settings);
  }, [logs, categories, settings]);


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
      
      const cat = categories.find(c => c.id === l.categoryId);
      const isProductive = cat ? (cat.isProductive !== false) : ['egitim', 'market', 'ibadet', 'sosyal'].includes(l.categoryId);
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
      <div 
        className="glass-panel" 
        style={{ 
          padding: '30px 24px', 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '20px'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '20px', flexWrap: 'wrap' }}>
          {/* Dynamic Avatar */}
          {avatarUrl ? (
            <img 
              src={avatarUrl} 
              alt="Avatar" 
              style={{ 
                width: '80px', 
                height: '80px', 
                borderRadius: '50%', 
                border: '3px solid var(--color-primary)', 
                boxShadow: '0 0 15px var(--color-primary-glow)',
                objectFit: 'cover'
              }} 
            />
          ) : (
            <div 
              style={{ 
                width: '80px', 
                height: '80px', 
                borderRadius: '50%', 
                background: 'linear-gradient(135deg, #38bdf8 0%, #8b5cf6 100%)', 
                color: '#fff', 
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'center', 
                fontWeight: '800', 
                fontSize: '2rem',
                border: '3px solid rgba(255,255,255,0.1)',
                fontFamily: 'Outfit, sans-serif'
              }}
            >
              {(name || 'K')[0].toUpperCase()}
            </div>
          )}

          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <h1 style={{ fontSize: '1.6rem', fontWeight: '800', fontFamily: 'Outfit', margin: 0 }}>{name}</h1>
              {user && (
                <span 
                  style={{ 
                    background: 'rgba(34,197,94,0.1)', 
                    border: '1px solid rgba(34,197,94,0.3)', 
                    color: '#22c55e', 
                    fontSize: '0.7rem', 
                    padding: '2px 8px', 
                    borderRadius: '10px', 
                    fontWeight: '700',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px'
                  }}
                >
                  <ShieldCheck size={12} />
                  Bulut Yedek Aktif
                </span>
              )}
            </div>
            
            <p style={{ fontSize: '0.82rem', color: 'var(--color-text-secondary)', marginTop: '4px', display: 'flex', alignItems: 'center', gap: '6px' }}>
              {user ? (
                <>
                  <Mail size={14} />
                  {user.email}
                </>
              ) : (
                <>
                  <User size={14} />
                  Yerel Profil (Çevrimdışı Çalışma)
                </>
              )}
            </p>
          </div>
        </div>

        {user && (
          <button 
            onClick={handleLogout} 
            className="btn btn-secondary" 
            style={{ 
              padding: '10px 20px', 
              borderRadius: '12px', 
              fontSize: '0.85rem', 
              borderColor: 'rgba(239, 68, 68, 0.25)', 
              color: '#ef4444' 
            }}
          >
            Oturumu Kapat
          </button>
        )}
      </div>

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

    </div>
  );
}
