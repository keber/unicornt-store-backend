package com.unicornt.store.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behaviour of {@link ProductionApiDocsGuard}: under the {@code prod} profile every OpenAPI and
 * Swagger path answers a JSON {@code 404} without reaching the dispatcher, and every other path
 * travels down the chain untouched. This is the belt-and-suspenders behind the requirement that
 * production ships no API documentation, so the path matching is asserted rather than assumed.
 */
@DisplayName("ProductionApiDocsGuard")
class ProductionApiDocsGuardTest {

    private ProductionApiDocsGuard guard;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        guard = new ProductionApiDocsGuard();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    private void dispatch(String uri) throws Exception {
        request.setRequestURI(uri);
        guard.doFilterInternal(request, response, chain);
    }

    @Nested
    @DisplayName("blocks documentation paths")
    class BlocksDocumentation {

        @ParameterizedTest(name = "{0} is answered with 404")
        @ValueSource(strings = {
                "/v3/api-docs",
                "/v3/api-docs/swagger-config",
                "/swagger-ui",
                "/swagger-ui/index.html",
                "/swagger-ui.html",
                "/api-docs",
                "/api-docs.yaml"
        })
        void answers_not_found_without_calling_the_chain(String uri) throws Exception {
            dispatch(uri);

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
            assertThat(response.getContentType()).isEqualTo("application/json");
            assertThat(chain.getRequest()).as("request must not reach the dispatcher").isNull();
        }

        @Test
        @DisplayName("the 404 body is JSON echoing the requested path")
        void body_is_json_echoing_the_path() throws Exception {
            dispatch("/swagger-ui.html");

            assertThat(response.getContentAsString())
                    .isEqualTo("{\"status\":404,\"error\":\"Not Found\",\"path\":\"/swagger-ui.html\"}");
        }

        @Test
        @DisplayName("setStatus is used, not sendError, so the security chain never sees an /error dispatch")
        void does_not_send_error() throws Exception {
            dispatch("/v3/api-docs");

            assertThat(response.getErrorMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("lets everything else through")
    class LetsEverythingElseThrough {

        @ParameterizedTest(name = "{0} reaches the chain")
        @ValueSource(strings = {
                "/api/v1/products",
                "/api/v1/auth/login",
                "/",
                "/actuator/health",
                "/apidocs",
                "/swagger"
        })
        void passes_the_request_down_the_chain(String uri) throws Exception {
            dispatch(uri);

            assertThat(chain.getRequest()).isSameAs(request);
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(response.getContentAsString()).isEmpty();
        }
    }
}
