import Link from 'next/link';

const navLinkClass = 'site-nav__link';

/**
 * Global header with primary navigation (DEC-20).
 */
export default function SiteHeader() {
  return (
    <header className="site-header">
      <div className="site-header__inner">
        <Link href="/tickets" className="site-header__brand">
          Ticket Management
        </Link>
        <nav className="site-nav" aria-label="Main">
          <Link href="/tickets" className={navLinkClass}>
            Tickets
          </Link>
          <Link href="/ask" className={navLinkClass}>
            Ask
          </Link>
        </nav>
      </div>
    </header>
  );
}
