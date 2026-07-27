package com.devrenno.bookland;

import com.devrenno.bookland.websupport.ProblemDetailErrorController;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the container's error dispatch renders. Unit-level because MockMvc does not run the ERROR
 * dispatch at all — an end-to-end assertion there passes against an empty body and proves nothing.
 */
class ProblemDetailErrorControllerTest {

    private final ProblemDetailErrorController controller = new ProblemDetailErrorController();

    @Test
    @DisplayName("a 500 says INTERNAL_ERROR and names the request that failed, not /error")
    void serverErrorNamesTheOriginalRequest() {
        ProblemDetail problem = handle(500, "/api/v1/auth/register");

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getProperties()).containsEntry("code", "INTERNAL_ERROR");
        assertThat(problem.getInstance()).hasToString("/api/v1/auth/register");
    }

    /** A stack trace, a SQL statement or a column name must never reach the client. */
    @Test
    @DisplayName("a 500 detail says nothing about the cause")
    void serverErrorLeaksNothing() {
        ProblemDetail problem = handle(500, "/api/v1/auth/register");

        assertThat(problem.getDetail()).isEqualTo("The server failed to process the request");
    }

    @Test
    @DisplayName("a 4xx keeps its own meaning")
    void clientErrorsKeepTheirStatusName() {
        assertThat(handle(404, "/api/v1/nope").getProperties()).containsEntry("code", "NOT_FOUND");
        assertThat(handle(405, "/api/v1/books").getProperties())
                .containsEntry("code", "METHOD_NOT_ALLOWED");
    }

    @Test
    @DisplayName("a dispatch with no status recorded is treated as a server error, not as success")
    void missingStatusFallsBackToServerError() {
        ResponseEntity<ProblemDetail> response = controller.handleError(new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ProblemDetail handle(int status, String originalPath) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, originalPath);

        return controller.handleError(request).getBody();
    }
}
