/**
 * Grounded ask API.
 * @module api/ask
 */

import { requestJson, unwrapData } from './client.js';

/**
 * @typedef {import('./types.js').AskResponseData} AskResponseData
 */

const ASK_PATH = '/api/v1/ai/ask';

/**
 * @param {string} question
 * @returns {Promise<AskResponseData>}
 */
export async function askQuestion(question) {
  const body = await requestJson(ASK_PATH, {
    method: 'POST',
    body: { question },
  });
  return /** @type {AskResponseData} */ (unwrapData(body));
}
