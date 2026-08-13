'use client';

import React, { useState, useEffect } from 'react';
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import ScheduleGrid from './components/ScheduleGrid';
import AnalyticsCharts from './components/AnalyticsCharts';
import CategoryManager from './components/CategoryManager';
import ProfileCloudView from './components/ProfileCloudView';

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

export default function Home() {
  const [activeTab, setActiveTab] = useState<'schedule' | 'analytics' | 'categories' | 'profile'>('schedule');
  const [isSyncing, setIsSyncing] = useState(false);
  const [syncedData, setSyncedData] = useState<any>(null);
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
    const googleClientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || '859090591444-gks1ocsevkb8kdcltbeoe24gi5lbo3pd.apps.googleusercontent.com';

    if (typeof window !== 'undefined' && window.google?.accounts?.id) {
      window.google.accounts.id.initialize({
        client_id: googleClientId,
        callback: handleGoogleCallback,
      });
    }
  }, []);

  const handleGoogleCallback = (response: any) => {
    try {
      if (response.credential) {
        // Decode JWT token payload
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
          name: payload.name || payload.email.substringBefore('@'),
          email: payload.email,
          photoUrl: payload.picture,
          isLoggedIn: true,
        };

        setUser(newUser);
        localStorage.setItem('optimum_user_session', JSON.stringify(newUser));
        fetchSyncedData(payload.email);
      }
    } catch (err) {
      console.error('Google Sign-In Error:', err);
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
    const googleClientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || '859090591444-gks1ocsevkb8kdcltbeoe24gi5lbo3pd.apps.googleusercontent.com';

    if (typeof window !== 'undefined' && window.google?.accounts?.id) {
      window.google.accounts.id.initialize({
        client_id: googleClientId,
        callback: handleGoogleCallback,
      });
      window.google.accounts.id.prompt();
    } else {
      alert('Google Sign-In kütüphanesi yükleniyor, lütfen birkaç saniye sonra tekrar deneyin.');
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
    <div className="min-h-screen flex flex-col bg-[#0b0f19] text-gray-100">
      {/* Top Navigation Bar */}
      <Header
        user={user}
        onLogin={handleLogin}
        onLogout={handleLogout}
        onSync={handleSync}
        isSyncing={isSyncing}
      />

      <div className="flex flex-1">
        {/* Left Sidebar Menu */}
        <Sidebar activeTab={activeTab} setActiveTab={setActiveTab} />

        {/* Main Content View */}
        <main className="flex-1 p-6 md:p-8 max-w-7xl">
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
    </div>
  );
}
