'use client';

import React, { useState, useEffect, useMemo } from 'react';
import {
  Tag,
  Plus,
  GripVertical,
  Layers,
  ListChecks,
  CheckCircle2,
  Trash2,
  Edit2,
  Save,
  Sparkles,
  ArrowUpDown,
  X,
} from 'lucide-react';

interface Activity {
  id: number | string;
  categoryId: number | string;
  name: string;
  shortCode?: string;
  activityNumber?: number;
  durationMinutes?: number;
  displayOrder?: number;
}

interface Category {
  id: number | string;
  name: string;
  color: string;
  code?: string;
  displayOrder?: number;
  activities: Activity[];
}

interface CategoryManagerProps {
  syncedData?: any;
}

export default function CategoryManager({ syncedData }: CategoryManagerProps) {
  const [categories, setCategories] = useState<Category[]>([]);
  const [draggedCategoryIndex, setDraggedCategoryIndex] = useState<number | null>(null);
  const [draggedActivityInfo, setDraggedActivityInfo] = useState<{
    catId: number | string;
    actIndex: number;
  } | null>(null);

  // New Category form state
  const [showAddCatModal, setShowAddCatModal] = useState(false);
  const [newCatName, setNewCatName] = useState('');
  const [newCatColor, setNewCatColor] = useState('#4f46e5');
  const [newCatCode, setNewCatCode] = useState('');

  // New Activity form state
  const [selectedCatForNewAct, setSelectedCatForNewAct] = useState<Category | null>(null);
  const [newActName, setNewActName] = useState('');
  const [newActShortCode, setNewActShortCode] = useState('');

  // Initialize from syncedData or fallback defaults
  useEffect(() => {
    if (syncedData?.categories && Array.isArray(syncedData.categories) && syncedData.categories.length > 0) {
      const allActs: any[] = syncedData.activities || [];
      const parsed: Category[] = syncedData.categories.map((c: any, idx: number) => {
        const catActs = allActs
          .filter((a: any) => a.categoryId === c.id || a.category === c.name)
          .map((a: any, aIdx: number) => ({
            id: a.id || `act-${idx}-${aIdx}`,
            categoryId: c.id,
            name: a.name,
            shortCode: a.shortCode || `${aIdx + 1}`,
            activityNumber: a.activityNumber || aIdx + 1,
            durationMinutes: a.durationMinutes || 60,
            displayOrder: a.displayOrder || aIdx,
          }));

        return {
          id: c.id,
          name: c.name,
          color: c.colorHex || c.color || '#4f46e5',
          code: c.code || (c.name ? c.name.charAt(0).toUpperCase() : 'K'),
          displayOrder: c.displayOrder || idx,
          activities: catActs.length > 0 ? catActs : [
            { id: `default-${c.id}`, categoryId: c.id, name: 'Genel Görev', shortCode: '1', activityNumber: 1 }
          ],
        };
      });
      setCategories(parsed);
    } else {
      // Default initial categories
      setCategories([
        {
          id: 1,
          name: 'Çalışma & Kodlama',
          color: '#4f46e5',
          code: 'Z',
          displayOrder: 0,
          activities: [
            { id: 101, categoryId: 1, name: 'Mimari Tasarım & Planlama', shortCode: '1', activityNumber: 1 },
            { id: 102, categoryId: 1, name: 'Kodlama & Geliştirme', shortCode: '2', activityNumber: 2 },
            { id: 103, categoryId: 1, name: 'Hata Ayıklama & Test', shortCode: 'b', activityNumber: 3 },
          ],
        },
        {
          id: 2,
          name: 'Spor & Sağlık',
          color: '#10b981',
          code: 'S',
          displayOrder: 1,
          activities: [
            { id: 201, categoryId: 2, name: 'Sabah Yürüyüşü', shortCode: 't', activityNumber: 1 },
            { id: 202, categoryId: 2, name: 'Ağırlık Antrenmanı', shortCode: '2', activityNumber: 2 },
          ],
        },
        {
          id: 3,
          name: 'Okuma & Eğitim',
          color: '#f59e0b',
          code: 'O',
          displayOrder: 2,
          activities: [
            { id: 301, categoryId: 3, name: 'Kitap Okuma', shortCode: '1', activityNumber: 1 },
            { id: 302, categoryId: 3, name: 'Teknik Makaleler', shortCode: '2', activityNumber: 2 },
          ],
        },
      ]);
    }
  }, [syncedData]);

  // ── Drag and Drop Handlers for Categories ──
  const handleCategoryDragStart = (e: React.DragEvent, index: number) => {
    setDraggedCategoryIndex(index);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleCategoryDragOver = (e: React.DragEvent, index: number) => {
    e.preventDefault();
    if (draggedCategoryIndex === null || draggedCategoryIndex === index) return;

    const reordered = [...categories];
    const item = reordered.splice(draggedCategoryIndex, 1)[0];
    reordered.splice(index, 0, item);
    setDraggedCategoryIndex(index);
    setCategories(reordered);
  };

  const handleCategoryDragEnd = () => {
    setDraggedCategoryIndex(null);
  };

  // ── Drag and Drop Handlers for Activities within a Category ──
  const handleActivityDragStart = (e: React.DragEvent, catId: number | string, actIndex: number) => {
    e.stopPropagation();
    setDraggedActivityInfo({ catId, actIndex });
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleActivityDragOver = (e: React.DragEvent, catId: number | string, actIndex: number) => {
    e.preventDefault();
    e.stopPropagation();
    if (!draggedActivityInfo || draggedActivityInfo.catId !== catId || draggedActivityInfo.actIndex === actIndex) {
      return;
    }

    const nextCategories = categories.map((cat) => {
      if (cat.id === catId) {
        const nextActs = [...cat.activities];
        const item = nextActs.splice(draggedActivityInfo.actIndex, 1)[0];
        nextActs.splice(actIndex, 0, item);
        return { ...cat, activities: nextActs };
      }
      return cat;
    });

    setDraggedActivityInfo({ catId, actIndex });
    setCategories(nextCategories);
  };

  const handleActivityDragEnd = (e: React.DragEvent) => {
    e.stopPropagation();
    setDraggedActivityInfo(null);
  };

  // Add Category Handler
  const handleAddCategory = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCatName.trim()) return;

    const code = newCatCode.trim() || newCatName.trim().charAt(0).toUpperCase();
    const newCat: Category = {
      id: Date.now(),
      name: newCatName.trim(),
      color: newCatColor,
      code: code,
      displayOrder: categories.length,
      activities: [
        {
          id: Date.now() + 1,
          categoryId: Date.now(),
          name: 'Genel Aktivite',
          shortCode: '1',
          activityNumber: 1,
        },
      ],
    };

    setCategories([...categories, newCat]);
    setNewCatName('');
    setNewCatCode('');
    setShowAddCatModal(false);
  };

  // Add Activity Handler
  const handleAddActivity = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedCatForNewAct || !newActName.trim()) return;

    const updated = categories.map((cat) => {
      if (cat.id === selectedCatForNewAct.id) {
        const nextNumber = cat.activities.length + 1;
        const newAct: Activity = {
          id: Date.now(),
          categoryId: cat.id,
          name: newActName.trim(),
          shortCode: newActShortCode.trim() || `${nextNumber}`,
          activityNumber: nextNumber,
        };
        return {
          ...cat,
          activities: [...cat.activities, newAct],
        };
      }
      return cat;
    });

    setCategories(updated);
    setNewActName('');
    setNewActShortCode('');
    setSelectedCatForNewAct(null);
  };

  // Delete Category
  const handleDeleteCategory = (catId: number | string) => {
    setCategories(categories.filter((c) => c.id !== catId));
  };

  // Delete Activity
  const handleDeleteActivity = (catId: number | string, actId: number | string) => {
    setCategories(
      categories.map((c) => {
        if (c.id === catId) {
          return {
            ...c,
            activities: c.activities.filter((a) => a.id !== actId),
          };
        }
        return c;
      })
    );
  };

  const totalActivitiesCount = useMemo(() => {
    return categories.reduce((acc, c) => acc + c.activities.length, 0);
  }, [categories]);

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <span>Kategoriler & Aktiviteler</span>
            <span className="text-xs font-semibold px-2.5 py-0.5 bg-indigo-50 text-indigo-700 rounded-full border border-indigo-100">
              Sürükle-Bırak Sıralama Aktif
            </span>
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Kategorileri ve aktiviteleri tutamaçlarından tutarak sürükleyip istediğiniz sıraya dizin
          </p>
        </div>

        <button
          onClick={() => setShowAddCatModal(true)}
          className="px-4 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs rounded-xl shadow-sm shadow-indigo-600/20 transition flex items-center gap-2 w-fit"
        >
          <Plus className="w-4 h-4" />
          <span>Yeni Kategori Ekle</span>
        </button>
      </div>

      {/* Overview Stat Strip */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="dashboard-card p-4 flex items-center justify-between">
          <div>
            <p className="text-[11px] font-semibold text-slate-500">Toplam Kategori</p>
            <h4 className="text-xl font-bold text-slate-900">{categories.length}</h4>
          </div>
          <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
            <Layers className="w-5 h-5" />
          </div>
        </div>

        <div className="dashboard-card p-4 flex items-center justify-between">
          <div>
            <p className="text-[11px] font-semibold text-slate-500">Tanımlı Aktiviteler</p>
            <h4 className="text-xl font-bold text-slate-900">{totalActivitiesCount}</h4>
          </div>
          <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <ListChecks className="w-5 h-5" />
          </div>
        </div>

        <div className="dashboard-card p-4 flex items-center justify-between">
          <div>
            <p className="text-[11px] font-semibold text-slate-500">Sıralama Durumu</p>
            <h4 className="text-xs font-bold text-indigo-700">Özel Masaüstü Düzeni</h4>
          </div>
          <div className="w-10 h-10 rounded-xl bg-sky-50 text-sky-600 flex items-center justify-center">
            <ArrowUpDown className="w-5 h-5" />
          </div>
        </div>
      </div>

      {/* Drag & Drop Categories List */}
      <div className="space-y-4">
        {categories.map((cat, index) => {
          const isCatDragging = draggedCategoryIndex === index;
          return (
            <div
              key={cat.id}
              draggable
              onDragStart={(e) => handleCategoryDragStart(e, index)}
              onDragOver={(e) => handleCategoryDragOver(e, index)}
              onDragEnd={handleCategoryDragEnd}
              className={`dashboard-card p-5 space-y-4 border-l-4 transition-all ${
                isCatDragging
                  ? 'opacity-40 border-indigo-400 scale-[0.99] shadow-inner'
                  : 'hover:shadow-md'
              }`}
              style={{ borderLeftColor: cat.color }}
            >
              {/* Category Header Row */}
              <div className="flex items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                  {/* Category Drag Handle */}
                  <div
                    className="p-1.5 text-slate-400 hover:text-slate-700 cursor-grab active:cursor-grabbing rounded-lg hover:bg-slate-100 transition"
                    title="Kategoriyi taşımak için sürükleyin"
                  >
                    <GripVertical className="w-4 h-4" />
                  </div>

                  {/* Category Color Tag & Name */}
                  <div className="flex items-center gap-2.5">
                    <span
                      className="w-4 h-4 rounded-full ring-2 ring-slate-100 shadow-xs"
                      style={{ backgroundColor: cat.color }}
                    />
                    <span
                      className="px-2 py-0.5 text-xs font-black text-white rounded-md"
                      style={{ backgroundColor: cat.color }}
                    >
                      {cat.code || 'K'}
                    </span>
                    <h3 className="text-sm font-bold text-slate-900">{cat.name}</h3>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setSelectedCatForNewAct(cat)}
                    className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs rounded-lg transition flex items-center gap-1"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Aktivite Ekle</span>
                  </button>

                  <button
                    onClick={() => handleDeleteCategory(cat.id)}
                    className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-rose-50 transition"
                    title="Kategoriyi Sil"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Sub-Activities Drag & Drop Area */}
              <div className="space-y-2 pt-1">
                <p className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                  Bağlı Aktiviteler ({cat.activities.length}) — Sıralamak için sürükleyin
                </p>

                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
                  {cat.activities.map((act, aIdx) => {
                    const isActDragging =
                      draggedActivityInfo?.catId === cat.id && draggedActivityInfo.actIndex === aIdx;

                    const displayCode = `${cat.code || 'K'}${act.shortCode || act.activityNumber || aIdx + 1}`;

                    return (
                      <div
                        key={act.id}
                        draggable
                        onDragStart={(e) => handleActivityDragStart(e, cat.id, aIdx)}
                        onDragOver={(e) => handleActivityDragOver(e, cat.id, aIdx)}
                        onDragEnd={handleActivityDragEnd}
                        className={`p-2.5 bg-slate-50 border border-slate-200 rounded-xl flex items-center justify-between gap-2 transition cursor-grab active:cursor-grabbing ${
                          isActDragging ? 'opacity-30 border-indigo-400' : 'hover:bg-white hover:border-slate-300'
                        }`}
                      >
                        <div className="flex items-center gap-2">
                          <GripVertical className="w-3.5 h-3.5 text-slate-300 hover:text-slate-600" />
                          <span
                            className="px-1.5 py-0.5 rounded text-[10px] font-black text-white"
                            style={{ backgroundColor: cat.color }}
                          >
                            {displayCode}
                          </span>
                          <span className="text-xs font-bold text-slate-800 truncate max-w-[150px]">
                            {act.name}
                          </span>
                        </div>

                        <button
                          onClick={() => handleDeleteActivity(cat.id, act.id)}
                          className="p-1 text-slate-300 hover:text-rose-600 rounded-md hover:bg-slate-100"
                        >
                          <X className="w-3 h-3" />
                        </button>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* ── MODAL: YENİ KATEGORİ EKLE ── */}
      {showAddCatModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white border border-slate-200 rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-5">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-bold text-slate-900">Yeni Kategori Tanımla</h3>
              <button
                onClick={() => setShowAddCatModal(false)}
                className="p-1.5 text-slate-400 hover:text-slate-700 rounded-lg hover:bg-slate-100"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleAddCategory} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">
                  Kategori Adı
                </label>
                <input
                  type="text"
                  required
                  placeholder="Örn: Yabancı Dil & Kelime"
                  value={newCatName}
                  onChange={(e) => setNewCatName(e.target.value)}
                  className="w-full px-3.5 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1.5">
                    Kısa Kod (Tek Harf)
                  </label>
                  <input
                    type="text"
                    maxLength={2}
                    placeholder="Örn: Y"
                    value={newCatCode}
                    onChange={(e) => setNewCatCode(e.target.value.toUpperCase())}
                    className="w-full px-3.5 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:border-indigo-500 font-mono font-bold"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1.5">
                    Renk
                  </label>
                  <div className="flex items-center gap-2 p-1.5 bg-slate-50 border border-slate-200 rounded-xl">
                    <input
                      type="color"
                      value={newCatColor}
                      onChange={(e) => setNewCatColor(e.target.value)}
                      className="w-7 h-7 rounded-lg bg-transparent border-0 cursor-pointer"
                    />
                    <span className="text-xs font-mono font-semibold text-slate-700">{newCatColor}</span>
                  </div>
                </div>
              </div>

              <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowAddCatModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  İptal
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-xl shadow-xs"
                >
                  Kategori Oluştur
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── MODAL: KATEGORİYE AKTİVİTE EKLE ── */}
      {selectedCatForNewAct && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white border border-slate-200 rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-5">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-slate-900">Aktivite Ekle</h3>
                <p className="text-xs text-slate-500 font-semibold">{selectedCatForNewAct.name}</p>
              </div>
              <button
                onClick={() => setSelectedCatForNewAct(null)}
                className="p-1.5 text-slate-400 hover:text-slate-700 rounded-lg hover:bg-slate-100"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleAddActivity} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">
                  Aktivite Adı
                </label>
                <input
                  type="text"
                  required
                  placeholder="Örn: İngilizce Makale Okuma"
                  value={newActName}
                  onChange={(e) => setNewActName(e.target.value)}
                  className="w-full px-3.5 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">
                  Alt Kod (Örn: 1, 2, b, t)
                </label>
                <input
                  type="text"
                  maxLength={3}
                  placeholder={`Örn: ${selectedCatForNewAct.activities.length + 1}`}
                  value={newActShortCode}
                  onChange={(e) => setNewActShortCode(e.target.value)}
                  className="w-full px-3.5 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:border-indigo-500 font-mono font-bold"
                />
                <p className="text-[11px] text-slate-400 mt-1">
                  Matriste görünecek etiket: <strong>{selectedCatForNewAct.code || 'K'}{newActShortCode || (selectedCatForNewAct.activities.length + 1)}</strong>
                </p>
              </div>

              <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setSelectedCatForNewAct(null)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  İptal
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-xl shadow-xs"
                >
                  Aktivite Kaydet
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
