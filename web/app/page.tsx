'use client';

import React, { useState, useEffect, useRef } from 'react';
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import ScheduleGrid from './components/ScheduleGrid';
import AnalyticsCharts from './components/AnalyticsCharts';
import CategoryManager from './components/CategoryManager';
import ProfileCloudView from './components/ProfileCloudView';
import { Sparkles, ArrowRight, ShieldCheck } from 'lucide-react';

declare global {
  interface Window {
    google?: any;
  }
}

interface UserSession {
  name: string;
  email: string;
  photoUrl?: string;
  isLoggedIn: boolean;
}

const ACTIVE_GOOGLE_CLIENT_ID = '41897653252-cc6ose1e7r8ogab59gnlt1pecp30hj6i.apps.googleusercontent.com';

export default function Home() {
  const [activeTab, setActiveTab] = useState<'schedule' | 'analytics' | 'categories' | 'profile'>('schedule');
  const [isSyncing, setIsSyncing] = useState(false);
  const [syncedData, setSyncedData] = useState<any>(null);
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [loginError, setLoginError] = useState<string | null>(null);
  const googleBtnRef = useRef<HTMLDivElement>(null);

  const [user, setUser] = useState<UserSession>({
    name: '',
    email: '',
    isLoggedIn: false,
  });

  // 1. Check URL hash for OAuth redirect token or existing session on mount
  useEffect(() => {
    try {
      // Check for OAuth direct redirect response (#id_token=...)
      if (typeof window !== 'undefined' && window.location.hash) {
        const hashParams = new URLSearchParams(window.location.hash.substring(1));
        const idToken = hashParams.get('id_token');
        if (idToken) {
          const base64Url = idToken.split('.')[1];
          const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
          const jsonPayload = decodeURIComponent(
            atob(base64)
              .split('')
              .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
              .join('')
          );
          const payload = JSON.parse(jsonPayload);
          const newUser: UserSession = {
            name: payload.name || payload.email?.split('@')[0] || 'Kullanıcı',
            email: payload.email,
            photoUrl: payload.picture,
            isLoggedIn: true,
          };
          setUser(newUser);
          localStorage.setItem('optimum_user_session', JSON.stringify(newUser));
          fetchSyncedData(payload.email);
          window.history.replaceState(null, '', window.location.pathname);
          return;
        }
      }

      // Check saved localStorage session
      const savedSession = localStorage.getItem('optimum_user_session');
      if (savedSession) {
        const parsed = JSON.parse(savedSession);
        if (parsed && parsed.email) {
          setUser(parsed);
          fetchSyncedData(parsed.email);
        }
      }
    } catch (e) {
      console.error('Session/Hash load error:', e);
    }
  }, []);

  // 2. Initialize Google Identity Services (GSI)
  useEffect(() => {
    const initGoogleAuth = () => {
      if (typeof window !== 'undefined' && window.google?.accounts?.id) {
        try {
          window.google.accounts.id.initialize({
            client_id: ACTIVE_GOOGLE_CLIENT_ID,
            callback: handleGoogleCallback,
            auto_select: false,
            cancel_on_tap_outside: true,
          });

          if (googleBtnRef.current) {
            window.google.accounts.id.renderButton(googleBtnRef.current, {
              theme: 'outline',
              size: 'large',
              text: 'signin_with',
              shape: 'rectangular',
              logo_alignment: 'left',
              width: 280,
            });
          }
        } catch (err) {
          console.error('GIS init error:', err);
        }
      }
    };

    initGoogleAuth();
    const interval = setInterval(() => {
      if (window.google?.accounts?.id) {
        initGoogleAuth();
        clearInterval(interval);
      }
    }, 500);

    return () => clearInterval(interval);
  }, [showLoginModal]);

  const handleGoogleCallback = (response: any) => {
    try {
      if (response.credential) {
        const base64Url = response.credential.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(
          atob(base64)
            .split('')
            .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
            .join('')
        );

        const payload = JSON.parse(jsonPayload);
        const newUser: UserSession = {
          name: payload.name || payload.email.split('@')[0],
          email: payload.email,
          photoUrl: payload.picture,
          isLoggedIn: true,
        };

        setUser(newUser);
        localStorage.setItem('optimum_user_session', JSON.stringify(newUser));
        setShowLoginModal(false);
        setLoginError(null);
        fetchSyncedData(payload.email);
      }
    } catch (err: any) {
      console.error('Google Sign-In Error:', err);
      setLoginError('Google kimlik doğrulaması tamamlanamadı. Lütfen tekrar deneyin.');
    }
  };

  const fetchSyncedData = async (email: string) => {
    if (!email) return;
    setIsSyncing(true);
    try {
      const res = await fetch(`/api/sync?email=${encodeURIComponent(email)}`);
      const result = await res.json();
      if (result.success && result.data) {
        setSyncedData(result.data);
      }
    } catch (e) {
      console.error('Fetch sync data error:', e);
    } finally {
      setIsSyncing(false);
    }
  };

  // Direct full-page OAuth redirect (Bulletproof against popup & origin blockers)
  const handleDirectOAuthLogin = () => {
    if (typeof window === 'undefined') return;
    const origin = window.location.origin;
    const nonce = Math.random().toString(36).substring(2) + Date.now().toString(36);
    const authUrl = `https://accounts.google.com/o/oauth2/v2/auth?client_id=${encodeURIComponent(
      ACTIVE_GOOGLE_CLIENT_ID
    )}&redirect_uri=${encodeURIComponent(
      origin
    )}&response_type=id_token&scope=openid%20email%20profile&nonce=${nonce}&prompt=select_account`;

    window.location.href = authUrl;
  };

  const handleLogin = () => {
    setLoginError(null);
    setShowLoginModal(true);

    if (typeof window !== 'undefined' && window.google?.accounts?.id) {
      window.google.accounts.id.prompt((notification: any) => {
        if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
          // Modal will display both Google GIS button and Direct Login button
        }
      });
    }
  };

  const handleLogout = () => {
    setUser({
      name: '',
      email: '',
      isLoggedIn: false,
    });
    setSyncedData(null);
    localStorage.removeItem('optimum_user_session');
  };

  const handleSync = () => {
    if (user.email) {
      fetchSyncedData(user.email);
    } else {
      handleLogin();
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-900">
      {/* Top Header */}
      <Header
        user={user}
        onLogin={handleLogin}
        onLogout={handleLogout}
        onSync={handleSync}
        isSyncing={isSyncing}
        lastSyncTime={syncedData?.syncedAt}
      />

      <div className="flex flex-1">
        {/* Left Sidebar */}
        <Sidebar
          activeTab={activeTab}
          setActiveTab={setActiveTab}
          syncedData={syncedData}
        />

        {/* Main Content Area */}
        <main className="flex-1 p-6 md:p-8 max-w-7xl mx-auto w-full space-y-6">
          {/* Welcome Banner for Unauthenticated Users */}
          {!user.isLoggedIn && (
            <div className="dashboard-card p-6 bg-gradient-to-r from-indigo-50/80 via-white to-sky-50/60 border-indigo-100 flex flex-col md:flex-row md:items-center justify-between gap-6">
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <span className="px-2.5 py-0.5 bg-indigo-100 text-indigo-800 text-[11px] font-bold rounded-full flex items-center gap-1">
                    <Sparkles className="w-3.5 h-3.5" />
                    <span>Bulut Senkronizasyonu</span>
                  </span>
                </div>
                <h2 className="text-lg font-bold text-slate-900">
                  Optimum Masaüstü Çizelgesine Hoş Geldiniz!
                </h2>
                <p className="text-xs text-slate-600 max-w-2xl leading-relaxed">
                  Android uygulamanızda kullandığınız Google hesabınızla giriş yaparak tüm zaman çizelgenizi, analitik verilerinizi ve öz değerlendirmelerinizi bilgisayarınızda büyük ekranda yönetebilirsiniz.
                </p>
              </div>

              <button
                onClick={handleDirectOAuthLogin}
                className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs rounded-xl shadow-md shadow-indigo-600/20 transition flex items-center justify-center gap-2 shrink-0 cursor-pointer"
              >
                <span>Google ile Oturum Aç</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          )}

          {/* Active Tab View */}
          {activeTab === 'schedule' && <ScheduleGrid syncedData={syncedData} />}
          {activeTab === 'analytics' && <AnalyticsCharts syncedData={syncedData} />}
          {activeTab === 'categories' && <CategoryManager syncedData={syncedData} />}
          {activeTab === 'profile' && (
            <ProfileCloudView
              user={user}
              syncedData={syncedData}
              onLogin={handleLogin}
              onSync={handleSync}
              isSyncing={isSyncing}
            />
          )}
        </main>
      </div>

      {/* Google Login Modal */}
      {showLoginModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white border border-slate-200 rounded-2xl p-6 md:p-8 max-w-md w-full shadow-2xl space-y-6 animate-in fade-in zoom-in-95 duration-200">
            <div className="text-center space-y-2">
              <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 mx-auto flex items-center justify-center shadow-xs">
                <ShieldCheck className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-900">Google Hesabınız ile Bağlanın</h3>
              <p className="text-xs text-slate-500">
                Optimum bulut eşitlemesi için Google hesabınızı doğrulayın.
              </p>
            </div>

            {loginError && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-700">
                {loginError}
              </div>
            )}

            {/* Direct Google Sign-In Button */}
            <div className="space-y-3 pt-2">
              <button
                onClick={handleDirectOAuthLogin}
                className="w-full flex items-center justify-center gap-3 px-4 py-3 bg-white hover:bg-slate-50 text-slate-700 font-semibold text-xs rounded-xl border border-slate-300 shadow-xs hover:border-slate-400 transition"
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
                <span>Google ile Doğrudan Giriş Yap</span>
              </button>

              <div className="flex flex-col items-center justify-center">
                <div ref={googleBtnRef} className="min-h-[44px]" />
              </div>
            </div>

            <div className="pt-2 border-t border-slate-100 flex items-center justify-between">
              <span className="text-[11px] text-slate-400">Optimum v2.0 Web Platform</span>
              <button
                onClick={() => setShowLoginModal(false)}
                className="text-xs font-semibold text-slate-500 hover:text-slate-800 transition px-3 py-1.5 rounded-lg hover:bg-slate-100"
              >
                Kapat
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
