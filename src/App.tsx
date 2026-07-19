import { useState, useEffect } from 'react';
import { LayoutDashboard, Settings as SettingsIcon, BarChart3, Timer, CloudSync, User as UserIcon } from 'lucide-react';
import { storageService } from './services/storageService';
import type { Category, TimeLog, AppSettings } from './services/storageService';
import { notificationService } from './services/notificationService';
import { useToast } from './components/Toast';
import { supabase } from './services/supabaseClient';
import type { User } from '@supabase/supabase-js';

import OptimumLogo from './components/OptimumLogo';
import Dashboard from './components/Dashboard';
import Settings from './components/Settings';
import Analytics from './components/Analytics';
import Pomodoro from './components/Pomodoro';
import Profile from './components/Profile';
import { usePomodoro } from './hooks/usePomodoro';

type Tab = 'dashboard' | 'pomodoro' | 'analytics' | 'profile' | 'settings';

export default function App() {
  const [activeTab, setActiveTab] = useState<Tab>('dashboard');
  const [categories, setCategories] = useState<Category[]>(() => storageService.getCategories());
  const [logs, setLogs] = useState<TimeLog[]>(() => storageService.getLogs());
  const [settings, setSettings] = useState<AppSettings>(() => storageService.getSettings());
  const [pendingLog, setPendingLog] = useState<{ slot: string; date: string } | null>(null);
  const [syncStatus, setSyncStatus] = useState<'synced' | 'pending'>('synced');
  
  // Initialize background Pomodoro timer hook
  const pomodoro = usePomodoro(settings.notificationSound || 'modern');

  // Supabase User State
  const [user, setUser] = useState<User | null>(null);


  const toast = useToast();

  // Supabase Auth State Listener
  useEffect(() => {
    if (!supabase) return;

    supabase.auth.getSession().then(({ data: { session } }) => {
      setUser(session?.user ?? null);
    });

    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      setUser(session?.user ?? null);
    });

    return () => subscription.unsubscribe();
  }, []);

  useEffect(() => {
    const hasUnsyncedLogs = logs.some(l => !l.synced);
    const hasUnsyncedDeletions = storageService.getDeletedLogIds().length > 0;
    const hasUnsynced = hasUnsyncedLogs || hasUnsyncedDeletions;

    setSyncStatus(hasUnsynced ? 'pending' : 'synced');

    if (hasUnsynced && user) {
      const timer = setTimeout(() => {
        triggerSync(user);
      }, 1500); // 1.5s debounce auto-sync
      return () => clearTimeout(timer);
    }
  }, [logs, user]);

  // Sync function helper
  const triggerSync = async (currentUser = user) => {
    if (!currentUser) return;
    setSyncStatus('pending');
    const mergedLogs = await storageService.syncLogsWithCloud(currentUser.id);
    setLogs(mergedLogs);
    
    const hasUnsyncedLogs = mergedLogs.some(l => !l.synced);
    const hasUnsyncedDeletions = storageService.getDeletedLogIds().length > 0;
    setSyncStatus(hasUnsyncedLogs || hasUnsyncedDeletions ? 'pending' : 'synced');
  };

  // Sync on user login/auth change + Periodic background sync
  useEffect(() => {
    if (!user) return;

    triggerSync(user);
    
    // Auto-set profile name from Google meta
    const currentSettings = storageService.getSettings();
    const googleName = user.user_metadata?.full_name;
    if (googleName && (!currentSettings.userName || currentSettings.userName === 'Kullanıcı')) {
      const updated = { ...currentSettings, userName: googleName };
      storageService.saveSettings(updated);
      setSettings(updated);
    }

    const intervalId = setInterval(() => {
      triggerSync(user);
    }, 30000);

    return () => clearInterval(intervalId);
  }, [user]);

  useEffect(() => {
    const handleNotificationClicked = (e: Event) => {
      const detail = (e as CustomEvent).detail;
      if (detail?.slot && detail?.date) {
        setPendingLog({ slot: detail.slot, date: detail.date });
        setActiveTab('dashboard');
      }
    };
    window.addEventListener('optimum-notification-clicked', handleNotificationClicked);

    const handleServiceWorkerMessage = (event: MessageEvent) => {
      if (event.data && event.data.type === 'optimum-notification-clicked') {
        const { slot, date } = event.data;
        if (slot && date) {
          setPendingLog({ slot, date });
          setActiveTab('dashboard');
        }
      }
    };
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.addEventListener('message', handleServiceWorkerMessage);
    }

    const urlParams = new URLSearchParams(window.location.search);
    const paramSlot = urlParams.get('slot');
    const paramDate = urlParams.get('date');
    if (paramSlot && paramDate) {
      setPendingLog({ slot: paramSlot, date: paramDate });
      setActiveTab('dashboard');
      window.history.replaceState({}, document.title, window.location.pathname);
    }

    const init = async () => {
      const status = await notificationService.getPermissionStatus();
      if (status === 'default' && settings.notificationsEnabled) {
        await notificationService.requestPermission();
      }
      await notificationService.rescheduleNotifications();
    };
    init();

    return () => {
      window.removeEventListener('optimum-notification-clicked', handleNotificationClicked);
      if ('serviceWorker' in navigator) {
        navigator.serviceWorker.removeEventListener('message', handleServiceWorkerMessage);
      }
    };
  }, [settings.notificationsEnabled]);

  const handleSettingsChange = async (newSettings: AppSettings) => {
    storageService.saveSettings(newSettings);
    setSettings(newSettings);
    await notificationService.rescheduleNotifications();
  };

  const handleLogAdd = async (logData: Omit<TimeLog, 'id' | 'timestamp' | 'synced' | 'updatedAt'>) => {
    const newLog = storageService.addLog(logData);
    setLogs(storageService.getLogs());
    await notificationService.rescheduleNotifications();

    if (user) {
      await storageService.addLogToCloud(newLog, user.id);
      storageService.markAsSynced([newLog.id]);
      setLogs(storageService.getLogs());
    }
    return newLog;
  };

  const handleLogDelete = async (id: string) => {
    storageService.deleteLog(id);
    setLogs(storageService.getLogs());

    if (user) {
      try {
        await storageService.deleteLogFromCloud(id, user.id);
        const currentQueue = storageService.getDeletedLogIds().filter(q => q !== id);
        storageService.saveDeletedLogIds(currentQueue);
      } catch (err) {
        console.error('Direct cloud delete failed, queued offline:', err);
      }
      setLogs(storageService.getLogs()); // trigger sync check
    }
  };

  const handleCategoriesChange = (newCategories: Category[]) => {
    storageService.saveCategories(newCategories);
    setCategories(newCategories);
  };

  const handleBackupImport = () => {
    setCategories(storageService.getCategories());
    setLogs(storageService.getLogs());
    setSettings(storageService.getSettings());
    notificationService.rescheduleNotifications();
    if (user) {
      triggerSync(user);
    }
  };

  const handleResetAll = async () => {
    if (user && supabase) {
      try {
        await storageService.clearAllLogsFromCloud(user.id);
        await supabase.auth.signOut();
      } catch (err) {
        console.error('Failed to clear cloud logs during reset:', err);
      }
    }
    storageService.resetAll();
    setCategories(storageService.getCategories());
    setLogs(storageService.getLogs());
    setSettings(storageService.getSettings());
    notificationService.cancelAllNotifications();
    setUser(null);
    setActiveTab('dashboard');
  };

  const handleLoadDemoData = () => {
    storageService.generateMockData();
    setLogs(storageService.getLogs());
    toast.success('Örnek (demo) veriler başarıyla yüklendi!');
  };


  const handleLogout = () => {
    setUser(null);
    setLogs(storageService.getLogs());
  };

  const handleSyncClick = async () => {
    if (user) {
      toast.info('Bulut senkronizasyonu başlatıldı...');
      await triggerSync(user);
      toast.success('Bulut senkronizasyonu tamamlandı!');
    } else {
      toast.info('Bulut senkronizasyonu için Google ile giriş yapmanız gerekiyor. Yönlendiriliyorsunuz...');
      setActiveTab('profile');
    }
  };

  const NAV_ITEMS: { key: Tab; label: string; icon: React.ElementType }[] = [
    { key: 'dashboard', label: 'Panel',    icon: LayoutDashboard },
    { key: 'pomodoro',  label: 'Pomodoro', icon: Timer           },
    { key: 'analytics', label: 'Analiz',   icon: BarChart3       },
    { key: 'settings',  label: 'Ayarlar',  icon: SettingsIcon    },
  ];


  return (
    <div className="app-container">
      <header className="app-header">
        {/* Logo + App Name */}
        <div className="app-logo" style={{ gap: '10px' }}>
          <OptimumLogo size={36} />
          <span
            style={{
              fontFamily: 'Outfit, sans-serif',
              fontWeight: '800',
              fontSize: '1.25rem',
              background: 'linear-gradient(135deg, #38bdf8 0%, #8b5cf6 50%, #f97316 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              backgroundClip: 'text',
              letterSpacing: '-0.01em',
            }}
          >
            Optimum
          </span>
        </div>

        {/* Sync Indicator and Profile Avatar */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <button
            className="btn btn-secondary"
            onClick={handleSyncClick}
            style={{ 
              padding: '8px 12px', 
              borderRadius: '12px', 
              fontSize: '0.85rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              border: '1px solid rgba(255,255,255,0.06)'
            }}
            title={
              !user 
                ? 'Bulut yedekleme aktif değil. Giriş yapmak için tıklayın.' 
                : (syncStatus === 'pending' 
                  ? 'Kaydedilmemiş değişiklikleriniz var. Eşitleniyor...' 
                  : 'Bulut verileriniz güncel ve eşitlendi.')
            }
          >
            <CloudSync 
              size={18} 
              color={
                !user 
                  ? '#64748b' 
                  : (syncStatus === 'pending' 
                    ? '#f97316' 
                    : '#22c55e')
              } 
            />
          </button>

          {user ? (
            <button
              onClick={() => setActiveTab('profile')}
              style={{
                background: 'none',
                padding: 0,
                cursor: 'pointer',
                borderRadius: '50%',
                overflow: 'hidden',
                width: '32px',
                height: '32px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 0 8px rgba(56, 189, 248, 0.2)',
                border: '1.5px solid var(--color-primary)'
              }}
              title="Profiliniz"
            >
              {user.user_metadata?.avatar_url ? (
                <img 
                  src={user.user_metadata.avatar_url} 
                  alt="Avatar" 
                  style={{ width: '100%', height: '100%', objectFit: 'cover' }} 
                />
              ) : (
                <div style={{ width: '100%', height: '100%', background: 'linear-gradient(135deg, #38bdf8 0%, #8b5cf6 100%)', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.78rem', fontWeight: 'bold' }}>
                  {(user.user_metadata?.full_name || settings.userName || 'K')[0].toUpperCase()}
                </div>
              )}
            </button>
          ) : (
            <button
              onClick={() => setActiveTab('profile')}
              className="btn btn-primary"
              style={{ 
                padding: '6px 14px', 
                borderRadius: '10px', 
                fontSize: '0.8rem',
                fontWeight: 'bold',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                border: 'none',
                cursor: 'pointer'
              }}
            >
              <UserIcon size={13} />
              Giriş Yap
            </button>
          )}
        </div>
      </header>

      <main className="main-content">
        {activeTab === 'dashboard' && (
          <Dashboard
            categories={categories}
            logs={logs}
            settings={settings}
            onLogAdd={handleLogAdd}
            onLogDelete={handleLogDelete}
            pendingLog={pendingLog}
            clearPendingLog={() => setPendingLog(null)}
          />
        )}
        {activeTab === 'pomodoro' && <Pomodoro pomodoroState={pomodoro} />}
        {activeTab === 'analytics' && (
          <Analytics 
            categories={categories} 
            logs={logs} 
            settings={settings}
            onNavigateToTab={(tab) => setActiveTab(tab)} 
          />
        )}
        {activeTab === 'profile' && (
          <Profile
            categories={categories}
            logs={logs}
            user={user}
            onLogout={handleLogout}
            onBackToDashboard={() => setActiveTab('dashboard')}
            onSettingsChange={handleSettingsChange}
          />
        )}
        {activeTab === 'settings' && (
          <Settings
            categories={categories}
            settings={settings}
            onCategoriesChange={handleCategoriesChange}
            onSettingsChange={handleSettingsChange}
            onBackupImport={handleBackupImport}
            onResetAll={handleResetAll}
            onLoadDemoData={handleLoadDemoData}
            user={user}
          />
        )}

      </main>

      <nav className="bottom-nav">
        {NAV_ITEMS.map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            className={`nav-item ${activeTab === key ? 'nav-item-active' : ''}`}
            onClick={() => setActiveTab(key)}
            aria-label={label}
          >
            <Icon size={20} />
            <span>{label}</span>
          </button>
        ))}
      </nav>
    </div>
  );
}
