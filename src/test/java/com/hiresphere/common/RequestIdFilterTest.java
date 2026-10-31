package com.hiresphere.common;

import com.hiresphere.common.web.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    private String run(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        AtomicReference<String> seenInsideChain = new AtomicReference<>();
        filter.doFilter(request, response, (req, res) -> seenInsideChain.set(MDC.get(RequestIdFilter.MDC_KEY)));
        return seenInsideChain.get();
    }

    @Test
    void shouldGenerateAnIdWhenTheClientSendsNone() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        String insideChain = run(new MockHttpServletRequest(), response);

        assertThat(insideChain).matches("[0-9a-f-]{36}");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo(insideChain);
    }

    @Test
    void shouldKeepAClientSuppliedIdThatLooksSafe() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "frontend-req-12345");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String insideChain = run(request, response);

        assertThat(insideChain).isEqualTo("frontend-req-12345");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("frontend-req-12345");
    }

    @Test
    void shouldReplaceUnsafeOrOddlyShapedIds() throws Exception {
        for (String bad : new String[]{"short", "has space in it 123", "line\r\nbreak-injection", "x".repeat(65)}) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Request-Id", bad);
            MockHttpServletResponse response = new MockHttpServletResponse();

            String insideChain = run(request, response);

            assertThat(insideChain).as(bad).isNotEqualTo(bad).matches("[0-9a-f-]{36}");
        }
    }

    @Test
    void shouldClearTheIdAfterTheRequest() throws Exception {
        run(new MockHttpServletRequest(), new MockHttpServletResponse());

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }
}