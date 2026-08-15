'use client';

import React, { useState, useEffect, useRef } from 'react';
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import ScheduleGrid from './components/ScheduleGrid';
import AnalyticsCharts from './components/AnalyticsCharts';
import CategoryManager from './components/CategoryManager';
import ProfileCloudView from './components/ProfileCloudView';
import { Sparkles, CheckCircle2, ArrowRight, ShieldCheck, Laptop, Clock } from 'lucide-react';

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

const ACTIVE_GOOGLE_CLIENT_ID =
  process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID ||
  '41897653252-cc6ose1e7r8ogab59gnlt1pecp30hj6i.apps.googleusercontent.com';

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

  // Load session from localStorage on initial render
  useEffect(() => {
    try {
      const savedSession = localStorage.getItem('optimum_user_session');
      if (savedSession) {
        const parsed = JSON.parse(savedSession);
        if (parsed && parsed.email) {
          setUser(parsed);
          fetchSyncedData(parsed.email);
        }
      }
    } catch (e) {
      console.error('Failed to parse session:', e);
    }
  }, []);

  // Initialize Google Identity Services
  useEffect(() => {
    const initGoogleAuth = () => {
      if (typeof window !== 'undefined' && window.google?.accounts?.id) {
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
      }
    };

    // Check if script already loaded or retry in interval
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
        // Decode JWT token payload safely
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

  const handleLogin = () => {
    setLoginError(null);
    setShowLoginModal(true);

    if (typeof window !== 'undefined' && window.google?.accounts?.id) {
      window.google.accounts.id.initialize({
        client_id: ACTIVE_GOOGLE_CLIENT_ID,
        callback: handleGoogleCallback,
      });
      window.google.accounts.id.prompt((notification: any) => {
        if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
          // Modal fallback will display the rendered Google button
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
                onClick={handleLogin}
                className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs rounded-xl shadow-md shadow-indigo-600/20 transition flex items-center justify-center gap-2 shrink-0"
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

            {/* Official Google GIS Button Container */}
            <div className="flex flex-col items-center justify-center py-2">
              <div ref={googleBtnRef} className="min-h-[44px]" />
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
