'use client';

import React from 'react';
import { Cloud, LogOut, User, Sparkles, RefreshCw } from 'lucide-react';

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
}

export default function Header({ user, onLogin, onLogout, onSync, isSyncing }: HeaderProps) {
  return (
    <header className="h-16 border-b border-gray-800 bg-gray-900/60 backdrop-blur-md sticky top-0 z-40 px-6 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-purple-600 flex items-center justify-center shadow-lg shadow-indigo-500/20">
          <Sparkles className="w-5 h-5 text-white" />
        </div>
        <div>
          <h1 className="font-bold text-lg text-white leading-none">OPTIMUM</h1>
          <span className="text-xs text-indigo-400 font-medium">Cloud Web Dashboard</span>
        </div>
      </div>

      <div className="flex items-center gap-4">
        {user.isLoggedIn ? (
          <>
            <button
              onClick={onSync}
              disabled={isSyncing}
              className="flex items-center gap-2 px-3 py-1.5 text-xs font-medium bg-gray-800 hover:bg-gray-700 text-gray-300 rounded-lg border border-gray-700 transition"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isSyncing ? 'animate-spin text-indigo-400' : 'text-emerald-400'}`} />
              <span>{isSyncing ? 'Senkronize Ediliyor...' : 'Bulut Senkronize'}</span>
            </button>

            <div className="flex items-center gap-3 pl-2 border-l border-gray-800">
              <div className="w-8 h-8 rounded-full bg-indigo-600 flex items-center justify-center text-white font-bold text-sm">
                {user.name.charAt(0).toUpperCase()}
              </div>
              <div className="hidden md:block text-left">
                <p className="text-xs font-semibold text-white leading-tight">{user.name}</p>
                <p className="text-[10px] text-gray-400">{user.email}</p>
              </div>
              <button
                onClick={onLogout}
                title="Çıkış Yap"
                className="p-2 text-gray-400 hover:text-red-400 rounded-lg hover:bg-gray-800 transition"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          </>
        ) : (
          <button
            onClick={onLogin}
            className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white font-medium text-xs rounded-xl shadow-lg shadow-indigo-500/25 transition glow-button"
          >
            <User className="w-4 h-4" />
            <span>Google ile Giriş Yap</span>
          </button>
        )}
      </div>
    </header>
  );
}
