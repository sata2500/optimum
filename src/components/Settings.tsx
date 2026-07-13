import { useState, useRef, useEffect } from 'react';
import {
  Plus, Trash2, Edit3, X, Save, AlertTriangle, Settings as SettingsIcon,
  FolderKanban, Download, Upload, Check, Bell, RefreshCcw, ShieldAlert,
  Clock, Zap
} from 'lucide-react';
import type { Category, Activity, AppSettings } from '../services/storageService';
import { storageService } from '../services/storageService';
import { notificationService } from '../services/notificationService';
import { useToast } from './Toast';
import ConfirmDialog from './ConfirmDialog';

interface SettingsProps {
  categories: Category[];
  settings: AppSettings;
  onCategoriesChange: (newCategories: Category[]) => void;
  onSettingsChange: (newSettings: AppSettings) => void;
  onBackupImport: () => void;
  onResetAll: () => void;
}

type SubTab = 'general' | 'categories' | 'backup';

interface ConfirmState {
  open: boolean;
  title: string;
  message: string;
  confirmLabel: string;
  danger: boolean;
  onConfirm: () => void;
}

const INITIAL_CONFIRM: ConfirmState = {
  open: false,
  title: '',
  message: '',
  confirmLabel: 'Onayla',
  danger: false,
  onConfirm: () => {},
};

const INTERVAL_OPTIONS = [
  { value: 15, label: '15', sub: 'Dakika' },
  { value: 30, label: '30', sub: 'Dakika' },
  { value: 60, label: '60', sub: 'Dakika / 1 Saat' },
];

const PRESET_COLORS = [
  { color: '#22c55e', text: '#ffffff' },
  { color: '#3b82f6', text: '#ffffff' },
  { color: '#8b5cf6', text: '#ffffff' },
  { color: '#ef4444', text: '#ffffff' },
  { color: '#eab308', text: '#000000' },
  { color: '#06b6d4', text: '#ffffff' },
  { color: '#ec4899', text: '#ffffff' },
  { color: '#f97316', text: '#ffffff' },
];

export default function Settings({
  categories,
  settings,
  onCategoriesChange,
  onSettingsChange,
  onBackupImport,
  onResetAll,
}: SettingsProps) {
  const toast = useToast();
  const [activeSubTab, setActiveSubTab] = useState<SubTab>('general');
  const [confirmState, setConfirmState] = useState<ConfirmState>(INITIAL_CONFIRM);

  // --- GENERAL SETTINGS STATES ---
  const [formInterval, setFormInterval] = useState<number>(settings.intervalMinutes);
  const [formStart, setFormStart] = useState<string>(settings.startHour);
  const [formEnd, setFormEnd] = useState<string>(settings.endHour);
  const [formNotifications, setFormNotifications] = useState<boolean>(settings.notificationsEnabled);
  const [notifPermission, setNotifPermission] = useState<string>('default');
  const [isSavingGeneral, setIsSavingGeneral] = useState(false);

  useEffect(() => {
    const checkPerm = async () => {
      const status = await notificationService.getPermissionStatus();
      setNotifPermission(status);
    };
    checkPerm();
  }, []);

  // --- CATEGORIES & ACTIVITIES STATES ---
  const [activeCategory, setActiveCategory] = useState<Category | null>(null);
  const [showCatModal, setShowCatModal] = useState<boolean>(false);
  const [catName, setCatName] = useState<string>('');
  const [catColor, setCatColor] = useState<string>('#3b82f6');

  const [showActModal, setShowActModal] = useState<boolean>(false);
  const [targetCatId, setTargetCatId] = useState<string>('');
  const [editingActivity, setEditingActivity] = useState<Activity | null>(null);
  const [actCode, setActCode] = useState<string>('');
  const [actName, setActName] = useState<string>('');
  const [validationError, setValidationError] = useState<string>('');

  const fileInputRef = useRef<HTMLInputElement>(null);

  // ── CONFIRM HELPER ──────────────────────────────────────────────────
  const showConfirm = (
    title: string,
    message: string,
    onConfirm: () => void,
    { danger = false, confirmLabel = 'Onayla' } = {}
  ) => {
    setConfirmState({ open: true, title, message, confirmLabel, danger, onConfirm });
  };

  const closeConfirm = () => setConfirmState(INITIAL_CONFIRM);

  // ── GENERAL SETTINGS ────────────────────────────────────────────────
  const requestAndVerifyPermission = async (): Promise<boolean> => {
    if (!('Notification' in window)) {
      toast.error('Tarayıcınız bildirim desteklemiyor. Lütfen localhost veya HTTPS bağlantısı kullanın.');
      return false;
    }
    if (Notification.permission === 'denied') {
      toast.error('Bildirim izni engellenmiş. Tarayıcı ayarlarından kilit simgesine tıklayarak izni açın.');
      return false;
    }
    const granted = await notificationService.requestPermission();
    const status = await notificationService.getPermissionStatus();
    setNotifPermission(status);
    if (!granted) {
      toast.warning('Bildirim izni reddedildi. Hatırlatıcılar devre dışı kalacak.');
    }
    return granted;
  };

  const handleSaveGeneralSettings = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSavingGeneral(true);
    let isNotifEnabled = formNotifications;

    if (formNotifications) {
      const granted = await requestAndVerifyPermission();
      if (!granted) {
        isNotifEnabled = false;
        setFormNotifications(false);
      }
    }

    onSettingsChange({
      intervalMinutes: formInterval,
      startHour: formStart,
      endHour: formEnd,
      notificationsEnabled: isNotifEnabled,
    });

    setIsSavingGeneral(false);
    toast.success('Genel ayarlar başarıyla kaydedildi!');
  };

  const handleTestNotification = async () => {
    const granted = await requestAndVerifyPermission();
    if (granted) {
      try {
        new Notification('Optimum Flow', {
          body: 'Zaman takip hatırlatıcı bildirimleriniz aktif edildi!',
          requireInteraction: false,
        });
        toast.success('Test bildirimi başarıyla gönderildi.');
        setFormNotifications(true);
        await notificationService.rescheduleNotifications();
      } catch (err) {
        console.error(err);
        toast.error('Bildirim gönderilemedi. Secure context (HTTPS/localhost) gerekli.');
      }
    }
  };

  // ── CATEGORY ACTIONS ────────────────────────────────────────────────
  const handleOpenCatModal = (category?: Category) => {
    if (category) {
      setActiveCategory(category);
      setCatName(category.name);
      setCatColor(category.color);
    } else {
      setActiveCategory(null);
      setCatName('');
      setCatColor('#3b82f6');
    }
    setShowCatModal(true);
  };

  const handleSaveCategory = () => {
    if (!catName.trim()) return;
    let updatedCategories: Category[];

    if (activeCategory) {
      updatedCategories = categories.map(cat => {
        if (cat.id === activeCategory.id) {
          const preset = PRESET_COLORS.find(p => p.color === catColor) || { text: '#ffffff' };
          return { ...cat, name: catName.trim(), color: catColor, textColor: preset.text };
        }
        return cat;
      });
    } else {
      const preset = PRESET_COLORS.find(p => p.color === catColor) || { text: '#ffffff' };
      const newCat: Category = {
        id: `cat_${Date.now()}`,
        name: catName.trim(),
        color: catColor,
        textColor: preset.text,
        activities: [],
      };
      updatedCategories = [...categories, newCat];
    }

    onCategoriesChange(updatedCategories);
    setShowCatModal(false);
    toast.success(activeCategory ? 'Kategori güncellendi.' : 'Yeni kategori oluşturuldu.');
  };

  const handleDeleteCategory = (catId: string, catName: string) => {
    showConfirm(
      'Kategoriyi Sil',
      `"${catName}" kategorisini ve altındaki TÜM aktiviteleri kalıcı olarak silmek istediğinize emin misiniz?`,
      () => {
        const updated = categories.filter(c => c.id !== catId);
        onCategoriesChange(updated);
        closeConfirm();
        toast.success('Kategori silindi.');
      },
      { danger: true, confirmLabel: 'Evet, Sil' }
    );
  };

  // ── ACTIVITY ACTIONS ────────────────────────────────────────────────
  const handleOpenActModal = (catId: string, activity?: Activity) => {
    setTargetCatId(catId);
    setValidationError('');

    if (activity) {
      setEditingActivity(activity);
      setActCode(activity.code);
      setActName(activity.name);
    } else {
      setEditingActivity(null);
      const cat = categories.find(c => c.id === catId);
      let nextCode = '';
      if (cat) {
        const firstLetter = cat.name.charAt(0).toUpperCase();
        let maxNum = 0;
        cat.activities.forEach(act => {
          const num = parseInt(act.code.replace(/[^0-9]/g, ''), 10);
          if (!isNaN(num) && num > maxNum) maxNum = num;
        });
        nextCode = `${firstLetter}${maxNum + 1}`;
      }
      setActCode(nextCode);
      setActName('');
    }
    setShowActModal(true);
  };

  const handleSaveActivity = () => {
    if (!actCode.trim() || !actName.trim()) return;
    const trimmedCode = actCode.trim().toUpperCase();

    const duplicate = categories.some(cat =>
      cat.activities.some(act =>
        act.code.toUpperCase() === trimmedCode &&
        !(editingActivity && editingActivity.id === act.id)
      )
    );

    if (duplicate) {
      setValidationError(`"${trimmedCode}" kodu başka bir aktivitede zaten kullanılıyor.`);
      return;
    }

    const updatedCategories = categories.map(cat => {
      if (cat.id !== targetCatId) return cat;
      let updatedActivities: Activity[];
      if (editingActivity) {
        updatedActivities = cat.activities.map(act =>
          act.id === editingActivity.id ? { ...act, code: trimmedCode, name: actName.trim() } : act
        );
      } else {
        const newAct: Activity = { id: `act_${Date.now()}`, code: trimmedCode, name: actName.trim() };
        updatedActivities = [...cat.activities, newAct];
      }
      return { ...cat, activities: updatedActivities };
    });

    onCategoriesChange(updatedCategories);
    setShowActModal(false);
    toast.success(editingActivity ? 'Aktivite güncellendi.' : 'Yeni aktivite eklendi.');
  };

  const handleDeleteActivity = (catId: string, actId: string, actName: string) => {
    showConfirm(
      'Aktiviteyi Sil',
      `"${actName}" aktivitesini kalıcı olarak silmek istediğinize emin misiniz?`,
      () => {
        const updatedCategories = categories.map(cat => {
          if (cat.id !== catId) return cat;
          return { ...cat, activities: cat.activities.filter(a => a.id !== actId) };
        });
        onCategoriesChange(updatedCategories);
        closeConfirm();
        toast.success('Aktivite silindi.');
      },
      { danger: true, confirmLabel: 'Evet, Sil' }
    );
  };

  // ── BACKUP ACTIONS ──────────────────────────────────────────────────
  const handleExportBackup = () => {
    const backup = storageService.exportBackup();
    const blob = new Blob([backup], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `optimum_flow_backup_${new Date().toISOString().split('T')[0]}.json`;
    a.click();
    URL.revokeObjectURL(url);
    toast.success('Yedek dosyası bilgisayarınıza kaydedildi.');
  };

  const handleImportBackup = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      const text = event.target?.result as string;
      const success = storageService.importBackup(text);
      if (success) {
        onBackupImport();
        toast.success('Yedek başarıyla geri yüklendi!');
      } else {
        toast.error('Yedek yükleme başarısız. Dosya formatını kontrol edin.');
      }
    };
    reader.readAsText(file);
    // Reset input so same file can be re-selected
    e.target.value = '';
  };

  const handleLoadMockData = () => {
    showConfirm(
      'Örnek Veri Yükle',
      '9 günlük örnek veriler yüklenecek. Bu işlem mevcut zaman kayıtlarınızın üzerine yazacak. Devam etmek istiyor musunuz?',
      () => {
        storageService.generateMockData();
        onBackupImport();
        closeConfirm();
        toast.success('Örnek veriler başarıyla yüklendi!');
      },
      { confirmLabel: 'Yükle' }
    );
  };

  const handleClearLogs = () => {
    showConfirm(
      'Tüm Kayıtları Temizle',
      'Tüm zaman kayıtlarınız kalıcı olarak silinecek. Kategorileriniz ve ayarlarınız korunacak. Bu işlem geri alınamaz!',
      () => {
        storageService.clearAllLogs();
        onBackupImport();
        closeConfirm();
        toast.success('Tüm zaman kayıtları temizlendi.');
      },
      { danger: true, confirmLabel: 'Evet, Temizle' }
    );
  };

  const handleResetAllClick = () => {
    showConfirm(
      'Tüm Uygulamayı Sıfırla',
      'TÜM verileriniz (zaman kayıtları, özel kategoriler, ayarlar) kalıcı olarak silinecek ve uygulama fabrika ayarlarına dönecek. Bu işlem GERİ ALINAMAZ!',
      () => {
        onResetAll();
        closeConfirm();
        toast.success('Uygulama fabrika ayarlarına döndürüldü.');
      },
      { danger: true, confirmLabel: '🗑 Evet, Her Şeyi Sıfırla' }
    );
  };

  // ── PERMISSION BADGE ────────────────────────────────────────────────
  const permBadge = () => {
    if (notifPermission === 'granted') return <span className="perm-badge perm-badge-granted">✓ İzin Verildi</span>;
    if (notifPermission === 'denied') return <span className="perm-badge perm-badge-denied">✗ Engellendi</span>;
    return <span className="perm-badge perm-badge-default">⚠ İzin Eksik</span>;
  };

  // ── RENDER ───────────────────────────────────────────────────────────
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>

      {/* Title */}
      <div>
        <h1 style={{ fontSize: '1.8rem', fontWeight: '800', fontFamily: 'Outfit', color: '#fff' }}>
          Uygulama Ayarları
        </h1>
        <p style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', marginTop: '4px' }}>
          Zamanlayıcıyı, kategorileri ve yedeklerinizi tek noktadan yönetin.
        </p>
      </div>

      {/* Segmented Sub Tabs */}
      <div className="segmented-control">
        <button
          className={`segment-btn ${activeSubTab === 'general' ? 'segment-btn-active' : ''}`}
          onClick={() => setActiveSubTab('general')}
        >
          <SettingsIcon size={13} style={{ marginRight: '6px', verticalAlign: 'middle', display: 'inline-block' }} />
          Genel Ayarlar
        </button>
        <button
          className={`segment-btn ${activeSubTab === 'categories' ? 'segment-btn-active' : ''}`}
          onClick={() => setActiveSubTab('categories')}
        >
          <FolderKanban size={13} style={{ marginRight: '6px', verticalAlign: 'middle', display: 'inline-block' }} />
          Kategoriler & İşler
        </button>
        <button
          className={`segment-btn ${activeSubTab === 'backup' ? 'segment-btn-active' : ''}`}
          onClick={() => setActiveSubTab('backup')}
        >
          <Download size={13} style={{ marginRight: '6px', verticalAlign: 'middle', display: 'inline-block' }} />
          Veri & Yedek
        </button>
      </div>

      {/* ── PANEL 1: GENERAL SETTINGS ─────────────────────────────── */}
      {activeSubTab === 'general' && (
        <form onSubmit={handleSaveGeneralSettings} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>

          {/* Interval Cards */}
          <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Clock size={18} color="var(--color-primary)" />
              <div>
                <h3 style={{ fontSize: '1rem', fontWeight: '700', fontFamily: 'Outfit' }}>Zaman Dilimi Aralığı</h3>
                <p style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', marginTop: '2px' }}>
                  Her kaç dakikada bir kayıt isteyeceğini seçin
                </p>
              </div>
            </div>

            <div className="interval-cards">
              {INTERVAL_OPTIONS.map(opt => (
                <button
                  key={opt.value}
                  type="button"
                  className={`interval-card ${formInterval === opt.value ? 'interval-card-active' : ''}`}
                  onClick={() => setFormInterval(opt.value)}
                >
                  <span className="interval-value">{opt.label}</span>
                  <span className="interval-unit">{opt.sub}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Time Range */}
          <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Zap size={18} color="var(--color-primary)" />
              <div>
                <h3 style={{ fontSize: '1rem', fontWeight: '700', fontFamily: 'Outfit' }}>Aktif Takip Saatleri</h3>
                <p style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', marginTop: '2px' }}>
                  Bu aralık dışında bildirim gönderilmez
                </p>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <label style={{ fontSize: '0.82rem', color: 'var(--color-text-secondary)', fontWeight: '600' }}>
                  Başlangıç Saati
                </label>
                <input
                  type="time"
                  value={formStart}
                  onChange={e => setFormStart(e.target.value)}
                  style={{ colorScheme: 'dark' }}
                />
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <label style={{ fontSize: '0.82rem', color: 'var(--color-text-secondary)', fontWeight: '600' }}>
                  Bitiş Saati
                </label>
                <input
                  type="time"
                  value={formEnd}
                  onChange={e => setFormEnd(e.target.value)}
                  style={{ colorScheme: 'dark' }}
                />
              </div>
            </div>
          </div>

          {/* Notifications */}
          <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Bell size={18} color="var(--color-primary)" />
              <h3 style={{ fontSize: '1rem', fontWeight: '700', fontFamily: 'Outfit' }}>Hatırlatıcı Bildirimler</h3>
            </div>

            <div className="settings-row">
              <div className="settings-row-label">
                <strong>Bildirimleri Etkinleştir</strong>
                <span>Her zaman diliminde size hatırlatıcı gönderilir</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                {permBadge()}
                <label className="toggle-switch" aria-label="Bildirimleri aç/kapat">
                  <input
                    type="checkbox"
                    checked={formNotifications}
                    onChange={e => setFormNotifications(e.target.checked)}
                  />
                  <span className="toggle-track" />
                </label>
              </div>
            </div>

            <button
              type="button"
              className="btn btn-secondary"
              onClick={handleTestNotification}
              style={{ alignSelf: 'flex-start', padding: '8px 16px', borderRadius: '10px', fontSize: '0.85rem', border: '1px solid var(--color-primary-glow)', color: 'var(--color-primary)' }}
            >
              <Bell size={14} />
              Test Bildirimi Gönder
            </button>
          </div>

          {/* Save Button */}
          <button
            type="submit"
            className="btn btn-primary"
            disabled={isSavingGeneral}
            style={{ alignSelf: 'flex-start', padding: '12px 28px' }}
          >
            <Save size={16} />
            {isSavingGeneral ? 'Kaydediliyor...' : 'Ayarları Kaydet'}
          </button>
        </form>
      )}

      {/* ── PANEL 2: CATEGORIES & ACTIVITIES ───────────────────────── */}
      {activeSubTab === 'categories' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
            <p style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>
              Kategorilerinizi ve aktivite listelerini özelleştirin.
              <span style={{ marginLeft: '8px', color: 'var(--color-primary)', fontWeight: '600' }}>
                {categories.length} kategori
              </span>
            </p>
            <button
              className="btn btn-primary"
              onClick={() => handleOpenCatModal()}
              style={{ padding: '8px 18px', borderRadius: '12px' }}
            >
              <Plus size={16} />
              Yeni Kategori
            </button>
          </div>

          {categories.length === 0 ? (
            <div className="glass-panel" style={{ padding: '48px 24px', textAlign: 'center', color: 'var(--color-text-secondary)' }}>
              <FolderKanban size={40} color="var(--color-text-muted)" style={{ marginBottom: '12px' }} />
              <h3 style={{ color: '#fff', marginBottom: '6px' }}>Henüz kategori yok</h3>
              <p style={{ fontSize: '0.85rem' }}>Yeni Kategori butonuna tıklayarak başlayın.</p>
            </div>
          ) : (
            <div className="categories-grid">
              {categories.map(cat => (
                <div
                  key={cat.id}
                  className="category-card animate-scale-in"
                  style={{ '--accent-color': cat.color } as React.CSSProperties}
                >
                  {/* Card Header */}
                  <div className="category-card-header">
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <span
                        className="color-indicator"
                        style={{
                          background: cat.color,
                          boxShadow: `0 0 8px ${cat.color}60`,
                        }}
                      />
                      <h3 className="category-name">{cat.name}</h3>
                      <span style={{
                        fontSize: '0.7rem',
                        color: 'var(--color-text-muted)',
                        background: 'rgba(255,255,255,0.05)',
                        padding: '1px 7px',
                        borderRadius: '10px',
                        fontWeight: '600',
                      }}>
                        {cat.activities.length}
                      </span>
                    </div>
                    <div className="category-actions">
                      <button
                        className="icon-btn"
                        onClick={() => handleOpenCatModal(cat)}
                        title="Düzenle"
                        aria-label={`${cat.name} düzenle`}
                      >
                        <Edit3 size={13} />
                      </button>
                      <button
                        className="icon-btn delete-btn"
                        onClick={() => handleDeleteCategory(cat.id, cat.name)}
                        title="Sil"
                        aria-label={`${cat.name} sil`}
                      >
                        <Trash2 size={13} />
                      </button>
                    </div>
                  </div>

                  {/* Activities List */}
                  <div className="activities-list">
                    {cat.activities.length === 0 ? (
                      <p className="no-activities">Henüz aktivite tanımlanmamış.</p>
                    ) : (
                      cat.activities.map(act => (
                        <div key={act.id} className="activity-item">
                          <div className="activity-info">
                            <span
                              className="activity-code"
                              style={{ background: `${cat.color}22`, color: cat.color }}
                            >
                              {act.code}
                            </span>
                            <span className="activity-name">{act.name}</span>
                          </div>
                          <div className="activity-item-actions">
                            <button
                              className="icon-btn"
                              onClick={() => handleOpenActModal(cat.id, act)}
                              title="Düzenle"
                              aria-label={`${act.name} düzenle`}
                            >
                              <Edit3 size={12} />
                            </button>
                            <button
                              className="icon-btn delete-btn"
                              onClick={() => handleDeleteActivity(cat.id, act.id, act.name)}
                              title="Sil"
                              aria-label={`${act.name} sil`}
                            >
                              <Trash2 size={12} />
                            </button>
                          </div>
                        </div>
                      ))
                    )}
                  </div>

                  {/* Add Activity Button */}
                  <button
                    className="add-activity-btn"
                    onClick={() => handleOpenActModal(cat.id)}
                  >
                    <Plus size={13} />
                    Aktivite Ekle
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ── PANEL 3: DATA & BACKUP ──────────────────────────────────── */}
      {activeSubTab === 'backup' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>

          <div className="glass-panel" style={{ padding: '24px' }}>
            <h2 style={{ fontSize: '1.1rem', fontWeight: '700', fontFamily: 'Outfit', marginBottom: '6px' }}>
              Veri Yönetimi & Güvenlik Yedekleri
            </h2>
            <p style={{ fontSize: '0.83rem', color: 'var(--color-text-secondary)', maxWidth: '600px' }}>
              Tüm verilerinizi JSON formatında dışa aktarabilir veya önceki bir yedeği geri yükleyebilirsiniz.
            </p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '14px' }}>

            {/* Export */}
            <div style={{ border: '1px solid var(--color-border)', borderRadius: '16px', padding: '20px', background: 'var(--bg-surface)', display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div style={{ width: '36px', height: '36px', borderRadius: '10px', background: 'rgba(6,182,212,0.1)', border: '1px solid rgba(6,182,212,0.25)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Download size={16} color="#06b6d4" />
                </div>
                <h4 style={{ fontSize: '0.92rem', fontWeight: '700', fontFamily: 'Outfit' }}>Yedek İndir</h4>
              </div>
              <p style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', flex: 1 }}>
                Tüm verilerinizi içeren bir .json yedek dosyası oluşturun.
              </p>
              <button className="btn btn-secondary" onClick={handleExportBackup} style={{ fontSize: '0.82rem' }}>
                <Download size={13} />
                Yedeği Kaydet
              </button>
            </div>

            {/* Import */}
            <div style={{ border: '1px solid var(--color-border)', borderRadius: '16px', padding: '20px', background: 'var(--bg-surface)', display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div style={{ width: '36px', height: '36px', borderRadius: '10px', background: 'rgba(139,92,246,0.1)', border: '1px solid rgba(139,92,246,0.25)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Upload size={16} color="#8b5cf6" />
                </div>
                <h4 style={{ fontSize: '0.92rem', fontWeight: '700', fontFamily: 'Outfit' }}>Yedeği Yükle</h4>
              </div>
              <p style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', flex: 1 }}>
                Daha önce aldığınız bir .json dosyasından geri yükleyin.
              </p>
              <label className="btn btn-secondary" style={{ cursor: 'pointer', textAlign: 'center', fontSize: '0.82rem' }}>
                <Upload size={13} />
                Dosya Seç
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleImportBackup}
                  accept=".json"
                  style={{ display: 'none' }}
                />
              </label>
            </div>

            {/* Mock Data */}
            <div style={{ border: '1px solid var(--color-border)', borderRadius: '16px', padding: '20px', background: 'var(--bg-surface)', display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div style={{ width: '36px', height: '36px', borderRadius: '10px', background: 'rgba(34,197,94,0.1)', border: '1px solid rgba(34,197,94,0.25)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <RefreshCcw size={16} color="#22c55e" />
                </div>
                <h4 style={{ fontSize: '0.92rem', fontWeight: '700', fontFamily: 'Outfit' }}>Örnek Veri</h4>
              </div>
              <p style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', flex: 1 }}>
                Uygulamayı test etmek için 9 günlük örnek veriler yükleyin.
              </p>
              <button className="btn btn-secondary" onClick={handleLoadMockData} style={{ fontSize: '0.82rem' }}>
                <RefreshCcw size={13} />
                Örnek Veri Yükle
              </button>
            </div>

            {/* Clear Logs */}
            <div style={{ border: '1px solid rgba(239,68,68,0.2)', borderRadius: '16px', padding: '20px', background: 'rgba(239,68,68,0.03)', display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div style={{ width: '36px', height: '36px', borderRadius: '10px', background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.25)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Trash2 size={16} color="#ef4444" />
                </div>
                <h4 style={{ fontSize: '0.92rem', fontWeight: '700', fontFamily: 'Outfit', color: '#ef4444' }}>Kayıtları Sil</h4>
              </div>
              <p style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', flex: 1 }}>
                Tüm zaman kayıtlarını siler. Kategoriler korunur.
              </p>
              <button
                className="btn btn-secondary"
                onClick={handleClearLogs}
                style={{ fontSize: '0.82rem', color: '#ef4444', border: '1px solid rgba(239,68,68,0.25)' }}
              >
                <Trash2 size={13} />
                Kayıtları Temizle
              </button>
            </div>

            {/* Factory Reset */}
            <div style={{ border: '1px solid rgba(239,68,68,0.35)', borderRadius: '16px', padding: '20px', background: 'rgba(239,68,68,0.05)', display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div style={{ width: '36px', height: '36px', borderRadius: '10px', background: 'rgba(239,68,68,0.15)', border: '1px solid rgba(239,68,68,0.35)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <ShieldAlert size={16} color="#ef4444" />
                </div>
                <h4 style={{ fontSize: '0.92rem', fontWeight: '700', fontFamily: 'Outfit', color: '#ef4444' }}>Fabrika Sıfırlama</h4>
              </div>
              <p style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', flex: 1 }}>
                Tüm veri, kategori ve ayarları siler. Uygulama baştan başlar.
              </p>
              <button
                className="btn btn-danger"
                onClick={handleResetAllClick}
                style={{ fontSize: '0.82rem' }}
              >
                <ShieldAlert size={13} />
                Her Şeyi Sıfırla
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── CATEGORY MODAL ───────────────────────────────────────────── */}
      {showCatModal && (
        <div className="modal-overlay">
          <div className="modal-content animate-scale-in" role="dialog" aria-modal="true" aria-labelledby="cat-modal-title">
            <div className="modal-header">
              <h3 id="cat-modal-title" style={{ fontSize: '1.1rem', fontFamily: 'Outfit' }}>
                {activeCategory ? 'Kategoriyi Düzenle' : 'Yeni Kategori Ekle'}
              </h3>
              <button
                className="btn btn-secondary"
                onClick={() => setShowCatModal(false)}
                aria-label="Kapat"
                style={{ padding: '6px', borderRadius: '50%' }}
              >
                <X size={14} />
              </button>
            </div>
            <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <label style={{ fontSize: '0.82rem', color: 'var(--color-text-secondary)', fontWeight: '600' }}>
                  Kategori Adı
                </label>
                <input
                  type="text"
                  value={catName}
                  onChange={e => setCatName(e.target.value)}
                  placeholder="örn. Eğitim, İbadet, İş..."
                  autoFocus
                  style={{ width: '100%' }}
                />
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <label style={{ fontSize: '0.82rem', color: 'var(--color-text-secondary)', fontWeight: '600' }}>
                  Tema Rengi
                </label>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '10px' }}>
                  {PRESET_COLORS.map(p => (
                    <button
                      key={p.color}
                      type="button"
                      onClick={() => setCatColor(p.color)}
                      style={{
                        background: p.color,
                        height: '40px',
                        borderRadius: '10px',
                        border: catColor === p.color ? '3px solid #fff' : '2px solid transparent',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        transition: 'all 0.15s',
                        boxShadow: catColor === p.color ? `0 0 12px ${p.color}60` : 'none',
                      }}
                    >
                      {catColor === p.color && <Check size={16} color={p.text} />}
                    </button>
                  ))}
                </div>
                {/* Preview */}
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px',
                  padding: '10px 14px',
                  background: `${catColor}12`,
                  border: `1px solid ${catColor}40`,
                  borderRadius: '10px',
                }}>
                  <span style={{ width: '12px', height: '12px', borderRadius: '50%', background: catColor, boxShadow: `0 0 8px ${catColor}` }} />
                  <span style={{ fontSize: '0.85rem', fontWeight: '700', color: '#fff', fontFamily: 'Outfit' }}>
                    {catName || 'Kategori Adı Önizleme'}
                  </span>
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setShowCatModal(false)}>İptal</button>
              <button className="btn btn-primary" onClick={handleSaveCategory} disabled={!catName.trim()}>
                <Save size={14} />
                Kaydet
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── ACTIVITY MODAL ───────────────────────────────────────────── */}
      {showActModal && (
        <div className="modal-overlay">
          <div className="modal-content animate-scale-in" role="dialog" aria-modal="true" aria-labelledby="act-modal-title">
            <div className="modal-header">
              <h3 id="act-modal-title" style={{ fontSize: '1.1rem', fontFamily: 'Outfit' }}>
                {editingActivity ? 'Aktiviteyi Düzenle' : 'Yeni Aktivite Ekle'}
              </h3>
              <button
                className="btn btn-secondary"
                onClick={() => setShowActModal(false)}
                aria-label="Kapat"
                style={{ padding: '6px', borderRadius: '50%' }}
              >
                <X size={14} />
              </button>
            </div>
            <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {validationError && (
                <div style={{
                  display: 'flex', gap: '8px', background: 'rgba(239,68,68,0.1)',
                  color: '#ef4444', padding: '10px 14px', borderRadius: '10px', fontSize: '0.82rem',
                  border: '1px solid rgba(239,68,68,0.2)',
                }}>
                  <AlertTriangle size={16} style={{ flexShrink: 0 }} />
                  <span>{validationError}</span>
                </div>
              )}

              <div style={{ display: 'grid', gridTemplateColumns: '90px 1fr', gap: '12px' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: '0.82rem', color: 'var(--color-text-secondary)', fontWeight: '600' }}>Kod</label>
                  <input
                    type="text"
                    value={actCode}
                    onChange={e => { setActCode(e.target.value.substring(0, 5)); setValidationError(''); }}
                    placeholder="E1"
                    maxLength={5}
                    autoFocus
                    style={{ width: '100%', textAlign: 'center', textTransform: 'uppercase', fontWeight: '800' }}
                  />
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: '0.82rem', color: 'var(--color-text-secondary)', fontWeight: '600' }}>Aktivite Adı</label>
                  <input
                    type="text"
                    value={actName}
                    onChange={e => setActName(e.target.value)}
                    placeholder="örn. Matematik Çalışması"
                    style={{ width: '100%' }}
                  />
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setShowActModal(false)}>İptal</button>
              <button
                className="btn btn-primary"
                onClick={handleSaveActivity}
                disabled={!actCode.trim() || !actName.trim()}
              >
                <Save size={14} />
                Kaydet
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── CONFIRM DIALOG ───────────────────────────────────────────── */}
      <ConfirmDialog
        open={confirmState.open}
        title={confirmState.title}
        message={confirmState.message}
        confirmLabel={confirmState.confirmLabel}
        danger={confirmState.danger}
        onConfirm={confirmState.onConfirm}
        onCancel={closeConfirm}
      />
    </div>
  );
}
