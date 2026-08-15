'use client';

import React from 'react';
import {
  Cloud,
  CheckCircle2,
  ShieldCheck,
  Laptop,
  RefreshCw,
  Smartphone,
  Database,
  ArrowRight,
  Sparkles,
} from 'lucide-react';

interface ProfileCloudViewProps {
  user: {
    name: string;
    email: string;
    photoUrl?: string;
    isLoggedIn: boolean;
  };
  syncedData?: any;
  onLogin: () => void;
  onSync: () => void;
  isSyncing: boolean;
}

export default function ProfileCloudView({
  user,
  syncedData,
  onLogin,
  onSync,
  isSyncing,
}: ProfileCloudViewProps) {
  const categoriesCount = syncedData?.categories?.length || 0;
  const logsCount = syncedData?.logs?.length || 0;
  const lastSyncTime = syncedData?.syncedAt
    ? new Date(syncedData.syncedAt).toLocaleString('tr-TR', {
        dateStyle: 'medium',
        timeStyle: 'short',
      })
    : 'Henüz senkronizasyon yapılmadı';

  return (
    <div className="space-y-6 max-w-5xl">
      {/* User Profile Hero Card */}
      <div className="dashboard-card p-6 md:p-8">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6">
          <div className="flex items-center gap-5">
            {user.isLoggedIn && user.photoUrl ? (
              <img
                src={user.photoUrl}
                alt={user.name}
                className="w-18 h-18 rounded-2xl object-cover border-2 border-indigo-100 shadow-md ring-4 ring-indigo-50"
              />
            ) : (
              <div className="w-18 h-18 rounded-2xl bg-gradient-to-tr from-indigo-600 to-sky-500 flex items-center justify-center text-white text-2xl font-bold shadow-md shadow-indigo-500/20">
                {user.isLoggedIn ? user.name.charAt(0).toUpperCase() : 'G'}
              </div>
            )}
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-bold text-slate-900">
                  {user.isLoggedIn ? user.name : 'Google Hesabı Bağlı Değil'}
                </h2>
                {user.isLoggedIn && (
                  <span className="px-2.5 py-0.5 bg-emerald-50 border border-emerald-200/80 text-emerald-700 text-[11px] font-semibold rounded-full flex items-center gap-1">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                    <span>Bağlı</span>
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-500">
                {user.isLoggedIn
                  ? user.email
                  : 'Google hesabınız ile giriş yaparak telefonunuzdaki verileri bilgisayarınıza eşitleyin.'}
              </p>
            </div>
          </div>

          <div>
            {user.isLoggedIn ? (
              <button
                onClick={onSync}
                disabled={isSyncing}
                className="w-full sm:w-auto px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs rounded-xl shadow-sm shadow-indigo-600/20 transition flex items-center justify-center gap-2"
              >
                <RefreshCw className={`w-4 h-4 ${isSyncing ? 'animate-spin' : ''}`} />
                <span>{isSyncing ? 'Eşitleniyor...' : 'Verileri Senkronize Et'}</span>
              </button>
            ) : (
              <button
                onClick={onLogin}
                className="w-full sm:w-auto px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs rounded-xl shadow-sm shadow-indigo-600/20 transition flex items-center justify-center gap-2"
              >
                <Sparkles className="w-4 h-4" />
                <span>Google ile Giriş Yap</span>
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Sync Details & Architecture Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Cloud Sync Database Card */}
        <div className="dashboard-card p-6 space-y-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
              <Database className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-slate-900">Neon PostgreSQL Veritabanı</h3>
              <p className="text-[11px] text-slate-500">Bulut eşitleme altyapısı & kayıt durumu</p>
            </div>
          </div>

          <div className="space-y-3 pt-2">
            <div className="flex items-center justify-between p-3 bg-slate-50 rounded-xl text-xs">
              <span className="text-slate-500 font-medium">Son Senkronizasyon:</span>
              <span className="font-semibold text-slate-800">{lastSyncTime}</span>
            </div>

            <div className="flex items-center justify-between p-3 bg-slate-50 rounded-xl text-xs">
              <span className="text-slate-500 font-medium">Kayıtlı Zaman Slotları:</span>
              <span className="font-semibold text-indigo-700">{logsCount} Slot</span>
            </div>

            <div className="flex items-center justify-between p-3 bg-slate-50 rounded-xl text-xs">
              <span className="text-slate-500 font-medium">Aktif Kategoriler:</span>
              <span className="font-semibold text-slate-800">{categoriesCount} Kategori</span>
            </div>
          </div>
        </div>

        {/* Security & Multiplatform Card */}
        <div className="dashboard-card p-6 space-y-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-slate-900">Güvenli & Özel Senkronizasyon</h3>
              <p className="text-[11px] text-slate-500">Kişisel Google hesabınızla korunan veriler</p>
            </div>
          </div>

          <p className="text-xs text-slate-600 leading-relaxed pt-1">
            Optimum, zaman çizelgenizi ve değerlendirmelerinizi yalnızca sizin Google hesabınıza özel olarak şifreler ve saklar. Başka hiçbir kullanıcı verilerinize erişemez.
          </p>

          <div className="p-3.5 bg-slate-50 border border-slate-200/80 rounded-xl space-y-1 text-xs">
            <p className="font-semibold text-slate-800 flex items-center gap-1.5">
              <Laptop className="w-4 h-4 text-indigo-600" />
              <span>Masaüstü Web Erişimi</span>
            </p>
            <p className="text-slate-500 text-[11px]">
              Tüm tarayıcılardan Optimum Web Dashboard ile çizelgenizi yönetebilir ve analitiklerinizi inceleyebilirsiniz.
            </p>
          </div>
        </div>
      </div>

      {/* 3 Step Onboarding / Pairing Guide */}
      <div className="dashboard-card p-6 space-y-4">
        <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
          <Smartphone className="w-4 h-4 text-indigo-600" />
          <span>Android Uygulaması ile Nasıl Eşitlenir?</span>
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-1">
          <div className="p-4 bg-slate-50 border border-slate-200/80 rounded-xl space-y-2">
            <div className="w-6 h-6 rounded-full bg-indigo-600 text-white font-bold text-xs flex items-center justify-center">
              1
            </div>
            <h4 className="text-xs font-bold text-slate-800">Telefonda Giriş Yap</h4>
            <p className="text-[11px] text-slate-500">
              Optimum Android uygulamasında <strong>Profil</strong> sekmesine gidin ve <strong>Google ile Giriş Yap</strong> butonuna basın.
            </p>
          </div>

          <div className="p-4 bg-slate-50 border border-slate-200/80 rounded-xl space-y-2">
            <div className="w-6 h-6 rounded-full bg-indigo-600 text-white font-bold text-xs flex items-center justify-center">
              2
            </div>
            <h4 className="text-xs font-bold text-slate-800">Şimdi Senkronize Et</h4>
            <p className="text-[11px] text-slate-500">
              Aynı ekrandaki <strong>"Şimdi Senkronize Et"</strong> butonuna basarak tüm verilerinizi buluta aktarın.
            </p>
          </div>

          <div className="p-4 bg-slate-50 border border-slate-200/80 rounded-xl space-y-2">
            <div className="w-6 h-6 rounded-full bg-indigo-600 text-white font-bold text-xs flex items-center justify-center">
              3
            </div>
            <h4 className="text-xs font-bold text-slate-800">Büyük Ekranda Yönet</h4>
            <p className="text-[11px] text-slate-500">
              Burada aynı Google hesabınızla oturum açtığınızda tüm zaman çizelgeniz anında karşınıza gelecektir.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
