'use client';

import React from 'react';
import { LayoutGrid, PieChart, Tags, UserCheck, Smartphone } from 'lucide-react';

interface SidebarProps {
  activeTab: 'schedule' | 'analytics' | 'categories' | 'profile';
  setActiveTab: (tab: 'schedule' | 'analytics' | 'categories' | 'profile') => void;
}

export default function Sidebar({ activeTab, setActiveTab }: SidebarProps) {
  const menuItems = [
    { id: 'schedule', label: 'Çizelge (Tablo)', icon: LayoutGrid },
    { id: 'analytics', label: 'Analiz & Değerlendirme', icon: PieChart },
    { id: 'categories', label: 'Kategoriler', icon: Tags },
    { id: 'profile', label: 'Profil & Bulut Durumu', icon: UserCheck },
  ] as const;

  return (
    <aside className="w-64 border-r border-gray-800 bg-gray-950/40 p-4 flex flex-col justify-between shrink-0 min-h-[calc(100vh-4rem)]">
      <div className="space-y-1">
        <p className="px-3 text-[10px] font-bold text-gray-500 uppercase tracking-wider mb-2">Menü</p>
        {menuItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id)}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl font-medium text-xs transition ${
                isActive
                  ? 'bg-indigo-600/20 text-indigo-400 border border-indigo-500/30'
                  : 'text-gray-400 hover:text-gray-200 hover:bg-gray-900/60'
              }`}
            >
              <Icon className={`w-4 h-4 ${isActive ? 'text-indigo-400' : 'text-gray-400'}`} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </div>

      <div className="p-3 bg-gradient-to-br from-indigo-950/40 to-purple-950/40 rounded-xl border border-indigo-900/30 text-xs">
        <div className="flex items-center gap-2 text-indigo-400 font-semibold mb-1">
          <Smartphone className="w-4 h-4" />
          <span>Android Senkronizasyonu</span>
        </div>
        <p className="text-gray-400 text-[11px]">
          Optimum Mobil uygulamasında girdiğiniz tüm veriler anında burada güncellenir.
        </p>
      </div>
    </aside>
  );
}
