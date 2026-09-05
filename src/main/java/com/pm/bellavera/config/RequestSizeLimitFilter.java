package com.pm.bellavera.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Caps how large a request body this API will read.
 *
 * <p>Without this there is no limit on a JSON body: a single POST can carry as much text as the
 * client is willing to send, and it lands in a {@code text} column. Bean-validation {@code @Size}
 * constraints bound individual fields, but they are checked only after the whole body has been
 * parsed into memory, so they are not the limit - this is.
 *
 * <p>Two checks, because they catch different things. A declared {@code Content-Length} over the
 * cap is refused outright with a 413 before a byte of body is read. A chunked request declares no
 * length, so the stream is also wrapped in a counter that trips at the same threshold mid-read;
 * that surfaces through the parser as a 400, which is the correct answer to a body that lied about
 * its shape.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestSizeLimitFilter.class);

    private final long maxBytes;

    public RequestSizeLimitFilter(HttpProperties httpProperties) {
        this.maxBytes = httpProperties.maxRequestBytesOrDefault();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        long declared = request.getContentLengthLong();
        if (declared > maxBytes) {
            log.info("Rejected a {} byte body on {} {} (limit {})",
                    declared, request.getMethod(), request.getRequestURI(), maxBytes);
            writeTooLarge(response);
            return;
        }
        filterChain.doFilter(new LimitedBodyRequest(request, maxBytes), response);
    }

    /** Written by hand rather than thrown: the body is refused before any handler is chosen. */
    private void writeTooLarge(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONNECTION, "close");
        response.getWriter().write("""
                {"type":"https://bellavera.app/problems/payload-too-large",\
                "title":"Payload Too Large","status":413,\
                "detail":"The request body is larger than this API accepts."}""");
    }

    /** Trips the same limit on a request that declared no length. */
    private static final class LimitedBodyRequest extends HttpServletRequestWrapper {

        private final long limit;

        private LimitedBodyRequest(HttpServletRequest request, long limit) {
            super(request);
            this.limit = limit;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            ServletInputStream delegate = super.getInputStream();
            return new ServletInputStream() {
                private long read;

                private int count(int bytes) throws IOException {
                    if (bytes > 0) {
                        read += bytes;
                        if (read > limit) {
                            throw new IOException("Request body exceeds the " + limit + " byte limit");
                        }
                    }
                    return bytes;
                }

                @Override
                public int read() throws IOException {
                    int value = delegate.read();
                    count(value < 0 ? -1 : 1);
                    return value;
                }

                @Override
                public int read(byte[] buffer, int off, int len) throws IOException {
                    return count(delegate.read(buffer, off, len));
                }

                @Override
                public boolean isFinished() {
                    return delegate.isFinished();
                }

                @Override
                public boolean isReady() {
                    return delegate.isReady();
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    delegate.setReadListener(readListener);
                }

                @Override
                public int available() throws IOException {
                    return delegate.available();
                }

                @Override
                public void close() throws IOException {
                    delegate.close();
                }
            };
        }

        @Override
        public java.io.BufferedReader getReader() throws IOException {
            InputStream stream = getInputStream();
            return new java.io.BufferedReader(new java.io.InputStreamReader(stream, getCharacterEncoding() == null
                    ? StandardCharsets.UTF_8.name()
                    : getCharacterEncoding()));
        }
    }
}
