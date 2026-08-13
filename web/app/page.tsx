'use client';

import React, { useState } from 'react';
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import ScheduleGrid from './components/ScheduleGrid';
import AnalyticsCharts from './components/AnalyticsCharts';
import CategoryManager from './components/CategoryManager';
import ProfileCloudView from './components/ProfileCloudView';

export default function Home() {
  const [activeTab, setActiveTab] = useState<'schedule' | 'analytics' | 'categories' | 'profile'>('schedule');
  const [isSyncing, setIsSyncing] = useState(false);
  const [user, setUser] = useState({
    name: 'Salih Evin',
    email: 'salih@optimum.tech',
    isLoggedIn: true,
  });

  const handleLogin = () => {
    setUser({
      name: 'Salih Evin',
      email: 'salih@optimum.tech',
      isLoggedIn: true,
    });
  };

  const handleLogout = () => {
    setUser({
      name: '',
      email: '',
      isLoggedIn: false,
    });
  };

  const handleSync = () => {
    setIsSyncing(true);
    setTimeout(() => {
      setIsSyncing(false);
    }, 1200);
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
          {activeTab === 'schedule' && <ScheduleGrid />}
          {activeTab === 'analytics' && <AnalyticsCharts />}
          {activeTab === 'categories' && <CategoryManager />}
          {activeTab === 'profile' && (
            <ProfileCloudView
              user={user}
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
