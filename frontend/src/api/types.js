/**
 * API DTO shapes — aligned with spec/api-contract.md §3 and spec/rag-api-contract.md §7.
 * @module api/types
 */

/**
 * @typedef {'OPEN'|'IN_PROGRESS'|'RESOLVED'|'CLOSED'|'CANCELLED'} TicketStatus
 */

/**
 * @typedef {'LOW'|'MEDIUM'|'HIGH'|'CRITICAL'} TicketPriority
 */

/**
 * @typedef {'PAYMENTS'|'SHIPMENT'|'BILLING'|'LOGIN'|'OTHER'} TicketCategory
 */

/**
 * @typedef {Object} TicketSummary
 * @property {string} id
 * @property {string} title
 * @property {TicketStatus} status
 * @property {TicketPriority} priority
 * @property {string|null} assignee
 * @property {TicketCategory|null} category
 * @property {string} createdAt
 * @property {string} updatedAt
 */

/**
 * @typedef {Object} Comment
 * @property {string} id
 * @property {string} body
 * @property {string} createdAt
 */

/**
 * @typedef {TicketSummary & {
 *   description: string,
 *   resolutionNotes: string|null,
 *   comments: Comment[],
 * }} TicketDetail
 */

/**
 * @typedef {Object} ListMeta
 * @property {number} page
 * @property {number} size
 * @property {number} totalElements
 * @property {number} totalPages
 * @property {string} sort
 */

/**
 * @typedef {Object} CreateTicketRequest
 * @property {string} title
 * @property {string} [description]
 * @property {TicketPriority} [priority]
 * @property {string} [assignee]
 * @property {TicketCategory} [category]
 */

/**
 * @typedef {Object} UpdateTicketRequest
 * @property {string} [title]
 * @property {string} [description]
 * @property {TicketPriority} [priority]
 * @property {string} [assignee]
 * @property {TicketCategory} [category]
 * @property {string} [resolutionNotes]
 * @property {TicketStatus} [status]
 */

/**
 * @typedef {Object} CreateCommentRequest
 * @property {string} body
 */

/**
 * @typedef {Object} AskRequest
 * @property {string} question
 */

/**
 * @typedef {Object} AskResponseData
 * @property {string} answer
 * @property {string[]} citedTicketIds
 */

/**
 * @typedef {Object} ApiErrorDetail
 * @property {string} [field]
 * @property {string} [message]
 * @property {string} [code]
 */

/**
 * @typedef {Object} ApiErrorPayload
 * @property {number} status
 * @property {string} code
 * @property {string} message
 * @property {ApiErrorDetail[]} [details]
 * @property {string} [timestamp]
 * @property {string} [path]
 */

export {};
