package ru.gameplatform.controller;

import io.javalin.Javalin;
import io.javalin.http.Context;
import ru.gameplatform.exception.ApiException;
import ru.gameplatform.model.User;
import ru.gameplatform.repository.UserDao;
import ru.gameplatform.security.JwtFilter;

import java.util.Map;

public class ProfileController {

    private final UserDao userDao = new UserDao();
    private final JwtFilter jwtFilter;

    public ProfileController(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/profile", jwtFilter.protect(this::profile));
    }

    private void profile(Context ctx) {
        int userId = ctx.attribute("userId");

        User user = userDao.findById(userId)
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