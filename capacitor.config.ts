import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.optimum.flow',
  appName: 'Optimum Flow',
  webDir: 'dist',
  plugins: {
    LocalNotifications: {
      smallIcon: 'ic_stat_icon_config_sample',
      iconColor: '#38bdf8',
      sound: 'beep.wav'
    }
  }
};


export default config;
