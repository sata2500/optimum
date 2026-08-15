'use client';

import React, { useMemo } from 'react';
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
  Info,
} from 'lucide-react';

interface AnalyticsChartsProps {
  syncedData?: any;
}

export default function AnalyticsCharts({ syncedData }: AnalyticsChartsProps) {
  const logs = useMemo(() => syncedData?.logs || [], [syncedData]);
  const evaluations = useMemo(() => syncedData?.evaluations || [], [syncedData]);
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

  // 1. Compute Category Distribution from REAL logs
  const categoryDistribution = useMemo(() => {
    if (!logs || logs.length === 0) return [];

    const counts: Record<string, { count: number; color: string }> = {};
    logs.forEach((log: any) => {
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

    const total = logs.length;
    return Object.entries(counts).map(([name, data]) => ({
      name,
      value: Math.round((data.count / total) * 100),
      rawCount: data.count,
      color: data.color,
    }));
  }, [logs, categoriesMap]);

  // 2. Compute Weekly / Daily Volume from REAL logs
  const volumeData = useMemo(() => {
    if (!logs || logs.length === 0) return [];

    // Group logs by date (last 7 recorded dates or current week)
    const dateGroups: Record<string, number> = {};
    logs.forEach((log: any) => {
      const logDate = log.date || log.createdAt?.split('T')[0] || log.syncedAt?.split('T')[0];
      if (logDate) {
        dateGroups[logDate] = (dateGroups[logDate] || 0) + 1; // Each slot = 0.5 hour or 1 slot
      }
    });

    return Object.entries(dateGroups)
      .sort(([a], [b]) => a.localeCompare(b))
      .slice(-7)
      .map(([date, count]) => {
        const d = new Date(date);
        const dayName = d.toLocaleDateString('tr-TR', { weekday: 'short', day: 'numeric', month: 'numeric' });
        return {
          date: dayName,
          fullDate: date,
          saat: +(count * 0.5).toFixed(1), // Hours
          slotSayisi: count,
        };
      });
  }, [logs]);

  // 3. Compute Real KPI Metrics
  const totalSlots = logs.length;
  const totalHours = (totalSlots * 0.5).toFixed(1);
  const avgRating = useMemo(() => {
    if (evaluations.length === 0) return 0;
    const sum = evaluations.reduce((acc: number, curr: any) => acc + (curr.rating || 0), 0);
    return (sum / evaluations.length).toFixed(1);
  }, [evaluations]);

  const topCategory = categoryDistribution[0]?.name || 'Henüz Yok';

  // Mood helper
  const renderMoodIcon = (mood: number) => {
    switch (mood) {
      case 4:
        return <span className="text-sm">🤩 Harika</span>;
      case 3:
        return <span className="text-sm">😊 İyi</span>;
      case 2:
        return <span className="text-sm">😐 Nötr</span>;
      case 1:
        return <span className="text-sm">😞 Düşük</span>;
      default:
        return null;
    }
  };

  const hasData = totalSlots > 0 || evaluations.length > 0;

  return (
    <div className="space-y-6">
      {/* Title */}
      <div>
        <h2 className="text-xl font-bold text-slate-900 tracking-tight">
          Verimlilik & Analiz Raporu
        </h2>
        <p className="text-xs text-slate-500 mt-0.5">
          Android uygulamanızdan eşitlenen gerçek zaman verileriniz ve öz değerlendirmeleriniz
        </p>
      </div>

      {/* 4 Real KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* KPI 1: Toplam Odak Süresi */}
        <div className="dashboard-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-medium text-slate-500">Toplam Odaklanma</p>
            <h3 className="text-2xl font-bold text-slate-900">
              {totalSlots > 0 ? `${totalHours} Saat` : '0 Saat'}
            </h3>
            <span className="text-[11px] font-semibold text-indigo-600">
              {totalSlots} Kayıtlı Slot
            </span>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600">
            <Clock className="w-6 h-6" />
          </div>
        </div>

        {/* KPI 2: Ortalama Değerlendirme Puanı */}
        <div className="dashboard-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-medium text-slate-500">Ort. Günlük Puan</p>
            <h3 className="text-2xl font-bold text-slate-900">
              {+avgRating > 0 ? `${avgRating} / 5` : '-'}
            </h3>
            <span className="text-[11px] font-semibold text-amber-600">
              {evaluations.length} Değerlendirme
            </span>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-amber-50 border border-amber-100 flex items-center justify-center text-amber-600">
            <Star className="w-6 h-6" />
          </div>
        </div>

        {/* KPI 3: Aktif Kategori Sayısı */}
        <div className="dashboard-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-medium text-slate-500">Tanımlı Kategori</p>
            <h3 className="text-2xl font-bold text-slate-900">
              {categories.length} Kategori
            </h3>
            <span className="text-[11px] font-semibold text-emerald-600">
              {syncedData?.activities?.length || 0} Aktif Aktivite
            </span>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-emerald-50 border border-emerald-100 flex items-center justify-center text-emerald-600">
            <Layers className="w-6 h-6" />
          </div>
        </div>

        {/* KPI 4: En Çok Zaman Ayrılan Kategori */}
        <div className="dashboard-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-medium text-slate-500">En Çok Zaman Ayrılan</p>
            <h3 className="text-sm font-bold text-slate-900 truncate max-w-[140px]" title={topCategory}>
              {topCategory}
            </h3>
            <span className="text-[11px] font-semibold text-sky-600">
              {categoryDistribution[0] ? `%${categoryDistribution[0].value} Pay` : 'Kayıt Yok'}
            </span>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-sky-50 border border-sky-100 flex items-center justify-center text-sky-600">
            <TrendingUp className="w-6 h-6" />
          </div>
        </div>
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Category Breakdown Donut Chart */}
        <div className="dashboard-card p-6 space-y-4">
          <div>
            <h3 className="text-base font-bold text-slate-900">
              Kategori Zaman Dağılımı (%)
            </h3>
            <p className="text-xs text-slate-500">Telefonunuzda kaydettiğiniz aktivitelerin yüzdelik oranları</p>
          </div>

          {categoryDistribution.length > 0 ? (
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
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
                    formatter={(value: any) => [`%${value}`, 'Oran']}
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
              </ResponsiveContainer>
            </div>
          ) : (
            <div className="h-72 flex flex-col items-center justify-center text-center p-6 bg-slate-50 rounded-xl border border-dashed border-slate-200 space-y-2">
              <Activity className="w-8 h-8 text-slate-400" />
              <p className="text-xs font-semibold text-slate-700">Henüz Kategori Verisi Yok</p>
              <p className="text-[11px] text-slate-500 max-w-xs">
                Telefonunuzdan aktivitelerinizi kaydettikten sonra 'Şimdi Senkronize Et' dediğinizde grafik otomatik oluşacaktır.
              </p>
            </div>
          )}
        </div>

        {/* Daily Volume Bar Chart */}
        <div className="dashboard-card p-6 space-y-4">
          <div>
            <h3 className="text-base font-bold text-slate-900">
              Günlük Aktivite Hacmi (Saat)
            </h3>
            <p className="text-xs text-slate-500">Günlere göre tamamlanan çalışma ve aktivite süreleri</p>
          </div>

          {volumeData.length > 0 ? (
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={volumeData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                  <XAxis
                    dataKey="date"
                    stroke="#94a3b8"
                    fontSize={11}
                    tickLine={false}
                    axisLine={{ stroke: '#e2e8f0' }}
                  />
                  <YAxis
                    stroke="#94a3b8"
                    fontSize={11}
                    tickLine={false}
                    axisLine={{ stroke: '#e2e8f0' }}
                  />
                  <Tooltip
                    formatter={(value: any) => [`${value} Saat`, 'Odak Süresi']}
                    contentStyle={{
                      backgroundColor: '#ffffff',
                      borderColor: '#e2e8f0',
                      borderRadius: '0.75rem',
                      fontSize: '12px',
                      boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
                    }}
                  />
                  <Bar dataKey="saat" fill="#4f46e5" radius={[6, 6, 0, 0]} name="Odak Süresi (Saat)" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <div className="h-72 flex flex-col items-center justify-center text-center p-6 bg-slate-50 rounded-xl border border-dashed border-slate-200 space-y-2">
              <Clock className="w-8 h-8 text-slate-400" />
              <p className="text-xs font-semibold text-slate-700">Henüz Günlük Kayıt Yok</p>
              <p className="text-[11px] text-slate-500 max-w-xs">
                Aktivite kayıtlarınız eşitlendiğinde günlere göre çalışma hacminiz burada listelenecektir.
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Daily Evaluations Section (REAL USER DATA ONLY) */}
      <div className="dashboard-card p-6 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-base font-bold text-slate-900">
              Günlük Öz Değerlendirmeler & Puanlar
            </h3>
            <p className="text-xs text-slate-500">
              Android uygulamanızda her günün sonunda verdiğiniz yıldız puanları ve değerlendirme notlarınız
            </p>
          </div>
          <span className="text-xs font-semibold text-indigo-700 bg-indigo-50 px-3 py-1 rounded-full border border-indigo-100">
            {evaluations.length} Kayıtlı Gün
          </span>
        </div>

        {evaluations.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {evaluations.map((evalItem: any, idx: number) => (
              <div key={idx} className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-2.5">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-slate-800 flex items-center gap-1.5">
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
                  <div className="text-xs font-medium text-slate-600 flex items-center gap-1">
                    <span>Mod:</span> {renderMoodIcon(evalItem.mood)}
                  </div>
                )}

                <p className="text-xs text-slate-600 leading-relaxed">
                  {evalItem.journalNote || evalItem.summary || evalItem.notes || 'Değerlendirme notu girilmemiş.'}
                </p>
              </div>
            ))}
          </div>
        ) : (
          <div className="p-8 bg-slate-50 rounded-xl border border-dashed border-slate-200 text-center space-y-2">
            <Star className="w-8 h-8 text-slate-400 mx-auto" />
            <p className="text-xs font-semibold text-slate-700">Henüz Günlük Değerlendirme Girilmemiş</p>
            <p className="text-[11px] text-slate-500 max-w-md mx-auto">
              Optimum Android uygulamasında "Analiz & Değerlendirme" sekmesinden günlerinize yıldız verip not eklediğinizde ve senkronize ettiğinizde tüm değerlendirmeleriniz burada görünecektir.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
