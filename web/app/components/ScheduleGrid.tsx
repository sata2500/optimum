'use client';

import React, { useState, useMemo } from 'react';
import {
  Calendar as CalendarIcon,
  ChevronLeft,
  ChevronRight,
  Search,
  Filter,
  CheckCircle2,
  Clock,
  Layers,
  LayoutGrid,
  Table as TableIcon,
  Sliders,
  Sparkles,
  Info,
} from 'lucide-react';

interface TimeLog {
  id: number | string;
  date: string;
  timeSlot: string;
  startTime?: string;
  endTime?: string;
  category: string;
  categoryColor: string;
  activity: string;
  notes?: string;
  isCompleted: boolean;
}

interface ScheduleGridProps {
  syncedData?: any;
}

export default function ScheduleGrid({ syncedData }: ScheduleGridProps) {
  // View Mode: 'daily' (tek gün kartlı görünüm) | 'table' (1-30 gün çoklu tablo modu)
  const [viewMode, setViewMode] = useState<'daily' | 'table'>('daily');
  const [daysToView, setDaysToView] = useState<number>(7); // 3, 7, 14, 30 gün
  const [selectedDate, setSelectedDate] = useState<string>(() => {
    return new Date().toISOString().split('T')[0];
  });
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategoryFilter, setSelectedCategoryFilter] = useState<string>('all');

  // Categories & Activities lookup map
  const categoriesMap = useMemo(() => {
    const map: Record<string | number, { name: string; color: string }> = {};
    if (syncedData?.categories && Array.isArray(syncedData.categories)) {
      syncedData.categories.forEach((cat: any) => {
        map[cat.id] = {
          name: cat.name,
          color: cat.colorHex || cat.color || '#4f46e5',
        };
        map[cat.name] = {
          name: cat.name,
          color: cat.colorHex || cat.color || '#4f46e5',
        };
      });
    }
    return map;
  }, [syncedData]);

  const activitiesMap = useMemo(() => {
    const map: Record<string | number, string> = {};
    if (syncedData?.activities && Array.isArray(syncedData.activities)) {
      syncedData.activities.forEach((act: any) => {
        map[act.id] = act.name;
      });
    }
    return map;
  }, [syncedData]);

  // All parsed logs from syncedData
  const allLogs: TimeLog[] = useMemo(() => {
    if (syncedData?.logs && Array.isArray(syncedData.logs) && syncedData.logs.length > 0) {
      return syncedData.logs.map((l: any, idx: number) => {
        const catInfo = categoriesMap[l.categoryId] || categoriesMap[l.category] || {
          name: l.category || 'Genel',
          color: '#4f46e5',
        };
        const actName = l.activity || activitiesMap[l.activityId] || l.name || 'Aktivite';
        const start = l.startTime || '08:00';
        const end = l.endTime || '08:30';
        const logDate = l.date || l.createdAt?.split('T')[0] || l.syncedAt?.split('T')[0] || selectedDate;

        return {
          id: l.id || idx,
          date: logDate,
          timeSlot: l.timeSlot || `${start} - ${end}`,
          startTime: start,
          endTime: end,
          category: catInfo.name,
          categoryColor: catInfo.color,
          activity: actName,
          notes: l.note || l.notes || '',
          isCompleted: l.completed ?? true,
        };
      });
    }
    return [];
  }, [syncedData, categoriesMap, activitiesMap, selectedDate]);

  // Daily mode logs (filtered for selected date)
  const dailyFilteredLogs: TimeLog[] = useMemo(() => {
    return allLogs.filter((log) => {
      const matchesDate = log.date === selectedDate;
      const matchesSearch =
        !searchQuery ||
        log.activity.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (log.notes && log.notes.toLowerCase().includes(searchQuery.toLowerCase()));
      const matchesCategory =
        selectedCategoryFilter === 'all' || log.category === selectedCategoryFilter;
      return matchesDate && matchesSearch && matchesCategory;
    });
  }, [allLogs, selectedDate, searchQuery, selectedCategoryFilter]);

  // Multi-day date array for table view (e.g. last N days ending at selectedDate)
  const multiDayDates = useMemo(() => {
    const dates: string[] = [];
    const base = new Date(selectedDate);
    for (let i = daysToView - 1; i >= 0; i--) {
      const d = new Date(base);
      d.setDate(base.getDate() - i);
      dates.push(d.toISOString().split('T')[0]);
    }
    return dates;
  }, [selectedDate, daysToView]);

  // Time buckets (06:00 to 23:00 hourly columns for table mode)
  const hourSlots = useMemo(() => {
    const slots: string[] = [];
    for (let h = 6; h <= 23; h++) {
      slots.push(`${h.toString().padStart(2, '0')}:00`);
    }
    return slots;
  }, []);

  // Quick navigation handlers
  const handleDateShift = (days: number) => {
    const d = new Date(selectedDate);
    d.setDate(d.getDate() + days);
    setSelectedDate(d.toISOString().split('T')[0]);
  };

  const handleSetToday = () => {
    setSelectedDate(new Date().toISOString().split('T')[0]);
  };

  const hasRealData = allLogs.length > 0;

  return (
    <div className="space-y-6">
      {/* Top Header & View Mode Switcher */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 tracking-tight">
            Zaman Çizelgesi
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            {viewMode === 'daily'
              ? 'Günün her zaman diliminde tamamlanan ve planlanan aktiviteleriniz'
              : `${daysToView} günlük zaman matrisi ve çoklu gün tablo görünümü`}
          </p>
        </div>

        {/* View Mode Toggle Button Group */}
        <div className="flex items-center gap-2 bg-slate-200/80 p-1 rounded-xl w-fit">
          <button
            onClick={() => setViewMode('daily')}
            className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-semibold transition ${
              viewMode === 'daily'
                ? 'bg-white text-indigo-700 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <LayoutGrid className="w-3.5 h-3.5" />
            <span>Günlük Görünüm</span>
          </button>

          <button
            onClick={() => setViewMode('table')}
            className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-semibold transition ${
              viewMode === 'table'
                ? 'bg-white text-indigo-700 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <TableIcon className="w-3.5 h-3.5" />
            <span>Tablo Modu ({daysToView} Gün)</span>
          </button>
        </div>
      </div>

      {/* Control Bar: Date Selector, Days Range Slider & Filters */}
      <div className="dashboard-card p-4 flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        {/* Date Navigation */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => handleDateShift(-1)}
            className="p-2 text-slate-600 hover:text-slate-900 bg-slate-100 hover:bg-slate-200 rounded-lg transition"
            title="Önceki Gün"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>

          <div className="flex items-center gap-2 px-3 py-1.5 bg-slate-100/90 rounded-lg border border-slate-200 text-xs font-semibold text-slate-800">
            <CalendarIcon className="w-4 h-4 text-indigo-600" />
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => e.target.value && setSelectedDate(e.target.value)}
              className="bg-transparent border-0 font-semibold text-slate-800 focus:outline-none cursor-pointer"
            />
          </div>

          <button
            onClick={() => handleDateShift(1)}
            className="p-2 text-slate-600 hover:text-slate-900 bg-slate-100 hover:bg-slate-200 rounded-lg transition"
            title="Sonraki Gün"
          >
            <ChevronRight className="w-4 h-4" />
          </button>

          <button
            onClick={handleSetToday}
            className="px-3 py-1.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-semibold text-xs rounded-lg border border-indigo-200/60 transition"
          >
            Bugün
          </button>
        </div>

        {/* Multi-Day Range Selector (Only in Table Mode) */}
        {viewMode === 'table' && (
          <div className="flex items-center gap-2 bg-slate-50 p-1.5 rounded-xl border border-slate-200">
            <Sliders className="w-3.5 h-3.5 text-indigo-600 ml-1.5" />
            <span className="text-xs font-semibold text-slate-600 mr-1">Görünüm Aralığı:</span>
            {[3, 7, 14, 30].map((days) => (
              <button
                key={days}
                onClick={() => setDaysToView(days)}
                className={`px-2.5 py-1 rounded-lg text-xs font-bold transition ${
                  daysToView === days
                    ? 'bg-indigo-600 text-white shadow-xs'
                    : 'text-slate-600 hover:bg-slate-200'
                }`}
              >
                {days === 30 ? '1 Ay (30G)' : `${days} Gün`}
              </button>
            ))}
          </div>
        )}

        {/* Search & Category Filter */}
        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Aktivite veya not ara..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:border-indigo-500 w-44 md:w-56"
            />
          </div>

          <div className="relative">
            <select
              value={selectedCategoryFilter}
              onChange={(e) => setSelectedCategoryFilter(e.target.value)}
              className="px-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:border-indigo-500 font-medium text-slate-700 cursor-pointer"
            >
              <option value="all">Tüm Kategoriler</option>
              {syncedData?.categories?.map((cat: any) => (
                <option key={cat.id} value={cat.name}>
                  {cat.name}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* ── MODE 1: DAILY VIEW ── */}
      {viewMode === 'daily' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between px-1">
            <span className="text-xs font-semibold text-slate-600">
              Seçili Tarih: <span className="text-slate-900 font-bold">{selectedDate}</span>
            </span>
            <span className="text-xs font-semibold text-indigo-700 bg-indigo-50 px-3 py-1 rounded-full border border-indigo-100">
              {dailyFilteredLogs.length} Kayıtlı Aktivite
            </span>
          </div>

          {dailyFilteredLogs.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {dailyFilteredLogs.map((log) => (
                <div
                  key={log.id}
                  className="dashboard-card dashboard-card-hover p-4 flex items-start justify-between gap-4 border-l-4"
                  style={{ borderLeftColor: log.categoryColor }}
                >
                  <div className="space-y-2 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="inline-flex items-center text-xs font-semibold text-slate-500 bg-slate-100 px-2 py-0.5 rounded-md">
                        <Clock className="w-3 h-3 mr-1 text-slate-400" />
                        {log.timeSlot}
                      </span>
                      <span
                        className="px-2 py-0.5 text-[11px] font-bold rounded-md"
                        style={{
                          backgroundColor: `${log.categoryColor}15`,
                          color: log.categoryColor,
                        }}
                      >
                        {log.category}
                      </span>
                    </div>

                    <h3 className="text-sm font-bold text-slate-900">{log.activity}</h3>

                    {log.notes && (
                      <p className="text-xs text-slate-500 leading-relaxed bg-slate-50 p-2 rounded-lg border border-slate-100">
                        {log.notes}
                      </p>
                    )}
                  </div>

                  <div className="flex items-center gap-1.5 px-2.5 py-1 bg-emerald-50 text-emerald-700 text-[11px] font-semibold rounded-lg border border-emerald-200/60 shrink-0">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                    <span>Tamamlandı</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="dashboard-card p-10 text-center space-y-3">
              <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 mx-auto flex items-center justify-center">
                <CalendarIcon className="w-6 h-6" />
              </div>
              <h3 className="text-base font-bold text-slate-800">
                Bu Tarih İçin Kayıtlı Aktivite Bulunamadı
              </h3>
              <p className="text-xs text-slate-500 max-w-md mx-auto leading-relaxed">
                {hasRealData
                  ? `Seçtiğiniz tarihte (${selectedDate}) henüz bir kayıt girilmemiş. Üstteki oklarla diğer günlere geçebilir veya telefonunuzdan aktivite ekleyebilirsiniz.`
                  : "Android uygulamanızda 'Profil' sekmesinden 'Şimdi Senkronize Et' butonuna basarak telefonunuzdaki tüm zaman kayıtlarını buraya aktarabilirsiniz."}
              </p>
            </div>
          )}
        </div>
      )}

      {/* ── MODE 2: MULTI-DAY TABLE MATRIX (1-30 GÜN) ── */}
      {viewMode === 'table' && (
        <div className="dashboard-card p-5 space-y-4 overflow-hidden">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <TableIcon className="w-4 h-4 text-indigo-600" />
              <h3 className="text-sm font-bold text-slate-900">
                {daysToView} Günlük Zaman Matrisi ({multiDayDates[0]} → {multiDayDates[multiDayDates.length - 1]})
              </h3>
            </div>
            <span className="text-xs text-slate-500 font-medium">
              Yatay kaydırarak saat bloklarını inceleyin
            </span>
          </div>

          <div className="overflow-x-auto border border-slate-200 rounded-xl">
            <table className="w-full text-left text-xs border-collapse min-w-[900px]">
              <thead>
                <tr className="bg-slate-100 border-b border-slate-200 text-slate-700 font-bold">
                  <th className="p-3 w-28 sticky left-0 bg-slate-100 z-10 border-r border-slate-200">
                    Tarih
                  </th>
                  {hourSlots.map((hour) => (
                    <th key={hour} className="p-2 text-center text-[11px] font-semibold border-r border-slate-200/60">
                      {hour}
                    </th>
                  ))}
                  <th className="p-3 text-center w-24">Toplam Saat</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {multiDayDates.map((dateStr) => {
                  const dayLogs = allLogs.filter((l) => l.date === dateStr);
                  const totalDayHours = (dayLogs.length * 0.5).toFixed(1); // Assuming 30m slots or interval
                  const isSelected = dateStr === selectedDate;

                  return (
                    <tr
                      key={dateStr}
                      className={`hover:bg-slate-50/80 transition ${
                        isSelected ? 'bg-indigo-50/40' : ''
                      }`}
                    >
                      {/* Date label column */}
                      <td className="p-3 font-semibold text-slate-800 sticky left-0 bg-white border-r border-slate-200">
                        <div className="flex items-center gap-1.5">
                          <span
                            className={`w-2 h-2 rounded-full ${
                              dayLogs.length > 0 ? 'bg-indigo-600' : 'bg-slate-300'
                            }`}
                          />
                          <span>{dateStr}</span>
                        </div>
                      </td>

                      {/* Hour block cells */}
                      {hourSlots.map((hour) => {
                        const hInt = parseInt(hour.split(':')[0], 10);
                        const matchLog = dayLogs.find((l) => {
                          const logHour = l.startTime ? parseInt(l.startTime.split(':')[0], 10) : -1;
                          return logHour === hInt;
                        });

                        return (
                          <td
                            key={hour}
                            className="p-1 border-r border-slate-100 text-center align-middle"
                          >
                            {matchLog ? (
                              <div
                                className="px-1.5 py-2 rounded-md text-[10px] font-bold text-white truncate shadow-2xs cursor-pointer transition hover:opacity-90"
                                style={{ backgroundColor: matchLog.categoryColor }}
                                title={`${matchLog.timeSlot} — ${matchLog.activity} (${matchLog.category}) ${
                                  matchLog.notes ? `\nNot: ${matchLog.notes}` : ''
                                }`}
                              >
                                {matchLog.activity}
                              </div>
                            ) : (
                              <div className="h-7 rounded bg-slate-50/60 border border-dashed border-slate-200/50" />
                            )}
                          </td>
                        );
                      })}

                      {/* Day summary */}
                      <td className="p-3 text-center font-bold text-indigo-700">
                        {dayLogs.length > 0 ? `${totalDayHours} sa` : '-'}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
