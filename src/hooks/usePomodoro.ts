import { useState, useEffect, useRef, useCallback } from 'react';
import { pomodoroService } from '../services/pomodoroService';
import type { PomodoroSettings, PomodoroSessionRecord } from '../services/pomodoroService';
import { playNotificationSound } from '../utils/audio';
import type { NotificationSoundType } from '../utils/audio';
import { useToast } from '../components/Toast';

export type PomodoroPhase = 'work' | 'short-break' | 'long-break';

export interface ActivePomodoroState {
  phase: PomodoroPhase;
  isRunning: boolean;
  timeLeft: number;
  completedCycles: number;
  endTime: number | null;
}

const STORAGE_KEY = 'optimum_active_pomodoro_state';

const PHASE_CONFIG = {
  'work': { title: '🎉 Pomodoro Tamamlandı!', body: 'Uzun mola zamanı. İyi dinlenmeler!', shortBody: 'Kısa mola zamanı. Nefes alın!' },
  'short-break': { title: '⏰ Mola Bitti!', body: 'Odaklanma zamanı. Haydi!' },
  'long-break': { title: '⏰ Mola Bitti!', body: 'Odaklanma zamanı. Haydi!' }
};

export function usePomodoro(notificationSound: NotificationSoundType) {
  const toast = useToast();
  const [settings, setSettings] = useState<PomodoroSettings>(() => pomodoroService.getSettings());
  const [phase, setPhase] = useState<PomodoroPhase>('work');
  const [timeLeft, setTimeLeft] = useState<number>(() => pomodoroService.getSettings().workMinutes * 60);
  const [isRunning, setIsRunning] = useState<boolean>(false);
  const [completedCycles, setCompletedCycles] = useState<number>(0);
  const [todayStat, setTodayStat] = useState<PomodoroSessionRecord | null>(() => pomodoroService.getSessionToday());

  const endTimeRef = useRef<number | null>(null);

  // Load state from localStorage on init
  useEffect(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const state: ActivePomodoroState = JSON.parse(raw);
        setPhase(state.phase);
        setCompletedCycles(state.completedCycles);
        
        if (state.isRunning && state.endTime) {
          const now = Date.now();
          const remaining = Math.max(0, Math.ceil((state.endTime - now) / 1000));
          if (remaining > 0) {
            setTimeLeft(remaining);
            setIsRunning(true);
            endTimeRef.current = state.endTime;
          } else {
            // Completed while away
            setTimeLeft(0);
            setIsRunning(false);
            endTimeRef.current = null;
            // Trigger completion after small delay so React is fully loaded
            setTimeout(() => handlePhaseEndDirect(state.phase, state.completedCycles), 100);
          }
        } else {
          setTimeLeft(state.timeLeft);
          setIsRunning(false);
          endTimeRef.current = null;
        }
      }
    } catch (e) {
      console.warn('Failed to parse active pomodoro state:', e);
    }
  }, []);

  // Save state to localStorage whenever it changes
  const saveState = useCallback((p: PomodoroPhase, running: boolean, time: number, cycles: number, end: number | null) => {
    try {
      const state: ActivePomodoroState = {
        phase: p,
        isRunning: running,
        timeLeft: time,
        completedCycles: cycles,
        endTime: end
      };
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {}
  }, []);

  const triggerNotification = useCallback(async (title: string, body: string) => {
    try {
      playNotificationSound(notificationSound);
      if ('Notification' in window && Notification.permission === 'granted') {
        let sent = false;
        if ('serviceWorker' in navigator && navigator.serviceWorker.controller) {
          try {
            const reg = await navigator.serviceWorker.ready;
            await reg.showNotification(title, { body });
            sent = true;
          } catch (swErr) {
            console.warn('SW notification failed in Pomodoro:', swErr);
          }
        }
        if (!sent) {
          new Notification(title, { body });
        }
      }
    } catch { /* silent */ }
  }, [notificationSound]);

  const handlePhaseEndDirect = useCallback((currPhase: PomodoroPhase, currCycles: number) => {
    const s = pomodoroService.getSettings();
    if (currPhase === 'work') {
      const newCC = currCycles + 1;
      pomodoroService.recordCompletedPomodoro(s.workMinutes);
      setCompletedCycles(newCC);
      setTodayStat(pomodoroService.getSessionToday());

      const goLong = newCC % s.cyclesBeforeLong === 0;

      if (goLong) {
        triggerNotification(PHASE_CONFIG['work'].title, PHASE_CONFIG['work'].body);
        toast.success(`🎉 ${s.cyclesBeforeLong} döngü tamamlandı! Uzun mola hak ettiniz.`);
        setPhase('long-break');
        setTimeLeft(s.longBreakMinutes * 60);
        setIsRunning(s.autoStartBreak);
        const end = s.autoStartBreak ? Date.now() + s.longBreakMinutes * 60 * 1000 : null;
        endTimeRef.current = end;
        saveState('long-break', s.autoStartBreak, s.longBreakMinutes * 60, newCC, end);
      } else {
        triggerNotification(PHASE_CONFIG['work'].title, PHASE_CONFIG['work'].shortBody);
        toast.info('✓ Pomodoro bitti. Kısa mola zamanı!');
        setPhase('short-break');
        setTimeLeft(s.shortBreakMinutes * 60);
        setIsRunning(s.autoStartBreak);
        const end = s.autoStartBreak ? Date.now() + s.shortBreakMinutes * 60 * 1000 : null;
        endTimeRef.current = end;
        saveState('short-break', s.autoStartBreak, s.shortBreakMinutes * 60, newCC, end);
      }
    } else {
      triggerNotification(PHASE_CONFIG['short-break'].title, PHASE_CONFIG['short-break'].body);
      toast.info('⏰ Mola bitti. Odaklanma zamanı!');
      setPhase('work');
      setTimeLeft(s.workMinutes * 60);
      setIsRunning(s.autoStartWork);
      const end = s.autoStartWork ? Date.now() + s.workMinutes * 60 * 1000 : null;
      endTimeRef.current = end;
      saveState('work', s.autoStartWork, s.workMinutes * 60, currCycles, end);
    }
  }, [toast, triggerNotification, saveState]);

  // Master tick check
  useEffect(() => {
    if (!isRunning) return;
    
    const intervalId = setInterval(() => {
      if (endTimeRef.current) {
        const remaining = Math.max(0, Math.ceil((endTimeRef.current - Date.now()) / 1000));
        if (remaining <= 0) {
          clearInterval(intervalId);
          setIsRunning(false);
          endTimeRef.current = null;
          setTimeLeft(0);
          handlePhaseEndDirect(phase, completedCycles);
        } else {
          setTimeLeft(remaining);
          // Periodically save state to keep time synchronized
          saveState(phase, true, remaining, completedCycles, endTimeRef.current);
        }
      }
    }, 1000);

    return () => clearInterval(intervalId);
  }, [isRunning, phase, completedCycles, handlePhaseEndDirect, saveState]);

  // Actions
  const handlePlayPause = useCallback(() => {
    setIsRunning(prev => {
      const nextRunning = !prev;
      if (nextRunning) {
        const end = Date.now() + timeLeft * 1000;
        endTimeRef.current = end;
        saveState(phase, true, timeLeft, completedCycles, end);
      } else {
        endTimeRef.current = null;
        saveState(phase, false, timeLeft, completedCycles, null);
      }
      return nextRunning;
    });
  }, [phase, timeLeft, completedCycles, saveState]);

  const handleReset = useCallback(() => {
    setIsRunning(false);
    endTimeRef.current = null;
    const s = pomodoroService.getSettings();
    let seconds = s.workMinutes * 60;
    if (phase === 'short-break') seconds = s.shortBreakMinutes * 60;
    else if (phase === 'long-break') seconds = s.longBreakMinutes * 60;

    setTimeLeft(seconds);
    saveState(phase, false, seconds, completedCycles, null);
  }, [phase, completedCycles, saveState]);

  const handleSkip = useCallback(() => {
    setIsRunning(false);
    endTimeRef.current = null;
    setTimeLeft(0);
    handlePhaseEndDirect(phase, completedCycles);
  }, [phase, completedCycles, handlePhaseEndDirect]);

  const handleUpdateSettings = useCallback((newSettings: PomodoroSettings) => {
    pomodoroService.saveSettings(newSettings);
    setSettings(newSettings);
    setIsRunning(false);
    endTimeRef.current = null;
    setPhase('work');
    setTimeLeft(newSettings.workMinutes * 60);
    setCompletedCycles(0);
    saveState('work', false, newSettings.workMinutes * 60, 0, null);
    toast.success('Pomodoro ayarları kaydedildi.');
  }, [saveState, toast]);

  return {
    settings,
    phase,
    timeLeft,
    isRunning,
    completedCycles,
    todayStat,
    handlePlayPause,
    handleReset,
    handleSkip,
    handleUpdateSettings
  };
}
