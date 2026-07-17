import { parseTimeToMinutes, generateSlots } from '../utils/timeUtils';
import { supabase } from './supabaseClient';

export interface Activity {
  id: string;
  code: string;
  name: string;
}

export interface Category {
  id: string;
  name: string;
  color: string; // Hex color code
  textColor: string; // Hex or CSS color for text readability
  activities: Activity[];
  isProductive?: boolean; // Represents productive/useful time
}

export interface TimeLog {
  id: string;
  timestamp: number; // milliseconds
  date: string; // YYYY-MM-DD
  timeSlot: string; // HH:MM
  activityId: string;
  activityCode: string;
  activityName: string;
  categoryId: string;
  categoryName: string;
  categoryColor: string;
  notes?: string;
  synced: boolean;
  updatedAt: number;
  durationMinutes?: number;
}

export interface AppSettings {
  intervalMinutes: number; // 15, 30, 60, custom
  startHour: string; // HH:MM, e.g. "07:30"
  endHour: string; // HH:MM, e.g. "00:30"
  notificationsEnabled: boolean;
  userName?: string; // Profile name
  dailyProductiveTargetHours?: number; // Total productive hours goal per day
  categoryTargets?: { [catId: string]: number }; // Individual category goals
  notificationSound?: 'modern' | 'classic' | 'soft' | 'silent';
}

const STORAGE_KEYS = {
  CATEGORIES: 'optimum_flow_categories',
  LOGS: 'optimum_flow_logs',
  SETTINGS: 'optimum_flow_settings',
};

const DEFAULT_CATEGORIES: Category[] = [
  {
    id: 'egitim',
    name: 'Eğitimlerim',
    color: '#22c55e', // Green
    textColor: '#ffffff',
    isProductive: true,
    activities: [
      { id: 'e1', code: 'E1', name: 'O.T.D' },
      { id: 'e2', code: 'E2', name: 'Circle' },
      { id: 'e3', code: 'E3', name: 'O.T. Kütüphane' },
      { id: 'e4', code: 'E4', name: 'Hızlı Kütüphane' },
      { id: 'e5', code: 'E5', name: 'Zihin Kodlama' },
      { id: 'e6', code: 'E6', name: 'İtikat Antrenmanları' },
      { id: 'e7', code: 'E7', name: 'Storbox & vd.' },
    ],
  },
  {
    id: 'market',
    name: 'Market / İş',
    color: '#3b82f6', // Blue
    textColor: '#ffffff',
    isProductive: true,
    activities: [
      { id: 'm9', code: 'M9', name: 'Müşteriler' },
      { id: 'm10', code: 'M10', name: 'Temizlik, Düzen' },
      { id: 'm11', code: 'M11', name: 'Raf ve vb. İşler' },
      { id: 'm12', code: 'M12', name: 'Muhasebe' },
      { id: 'm13', code: 'M13', name: 'Sohbet (-)' },
      { id: 'm14', code: 'M14', name: 'Diğer' },
    ],
  },
  {
    id: 'ibadet',
    name: 'İbadetlerim',
    color: '#8b5cf6', // Violet/Purple
    textColor: '#ffffff',
    isProductive: true,
    activities: [
      { id: 'i15', code: 'İ15', name: 'Abdest' },
      { id: 'i16', code: 'İ16', name: 'Namaz' },
      { id: 'i17', code: 'İ17', name: 'Dini Okumalarım' },
      { id: 'i18', code: 'İ18', name: 'Sohbet Meclisleri' },
      { id: 'i19', code: 'İ19', name: 'Dini Videolar' },
      { id: 'i20', code: 'İ20', name: 'Muhasebe' },
    ],
  },
  {
    id: 'zaman_kaybi',
    name: 'Zaman Kaybı / Boş',
    color: '#ef4444', // Red
    textColor: '#ffffff',
    isProductive: false,
    activities: [
      { id: 'z21', code: 'Z21', name: 'Boş Vakit Geçirmek' },
      { id: 'z22', code: 'Z22', name: 'Haberler / Sosyal Medya' },
      { id: 'z23', code: 'Z23', name: 'Kişisel Bakım / İhtiyaç' },
    ],
  },
  {
    id: 'telefon',
    name: 'Telefon',
    color: '#eab308', // Yellow
    textColor: '#000000',
    isProductive: false,
    activities: [
      { id: 't27', code: 'T27', name: 'Kişisel ve Sosyal Medya' },
      { id: 't28', code: 'T28', name: 'Sosyal Medya Dışı' },
      { id: 't29', code: 'T29', name: 'Arama vb.' },
    ],
  },
  {
    id: 'sosyal',
    name: 'Sosyal & Aktiviteler',
    color: '#06b6d4', // Cyan
    textColor: '#ffffff',
    isProductive: true,
    activities: [
      { id: 's31', code: 'S31', name: 'Kitaplık' },
      { id: 's32', code: 'S32', name: 'Arkadaşlar' },
      { id: 's33', code: 'S33', name: 'Spor / Yürüyüş' },
    ],
  },
];

const DEFAULT_SETTINGS: AppSettings = {
  intervalMinutes: 15,
  startHour: '07:30',
  endHour: '00:30',
  notificationsEnabled: true,
  userName: 'Kullanıcı',
  dailyProductiveTargetHours: 4,
  categoryTargets: {
    egitim: 2,
    market: 3,
    ibadet: 1
  },
  notificationSound: 'modern'
};

export const storageService = {
  // --- SETTINGS ---
  getSettings(): AppSettings {
    const data = localStorage.getItem(STORAGE_KEYS.SETTINGS);
    if (!data) return DEFAULT_SETTINGS;
    try {
      return { ...DEFAULT_SETTINGS, ...JSON.parse(data) };
    } catch {
      return DEFAULT_SETTINGS;
    }
  },

  saveSettings(settings: AppSettings): void {
    localStorage.setItem(STORAGE_KEYS.SETTINGS, JSON.stringify(settings));
  },

  // --- CATEGORIES ---
  getCategories(): Category[] {
    const data = localStorage.getItem(STORAGE_KEYS.CATEGORIES);
    if (!data) {
      this.saveCategories(DEFAULT_CATEGORIES);
      return DEFAULT_CATEGORIES;
    }
    try {
      return JSON.parse(data);
    } catch {
      return DEFAULT_CATEGORIES;
    }
  },

  saveCategories(categories: Category[]): void {
    localStorage.setItem(STORAGE_KEYS.CATEGORIES, JSON.stringify(categories));
  },

  // Helper to find an activity by code
  findActivityByCode(code: string): { category: Category; activity: Activity } | null {
    const categories = this.getCategories();
    const cleanCode = code.trim().toUpperCase();
    for (const cat of categories) {
      const act = cat.activities.find(a => a.code.toUpperCase() === cleanCode);
      if (act) {
        return { category: cat, activity: act };
      }
    }
    return null;
  },

  // Helper to find an activity by ID
  findActivityById(id: string): { category: Category; activity: Activity } | null {
    const categories = this.getCategories();
    for (const cat of categories) {
      const act = cat.activities.find(a => a.id === id);
      if (act) {
        return { category: cat, activity: act };
      }
    }
    return null;
  },

  // --- LOGS ---
  getLogs(): TimeLog[] {
    const data = localStorage.getItem(STORAGE_KEYS.LOGS);
    if (!data) return [];
    try {
      return JSON.parse(data);
    } catch {
      return [];
    }
  },

  saveLogs(logs: TimeLog[]): void {
    localStorage.setItem(STORAGE_KEYS.LOGS, JSON.stringify(logs));
  },

  addLog(logData: Omit<TimeLog, 'id' | 'timestamp' | 'synced' | 'updatedAt'>): TimeLog {
    const logs = this.getLogs();
    const now = Date.now();
    const newLog: TimeLog = {
      ...logData,
      id: `log_${now}_${Math.random().toString(36).substr(2, 9)}`,
      timestamp: now,
      synced: false,
      updatedAt: now,
    };
    
    // Check if slot already has a log on that date, if so, replace it
    const existingIndex = logs.findIndex(
      l => l.date === logData.date && l.timeSlot === logData.timeSlot
    );

    if (existingIndex > -1) {
      logs[existingIndex] = newLog;
    } else {
      logs.push(newLog);
    }

    this.saveLogs(logs);
    return newLog;
  },

  deleteLog(id: string): void {
    const logs = this.getLogs();
    const updated = logs.filter(l => l.id !== id);
    this.saveLogs(updated);
  },

  clearAllLogs(): void {
    this.saveLogs([]);
  },

  // --- CLOUD OPERATIONS (SUPABASE) ---
  async syncLogsWithCloud(userId: string): Promise<TimeLog[]> {
    if (!supabase) return this.getLogs();
    try {
      const { data: cloudLogs, error } = await supabase
        .from('time_logs')
        .select('*')
        .eq('user_id', userId);

      if (error) throw error;

      const localLogs = this.getLogs();
      const mergedMap = new Map<string, TimeLog>();

      // Index local logs
      localLogs.forEach(l => mergedMap.set(l.id, l));

      const logsToInsertInCloud: any[] = [];
      const logsToUpdateInCloud: any[] = [];

      // Process cloud logs
      if (cloudLogs) {
        cloudLogs.forEach((c: any) => {
          const mappedCloudLog: TimeLog = {
            id: c.id,
            timestamp: Number(c.timestamp),
            date: c.date,
            timeSlot: c.time_slot,
            activityId: c.activity_id,
            activityCode: c.activity_code,
            activityName: c.activity_name,
            categoryId: c.category_id,
            categoryName: c.category_name,
            categoryColor: c.category_color,
            notes: c.notes || undefined,
            synced: true,
            updatedAt: Number(c.updated_at),
          };

          const local = mergedMap.get(c.id);
          if (!local) {
            mergedMap.set(c.id, mappedCloudLog);
          } else {
            if (mappedCloudLog.updatedAt > local.updatedAt) {
              mergedMap.set(c.id, mappedCloudLog);
            } else if (local.updatedAt > mappedCloudLog.updatedAt || !local.synced) {
              logsToUpdateInCloud.push(local);
            }
          }
        });
      }

      // Check local logs not in cloud
      localLogs.forEach(l => {
        const inCloud = cloudLogs?.some((c: any) => c.id === l.id);
        if (!inCloud) {
          logsToInsertInCloud.push(l);
        }
      });

      // Insert new to cloud
      if (logsToInsertInCloud.length > 0) {
        const inserts = logsToInsertInCloud.map(l => ({
          id: l.id,
          user_id: userId,
          timestamp: l.timestamp,
          date: l.date,
          time_slot: l.timeSlot,
          activity_id: l.activityId,
          activity_code: l.activityCode,
          activity_name: l.activityName,
          category_id: l.categoryId,
          category_name: l.categoryName,
          category_color: l.categoryColor,
          notes: l.notes || null,
          updated_at: l.updatedAt
        }));
        const { error: insErr } = await supabase.from('time_logs').insert(inserts);
        if (insErr) console.error('Cloud insert error:', insErr.message);
      }

      // Update in cloud
      for (const l of logsToUpdateInCloud) {
        const { error: updErr } = await supabase
          .from('time_logs')
          .update({
            timestamp: l.timestamp,
            date: l.date,
            time_slot: l.timeSlot,
            activity_id: l.activityId,
            activity_code: l.activityCode,
            activity_name: l.activityName,
            category_id: l.categoryId,
            category_name: l.categoryName,
            category_color: l.categoryColor,
            notes: l.notes || null,
            updated_at: l.updatedAt
          })
          .eq('id', l.id)
          .eq('user_id', userId);
        if (updErr) console.error('Cloud update error:', updErr.message);
      }

      const finalLogs = Array.from(mergedMap.values()).map(l => ({ ...l, synced: true }));
      this.saveLogs(finalLogs);
      return finalLogs;
    } catch (err) {
      console.error('Synchronization failed:', err);
      return this.getLogs();
    }
  },

  async addLogToCloud(log: TimeLog, userId: string): Promise<void> {
    if (!supabase) return;
    try {
      await supabase.from('time_logs').upsert({
        id: log.id,
        user_id: userId,
        timestamp: log.timestamp,
        date: log.date,
        time_slot: log.timeSlot,
        activity_id: log.activityId,
        activity_code: log.activityCode,
        activity_name: log.activityName,
        category_id: log.categoryId,
        category_name: log.categoryName,
        category_color: log.categoryColor,
        notes: log.notes || null,
        updated_at: log.updatedAt
      });
    } catch (err) {
      console.error('Add to cloud failed:', err);
    }
  },

  async deleteLogFromCloud(id: string, userId: string): Promise<void> {
    if (!supabase) return;
    try {
      await supabase
        .from('time_logs')
        .delete()
        .eq('id', id)
        .eq('user_id', userId);
    } catch (err) {
      console.error('Delete from cloud failed:', err);
    }
  },

  async clearAllLogsFromCloud(userId: string): Promise<void> {
    if (!supabase) return;
    try {
      await supabase
        .from('time_logs')
        .delete()
        .eq('user_id', userId);
    } catch (err) {
      console.error('Clear cloud logs failed:', err);
    }
  },

  // --- SYNC / IMPORT-EXPORT ---
  exportBackup(): string {
    const data = {
      categories: this.getCategories(),
      logs: this.getLogs(),
      settings: this.getSettings(),
      version: '1.0.0',
      exportedAt: Date.now(),
    };
    return JSON.stringify(data, null, 2);
  },

  importBackup(backupString: string): boolean {
    try {
      const parsed = JSON.parse(backupString);
      if (!parsed.categories || !parsed.logs || !parsed.settings) {
        return false;
      }
      this.saveCategories(parsed.categories);
      this.saveLogs(parsed.logs);
      this.saveSettings(parsed.settings);
      return true;
    } catch (e) {
      console.error('Import error:', e);
      return false;
    }
  },

  // --- FACTORY RESET ---
  resetAll(): void {
    Object.values(STORAGE_KEYS).forEach(key => localStorage.removeItem(key));
  },

  // Mark all logs as synced (called after successful backend sync)
  markAsSynced(logIds: string[]): void {
    const logs = this.getLogs();
    const updated = logs.map(log => {
      if (logIds.includes(log.id)) {
        return { ...log, synced: true, updatedAt: Date.now() };
      }
      return log;
    });
    this.saveLogs(updated);
  },

  // Generate 9 days of mock data
  generateMockData(): void {


    const categories = this.getCategories();
    const logs: TimeLog[] = [];
    const settings = this.getSettings();
    const timeSlots = generateSlots(settings.startHour, settings.endHour, settings.intervalMinutes);
    
    const now = new Date();
    // 9 days: from 8 days ago until today
    for (let d = 8; d >= 0; d--) {
      const targetDate = new Date(now.getTime() - d * 24 * 60 * 60 * 1000);
      const dateStr = targetDate.toISOString().split('T')[0];
      
      timeSlots.forEach(slot => {
        // 75% fill rate
        if (Math.random() > 0.25) {
          const timeMin = parseTimeToMinutes(slot);
          let categoryId = 'zaman_kaybi';
          
          if (timeMin >= 450 && timeMin < 510) { // 07:30 - 08:30
            categoryId = Math.random() > 0.5 ? 'ibadet' : 'zaman_kaybi';
          } else if (timeMin >= 510 && timeMin < 750) { // 08:30 - 12:30
            categoryId = Math.random() > 0.4 ? 'egitim' : 'market';
          } else if (timeMin >= 750 && timeMin < 810) { // 12:30 - 13:30
            categoryId = Math.random() > 0.5 ? 'ibadet' : 'zaman_kaybi';
          } else if (timeMin >= 810 && timeMin < 1050) { // 13:30 - 17:30
            categoryId = Math.random() > 0.5 ? 'market' : 'egitim';
          } else if (timeMin >= 1050 && timeMin < 1170) { // 17:30 - 19:30
            categoryId = Math.random() > 0.5 ? 'sosyal' : 'ibadet';
          } else if (timeMin >= 1170 && timeMin < 1290) { // 19:30 - 21:30
            categoryId = Math.random() > 0.6 ? 'telefon' : 'sosyal';
          } else if (timeMin >= 1290 || timeMin < 30) { // 21:30 - 00:30
            categoryId = Math.random() > 0.5 ? 'ibadet' : 'egitim';
          }
          
          const cat = categories.find(c => c.id === categoryId);
          if (cat && cat.activities.length > 0) {
            const act = cat.activities[Math.floor(Math.random() * cat.activities.length)];
            
            const logTime = new Date(targetDate);
            const [h, m] = slot.split(':').map(Number);
            logTime.setHours(h, m, 0, 0);
            
            logs.push({
              id: `log_mock_${logTime.getTime()}_${Math.random().toString(36).substr(2, 5)}`,
              timestamp: logTime.getTime(),
              date: dateStr,
              timeSlot: slot,
              activityId: act.id,
              activityCode: act.code,
              activityName: act.name,
              categoryId: cat.id,
              categoryName: cat.name,
              categoryColor: cat.color,
              notes: Math.random() > 0.85 ? 'Örnek not detayı' : undefined,
              synced: true,
              updatedAt: logTime.getTime()
            });
          }
        }
      });
    }
    
    this.saveLogs(logs);
  }
};
