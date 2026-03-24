package ru.iposhka.gateway.filter;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest.Builder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtClaimsToHeadersGlobalFilter implements GlobalFilter {
    private static final String AUTH_PATH_PREFIX = "/api/auth/";

    private final ReactiveJwtDecoder jwtDecoder;

    public static final String X_AUTH_USER_ID_HEADER = "X-Auth-User-Id";
    public static final String X_AUTH_EMAIL_HEADER = "X-Auth-Email";
    public static final String X_AUTH_GENDER_HEADER = "X-Auth-Gender";
    public static final String X_AUTH_NAME_HEADER = "X-Auth-Name";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith(AUTH_PATH_PREFIX)) {
            return chain.filter(exchange);
        }

        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || auth.isBlank() || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return unauthorized(exchange);
        }

        String token = auth.substring(7).trim();

        return jwtDecoder.decode(token)
                .map(Jwt::getClaims)
                .flatMap(claims -> {
                    Builder req = exchange.getRequest().mutate();
                    enrichHeadersFromJwtClaims(claims, req);
                    return chain.filter(exchange.mutate().request(req.build()).build());
                })
                .onErrorResume(e -> unauthorized(exchange));
    }

    private void enrichHeadersFromJwtClaims(Map<String, Object> claims, Builder req) {
        Object userId = claims.get("user_id");
        if (userId != null) {
            req.header(X_AUTH_USER_ID_HEADER, String.valueOf(userId));
        }
        String email = getStringClaim(claims, "email");
        if (email != null) {
            req.header(X_AUTH_EMAIL_HEADER, email);
        }
        String name = getStringClaim(claims, "name");
        if (name != null) {
            req.header(X_AUTH_NAME_HEADER, name);
        }
        String gender = getStringClaim(claims, "gender");
        if (gender != null) {
            req.header(X_AUTH_GENDER_HEADER, gender);
        }
    }

    private String getStringClaim(Map<String, Object> claims, String claimName) {
        Object value = claims.get(claimName);
        return value != null ? String.valueOf(value) : null;
    }

    private static Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
