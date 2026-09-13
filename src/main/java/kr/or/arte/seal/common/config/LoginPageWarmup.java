package kr.or.arte.seal.common.config;

import java.net.*;
import java.net.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Warm the public login GET before Spring Boot announces ACCEPTING_TRAFFIC. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "seal.login-warmup.enabled", havingValue = "true", matchIfMissing = true)
public class LoginPageWarmup implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LoginPageWarmup.class);
    private final ApplicationContext context;

    public LoginPageWarmup(ApplicationContext context) {
        this.context = context;
    }

    @Override public void run(ApplicationArguments args) throws Exception {
        if (!(context instanceof ServletWebServerApplicationContext web) || web.getWebServer() == null) {
            log.warn("Login warm-up skipped: external container must perform deployment-time warm-up");
            return;
        }
        var env = context.getEnvironment();
        if (env.getProperty("server.ssl.enabled", Boolean.class, false)) {
            throw new IllegalStateException("Embedded HTTPS requires deployment-time warm-up; set seal.login-warmup.enabled=false");
        }
        String host = env.getProperty("server.address", "127.0.0.1");
        if (InetAddress.getByName(host).isAnyLocalAddress()) host = "127.0.0.1";
        URI uri = new URI("http", null, host, web.getWebServer().getPort(),
                web.getServletContext().getContextPath() + "/login", null, null);
        long start = System.nanoTime();
        fetchLoginPage(uri);
        log.info("Login page warm-up completed in {} ms (anonymous GET only)", (System.nanoTime() - start) / 1_000_000);
    }

    static void fetchLoginPage(URI uri) throws IOException, InterruptedException {
        // No cookies/authentication, no redirect following, and no outbound proxy.
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER).proxy(new ProxySelector() {
                    @Override public List<Proxy> select(URI target) { return List.of(Proxy.NO_PROXY); }
                    @Override public void connectFailed(URI target, SocketAddress address, IOException error) { }
                }).build();
        var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200 || !response.body().contains("name=\"username\"")
                || !response.body().contains("name=\"_csrf\"")) {
            throw new IllegalStateException("Login warm-up failed: expected login form, HTTP " + response.statusCode());
        }
    }
}
