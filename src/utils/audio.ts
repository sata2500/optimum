export type NotificationSoundType = 'modern' | 'classic' | 'soft' | 'silent';

export function playNotificationSound(sound: NotificationSoundType) {
  if (sound === 'silent') return;

  try {
    const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
    if (!AudioCtx) return;
    const ctx = new AudioCtx();

    if (sound === 'modern') {
      // Dynamic modern arpeggio (C5 -> E5 -> G5 -> C6)
      const notes = [523.25, 659.25, 783.99, 1046.50];
      const noteDuration = 0.08;
      
      notes.forEach((freq, index) => {
        const osc = ctx.createOscillator();
        const gainNode = ctx.createGain();
        
        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, ctx.currentTime + index * noteDuration);
        
        gainNode.gain.setValueAtTime(0, ctx.currentTime + index * noteDuration);
        gainNode.gain.linearRampToValueAtTime(0.3, ctx.currentTime + index * noteDuration + 0.01);
        gainNode.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + index * noteDuration + 0.25);
        
        osc.connect(gainNode);
        gainNode.connect(ctx.destination);
        
        osc.start(ctx.currentTime + index * noteDuration);
        osc.stop(ctx.currentTime + index * noteDuration + 0.3);
      });
      
    } else if (sound === 'classic') {
      // Traditional double-beep
      const duration = 0.12;
      const gap = 0.15;
      const beeps = [0, duration + gap];
      
      beeps.forEach(delay => {
        const osc = ctx.createOscillator();
        const gainNode = ctx.createGain();
        
        osc.type = 'sine';
        osc.frequency.setValueAtTime(880.00, ctx.currentTime + delay); // A5
        
        gainNode.gain.setValueAtTime(0, ctx.currentTime + delay);
        gainNode.gain.linearRampToValueAtTime(0.3, ctx.currentTime + delay + 0.01);
        gainNode.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + delay + duration);
        
        osc.connect(gainNode);
        gainNode.connect(ctx.destination);
        
        osc.start(ctx.currentTime + delay);
        osc.stop(ctx.currentTime + delay + duration);
      });
      
    } else if (sound === 'soft') {
      // Gentle warm gong
      const osc = ctx.createOscillator();
      const gainNode = ctx.createGain();
      
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(329.63, ctx.currentTime); // E4
      osc.frequency.exponentialRampToValueAtTime(220.00, ctx.currentTime + 0.3); // A3
      
      gainNode.gain.setValueAtTime(0.4, ctx.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 1.2);
      
      osc.connect(gainNode);
      gainNode.connect(ctx.destination);
      
      osc.start();
      osc.stop(ctx.currentTime + 1.2);
    }
  } catch (err) {
    console.warn('Failed to play dynamic audio tone:', err);
  }
}
