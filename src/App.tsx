import { useState, useEffect } from 'react';
import { LayoutDashboard, Settings as SettingsIcon, BarChart3, Timer, CloudSync } from 'lucide-react';
import { storageService } from './services/storageService';
import type { Category, TimeLog, AppSettings } from './services/storageService';
import { notificationService } from './services/notificationService';
import { useToast } from './components/Toast';
import OptimumLogo from './components/OptimumLogo';
import Dashboard from './components/Dashboard';
import Settings from './components/Settings';
import Analytics from './components/Analytics';
import Pomodoro from './components/Pomodoro';

type Tab = 'dashboard' | 'pomodoro' | 'analytics' | 'settings';

export default function App() {
  const [activeTab, setActiveTab] = useState<Tab>('dashboard');
  const [categories, setCategories] = useState<Category[]>(() => storageService.getCategories());
  const [logs, setLogs] = useState<TimeLog[]>(() => storageService.getLogs());
  const [settings, setSettings] = useState<AppSettings>(() => storageService.getSettings());
  const [pendingLog, setPendingLog] = useState<{ slot: string; date: string } | null>(null);
  const [syncStatus, setSyncStatus] = useState<'synced' | 'pending'>('synced');

  const toast = useToast();

  useEffect(() => {
    const hasUnsynced = logs.some(l => !l.synced);
    setSyncStatus(hasUnsynced ? 'pending' : 'synced');
  }, [logs]);

  useEffect(() => {
    const handleNotificationClicked = (e: Event) => {
      const detail = (e as CustomEvent).detail;
      if (detail?.slot && detail?.date) {
        setPendingLog({ slot: detail.slot, date: detail.date });
        setActiveTab('dashboard');
      }
    };
    window.addEventListener('optimum-notification-clicked', handleNotificationClicked);

    const init = async () => {
      const status = await notificationService.getPermissionStatus();
      if (status === 'default' && settings.notificationsEnabled) {
        await notificationService.requestPermission();
      }
      await notificationService.rescheduleNotifications();
    };
    init();

    return () => window.removeEventListener('optimum-notification-clicked', handleNotificationClicked);
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
    return newLog;
  };

  const handleLogDelete = async (id: string) => {
    storageService.deleteLog(id);
    setLogs(storageService.getLogs());
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
  };

  const handleResetAll = () => {
    storageService.resetAll();
    setCategories(storageService.getCategories());
    setLogs(storageService.getLogs());
    setSettings(storageService.getSettings());
    notificationService.cancelAllNotifications();
    setActiveTab('dashboard');
  };

  const handleSync = () => {
    if (syncStatus === 'pending') {
      const unsyncedIds = logs.filter(l => !l.synced).map(l => l.id);
      storageService.markAsSynced(unsyncedIds);
      setLogs(storageService.getLogs());
      setSyncStatus('synced');
      toast.success('Tüm veriler başarıyla senkronize edildi!');
    } else {
      toast.info('Tüm veriler zaten güncel.');
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

        {/* Sync Indicator */}
        <button
          className="btn btn-secondary"
          onClick={handleSync}
          style={{ padding: '8px 14px', borderRadius: '12px', fontSize: '0.85rem' }}
          title={syncStatus === 'pending' ? 'Senkronize edilmemiş kayıtlar var.' : 'Tüm veriler güncel.'}
        >
          <CloudSync size={18} color={syncStatus === 'pending' ? '#eab308' : '#22c55e'} />
        </button>
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
        {activeTab === 'pomodoro' && <Pomodoro />}
        {activeTab === 'analytics' && (
          <Analytics categories={categories} logs={logs} />
        )}
        {activeTab === 'settings' && (
          <Settings
            categories={categories}
            settings={settings}
            onCategoriesChange={handleCategoriesChange}
            onSettingsChange={handleSettingsChange}
            onBackupImport={handleBackupImport}
            onResetAll={handleResetAll}
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
