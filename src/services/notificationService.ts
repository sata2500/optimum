import { Capacitor } from '@capacitor/core';
import { LocalNotifications } from '@capacitor/local-notifications';
import { storageService } from './storageService';

// Time calculation helper functions
function parseTimeToMinutes(timeStr: string): number {
  const [h, m] = timeStr.split(':').map(Number);
  return h * 60 + m;
}

function isTimeInActiveRange(timeMinutes: number, startMin: number, endMin: number): boolean {
  if (endMin >= startMin) {
    return timeMinutes >= startMin && timeMinutes <= endMin;
  } else {
    // Midnight crossover, e.g., 07:30 to 00:30
    return timeMinutes >= startMin || timeMinutes <= endMin;
  }
}

export function getNextSlots(
  count: number,
  intervalMinutes: number,
  startHourStr: string,
  endHourStr: string
): Date[] {
  const slots: Date[] = [];
  const startMin = parseTimeToMinutes(startHourStr);
  const endMin = parseTimeToMinutes(endHourStr);
  
  const current = new Date();
  const msInMinute = 60 * 1000;
  const minutesSinceMidnight = current.getHours() * 60 + current.getMinutes();
  
  // Align current to the next interval boundary
  const remainder = minutesSinceMidnight % intervalMinutes;
  // If we are exactly on the boundary, schedule for the next one
  const minutesToAdd = remainder === 0 ? intervalMinutes : intervalMinutes - remainder;
  
  let checkTime = new Date(
    current.getTime() + minutesToAdd * msInMinute - current.getSeconds() * 1000 - current.getMilliseconds()
  );
  
  // Safety check: prevent infinite loop if settings are invalid
  let iterations = 0;
  const maxIterations = 2000; // Look up to ~20 days ahead for 15-min intervals

  while (slots.length < count && iterations < maxIterations) {
    iterations++;
    const h = checkTime.getHours();
    const m = checkTime.getMinutes();
    const timeMinutes = h * 60 + m;
    
    if (isTimeInActiveRange(timeMinutes, startMin, endMin)) {
      slots.push(new Date(checkTime));
    }
    
    // Add interval
    checkTime = new Date(checkTime.getTime() + intervalMinutes * msInMinute);
  }
  
  return slots;
}

export const notificationService = {
  isSupported(): boolean {
    return Capacitor.isNativePlatform() || 'Notification' in window;
  },

  async requestPermission(): Promise<boolean> {
    if (Capacitor.isNativePlatform()) {
      try {
        const perm = await LocalNotifications.requestPermissions();
        return perm.display === 'granted';
      } catch (err) {
        console.error('Capacitor permissions request error:', err);
        return false;
      }
    } else if ('Notification' in window) {
      try {
        // Modern Promise-based style
        const permission = await Notification.requestPermission();
        return permission === 'granted';
      } catch (err) {
        console.warn('Notification.requestPermission returned no Promise, using Callback fallback:', err);
        return new Promise((resolve) => {
          try {
            Notification.requestPermission((permission) => {
              resolve(permission === 'granted');
            });
          } catch (e) {
            console.error('Callback Notification request failed:', e);
            resolve(false);
          }
        });
      }
    }
    return false;
  },

  async getPermissionStatus(): Promise<'granted' | 'denied' | 'default'> {
    if (Capacitor.isNativePlatform()) {
      const status = await LocalNotifications.checkPermissions();
      if (status.display === 'granted') return 'granted';
      if (status.display === 'denied') return 'denied';
      return 'default';
    } else if ('Notification' in window) {
      return Notification.permission;
    }
    return 'denied';
  },

  // Reset and schedule a batch of future notifications
  async rescheduleNotifications(): Promise<void> {
    const settings = storageService.getSettings();
    if (!settings.notificationsEnabled) {
      await this.cancelAllNotifications();
      return;
    }

    let permStatus = await this.getPermissionStatus();
    if (permStatus === 'default') {
      const granted = await this.requestPermission();
      permStatus = granted ? 'granted' : 'denied';
    }

    if (permStatus !== 'granted') {
      console.warn('Notifications enabled in settings but permission is not granted.');
      return;
    }

    if (Capacitor.isNativePlatform()) {
      try {
        // Cancel existing scheduled notifications
        await this.cancelAllNotifications();

        // Calculate next 20 slots
        const slots = getNextSlots(
          20, 
          settings.intervalMinutes, 
          settings.startHour, 
          settings.endHour
        );

        if (slots.length === 0) return;

        const notifications = slots.map((slotDate, index) => {
          const slotStr = `${String(slotDate.getHours()).padStart(2, '0')}:${String(slotDate.getMinutes()).padStart(2, '0')}`;
          const dateStr = slotDate.toISOString().split('T')[0];
          
          return {
            title: 'Optimum Flow',
            body: `Son dilimde (${slotStr}) ne yaptın? Kaydetmek için dokun.`,
            id: index + 1,
            schedule: { at: slotDate },
            sound: undefined,
            attachments: undefined,
            actionTypeId: '',
            extra: {
              slot: slotStr,
              date: dateStr,
            },
          };
        });

        await LocalNotifications.schedule({ notifications });
        console.log(`Successfully scheduled ${notifications.length} mobile notifications.`);
      } catch (err) {
        console.error('Failed to schedule mobile notifications:', err);
      }
    } else {
      // In Web Browser, we schedule a single timer for the very next slot
      this.scheduleBrowserTimer();
    }
  },

  async cancelAllNotifications(): Promise<void> {
    if (Capacitor.isNativePlatform()) {
      try {
        const pending = await LocalNotifications.getPending();
        if (pending.notifications.length > 0) {
          await LocalNotifications.cancel({
            notifications: pending.notifications.map(n => ({ id: n.id })),
          });
        }
      } catch (err) {
        console.error('Failed to cancel notifications:', err);
      }
    } else {
      if (this.browserTimer) {
        clearTimeout(this.browserTimer);
        this.browserTimer = null;
      }
    }
  },

  // Web Browser specific timer reference
  browserTimer: null as any,

  scheduleBrowserTimer(): void {
    if (this.browserTimer) {
      clearTimeout(this.browserTimer);
    }

    if (!('Notification' in window) || Notification.permission !== 'granted') {
      return;
    }

    const settings = storageService.getSettings();
    if (!settings.notificationsEnabled) return;

    // Get the single next slot
    const slots = getNextSlots(
      1, 
      settings.intervalMinutes, 
      settings.startHour, 
      settings.endHour
    );
    if (slots.length === 0) return;

    const nextSlot = slots[0];
    const msUntilSlot = nextSlot.getTime() - Date.now();

    if (msUntilSlot <= 0) return;

    this.browserTimer = setTimeout(() => {
      const slotStr = `${String(nextSlot.getHours()).padStart(2, '0')}:${String(nextSlot.getMinutes()).padStart(2, '0')}`;
      const dateStr = nextSlot.toISOString().split('T')[0];

      try {
        const notification = new Notification('Optimum Flow', {
          body: `Son dilimde (${slotStr}) ne yaptın? Kaydetmek için tıkla.`,
          tag: 'optimum-flow-reminder',
          requireInteraction: true,
        });

        notification.onclick = () => {
          window.focus();
          const event = new CustomEvent('optimum-notification-clicked', {
            detail: { slot: slotStr, date: dateStr },
          });
          window.dispatchEvent(event);
          notification.close();
        };
      } catch (err) {
        console.error('Failed to create browser Notification:', err);
        // Fallback to alert if in focus, or fallback alert notification
      }

      // Reschedule the next one
      this.scheduleBrowserTimer();
    }, msUntilSlot);
  },
};

// Setup Capacitor notification action listener
if (Capacitor.isNativePlatform()) {
  LocalNotifications.addListener('localNotificationActionPerformed', (action) => {
    const extra = action.notification.extra;
    if (extra && extra.slot && extra.date) {
      // Trigger event for React to show logging dialog
      setTimeout(() => {
        const event = new CustomEvent('optimum-notification-clicked', {
          detail: { slot: extra.slot, date: extra.date },
        });
        window.dispatchEvent(event);
      }, 500); // Small delay to ensure React App is fully loaded
    }
  });
}
