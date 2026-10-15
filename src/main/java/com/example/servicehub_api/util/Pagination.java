package com.example.servicehub_api.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class Pagination {

    private Pagination() {
    }

    public static Pageable of(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page deve ser >= 0 e size deve estar entre 1 e 50");
        }
        return PageRequest.of(page, size);
    }
}
