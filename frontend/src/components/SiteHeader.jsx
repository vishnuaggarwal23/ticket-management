'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';

/**
 * @param {string} href
 * @param {string} pathname
 * @returns {boolean}
 */
function isNavActive(href, pathname) {
  if (href === '/tickets') {
    return pathname === '/tickets' || pathname.startsWith('/tickets/');
  }
  return pathname === href || pathname.startsWith(`${href}/`);
}

function showNewTicketCta(pathname) {
  return !pathname.startsWith('/tickets/new');
}

/**
 * Global header with primary navigation (DEC-20).
 */
export default function SiteHeader() {
  const pathname = usePathname() ?? '';

  return (
    <header className="site-header">
      <a href="#main-content" className="skip-link">
        Skip to content
      </a>
      <div className="site-header__inner">
        <Link href="/tickets" className="site-header__brand">
          Ticket Management
        </Link>
        <div className="site-header__actions">
          <nav className="site-nav" aria-label="Main">
            <Link
              href="/tickets"
              className={`site-nav__link${isNavActive('/tickets', pathname) ? ' site-nav__link--active' : ''}`}
              aria-current={isNavActive('/tickets', pathname) ? 'page' : undefined}
            >
              Tickets
            </Link>
            <Link
              href="/ask"
              className={`site-nav__link${isNavActive('/ask', pathname) ? ' site-nav__link--active' : ''}`}
              aria-current={isNavActive('/ask', pathname) ? 'page' : undefined}
            >
              Ask
            </Link>
          </nav>
          {showNewTicketCta(pathname) ? (
            <Link href="/tickets/new" className="button button--compact site-header__cta">
              New ticket
            </Link>
          ) : null}
        </div>
      </div>
    </header>
  );
}
