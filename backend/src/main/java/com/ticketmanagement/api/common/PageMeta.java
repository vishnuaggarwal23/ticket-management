package com.ticketmanagement.api.common;

public record PageMeta(int page, int size, long totalElements, int totalPages, String sort) {
}
