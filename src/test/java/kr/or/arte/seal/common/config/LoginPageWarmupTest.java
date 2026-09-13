package kr.or.arte.seal.common.config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class LoginPageWarmupTest {
    @Test void fetchesOnlyLoginWithoutCredentialsOrSessionReuse() throws Exception {
        var calls = new AtomicInteger();
        var observed = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/seal/login", exchange -> {
            calls.incrementAndGet();
            observed.set(exchange.getRequestMethod() + " " + exchange.getRequestURI()
                    + " cookie=" + exchange.getRequestHeaders().getFirst("Cookie")
                    + " auth=" + exchange.getRequestHeaders().getFirst("Authorization")
                    + " bytes=" + exchange.getRequestBody().readAllBytes().length);
            var body = "<form><input name=\"username\"><input name=\"_csrf\"></form>".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Set-Cookie", "SEALSESSION=warmup-only; HttpOnly");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);exchange.close();
        });
        server.start();
        try {
            LoginPageWarmup.fetchLoginPage(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/seal/login"));
            assertThat(calls.get()).isEqualTo(1);
            assertThat(observed.get()).isEqualTo("GET /seal/login cookie=null auth=null bytes=0");
        } finally { server.stop(0); }
    }

    @Test void rejectsRedirectsErrorsAndNonLoginContent() throws Exception {
        for (int status : new int[]{302, 500, 200}) {
            var redirectedCalls = new AtomicInteger();
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/login", exchange -> {
                exchange.getResponseHeaders().add("Location", "/unexpected");
                byte[] body = "not a login page".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, body.length);
                exchange.getResponseBody().write(body);exchange.close();
            });
            server.createContext("/unexpected", exchange -> { redirectedCalls.incrementAndGet(); exchange.sendResponseHeaders(204, -1); exchange.close(); });
            server.start();
            try {
                URI uri=URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/login");
                assertThatThrownBy(() -> LoginPageWarmup.fetchLoginPage(uri)).isInstanceOf(IllegalStateException.class);
                assertThat(redirectedCalls.get()).isZero();
            } finally { server.stop(0); }
        }
    }
}
