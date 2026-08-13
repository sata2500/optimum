'use client';

import React, { useState } from 'react';
import { ChevronLeft, ChevronRight, Calendar as CalendarIcon, Plus, Clock, CheckCircle2 } from 'lucide-react';

interface TimeLog {
  id: number;
  timeSlot: string;
  category: string;
  categoryColor: string;
  activity: string;
  notes?: string;
}

const mockCategories = [
  { id: 1, name: 'Çalışma & Kodlama', color: '#6366f1' },
  { id: 2, name: 'Spor & Sağlık', color: '#10b981' },
  { id: 3, name: 'Okuma & Eğitim', color: '#f59e0b' },
  { id: 4, name: 'Dinlenme & Mola', color: '#ec4899' },
  { id: 5, name: 'Sosyal & Aile', color: '#8b5cf6' },
];

const generateInitialLogs = (): TimeLog[] => [
  { id: 1, timeSlot: '08:00 - 09:00', category: 'Çalışma & Kodlama', categoryColor: '#6366f1', activity: 'React & Next.js Mimari Geliştirme', notes: 'Vercel web dashboard tasarlanıyor' },
  { id: 2, timeSlot: '09:00 - 10:00', category: 'Çalışma & Kodlama', categoryColor: '#6366f1', activity: 'Android Google Sign-In Entegrasyonu', notes: 'Credential Manager SDK eklendi' },
  { id: 3, timeSlot: '10:00 - 10:30', category: 'Dinlenme & Mola', categoryColor: '#ec4899', activity: 'Kahve Arası', notes: 'Kısa yürüyüş' },
  { id: 4, timeSlot: '10:30 - 12:00', category: 'Çalışma & Kodlama', categoryColor: '#6366f1', activity: 'Firestore Sync Yapılandırması' },
  { id: 5, timeSlot: '12:00 - 13:00', category: 'Spor & Sağlık', categoryColor: '#10b981', activity: 'Öğle Yemeği & Yürüyüş' },
  { id: 6, timeSlot: '13:00 - 15:00', category: 'Okuma & Eğitim', categoryColor: '#f59e0b', activity: 'Teknoloji Makaleleri Incelemesi' },
  { id: 7, timeSlot: '15:00 - 17:00', category: 'Çalışma & Kodlama', categoryColor: '#6366f1', activity: 'Web UI Test ve Polaj' },
];

export default function ScheduleGrid() {
  const [selectedDate, setSelectedDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );
  const [logs, setLogs] = useState<TimeLog[]>(generateInitialLogs());
  const [newActivity, setNewActivity] = useState('');
  const [selectedCat, setSelectedCat] = useState(mockCategories[0]);
  const [selectedTimeSlot, setSelectedTimeSlot] = useState('17:00 - 18:00');

  const handleAddLog = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newActivity.trim()) return;

    const newLog: TimeLog = {
      id: Date.now(),
      timeSlot: selectedTimeSlot,
      category: selectedCat.name,
      categoryColor: selectedCat.color,
      activity: newActivity,
    };

    setLogs([...logs, newLog]);
    setNewActivity('');
  };

  return (
    <div className="space-y-6">
      {/* Date Header & Quick Actions */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 glass-panel p-4 rounded-2xl">
        <div className="flex items-center gap-3">
          <button
            onClick={() => {
              const d = new Date(selectedDate);
              d.setDate(d.getDate() - 1);
              setSelectedDate(d.toISOString().split('T')[0]);
            }}
            className="p-2 bg-gray-800 hover:bg-gray-700 rounded-xl text-gray-300 transition"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>

          <div className="flex items-center gap-2 px-4 py-2 bg-gray-900 border border-gray-800 rounded-xl">
            <CalendarIcon className="w-4 h-4 text-indigo-400" />
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="bg-transparent text-white text-sm font-semibold focus:outline-none cursor-pointer"
            />
          </div>

          <button
            onClick={() => {
              const d = new Date(selectedDate);
              d.setDate(d.getDate() + 1);
              setSelectedDate(d.toISOString().split('T')[0]);
            }}
            className="p-2 bg-gray-800 hover:bg-gray-700 rounded-xl text-gray-300 transition"
          >
            <ChevronRight className="w-5 h-5" />
          </button>
        </div>

        {/* Quick Stats Pill */}
        <div className="flex items-center gap-4 text-xs">
          <div className="px-3 py-1.5 bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 rounded-lg">
            Toplam Kayıt: <span className="font-bold text-white">{logs.length} Zaman Dilimi</span>
          </div>
          <div className="px-3 py-1.5 bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 rounded-lg">
            Tamamlanan: <span className="font-bold text-white">%85 Verimlilik</span>
          </div>
        </div>
      </div>

      {/* Main Grid View */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Table Log Timeline */}
        <div className="lg:col-span-2 space-y-3">
          <h2 className="text-sm font-bold text-gray-400 uppercase tracking-wider px-1">
            Günlük Zaman Dilimleri ({selectedDate})
          </h2>

          <div className="space-y-2">
            {logs.map((log) => (
              <div
                key={log.id}
                className="glass-panel p-4 rounded-xl flex items-center justify-between gap-4 hover:border-gray-700 transition"
              >
                <div className="flex items-center gap-4">
                  <div
                    className="w-1.5 h-12 rounded-full"
                    style={{ backgroundColor: log.categoryColor }}
                  />
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-semibold px-2 py-0.5 rounded bg-gray-800 text-gray-300">
                        {log.timeSlot}
                      </span>
                      <span
                        className="text-[11px] font-medium px-2 py-0.5 rounded"
                        style={{
                          backgroundColor: `${log.categoryColor}20`,
                          color: log.categoryColor,
                        }}
                      >
                        {log.category}
                      </span>
                    </div>
                    <p className="text-sm font-semibold text-white mt-1">{log.activity}</p>
                    {log.notes && (
                      <p className="text-xs text-gray-400 mt-0.5">{log.notes}</p>
                    )}
                  </div>
                </div>

                <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
              </div>
            ))}
          </div>
        </div>

        {/* Quick Add Slot Card */}
        <div className="space-y-4">
          <h2 className="text-sm font-bold text-gray-400 uppercase tracking-wider px-1">
            Yeni Aktivite Ekle
          </h2>

          <form onSubmit={handleAddLog} className="glass-panel p-5 rounded-2xl space-y-4">
            <div>
              <label className="block text-xs font-medium text-gray-400 mb-1">
                Zaman Dilimi
              </label>
              <select
                value={selectedTimeSlot}
                onChange={(e) => setSelectedTimeSlot(e.target.value)}
                className="w-full bg-gray-900 border border-gray-800 text-white rounded-xl p-2.5 text-xs focus:outline-none focus:border-indigo-500"
              >
                <option>17:00 - 18:00</option>
                <option>18:00 - 19:00</option>
                <option>19:00 - 20:00</option>
                <option>20:00 - 21:00</option>
                <option>21:00 - 22:00</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-400 mb-1">
                Kategori
              </label>
              <select
                value={selectedCat.name}
                onChange={(e) => {
                  const cat = mockCategories.find((c) => c.name === e.target.value);
                  if (cat) setSelectedCat(cat);
                }}
                className="w-full bg-gray-900 border border-gray-800 text-white rounded-xl p-2.5 text-xs focus:outline-none focus:border-indigo-500"
              >
                {mockCategories.map((c) => (
                  <option key={c.id} value={c.name}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-400 mb-1">
                Aktivite Tanımı
              </label>
              <input
                type="text"
                placeholder="Örn: Proje incelemesi"
                value={newActivity}
                onChange={(e) => setNewActivity(e.target.value)}
                className="w-full bg-gray-900 border border-gray-800 text-white rounded-xl p-2.5 text-xs focus:outline-none focus:border-indigo-500"
              />
            </div>

            <button
              type="submit"
              className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs rounded-xl transition flex items-center justify-center gap-2 shadow-lg shadow-indigo-600/30"
            >
              <Plus className="w-4 h-4" />
              <span>Çizelgeye Ekle</span>
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
