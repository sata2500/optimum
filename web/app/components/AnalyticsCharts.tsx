'use client';

import React from 'react';
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
  CheckCircle2,
  Calendar,
} from 'lucide-react';

const defaultCategoryData = [
  { name: 'Çalışma & Kodlama', value: 45, color: '#4f46e5' },
  { name: 'Okuma & Eğitim', value: 20, color: '#f59e0b' },
  { name: 'Spor & Sağlık', value: 15, color: '#10b981' },
  { name: 'Dinlenme & Mola', value: 12, color: '#ec4899' },
  { name: 'Sosyal & Aile', value: 8, color: '#8b5cf6' },
];

const defaultWeeklyData = [
  { day: 'Pzt', calisma: 6.5, spor: 1.0, okuma: 1.5 },
  { day: 'Sal', calisma: 7.0, spor: 1.5, okuma: 1.0 },
  { day: 'Çar', calisma: 8.0, spor: 0.5, okuma: 2.0 },
  { day: 'Per', calisma: 6.0, spor: 1.0, okuma: 1.5 },
  { day: 'Cum', calisma: 7.5, spor: 1.0, okuma: 1.0 },
  { day: 'Cmt', calisma: 4.0, spor: 2.0, okuma: 3.0 },
  { day: 'Paz', calisma: 3.5, spor: 1.5, okuma: 2.5 },
];

interface AnalyticsChartsProps {
  syncedData?: any;
}

export default function AnalyticsCharts({ syncedData }: AnalyticsChartsProps) {
  // Extract category distribution from synced logs if available
  const categoryData = React.useMemo(() => {
    if (syncedData?.logs && Array.isArray(syncedData.logs) && syncedData.logs.length > 0) {
      const counts: Record<string, number> = {};
      syncedData.logs.forEach((log: any) => {
        const cat = log.category || 'Genel';
        counts[cat] = (counts[cat] || 0) + 1;
      });

      const total = syncedData.logs.length;
      const palette = ['#4f46e5', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#0ea5e9'];
      return Object.entries(counts).map(([name, count], index) => ({
        name,
        value: Math.round((count / total) * 100),
        color: palette[index % palette.length],
      }));
    }
    return defaultCategoryData;
  }, [syncedData]);

  const totalLogs = syncedData?.logs?.length || 128;
  const evaluations = syncedData?.evaluations || [];

  return (
    <div className="space-y-6">
      {/* Page Title */}
      <div>
        <h2 className="text-xl font-bold text-slate-900 tracking-tight">
          Verimlilik & Analiz Raporu
        </h2>
        <p className="text-xs text-slate-500 mt-0.5">
          Zamanınızı nereye harcadığınızı, odaklanma sürelerinizi ve haftalık hedeflerinizi inceleyin.
        </p>
      </div>

      {/* 4 Key Metric KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* KPI 1: Toplam Odak Süresi */}
        <div className="dashboard-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-medium text-slate-500">Haftalık Odak Süresi</p>
            <h3 className="text-2xl font-bold text-slate-900">42.5 Saat</h3>
            <span className="inline-flex items-center text-[11px] font-semibold text-emerald-600">
              <TrendingUp className="w-3 h-3 mr-1" /> %14 Geçen Haftaya Göre
            </span>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600">
            <Clock className="w-6 h-6" />
          </div>
        </div>

        {/* KPI 2: Verimlilik Skoru */}
        <div className="dashboard-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-medium text-slate-500">Verimlilik Skoru</p>
            <h3 className="text-2xl font-bold text-slate-900">8.9 / 10</h3>
            <span className="inline-flex items-center text-[11px] font-semibold text-emerald-600">
              <CheckCircle2 className="w-3 h-3 mr-1" /> Yüksek Performans
            </span>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-emerald-50 border border-emerald-100 flex items-center justify-center text-emerald-600">
            <Award className="w-6 h-6" />
          </div>
        </div>

        {/* KPI 3: Tamamlanan Slotlar */}
        <div className="dashboard-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-medium text-slate-500">Kayıtlı Aktivite</p>
            <h3 className="text-2xl font-bold text-slate-900">{totalLogs} Slot</h3>
            <span className="inline-flex items-center text-[11px] font-semibold text-indigo-600">
              <Activity className="w-3 h-3 mr-1" /> Güncel Ay
            </span>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-sky-50 border border-sky-100 flex items-center justify-center text-sky-600">
            <Zap className="w-6 h-6" />
          </div>
        </div>

        {/* KPI 4: En Çok Yapılan */}
        <div className="dashboard-card p-5 flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-medium text-slate-500">En Aktif Alan</p>
            <h3 className="text-base font-bold text-slate-900 truncate max-w-[140px]">
              Çalışma & Kodlama
            </h3>
            <span className="inline-flex items-center text-[11px] font-semibold text-amber-600">
              <Star className="w-3 h-3 mr-1" /> %45 Toplam Pay
            </span>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-amber-50 border border-amber-100 flex items-center justify-center text-amber-600">
            <TrendingUp className="w-6 h-6" />
          </div>
        </div>
      </div>

      {/* Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Category Breakdown Donut Chart */}
        <div className="dashboard-card p-6 space-y-4">
          <div>
            <h3 className="text-base font-bold text-slate-900">
              Kategori Zaman Dağılımı (%)
            </h3>
            <p className="text-xs text-slate-500">Aktivitelerinizin kategorilere göre yüzdelik oranları</p>
          </div>

          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={categoryData}
                  cx="50%"
                  cy="50%"
                  innerRadius={65}
                  outerRadius={95}
                  paddingAngle={4}
                  dataKey="value"
                >
                  {categoryData.map((entry, index) => (
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
        </div>

        {/* Weekly Activity Volume Bar Chart */}
        <div className="dashboard-card p-6 space-y-4">
          <div>
            <h3 className="text-base font-bold text-slate-900">
              Haftalık Aktivite Hacmi (Saat)
            </h3>
            <p className="text-xs text-slate-500">Günlere göre harcanan toplam odak süreleri</p>
          </div>

          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={defaultWeeklyData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <XAxis
                  dataKey="day"
                  stroke="#94a3b8"
                  fontSize={12}
                  tickLine={false}
                  axisLine={{ stroke: '#e2e8f0' }}
                />
                <YAxis
                  stroke="#94a3b8"
                  fontSize={12}
                  tickLine={false}
                  axisLine={{ stroke: '#e2e8f0' }}
                />
                <Tooltip
                  formatter={(value: any) => [`${value} Saat`, '']}
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
                    <span className="text-xs text-slate-700 font-medium capitalize">
                      {value === 'calisma' ? 'Çalışma' : value === 'spor' ? 'Spor' : 'Okuma'}
                    </span>
                  )}
                />
                <Bar dataKey="calisma" fill="#4f46e5" radius={[4, 4, 0, 0]} name="calisma" />
                <Bar dataKey="okuma" fill="#f59e0b" radius={[4, 4, 0, 0]} name="okuma" />
                <Bar dataKey="spor" fill="#10b981" radius={[4, 4, 0, 0]} name="spor" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Daily Evaluations Section */}
      <div className="dashboard-card p-6 space-y-4">
        <div>
          <h3 className="text-base font-bold text-slate-900">
            Günlük Öz Değerlendirmeler & Puanlar
          </h3>
          <p className="text-xs text-slate-500">
            Android uygulamasında her günün sonunda girdiğiniz puanlar ve değerlendirme notları
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {evaluations.length > 0 ? (
            evaluations.slice(0, 6).map((evalItem: any, idx: number) => (
              <div key={idx} className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-indigo-600" />
                    {evalItem.date}
                  </span>
                  <div className="flex items-center gap-0.5 text-amber-500">
                    {Array.from({ length: 5 }).map((_, i) => (
                      <Star
                        key={i}
                        className={`w-3.5 h-3.5 ${
                          i < (evalItem.rating || 5) ? 'fill-amber-400 text-amber-400' : 'text-slate-300'
                        }`}
                      />
                    ))}
                  </div>
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">
                  {evalItem.summary || evalItem.notes || 'Günün hedefleri başarıyla tamamlandı.'}
                </p>
              </div>
            ))
          ) : (
            <>
              <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-indigo-600" />
                    Bugün
                  </span>
                  <div className="flex items-center gap-0.5 text-amber-400">
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                  </div>
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">
                  Tüm çalışma blokları planlandığı gibi tamamlandı. Odaklanma seviyesi çok yüksek bir gündü.
                </p>
              </div>

              <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-indigo-600" />
                    Dün
                  </span>
                  <div className="flex items-center gap-0.5 text-amber-400">
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                    <Star className="w-3.5 h-3.5 text-slate-300" />
                  </div>
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">
                  Öğleden sonraki toplantılar planı biraz aksattı ancak akşam saatlerinde telafi edildi.
                </p>
              </div>

              <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-indigo-600" />
                    2 Gün Önce
                  </span>
                  <div className="flex items-center gap-0.5 text-amber-400">
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                  </div>
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">
                  Spor ve çalışma dengesi kusursuzdu. Enerji seviyesi gün boyu yüksek kaldı.
                </p>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
