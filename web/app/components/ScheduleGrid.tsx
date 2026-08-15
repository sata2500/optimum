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
  AlertTriangle,
  ChevronDown,
  ChevronUp,
  X,
  Plus,
  Edit3,
} from 'lucide-react';

interface TimeLog {
  id: number | string;
  date: string;
  timeSlot: string;
  startTime: string;
  endTime: string;
  categoryId: number | string;
  category: string;
  categoryColor: string;
  categoryCode?: string;
  activityId?: number | string;
  activity: string;
  shortCode?: string;
  activityNumber?: number;
  notes?: string;
  isCompleted: boolean;
}

interface ScheduleGridProps {
  syncedData?: any;
}

export default function ScheduleGrid({ syncedData }: ScheduleGridProps) {
  // View Mode: 'table' (Android çoklu gün dikey saat matrisi) | 'daily' (Tek satır dikey akış)
  const [viewMode, setViewMode] = useState<'table' | 'daily'>('table');
  const [daysToView, setDaysToView] = useState<number>(9); // 1 - 30 days
  const [selectedDate, setSelectedDate] = useState<string>(() => {
    return new Date().toISOString().split('T')[0];
  });

  // Filter & Search states
  const [isFilterExpanded, setIsFilterExpanded] = useState<boolean>(true);
  const [selectedCategoryIds, setSelectedCategoryIds] = useState<Set<number | string>>(new Set());
  const [selectedActivityIds, setSelectedActivityIds] = useState<Set<number | string>>(new Set());
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Selected slot modal state for detailed view / editing
  const [activeSlotDetail, setActiveSlotDetail] = useState<{
    date: string;
    timeSlot: string;
    log?: TimeLog;
    isPastEmpty?: boolean;
    isFuture?: boolean;
  } | null>(null);

  // Categories & Activities lookup
  const categoriesList = useMemo(() => {
    return syncedData?.categories || [];
  }, [syncedData]);

  const activitiesList = useMemo(() => {
    return syncedData?.activities || [];
  }, [syncedData]);

  const categoriesMap = useMemo(() => {
    const map: Record<string | number, { name: string; color: string; code: string }> = {};
    categoriesList.forEach((cat: any) => {
      const code = cat.name ? cat.name.charAt(0).toUpperCase() : 'K';
      map[cat.id] = {
        name: cat.name,
        color: cat.colorHex || cat.color || '#4f46e5',
        code: cat.code || code,
      };
      map[cat.name] = {
        name: cat.name,
        color: cat.colorHex || cat.color || '#4f46e5',
        code: cat.code || code,
      };
    });
    return map;
  }, [categoriesList]);

  const activitiesMap = useMemo(() => {
    const map: Record<string | number, { name: string; shortCode: string; activityNumber: number; categoryId: any }> = {};
    activitiesList.forEach((act: any, idx: number) => {
      map[act.id] = {
        name: act.name,
        shortCode: act.shortCode || `${idx + 1}`,
        activityNumber: act.activityNumber || (idx + 1),
        categoryId: act.categoryId,
      };
    });
    return map;
  }, [activitiesList]);

  // All parsed logs
  const allLogs: TimeLog[] = useMemo(() => {
    if (syncedData?.logs && Array.isArray(syncedData.logs)) {
      return syncedData.logs.map((l: any, idx: number) => {
        const catInfo = categoriesMap[l.categoryId] || categoriesMap[l.category] || {
          name: l.category || 'Genel',
          color: '#4f46e5',
          code: 'G',
        };
        const actInfo = activitiesMap[l.activityId] || {
          name: l.activity || l.name || 'Aktivite',
          shortCode: '1',
          activityNumber: 1,
          categoryId: l.categoryId,
        };

        const start = l.startTime || '08:00';
        const end = l.endTime || '08:30';
        const logDate = l.date || l.createdAt?.split('T')[0] || l.syncedAt?.split('T')[0] || selectedDate;

        // Display short code e.g. Z1, Z2, St, K3
        const shortCode = `${catInfo.code}${actInfo.shortCode || actInfo.activityNumber || ''}`;

        return {
          id: l.id || idx,
          date: logDate,
          timeSlot: l.timeSlot || `${start} - ${end}`,
          startTime: start,
          endTime: end,
          categoryId: l.categoryId,
          category: catInfo.name,
          categoryColor: catInfo.color,
          categoryCode: catInfo.code,
          activityId: l.activityId,
          activity: actInfo.name,
          shortCode: shortCode,
          notes: l.note || l.notes || '',
          isCompleted: l.completed ?? true,
        };
      });
    }
    return [];
  }, [syncedData, categoriesMap, activitiesMap, selectedDate]);

  // Generate standard 30-min time slots for the day (00:00 to 23:30)
  const allTimeSlots = useMemo(() => {
    const slots: { start: string; end: string; label: string }[] = [];
    for (let h = 0; h < 24; h++) {
      const hStr = h.toString().padStart(2, '0');
      slots.push({
        start: `${hStr}:00`,
        end: `${hStr}:30`,
        label: `${hStr}:00`,
      });
      slots.push({
        start: `${hStr}:30`,
        end: `${(h + 1).toString().padStart(2, '0')}:00`,
        label: `${hStr}:30`,
      });
    }
    return slots;
  }, []);

  // Multi-day date columns (ending at selectedDate or starting from it, newest to oldest like in Android)
  const multiDayDates = useMemo(() => {
    const dates: { dateStr: string; dayName: string; formattedDate: string; isToday: boolean }[] = [];
    const base = new Date(selectedDate);
    const todayStr = new Date().toISOString().split('T')[0];

    for (let i = 0; i < daysToView; i++) {
      const d = new Date(base);
      d.setDate(base.getDate() - i);
      const dStr = d.toISOString().split('T')[0];
      const dayName = d.toLocaleDateString('tr-TR', { weekday: 'short' });
      const formattedDate = d.toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit' });

      dates.push({
        dateStr: dStr,
        dayName,
        formattedDate,
        isToday: dStr === todayStr,
      });
    }
    return dates;
  }, [selectedDate, daysToView]);

  // Check if slot is in the past or future relative to right now
  const isSlotPast = (dateStr: string, slotStartTime: string) => {
    const now = new Date();
    const [h, m] = slotStartTime.split(':').map(Number);
    const slotDate = new Date(dateStr);
    slotDate.setHours(h, m, 0, 0);
    return slotDate < now;
  };

  // Helper to toggle category filter
  const toggleCategoryFilter = (catId: number | string) => {
    const next = new Set(selectedCategoryIds);
    if (next.has(catId)) {
      next.delete(catId);
    } else {
      next.add(catId);
    }
    setSelectedCategoryIds(next);
  };

  // Helper to toggle activity filter
  const toggleActivityFilter = (actId: number | string) => {
    const next = new Set(selectedActivityIds);
    if (next.has(actId)) {
      next.delete(actId);
    } else {
      next.add(actId);
    }
    setSelectedActivityIds(next);
  };

  const clearAllFilters = () => {
    setSelectedCategoryIds(new Set());
    setSelectedActivityIds(new Set());
    setSearchQuery('');
  };

  // Date Navigation handlers
  const handleDateShift = (days: number) => {
    const d = new Date(selectedDate);
    d.setDate(d.getDate() + days);
    setSelectedDate(d.toISOString().split('T')[0]);
  };

  const handleSetToday = () => {
    setSelectedDate(new Date().toISOString().split('T')[0]);
  };

  // Formatted date string for header
  const formattedSelectedDateText = useMemo(() => {
    const d = new Date(selectedDate);
    const todayStr = new Date().toISOString().split('T')[0];
    const isToday = selectedDate === todayStr;

    const fullStr = d.toLocaleDateString('tr-TR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      weekday: 'long',
    });

    return { fullStr, isToday };
  }, [selectedDate]);

  return (
    <div className="space-y-5">
      {/* ── SECTION 1: HEADER & VIEW MODE SELECTOR ── */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 tracking-tight">
            Zaman Çizelgesi
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            {viewMode === 'table'
              ? `${daysToView} günlük dikey saat matrisi ve renk kodlu aktivite blokları`
              : 'Günlük tek satır dikey akış ve detaylı zaman slotları'}
          </p>
        </div>

        {/* View Mode Toggle Pill */}
        <div className="flex items-center gap-1.5 bg-slate-200/80 p-1 rounded-xl">
          <button
            onClick={() => setViewMode('table')}
            className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-bold transition ${
              viewMode === 'table'
                ? 'bg-white text-indigo-700 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <TableIcon className="w-3.5 h-3.5" />
            <span>Matris Tablosu ({daysToView}G)</span>
          </button>

          <button
            onClick={() => setViewMode('daily')}
            className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-bold transition ${
              viewMode === 'daily'
                ? 'bg-white text-indigo-700 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <LayoutGrid className="w-3.5 h-3.5" />
            <span>Tekli Günlük Akış</span>
          </button>
        </div>
      </div>

      {/* ── SECTION 2: DAYS SLIDER (1 - 30 GÜN KAYDIRAÇ) ── */}
      <div className="dashboard-card p-4 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Sliders className="w-4 h-4 text-indigo-600" />
            <span className="text-xs font-bold text-slate-800">Görünüm Aralığı</span>
          </div>

          <div className="flex items-center gap-2">
            <span className="px-3 py-1 bg-indigo-50 border border-indigo-200/80 text-indigo-700 font-bold text-xs rounded-full">
              {daysToView} Gün
            </span>
          </div>
        </div>

        {/* Custom Range Slider with dotted styling */}
        <div className="flex items-center gap-4 pt-1">
          <span className="text-xs font-semibold text-slate-400">1G</span>
          <input
            type="range"
            min={1}
            max={30}
            step={1}
            value={daysToView}
            onChange={(e) => setDaysToView(Number(e.target.value))}
            className="w-full h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-indigo-600"
          />
          <span className="text-xs font-semibold text-slate-400">30G</span>
        </div>

        {/* Quick presets buttons */}
        <div className="flex flex-wrap items-center gap-2 pt-1">
          <span className="text-[11px] font-semibold text-slate-400 mr-1">Hızlı Seçim:</span>
          {[
            { label: '1 Gün', val: 1 },
            { label: '3 Gün', val: 3 },
            { label: '7 Gün (1 Hafta)', val: 7 },
            { label: '9 Gün', val: 9 },
            { label: '14 Gün (2 Hafta)', val: 14 },
            { label: '30 Gün (1 Ay)', val: 30 },
          ].map((preset) => (
            <button
              key={preset.val}
              onClick={() => setDaysToView(preset.val)}
              className={`px-2.5 py-1 text-xs font-semibold rounded-lg border transition ${
                daysToView === preset.val
                  ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs'
                  : 'bg-slate-50 hover:bg-slate-100 text-slate-600 border-slate-200'
              }`}
            >
              {preset.label}
            </button>
          ))}
        </div>
      </div>

      {/* ── SECTION 3: DATE NAVIGATION BAR ── */}
      <div className="dashboard-card p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <button
            onClick={() => handleDateShift(-1)}
            className="p-2 text-slate-600 hover:text-slate-900 bg-slate-100 hover:bg-slate-200 rounded-xl transition"
            title="Önceki Gün"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>

          <div className="flex flex-col items-center px-4 py-1.5 bg-slate-50 border border-slate-200 rounded-xl min-w-[220px]">
            <div className="flex items-center gap-2">
              <CalendarIcon className="w-4 h-4 text-indigo-600" />
              <input
                type="date"
                value={selectedDate}
                onChange={(e) => e.target.value && setSelectedDate(e.target.value)}
                className="bg-transparent border-0 font-bold text-xs text-slate-800 focus:outline-none cursor-pointer"
              />
            </div>
            <span className="text-[11px] font-semibold text-slate-500">
              {formattedSelectedDateText.isToday ? 'Bugün' : formattedSelectedDateText.fullStr}
            </span>
          </div>

          <button
            onClick={() => handleDateShift(1)}
            className="p-2 text-slate-600 hover:text-slate-900 bg-slate-100 hover:bg-slate-200 rounded-xl transition"
            title="Sonraki Gün"
          >
            <ChevronRight className="w-4 h-4" />
          </button>

          <button
            onClick={handleSetToday}
            className="px-3.5 py-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-bold text-xs rounded-xl border border-indigo-200/60 transition"
          >
            Bugün
          </button>
        </div>

        {/* Search Input */}
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Aktivite veya not filtrele..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-9 pr-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:border-indigo-500 w-full sm:w-64"
          />
        </div>
      </div>

      {/* ── SECTION 4: EXPANDABLE FILTER ACCORDION (v Filtreler) ── */}
      {categoriesList.length > 0 && (
        <div className="dashboard-card overflow-hidden transition-all">
          <button
            onClick={() => setIsFilterExpanded(!isFilterExpanded)}
            className="w-full p-4 flex items-center justify-between hover:bg-slate-50/60 transition"
          >
            <div className="flex items-center gap-2">
              <Filter className="w-4 h-4 text-indigo-600" />
              <h3 className="text-xs font-bold text-slate-800">
                Filtreler{' '}
                {(selectedCategoryIds.size > 0 || selectedActivityIds.size > 0) && (
                  <span className="text-indigo-600 font-semibold">
                    ({selectedCategoryIds.size + selectedActivityIds.size} Seçili)
                  </span>
                )}
              </h3>
            </div>
            {isFilterExpanded ? (
              <ChevronUp className="w-4 h-4 text-slate-400" />
            ) : (
              <ChevronDown className="w-4 h-4 text-slate-400" />
            )}
          </button>

          {isFilterExpanded && (
            <div className="p-4 pt-1 border-t border-slate-100 space-y-3 bg-slate-50/40">
              {/* Category Chips */}
              <div>
                <p className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-2">
                  Kategoriler
                </p>
                <div className="flex flex-wrap gap-2">
                  <button
                    onClick={clearAllFilters}
                    className={`px-3 py-1 text-xs font-semibold rounded-lg border transition ${
                      selectedCategoryIds.size === 0 && selectedActivityIds.size === 0
                        ? 'bg-indigo-600 text-white border-indigo-600'
                        : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    Tümü
                  </button>

                  {categoriesList.map((cat: any) => {
                    const isSelected = selectedCategoryIds.has(cat.id);
                    const color = cat.colorHex || cat.color || '#4f46e5';
                    return (
                      <button
                        key={cat.id}
                        onClick={() => toggleCategoryFilter(cat.id)}
                        className={`px-3 py-1 text-xs font-semibold rounded-lg border transition flex items-center gap-1.5 ${
                          isSelected
                            ? 'bg-slate-900 text-white border-slate-900 shadow-xs'
                            : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
                        }`}
                      >
                        <span
                          className="w-2.5 h-2.5 rounded-full"
                          style={{ backgroundColor: color }}
                        />
                        <span>{cat.name}</span>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Activity Chips (belonging to categories) */}
              {activitiesList.length > 0 && (
                <div>
                  <p className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-2">
                    Aktiviteler
                  </p>
                  <div className="flex flex-wrap gap-2">
                    {activitiesList.map((act: any) => {
                      const isSelected = selectedActivityIds.has(act.id);
                      const catInfo = categoriesMap[act.categoryId] || { color: '#4f46e5', code: 'A' };
                      const shortCode = `${catInfo.code}${act.shortCode || act.activityNumber || ''}`;

                      return (
                        <button
                          key={act.id}
                          onClick={() => toggleActivityFilter(act.id)}
                          className={`px-2.5 py-1 text-xs font-medium rounded-lg border transition flex items-center gap-1.5 ${
                            isSelected
                              ? 'bg-indigo-50 border-indigo-400 text-indigo-900 font-bold'
                              : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'
                          }`}
                        >
                          <span
                            className="px-1 py-0.2 rounded text-[10px] font-bold text-white"
                            style={{ backgroundColor: catInfo.color }}
                          >
                            {shortCode}
                          </span>
                          <span>{act.name}</span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* ── MODE 1: MATRİS TABLOSU (VERTICAL HOURS x HORIZONTAL DAYS) ── */}
      {viewMode === 'table' && (
        <div className="dashboard-card p-4 overflow-hidden space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <TableIcon className="w-4 h-4 text-indigo-600" />
              <h3 className="text-sm font-bold text-slate-900">
                {daysToView} Günlük Zaman Matrisi
              </h3>
            </div>
            <div className="flex items-center gap-4 text-xs text-slate-500 font-medium">
              <span className="flex items-center gap-1">
                <span className="w-3 h-3 rounded-xs bg-indigo-100 border border-indigo-300" />
                Dolu Slot
              </span>
              <span className="flex items-center gap-1">
                <span className="w-3 h-3 rounded-xs bg-rose-100 border border-rose-300 flex items-center justify-center text-[8px] text-rose-600 font-bold">
                  ⚠️
                </span>
                Eksik Slot
              </span>
              <span className="flex items-center gap-1">
                <span className="w-3 h-3 rounded-xs bg-slate-100 border border-slate-200" />
                Gelecek / Boş
              </span>
            </div>
          </div>

          {/* Matrix Table Container with Sticky Columns */}
          <div className="overflow-x-auto overflow-y-auto max-h-[700px] border border-slate-200 rounded-xl relative shadow-2xs">
            <table className="w-full text-left text-xs border-collapse">
              {/* Table Header Row (Dates) */}
              <thead className="sticky top-0 bg-slate-100 z-20 shadow-xs border-b border-slate-200">
                <tr>
                  <th className="p-3 w-20 sticky left-0 bg-slate-100 z-30 border-r border-slate-200 text-center font-bold text-indigo-700">
                    Saat
                  </th>
                  {multiDayDates.map((col) => (
                    <th
                      key={col.dateStr}
                      className={`p-2.5 min-w-[54px] max-w-[64px] text-center border-r border-slate-200/80 ${
                        col.isToday ? 'bg-indigo-50/90 text-indigo-900 font-black' : 'text-slate-700 font-bold'
                      }`}
                    >
                      <div className="text-[11px] leading-tight">{col.formattedDate}</div>
                      <div className="text-[10px] text-slate-500 font-semibold">{col.dayName}</div>
                    </th>
                  ))}
                </tr>
              </thead>

              {/* Table Body Rows (Hours 00:00 to 23:30) */}
              <tbody className="divide-y divide-slate-100 bg-white">
                {allTimeSlots.map((slot) => {
                  return (
                    <tr key={slot.start} className="hover:bg-slate-50/50 transition">
                      {/* Sticky Left Hour Column */}
                      <td className="p-2 w-20 sticky left-0 bg-white z-10 border-r border-slate-200 text-center font-mono font-bold text-slate-700 text-xs shadow-xs">
                        {slot.label}
                      </td>

                      {/* Day Cells */}
                      {multiDayDates.map((col) => {
                        // Find matching log
                        const matchLog = allLogs.find((l) => {
                          return l.date === col.dateStr && l.startTime === slot.start;
                        });

                        const past = isSlotPast(col.dateStr, slot.start);
                        const isPastEmpty = past && !matchLog;
                        const isFuture = !past && !matchLog;

                        // Check filters
                        const isCategoryFiltered =
                          selectedCategoryIds.size > 0 &&
                          matchLog &&
                          !selectedCategoryIds.has(matchLog.categoryId);

                        const isActivityFiltered =
                          selectedActivityIds.size > 0 &&
                          matchLog &&
                          matchLog.activityId &&
                          !selectedActivityIds.has(matchLog.activityId);

                        const isSearchFiltered =
                          searchQuery &&
                          matchLog &&
                          !matchLog.activity.toLowerCase().includes(searchQuery.toLowerCase()) &&
                          !matchLog.notes?.toLowerCase().includes(searchQuery.toLowerCase());

                        const isFilteredOut = isCategoryFiltered || isActivityFiltered || isSearchFiltered;

                        return (
                          <td
                            key={col.dateStr}
                            className="p-1 border-r border-slate-100 text-center align-middle"
                          >
                            {matchLog ? (
                              <button
                                onClick={() =>
                                  setActiveSlotDetail({
                                    date: col.dateStr,
                                    timeSlot: `${slot.start} - ${slot.end}`,
                                    log: matchLog,
                                  })
                                }
                                className={`w-full h-8 rounded-lg flex items-center justify-center text-[11px] font-black transition transform active:scale-95 shadow-2xs border ${
                                  isFilteredOut ? 'opacity-20' : 'opacity-100 hover:ring-2 hover:ring-indigo-400'
                                }`}
                                style={{
                                  backgroundColor: `${matchLog.categoryColor}25`,
                                  borderColor: `${matchLog.categoryColor}60`,
                                  color: matchLog.categoryColor,
                                }}
                                title={`${col.dateStr} | ${slot.start} - ${slot.end}\n${matchLog.activity} (${matchLog.category})\n${
                                  matchLog.notes ? `Not: ${matchLog.notes}` : ''
                                }`}
                              >
                                {matchLog.shortCode || 'Z1'}
                              </button>
                            ) : isPastEmpty ? (
                              <button
                                onClick={() =>
                                  setActiveSlotDetail({
                                    date: col.dateStr,
                                    timeSlot: `${slot.start} - ${slot.end}`,
                                    isPastEmpty: true,
                                  })
                                }
                                className={`w-full h-8 rounded-lg bg-rose-50 border border-rose-200/80 flex items-center justify-center text-rose-500 hover:bg-rose-100 transition ${
                                  isFilteredOut ? 'opacity-20' : 'opacity-100'
                                }`}
                                title={`${col.dateStr} | ${slot.start} - ${slot.end} — Eksik Kayıt`}
                              >
                                <AlertTriangle className="w-3 h-3 text-rose-500" />
                              </button>
                            ) : (
                              <div
                                onClick={() =>
                                  setActiveSlotDetail({
                                    date: col.dateStr,
                                    timeSlot: `${slot.start} - ${slot.end}`,
                                    isFuture: true,
                                  })
                                }
                                className="w-full h-8 rounded-lg bg-slate-50/60 border border-dashed border-slate-200/60 hover:border-slate-300 transition cursor-pointer"
                                title={`${col.dateStr} | ${slot.start} - ${slot.end} — Gelecek Zaman`}
                              />
                            )}
                          </td>
                        );
                      })}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ── MODE 2: TEKLİ GÜNLÜK AKIŞ (HER SATIRDA TEK BİR SLOT) ── */}
      {viewMode === 'daily' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between px-1">
            <span className="text-xs font-semibold text-slate-600">
              Seçili Gün: <span className="text-slate-900 font-bold">{selectedDate}</span>
            </span>
            <span className="text-xs font-bold text-indigo-700 bg-indigo-50 px-3 py-1 rounded-full border border-indigo-100">
              48 Zaman Slotu (30 Dk)
            </span>
          </div>

          {/* Single Column Vertical Stack: One slot per row */}
          <div className="space-y-2">
            {allTimeSlots.map((slot) => {
              const matchLog = allLogs.find((l) => l.date === selectedDate && l.startTime === slot.start);
              const past = isSlotPast(selectedDate, slot.start);
              const isPastEmpty = past && !matchLog;
              const isFuture = !past && !matchLog;

              return (
                <div
                  key={slot.start}
                  onClick={() =>
                    setActiveSlotDetail({
                      date: selectedDate,
                      timeSlot: `${slot.start} - ${slot.end}`,
                      log: matchLog,
                      isPastEmpty,
                      isFuture,
                    })
                  }
                  className={`dashboard-card dashboard-card-hover p-3.5 flex items-center justify-between gap-4 cursor-pointer transition border-l-4 ${
                    matchLog
                      ? 'bg-white'
                      : isPastEmpty
                      ? 'bg-rose-50/30 border-rose-300'
                      : 'bg-slate-50/60 border-slate-200'
                  }`}
                  style={{
                    borderLeftColor: matchLog ? matchLog.categoryColor : isPastEmpty ? '#f43f5e' : '#cbd5e1',
                  }}
                >
                  {/* Left: Time Badge & Short code */}
                  <div className="flex items-center gap-4">
                    <span className="w-28 text-xs font-mono font-bold text-slate-700 bg-slate-100 px-2.5 py-1 rounded-md flex items-center gap-1.5 shrink-0">
                      <Clock className="w-3.5 h-3.5 text-slate-400" />
                      <span>{slot.start} - {slot.end}</span>
                    </span>

                    {matchLog ? (
                      <div className="flex items-center gap-2.5">
                        <span
                          className="px-2 py-0.5 rounded text-[11px] font-black text-white shrink-0"
                          style={{ backgroundColor: matchLog.categoryColor }}
                        >
                          {matchLog.shortCode || 'Z1'}
                        </span>
                        <div>
                          <h4 className="text-xs font-bold text-slate-900">{matchLog.activity}</h4>
                          <span className="text-[11px] font-medium text-slate-500">{matchLog.category}</span>
                        </div>
                      </div>
                    ) : isPastEmpty ? (
                      <div className="flex items-center gap-2 text-rose-600">
                        <AlertTriangle className="w-4 h-4 text-rose-500" />
                        <span className="text-xs font-semibold">Doldurulmamış Zaman Dilimi (Eksik Kayıt)</span>
                      </div>
                    ) : (
                      <span className="text-xs font-medium text-slate-400">Planlanmamış Zaman Dilimi</span>
                    )}
                  </div>

                  {/* Right: Notes / Status badge */}
                  <div className="flex items-center gap-3">
                    {matchLog?.notes && (
                      <p className="hidden md:block text-xs text-slate-500 max-w-sm truncate bg-slate-50 px-2.5 py-1 rounded-md border border-slate-100">
                        💬 {matchLog.notes}
                      </p>
                    )}

                    {matchLog ? (
                      <span className="px-2.5 py-1 bg-emerald-50 text-emerald-700 text-[11px] font-semibold rounded-md border border-emerald-200/60 flex items-center gap-1">
                        <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                        <span>Tamamlandı</span>
                      </span>
                    ) : isPastEmpty ? (
                      <span className="px-2.5 py-1 bg-rose-100/80 text-rose-700 text-[11px] font-semibold rounded-md">
                        Eksik
                      </span>
                    ) : null}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* ── SLOT DETAIL MODAL / DIALOG ── */}
      {activeSlotDetail && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white border border-slate-200 rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-5 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <Clock className="w-5 h-5 text-indigo-600" />
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Zaman Dilimi Detayı</h3>
                  <p className="text-[11px] text-slate-500 font-medium">
                    {activeSlotDetail.date} | {activeSlotDetail.timeSlot}
                  </p>
                </div>
              </div>
              <button
                onClick={() => setActiveSlotDetail(null)}
                className="p-1.5 text-slate-400 hover:text-slate-700 rounded-lg hover:bg-slate-100"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {activeSlotDetail.log ? (
              <div className="space-y-4">
                <div className="flex items-center gap-3 p-3.5 bg-slate-50 rounded-xl border border-slate-100">
                  <span
                    className="px-2.5 py-1 rounded-md text-xs font-black text-white"
                    style={{ backgroundColor: activeSlotDetail.log.categoryColor }}
                  >
                    {activeSlotDetail.log.shortCode || 'Z1'}
                  </span>
                  <div>
                    <h4 className="text-sm font-bold text-slate-900">
                      {activeSlotDetail.log.activity}
                    </h4>
                    <p className="text-xs text-slate-500">{activeSlotDetail.log.category}</p>
                  </div>
                </div>

                {activeSlotDetail.log.notes ? (
                  <div className="space-y-1">
                    <label className="text-[11px] font-bold text-slate-400 uppercase">Notlar</label>
                    <p className="text-xs text-slate-700 bg-slate-50 p-3 rounded-xl border border-slate-100 leading-relaxed">
                      {activeSlotDetail.log.notes}
                    </p>
                  </div>
                ) : (
                  <p className="text-xs text-slate-400 italic">Bu slot için özel not girilmemiş.</p>
                )}

                <div className="flex items-center justify-between text-xs pt-2">
                  <span className="text-slate-500">Durum:</span>
                  <span className="font-semibold text-emerald-600 flex items-center gap-1">
                    <CheckCircle2 className="w-3.5 h-3.5" /> Tamamlandı
                  </span>
                </div>
              </div>
            ) : activeSlotDetail.isPastEmpty ? (
              <div className="p-4 bg-rose-50 border border-rose-200/80 rounded-xl space-y-2 text-center">
                <AlertTriangle className="w-6 h-6 text-rose-500 mx-auto" />
                <h4 className="text-xs font-bold text-rose-800">Doldurulmamış Zaman Slotu</h4>
                <p className="text-[11px] text-rose-600">
                  Bu zaman dilimi geçmişte kalmış ancak henüz bir aktivite kaydı yapılmamış. Android uygulamanızda bu slota dokunarak aktivitenizi kaydedebilirsiniz.
                </p>
              </div>
            ) : (
              <div className="p-4 bg-slate-50 border border-slate-200/80 rounded-xl space-y-1 text-center text-xs text-slate-500">
                <p className="font-semibold text-slate-700">Gelecek Zaman Dilimi</p>
                <p className="text-[11px]">Bu saat dilimi henüz tamamlanmadı.</p>
              </div>
            )}

            <div className="pt-2 border-t border-slate-100 flex justify-end">
              <button
                onClick={() => setActiveSlotDetail(null)}
                className="px-4 py-2 bg-slate-900 text-white font-semibold text-xs rounded-xl hover:bg-slate-800 transition"
              >
                Kapat
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
