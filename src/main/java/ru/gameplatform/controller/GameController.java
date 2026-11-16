package ru.gameplatform.controller;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.springframework.stereotype.Component;
import ru.gameplatform.dto.PlayCoinFlipRequest;
import ru.gameplatform.dto.PlayMinesRequest;
import ru.gameplatform.dto.PlayRouletteRequest;
import ru.gameplatform.security.JwtFilter;
import ru.gameplatform.service.CoinFlipService;
import ru.gameplatform.service.MinesService;
import ru.gameplatform.service.RouletteService;

import java.util.Map;

@Component
public class GameController {

    private final JwtFilter jwtFilter;
    private final CoinFlipService coinFlipService;
    private final RouletteService rouletteService;
    private final MinesService minesService;

    public GameController(JwtFilter jwtFilter,
                          CoinFlipService coinFlipService,
                          RouletteService rouletteService,
                          MinesService minesService) {
        this.jwtFilter = jwtFilter;
        this.coinFlipService = coinFlipService;
        this.rouletteService = rouletteService;
        this.minesService = minesService;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/games/coinflip/play", jwtFilter.protect(this::playCoinFlip));
        app.post("/api/games/roulette/play", jwtFilter.protect(this::playRoulette));
        app.post("/api/games/mines/play", jwtFilter.protect(this::playMines));
    }

    private void playCoinFlip(Context ctx) {
        int userId = ctx.attribute("userId");
        PlayCoinFlipRequest req = ctx.bodyAsClass(PlayCoinFlipRequest.class);
        ctx.json(Map.of("success", true, "data", coinFlipService.play(userId, req)));
    }

    private void playRoulette(Context ctx) {
        int userId = ctx.attribute("userId");
        PlayRouletteRequest req = ctx.bodyAsClass(PlayRouletteRequest.class);
        ctx.json(Map.of("success", true, "data", rouletteService.play(userId, req)));
    }

    private void playMines(Context ctx) {
        int userId = ctx.attribute("userId");
        PlayMinesRequest req = ctx.bodyAsClass(PlayMinesRequest.class);
        ctx.json(Map.of("success", true, "data", minesService.play(userId, req)));
    }
}