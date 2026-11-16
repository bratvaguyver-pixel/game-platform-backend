package ru.gameplatform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gameplatform.dao.GameDAO;
import ru.gameplatform.dao.GameSessionDAO;
import ru.gameplatform.dao.GameSettingsDAO;
import ru.gameplatform.dao.TransactionDAO;
import ru.gameplatform.dao.UserDAO;
import ru.gameplatform.dto.GameRoundResponse;
import ru.gameplatform.dto.PlayMinesRequest;
import ru.gameplatform.exception.ApiException;
import ru.gameplatform.model.Game;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Service
public class MinesService {

    private static final String GAME_TYPE = "MINES";

    private final Random random = new SecureRandom();
    private final UserDAO userDAO;
    private final GameDAO gameDAO;
    private final GameSettingsDAO gameSettingsDAO;
    private final GameSessionDAO gameSessionDAO;
    private final TransactionDAO transactionDAO;

    public MinesService(UserDAO userDAO,
                        GameDAO gameDAO,
                        GameSettingsDAO gameSettingsDAO,
                        GameSessionDAO gameSessionDAO,
                        TransactionDAO transactionDAO) {
        this.userDAO = userDAO;
        this.gameDAO = gameDAO;
        this.gameSettingsDAO = gameSettingsDAO;
        this.gameSessionDAO = gameSessionDAO;
        this.transactionDAO = transactionDAO;
    }

    @Transactional
    public GameRoundResponse play(int userId, PlayMinesRequest req) {

        if (req == null || req.betAmount() == null || req.picks() == null || req.picks().isEmpty()) {
            throw new ApiException(400, "Необходимо указать betAmount и picks (номера клеток)");
        }

        Game game = gameDAO.findByType(GAME_TYPE)
                .orElseThrow(() -> new ApiException(404, "Игра не найдена или отключена"));

        BigDecimal bet = req.betAmount();
        if (bet.compareTo(game.minBet()) < 0 || bet.compareTo(game.maxBet()) > 0) {
            throw new ApiException(400,
                    "Ставка должна быть от " + game.minBet() + " до " + game.maxBet());
        }

        int cells = Integer.parseInt(gameSettingsDAO
                .findValue(game.id(), "grid_size").orElse("9"));
        int mines = Integer.parseInt(gameSettingsDAO
                .findValue(game.id(), "mine_count").orElse("3"));

        List<Integer> picks = req.picks().stream().distinct().toList();
        if (picks.size() != req.picks().size()) {
            throw new ApiException(400, "Номера клеток не должны повторяться");
        }
        for (int p : picks) {
            if (p < 0 || p >= cells) {
                throw new ApiException(400, "Номер клетки должен быть от 0 до " + (cells - 1));
            }
        }
        if (picks.size() > cells - mines) {
            throw new ApiException(400, "Нельзя открыть больше " + (cells - mines) + " клеток");
        }

        if (!userDAO.decreaseBalance(userId, bet)) {
            throw new ApiException(400, "Недостаточно средств на балансе");
        }
        transactionDAO.insert(userId, bet, "BET", "Ставка в игре " + game.name());

        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < cells; i++) {
            all.add(i);
        }
        Collections.shuffle(all, random);
        Set<Integer> mineSet = new HashSet<>(all.subList(0, mines));

        boolean win = picks.stream().noneMatch(mineSet::contains);

        BigDecimal winAmount = BigDecimal.ZERO;
        if (win) {
            BigDecimal multiplier = BigDecimal.ONE;
            for (int i = 0; i < picks.size(); i++) {
                multiplier = multiplier
                        .multiply(BigDecimal.valueOf(cells - i))
                        .divide(BigDecimal.valueOf(cells - mines - i), 6, RoundingMode.HALF_UP);
            }
            multiplier = multiplier.setScale(2, RoundingMode.HALF_UP);
            if (multiplier.compareTo(BigDecimal.ONE) < 0) {
                multiplier = BigDecimal.ONE;
            }
            winAmount = bet.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
            userDAO.increaseBalance(userId, winAmount);
            transactionDAO.insert(userId, winAmount, "WIN", "Выигрыш в игре " + game.name());
        }

        gameSessionDAO.insert(userId, game.id(), bet, win ? "WIN" : "LOSS", winAmount);

        BigDecimal balance = userDAO.getBalance(userId);

        StringBuilder field = new StringBuilder();
        for (int i = 0; i < cells; i++) {
            field.append(mineSet.contains(i) ? "M" : ".");
        }

        return new GameRoundResponse(game.type(), win ? "WIN" : "LOSS",
                bet, winAmount, balance, field.toString());
    }
}