package com.example.servicehub_api.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(OffsetDateTime timestamp, int status, String erro, List<String> mensagens) {
}
