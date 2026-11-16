package ru.gameplatform;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import ru.gameplatform.config.AppConfig;
import ru.gameplatform.config.DatabaseConfig;
import ru.gameplatform.controller.AuthController;
import ru.gameplatform.controller.GameController;
import ru.gameplatform.controller.HistoryController;
import ru.gameplatform.controller.ProfileController;
import ru.gameplatform.exception.ApiException;
import ru.gameplatform.repository.GameDao;
import ru.gameplatform.security.JwtFilter;
import ru.gameplatform.security.JwtUtil;
import ru.gameplatform.service.AuthService;
import ru.gameplatform.service.CoinFlipService;
import ru.gameplatform.service.HistoryService;
import ru.gameplatform.service.MinesService;
import ru.gameplatform.service.RouletteService;

import java.util.Map;

public class Main {

    public static void main(String[] args) {

        JwtUtil jwtUtil = new JwtUtil(
                AppConfig.get("jwt.secret"),
                AppConfig.getInt("jwt.expiration.minutes", 1440)
        );

        JwtFilter jwtFilter = new JwtFilter(jwtUtil);

        AuthService authService = new AuthService(jwtUtil);
        AuthController authController = new AuthController(authService);
        ProfileController profileController = new ProfileController(jwtFilter);

        GameController gameController = new GameController(
                jwtFilter,
                new CoinFlipService(),
                new RouletteService(),
                new MinesService()
        );

        HistoryController historyController = new HistoryController(jwtFilter, new HistoryService());

        GameDao gameDao = new GameDao();

        Javalin app = Javalin.create(config -> {

            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
            });

            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        });

        app.get("/api/health", ctx -> ctx.json(Map.of(
                "success", true,
                "message", "Сервер игровой платформы запущен на Java 25",
                "database", isDatabaseAvailable()
        )));

        app.get("/api/games", ctx -> ctx.json(Map.of(
                "success", true,
                "data", gameDao.findAllActive()
        )));

        authController.registerRoutes(app);
        profileController.registerRoutes(app);
        gameController.registerRoutes(app);
        historyController.registerRoutes(app);

        app.after(ctx -> {
            if (ctx.path().startsWith("/api/")) {
                ctx.contentType("application/json; charset=utf-8");
            }
        });

        app.exception(ApiException.class, (e, ctx) ->
                ctx.status(e.getStatus()).json(Map.of(
                        "success", false,
                        "error", e.getMessage()
                )));

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(Map.of(
                    "success", false,
                    "error", "Внутренняя ошибка сервера"
            ));
        });

        app.events(event -> event.serverStopping(() -> DatabaseConfig.close()));

        app.start(8080);
    }

    private static boolean isDatabaseAvailable() {
        try (var conn = DatabaseConfig.getDataSource().getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}