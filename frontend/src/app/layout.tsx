import type { Metadata, Viewport } from 'next';
import './globals.css';
import { AuthProvider } from '@/lib/auth/AuthContext';
import { ToastProvider } from '@/components/ui/Toast';

export const metadata: Metadata = {
  title: 'D & Z Fashion | Thoughtful Everyday Style',
  description:
    'Discover thoughtfully selected women’s and girls’ fashion from D & Z Fashion. Crafted in Surat, Gujarat with authentic quality and pan-India delivery.',
  icons: {
    icon: '/brand/dz-monogram.svg',
  },
};

export const viewport: Viewport = {
  themeColor: '#6d2836',
  width: 'device-width',
  initialScale: 1,
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en-IN">
      <body className="font-sans">
        <AuthProvider>
          <ToastProvider>{children}</ToastProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
