package ru.gameplatform.controller;

import io.javalin.Javalin;
import io.javalin.http.Context;
import ru.gameplatform.security.JwtFilter;
import ru.gameplatform.service.HistoryService;

import java.util.Map;

public class HistoryController {

    private final JwtFilter jwtFilter;
    private final HistoryService historyService;

    public HistoryController(JwtFilter jwtFilter, HistoryService historyService) {
        this.jwtFilter = jwtFilter;
        this.historyService = historyService;
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/history/sessions", jwtFilter.protect(this::sessions));
        app.get("/api/history/transactions", jwtFilter.protect(this::transactions));
    }

    private void sessions(Context ctx) {
        int userId = ctx.attribute("userId");
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(0);
        int size = ctx.queryParamAsClass("size", Integer.class).getOrDefault(10);
        String game = ctx.queryParam("game");
        String result = ctx.queryParam("result");

        ctx.json(Map.of("success", true,
                "data", historyService.sessions(userId, game, result, page, size)));
    }

    private void transactions(Context ctx) {
        int userId = ctx.attribute("userId");
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(0);
        int size = ctx.queryParamAsClass("size", Integer.class).getOrDefault(10);
        String type = ctx.queryParam("type");
        String from = ctx.queryParam("from");
        String to = ctx.queryParam("to");

        ctx.json(Map.of("success", true,
                "data", historyService.transactions(userId, type, from, to, page, size)));
    }
}