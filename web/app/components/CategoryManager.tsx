'use client';

import React, { useState } from 'react';
import { Plus, Trash2, Edit2, Tags, Layers } from 'lucide-react';

interface Category {
  id: number;
  name: string;
  color: string;
  activityCount: number;
}

const initialCategories: Category[] = [
  { id: 1, name: 'Çalışma & Kodlama', color: '#6366f1', activityCount: 12 },
  { id: 2, name: 'Spor & Sağlık', color: '#10b981', activityCount: 5 },
  { id: 3, name: 'Okuma & Eğitim', color: '#f59e0b', activityCount: 8 },
  { id: 4, name: 'Dinlenme & Mola', color: '#ec4899', activityCount: 4 },
  { id: 5, name: 'Sosyal & Aile', color: '#8b5cf6', activityCount: 6 },
];

interface CategoryManagerProps {
  syncedData?: any;
}

export default function CategoryManager({ syncedData }: CategoryManagerProps = {}) {
  const [categories, setCategories] = useState<Category[]>(initialCategories);
  const [newName, setNewName] = useState('');
  const [newColor, setNewColor] = useState('#3b82f6');

  const handleAdd = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim()) return;

    setCategories([
      ...categories,
      {
        id: Date.now(),
        name: newName,
        color: newColor,
        activityCount: 0,
      },
    ]);
    setNewName('');
  };

  const handleDelete = (id: number) => {
    setCategories(categories.filter((c) => c.id !== id));
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-white">Kategori ve Aktivite Yönetimi</h2>
          <p className="text-xs text-gray-400">Optimum Android uygulaması ile eşzamanlanan kategorileriniz</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Category List */}
        <div className="lg:col-span-2 space-y-3">
          {categories.map((cat) => (
            <div
              key={cat.id}
              className="glass-panel p-4 rounded-xl flex items-center justify-between gap-4"
            >
              <div className="flex items-center gap-3">
                <span
                  className="w-5 h-5 rounded-lg shadow-sm"
                  style={{ backgroundColor: cat.color }}
                />
                <div>
                  <h4 className="text-sm font-semibold text-white">{cat.name}</h4>
                  <p className="text-xs text-gray-400">{cat.activityCount} kayıtlı aktivite</p>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => handleDelete(cat.id)}
                  className="p-2 text-gray-400 hover:text-red-400 rounded-lg hover:bg-gray-800 transition"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>

        {/* Add Category Form */}
        <div className="glass-panel p-5 rounded-2xl h-fit space-y-4">
          <h3 className="text-sm font-bold text-white flex items-center gap-2">
            <Plus className="w-4 h-4 text-indigo-400" />
            <span>Yeni Kategori Ekle</span>
          </h3>

          <form onSubmit={handleAdd} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-gray-400 mb-1">
                Kategori Adı
              </label>
              <input
                type="text"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                placeholder="Örn: Dil Öğrenimi"
                className="w-full bg-gray-900 border border-gray-800 text-white rounded-xl p-2.5 text-xs focus:outline-none focus:border-indigo-500"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-400 mb-1">
                Renk Etiketi
              </label>
              <div className="flex items-center gap-3">
                <input
                  type="color"
                  value={newColor}
                  onChange={(e) => setNewColor(e.target.value)}
                  className="w-10 h-10 rounded-xl bg-transparent border-0 cursor-pointer"
                />
                <span className="text-xs text-gray-300 font-mono">{newColor}</span>
              </div>
            </div>

            <button
              type="submit"
              className="w-full py-2.5 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white font-semibold text-xs rounded-xl transition shadow-lg shadow-indigo-600/25"
            >
              Kategori Oluştur
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
