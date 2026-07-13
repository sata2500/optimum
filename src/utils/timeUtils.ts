/**
 * Parses a HH:MM formatted time string into minutes from midnight.
 * Example: "01:30" -> 90
 */
export function parseTimeToMinutes(timeStr: string): number {
  const [h, m] = timeStr.split(':').map(Number);
  return h * 60 + m;
}

/**
 * Generates slots from a start time to an end time using an interval.
 * Handles midnight crossovers.
 */
export function generateSlots(start: string, end: string, interval: number): string[] {
  const slots: string[] = [];
  const startMin = parseTimeToMinutes(start);
  let endMin = parseTimeToMinutes(end);
  
  if (endMin < startMin) {
    endMin += 1440; // Spans midnight
  }
  
  for (let min = startMin; min <= endMin; min += interval) {
    const totalMin = min % 1440;
    const h = Math.floor(totalMin / 60);
    const m = totalMin % 60;
    slots.push(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`);
  }
  return slots;
}
