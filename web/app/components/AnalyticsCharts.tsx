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
import { TrendingUp, Award, Zap, Activity } from 'lucide-react';

const categoryData = [
  { name: 'Çalışma & Kodlama', value: 45, color: '#6366f1' },
  { name: 'Okuma & Eğitim', value: 20, color: '#f59e0b' },
  { name: 'Spor & Sağlık', value: 15, color: '#10b981' },
  { name: 'Dinlenme & Mola', value: 12, color: '#ec4899' },
  { name: 'Sosyal & Aile', value: 8, color: '#8b5cf6' },
];

const weeklyData = [
  { day: 'Pzt', calisma: 6.5, spor: 1, okuma: 1.5 },
  { day: 'Sal', calisma: 7.0, spor: 1.5, okuma: 1.0 },
  { day: 'Çar', calisma: 8.0, spor: 0.5, okuma: 2.0 },
  { day: 'Per', calisma: 6.0, spor: 1.0, okuma: 1.5 },
  { day: 'Cum', calisma: 7.5, spor: 1.0, okuma: 1.0 },
  { day: 'Cmt', calisma: 4.0, spor: 2.0, okuma: 3.0 },
  { day: 'Paz', calisma: 3.0, spor: 1.5, okuma: 2.5 },
];

interface AnalyticsChartsProps {
  syncedData?: any;
}

export default function AnalyticsCharts({ syncedData }: AnalyticsChartsProps = {}) {
  return (
    <div className="space-y-6">
      {/* Top Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="glass-panel p-4 rounded-2xl flex items-center justify-between">
          <div>
            <p className="text-xs text-gray-400 font-medium">Haftalık Çalışma</p>
            <h3 className="text-2xl font-bold text-white mt-1">42.0 Saat</h3>
            <span className="text-[11px] text-emerald-400 font-semibold">↑ %12 Artış</span>
          </div>
          <div className="w-12 h-12 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400">
            <Zap className="w-6 h-6" />
          </div>
        </div>

        <div className="glass-panel p-4 rounded-2xl flex items-center justify-between">
          <div>
            <p className="text-xs text-gray-400 font-medium">Verimlilik Skoru</p>
            <h3 className="text-2xl font-bold text-white mt-1">8.8 / 10</h3>
            <span className="text-[11px] text-emerald-400 font-semibold">Harika Performans</span>
          </div>
          <div className="w-12 h-12 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
            <Award className="w-6 h-6" />
          </div>
        </div>

        <div className="glass-panel p-4 rounded-2xl flex items-center justify-between">
          <div>
            <p className="text-xs text-gray-400 font-medium">Tamamlanan Aktivite</p>
            <h3 className="text-2xl font-bold text-white mt-1">128 Slot</h3>
            <span className="text-[11px] text-indigo-400 font-semibold">Bu Ay</span>
          </div>
          <div className="w-12 h-12 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
            <Activity className="w-6 h-6" />
          </div>
        </div>

        <div className="glass-panel p-4 rounded-2xl flex items-center justify-between">
          <div>
            <p className="text-xs text-gray-400 font-medium">Hedef Odaklılık</p>
            <h3 className="text-2xl font-bold text-white mt-1">%91</h3>
            <span className="text-[11px] text-emerald-400 font-semibold">Planlanan vs Gerçekleşen</span>
          </div>
          <div className="w-12 h-12 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
            <TrendingUp className="w-6 h-6" />
          </div>
        </div>
      </div>

      {/* Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Category Breakdown Pie Chart */}
        <div className="glass-panel p-6 rounded-2xl space-y-4">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <span>Kategori Dağılım Oranı (%)</span>
          </h3>

          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={categoryData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={90}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {categoryData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{ backgroundColor: '#111827', borderColor: '#374151', borderRadius: '12px' }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 pt-2 border-t border-gray-800">
            {categoryData.map((item) => (
              <div key={item.name} className="flex items-center gap-2 text-xs">
                <span className="w-3 h-3 rounded-full" style={{ backgroundColor: item.color }} />
                <span className="text-gray-300 font-medium truncate">{item.name}</span>
                <span className="text-gray-400 font-bold ml-auto">%{item.value}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Weekly Activity Bar Chart */}
        <div className="glass-panel p-6 rounded-2xl space-y-4">
          <h3 className="text-base font-bold text-white">Haftalık Saat Dağılımı</h3>

          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={weeklyData}>
                <XAxis dataKey="day" stroke="#9ca3af" fontSize={12} />
                <YAxis stroke="#9ca3af" fontSize={12} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#111827', borderColor: '#374151', borderRadius: '12px' }}
                />
                <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '10px' }} />
                <Bar dataKey="calisma" name="Çalışma (Saat)" fill="#6366f1" radius={[4, 4, 0, 0]} />
                <Bar dataKey="spor" name="Spor (Saat)" fill="#10b981" radius={[4, 4, 0, 0]} />
                <Bar dataKey="okuma" name="Okuma (Saat)" fill="#f59e0b" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
}
