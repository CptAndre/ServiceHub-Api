package com.example.servicehub_api.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.servicehub_api.config.RequestIdFilter;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerWebTest {
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new ErrorController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @CsvSource({"integrity,409", "concurrent,409", "denied,403", "generic,500", "status,404", "upload,413"})
    void springInvokesTheAnnotatedHandlers(String kind, int expectedStatus) throws Exception {
        mvc.perform(get("/handler/errors/{kind}", kind))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(content().string(not(containsString("sensitive-test-detail"))));
    }

    @Test
    void unsupportedMethodPreservesTheAllowHeader() throws Exception {
        mvc.perform(put("/handler/input"))
                .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.status").value(405))
                .andExpect(header().string("Allow", containsString("POST")));
    }

    @Test
    void malformedJsonUsesTheHandlerWithoutExceptionParameter() throws Exception {
        mvc.perform(post("/handler/input").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void invalidPathParameterKeepsTheStandardResponse() throws Exception {
        mvc.perform(get("/handler/id/not-a-number"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unsupportedContentTypeReturns415InsteadOf500() throws Exception {
        mvc.perform(post("/handler/input").contentType(MediaType.TEXT_PLAIN).content("plain text"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void unacceptableResponseFormatKeeps406AndTheJsonErrorContract() throws Exception {
        mvc.perform(get("/handler/output").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable()).andExpect(jsonPath("$.status").value(406))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void missingRouteReturns404InsteadOf500() throws Exception {
        mvc.perform(get("/handler/does-not-exist"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void missingQueryParameterReturns400InsteadOf500() throws Exception {
        mvc.perform(get("/handler/required"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void beanValidationKeepsFieldMessages() throws Exception {
        mvc.perform(post("/handler/valid").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagens[0]", containsString("name")));
    }

    @ParameterizedTest
    @CsvSource({"400", "401", "403", "404", "409", "422", "429", "503", "599"})
    void declaredHttpStatusesRemainIntactWithoutLeakingServerErrors(int code) throws Exception {
        mvc.perform(get("/handler/status/{code}", code))
                .andExpect(status().is(code)).andExpect(jsonPath("$.status").value(code))
                .andExpect(content().string(not(containsString("sensitive-test-detail"))));
    }

    @Test
    void frameworkErrorPreservesRetryAfter() throws Exception {
        mvc.perform(get("/handler/retry"))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.status").value(429))
                .andExpect(header().string("Retry-After", "10"));
    }

    @Test
    void unexpectedFailureIsCorrelatedWithoutLoggingTheExceptionMessage() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        var logs = new ListAppender<ILoggingEvent>() {
            @Override protected void append(ILoggingEvent event) {
                event.prepareForDeferredProcessing();
                super.append(event);
            }
        };
        logs.start();
        logger.addAppender(logs);
        try {
            var filteredMvc = MockMvcBuilders.standaloneSetup(new ErrorController())
                    .setControllerAdvice(new GlobalExceptionHandler()).addFilters(new RequestIdFilter()).build();
            filteredMvc.perform(get("/handler/errors/generic").header(RequestIdFilter.HEADER, "failure-correlation"))
                    .andExpect(status().isInternalServerError()).andExpect(header().string(RequestIdFilter.HEADER, "failure-correlation"));
            assertEquals(1, logs.list.size());
            var event = logs.list.getFirst();
            assertEquals("failure-correlation", event.getMDCPropertyMap().get(RequestIdFilter.MDC_KEY));
            assertTrue(event.getFormattedMessage().contains("IllegalStateException"));
            assertFalse(event.getFormattedMessage().contains("sensitive-test-detail"));
            assertNull(event.getThrowableProxy());
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }
    }

    @RestController
    static class ErrorController {
        @GetMapping("/handler/errors/{kind}")
        void error(@PathVariable String kind) {
            switch (kind) {
                case "integrity" -> throw new DataIntegrityViolationException("sensitive-test-detail");
                case "concurrent" -> throw new ConcurrencyFailureException("sensitive-test-detail");
                case "denied" -> throw new AccessDeniedException("sensitive-test-detail");
                case "status" -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso inexistente");
                case "upload" -> throw new MaxUploadSizeExceededException(1024);
                default -> throw new IllegalStateException("sensitive-test-detail");
            }
        }

        @PostMapping("/handler/input")
        Map<String, String> input(@RequestBody Map<String, String> body) {
            return body;
        }

        @GetMapping("/handler/id/{id}")
        long id(@PathVariable long id) {
            return id;
        }

        @GetMapping(value = "/handler/output", produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String, String> output() {
            return Map.of("value", "ok");
        }

        @GetMapping("/handler/required")
        String required(@RequestParam String id) {
            return id;
        }

        @PostMapping("/handler/valid")
        ValidInput valid(@Valid @RequestBody ValidInput input) {
            return input;
        }

        @GetMapping("/handler/status/{code}")
        void declaredStatus(@PathVariable int code) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(code),
                    code >= 500 ? "sensitive-test-detail" : "Mensagem pública");
        }

        @GetMapping("/handler/retry")
        void retry() {
            var error = new ErrorResponseException(HttpStatus.TOO_MANY_REQUESTS);
            error.getHeaders().set("Retry-After", "10");
            throw error;
        }
    }

    record ValidInput(@NotBlank String name) {}
}
