import { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import { 
  AlertCircle, ChevronRight, X, ChevronLeft, Calendar, Plus, Trash2, ZoomIn, ZoomOut, Maximize, Minimize,
  Download, Printer
} from 'lucide-react';
import type { Category, TimeLog, AppSettings } from '../services/storageService';


import { notificationService } from '../services/notificationService';
import { parseTimeToMinutes, generateSlots } from '../utils/timeUtils';
import { useToast } from './Toast';


interface DashboardProps {
  categories: Category[];
  logs: TimeLog[];
  settings: AppSettings;
  onLogAdd: (logData: Omit<TimeLog, 'id' | 'timestamp' | 'synced' | 'updatedAt'>) => Promise<TimeLog>;
  onLogDelete: (id: string) => Promise<void>;
  pendingLog: { slot: string; date: string } | null;
  clearPendingLog: () => void;
}



const DAYS_SHORT_TR = ['Pzt', 'Sal', 'Çar', 'Per', 'Cum', 'Cmt', 'Paz'];

export default function Dashboard({
  categories,
  logs,
  settings,
  onLogAdd,
  onLogDelete,
  pendingLog,
  clearPendingLog,
}: DashboardProps) {

  const toast = useToast();

  // Excel Grid Configuration & Filters
  const [currentAnchorDate, setCurrentAnchorDate] = useState<Date>(() => {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    return d;
  });
  

  // Multi-select Hierarchical Filters
  const [selectedGridCatIds, setSelectedGridCatIds] = useState<Set<string>>(new Set());
  const [selectedGridActIds, setSelectedGridActIds] = useState<Set<string>>(new Set());

  // Custom Range Slider Days Count (starts with 1 - Daily list view)
  const [customDaysCount, setCustomDaysCount] = useState<number>(1);
  const [localDaysCount, setLocalDaysCount] = useState<number>(1);

  const debounceTimeoutRef = useRef<any>(null);
  const debouncedSetDaysCount = useCallback((val: number) => {
    if (debounceTimeoutRef.current) {
      clearTimeout(debounceTimeoutRef.current);
    }
    debounceTimeoutRef.current = setTimeout(() => {
      setCustomDaysCount(val);
    }, 120);
  }, []);

  // Cleanup debounce on unmount
  useEffect(() => {
    return () => {
      if (debounceTimeoutRef.current) {
        clearTimeout(debounceTimeoutRef.current);
      }
    };
  }, []);

  // Cell logging modal state
  const [showLogModal, setShowLogModal] = useState<boolean>(false);
  const [activeCell, setActiveCell] = useState<{ slot: string; date: string } | null>(null);
  const [selectedLog, setSelectedLog] = useState<TimeLog | null>(null);
  const [tempSelectedActivity, setTempSelectedActivity] = useState<{ activity: any; category: Category } | null>(null);
  const [notes, setNotes] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedCategoryId, setSelectedCategoryId] = useState<string>('all');
  const [selectedDuration, setSelectedDuration] = useState<number>(settings.intervalMinutes);

  // Missed slots list
  const [missedSlots, setMissedSlots] = useState<{ slot: string; date: string }[]>([]);
  const [currentMissedIndex, setCurrentMissedIndex] = useState<number>(-1);

  const [notifPermission, setNotifPermission] = useState<string>('default');
  const [isBannerDismissed, setIsBannerDismissed] = useState<boolean>(() => sessionStorage.getItem('optimum_notif_banner_dismissed') === 'true');

  useEffect(() => {
    const checkPerm = async () => {
      const status = await notificationService.getPermissionStatus();
      setNotifPermission(status);
    };
    checkPerm();
    window.addEventListener('focus', checkPerm);
    return () => window.removeEventListener('focus', checkPerm);
  }, []);
  const [zoomScale, setZoomScale] = useState<number>(1.0);
  const [isFullscreen, setIsFullscreen] = useState<boolean>(false);
  const fullscreenWrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleFullscreenChange = () => {
      const isCurrentlyFullscreen = !!document.fullscreenElement;
      setIsFullscreen(isCurrentlyFullscreen);
      if (!isCurrentlyFullscreen) {
        if (screen.orientation && typeof screen.orientation.unlock === 'function') {
          try {
            screen.orientation.unlock();
          } catch(e) {}
        }
      }
    };
    document.addEventListener('fullscreenchange', handleFullscreenChange);
    return () => document.removeEventListener('fullscreenchange', handleFullscreenChange);
  }, []);
  const tableContainerRef = useRef<HTMLDivElement>(null);

  const [tableHeight, setTableHeight] = useState<number>(() => {
    try {
      const saved = localStorage.getItem('optimum_table_height');
      return saved ? parseInt(saved, 10) : 480;
    } catch {
      return 480;
    }
  });

  const isResizingRef = useRef(false);
  const startYRef = useRef(0);
  const startHeightRef = useRef(480);

  const handleMouseMove = useCallback((e: MouseEvent) => {
    if (!isResizingRef.current) return;
    const deltaY = e.clientY - startYRef.current;
    const newHeight = Math.max(220, Math.min(1000, startHeightRef.current + deltaY));
    setTableHeight(newHeight);
    try {
      localStorage.setItem('optimum_table_height', String(newHeight));
    } catch {}
  }, []);

  const handleMouseUp = useCallback(() => {
    isResizingRef.current = false;
    document.removeEventListener('mousemove', handleMouseMove);
    document.removeEventListener('mouseup', handleMouseUp);
    document.body.style.cursor = '';
    document.body.style.userSelect = '';
  }, [handleMouseMove]);

  const handleMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    isResizingRef.current = true;
    startYRef.current = e.clientY;
    startHeightRef.current = tableHeight;
    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
    document.body.style.cursor = 'row-resize';
    document.body.style.userSelect = 'none';
  };

  const handleTouchMove = useCallback((e: TouchEvent) => {
    if (!isResizingRef.current) return;
    e.preventDefault();
    const deltaY = e.touches[0].clientY - startYRef.current;
    const newHeight = Math.max(220, Math.min(1000, startHeightRef.current + deltaY));
    setTableHeight(newHeight);
    try {
      localStorage.setItem('optimum_table_height', String(newHeight));
    } catch {}
  }, []);

  const handleTouchEnd = useCallback(() => {
    isResizingRef.current = false;
    document.removeEventListener('touchmove', handleTouchMove);
    document.removeEventListener('touchend', handleTouchEnd);
    document.body.style.userSelect = '';
  }, [handleTouchMove]);

  const handleTouchStart = (e: React.TouchEvent) => {
    isResizingRef.current = true;
    startYRef.current = e.touches[0].clientY;
    startHeightRef.current = tableHeight;
    document.addEventListener('touchmove', handleTouchMove, { passive: false });
    document.addEventListener('touchend', handleTouchEnd);
    document.body.style.userSelect = 'none';
  };

  useEffect(() => {
    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      document.removeEventListener('touchmove', handleTouchMove);
      document.removeEventListener('touchend', handleTouchEnd);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };
  }, [handleMouseMove, handleMouseUp, handleTouchMove, handleTouchEnd]);

  const currentZoomScaleRef = useRef<number>(zoomScale);

  useEffect(() => {
    currentZoomScaleRef.current = zoomScale;
  }, [zoomScale]);

  useEffect(() => {
    const container = tableContainerRef.current;
    if (!container) return;

    let touchStartDist = 0;
    let initialScale = 1.0;
    let isPinching = false;

    const getDistance = (touches: TouchList) => {
      if (touches.length < 2) return 0;
      return Math.hypot(
        touches[0].clientX - touches[1].clientX,
        touches[0].clientY - touches[1].clientY
      );
    };

    const handleTouchStart = (e: TouchEvent) => {
      if (e.touches.length === 2) {
        touchStartDist = getDistance(e.touches);
        initialScale = currentZoomScaleRef.current;
        isPinching = true;
        
        if (e.cancelable) {
          e.preventDefault();
        }
      }
    };

    const handleTouchMove = (e: TouchEvent) => {
      if (isPinching && e.touches.length === 2) {
        const currentDist = getDistance(e.touches);
        if (touchStartDist > 0 && currentDist > 0) {
          const factor = currentDist / touchStartDist;
          const newScale = Math.min(1.4, Math.max(0.6, initialScale * factor));
          setZoomScale(Math.round(newScale * 100) / 100);
        }
        if (e.cancelable) {
          e.preventDefault();
        }
      }
    };

    const handleTouchEnd = () => {
      isPinching = false;
      touchStartDist = 0;
    };

    container.addEventListener('touchstart', handleTouchStart, { passive: false });
    container.addEventListener('touchmove', handleTouchMove, { passive: false });
    container.addEventListener('touchend', handleTouchEnd);
    container.addEventListener('touchcancel', handleTouchEnd);

    return () => {
      container.removeEventListener('touchstart', handleTouchStart);
      container.removeEventListener('touchmove', handleTouchMove);
      container.removeEventListener('touchend', handleTouchEnd);
      container.removeEventListener('touchcancel', handleTouchEnd);
    };
  }, []);
  // Hook notification payload
  useEffect(() => {
    if (pendingLog) {
      setActiveCell({ slot: pendingLog.slot, date: pendingLog.date });
      setSelectedLog(logs.find(l => l.date === pendingLog.date && l.timeSlot === pendingLog.slot) || null);
      setNotes('');
      setCurrentMissedIndex(-1);
      setShowLogModal(true);
    }
  }, [pendingLog, logs]);




  // Check for missed slots in the last 12 hours
  useEffect(() => {
    const checkMissed = () => {
      const startMin = parseTimeToMinutes(settings.startHour);
      const endMin = parseTimeToMinutes(settings.endHour);
      const missed: { slot: string; date: string }[] = [];
      const now = new Date();
      const lookBackMin = 12 * 60; // 12 hours limit

      for (let i = settings.intervalMinutes; i <= lookBackMin; i += settings.intervalMinutes) {
        const past = new Date(now.getTime() - i * 60 * 1000);
        const h = past.getHours();
        const m = past.getMinutes();
        const timeMin = h * 60 + m;

        if (m % settings.intervalMinutes !== 0) continue;

        if (isTimeInActiveRange(timeMin, startMin, endMin)) {
          const dateStr = past.toISOString().split('T')[0];
          const todayStr = now.toISOString().split('T')[0];

          if (dateStr === todayStr) {
            const hasLog = logs.some(l => {
              if (l.date !== dateStr) return false;
              const startMin = parseTimeToMinutes(l.timeSlot);
              const duration = l.durationMinutes || settings.intervalMinutes;
              const endMin = startMin + duration;
              return timeMin >= startMin && timeMin < endMin;
            });
            if (!hasLog) {
              const endIntervalMin = timeMin + settings.intervalMinutes;
              const eh = Math.floor((endIntervalMin % 1440) / 60);
              const em = (endIntervalMin % 1440) % 60;
              const slotStr = `${String(eh).padStart(2, '0')}:${String(em).padStart(2, '0')}`;
              missed.push({ slot: slotStr, date: dateStr });
            }
          }
        }
      }

      // Chronological sort
      missed.sort((a, b) => {
        const timeA = parseTimeToMinutes(a.slot) + (a.date === now.toISOString().split('T')[0] ? 1440 : 0);
        const timeB = parseTimeToMinutes(b.slot) + (b.date === now.toISOString().split('T')[0] ? 1440 : 0);
        return timeA - timeB;
      });

      setMissedSlots(missed);
    };

    checkMissed();
  }, [logs, settings]);




  const isTimeInActiveRange = (timeMinutes: number, startMin: number, endMin: number): boolean => {
    if (endMin >= startMin) {
      return timeMinutes >= startMin && timeMinutes <= endMin;
    } else {
      return timeMinutes >= startMin || timeMinutes <= endMin;
    }
  };

  // Date range calculations
  const getDatesToRender = (): Date[] => {
    const dates: Date[] = [];
    const anchor = new Date(currentAnchorDate);
    anchor.setHours(0, 0, 0, 0);
    
    for (let i = 0; i < customDaysCount; i++) {
      const d = new Date(anchor);
      d.setDate(anchor.getDate() - i);
      dates.push(d);
    }
    
    const sorted = dates.sort((a, b) => b.getTime() - a.getTime());
    
    // Filter out future dates
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return sorted.filter(d => d.getTime() <= today.getTime());
  };

  const getGridIntervalMinutes = (): number => {
    if (settings.intervalMinutes < 15) {
      return 15;
    }
    return settings.intervalMinutes;
  };

  const datesToRender = getDatesToRender();
  const rawSlots = generateSlots(settings.startHour, settings.endHour, getGridIntervalMinutes());
  const timeSlots = rawSlots.slice(1);

  // Navigate anchor date
  const handleNavigateAnchor = (direction: 'prev' | 'next') => {
    const newAnchor = new Date(currentAnchorDate);
    const daysShift = customDaysCount;
    newAnchor.setDate(currentAnchorDate.getDate() + (direction === 'next' ? daysShift : -daysShift));
    setCurrentAnchorDate(newAnchor);
  };

  const isNextPeriodFuture = (): boolean => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return currentAnchorDate >= today;
  };



  const formatDateStr = (date: Date): string => {
    return date.toISOString().split('T')[0];
  };

  const formatPeriodLabel = (): string => {
    if (datesToRender.length === 0) return 'Tarih seçilmedi';
    
    if (customDaysCount === 1) {
      return currentAnchorDate.toLocaleDateString('tr-TR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
    }
    
    const sortedAsc = [...datesToRender].sort((a, b) => a.getTime() - b.getTime());
    const oldest = sortedAsc[0];
    const newest = sortedAsc[sortedAsc.length - 1];
    return `${oldest.getDate()} ${oldest.toLocaleString('tr-TR', { month: 'short' })} - ${newest.getDate()} ${newest.toLocaleString('tr-TR', { month: 'short' })} ${newest.getFullYear()} (${customDaysCount} Gün)`;
  };

  // Pre-index logs by date and slot for O(1) rendering performance
  const logsMap = useMemo(() => {
    const map = new Map<string, TimeLog>();
    const step = getGridIntervalMinutes();
    logs.forEach(l => {
      const startMin = parseTimeToMinutes(l.timeSlot);
      const duration = l.durationMinutes || step;
      // Calculate all slots covered by this log entry
      for (let min = startMin; min < startMin + duration; min += step) {
        const totalMin = min % 1440;
        const h = Math.floor(totalMin / 60);
        const m = totalMin % 60;
        const slotKey = `${l.date}_${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
        map.set(slotKey, l);
      }
    });
    return map;
  }, [logs, settings.intervalMinutes]);

  // Cell Interaction
  const getLogForCell = useCallback((dateStr: string, cellSlotStr: string): TimeLog | undefined => {
    const endMin = parseTimeToMinutes(cellSlotStr);
    const step = getGridIntervalMinutes();
    let startMin = endMin - step;
    let targetDateStr = dateStr;
    
    if (startMin < 0) {
      startMin += 1440;
      const dateObj = new Date(dateStr);
      dateObj.setDate(dateObj.getDate() - 1);
      targetDateStr = dateObj.toISOString().split('T')[0];
    }
    
    const h = Math.floor(startMin / 60);
    const m = startMin % 60;
    const startSlotStr = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
    
    return logsMap.get(`${targetDateStr}_${startSlotStr}`);
  }, [logsMap, settings.intervalMinutes]);

  const handleExportExcel = () => {
    const sortedLogs = [...logs].sort((a, b) => {
      if (a.date !== b.date) return a.date.localeCompare(b.date);
      return a.timeSlot.localeCompare(b.timeSlot);
    });

    let csvContent = '\uFEFFDate;Time Slot;Category;Activity Code;Activity Name;Duration (Min);Notes\n';
    sortedLogs.forEach(l => {
      const row = [
        l.date,
        l.timeSlot,
        l.categoryName,
        l.activityCode,
        l.activityName,
        l.durationMinutes || settings.intervalMinutes,
        (l.notes || '').replace(/"/g, '""').replace(/\n/g, ' ')
      ];
      csvContent += row.map(val => `"${val}"`).join(';') + '\n';
    });

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', `optimum_zaman_paneli_raporu_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    toast.success('Rapor CSV olarak başarıyla indirildi!');
  };

  const handlePrint = () => {
    window.print();
  };

  const handleCellClick = (slot: string, date: string, existingLog: TimeLog | null) => {
    setActiveCell({ slot, date });
    if (existingLog) {
      setSelectedLog(existingLog);
      setNotes(existingLog.notes || '');
      setSelectedDuration(existingLog.durationMinutes || getGridIntervalMinutes());
      setTempSelectedActivity({
        activity: { id: existingLog.activityId, code: existingLog.activityCode, name: existingLog.activityName },
        category: { id: existingLog.categoryId, name: existingLog.categoryName, color: existingLog.categoryColor, textColor: '#fff', activities: [] }
      });
    } else {
      setSelectedLog(null);
      setNotes('');
      setSelectedDuration(getGridIntervalMinutes());
      setTempSelectedActivity(null);
    }
    setShowLogModal(true);
  };

  const handleSelectActivity = async (activity: { id: string; code: string; name: string }, category: Category) => {
    if (!activeCell) return;

    // Delete old log if we are editing (to prevent duplicates due to duration shifts)
    if (selectedLog) {
      await onLogDelete(selectedLog.id);
    }

    // Calculate backward start slot and date
    const endMin = parseTimeToMinutes(activeCell.slot);
    let startMin = endMin - selectedDuration;
    let logDate = activeCell.date;

    if (startMin < 0) {
      startMin += 1440;
      const dateObj = new Date(activeCell.date);
      dateObj.setDate(dateObj.getDate() - 1);
      logDate = dateObj.toISOString().split('T')[0];
    }

    const h = Math.floor(startMin / 60);
    const m = startMin % 60;
    const logSlot = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;

    await onLogAdd({
      date: logDate,
      timeSlot: logSlot,
      activityId: activity.id,
      activityCode: activity.code,
      activityName: activity.name,
      categoryId: category.id,
      categoryName: category.name,
      categoryColor: category.color,
      notes: notes.trim() || undefined,
      durationMinutes: selectedDuration,
    });

    if (currentMissedIndex > -1 && currentMissedIndex < missedSlots.length - 1) {
      const nextIdx = currentMissedIndex + 1;
      setCurrentMissedIndex(nextIdx);
      const nextDate = missedSlots[nextIdx].date;
      const nextSlot = missedSlots[nextIdx].slot;
      setActiveCell({ slot: nextSlot, date: nextDate });
      
      const nextLog = getLogForCell(nextDate, nextSlot) || null;
      setSelectedLog(nextLog);
      if (nextLog) {
        setTempSelectedActivity({
          activity: { id: nextLog.activityId, code: nextLog.activityCode, name: nextLog.activityName },
          category: { id: nextLog.categoryId, name: nextLog.categoryName, color: nextLog.categoryColor, textColor: '#fff', activities: [] }
        });
        setNotes(nextLog.notes || '');
      } else {
        setTempSelectedActivity(null);
        setNotes('');
      }
    } else {
      setShowLogModal(false);
      setActiveCell(null);
      setSelectedLog(null);
      setTempSelectedActivity(null);
      setCurrentMissedIndex(-1);
      clearPendingLog();
    }
  };

  const handleSaveClick = async () => {
    if (!tempSelectedActivity) return;
    await handleSelectActivity(tempSelectedActivity.activity, tempSelectedActivity.category);
  };

  const handleDeleteLogClick = async () => {
    if (selectedLog) {
      await onLogDelete(selectedLog.id);
      setShowLogModal(false);
      setActiveCell(null);
      setSelectedLog(null);
      setTempSelectedActivity(null);
    }
  };

  const handleLogMissedSequence = () => {
    if (missedSlots.length > 0) {
      setCurrentMissedIndex(0);
      setActiveCell({ slot: missedSlots[0].slot, date: missedSlots[0].date });
      setSelectedLog(logs.find(l => l.date === missedSlots[0].date && l.timeSlot === missedSlots[0].slot) || null);
      setNotes('');
      setShowLogModal(true);
    }
  };

  const handleToggleFullscreen = async () => {
    const wrapper = fullscreenWrapperRef.current;
    if (!wrapper) return;

    if (!document.fullscreenElement) {
      try {
        if (wrapper.requestFullscreen) {
          await wrapper.requestFullscreen();
        }
        if (screen.orientation && typeof screen.orientation.lock === 'function') {
          await screen.orientation.lock('landscape').catch((e) => {
            console.warn('Screen orientation lock failed:', e);
          });
        }
      } catch (err) {
        console.error('Fullscreen request failed:', err);
      }
      setIsFullscreen(true);
    } else {
      if (document.exitFullscreen) {
        await document.exitFullscreen();
      }
      setIsFullscreen(false);
    }
  };

  const requestAndVerifyPermission = async (): Promise<boolean> => {
    if (!('Notification' in window)) {
      toast.error('Tarayıcınız bildirim özelliğini desteklemiyor veya güvenli olmayan (HTTP) bir bağlantı üzerinden bağlanıyorsunuz. Bildirimleri kullanabilmek için lütfen "http://localhost:5173" veya bir "HTTPS" bağlantısı kullanın.');
      return false;
    }

    if (Notification.permission === 'denied') {
      toast.error('Bildirim izni daha önce engellenmiş. Bildirimleri almak için lütfen tarayıcınızın adres çubuğundaki kilit simgesine (kilit/kamera/ayar ikonu) tıklayarak Bildirim İznini "İzin Ver" (Allow) olarak değiştirin ve sayfayı yenileyin.');
      return false;
    }

    const granted = await notificationService.requestPermission();
    const status = await notificationService.getPermissionStatus();
    setNotifPermission(status);

    if (!granted) {
      toast.error('Bildirim izni reddedildi. Hatırlatıcıları almak için izne onay vermeniz gerekmektedir.');
    }

    return granted;
  };




  const startSlotStr = useMemo(() => {
    if (!activeCell) return '';
    const endMin = parseTimeToMinutes(activeCell.slot);
    let startMin = endMin - selectedDuration;
    if (startMin < 0) {
      startMin += 1440;
    }
    const h = Math.floor(startMin / 60);
    const m = startMin % 60;
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
  }, [activeCell?.slot, selectedDuration]);

  // Filter activities
  const filteredActivities: { category: Category; activity: any }[] = [];
  categories.forEach(cat => {
    if (selectedCategoryId === 'all' || selectedCategoryId === cat.id) {
      cat.activities.forEach(act => {
        const matchesSearch = 
          act.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
          act.code.toLowerCase().includes(searchQuery.toLowerCase());
        if (matchesSearch) {
          filteredActivities.push({ category: cat, activity: act });
        }
      });
    }
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* 0. Notification Permission Warning Banner */}
      {settings.notificationsEnabled && notifPermission !== 'granted' && !isBannerDismissed && (
        <div 
          className="glass-panel animate-scale-in" 
          style={{ 
            padding: '12px 20px', 
            background: 'rgba(239, 68, 68, 0.08)', 
            border: '1px solid rgba(239, 68, 68, 0.3)',
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '12px',
            borderRadius: '16px'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <AlertCircle size={18} color="#ef4444" />
            <span style={{ fontSize: '0.85rem', color: '#fca5a5', fontWeight: '500' }}>
              Hatırlatıcılar açık fakat tarayıcınızın bildirim izni eksik! Bildirimleri alamayacaksınız.
            </span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <button 
              type="button"
              className="btn" 
              onClick={async () => {
                const granted = await requestAndVerifyPermission();
                if (granted) {
                  try {
                    let sent = false;
                    if ('serviceWorker' in navigator && navigator.serviceWorker.controller) {
                      try {
                        const reg = await navigator.serviceWorker.ready;
                        await reg.showNotification('Optimum Flow', {
                          body: 'Tebrikler! Bildirimler başarıyla etkinleştirildi.',
                        });
                        sent = true;
                      } catch (swErr) {
                        console.warn('SW success notification failed:', swErr);
                      }
                    }
                    if (!sent && 'Notification' in window) {
                      new Notification('Optimum Flow', { body: 'Tebrikler! Bildirimler başarıyla etkinleştirildi.' });
                    }
                    notificationService.rescheduleNotifications();
                  } catch (e) {
                    console.error(e);
                  }
                }
              }}
              style={{ 
                padding: '6px 12px', 
                fontSize: '0.78rem', 
                background: '#ef4444', 
                color: '#fff', 
                borderRadius: '8px',
                border: 'none',
                fontWeight: '700',
                cursor: 'pointer'
              }}
            >
              İzni Etkinleştir
            </button>
            <button
              type="button"
              onClick={() => {
                setIsBannerDismissed(true);
                sessionStorage.setItem('optimum_notif_banner_dismissed', 'true');
              }}
              style={{
                background: 'none',
                border: 'none',
                color: '#ef4444',
                cursor: 'pointer',
                padding: '4px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                borderRadius: '50%',
                transition: 'background 0.2s',
              }}
              onMouseEnter={e => { e.currentTarget.style.background = 'rgba(239, 68, 68, 0.15)'; }}
              onMouseLeave={e => { e.currentTarget.style.background = 'none'; }}
              title="Kapat"
            >
              <X size={16} />
            </button>
          </div>
        </div>
      )}
      
      {/* Title Section */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', margin: '4px 0 12px 0' }}>
        <h1 style={{ fontSize: '1.8rem', fontWeight: '800', fontFamily: 'Outfit, sans-serif', color: '#fff' }}>
          Zaman Paneli
        </h1>
      </div>

      {/* Daily Productive Target & Streak Widget has been moved to Profile Page */}





      {/* Missed slots notification bar */}
      {missedSlots.length > 0 && (
        <div 
          className="glass-panel animate-slide-up" 
          style={{ 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center', 
            padding: '14px 20px', 
            borderColor: 'rgba(239, 68, 68, 0.3)',
            background: 'rgba(239, 68, 68, 0.08)' 
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <AlertCircle color="#ef4444" size={20} />
            <span style={{ fontSize: '0.85rem' }}>
              Doldurulmamış <strong>{missedSlots.length}</strong> zaman dilimi var.
            </span>
          </div>
          <button className="btn btn-primary" onClick={handleLogMissedSequence} style={{ padding: '6px 14px', fontSize: '0.75rem', borderRadius: '8px' }}>
            Doldur
            <ChevronRight size={14} />
          </button>
        </div>
      )}



      {/* 2. Hierarchical Category Legend & Filters (Visual Pill Style - Multi-Select V4) */}
      <div className="glass-panel" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <div>
          <h3 style={{ fontSize: '1.05rem', fontWeight: '700', color: 'var(--color-primary)', fontFamily: 'Outfit' }}>
            Zaman Çizelgesi Takip Matrisi
          </h3>
          <p style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', marginTop: '4px' }}>
            Kategori Göstergeleri (Çoklu filtrelemek için tıklayın)
          </p>
        </div>

        {/* Main Category Pills — max 2 rows, horizontal scroll */}
        <div className="pills-scroll-wrapper">
          <div className="pills-grid-2">
            <button
              className={`legend-pill ${selectedGridCatIds.size === 0 ? 'legend-pill-active' : ''}`}
              onClick={() => {
                setSelectedGridCatIds(new Set());
                setSelectedGridActIds(new Set());
              }}
              style={{
                '--pill-color': 'var(--color-primary)',
                '--pill-bg-glow': 'var(--color-primary-glow)',
                '--pill-bg-shadow': 'rgba(139, 92, 246, 0.15)',
              } as React.CSSProperties}
            >
              <span className="legend-pill-circle" />
              Tümü
            </button>

            {categories.map(cat => {
              const isSelected = selectedGridCatIds.has(cat.id);
              return (
                <button
                  key={cat.id}
                  className={`legend-pill ${isSelected ? 'legend-pill-active' : ''}`}
                  onClick={() => {
                    const newCats = new Set(selectedGridCatIds);
                    if (newCats.has(cat.id)) {
                      newCats.delete(cat.id);
                    } else {
                      newCats.add(cat.id);
                    }
                    setSelectedGridCatIds(newCats);
                    setSelectedGridActIds(new Set());
                  }}
                  style={{
                    '--pill-color': cat.color,
                    '--pill-bg-glow': `${cat.color}12`,
                    '--pill-bg-shadow': `${cat.color}15`,
                  } as React.CSSProperties}
                >
                  <span className="legend-pill-circle" />
                  {cat.name}
                </button>
              );
            })}
          </div>
        </div>

        {/* Sub-activity Dependent Pills */}
        <div 
          className="animate-slide-up" 
          style={{ 
            borderTop: '1px solid var(--color-border)', 
            paddingTop: '14px', 
            display: 'flex', 
            flexDirection: 'column', 
            gap: '8px' 
          }}
        >
          <span style={{ fontSize: '0.78rem', color: 'var(--color-text-secondary)', fontWeight: '600' }}>
            Alt Başlıklar (Seçilen Kategorilerin İşleri):
          </span>
          {/* Sub-activity pills — max 3 rows, horizontal scroll */}
          <div className="pills-scroll-wrapper">
            <div className="pills-grid-3" style={{ alignItems: 'start' }}>
              <button
                onClick={() => setSelectedGridActIds(new Set())}
                style={{
                  padding: '6px 14px',
                  borderRadius: '16px',
                  fontSize: '0.8rem',
                  fontWeight: '600',
                  cursor: 'pointer',
                  border: '1px solid var(--color-border)',
                  background: selectedGridActIds.size === 0 ? 'var(--color-primary)' : 'rgba(255,255,255,0.02)',
                  color: '#fff',
                  transition: 'all 0.15s ease',
                  whiteSpace: 'nowrap',
                }}
              >
                Tümü
              </button>
              {categories
                .filter(cat => selectedGridCatIds.size === 0 || selectedGridCatIds.has(cat.id))
                .flatMap(cat =>
                  cat.activities.map(act => {
                    const isActSelected = selectedGridActIds.has(act.id);
                    return (
                      <button
                        key={act.id}
                        onClick={() => {
                          const newActs = new Set(selectedGridActIds);
                          if (newActs.has(act.id)) {
                            newActs.delete(act.id);
                          } else {
                            newActs.add(act.id);
                          }
                          setSelectedGridActIds(newActs);
                        }}
                        style={{
                          padding: '6px 14px',
                          borderRadius: '16px',
                          fontSize: '0.8rem',
                          fontWeight: '600',
                          cursor: 'pointer',
                          border: `1px solid ${isActSelected ? cat.color : 'var(--color-border)'}`,
                          background: isActSelected ? `${cat.color}15` : 'rgba(255,255,255,0.02)',
                          color: '#fff',
                          transition: 'all 0.15s ease',
                          whiteSpace: 'nowrap',
                        }}
                      >
                        [{act.code}] {act.name}
                      </button>
                    );
                  })
                )}
            </div>
          </div>
        </div>
      </div>

      {/* 3 & 4. Fullscreen Wrapper (V11) */}
      <div 
        ref={fullscreenWrapperRef} 
        style={isFullscreen ? { 
          position: 'fixed', 
          top: 0, 
          left: 0, 
          width: '100vw', 
          height: '100vh', 
          background: '#040711', 
          zIndex: 9999, 
          padding: '24px', 
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
          gap: '16px'
        } : { 
          display: 'flex', 
          flexDirection: 'column', 
          gap: '16px',
          width: '100%'
        }}
      >
        {/* 3. Range Selector & Excel Navigation Header */}
        <div 
          className="glass-panel" 
          style={{ 
            padding: '14px 20px', 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '12px' 
          }}
        >

        {/* Actions (Excel, PDF Print) */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '4px 8px' }}>
          <button 
            type="button"
            className="btn btn-secondary" 
            onClick={handleExportExcel}
            style={{ 
              padding: '8px 16px', 
              fontSize: '0.8rem', 
              borderRadius: '8px', 
              display: 'flex', 
              alignItems: 'center', 
              gap: '6px',
              border: '1px solid rgba(255, 255, 255, 0.08)'
            }}
            title="Excel (CSV) Olarak İndir"
          >
            <Download size={14} />
            <span style={{ fontFamily: 'Outfit, sans-serif', fontWeight: '600' }}>Excel</span>
          </button>
          <button 
            type="button"
            className="btn btn-secondary" 
            onClick={handlePrint}
            style={{ 
              padding: '8px 16px', 
              fontSize: '0.8rem', 
              borderRadius: '8px', 
              display: 'flex', 
              alignItems: 'center', 
              gap: '6px',
              border: '1px solid rgba(255, 255, 255, 0.08)'
            }}
            title="Yazdır / PDF Kaydet"
          >
            <Printer size={14} />
            <span style={{ fontFamily: 'Outfit, sans-serif', fontWeight: '600' }}>Yazdır (PDF)</span>
          </button>
        </div>

        {/* Custom date range slider (V4) */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', minWidth: '220px', flex: 1, padding: '4px 8px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', fontWeight: '600' }}>
              Zaman Aralığı (Gün Sayısı):
            </span>
            <span style={{ fontSize: '0.85rem', color: 'var(--color-primary)', fontWeight: '800', fontFamily: 'Outfit' }}>
              {localDaysCount} Gün
            </span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>1 G</span>
            <input 
              type="range" 
              min={1} 
              max={60} 
              value={localDaysCount} 
              onChange={(e) => {
                const val = Number(e.target.value);
                setLocalDaysCount(val);
                debouncedSetDaysCount(val);
              }}
              style={{ 
                flex: 1, 
                height: '6px', 
                borderRadius: '3px',
                background: 'rgba(255,255,255,0.1)',
                outline: 'none',
                cursor: 'pointer',
                accentColor: 'var(--color-primary)'
              }}
            />
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>60 G</span>
          </div>
        </div>

        {/* Zoom & Fullscreen Controls (V9 & V11) */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '4px 8px', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <button 
              type="button"
              className="btn btn-secondary" 
              onClick={() => setZoomScale(prev => Math.max(0.6, prev - 0.1))} 
              style={{ padding: '6px 10px', borderRadius: '8px' }}
              title="Uzaklaştır (%10 azalt)"
            >
              <ZoomOut size={14} />
            </button>
            
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setZoomScale(1.0)}
              style={{ padding: '6px 12px', fontSize: '0.8rem', borderRadius: '8px', fontWeight: '700', minWidth: '55px', textAlign: 'center' }}
              title="Varsayılana Sıfırla (%100)"
            >
              %{Math.round(zoomScale * 100)}
            </button>

            <button 
              type="button"
              className="btn btn-secondary" 
              onClick={() => setZoomScale(prev => Math.min(1.4, prev + 0.1))} 
              style={{ padding: '6px 10px', borderRadius: '8px' }}
              title="Yakınlaştır (%10 artır)"
            >
              <ZoomIn size={14} />
            </button>
          </div>

          <button 
            type="button"
            className="btn btn-secondary" 
            onClick={handleToggleFullscreen} 
            style={{ padding: '6px 12px', borderRadius: '8px', display: 'flex', alignItems: 'center', gap: '4px', border: isFullscreen ? '1px solid var(--color-primary)' : '1px solid rgba(255,255,255,0.08)' }}
            title={isFullscreen ? "Tam Ekrandan Çık" : "Tam Ekran Yap"}
          >
            {isFullscreen ? <Minimize size={14} color="var(--color-primary)" /> : <Maximize size={14} />}
            <span style={{ fontSize: '0.78rem', fontWeight: '700', fontFamily: 'Outfit', color: isFullscreen ? 'var(--color-primary)' : '#fff' }}>
              {isFullscreen ? "Küçült" : "Tam Ekran"}
            </span>
          </button>
        </div>
      </div>

      {/* Centered Period Header with Conditional Date Navigation */}
      <div 
        className="glass-panel" 
        style={{ 
          padding: '8px 20px', 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center', 
          background: 'linear-gradient(135deg, rgba(255,255,255,0.03) 0%, rgba(255,255,255,0.01) 100%)',
          borderColor: 'rgba(255, 255, 255, 0.05)',
          boxShadow: '0 4px 30px rgba(0, 0, 0, 0.1)',
          backdropFilter: 'blur(5px)',
          margin: '8px 0',
          width: '100%'
        }}
      >
        {customDaysCount === 1 ? (
          <button 
            type="button"
            className="btn btn-secondary" 
            onClick={() => handleNavigateAnchor('prev')} 
            style={{ 
              padding: '6px 10px', 
              borderRadius: '8px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              border: '1px solid rgba(255,255,255,0.08)'
            }} 
            title="Önceki Gün"
          >
            <ChevronLeft size={16} />
          </button>
        ) : (
          <div style={{ width: '36px', height: '32px' }} />
        )}

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '1.05rem', fontWeight: '800', fontFamily: 'Outfit, sans-serif', color: '#fff' }}>
          <Calendar size={18} color="var(--color-primary)" />
          <span style={{ textShadow: '0 0 10px rgba(99, 102, 241, 0.3)' }}>{formatPeriodLabel()}</span>
        </div>

        {customDaysCount === 1 ? (
          <button 
            type="button"
            className="btn btn-secondary" 
            onClick={() => handleNavigateAnchor('next')} 
            disabled={isNextPeriodFuture()}
            style={{ 
              padding: '6px 10px', 
              borderRadius: '8px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              opacity: isNextPeriodFuture() ? 0.5 : 1,
              cursor: isNextPeriodFuture() ? 'not-allowed' : 'pointer',
              border: '1px solid rgba(255,255,255,0.08)'
            }}
            title="Sonraki Gün"
          >
            <ChevronRight size={16} />
          </button>
        ) : (
          <div style={{ width: '36px', height: '32px' }} />
        )}
      </div>

      {/* 4. The Main Excel Grid Table / Daily Detail List Table */}
      <div 
        ref={tableContainerRef}
        className="glass-panel" 
        style={isFullscreen ? { 
          padding: '12px', 
          overflowX: 'auto', 
          width: '100%',
          flex: 1,
          minHeight: 0,
          overflowY: 'auto'
        } : { 
          padding: '12px', 
          overflowX: 'auto', 
          width: '100%',
          height: `${tableHeight}px`,
          minHeight: '220px',
          maxHeight: '85vh',
          overflowY: 'auto',
          borderRadius: '20px 20px 0 0',
          borderBottom: 'none'
        }}
      >
        {customDaysCount === 1 ? (
          <table 
            className="excel-table"
            style={{ 
              width: '100%', 
              minWidth: '100%', 
              tableLayout: 'fixed' 
            }}
          >
            <thead>
              <tr style={{ background: '#090d16' }}>
                <th style={{ width: '28%', padding: `${Math.round(10 * zoomScale)}px ${Math.round(12 * zoomScale)}px`, fontSize: `${0.85 * zoomScale}rem`, color: 'var(--color-text-secondary)', borderBottom: '2px solid var(--color-border)', textAlign: 'left' }}>
                  Saat
                </th>
                <th style={{ width: '32%', padding: `${Math.round(10 * zoomScale)}px ${Math.round(12 * zoomScale)}px`, fontSize: `${0.85 * zoomScale}rem`, color: 'var(--color-text-secondary)', borderBottom: '2px solid var(--color-border)', textAlign: 'left' }}>
                  Kategori
                </th>
                <th style={{ width: '40%', padding: `${Math.round(10 * zoomScale)}px ${Math.round(12 * zoomScale)}px`, fontSize: `${0.85 * zoomScale}rem`, color: 'var(--color-text-secondary)', borderBottom: '2px solid var(--color-border)', textAlign: 'left' }}>
                  Alt Kategori
                </th>
              </tr>
            </thead>
            <tbody>
              {(() => {
                const isTodayAnchor = formatDateStr(currentAnchorDate) === formatDateStr(new Date());
                const nowMin = new Date().getHours() * 60 + new Date().getMinutes();
                const displayedSlots = timeSlots.filter(slot => {
                  if (!isTodayAnchor) return true;
                  return parseTimeToMinutes(slot) <= nowMin;
                });

                return displayedSlots.map((slot) => {
                  const dateStr = formatDateStr(currentAnchorDate);
                  const log = getLogForCell(dateStr, slot);
                  const isFuture = currentAnchorDate.getTime() + parseTimeToMinutes(slot) * 60 * 1000 > Date.now();


                // Filter visibility check
                const isFilteredOut = log && (
                  (selectedGridCatIds.size > 0 && !selectedGridCatIds.has(log.categoryId)) ||
                  (selectedGridActIds.size > 0 && !selectedGridActIds.has(log.activityId))
                );

                const todayStr = new Date().toISOString().split('T')[0];
                const isEditable = dateStr === todayStr && !isFuture;

                return (
                  <tr 
                    key={slot}
                    className={isEditable ? "grid-cell" : ""}
                    onClick={() => isEditable && handleCellClick(slot, dateStr, log || null)}
                    style={{ 
                      cursor: isEditable ? 'pointer' : 'default',
                      opacity: isFuture ? 0.35 : (isEditable ? 1 : 0.8),
                      background: 'rgba(255,255,255,0.01)',
                      transition: 'all 0.15s ease'
                    }}
                  >
                    {/* 1. Saat */}
                    <td 
                      className="excel-cell-capsule"
                      style={{ 
                        padding: `${Math.round(10 * zoomScale)}px ${Math.round(12 * zoomScale)}px`, 
                        fontSize: `${0.85 * zoomScale}rem`, 
                        fontWeight: '700', 
                        color: 'var(--color-text-primary)'
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: `${Math.round(8 * zoomScale)}px` }}>
                        <span style={{ color: 'var(--color-primary)' }}>{slot}</span>
                      </div>
                    </td>

                    {/* 2. Kategori */}
                    <td className="excel-cell-capsule" style={{ padding: `${Math.round(8 * zoomScale)}px ${Math.round(12 * zoomScale)}px` }}>
                      {log ? (
                        <span 
                          style={{
                            background: isFilteredOut ? '#1f2937' : log.categoryColor,
                            color: isFilteredOut ? '#4b5563' : '#fff',
                            padding: `${Math.round(6 * zoomScale)}px ${Math.round(12 * zoomScale)}px`,
                            borderRadius: `${Math.round(12 * zoomScale)}px`,
                            fontSize: `${0.8 * zoomScale}rem`,
                            fontWeight: '800',
                            display: 'inline-block',
                            boxShadow: isFilteredOut ? 'none' : `0 2px 6px ${log.categoryColor}25`,
                            opacity: isFilteredOut ? 0.15 : 1,
                            transition: 'all 0.15s ease'
                          }}
                        >
                          {log.categoryName}
                        </span>
                      ) : (
                        <span style={{ fontSize: `${0.8 * zoomScale}rem`, color: 'var(--color-text-muted)', fontStyle: 'italic' }}>
                          Kayıt Yok
                        </span>
                      )}
                    </td>

                    {/* 3. Alt Kategori */}
                    <td className="excel-cell-capsule" style={{ padding: `${Math.round(8 * zoomScale)}px ${Math.round(12 * zoomScale)}px` }}>
                      {log ? (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: `${Math.round(2 * zoomScale)}px`, opacity: isFilteredOut ? 0.15 : 1, transition: 'all 0.15s ease' }}>
                          <span style={{ fontSize: `${0.85 * zoomScale}rem`, fontWeight: '700', color: '#fff' }}>
                            [{log.activityCode}] {log.activityName}
                          </span>
                          {log.notes && (
                            <span style={{ fontSize: `${0.72 * zoomScale}rem`, color: 'var(--color-text-secondary)', fontStyle: 'italic' }}>
                              Not: {log.notes}
                            </span>
                          )}
                        </div>
                      ) : (
                        <div style={{ fontSize: `${0.8 * zoomScale}rem`, color: 'rgba(255,255,255,0.15)', display: 'flex', alignItems: 'center', gap: `${Math.round(6 * zoomScale)}px` }}>
                          {!isFuture && isEditable && (
                            <>
                              <Plus size={Math.round(12 * zoomScale)} />
                              <span>Yeni kayıt eklemek için dokunun...</span>
                            </>
                          )}
                        </div>
                      )}
                    </td>
                  </tr>
                );
              });
            })()}
          </tbody>
          </table>
        ) : (
          <table 
            className="excel-table"
            style={{ 
              width: '100%', 
              minWidth: `${(75 + datesToRender.length * 95) * zoomScale}px`,
              tableLayout: 'fixed',
              borderCollapse: 'separate',
              borderSpacing: `${4 * zoomScale}px ${6 * zoomScale}px`
            }}
          >
          <thead>
            <tr>
              {/* Top-left empty corner */}
              <th 
                style={{ 
                  width: `${75 * zoomScale}px`, 
                  minWidth: `${75 * zoomScale}px`,
                  padding: `${Math.round(10 * zoomScale)}px`, 
                  fontSize: `${0.8 * zoomScale}rem`, 
                  color: 'var(--color-text-secondary)',
                  position: 'sticky',
                  top: 0,
                  left: 0,
                  background: '#090d16',
                  zIndex: 10,
                  boxShadow: '2px 0 5px rgba(0, 0, 0, 0.4)'
                }}
              >
                Saat
              </th>
              {datesToRender.map((day, idx) => {
                const isToday = formatDateStr(day) === formatDateStr(new Date());
                const dayOfWeekStr = DAYS_SHORT_TR[day.getDay() === 0 ? 6 : day.getDay() - 1];
                return (
                  <th 
                    key={idx}
                    style={{ 
                      width: `${95 * zoomScale}px`,
                      minWidth: `${95 * zoomScale}px`,
                      padding: `${Math.round(10 * zoomScale)}px ${Math.round(6 * zoomScale)}px`,
                      fontSize: `${0.85 * zoomScale}rem`,
                      fontWeight: '700',
                      position: 'sticky',
                      top: 0,
                      background: '#090d16',
                      zIndex: 5,
                      color: isToday ? 'var(--color-primary)' : 'var(--color-text-primary)'
                    }}
                  >
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                      <span style={{ fontSize: `${0.7 * zoomScale}rem`, fontWeight: '400', opacity: 0.8 }}>{dayOfWeekStr}</span>
                      <span 
                        style={{ 
                          fontSize: `${0.9 * zoomScale}rem`, 
                          marginTop: `${Math.round(2 * zoomScale)}px`,
                          background: isToday ? 'var(--color-primary-glow)' : 'transparent',
                          padding: isToday ? `${Math.round(1 * zoomScale)}px ${Math.round(6 * zoomScale)}px` : '0',
                          borderRadius: `${Math.round(6 * zoomScale)}px`
                        }}
                      >
                        {day.getDate()} {day.toLocaleString('tr-TR', { month: 'short' })}
                      </span>
                    </div>
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody>
            {timeSlots.map((slot) => (
              <tr key={slot}>
                {/* Time slot row header */}
                <td 
                  style={{ 
                    width: `${75 * zoomScale}px`,
                    minWidth: `${75 * zoomScale}px`,
                    padding: `${Math.round(8 * zoomScale)}px ${Math.round(4 * zoomScale)}px`, 
                    fontSize: `${0.8 * zoomScale}rem`, 
                    fontWeight: '600', 
                    textAlign: 'center',
                    color: 'var(--color-text-secondary)',
                    position: 'sticky',
                    left: 0,
                    background: '#090d16',
                    zIndex: 2,
                    boxShadow: '2px 0 5px rgba(0, 0, 0, 0.4)'
                  }}
                >
                  {slot}
                </td>
                
                {/* 7 or more Days columns cells */}
                {datesToRender.map((day, dayIdx) => {
                  const dateStr = formatDateStr(day);
                  const log = getLogForCell(dateStr, slot);
                  const isFuture = day.getTime() + parseTimeToMinutes(slot) * 60 * 1000 > Date.now();
                  const todayStr = new Date().toISOString().split('T')[0];
                  const isEditable = dateStr === todayStr && !isFuture;
                  
                  // Filter visibility check
                  const isFilteredOut = log && (
                    (selectedGridCatIds.size > 0 && !selectedGridCatIds.has(log.categoryId)) ||
                    (selectedGridActIds.size > 0 && !selectedGridActIds.has(log.activityId))
                  );

                  return (
                    <td 
                      key={dayIdx}
                      className={`excel-cell-capsule ${isEditable ? 'grid-cell' : ''}`}
                      onClick={() => isEditable && handleCellClick(slot, dateStr, log || null)}
                      style={{ 
                        width: `${95 * zoomScale}px`,
                        minWidth: `${95 * zoomScale}px`,
                        padding: '1px',
                        height: `${42 * zoomScale}px`,
                        position: 'relative',
                        cursor: isEditable ? 'pointer' : 'default',
                        opacity: isFuture ? 0.35 : (isEditable ? 1 : 0.8),
                        background: 'transparent',
                        transition: 'all 0.15s ease'
                      }}
                    >
                      {log ? (
                        <div 
                          style={{
                            background: isFilteredOut ? '#1f2937' : log.categoryColor,
                            color: isFilteredOut ? '#4b5563' : '#fff',
                            width: '100%',
                            height: '100%',
                            borderRadius: `${Math.round(10 * zoomScale)}px`,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            boxShadow: isFilteredOut ? 'none' : `0 2px 8px ${log.categoryColor}30`,
                            transition: 'all 0.15s ease',
                            opacity: isFilteredOut ? 0.12 : 1
                          }}
                          title={`${log.activityCode}: ${log.activityName}${log.notes ? `\nNot: ${log.notes}` : ''}`}
                        >
                          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', width: '100%', padding: `${Math.round(2 * zoomScale)}px`, textAlign: 'center', lineHeight: '1.2', overflow: 'hidden' }}>
                            <span style={{ fontWeight: '800', fontSize: `${0.75 * zoomScale}rem`, fontFamily: 'Outfit' }}>{log.activityCode}</span>
                            <span style={{ 
                              fontSize: `${0.58 * zoomScale}rem`, 
                              fontWeight: '500',
                              opacity: isFilteredOut ? 0.4 : 0.9, 
                              whiteSpace: 'nowrap', 
                              overflow: 'hidden', 
                              textOverflow: 'ellipsis', 
                              width: '100%',
                              marginTop: '1px'
                            }}>
                              {log.activityName}
                            </span>
                          </div>
                        </div>
                      ) : (
                        <div 
                          style={{
                            width: '100%',
                            height: '100%',
                            borderRadius: `${Math.round(10 * zoomScale)}px`,
                            background: 'rgba(255, 255, 255, 0.03)',
                            border: '1px solid rgba(255, 255, 255, 0.04)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: 'rgba(255, 255, 255, 0.1)',
                            transition: 'all 0.15s ease'
                          }}
                          onMouseEnter={(e) => {
                            if (isEditable) {
                              e.currentTarget.style.background = 'rgba(255, 255, 255, 0.06)';
                              e.currentTarget.style.borderColor = 'var(--color-primary-glow)';
                            }
                          }}
                          onMouseLeave={(e) => {
                            if (isEditable) {
                              e.currentTarget.style.background = 'rgba(255, 255, 255, 0.03)';
                              e.currentTarget.style.borderColor = 'rgba(255, 255, 255, 0.04)';
                            }
                          }}
                        >
                          {!isFuture && isEditable && <Plus size={Math.round(10 * zoomScale)} style={{ opacity: 0 }} className="cell-plus" />}
                        </div>
                      )}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
        )}
      </div>

      {/* Vertical Resize Handle Bar (Only visible when NOT in fullscreen) */}
      {!isFullscreen && (
        <div 
          style={{
            width: '100%',
            height: '10px',
            cursor: 'row-resize',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: 'linear-gradient(180deg, rgba(255,255,255,0.02) 0%, rgba(255,255,255,0.01) 100%)',
            border: '1px solid var(--color-border)',
            borderTop: 'none',
            borderRadius: '0 0 20px 20px',
            userSelect: 'none',
            touchAction: 'none'
          }}
          onMouseDown={handleMouseDown}
          onTouchStart={handleTouchStart}
        >
          {/* Visual handle pill */}
          <div 
            style={{ 
              width: '40px', 
              height: '4px', 
              background: 'rgba(255, 255, 255, 0.2)', 
              borderRadius: '2px',
              transition: 'background 0.2s'
            }}
            onMouseEnter={e => { e.currentTarget.style.background = 'var(--color-primary)'; }}
            onMouseLeave={e => { e.currentTarget.style.background = 'rgba(255, 255, 255, 0.2)'; }}
          />
        </div>
      )}
      </div>

      <style>{`
        .grid-cell:hover {
          background: rgba(255, 255, 255, 0.02) !important;
        }
        .grid-cell:hover .cell-plus {
          opacity: 1 !important;
        }
      `}</style>

      {/* Cell Editor / Log Input Modal */}
      {showLogModal && activeCell && (
        <div className="modal-overlay">
          <div className="modal-content animate-scale-in">
            
            <div className="modal-header">
              <div>
                <h3 style={{ fontSize: '1.25rem' }}>
                  {currentMissedIndex > -1 ? `Kaçırılan Dilim (${currentMissedIndex + 1}/${missedSlots.length})` : (selectedLog ? 'Kayıt Düzenle' : 'Dilim Doldur')}
                </h3>
                <p style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', marginTop: '4px' }}>
                  Tarih: <strong>{activeCell.date}</strong> | Aralık: <strong>{startSlotStr} - {activeCell.slot}</strong> ({selectedDuration} dk geçmişe yönelik)
                </p>
              </div>
              <button 
                className="btn btn-secondary" 
                onClick={() => {
                  setShowLogModal(false);
                  setCurrentMissedIndex(-1);
                  clearPendingLog();
                }} 
                style={{ padding: '6px', borderRadius: '50%' }}
              >
                <X size={16} />
                <span style={{ display: 'none' }}>Kapat</span>
              </button>
            </div>

            <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              
              {selectedLog && (
                <div 
                  style={{ 
                    background: `${selectedLog.categoryColor}15`, 
                    borderLeft: `4px solid ${selectedLog.categoryColor}`,
                    padding: '12px 16px',
                    borderRadius: '8px',
                    fontSize: '0.9rem'
                  }}
                >
                  Şu anki kayıt: <strong>[{selectedLog.activityCode}] {selectedLog.activityName}</strong> (Süre: {selectedLog.durationMinutes || getGridIntervalMinutes()} dk)
                </div>
              )}

              {/* Duration Selector (V12) */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <span style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', fontWeight: '600' }}>
                  Aktivite Süresi:
                </span>
                <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                  {(() => {
                    const gridInterval = getGridIntervalMinutes();
                    const durationOptions = [
                      { label: `${gridInterval} dk`, value: gridInterval },
                      { label: `${gridInterval * 2} dk (${(gridInterval * 2) / 60} sa)`, value: gridInterval * 2 },
                      { label: `${gridInterval * 3} dk`, value: gridInterval * 3 },
                      { label: `${gridInterval * 4} dk (1 sa)`, value: gridInterval * 4 },
                      { label: `${gridInterval * 6} dk (1.5 sa)`, value: gridInterval * 6 },
                      { label: `${gridInterval * 8} dk (2 sa)`, value: gridInterval * 8 },
                    ].filter(opt => opt.value <= 120);
                    return durationOptions.map(opt => (
                      <button
                        key={opt.value}
                        type="button"
                        className="btn"
                        onClick={() => setSelectedDuration(opt.value)}
                        style={{
                          padding: '6px 12px',
                          borderRadius: '8px',
                          fontSize: '0.8rem',
                          background: selectedDuration === opt.value ? 'var(--color-primary)' : 'rgba(255,255,255,0.05)',
                          border: selectedDuration === opt.value ? '1px solid var(--color-primary)' : '1px solid rgba(255,255,255,0.1)',
                          color: '#fff',
                          cursor: 'pointer',
                          transition: 'all 0.15s ease'
                        }}
                      >
                        {opt.label}
                      </button>
                    ));
                  })()}
                </div>
              </div>

              {/* Search Bar */}
              <input 
                type="text" 
                placeholder="Aktivite adı veya kod ara..." 
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                style={{ width: '100%' }}
              />

              {/* Category Quick Filter */}
              <div style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '4px', width: '100%' }}>
                <button 
                  className="btn" 
                  onClick={() => setSelectedCategoryId('all')}
                  style={{ 
                    padding: '10px 18px', 
                    borderRadius: '8px', 
                    fontSize: '0.82rem',
                    flexShrink: 0,
                    minWidth: '85px',
                    background: selectedCategoryId === 'all' ? 'var(--color-primary)' : 'rgba(255,255,255,0.05)',
                    color: '#fff'
                  }}
                >
                  Tümü
                </button>
                {categories.map(cat => (
                  <button 
                    key={cat.id} 
                    className="btn" 
                    onClick={() => setSelectedCategoryId(cat.id)}
                    style={{ 
                      padding: '10px 18px', 
                      borderRadius: '8px', 
                      fontSize: '0.82rem',
                      flexShrink: 0,
                      minWidth: '105px',
                      background: selectedCategoryId === cat.id ? cat.color : 'rgba(255,255,255,0.05)',
                      color: selectedCategoryId === cat.id ? cat.textColor : 'var(--color-text-secondary)'
                    }}
                  >
                    {cat.name}
                  </button>
                ))}
              </div>

              {/* Activities selection list (Grid) */}
              <div 
                style={{ 
                  display: 'grid', 
                  gridTemplateColumns: 'repeat(auto-fill, minmax(130px, 1fr))', 
                  gap: '10px', 
                  maxHeight: '220px', 
                  overflowY: 'auto',
                  padding: '4px',
                  width: '100%'
                }}
              >
                {filteredActivities.length > 0 ? (
                  filteredActivities.map(({ category, activity }) => {
                    const isSelected = tempSelectedActivity?.activity.id === activity.id;
                    return (
                      <button
                        key={activity.id}
                        type="button"
                        onClick={() => setTempSelectedActivity({ activity, category })}
                        style={{
                          background: isSelected ? category.color : 'rgba(255,255,255,0.03)',
                          border: isSelected ? `2px solid ${category.color}` : `1px solid ${category.color}40`,
                          borderRadius: '12px',
                          padding: '12px 10px',
                          display: 'flex',
                          flexDirection: 'column',
                          alignItems: 'center',
                          justifyContent: 'center',
                          gap: '6px',
                          cursor: 'pointer',
                          transition: 'all 0.2s ease',
                          boxShadow: isSelected ? `0 0 12px ${category.color}50` : 'none',
                        }}
                        onMouseEnter={(e) => {
                          if (!isSelected) {
                            e.currentTarget.style.background = `${category.color}15`;
                            e.currentTarget.style.borderColor = category.color;
                            e.currentTarget.style.transform = 'translateY(-2px)';
                          }
                        }}
                        onMouseLeave={(e) => {
                          if (!isSelected) {
                            e.currentTarget.style.background = 'rgba(255,255,255,0.03)';
                            e.currentTarget.style.borderColor = `${category.color}40`;
                            e.currentTarget.style.transform = 'none';
                          }
                        }}
                      >
                        <span 
                          style={{ 
                            background: isSelected ? 'rgba(255, 255, 255, 0.2)' : category.color, 
                            color: isSelected ? '#fff' : category.textColor, 
                            padding: '2px 8px', 
                            borderRadius: '6px', 
                            fontWeight: '800',
                            fontSize: '0.75rem' 
                          }}
                        >
                          {activity.code}
                        </span>
                        <span 
                          style={{ 
                            fontSize: '0.82rem', 
                            fontWeight: '600', 
                            textAlign: 'center',
                            display: '-webkit-box',
                            WebkitLineClamp: 2,
                            WebkitBoxOrient: 'vertical',
                            overflow: 'hidden',
                            height: '2.4em',
                            lineHeight: '1.2em',
                            color: isSelected ? '#fff' : 'rgba(255,255,255,0.95)'
                          }}
                        >
                          {activity.name}
                        </span>
                      </button>
                    );
                  })
                ) : (
                  <div style={{ gridColumn: '1/-1', textAlign: 'center', padding: '24px 0', color: 'var(--color-text-secondary)' }}>
                    Aktivite bulunamadı.
                  </div>
                )}
              </div>

              {/* Notes input */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginTop: '10px', width: '100%' }}>
                <label style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>
                  Açıklama / Notlar
                </label>
                <textarea 
                  rows={2}
                  placeholder="Ekstra detaylar ekleyin..." 
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  style={{ resize: 'none', width: '100%' }}
                />
              </div>

            </div>

            <div className="modal-footer" style={{ justifyContent: 'space-between' }}>
              {selectedLog ? (
                <button className="btn btn-danger" onClick={handleDeleteLogClick} style={{ padding: '10px 16px' }}>
                  <Trash2 size={16} />
                  Kayıt Sil
                </button>
              ) : (
                <div />
              )}
              <div style={{ display: 'flex', gap: '12px' }}>
                <button 
                  className="btn btn-secondary" 
                  onClick={() => {
                    setShowLogModal(false);
                    setSelectedLog(null);
                    setTempSelectedActivity(null);
                    setActiveCell(null);
                    setCurrentMissedIndex(-1);
                    clearPendingLog();
                  }}
                >
                  İptal
                </button>
                <button 
                  type="button"
                  className="btn btn-primary" 
                  disabled={!tempSelectedActivity} 
                  onClick={handleSaveClick}
                  style={{ 
                    padding: '10px 24px', 
                    opacity: !tempSelectedActivity ? 0.5 : 1, 
                    cursor: !tempSelectedActivity ? 'not-allowed' : 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px'
                  }}
                >
                  Kaydet
                </button>
              </div>
            </div>

          </div>
        </div>
      )}

    </div>
  );
}
