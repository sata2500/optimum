interface OptimumLogoProps {
  size?: number;
  className?: string;
}

export default function OptimumLogo({ size = 36, className }: OptimumLogoProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 200 200"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
    >
      <defs>
        <linearGradient id="ol-bg" x1="0" y1="0" x2="200" y2="200" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#1e293b"/>
          <stop offset="100%" stopColor="#0f172a"/>
        </linearGradient>
        <linearGradient id="ol-blue" x1="30" y1="65" x2="100" y2="135" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#38bdf8"/>
          <stop offset="100%" stopColor="#818cf8"/>
        </linearGradient>
        <linearGradient id="ol-orange" x1="100" y1="65" x2="170" y2="135" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#fb923c"/>
          <stop offset="100%" stopColor="#ef4444"/>
        </linearGradient>
        <linearGradient id="ol-arrow" x1="115" y1="130" x2="162" y2="38" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#fb923c"/>
          <stop offset="100%" stopColor="#fbbf24"/>
        </linearGradient>
        <filter id="ol-glow-b">
          <feGaussianBlur stdDeviation="3" result="blur"/>
          <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
        <filter id="ol-glow-o">
          <feGaussianBlur stdDeviation="3" result="blur"/>
          <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
      </defs>

      {/* Background */}
      <circle cx="100" cy="100" r="96" fill="url(#ol-bg)"/>

      {/* Outer ring */}
      <circle cx="100" cy="100" r="92" fill="none" stroke="#334155" strokeWidth="2"/>

      {/* Tick marks at 12, 3, 6, 9 */}
      <line x1="100" y1="10" x2="100" y2="24" stroke="#475569" strokeWidth="4" strokeLinecap="round"/>
      <line x1="190" y1="100" x2="176" y2="100" stroke="#475569" strokeWidth="4" strokeLinecap="round"/>
      <line x1="100" y1="190" x2="100" y2="176" stroke="#475569" strokeWidth="4" strokeLinecap="round"/>
      <line x1="10" y1="100" x2="24" y2="100" stroke="#475569" strokeWidth="4" strokeLinecap="round"/>

      {/* Clock numbers */}
      <text x="164" y="108" fill="#64748b" fontSize="18" fontWeight="700" fontFamily="sans-serif" textAnchor="middle">3</text>
      <text x="36" y="108" fill="#64748b" fontSize="18" fontWeight="700" fontFamily="sans-serif" textAnchor="middle">9</text>
      <text x="100" y="183" fill="#64748b" fontSize="18" fontWeight="700" fontFamily="sans-serif" textAnchor="middle">6</text>

      {/* Blue infinity lobe (left) */}
      <path
        d="M100,100 C100,65 55,55 30,100 C55,145 100,135 100,100"
        fill="none"
        stroke="url(#ol-blue)"
        strokeWidth="7"
        strokeLinecap="round"
        filter="url(#ol-glow-b)"
      />

      {/* Orange infinity lobe (right) */}
      <path
        d="M100,100 C100,65 145,55 170,100 C145,145 100,135 100,100"
        fill="none"
        stroke="url(#ol-orange)"
        strokeWidth="7"
        strokeLinecap="round"
        filter="url(#ol-glow-o)"
      />

      {/* Arrow going upper-right (orange/amber) */}
      <line x1="115" y1="130" x2="158" y2="42" stroke="url(#ol-arrow)" strokeWidth="8" strokeLinecap="round"/>
      <polyline
        points="140,36 158,42 148,60"
        fill="none"
        stroke="url(#ol-arrow)"
        strokeWidth="7"
        strokeLinecap="round"
        strokeLinejoin="round"
      />

      {/* Clock center dot (blue glow) */}
      <circle cx="100" cy="100" r="7" fill="#38bdf8" filter="url(#ol-glow-b)"/>

      {/* Minute hand (12 o'clock) */}
      <line x1="100" y1="100" x2="100" y2="38" stroke="#38bdf8" strokeWidth="4.5" strokeLinecap="round" filter="url(#ol-glow-b)"/>

      {/* Hour hand (~2 o'clock) */}
      <line x1="100" y1="100" x2="132" y2="74" stroke="#7dd3fc" strokeWidth="3.5" strokeLinecap="round"/>
    </svg>
  );
}
