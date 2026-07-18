import type { Category, TimeLog, AppSettings } from '../services/storageService';

/**
 * Checks if a specific category ID is productive, using category metadata or hardcoded defaults.
 */
export function isCategoryProductive(categoryId: string, categories: Category[]): boolean {
  const cat = categories.find(c => c.id === categoryId);
  if (cat) {
    return cat.isProductive !== false;
  }
  // Fallback for default categories if categories are not loaded/found
  return ['egitim', 'market', 'ibadet', 'sosyal'].includes(categoryId);
}

/**
 * Checks if a TimeLog is productive based on the category list.
 */
export function isLogProductive(log: TimeLog, categories: Category[]): boolean {
  return isCategoryProductive(log.categoryId, categories);
}

/**
 * Calculates the current productivity streak (chain of consecutive days meeting the daily goal).
 */
export function calculateStreak(
  logs: TimeLog[],
  categories: Category[],
  settings: AppSettings
): number {
  if (logs.length === 0) return 0;
  const dailyGoalHours = settings.dailyProductiveTargetHours || 4;
  const intervalMinutes = settings.intervalMinutes;

  // Group productive minutes by date
  const dailyProductiveMinutes: { [date: string]: number } = {};
  logs.forEach(l => {
    if (isLogProductive(l, categories)) {
      const mins = l.durationMinutes || intervalMinutes;
      dailyProductiveMinutes[l.date] = (dailyProductiveMinutes[l.date] || 0) + mins;
    }
  });

  let streak = 0;
  const todayStr = new Date().toISOString().split('T')[0];
  
  // Check today first
  const todayMins = dailyProductiveMinutes[todayStr] || 0;
  const todayMet = (todayMins / 60) >= dailyGoalHours;

  // Start checking from yesterday backwards
  const checkDate = new Date();
  checkDate.setDate(checkDate.getDate() - 1);

  while (true) {
    const dateKey = checkDate.toISOString().split('T')[0];
    const dayMins = dailyProductiveMinutes[dateKey] || 0;
    const met = (dayMins / 60) >= dailyGoalHours;
    if (met) {
      streak++;
      checkDate.setDate(checkDate.getDate() - 1);
    } else {
      break;
    }
  }

  if (todayMet) {
    streak++;
  }

  return streak;
}
