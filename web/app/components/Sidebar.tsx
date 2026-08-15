'use client';

import React from 'react';
import {
  CalendarDays,
  BarChart3,
  Tag,
  Cloud,
  Smartphone,
  Sparkles,
  ArrowUpRight,
} from 'lucide-react';

interface SidebarProps {
  activeTab: 'schedule' | 'analytics' | 'categories' | 'profile';
  setActiveTab: (tab: 'schedule' | 'analytics' | 'categories' | 'profile') => void;
  syncedData?: any;
}

export default function Sidebar({ activeTab, setActiveTab, syncedData }: SidebarProps) {
  const categoriesCount = syncedData?.categories?.length || 0;
  const logsCount = syncedData?.logs?.length || 0;

  const menuItems = [
    {
      id: 'schedule',
      label: 'Zaman Çizelgesi',
      description: 'Günlük slotlar & aktiviteler',
      icon: CalendarDays,
      badge: logsCount > 0 ? `${logsCount} kayıt` : undefined,
    },
    {
      id: 'analytics',
      label: 'Analiz & Değerlendirme',
      description: 'Verimlilik & grafikler',
      icon: BarChart3,
      badge: undefined,
    },
    {
      id: 'categories',
      label: 'Kategoriler',
      description: 'Etiketler & renkler',
      icon: Tag,
      badge: categoriesCount > 0 ? `${categoriesCount}` : undefined,
    },
    {
      id: 'profile',
      label: 'Bulut & Senkronizasyon',
      description: 'Hesap durumu & eşitleme',
      icon: Cloud,
      badge: undefined,
    },
  ] as const;

  return (
    <aside className="w-72 border-r border-slate-200 bg-white p-5 flex flex-col justify-between shrink-0 min-h-[calc(100vh-4rem)]">
      <div className="space-y-6">
        {/* Section Header */}
        <div>
          <p className="px-3 text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-3">
            Ana Menü
          </p>

          <div className="space-y-1.5">
            {menuItems.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => setActiveTab(item.id)}
                  className={`w-full flex items-center justify-between p-3 rounded-xl font-medium text-xs transition text-left ${
                    isActive
                      ? 'bg-indigo-50/80 text-indigo-900 border border-indigo-200/80 shadow-xs'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50 border border-transparent'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div
                      className={`w-8 h-8 rounded-lg flex items-center justify-center transition ${
                        isActive
                          ? 'bg-indigo-600 text-white shadow-xs'
                          : 'bg-slate-100 text-slate-500 group-hover:bg-slate-200'
                      }`}
                    >
                      <Icon className="w-4 h-4" />
                    </div>
                    <div>
                      <p
                        className={`font-semibold text-xs leading-tight ${
                          isActive ? 'text-indigo-950' : 'text-slate-700'
                        }`}
                      >
                        {item.label}
                      </p>
                      <p className="text-[10px] text-slate-400 mt-0.5">{item.description}</p>
                    </div>
                  </div>

                  {item.badge && (
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                        isActive
                          ? 'bg-indigo-200/60 text-indigo-800'
                          : 'bg-slate-100 text-slate-500'
                      }`}
                    >
                      {item.badge}
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Bottom Sync Info Card */}
      <div className="p-4 bg-gradient-to-br from-slate-50 to-indigo-50/40 rounded-2xl border border-indigo-100 text-xs space-y-2">
        <div className="flex items-center gap-2 text-indigo-700 font-semibold">
          <Smartphone className="w-4 h-4" />
          <span>Android ile Eşzamanlı</span>
        </div>
        <p className="text-slate-500 text-[11px] leading-relaxed">
          Telefonda kaydettiğiniz her aktivite Neon PostgreSQL bulut veritabanında anında güncellenir.
        </p>
      </div>
    </aside>
  );
}
