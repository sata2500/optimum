interface OptimumLogoProps {
  size?: number;
  className?: string;
}

export default function OptimumLogo({ size = 36, className }: OptimumLogoProps) {
  return (
    <img
      src="/logo.jpg"
      alt="Optimum Logo"
      width={size}
      height={size}
      className={className}
      style={{
        borderRadius: '50%',
        objectFit: 'cover',
        boxShadow: '0 0 12px rgba(56, 189, 248, 0.4)',
        border: '1.5px solid rgba(255, 255, 255, 0.15)',
        display: 'block',
      }}
    />
  );
}
