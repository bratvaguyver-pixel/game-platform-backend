package ru.gameplatform.controller;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.springframework.stereotype.Component;
import ru.gameplatform.dao.UserDAO;
import ru.gameplatform.exception.ApiException;
import ru.gameplatform.model.User;
import ru.gameplatform.security.JwtFilter;

import java.util.Map;

@Component
public class ProfileController {

    private final UserDAO userDAO;
    private final JwtFilter jwtFilter;

    public ProfileController(UserDAO userDAO, JwtFilter jwtFilter) {
        this.userDAO = userDAO;
        this.jwtFilter = jwtFilter;
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/profile", jwtFilter.protect(this::profile));
    }

    private void profile(Context ctx) {
        int userId = ctx.attribute("userId");

        User user = userDAO.findById(userId)
                .orElseThrow(() -> new ApiException(404, "Пользователь не найден"));

        ctx.json(Map.of(
                "success", true,
                "data", Map.of(
                        "id", user.id(),
                        "username", user.username(),
                        "email", user.email(),
                        "role", user.role(),
                        "balance", user.balance()
                )
        ));
    }
}