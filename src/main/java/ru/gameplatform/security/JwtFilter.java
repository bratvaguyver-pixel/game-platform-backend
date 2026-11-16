package ru.gameplatform.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.springframework.stereotype.Component;
import ru.gameplatform.exception.ApiException;

@Component
public class JwtFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public void handle(Context ctx) {
        String header = ctx.header("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            throw new ApiException(401, "Отсутствует токен авторизации");
        }

        String token = header.substring("Bearer ".length());

        try {
            DecodedJWT jwt = jwtUtil.verify(token);
            ctx.attribute("userId", Integer.parseInt(jwt.getSubject()));
            ctx.attribute("username", jwt.getClaim("username").asString());
            ctx.attribute("role", jwt.getClaim("role").asString());
        } catch (JWTVerificationException e) {
            throw new ApiException(401, "Токен недействителен или истёк");
        }
    }

    public Handler protect(Handler handler) {
        return ctx -> {
            handle(ctx);
            handler.handle(ctx);
        };
    }
}