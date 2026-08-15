'use client';

import React, { useState, useMemo } from 'react';
import {
  ChevronLeft,
  ChevronRight,
  Calendar as CalendarIcon,
  Search,
  Filter,
  CheckCircle2,
  Clock,
  Sparkles,
  Tag,
  FileText,
  AlertCircle,
} from 'lucide-react';

interface TimeLog {
  id: number | string;
  timeSlot: string;
  category: string;
  categoryColor: string;
  activity: string;
  notes?: string;
  isCompleted?: boolean;
}

const defaultCategories = [
  { id: 1, name: 'Çalışma & Kodlama', color: '#4f46e5' },
  { id: 2, name: 'Spor & Sağlık', color: '#10b981' },
  { id: 3, name: 'Okuma & Eğitim', color: '#f59e0b' },
  { id: 4, name: 'Dinlenme & Mola', color: '#ec4899' },
  { id: 5, name: 'Sosyal & Aile', color: '#8b5cf6' },
  { id: 6, name: 'Rutin & İşler', color: '#0ea5e9' },
];

const sampleDemoLogs: TimeLog[] = [
  { id: 1, timeSlot: '08:00 - 09:00', category: 'Çalışma & Kodlama', categoryColor: '#4f46e5', activity: 'Mimari Tasarım & Planlama', notes: 'Yeni özelliklerin analizi yapıldı', isCompleted: true },
  { id: 2, timeSlot: '09:00 - 10:30', category: 'Çalışma & Kodlama', categoryColor: '#4f46e5', activity: 'Optimum Android & Web Geliştirme', notes: 'Google Auth & Bulut Sync entegrasyonu', isCompleted: true },
  { id: 3, timeSlot: '10:30 - 11:00', category: 'Dinlenme & Mola', categoryColor: '#ec4899', activity: 'Kahve & Zihin Molası', notes: 'Kısa yürüyüş', isCompleted: true },
  { id: 4, timeSlot: '11:00 - 12:30', category: 'Okuma & Eğitim', categoryColor: '#f59e0b', activity: 'Teknoloji Araştırmaları', notes: 'Next.js & Cloud mimarisi makaleleri', isCompleted: true },
  { id: 5, timeSlot: '12:30 - 13:30', category: 'Spor & Sağlık', categoryColor: '#10b981', activity: 'Öğle Yemeği & Açık Hava Yürüyüşü', isCompleted: true },
  { id: 6, timeSlot: '13:30 - 15:30', category: 'Çalışma & Kodlama', categoryColor: '#4f46e5', activity: 'Kod İnceleme ve Testler', notes: 'Unit testler ve UI doğrulaması', isCompleted: true },
  { id: 7, timeSlot: '15:30 - 17:00', category: 'Sosyal & Aile', categoryColor: '#8b5cf6', activity: 'Görüşmeler & Planlama', isCompleted: false },
  { id: 8, timeSlot: '17:00 - 18:30', category: 'Spor & Sağlık', categoryColor: '#10b981', activity: 'Antrenman & Egzersiz', notes: 'Kardiyo & Esneme', isCompleted: false },
];

interface ScheduleGridProps {
  syncedData?: any;
}

export default function ScheduleGrid({ syncedData }: ScheduleGridProps) {
  const [selectedDate, setSelectedDate] = useState<string>(
    () => new Date().toISOString().split('T')[0]
  );
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategoryFilter, setSelectedCategoryFilter] = useState<string>('all');

  // Categories lookup map
  const categoriesMap = useMemo(() => {
    const map: Record<string, { name: string; color: string }> = {};
    defaultCategories.forEach((c) => {
      map[c.name] = { name: c.name, color: c.color };
    });

    if (syncedData?.categories && Array.isArray(syncedData.categories)) {
      syncedData.categories.forEach((c: any) => {
        map[c.id || c.name] = {
          name: c.name,
          color: c.colorHex || c.color || '#4f46e5',
        };
        map[c.name] = {
          name: c.name,
          color: c.colorHex || c.color || '#4f46e5',
        };
      });
    }
    return map;
  }, [syncedData]);

  // Extract logs for the chosen date or fallback to sample
  const displayedLogs: TimeLog[] = useMemo(() => {
    if (syncedData?.logs && Array.isArray(syncedData.logs) && syncedData.logs.length > 0) {
      const dateLogs = syncedData.logs.filter((l: any) => {
        const logDate = l.date || l.createdAt?.split('T')[0] || l.syncedAt?.split('T')[0];
        return !logDate || logDate === selectedDate;
      });

      if (dateLogs.length > 0) {
        return dateLogs.map((l: any, idx: number) => {
          const catInfo = categoriesMap[l.categoryId] || categoriesMap[l.category] || {
            name: l.category || 'Genel',
            color: '#4f46e5',
          };
          return {
            id: l.id || idx,
            timeSlot: l.timeSlot || `${l.startTime || '08:00'} - ${l.endTime || '09:00'}`,
            category: catInfo.name,
            categoryColor: catInfo.color,
            activity: l.activity || l.name || 'Planlanan Aktivite',
            notes: l.notes || l.note || '',
            isCompleted: l.completed ?? true,
          } as TimeLog;
        });
      }
    }
    return sampleDemoLogs;
  }, [syncedData, selectedDate, categoriesMap]);

  // Filtered logs
  const filteredLogs: TimeLog[] = useMemo(() => {
    return displayedLogs.filter((log: TimeLog) => {
      const matchesSearch =
        log.activity.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (log.notes && log.notes.toLowerCase().includes(searchQuery.toLowerCase()));
      const matchesCategory =
        selectedCategoryFilter === 'all' || log.category === selectedCategoryFilter;
      return matchesSearch && matchesCategory;
    });
  }, [displayedLogs, searchQuery, selectedCategoryFilter]);

  // Quick navigation handlers
  const handleDateShift = (days: number) => {
    const d = new Date(selectedDate);
    d.setDate(d.getDate() + days);
    setSelectedDate(d.toISOString().split('T')[0]);
  };

  const handleSetToday = () => {
    setSelectedDate(new Date().toISOString().split('T')[0]);
  };

  const completedCount = filteredLogs.filter((l) => l.isCompleted).length;
  const completionRate = filteredLogs.length > 0
    ? Math.round((completedCount / filteredLogs.length) * 100)
    : 0;

  return (
    <div className="space-y-6">
      {/* Top Header Card */}
      <div className="dashboard-card p-5 space-y-4">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div>
            <h2 className="text-xl font-bold text-slate-900 tracking-tight">
              Günlük Zaman Çizelgesi
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Günün her zaman diliminde planlanan ve tamamlanan aktivitelerinizi inceleyin.
            </p>
          </div>

          {/* Date Selector Controls */}
          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={() => handleDateShift(-1)}
              className="p-2 text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-xl transition"
              title="Önceki Gün"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>

            <div className="flex items-center gap-2 px-3 py-1.5 bg-slate-50 border border-slate-200 rounded-xl">
              <CalendarIcon className="w-4 h-4 text-indigo-600 shrink-0" />
              <input
                type="date"
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
                className="bg-transparent text-xs font-semibold text-slate-800 focus:outline-none cursor-pointer"
              />
            </div>

            <button
              onClick={() => handleDateShift(1)}
              className="p-2 text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-xl transition"
              title="Sonraki Gün"
            >
              <ChevronRight className="w-4 h-4" />
            </button>

            <button
              onClick={handleSetToday}
              className="px-3 py-1.5 text-xs font-semibold bg-indigo-50 hover:bg-indigo-100 text-indigo-700 rounded-xl border border-indigo-200/60 transition"
            >
              Bugün
            </button>
          </div>
        </div>

        {/* Quick Stats Banner & Filters */}
        <div className="pt-3 border-t border-slate-100 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex flex-wrap items-center gap-2">
            {/* Search Input */}
            <div className="relative min-w-[200px]">
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Aktivite veya not ara..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-8 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:border-indigo-500 text-slate-800"
              />
            </div>

            {/* Category Filter Dropdown */}
            <select
              value={selectedCategoryFilter}
              onChange={(e) => setSelectedCategoryFilter(e.target.value)}
              className="px-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:border-indigo-500 text-slate-700 font-medium cursor-pointer"
            >
              <option value="all">Tüm Kategoriler</option>
              {defaultCategories.map((c) => (
                <option key={c.id} value={c.name}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          {/* Quick Metrics */}
          <div className="flex items-center gap-3 text-xs">
            <div className="px-3 py-1 bg-slate-100 text-slate-700 rounded-lg font-medium">
              Toplam: <span className="font-bold text-slate-900">{filteredLogs.length} Slot</span>
            </div>
            <div className="px-3 py-1 bg-emerald-50 text-emerald-700 border border-emerald-200/60 rounded-lg font-medium">
              Tamamlanan: <span className="font-bold text-emerald-800">%{completionRate}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Synced Info Notice if demo data */}
      {(!syncedData || !syncedData.logs || syncedData.logs.length === 0) && (
        <div className="p-4 bg-indigo-50/60 border border-indigo-100 rounded-2xl flex items-start gap-3 text-xs text-indigo-900">
          <Sparkles className="w-4 h-4 text-indigo-600 mt-0.5 shrink-0" />
          <div>
            <p className="font-semibold">Örnek Zaman Çizelgesi Görünümü</p>
            <p className="text-slate-600 text-[11px] mt-0.5">
              Android uygulamanızda Google hesabınızla giriş yapıp <strong>"Şimdi Senkronize Et"</strong> butonuna bastığınızda, gerçek telefon çizelgeniz anında burada görüntülenecektir.
            </p>
          </div>
        </div>
      )}

      {/* Timeline Slots Grid */}
      <div className="space-y-3">
        {filteredLogs.length > 0 ? (
          filteredLogs.map((log) => (
            <div
              key={log.id}
              className="dashboard-card dashboard-card-hover p-4 flex flex-col md:flex-row md:items-center justify-between gap-4 border-l-4"
              style={{ borderLeftColor: log.categoryColor }}
            >
              {/* Left Column: Time & Category */}
              <div className="flex items-start md:items-center gap-4">
                {/* Time Badge */}
                <div className="px-3 py-1.5 bg-slate-100 rounded-xl text-xs font-bold text-slate-700 flex items-center gap-1.5 shrink-0">
                  <Clock className="w-3.5 h-3.5 text-slate-500" />
                  <span>{log.timeSlot}</span>
                </div>

                {/* Activity Details */}
                <div className="space-y-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <h3 className="text-sm font-semibold text-slate-900 leading-tight">
                      {log.activity}
                    </h3>
                    <span
                      className="px-2.5 py-0.5 text-[10px] font-bold rounded-full text-white shadow-xs"
                      style={{ backgroundColor: log.categoryColor }}
                    >
                      {log.category}
                    </span>
                  </div>

                  {log.notes && (
                    <p className="text-xs text-slate-500 flex items-center gap-1.5">
                      <FileText className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                      <span>{log.notes}</span>
                    </p>
                  )}
                </div>
              </div>

              {/* Right Column: Status */}
              <div className="flex items-center gap-3 shrink-0 self-end md:self-center">
                {log.isCompleted ? (
                  <span className="flex items-center gap-1.5 px-3 py-1 bg-emerald-50 text-emerald-700 border border-emerald-200/80 rounded-lg text-xs font-semibold">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                    <span>Tamamlandı</span>
                  </span>
                ) : (
                  <span className="flex items-center gap-1.5 px-3 py-1 bg-slate-100 text-slate-500 rounded-lg text-xs font-medium">
                    <Clock className="w-3.5 h-3.5 text-slate-400" />
                    <span>Planlandı</span>
                  </span>
                )}
              </div>
            </div>
          ))
        ) : (
          <div className="dashboard-card p-12 text-center space-y-3">
            <AlertCircle className="w-8 h-8 text-slate-400 mx-auto" />
            <p className="text-sm font-semibold text-slate-700">Bu kriterlere uygun kayıt bulunamadı</p>
            <p className="text-xs text-slate-400">Arama filtrenizi temizleyebilir veya farklı bir tarih seçebilirsiniz.</p>
          </div>
        )}
      </div>
    </div>
  );
}
