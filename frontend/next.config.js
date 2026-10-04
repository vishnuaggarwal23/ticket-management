/** @type {import('next').NextConfig} */
const backendRewriteBase =
  process.env.BACKEND_REWRITE_URL?.replace(/\/$/, '') || 'http://127.0.0.1:8080';

const nextConfig = {
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${backendRewriteBase}/api/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;
