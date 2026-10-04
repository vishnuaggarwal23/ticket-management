import SiteHeader from '@/components/SiteHeader';
import './globals.css';

export const metadata = {
  title: 'Ticket Management',
  description: 'Support ticket management and grounded Q&A',
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>
        <SiteHeader />
        <main id="main-content">{children}</main>
      </body>
    </html>
  );
}
