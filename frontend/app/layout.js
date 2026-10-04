import './globals.css';

export const metadata = {
  title: 'Ticket Management',
  description: 'Support ticket management and grounded Q&A',
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
