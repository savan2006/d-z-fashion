import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'D & Z Fashion | Thoughtful Everyday Style',
  description: 'Discover thoughtfully selected women’s and girls’ fashion from D & Z Fashion.',
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en-IN">
      <body>{children}</body>
    </html>
  );
}
