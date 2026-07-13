import { useState } from 'react';
import { BarChart3, ListFilter, AlertCircle } from 'lucide-react';
import type { Category, TimeLog } from '../services/storageService';
import { storageService } from '../services/storageService';

interface AnalyticsProps {
  categories: Category[];
  logs: TimeLog[];
}

export default function Analytics({ categories, logs }: AnalyticsProps) {
  const [filterCategoryId, setFilterCategoryId] = useState<string>('all');
  const [timeRange, setTimeRange] = useState<'7days' | '30days' | 'all'>('7days');

  // Filter logs by date range
  const getFilteredLogsByRange = () => {
    const now = Date.now();
    let msLimit = Infinity;
    
    if (timeRange === '7days') msLimit = 7 * 24 * 60 * 60 * 1000;
    else if (timeRange === '30days') msLimit = 30 * 24 * 60 * 60 * 1000;
    
    return logs.filter(log => now - log.timestamp <= msLimit);
  };

  const rangeLogs = getFilteredLogsByRange();

  // 1. Productivity Calculation
  // Productive categories: Eğitim, Market, İbadet, Sosyal
  // Unproductive categories: Telefon, Zaman Kaybı
  const getProductivityMetrics = () => {
    if (rangeLogs.length === 0) return { score: 0, productiveHours: 0, totalHours: 0 };
    
    const intervalMinutes = storageService.getSettings().intervalMinutes;
    let totalMinutes = 0;
    let productiveMinutes = 0;
    
    rangeLogs.forEach(l => {
      const mins = l.durationMinutes || intervalMinutes;
      totalMinutes += mins;
      if (['egitim', 'market', 'ibadet', 'sosyal'].includes(l.categoryId)) {
        productiveMinutes += mins;
      }
    });

    const totalHours = totalMinutes / 60;
    const productiveHours = productiveMinutes / 60;
    const score = totalMinutes > 0 ? Math.round((productiveMinutes / totalMinutes) * 100) : 0;

    return { score, productiveHours, totalHours };
  };

  const { score: productivityScore, productiveHours, totalHours } = getProductivityMetrics();

  // 2. Category Share Breakdown for Donut Chart
  const getCategoryShare = () => {
    const shares: { [key: string]: { category: Category; minutes: number; hours: number; percentage: number } } = {};
    
    // Initialize
    categories.forEach(cat => {
      shares[cat.id] = { category: cat, minutes: 0, hours: 0, percentage: 0 };
    });

    const intervalMinutes = storageService.getSettings().intervalMinutes;
    let totalMinutes = 0;

    // Add minutes
    rangeLogs.forEach(log => {
      if (shares[log.categoryId]) {
        const mins = log.durationMinutes || intervalMinutes;
        shares[log.categoryId].minutes += mins;
        totalMinutes += mins;
      }
    });

    if (totalMinutes === 0) return [];

    // Calculate percentage and hours
    return Object.values(shares)
      .map(item => {
        item.hours = item.minutes / 60;
        item.percentage = Math.round((item.minutes / totalMinutes) * 100);
        return item;
      })
      .filter(item => item.minutes > 0)
      .sort((a, b) => b.minutes - a.minutes);
  };

  const categoryShares = getCategoryShare();

  // 3. Weekly Stacked Daily Chart (Last 7 Days)
  const getWeeklyStackedData = () => {
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
  };

  const weeklyStackedData = getWeeklyStackedData();

  // History list filter
  const displayedHistoryLogs = rangeLogs
    .filter(log => filterCategoryId === 'all' || log.categoryId === filterCategoryId)
    .sort((a, b) => b.timestamp - a.timestamp)
    .slice(0, 50); // Show last 50 entries

  // SVG parameters for Donut Chart
  const donutRadius = 60;
  const donutStrokeWidth = 14;
  const donutCircumference = 2 * Math.PI * donutRadius;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Time Range Selector & Navigation */}
      <div 
        className="glass-panel" 
        style={{ 
          padding: '16px 20px', 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center', 
          flexWrap: 'wrap', 
          gap: '12px' 
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <BarChart3 size={20} color="var(--color-primary)" />
          <h2 style={{ fontSize: '1.2rem', fontFamily: 'Outfit' }}>Zaman Dağılım Analizi</h2>
        </div>
        
        <div style={{ display: 'flex', gap: '6px' }}>
          {(['7days', '30days', 'all'] as const).map(range => (
            <button
              key={range}
              onClick={() => setTimeRange(range)}
              style={{
                padding: '6px 12px',
                borderRadius: '8px',
                fontSize: '0.8rem',
                border: 'none',
                cursor: 'pointer',
                background: timeRange === range ? 'var(--color-primary)' : 'rgba(255,255,255,0.05)',
                color: '#fff',
                fontWeight: '600',
                transition: 'all 0.15s ease'
              }}
            >
              {range === '7days' ? 'Son 7 Gün' : range === '30days' ? 'Son 30 Gün' : 'Tümü'}
            </button>
          ))}
        </div>
      </div>

      {logs.length === 0 ? (
        <div 
          className="glass-panel" 
          style={{ 
            padding: '40px 20px', 
            textAlign: 'center', 
            color: 'var(--color-text-secondary)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '12px'
          }}
        >
          <AlertCircle size={32} color="var(--color-text-muted)" />
          <div>
            <h3 style={{ color: '#fff', fontSize: '1.1rem', marginBottom: '4px' }}>Henüz Kayıt Bulunmuyor</h3>
            <p style={{ fontSize: '0.85rem' }}>
              Grafiklerin ve analizlerin görünmesi için zamanlayıcıdan veya matris üzerinden kayıtlar ekleyin.
            </p>
          </div>
        </div>
      ) : (
        <>
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
                <h3 style={{ fontSize: '1.4rem', margin: '4px 0', fontFamily: 'Outfit' }}>Faydalı Akış</h3>
                <p style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)' }}>
                  Zamanınızın <strong>{productivityScore}%</strong> kısmını gelişim ve iş aktivitelerine ayırdınız.
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
              
              {/* Stacked Chart Container */}
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
                {/* Horizontal grid lines */}
                <div style={{ position: 'absolute', bottom: '20px', left: 0, right: 0, height: '1px', background: 'rgba(255,255,255,0.05)' }} />
                <div style={{ position: 'absolute', bottom: '60px', left: 0, right: 0, height: '1px', background: 'rgba(255,255,255,0.05)' }} />
                <div style={{ position: 'absolute', bottom: '100px', left: 0, right: 0, height: '1px', background: 'rgba(255,255,255,0.05)' }} />
                <div style={{ position: 'absolute', bottom: '140px', left: 0, right: 0, height: '1px', background: 'rgba(255,255,255,0.05)' }} />

                {weeklyStackedData.map((day, idx) => {
                  const maxSlotsPerDay = 35; // Cap at 35 slots for scaling height
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
                      {/* Stacked bars wrapper */}
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

                      {/* X Axis Label */}
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

          {/* History Log Entries List */}
          <div className="glass-panel" style={{ padding: '24px' }}>
            
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px', marginBottom: '20px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <ListFilter size={18} color="var(--color-primary)" />
                <h3 style={{ fontSize: '1.05rem', fontFamily: 'Outfit' }}>Geçmiş Kayıtlar</h3>
              </div>

              {/* History Category Selector */}
              <select 
                value={filterCategoryId} 
                onChange={(e) => setFilterCategoryId(e.target.value)}
                style={{ padding: '6px 12px', borderRadius: '8px', fontSize: '0.8rem' }}
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
                        <h4 style={{ fontSize: '0.85rem', fontWeight: '600' }}>{log.activityName}</h4>
                        <span style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)' }}>{log.categoryName}</span>
                        {log.notes && (
                          <p style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)', marginTop: '4px', fontStyle: 'italic' }}>
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
