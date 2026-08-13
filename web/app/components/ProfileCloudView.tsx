'use client';

import React from 'react';
import { Cloud, CheckCircle2, ShieldCheck, Laptop, RefreshCw, Smartphone, Key } from 'lucide-react';

interface ProfileCloudViewProps {
  user: {
    name: string;
    email: string;
    isLoggedIn: boolean;
  };
  onLogin: () => void;
  onSync: () => void;
  isSyncing: boolean;
}

export default function ProfileCloudView({ user, onLogin, onSync, isSyncing }: ProfileCloudViewProps) {
  return (
    <div className="space-y-6 max-w-4xl">
      <div className="glass-panel p-6 rounded-2xl space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-indigo-600 to-purple-600 flex items-center justify-center text-white text-2xl font-bold shadow-lg shadow-indigo-500/30">
              {user.isLoggedIn ? user.name.charAt(0).toUpperCase() : 'G'}
            </div>
            <div>
              <h2 className="text-xl font-bold text-white">
                {user.isLoggedIn ? user.name : 'Google Hesabı Bağlı Değil'}
              </h2>
              <p className="text-xs text-gray-400">
                {user.isLoggedIn ? user.email : 'Verilerinizi buluta eşitlemek için giriş yapın'}
              </p>
            </div>
          </div>

          {user.isLoggedIn ? (
            <span className="px-3 py-1 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-semibold rounded-full flex items-center gap-1.5">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>Hesap Aktif</span>
            </span>
          ) : (
            <button
              onClick={onLogin}
              className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs rounded-xl transition shadow-lg shadow-indigo-600/30"
            >
              Google ile Giriş Yap
            </button>
          )}
        </div>
      </div>

      {/* Cloud Sync Status */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="glass-panel p-5 rounded-2xl space-y-3">
          <div className="flex items-center gap-3 text-indigo-400 font-bold text-sm">
            <Cloud className="w-5 h-5" />
            <span>Bulut Eşitleme Durumu</span>
          </div>
          <p className="text-xs text-gray-300">
            Son Senkronizasyon: <span className="font-semibold text-white">Bugün 13:20</span>
          </p>
          <p className="text-xs text-gray-400">
            Android uygulamasından girilen son 128 zaman slotu ve 5 kategori tamamen eşitlendi.
          </p>
          <button
            onClick={onSync}
            disabled={isSyncing}
            className="w-full py-2 bg-gray-800 hover:bg-gray-700 text-gray-200 font-semibold text-xs rounded-xl border border-gray-700 transition flex items-center justify-center gap-2"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isSyncing ? 'animate-spin text-indigo-400' : ''}`} />
            <span>Manuel Yenile & Senkronize Et</span>
          </button>
        </div>

        <div className="glass-panel p-5 rounded-2xl space-y-3">
          <div className="flex items-center gap-3 text-purple-400 font-bold text-sm">
            <Laptop className="w-5 h-5" />
            <span>Vercel Cloud Deployment</span>
          </div>
          <p className="text-xs text-gray-300">
            Platform: <span className="font-semibold text-white">Next.js App Router + Vercel</span>
          </p>
          <p className="text-xs text-gray-400">
            Tüm masaüstü tarayıcılardan Google hesabınızla anında çizelgenize erişebilir, büyük ekranın tadını çıkarabilirsiniz.
          </p>
          <div className="px-3 py-1.5 bg-purple-500/10 border border-purple-500/20 text-purple-300 text-xs rounded-lg font-mono">
            https://optimum-pi-black.vercel.app
          </div>
        </div>
      </div>
    </div>
  );
}
