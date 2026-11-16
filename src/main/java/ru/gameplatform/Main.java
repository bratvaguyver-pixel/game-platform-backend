package ru.gameplatform;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.gameplatform.config.SpringConfig;
import ru.gameplatform.controller.AuthController;
import ru.gameplatform.controller.GameController;
import ru.gameplatform.controller.HistoryController;
import ru.gameplatform.controller.ProfileController;
import ru.gameplatform.dao.GameDAO;
import ru.gameplatform.exception.ApiException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(SpringConfig.class);

        AuthController authController = context.getBean(AuthController.class);
        ProfileController profileController = context.getBean(ProfileController.class);
        GameController gameController = context.getBean(GameController.class);
        HistoryController historyController = context.getBean(HistoryController.class);
        GameDAO gameDAO = context.getBean(GameDAO.class);
        DataSource dataSource = context.getBean(DataSource.class);

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
                "message", "Сервер игровой платформы: Java 25 + Spring Core",
                "database", isDatabaseAvailable(dataSource)
        )));

        app.get("/api/games", ctx -> ctx.json(Map.of(
                "success", true,
                "data", gameDAO.findAllActive()
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

        app.events(event -> event.serverStopping(() -> context.close()));

        app.start(8080);
    }

    private static boolean isDatabaseAvailable(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}