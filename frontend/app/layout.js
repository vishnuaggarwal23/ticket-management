import { Inter } from 'next/font/google';
import SiteHeader from '@/components/SiteHeader';
import './globals.css';

const inter = Inter({
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-inter',
});

export const metadata = {
  title: 'Ticket Management',
  description: 'Support ticket management and grounded Q&A',
};

export default function RootLayout({ children }) {
  return (
    <html lang="en" className={inter.variable}>
      <body>
        <SiteHeader />
        <main id="main-content">{children}</main>
      </body>
    </html>
  );
}
