import './globals.css';
import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Optimum Web Dashboard | Çizelge & Analiz',
  description: 'Google hesabı ile senkronize edilen Optimum Android zaman yönetimi ve analitik web platformu.',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="tr" className="dark">
      <body className="antialiased selection:bg-indigo-500 selection:text-white">
        {children}
      </body>
    </html>
  );
}
