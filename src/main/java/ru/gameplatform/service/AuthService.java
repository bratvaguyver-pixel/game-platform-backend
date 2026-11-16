package ru.gameplatform.service;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gameplatform.dao.AuditLogDAO;
import ru.gameplatform.dao.TransactionDAO;
import ru.gameplatform.dao.UserDAO;
import ru.gameplatform.dao.RoleDAO;
import ru.gameplatform.dto.AuthResponse;
import ru.gameplatform.dto.LoginRequest;
import ru.gameplatform.dto.RegisterRequest;
import ru.gameplatform.exception.ApiException;
import ru.gameplatform.model.User;
import ru.gameplatform.security.JwtUtil;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class AuthService {

    private static final BigDecimal REGISTRATION_BONUS = new BigDecimal("1000.00");

    private final UserDAO userDAO;
    private final RoleDAO userRoleDAO;
    private final TransactionDAO transactionDAO;
    private final AuditLogDAO auditLogDAO;
    private final JwtUtil jwtUtil;

    public AuthService(UserDAO userDAO,
                       RoleDAO userRoleDAO,
                       TransactionDAO transactionDAO,
                       AuditLogDAO auditLogDAO,
                       JwtUtil jwtUtil) {
        this.userDAO = userDAO;
        this.userRoleDAO = userRoleDAO;
        this.transactionDAO = transactionDAO;
        this.auditLogDAO = auditLogDAO;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public void register(RegisterRequest req, String ip) {
        if (req == null || req.username() == null || req.email() == null || req.password() == null) {
            throw new ApiException(400, "Все поля обязательны: username, email, password");
        }

        String username = req.username().trim();
        String email = req.email().trim().toLowerCase();
        String password = req.password();

        if (username.length() < 3 || username.length() > 50) {
            throw new ApiException(400, "Имя пользователя должно быть от 3 до 50 символов");
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.+-]+$")) {
            throw new ApiException(400, "Некорректный формат email");
        }
        if (password.length() < 6) {
            throw new ApiException(400, "Пароль должен быть не короче 6 символов");
        }

        if (userDAO.existsByUsername(username)) {
            throw new ApiException(409, "Имя пользователя уже занято");
        }
        if (userDAO.findByEmail(email).isPresent()) {
            throw new ApiException(409, "Пользователь с таким email уже зарегистрирован");
        }

        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(10));

        int id = userDAO.insert(username, email, passwordHash, REGISTRATION_BONUS);
        userRoleDAO.assign(id, "PLAYER");
        transactionDAO.insert(id, REGISTRATION_BONUS, "REGISTRATION_BONUS",
                "Стартовый бонус при регистрации");

        auditLogDAO.log("REGISTER", id, "{\"email\":\"" + email + "\"}", ip);
    }

    public AuthResponse login(LoginRequest req, String ip) {
        if (req == null || req.email() == null || req.email().isBlank()
                || req.password() == null || req.password().isBlank()) {
            throw new ApiException(400, "Email и пароль обязательны");
        }

        String email = req.email().trim().toLowerCase();
        Optional<User> found = userDAO.findByEmail(email);

        if (found.isEmpty()) {
            auditLogDAO.log("LOGIN_FAILED", null, "{\"email\":\"" + email + "\"}", ip);
            throw new ApiException(401, "Неверный email или пароль");
        }

        User user = found.get();

        if (!BCrypt.checkpw(req.password(), user.passwordHash())) {
            auditLogDAO.log("LOGIN_FAILED", user.id(), null, ip);
            throw new ApiException(401, "Неверный email или пароль");
        }

        if (!user.active()) {
            throw new ApiException(403, "Учётная запись деактивирована");
        }

        auditLogDAO.log("LOGIN", user.id(), null, ip);

        String token = jwtUtil.generateToken(user.id(), user.username(), user.role());
        return new AuthResponse(token, user.username(), user.role(), user.balance());
    }
}