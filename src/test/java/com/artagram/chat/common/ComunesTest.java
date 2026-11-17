package com.artagram.chat.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ComunesTest {

    @Test
    void traceIdFilterPropagaOGeneraElTraceId() throws Exception {
        TraceIdFilter filtro = new TraceIdFilter();
        for (String recibido : new String[]{"trace-1", null, " ", "x".repeat(65)}) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            if (recibido != null) req.addHeader(TraceIdFilter.TRACE_ID_HEADER, recibido);
            MockHttpServletResponse res = new MockHttpServletResponse();
            AtomicReference<String> visto = new AtomicReference<>();

            filtro.doFilter(req, res, (a, b) -> visto.set(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)));

            assertThat(res.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo(visto.get());
            assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
            if ("trace-1".equals(recibido)) assertThat(visto.get()).isEqualTo("trace-1");
            else assertThat(visto.get()).hasSize(36);
        }
    }

    @Test
    void apiExceptionFabricas() {
        assertThat(ApiException.badRequest("A", "a").getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ApiException.notFound("B", "b").getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ApiException.forbidden("C", "c").getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ApiException.unauthorized("D", "d").getCode()).isEqualTo("D");
    }
}
