package com.ticketmanagement.api.common;

import java.util.List;

public record PageResponse<T>(List<T> data, PageMeta meta) {
}
