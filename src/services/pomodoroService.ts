export interface PomodoroSettings {
  workMinutes: number;
  shortBreakMinutes: number;
  longBreakMinutes: number;
  cyclesBeforeLong: number;
  totalSessions: number;     // 0 = unlimited
  autoStartBreak: boolean;
  autoStartWork: boolean;
}

export interface PomodoroSessionRecord {
  id: string;
  date: string;              // YYYY-MM-DD
  completedPomodoros: number;
  totalFocusMinutes: number;
}

const KEYS = {
  SETTINGS: 'optimum_pomodoro_settings',
  SESSIONS: 'optimum_pomodoro_sessions',
} as const;

const DEFAULT_SETTINGS: PomodoroSettings = {
  workMinutes: 25,
  shortBreakMinutes: 5,
  longBreakMinutes: 15,
  cyclesBeforeLong: 4,
  totalSessions: 0,
  autoStartBreak: false,
  autoStartWork: false,
};

const todayStr = (): string => new Date().toISOString().split('T')[0];

export const pomodoroService = {
  getSettings(): PomodoroSettings {
    try {
      const raw = localStorage.getItem(KEYS.SETTINGS);
      if (!raw) return { ...DEFAULT_SETTINGS };
      return { ...DEFAULT_SETTINGS, ...JSON.parse(raw) };
    } catch {
      return { ...DEFAULT_SETTINGS };
    }
  },

  saveSettings(settings: PomodoroSettings): void {
    localStorage.setItem(KEYS.SETTINGS, JSON.stringify(settings));
  },

  getSessionToday(): PomodoroSessionRecord | null {
    try {
      const raw = localStorage.getItem(KEYS.SESSIONS);
      if (!raw) return null;
      const sessions: PomodoroSessionRecord[] = JSON.parse(raw);
      return sessions.find(s => s.date === todayStr()) ?? null;
    } catch {
      return null;
    }
  },

  recordCompletedPomodoro(focusMinutes: number): void {
    try {
      const raw = localStorage.getItem(KEYS.SESSIONS);
      const sessions: PomodoroSessionRecord[] = raw ? JSON.parse(raw) : [];
      const today = todayStr();
      const idx = sessions.findIndex(s => s.date === today);
      if (idx >= 0) {
        sessions[idx].completedPomodoros += 1;
        sessions[idx].totalFocusMinutes += focusMinutes;
      } else {
        sessions.push({
          id: `pom_${Date.now()}`,
          date: today,
          completedPomodoros: 1,
          totalFocusMinutes: focusMinutes,
        });
      }
      // Keep only last 30 days
      localStorage.setItem(KEYS.SESSIONS, JSON.stringify(sessions.slice(-30)));
    } catch { /* silent */ }
  },

  resetTodaySession(): void {
    try {
      const raw = localStorage.getItem(KEYS.SESSIONS);
      if (!raw) return;
      const sessions: PomodoroSessionRecord[] = JSON.parse(raw);
      const today = todayStr();
      const filtered = sessions.filter(s => s.date !== today);
      localStorage.setItem(KEYS.SESSIONS, JSON.stringify(filtered));
    } catch { /* silent */ }
  },
};
