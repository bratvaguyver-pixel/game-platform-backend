package ru.gameplatform.controller;

import io.javalin.http.Context;
import io.javalin.Javalin;
import ru.gameplatform.dto.LoginRequest;
import ru.gameplatform.dto.RegisterRequest;
import ru.gameplatform.service.AuthService;

import java.util.Map;

public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/auth/register", this::register);
        app.post("/api/auth/login", this::login);
    }

    private void register(Context ctx) {
        RegisterRequest req = ctx.bodyAsClass(RegisterRequest.class);
        authService.register(req, ctx.ip());
        ctx.status(201).json(Map.of(
                "success", true,
                "message", "Регистрация успешна. Выполните вход."
        ));
    }

    private void login(Context ctx) {
        LoginRequest req = ctx.bodyAsClass(LoginRequest.class);
        ctx.json(Map.of(
                "success", true,
                "data", authService.login(req, ctx.ip())
        ));
    }
}