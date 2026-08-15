'use client';

import React, { useState, useMemo } from 'react';
import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
} from 'recharts';
import {
  TrendingUp,
  Award,
  Zap,
  Activity,
  Star,
  Clock,
  Calendar,
  Layers,
  Smile,
  Meh,
  Frown,
  Sparkles,
  Plus,
  Edit3,
  X,
  CheckCircle2,
} from 'lucide-react';

interface AnalyticsChartsProps {
  syncedData?: any;
}

export default function AnalyticsCharts({ syncedData }: AnalyticsChartsProps) {
  // Time range: 1d (Bugün), 7d (Bu Hafta), 30d (Bu Ay), all (Tümü)
  const [selectedRange, setSelectedRange] = useState<'1d' | '7d' | '30d' | 'all'>('7d');
  const [chartType, setChartType] = useState<'pie' | 'bar'>('pie');

  // New Daily Evaluation modal state
  const [showAddEvalModal, setShowAddEvalModal] = useState(false);
  const [evalDate, setEvalDate] = useState(() => new Date().toISOString().split('T')[0]);
  const [evalRating, setEvalRating] = useState(5);
  const [evalMood, setEvalMood] = useState(4); // 4=Harika, 3=İyi, 2=Nötr, 1=Düşük
  const [evalNote, setEvalNote] = useState('');

  const [localEvaluations, setLocalEvaluations] = useState<any[]>([]);

  // Merge synced evaluations with local evaluations
  const evaluations = useMemo(() => {
    const fromSync = syncedData?.evaluations || [];
    const combined = [...localEvaluations, ...fromSync];
    // deduplicate by date
    const map = new Map();
    combined.forEach((item) => {
      if (!map.has(item.date)) {
        map.set(item.date, item);
      }
    });
    return Array.from(map.values()).sort((a: any, b: any) => b.date.localeCompare(a.date));
  }, [syncedData, localEvaluations]);

  const rawLogs = useMemo(() => syncedData?.logs || [], [syncedData]);
  const categories = useMemo(() => syncedData?.categories || [], [syncedData]);

  // Categories lookup map for colors
  const categoriesMap = useMemo(() => {
    const map: Record<string | number, { name: string; color: string }> = {};
    if (Array.isArray(categories)) {
      categories.forEach((cat: any) => {
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
  }, [categories]);

  // Filter logs by selected range
  const filteredLogs = useMemo(() => {
    if (!rawLogs || rawLogs.length === 0) return [];
    if (selectedRange === 'all') return rawLogs;

    const today = new Date();
    const daysLimit = selectedRange === '1d' ? 1 : selectedRange === '7d' ? 7 : 30;
    const cutoffDate = new Date(today);
    cutoffDate.setDate(today.getDate() - (daysLimit - 1));
    const cutoffStr = cutoffDate.toISOString().split('T')[0];

    return rawLogs.filter((log: any) => {
      const logDate = log.date || log.createdAt?.split('T')[0] || log.syncedAt?.split('T')[0];
      return logDate && logDate >= cutoffStr;
    });
  }, [rawLogs, selectedRange]);

  // 1. Compute Category Distribution
  const categoryDistribution = useMemo(() => {
    if (!filteredLogs || filteredLogs.length === 0) return [];

    const counts: Record<string, { count: number; color: string }> = {};
    filteredLogs.forEach((log: any) => {
      const catInfo = categoriesMap[log.categoryId] || categoriesMap[log.category] || {
        name: log.category || 'Genel',
        color: '#4f46e5',
      };
      const catName = catInfo.name;
      if (!counts[catName]) {
        counts[catName] = { count: 0, color: catInfo.color };
      }
      counts[catName].count += 1;
    });

    const total = filteredLogs.length;
    return Object.entries(counts).map(([name, data]) => ({
      name,
      value: Math.round((data.count / total) * 100),
      rawCount: data.count,
      hours: +(data.count * 0.5).toFixed(1),
      color: data.color,
    }));
  }, [filteredLogs, categoriesMap]);

  // 2. Compute Daily Volume
  const volumeData = useMemo(() => {
    if (!filteredLogs || filteredLogs.length === 0) return [];

    const dateGroups: Record<string, number> = {};
    filteredLogs.forEach((log: any) => {
      const logDate = log.date || log.createdAt?.split('T')[0] || log.syncedAt?.split('T')[0];
      if (logDate) {
        dateGroups[logDate] = (dateGroups[logDate] || 0) + 1;
      }
    });

    return Object.entries(dateGroups)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, count]) => {
        const d = new Date(date);
        const dayName = d.toLocaleDateString('tr-TR', { weekday: 'short', day: 'numeric', month: 'numeric' });
        return {
          date: dayName,
          fullDate: date,
          saat: +(count * 0.5).toFixed(1),
          slotSayisi: count,
        };
      });
  }, [filteredLogs]);

  // 3. Compute Real KPI Metrics
  const totalSlots = filteredLogs.length;
  const totalHours = (totalSlots * 0.5).toFixed(1);
  const avgRating = useMemo(() => {
    if (evaluations.length === 0) return 0;
    const sum = evaluations.reduce((acc: number, curr: any) => acc + (curr.rating || 0), 0);
    return (sum / evaluations.length).toFixed(1);
  }, [evaluations]);

  const topCategory = categoryDistribution[0]?.name || 'Henüz Yok';

  // Mood helper
  const renderMoodBadge = (mood: number) => {
    switch (mood) {
      case 4:
        return <span className="px-2 py-0.5 bg-emerald-50 text-emerald-700 rounded-md text-[11px] font-bold">🤩 Harika</span>;
      case 3:
        return <span className="px-2 py-0.5 bg-sky-50 text-sky-700 rounded-md text-[11px] font-bold">😊 İyi</span>;
      case 2:
        return <span className="px-2 py-0.5 bg-amber-50 text-amber-700 rounded-md text-[11px] font-bold">😐 Nötr</span>;
      case 1:
        return <span className="px-2 py-0.5 bg-rose-50 text-rose-700 rounded-md text-[11px] font-bold">😞 Düşük</span>;
      default:
        return null;
    }
  };

  const handleSaveEvaluation = (e: React.FormEvent) => {
    e.preventDefault();
    const newEval = {
      date: evalDate,
      rating: evalRating,
      mood: evalMood,
      journalNote: evalNote.trim(),
      updatedTimestamp: Date.now(),
    };
    setLocalEvaluations([newEval, ...localEvaluations]);
    setEvalNote('');
    setShowAddEvalModal(false);
  };

  return (
    <div className="space-y-6">
      {/* Top Header & Range Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 tracking-tight">
            Gelişmiş Analitik & Değerlendirme 📊
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Zamanınızı nasıl değerlendirdiğinizi, kategori odaklarınızı ve günlük puanlarınızı inceleyin
          </p>
        </div>

        {/* Range Selector Pill */}
        <div className="flex items-center gap-1.5 bg-slate-200/80 p-1 rounded-xl w-fit">
          {[
            { id: '1d', label: 'Bugün (1G)' },
            { id: '7d', label: 'Bu Hafta (7G)' },
            { id: '30d', label: 'Bu Ay (30G)' },
            { id: 'all', label: 'Tüm Zamanlar' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setSelectedRange(tab.id as any)}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition ${
                selectedRange === tab.id
                  ? 'bg-white text-indigo-700 shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* 4 Real KPI Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* KPI 1 */}
        <div className="dashboard-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-medium text-slate-500">Seçili Dönem Odaklanma</p>
            <h3 className="text-2xl font-bold text-slate-900">{totalHours} Saat</h3>
            <span className="text-[11px] font-semibold text-indigo-600">
              {totalSlots} Kayıtlı Slot
            </span>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600">
            <Clock className="w-6 h-6" />
          </div>
        </div>

        {/* KPI 2 */}
        <div className="dashboard-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-medium text-slate-500">Ortalama Günlük Puan</p>
            <h3 className="text-2xl font-bold text-slate-900">
              {+avgRating > 0 ? `${avgRating} / 5` : '-'}
            </h3>
            <span className="text-[11px] font-semibold text-amber-600">
              {evaluations.length} Gün Değerlendirildi
            </span>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-amber-50 border border-amber-100 flex items-center justify-center text-amber-600">
            <Star className="w-6 h-6" />
          </div>
        </div>

        {/* KPI 3 */}
        <div className="dashboard-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-medium text-slate-500">Aktif Kategori Havuzu</p>
            <h3 className="text-2xl font-bold text-slate-900">{categories.length} Kategori</h3>
            <span className="text-[11px] font-semibold text-emerald-600">
              {syncedData?.activities?.length || 0} Aktif Aktivite
            </span>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-emerald-50 border border-emerald-100 flex items-center justify-center text-emerald-600">
            <Layers className="w-6 h-6" />
          </div>
        </div>

        {/* KPI 4 */}
        <div className="dashboard-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-medium text-slate-500">En Çok Zaman Ayrılan</p>
            <h3 className="text-sm font-bold text-slate-900 truncate max-w-[140px]" title={topCategory}>
              {topCategory}
            </h3>
            <span className="text-[11px] font-semibold text-sky-600">
              {categoryDistribution[0] ? `%${categoryDistribution[0].value} Pay (${categoryDistribution[0].hours} sa)` : 'Kayıt Yok'}
            </span>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-sky-50 border border-sky-100 flex items-center justify-center text-sky-600">
            <TrendingUp className="w-6 h-6" />
          </div>
        </div>
      </div>

      {/* Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Kategori Dağılım Grafiği (Donut / Bar) */}
        <div className="dashboard-card p-6 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-base font-bold text-slate-900">Kategori Zaman Dağılımı</h3>
              <p className="text-xs text-slate-500">Aktivitelerinizin kategorilere göre yüzdelik ve saatlik payı</p>
            </div>
            <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-lg">
              <button
                onClick={() => setChartType('pie')}
                className={`px-2.5 py-1 text-[11px] font-bold rounded-md transition ${
                  chartType === 'pie' ? 'bg-white text-indigo-700 shadow-2xs' : 'text-slate-500'
                }`}
              >
                Pasta 🥧
              </button>
              <button
                onClick={() => setChartType('bar')}
                className={`px-2.5 py-1 text-[11px] font-bold rounded-md transition ${
                  chartType === 'bar' ? 'bg-white text-indigo-700 shadow-2xs' : 'text-slate-500'
                }`}
              >
                Çubuk 📊
              </button>
            </div>
          </div>

          {categoryDistribution.length > 0 ? (
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                {chartType === 'pie' ? (
                  <PieChart>
                    <Pie
                      data={categoryDistribution}
                      cx="50%"
                      cy="50%"
                      innerRadius={65}
                      outerRadius={95}
                      paddingAngle={4}
                      dataKey="value"
                    >
                      {categoryDistribution.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip
                      formatter={(value: any, name: any, props: any) => [
                        `%${value} (${props.payload.hours} Saat)`,
                        props.payload.name,
                      ]}
                      contentStyle={{
                        backgroundColor: '#ffffff',
                        borderColor: '#e2e8f0',
                        borderRadius: '0.75rem',
                        fontSize: '12px',
                        boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
                      }}
                    />
                    <Legend
                      verticalAlign="bottom"
                      height={36}
                      formatter={(value) => (
                        <span className="text-xs text-slate-700 font-medium">{value}</span>
                      )}
                    />
                  </PieChart>
                ) : (
                  <BarChart data={categoryDistribution} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                    <XAxis dataKey="name" stroke="#94a3b8" fontSize={11} tickLine={false} />
                    <YAxis stroke="#94a3b8" fontSize={11} tickLine={false} />
                    <Tooltip
                      formatter={(value: any, name: any, props: any) => [`${props.payload.hours} Saat (%${props.payload.value})`, '']}
                      contentStyle={{
                        backgroundColor: '#ffffff',
                        borderColor: '#e2e8f0',
                        borderRadius: '0.75rem',
                        fontSize: '12px',
                      }}
                    />
                    <Bar dataKey="hours" radius={[6, 6, 0, 0]}>
                      {categoryDistribution.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Bar>
                  </BarChart>
                )}
              </ResponsiveContainer>
            </div>
          ) : (
            <div className="h-72 flex flex-col items-center justify-center text-center p-6 bg-slate-50 rounded-xl border border-dashed border-slate-200 space-y-2">
              <Activity className="w-8 h-8 text-slate-400" />
              <p className="text-xs font-semibold text-slate-700">Seçili Aralıkta Veri Yok</p>
              <p className="text-[11px] text-slate-500 max-w-xs">
                Telefonunuzdan aktivitelerinizi kaydettikten sonra 'Şimdi Senkronize Et' diyerek buraya aktarabilirsiniz.
              </p>
            </div>
          )}
        </div>

        {/* Günlük Aktivite Hacmi (Bar Chart) */}
        <div className="dashboard-card p-6 space-y-4">
          <div>
            <h3 className="text-base font-bold text-slate-900">Günlük Odak Hacmi (Saat)</h3>
            <p className="text-xs text-slate-500">Seçili dönemdeki günlere göre toplam odaklanma süreleri</p>
          </div>

          {volumeData.length > 0 ? (
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={volumeData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                  <XAxis dataKey="date" stroke="#94a3b8" fontSize={11} tickLine={false} />
                  <YAxis stroke="#94a3b8" fontSize={11} tickLine={false} />
                  <Tooltip
                    formatter={(value: any) => [`${value} Saat`, 'Odak Süresi']}
                    contentStyle={{
                      backgroundColor: '#ffffff',
                      borderColor: '#e2e8f0',
                      borderRadius: '0.75rem',
                      fontSize: '12px',
                    }}
                  />
                  <Bar dataKey="saat" fill="#4f46e5" radius={[6, 6, 0, 0]} name="Odak Süresi (Saat)" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <div className="h-72 flex flex-col items-center justify-center text-center p-6 bg-slate-50 rounded-xl border border-dashed border-slate-200 space-y-2">
              <Clock className="w-8 h-8 text-slate-400" />
              <p className="text-xs font-semibold text-slate-700">Günlük Kayıt Bulunamadı</p>
              <p className="text-[11px] text-slate-500 max-w-xs">
                Telefonunuzdaki zaman kayıtları eşitlendiğinde günlere göre çalışma hacminiz burada listelenecektir.
              </p>
            </div>
          )}
        </div>
      </div>

      {/* ── GÜNLÜK ÖZ DEĞERLENDİRMELER & PUANLAR (REAL USER DATA) ── */}
      <div className="dashboard-card p-6 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h3 className="text-base font-bold text-slate-900">
              Günlük Öz Değerlendirmeler & Yansımalar
            </h3>
            <p className="text-xs text-slate-500">
              Günün sonunda verdiğiniz yıldız puanları, ruh haliniz ve değerlendirme notlarınız
            </p>
          </div>

          <button
            onClick={() => setShowAddEvalModal(true)}
            className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs rounded-xl shadow-xs transition flex items-center gap-1.5 w-fit"
          >
            <Plus className="w-4 h-4" />
            <span>Günü Değerlendir</span>
          </button>
        </div>

        {evaluations.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-1">
            {evaluations.map((evalItem: any, idx: number) => (
              <div key={idx} className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-2.5 hover:shadow-xs transition">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-indigo-600" />
                    {evalItem.date}
                  </span>
                  <div className="flex items-center gap-0.5 text-amber-500">
                    {Array.from({ length: 5 }).map((_, i) => (
                      <Star
                        key={i}
                        className={`w-3.5 h-3.5 ${
                          i < (evalItem.rating || 0) ? 'fill-amber-400 text-amber-400' : 'text-slate-300'
                        }`}
                      />
                    ))}
                  </div>
                </div>

                {evalItem.mood && evalItem.mood > 0 && (
                  <div className="flex items-center gap-1.5">
                    {renderMoodBadge(evalItem.mood)}
                  </div>
                )}

                <p className="text-xs text-slate-600 leading-relaxed bg-white p-3 rounded-lg border border-slate-200/80">
                  {evalItem.journalNote || evalItem.summary || evalItem.notes || 'Değerlendirme notu girilmemiş.'}
                </p>
              </div>
            ))}
          </div>
        ) : (
          <div className="p-8 bg-slate-50 rounded-xl border border-dashed border-slate-200 text-center space-y-2">
            <Star className="w-8 h-8 text-slate-400 mx-auto" />
            <p className="text-xs font-semibold text-slate-700">Henüz Günlük Değerlendirme Yok</p>
            <p className="text-[11px] text-slate-500 max-w-md mx-auto">
              Optimum Android uygulamasında gün sonunda verdiğiniz yıldız puanları ve değerlendirme özetleri burada görünecektir. Ayrıca yukarıdaki "Günü Değerlendir" butonuna basarak web üzerinden de doğrudan gününüze not ve puan verebilirsiniz!
            </p>
          </div>
        )}
      </div>

      {/* ── MODAL: WEB ÜZERİNDEN GÜNÜ DEĞERLENDİR ── */}
      {showAddEvalModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white border border-slate-200 rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-5">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-bold text-slate-900">Günü Değerlendir 🌟</h3>
              <button
                onClick={() => setShowAddEvalModal(false)}
                className="p-1.5 text-slate-400 hover:text-slate-700 rounded-lg hover:bg-slate-100"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSaveEvaluation} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">Tarih</label>
                <input
                  type="date"
                  required
                  value={evalDate}
                  onChange={(e) => setEvalDate(e.target.value)}
                  className="w-full px-3.5 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:border-indigo-500"
                />
              </div>

              {/* Star Rating */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">
                  Günün Verimlilik Puanı ({evalRating}/5)
                </label>
                <div className="flex items-center gap-2">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      key={star}
                      type="button"
                      onClick={() => setEvalRating(star)}
                      className="p-1.5 hover:scale-110 transition"
                    >
                      <Star
                        className={`w-6 h-6 ${
                          star <= evalRating ? 'fill-amber-400 text-amber-400' : 'text-slate-300'
                        }`}
                      />
                    </button>
                  ))}
                </div>
              </div>

              {/* Mood Selector */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">Ruh Hali / Enerji</label>
                <div className="grid grid-cols-4 gap-2">
                  {[
                    { id: 4, label: '🤩 Harika' },
                    { id: 3, label: '😊 İyi' },
                    { id: 2, label: '😐 Nötr' },
                    { id: 1, label: '😞 Düşük' },
                  ].map((moodItem) => (
                    <button
                      key={moodItem.id}
                      type="button"
                      onClick={() => setEvalMood(moodItem.id)}
                      className={`py-2 text-xs font-bold rounded-xl border transition ${
                        evalMood === moodItem.id
                          ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs'
                          : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                      }`}
                    >
                      {moodItem.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Journal Note */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">
                  Günün Özeti & Değerlendirme Notu
                </label>
                <textarea
                  rows={3}
                  required
                  placeholder="Bugün en çok hangi hedefe odaklandınız? Neler iyi gitti, neler geliştirilebilir?"
                  value={evalNote}
                  onChange={(e) => setEvalNote(e.target.value)}
                  className="w-full px-3.5 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:border-indigo-500 leading-relaxed"
                />
              </div>

              <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowAddEvalModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  İptal
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-xl shadow-xs"
                >
                  Kaydet
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
