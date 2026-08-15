'use client';

import React, { useState, useMemo } from 'react';
import { Tag, Plus, CheckCircle2, Sparkles, Layers, ListChecks } from 'lucide-react';

interface Category {
  id: number | string;
  name: string;
  color: string;
  activityCount: number;
  activities?: string[];
}

const defaultCategories: Category[] = [
  {
    id: 1,
    name: 'Çalışma & Kodlama',
    color: '#4f46e5',
    activityCount: 5,
    activities: ['Mimari Tasarım', 'Kodlama & Geliştirme', 'Hata Ayıklama', 'Kod İnceleme', 'Dokümantasyon'],
  },
  {
    id: 2,
    name: 'Spor & Sağlık',
    color: '#10b981',
    activityCount: 4,
    activities: ['Sabah Yürüyüşü', 'Ağırlık Antrenmanı', 'Esneme & Yoga', 'Öğle Molası'],
  },
  {
    id: 3,
    name: 'Okuma & Eğitim',
    color: '#f59e0b',
    activityCount: 3,
    activities: ['Teknik Makaleler', 'Kitap Okuma', 'Online Kurslar'],
  },
  {
    id: 4,
    name: 'Dinlenme & Mola',
    color: '#ec4899',
    activityCount: 2,
    activities: ['Kahve Molası', 'Kısa Uyku & Dinlenme'],
  },
  {
    id: 5,
    name: 'Sosyal & Aile',
    color: '#8b5cf6',
    activityCount: 3,
    activities: ['Aile Zamanı', 'Arkadaşlar ile Sohbet', 'Görüşmeler'],
  },
  {
    id: 6,
    name: 'Rutin & İşler',
    color: '#0ea5e9',
    activityCount: 2,
    activities: ['E-posta Kontrolü', 'Ev İşleri & Planlama'],
  },
];

interface CategoryManagerProps {
  syncedData?: any;
}

export default function CategoryManager({ syncedData }: CategoryManagerProps) {
  const [categories, setCategories] = useState<Category[]>(defaultCategories);
  const [newName, setNewName] = useState('');
  const [newColor, setNewColor] = useState('#4f46e5');

  // If synced data has categories and activities, merge them
  const displayedCategories: Category[] = useMemo(() => {
    if (syncedData?.categories && Array.isArray(syncedData.categories) && syncedData.categories.length > 0) {
      const activitiesList = syncedData.activities || [];
      return syncedData.categories.map((c: any) => {
        const catActivities = activitiesList
          .filter((a: any) => a.categoryId === c.id || a.category === c.name)
          .map((a: any) => a.name);

        return {
          id: c.id,
          name: c.name,
          color: c.colorHex || c.color || '#4f46e5',
          activityCount: catActivities.length || c.activityCount || 1,
          activities: catActivities.length > 0 ? catActivities : ['Genel Aktivite'],
        };
      });
    }
    return categories;
  }, [syncedData, categories]);

  const handleAddCategory = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim()) return;

    setCategories([
      ...categories,
      {
        id: Date.now(),
        name: newName.trim(),
        color: newColor,
        activityCount: 1,
        activities: ['Varsayılan Aktivite'],
      },
    ]);
    setNewName('');
  };

  return (
    <div className="space-y-6">
      {/* Header Info */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 tracking-tight">
            Kategoriler & Aktiviteler
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Optimum Android uygulamanızda tanımlı tüm kategoriler ve bağlı aktivite havuzu
          </p>
        </div>

        <div className="px-3.5 py-1.5 bg-indigo-50 border border-indigo-100 text-indigo-700 text-xs font-semibold rounded-xl flex items-center gap-2">
          <Layers className="w-4 h-4 text-indigo-600" />
          <span>{displayedCategories.length} Aktif Kategori</span>
        </div>
      </div>

      {/* Main Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Categories List */}
        <div className="lg:col-span-2 space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {displayedCategories.map((cat) => (
              <div
                key={cat.id}
                className="dashboard-card dashboard-card-hover p-5 space-y-3 relative overflow-hidden border-t-4"
                style={{ borderTopColor: cat.color }}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <span
                      className="w-3.5 h-3.5 rounded-full shadow-xs shrink-0"
                      style={{ backgroundColor: cat.color }}
                    />
                    <h3 className="text-sm font-bold text-slate-900">{cat.name}</h3>
                  </div>
                  <span className="text-[11px] font-semibold text-slate-400">
                    {cat.activityCount} aktivite
                  </span>
                </div>

                {/* Sub activities pill list */}
                {cat.activities && cat.activities.length > 0 && (
                  <div className="flex flex-wrap gap-1.5 pt-1">
                    {cat.activities.map((act, i) => (
                      <span
                        key={i}
                        className="px-2 py-0.5 text-[10px] font-medium bg-slate-100 text-slate-600 rounded-md flex items-center gap-1"
                      >
                        <ListChecks className="w-3 h-3 text-slate-400" />
                        {act}
                      </span>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Add Category Form Card */}
        <div className="dashboard-card p-6 h-fit space-y-4">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-indigo-50 flex items-center justify-center text-indigo-600">
              <Plus className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-slate-900">Yeni Kategori Ekle</h3>
              <p className="text-[11px] text-slate-400">Web üzerinde önizleme oluşturun</p>
            </div>
          </div>

          <form onSubmit={handleAddCategory} className="space-y-4 pt-1">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Kategori Adı
              </label>
              <input
                type="text"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                placeholder="Örn: Yabancı Dil Eğitimi"
                className="w-full px-3.5 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:border-indigo-500 text-slate-800"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Renk Etiketi
              </label>
              <div className="flex items-center gap-3 p-2 bg-slate-50 border border-slate-200 rounded-xl">
                <input
                  type="color"
                  value={newColor}
                  onChange={(e) => setNewColor(e.target.value)}
                  className="w-8 h-8 rounded-lg bg-transparent border-0 cursor-pointer"
                />
                <span className="text-xs font-mono text-slate-600 font-semibold">{newColor}</span>
              </div>
            </div>

            <button
              type="submit"
              className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs rounded-xl transition shadow-sm shadow-indigo-600/20"
            >
              Kategori Oluştur
            </button>
          </form>

          <div className="p-3 bg-slate-50 border border-slate-200/80 rounded-xl text-[11px] text-slate-500 space-y-1">
            <p className="font-semibold text-slate-700">💡 İpucu</p>
            <p>
              Android uygulamanızda eklediğiniz kategoriler otomatik olarak burada listelenir ve buluta yedeklenir.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
