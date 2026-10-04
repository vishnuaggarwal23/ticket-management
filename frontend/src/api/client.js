/**
 * HTTP client — envelopes, base URL, and ApiError.
 * @module api/client
 */

/**
 * @typedef {import('./types.js').ListMeta} ListMeta
 */

/**
 * Thrown when the API returns a non-2xx response with an error envelope.
 */
export class ApiError extends Error {
  /**
   * @param {Object} params
   * @param {string} params.message
   * @param {number} params.status
   * @param {string} params.code
   * @param {import('./types.js').ApiErrorDetail[]} [params.details]
   * @param {string} [params.path]
   */
  constructor({ message, status, code, details = [], path }) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.details = details;
    this.path = path;
  }
}

/**
 * Base URL for ticket/ask API calls. Empty string uses same-origin paths (Next rewrites or Vite proxy).
 * @returns {string}
 */
export function getBaseUrl() {
  if (typeof import.meta !== 'undefined' && import.meta.env) {
    const vite = import.meta.env.VITE_API_BASE_URL;
    if (vite !== undefined && vite !== '') {
      return vite;
    }
  }
  if (typeof process !== 'undefined' && process.env.NEXT_PUBLIC_API_BASE_URL) {
    return process.env.NEXT_PUBLIC_API_BASE_URL;
  }
  return '';
}

/**
 * Origin used for fetch when no explicit API base is configured.
 * Browser: relative paths (Next rewrites). SSR: loop back through the Next server.
 * @returns {string}
 */
function getFetchBaseUrl() {
  const configured = getBaseUrl();
  if (configured) {
    return configured;
  }
  if (typeof window !== 'undefined') {
    return '';
  }
  const port = process.env.PORT || '3000';
  const host = process.env.HOSTNAME || '127.0.0.1';
  return `http://${host}:${port}`;
}

/**
 * @param {string} path
 * @returns {string}
 */
function resolveUrl(path) {
  const base = getFetchBaseUrl();
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  if (!base) {
    return normalizedPath;
  }
  const normalizedBase = base.endsWith('/') ? base.slice(0, -1) : base;
  return `${normalizedBase}${normalizedPath}`;
}

/**
 * @param {unknown} body
 * @returns {unknown}
 */
export function unwrapData(body) {
  if (body === null || typeof body !== 'object' || !('data' in body)) {
    throw new Error('Invalid API response: missing data envelope');
  }
  return body.data;
}

/**
 * @param {unknown} body
 * @returns {{ data: unknown[], meta: ListMeta }}
 */
export function unwrapListEnvelope(body) {
  const data = unwrapData(body);
  if (!Array.isArray(data)) {
    throw new Error('Invalid API response: list data must be an array');
  }
  if (body === null || typeof body !== 'object' || !('meta' in body) || body.meta == null) {
    throw new Error('Invalid API response: list missing meta');
  }
  return { data, meta: body.meta };
}

/**
 * @param {string} path
 * @param {RequestInit & { body?: unknown }} [options]
 * @returns {Promise<unknown>}
 */
export async function requestJson(path, options = {}) {
  const { method = 'GET', body, headers: extraHeaders, ...rest } = options;
  const headers = {
    Accept: 'application/json',
    ...extraHeaders,
  };
  let payload;
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }

  const res = await fetch(resolveUrl(path), {
    method,
    headers,
    body: payload,
    ...rest,
  });

  const text = await res.text();
  /** @type {unknown} */
  let parsed = null;
  if (text) {
    try {
      parsed = JSON.parse(text);
    } catch {
      if (!res.ok) {
        throw new ApiError({
          message: res.statusText || 'Request failed',
          status: res.status,
          code: 'BAD_RESPONSE',
          details: [],
        });
      }
      throw new Error('Invalid JSON in API response');
    }
  }

  if (!res.ok) {
    const err =
      parsed !== null && typeof parsed === 'object' && 'error' in parsed
        ? parsed.error
        : null;
    throw new ApiError({
      message:
        err && typeof err === 'object' && 'message' in err && err.message
          ? String(err.message)
          : res.statusText || 'Request failed',
      status:
        err && typeof err === 'object' && 'status' in err && typeof err.status === 'number'
          ? err.status
          : res.status,
      code:
        err && typeof err === 'object' && 'code' in err && err.code
          ? String(err.code)
          : 'UNKNOWN',
      details:
        err && typeof err === 'object' && Array.isArray(err.details) ? err.details : [],
      path:
        err && typeof err === 'object' && 'path' in err && err.path
          ? String(err.path)
          : undefined,
    });
  }

  return parsed;
}
