import { useState, useMemo } from 'react';
import { 
  BarChart3, ListFilter, AlertCircle, Calendar, Download, Printer, TrendingUp, Sparkles
} from 'lucide-react';
import type { Category, TimeLog } from '../services/storageService';
import { storageService } from '../services/storageService';

interface AnalyticsProps {
  categories: Category[];
  logs: TimeLog[];
}

export default function Analytics({ categories, logs }: AnalyticsProps) {
  const [filterCategoryId, setFilterCategoryId] = useState<string>('all');
  const [timeRange, setTimeRange] = useState<'7days' | '30days' | 'all' | 'custom'>('7days');
  
  // Custom Date Picker States
  const [startDateStr, setStartDateStr] = useState<string>(() => {
    const d = new Date();
    d.setDate(d.getDate() - 7);
    return d.toISOString().split('T')[0];
  });
  const [endDateStr, setEndDateStr] = useState<string>(() => {
    return new Date().toISOString().split('T')[0];
  });

  // ── FILTER LOGS BY DATE RANGE ─────────────────────────────────────
  const rangeLogs = useMemo(() => {
    const now = Date.now();
    
    if (timeRange === 'custom') {
      const startMs = new Date(startDateStr + 'T00:00:00').getTime();
      const endMs = new Date(endDateStr + 'T23:59:59').getTime();
      return logs.filter(log => log.timestamp >= startMs && log.timestamp <= endMs);
    }
    
    let msLimit = Infinity;
    if (timeRange === '7days') msLimit = 7 * 24 * 60 * 60 * 1000;
    else if (timeRange === '30days') msLimit = 30 * 24 * 60 * 60 * 1000;
    
    return logs.filter(log => now - log.timestamp <= msLimit);
  }, [logs, timeRange, startDateStr, endDateStr]);

  // ── 1. PRODUCTIVITY METRICS ────────────────────────────────────────
  const { productivityScore, productiveHours, totalHours } = useMemo(() => {
    if (rangeLogs.length === 0) return { productivityScore: 0, productiveHours: 0, totalHours: 0 };
    
    const intervalMinutes = storageService.getSettings().intervalMinutes;
    let totalMinutes = 0;
    let productiveMinutes = 0;
    
    rangeLogs.forEach(l => {
      const mins = l.durationMinutes || intervalMinutes;
      totalMinutes += mins;
      const cat = categories.find(c => c.id === l.categoryId);
      const isProductive = cat ? (cat.isProductive !== false) : ['egitim', 'market', 'ibadet', 'sosyal'].includes(l.categoryId);
      if (isProductive) {
        productiveMinutes += mins;
      }
    });

    const totalHours = totalMinutes / 60;
    const productiveHours = productiveMinutes / 60;
    const productivityScore = totalMinutes > 0 ? Math.round((productiveMinutes / totalMinutes) * 100) : 0;

    return { productivityScore, productiveHours, totalHours };
  }, [rangeLogs, categories]);

  // ── 2. CATEGORY SHARE BREAKDOWN (DONUT) ───────────────────────────
  const categoryShares = useMemo(() => {
    const shares: { [key: string]: { category: Category; minutes: number; hours: number; percentage: number } } = {};
    
    categories.forEach(cat => {
      shares[cat.id] = { category: cat, minutes: 0, hours: 0, percentage: 0 };
    });

    const intervalMinutes = storageService.getSettings().intervalMinutes;
    let totalMinutes = 0;

    rangeLogs.forEach(log => {
      if (shares[log.categoryId]) {
        const mins = log.durationMinutes || intervalMinutes;
        shares[log.categoryId].minutes += mins;
        totalMinutes += mins;
      }
    });

    if (totalMinutes === 0) return [];

    return Object.values(shares)
      .map(item => {
        item.hours = item.minutes / 60;
        item.percentage = Math.round((item.minutes / totalMinutes) * 100);
        return item;
      })
      .filter(item => item.minutes > 0)
      .sort((a, b) => b.minutes - a.minutes);
  }, [rangeLogs, categories]);

  // ── 3. WEEKLY STACKED DAILY CHART (LAST 7 DAYS) ────────────────────
  const weeklyStackedData = useMemo(() => {
    const data: { dateLabel: string; displayDate: string; categories: { [catId: string]: number }; totalCount: number }[] = [];
    const intervalMinutes = storageService.getSettings().intervalMinutes;
    
    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      const dateStr = d.toISOString().split('T')[0];
      const displayStr = d.toLocaleDateString('tr-TR', { weekday: 'short', day: 'numeric' });
      
      const dayLogs = rangeLogs.filter(l => l.date === dateStr);
      
      const catCounts: { [catId: string]: number } = {};
      categories.forEach(c => { catCounts[c.id] = 0; });
      
      let dayTotalMinutes = 0;
      dayLogs.forEach(log => {
        if (catCounts[log.categoryId] !== undefined) {
          const mins = log.durationMinutes || intervalMinutes;
          catCounts[log.categoryId] += mins;
          dayTotalMinutes += mins;
        }
      });

      data.push({
        dateLabel: displayStr,
        displayDate: dateStr,
        categories: catCounts,
        totalCount: dayTotalMinutes / intervalMinutes
      });
    }
    return data;
  }, [rangeLogs, categories]);

  // ── 4. DAILY PRODUCTIVITY TREND (LINE CHART) ───────────────────────
  const dailyTrendData = useMemo(() => {
    const datesList: string[] = [];
    const intervalMinutes = storageService.getSettings().intervalMinutes;
    
    if (timeRange === 'custom') {
      const start = new Date(startDateStr);
      const end = new Date(endDateStr);
      for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
        datesList.push(d.toISOString().split('T')[0]);
      }
    } else {
      const daysCount = timeRange === '7days' ? 7 : timeRange === '30days' ? 30 : 15;
      for (let i = daysCount - 1; i >= 0; i--) {
        const d = new Date();
        d.setDate(d.getDate() - i);
        datesList.push(d.toISOString().split('T')[0]);
      }
    }

    return datesList.map(dateStr => {
      const dayLogs = logs.filter(l => l.date === dateStr);
      let totalMin = 0;
      let prodMin = 0;
      dayLogs.forEach(l => {
        const mins = l.durationMinutes || intervalMinutes;
        totalMin += mins;
        const cat = categories.find(c => c.id === l.categoryId);
        const isProductive = cat ? (cat.isProductive !== false) : ['egitim', 'market', 'ibadet', 'sosyal'].includes(l.categoryId);
        if (isProductive) prodMin += mins;
      });
      
      const score = totalMin > 0 ? Math.round((prodMin / totalMin) * 100) : 0;
      const shortDate = new Date(dateStr).toLocaleDateString('tr-TR', { day: 'numeric', month: 'short' });
      return { date: dateStr, shortDate, score, hasLogs: dayLogs.length > 0 };
    });
  }, [logs, categories, timeRange, startDateStr, endDateStr]);

  // ── 5. TOP 5 MOST TIMED ACTIVITIES ────────────────────────────────
  const topActivities = useMemo(() => {
    const activitiesMap: { [key: string]: { name: string; code: string; color: string; minutes: number } } = {};
    const intervalMinutes = storageService.getSettings().intervalMinutes;
    
    rangeLogs.forEach(log => {
      const mins = log.durationMinutes || intervalMinutes;
      if (!activitiesMap[log.activityId]) {
        activitiesMap[log.activityId] = {
          name: log.activityName,
          code: log.activityCode,
          color: log.categoryColor,
          minutes: 0
        };
      }
      activitiesMap[log.activityId].minutes += mins;
    });

    const sorted = Object.values(activitiesMap).sort((a, b) => b.minutes - a.minutes);
    const maxMinutes = sorted[0]?.minutes || 1;

    return sorted.slice(0, 5).map(item => ({
      ...item,
      hours: item.minutes / 60,
      percentOfMax: Math.round((item.minutes / maxMinutes) * 100)
    }));
  }, [rangeLogs]);

  // History list filter
  const displayedHistoryLogs = useMemo(() => {
    return rangeLogs
      .filter(log => filterCategoryId === 'all' || log.categoryId === filterCategoryId)
      .sort((a, b) => b.timestamp - a.timestamp)
      .slice(0, 50);
  }, [rangeLogs, filterCategoryId]);

  // ── CSV EXPORT ─────────────────────────────────────────────────────
  const handleExportCSV = () => {
    if (rangeLogs.length === 0) return;
    
    const headers = 'Tarih;Zaman Dilimi;Kategori;Aktivite Kodu;Aktivite Adı;Süre (Dk);Notlar;Senkronize Durumu\n';
    const rows = rangeLogs.map(l => {
      const intervalMinutes = storageService.getSettings().intervalMinutes;
      const duration = l.durationMinutes || intervalMinutes;
      return `"${l.date}";"${l.timeSlot}";"${l.categoryName}";"${l.activityCode}";"${l.activityName}";${duration};"${l.notes || ''}";"${l.synced ? 'Evet' : 'Hayır'}"`;
    }).join('\n');

    // UTF-8 BOM so Excel opens special Turkish characters correctly
    const blob = new Blob(['\uFEFF' + headers + rows], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `optimum_analiz_raporu_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // ── PRINT REPORT (PDF) ─────────────────────────────────────────────
  const handlePrint = () => {
    window.print();
  };

  // SVG parameters for Donut Chart
  const donutRadius = 60;
  const donutStrokeWidth = 14;
  const donutCircumference = 2 * Math.PI * donutRadius;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Time Range Selector & Actions Header */}
      <div 
        className="glass-panel no-print" 
        style={{ 
          padding: '18px 24px', 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center', 
          flexWrap: 'wrap', 
          gap: '16px' 
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <BarChart3 size={20} color="var(--color-primary)" />
          <h2 style={{ fontSize: '1.2rem', fontFamily: 'Outfit', margin: 0 }}>Analiz Paneli</h2>
        </div>
        
        {/* Buttons and controls */}
        <div style={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}>
          {/* Ranges */}
          <div style={{ display: 'flex', background: 'rgba(255,255,255,0.03)', padding: '2px', borderRadius: '8px', border: '1px solid var(--color-border)' }}>
            {(['7days', '30days', 'all', 'custom'] as const).map(range => (
              <button
                key={range}
                onClick={() => setTimeRange(range)}
                style={{
                  padding: '6px 12px',
                  borderRadius: '6px',
                  fontSize: '0.8rem',
                  border: 'none',
                  cursor: 'pointer',
                  background: timeRange === range ? 'var(--color-primary)' : 'transparent',
                  color: timeRange === range ? '#fff' : 'var(--color-text-secondary)',
                  fontWeight: '600',
                  transition: 'all 0.15s ease'
                }}
              >
                {range === '7days' ? '7 Gün' : range === '30days' ? '30 Gün' : range === 'all' ? 'Tümü' : 'Özel'}
              </button>
            ))}
          </div>

          {/* CSV & PDF Actions */}
          {logs.length > 0 && (
            <div style={{ display: 'flex', gap: '6px' }}>
              <button 
                onClick={handleExportCSV} 
                className="btn btn-secondary" 
                style={{ padding: '6px 12px', fontSize: '0.8rem', borderRadius: '8px' }}
                title="Verileri Excel/CSV olarak indir"
              >
                <Download size={14} />
                Excel / CSV
              </button>
              <button 
                onClick={handlePrint} 
                className="btn btn-secondary" 
                style={{ padding: '6px 12px', fontSize: '0.8rem', borderRadius: '8px' }}
                title="Raporu Yazdır / PDF Kaydet"
              >
                <Printer size={14} />
                Yazdır / PDF
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Custom Date Pickers Drawer */}
      {timeRange === 'custom' && (
        <div className="glass-panel no-print animate-scale-in" style={{ padding: '16px 20px', display: 'flex', alignItems: 'center', gap: '16px', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Calendar size={15} color="var(--color-text-muted)" />
            <span style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', fontWeight: '600' }}>Tarih Aralığı Seçin:</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <input 
              type="date" 
              value={startDateStr} 
              onChange={e => setStartDateStr(e.target.value)} 
              style={{ colorScheme: 'dark', padding: '6px 12px', borderRadius: '8px', fontSize: '0.85rem' }} 
            />
            <span style={{ color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>-</span>
            <input 
              type="date" 
              value={endDateStr} 
              onChange={e => setEndDateStr(e.target.value)} 
              style={{ colorScheme: 'dark', padding: '6px 12px', borderRadius: '8px', fontSize: '0.85rem' }} 
            />
          </div>
        </div>
      )}

      {logs.length === 0 ? (
        <div 
          className="glass-panel" 
          style={{ 
            padding: '48px 24px', 
            textAlign: 'center', 
            color: 'var(--color-text-secondary)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '12px'
          }}
        >
          <AlertCircle size={40} color="var(--color-text-muted)" />
          <div>
            <h3 style={{ color: '#fff', fontSize: '1.15rem', marginBottom: '6px' }}>Analiz Kaydı Bulunmuyor</h3>
            <p style={{ fontSize: '0.85rem' }}>
              Grafiklerin ve istatistiklerin görünebilmesi için öncelikle zaman kaydı ekleyin.
            </p>
          </div>
        </div>
      ) : (
        <>
          {/* Printable Report Header */}
          <div className="only-print" style={{ display: 'none', marginBottom: '20px' }}>
            <h1 style={{ fontSize: '2rem', fontWeight: '800' }}>OPTIMUM ZAMAN RAPORU</h1>
            <p style={{ fontSize: '0.9rem', color: '#666' }}>
              Rapor Tarihi: {new Date().toLocaleDateString('tr-TR')} | 
              Aralık: {timeRange === 'custom' ? `${startDateStr} ile ${endDateStr}` : timeRange === '7days' ? 'Son 7 Gün' : timeRange === '30days' ? 'Son 30 Gün' : 'Tüm Zamanlar'}
            </p>
          </div>

          {/* Productivity Dashboard Widget Row */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '16px' }}>
            
            {/* Efficiency Score Dial */}
            <div 
              className="glass-panel" 
              style={{ 
                padding: '24px', 
                display: 'flex', 
                alignItems: 'center', 
                gap: '20px' 
              }}
            >
              <div style={{ position: 'relative', width: '90px', height: '90px', flexShrink: 0 }}>
                <svg width="90" height="90" style={{ transform: 'rotate(-90deg)' }}>
                  <circle cx="45" cy="45" r="38" fill="transparent" stroke="rgba(255,255,255,0.03)" strokeWidth="6" />
                  <circle 
                    cx="45" 
                    cy="45" 
                    r="38" 
                    fill="transparent" 
                    stroke="var(--color-primary)" 
                    strokeWidth="6" 
                    strokeDasharray={2 * Math.PI * 38}
                    strokeDashoffset={2 * Math.PI * 38 - (productivityScore / 100) * (2 * Math.PI * 38)}
                    strokeLinecap="round"
                    style={{ transition: 'stroke-dashoffset 0.8s ease-out' }}
                  />
                </svg>
                <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', fontWeight: '800', fontSize: '1.25rem' }}>
                  {productivityScore}%
                </div>
              </div>

              <div>
                <h4 style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                  Verimlilik Endeksi
                </h4>
                <h3 style={{ fontSize: '1.4rem', margin: '4px 0', fontFamily: 'Outfit' }}>Üretken Akış</h3>
                <p style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)' }}>
                  Zamanınızın <strong>{productivityScore}%</strong> kısmını üretken/faydalı kategorilere ayırdınız.
                </p>
              </div>
            </div>

            {/* Total Hours Stats */}
            <div 
              className="glass-panel" 
              style={{ 
                padding: '24px', 
                display: 'flex', 
                flexDirection: 'column', 
                justifyContent: 'center',
                gap: '8px'
              }}
            >
              <h4 style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Harcanan Süre Özeti
              </h4>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px' }}>
                <span style={{ fontSize: '2.2rem', fontWeight: '800', fontFamily: 'Outfit', color: 'var(--color-primary)' }}>
                  {productiveHours.toFixed(1)}
                </span>
                <span style={{ fontSize: '0.9rem', color: 'var(--color-text-secondary)' }}>saat verimli</span>
              </div>
              <p style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)' }}>
                Toplamda <strong>{totalHours.toFixed(1)}</strong> saatlik zaman dilimi kaydı tuttunuz.
              </p>
            </div>

          </div>

          {/* SVG Charts Row */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '16px' }}>
            
            {/* Category Share Donut Chart */}
            <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <h3 style={{ fontSize: '1.05rem', fontFamily: 'Outfit' }}>Kategori Dağılım Payları</h3>
              
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '24px', flexWrap: 'wrap', padding: '10px 0' }}>
                
                {/* SVG Donut */}
                <div style={{ position: 'relative', width: '150px', height: '150px' }}>
                  <svg width="150" height="150" style={{ transform: 'rotate(-90deg)' }}>
                    <circle cx="75" cy="75" r={donutRadius} fill="transparent" stroke="rgba(255,255,255,0.03)" strokeWidth={donutStrokeWidth} />
                    
                    {(() => {
                      let accumulatedPercentage = 0;
                      return categoryShares.map((item, idx) => {
                        const dashoffset = - (accumulatedPercentage / 100) * donutCircumference;
                        accumulatedPercentage += item.percentage;
                        
                        return (
                          <circle
                            key={idx}
                            cx="75"
                            cy="75"
                            r={donutRadius}
                            fill="transparent"
                            stroke={item.category.color}
                            strokeWidth={donutStrokeWidth}
                            strokeDasharray={donutCircumference}
                            strokeDashoffset={dashoffset}
                            strokeLinecap="butt"
                            style={{ transition: 'stroke-dashoffset 0.8s ease-out' }}
                          />
                        );
                      });
                    })()}
                  </svg>
                  <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', textAlign: 'center' }}>
                    <span style={{ fontSize: '1.4rem', fontWeight: '800', fontFamily: 'Outfit' }}>{totalHours.toFixed(1)}</span>
                    <br />
                    <span style={{ fontSize: '0.7rem', color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>saat</span>
                  </div>
                </div>

                {/* Donut Legend */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', flex: '1', minWidth: '150px' }}>
                  {categoryShares.map(item => (
                    <div key={item.category.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.85rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: item.category.color }} />
                        <span style={{ fontWeight: '500' }}>{item.category.name}</span>
                      </div>
                      <span style={{ fontWeight: '700', color: 'var(--color-text-secondary)', marginLeft: 'auto' }}>
                        {item.percentage}% ({item.hours.toFixed(1)} sa)
                      </span>
                    </div>
                  ))}
                </div>

              </div>
            </div>

            {/* Stacked Daily Bar Chart */}
            <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <h3 style={{ fontSize: '1.05rem', fontFamily: 'Outfit' }}>Son 7 Günlük Yoğunluk</h3>
              
              <div 
                style={{ 
                  height: '180px', 
                  display: 'flex', 
                  alignItems: 'flex-end', 
                  justifyContent: 'space-around',
                  padding: '10px 0 20px 0',
                  position: 'relative'
                }}
              >
                <div style={{ position: 'absolute', bottom: '20px', left: 0, right: 0, height: '1px', background: 'rgba(255,255,255,0.05)' }} />
                <div style={{ position: 'absolute', bottom: '60px', left: 0, right: 0, height: '1px', background: 'rgba(255,255,255,0.05)' }} />
                <div style={{ position: 'absolute', bottom: '100px', left: 0, right: 0, height: '1px', background: 'rgba(255,255,255,0.05)' }} />
                <div style={{ position: 'absolute', bottom: '140px', left: 0, right: 0, height: '1px', background: 'rgba(255,255,255,0.05)' }} />

                {weeklyStackedData.map((day, idx) => {
                  const maxSlotsPerDay = 35;
                  const totalLogged = day.totalCount;
                  const scaleFactor = Math.min(130 / maxSlotsPerDay, 130 / (totalLogged || 1));
                  
                  return (
                    <div 
                      key={idx} 
                      style={{ 
                        display: 'flex', 
                        flexDirection: 'column', 
                        alignItems: 'center', 
                        height: '100%',
                        justifyContent: 'flex-end',
                        width: '32px',
                        zIndex: 2
                      }}
                    >
                      <div 
                        style={{ 
                          width: '12px', 
                          borderRadius: '6px', 
                          overflow: 'hidden', 
                          display: 'flex', 
                          flexDirection: 'column-reverse', 
                          height: `${totalLogged * scaleFactor}px`,
                          background: 'rgba(255,255,255,0.03)',
                          transition: 'height 0.3s ease-out'
                        }}
                      >
                        {categories.map(cat => {
                          const mins = day.categories[cat.id] || 0;
                          if (mins === 0) return null;
                          const equivSlots = mins / storageService.getSettings().intervalMinutes;
                          return (
                            <div 
                              key={cat.id} 
                              style={{ 
                                height: `${equivSlots * scaleFactor}px`, 
                                background: cat.color,
                                width: '100%' 
                              }}
                              title={`${cat.name}: ${(mins / 60).toFixed(1)} saat`}
                            />
                          );
                        })}
                      </div>

                      <span 
                        style={{ 
                          fontSize: '0.7rem', 
                          color: 'var(--color-text-secondary)', 
                          marginTop: '8px', 
                          whiteSpace: 'nowrap',
                          transform: 'rotate(-15deg)'
                        }}
                      >
                        {day.dateLabel}
                      </span>
                    </div>
                  );
                })}

              </div>
            </div>

          </div>

          {/* Trend & Top Activities Row */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '16px' }}>
            
            {/* SVG Trend Line Chart */}
            <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <TrendingUp size={16} color="var(--color-primary)" />
                <h3 style={{ fontSize: '1.05rem', fontFamily: 'Outfit', margin: 0 }}>Verimlilik Trendi</h3>
              </div>
              
              <div style={{ position: 'relative', width: '100%', height: '180px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                {dailyTrendData.length > 1 ? (
                  <svg viewBox="0 0 380 150" width="100%" height="150" style={{ overflow: 'visible' }}>
                    <defs>
                      <linearGradient id="trend-fill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="var(--color-primary)" stopOpacity="0.18" />
                        <stop offset="100%" stopColor="var(--color-primary)" stopOpacity="0.0" />
                      </linearGradient>
                    </defs>

                    {/* horizontal grid lines */}
                    <line x1="20" y1="20" x2="360" y2="20" stroke="rgba(255,255,255,0.03)" strokeWidth="1" />
                    <line x1="20" y1="65" x2="360" y2="65" stroke="rgba(255,255,255,0.03)" strokeWidth="1" />
                    <line x1="20" y1="110" x2="360" y2="110" stroke="rgba(255,255,255,0.03)" strokeWidth="1" />

                    {/* SVG Line / Path generation */}
                    {(() => {
                      const points = dailyTrendData.map((d, i) => {
                        const x = (i / (dailyTrendData.length - 1)) * 340 + 20;
                        const y = 110 - (d.score / 100) * 90;
                        return { x, y };
                      });

                      const linePath = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ');
                      const areaPath = `${linePath} L ${points[points.length - 1].x} 110 L ${points[0].x} 110 Z`;

                      return (
                        <>
                          {/* Filled Area */}
                          <path d={areaPath} fill="url(#trend-fill)" />
                          {/* Line */}
                          <path d={linePath} fill="none" stroke="var(--color-primary)" strokeWidth="3" strokeLinejoin="round" strokeLinecap="round" />
                          {/* Circles on Nodes */}
                          {points.map((p, i) => (
                            <g key={i} className="trend-node" style={{ cursor: 'pointer' }}>
                              <circle cx={p.x} cy={p.y} r="5" fill="#070a13" stroke="var(--color-primary)" strokeWidth="2.5" />
                              <circle cx={p.x} cy={p.y} r="8" fill="var(--color-primary)" fillOpacity="0" />
                              <title>{dailyTrendData[i].shortDate}: %{dailyTrendData[i].score}</title>
                            </g>
                          ))}
                        </>
                      );
                    })()}

                    {/* Labels */}
                    <text x="5" y="24" fill="var(--color-text-muted)" fontSize="9" fontWeight="700">%100</text>
                    <text x="5" y="69" fill="var(--color-text-muted)" fontSize="9" fontWeight="700">%50</text>
                    <text x="5" y="114" fill="var(--color-text-muted)" fontSize="9" fontWeight="700">%0</text>

                    {/* X-axis labels */}
                    {dailyTrendData.map((d, i) => {
                      // Skip every other label on long data ranges to avoid overlap
                      if (dailyTrendData.length > 8 && i % Math.ceil(dailyTrendData.length / 7) !== 0) return null;
                      const x = (i / (dailyTrendData.length - 1)) * 340 + 20;
                      return (
                        <text key={i} x={x} y="132" fill="var(--color-text-secondary)" fontSize="9" textAnchor="middle" fontWeight="500">
                          {d.shortDate}
                        </text>
                      );
                    })}
                  </svg>
                ) : (
                  <p style={{ textAlign: 'center', fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>Trend grafiği için yeterli gün kaydı yok.</p>
                )}
              </div>
            </div>

            {/* Top 5 Activities Bar Rank */}
            <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Sparkles size={16} color="#eab308" />
                <h3 style={{ fontSize: '1.05rem', fontFamily: 'Outfit', margin: 0 }}>Top 5 En Çok Zaman Alan İş</h3>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {topActivities.length > 0 ? (
                  topActivities.map((act, idx) => (
                    <div key={idx} style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.8rem' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <span style={{ fontSize: '0.7rem', fontWeight: 'bold', background: `${act.color}22`, color: act.color, padding: '2px 6px', borderRadius: '4px' }}>
                            {act.code}
                          </span>
                          <span style={{ fontWeight: '600' }}>{act.name}</span>
                        </div>
                        <span style={{ color: 'var(--color-text-secondary)', fontWeight: 'bold' }}>
                          {act.hours.toFixed(1)} sa
                        </span>
                      </div>
                      {/* Progress background track */}
                      <div style={{ width: '100%', height: '6px', background: 'rgba(255,255,255,0.03)', borderRadius: '3px', overflow: 'hidden' }}>
                        <div 
                          style={{ 
                            height: '100%', 
                            background: act.color, 
                            borderRadius: '3px', 
                            width: `${act.percentOfMax}%`,
                            boxShadow: `0 0 8px ${act.color}40`,
                            transition: 'width 0.5s ease-out'
                          }} 
                        />
                      </div>
                    </div>
                  ))
                ) : (
                  <p style={{ textAlign: 'center', fontSize: '0.85rem', color: 'var(--color-text-secondary)', padding: '20px 0' }}>En çok harcanan aktivite kaydı bulunamadı.</p>
                )}
              </div>
            </div>

          </div>

          {/* History Log Entries List */}
          <div className="glass-panel" style={{ padding: '24px' }}>
            
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px', marginBottom: '20px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <ListFilter size={18} color="var(--color-primary)" />
                <h3 style={{ fontSize: '1.05rem', fontFamily: 'Outfit', margin: 0 }}>Geçmiş Kayıtlar</h3>
              </div>

              {/* History Category Selector */}
              <select 
                value={filterCategoryId} 
                onChange={(e) => setFilterCategoryId(e.target.value)}
                style={{ padding: '6px 12px', borderRadius: '8px', fontSize: '0.8rem' }}
                className="no-print"
              >
                <option value="all">Tüm Kategoriler</option>
                {categories.map(cat => (
                  <option key={cat.id} value={cat.id}>{cat.name}</option>
                ))}
              </select>
            </div>

            {/* Logs List scroll container */}
            <div style={{ maxHeight: '350px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '10px', paddingRight: '4px' }}>
              {displayedHistoryLogs.length > 0 ? (
                displayedHistoryLogs.map(log => (
                  <div 
                    key={log.id}
                    style={{
                      background: 'rgba(255,255,255,0.01)',
                      border: '1px solid var(--color-border)',
                      borderRadius: '12px',
                      padding: '12px 16px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      flexWrap: 'wrap',
                      gap: '12px'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <span 
                        style={{
                          background: log.categoryColor,
                          color: '#fff',
                          padding: '3px 8px',
                          borderRadius: '6px',
                          fontWeight: '800',
                          fontSize: '0.75rem',
                          fontFamily: 'Outfit, sans-serif'
                        }}
                      >
                        {log.activityCode}
                      </span>
                      <div>
                        <h4 style={{ fontSize: '0.85rem', fontWeight: '600', margin: 0 }}>{log.activityName}</h4>
                        <span style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)' }}>{log.categoryName}</span>
                        {log.notes && (
                          <p style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)', marginTop: '4px', fontStyle: 'italic', margin: '4px 0 0 0' }}>
                            "{log.notes}"
                          </p>
                        )}
                      </div>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginLeft: 'auto' }}>
                      <div style={{ textAlign: 'right' }}>
                        <span style={{ display: 'block', fontSize: '0.8rem', fontWeight: '600' }}>{log.timeSlot}</span>
                        <span style={{ display: 'block', fontSize: '0.7rem', color: 'var(--color-text-secondary)' }}>{log.date}</span>
                      </div>
                    </div>
                  </div>
                ))
              ) : (
                <div style={{ textAlign: 'center', padding: '30px 0', color: 'var(--color-text-secondary)', fontSize: '0.85rem' }}>
                  Kayıt bulunamadı.
                </div>
              )}
            </div>

          </div>
        </>
      )}

    </div>
  );
}
