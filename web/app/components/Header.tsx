'use client';

import React from 'react';
import { Sparkles, RefreshCw, LogOut, User, CheckCircle2, Smartphone, ShieldCheck } from 'lucide-react';

interface HeaderProps {
  user: {
    name: string;
    email: string;
    photoUrl?: string;
    isLoggedIn: boolean;
  };
  onLogin: () => void;
  onLogout: () => void;
  onSync: () => void;
  isSyncing: boolean;
  lastSyncTime?: string;
}

export default function Header({
  user,
  onLogin,
  onLogout,
  onSync,
  isSyncing,
  lastSyncTime,
}: HeaderProps) {
  return (
    <header className="h-16 border-b border-slate-200 bg-white/80 backdrop-blur-md sticky top-0 z-40 px-6 flex items-center justify-between shadow-xs">
      {/* Brand Logo & Name */}
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 via-indigo-500 to-sky-500 flex items-center justify-center shadow-md shadow-indigo-500/20">
          <Sparkles className="w-5 h-5 text-white" />
        </div>
        <div>
          <div className="flex items-center gap-2">
            <h1 className="font-bold text-lg text-slate-900 tracking-tight leading-none">OPTIMUM</h1>
            <span className="px-2 py-0.5 text-[10px] font-semibold bg-indigo-50 text-indigo-700 border border-indigo-200/60 rounded-md">
              v2.0 Web
            </span>
          </div>
          <p className="text-xs text-slate-500 font-medium">Zaman Yönetimi & Bulut Analitik</p>
        </div>
      </div>

      {/* Right Action Controls */}
      <div className="flex items-center gap-3">
        {user.isLoggedIn ? (
          <>
            {/* Sync Status Button */}
            <button
              onClick={onSync}
              disabled={isSyncing}
              className="flex items-center gap-2 px-3.5 py-1.5 text-xs font-medium bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg border border-slate-200 transition"
              title="Android uygulamasından gelen verileri yeniden yükle"
            >
              <RefreshCw
                className={`w-3.5 h-3.5 ${
                  isSyncing ? 'animate-spin text-indigo-600' : 'text-emerald-600'
                }`}
              />
              <span className="hidden sm:inline">
                {isSyncing ? 'Eşitleniyor...' : 'Verileri Yenile'}
              </span>
            </button>

            {/* User Profile Pill */}
            <div className="flex items-center gap-3 pl-3 border-l border-slate-200">
              {user.photoUrl ? (
                <img
                  src={user.photoUrl}
                  alt={user.name}
                  className="w-8 h-8 rounded-full object-cover border border-slate-200 ring-2 ring-indigo-50"
                />
              ) : (
                <div className="w-8 h-8 rounded-full bg-indigo-600 flex items-center justify-center text-white font-bold text-xs ring-2 ring-indigo-50">
                  {user.name.charAt(0).toUpperCase() || 'U'}
                </div>
              )}
              <div className="hidden md:block text-left">
                <p className="text-xs font-semibold text-slate-800 leading-tight">{user.name}</p>
                <p className="text-[11px] text-slate-500 leading-tight">{user.email}</p>
              </div>
              <button
                onClick={onLogout}
                title="Oturumu Kapat"
                className="p-2 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-slate-100 transition"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          </>
        ) : (
          <button
            onClick={onLogin}
            className="flex items-center gap-2.5 px-4 py-2 bg-white hover:bg-slate-50 text-slate-700 font-medium text-xs rounded-xl border border-slate-300 shadow-xs hover:border-slate-400 transition"
          >
            <svg className="w-4 h-4" viewBox="0 0 24 24">
              <path
                fill="#4285F4"
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
              />
              <path
                fill="#34A853"
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
              />
              <path
                fill="#FBBC05"
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
              />
              <path
                fill="#EA4335"
                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
              />
            </svg>
            <span>Google ile Giriş Yap</span>
          </button>
        )}
      </div>
    </header>
  );
}
