import './globals.css';
import type { Metadata } from 'next';
import Script from 'next/script';
import { Inter } from 'next/font/google';

const inter = Inter({
  subsets: ['latin'],
  variable: '--font-inter',
  display: 'swap',
});

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
    <html lang="tr" className={inter.variable}>
      <head>
        <Script
          src="https://accounts.google.com/gsi/client"
          strategy="afterInteractive"
        />
      </head>
      <body className="antialiased font-sans text-slate-900 bg-slate-50 selection:bg-indigo-500 selection:text-white">
        {children}
      </body>
    </html>
  );
}
