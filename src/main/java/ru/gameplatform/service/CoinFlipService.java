package ru.gameplatform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gameplatform.dao.GameDAO;
import ru.gameplatform.dao.GameSessionDAO;
import ru.gameplatform.dao.GameSettingsDAO;
import ru.gameplatform.dao.TransactionDAO;
import ru.gameplatform.dao.UserDAO;
import ru.gameplatform.dto.GameRoundResponse;
import ru.gameplatform.dto.PlayCoinFlipRequest;
import ru.gameplatform.exception.ApiException;
import ru.gameplatform.model.Game;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Random;

@Service
public class CoinFlipService {

    private static final String GAME_TYPE = "COINFLIP";

    private final Random random = new SecureRandom();
    private final UserDAO userDAO;
    private final GameDAO gameDAO;
    private final GameSettingsDAO gameSettingsDAO;
    private final GameSessionDAO gameSessionDAO;
    private final TransactionDAO transactionDAO;

    public CoinFlipService(UserDAO userDAO,
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
    public GameRoundResponse play(int userId, PlayCoinFlipRequest req) {

        if (req == null || req.betAmount() == null || req.choice() == null) {
            throw new ApiException(400, "Необходимо указать betAmount и choice");
        }

        String choice = req.choice().trim().toUpperCase();
        if (!choice.equals("HEADS") && !choice.equals("TAILS")) {
            throw new ApiException(400, "choice должен быть HEADS или TAILS");
        }

        Game game = gameDAO.findByType(GAME_TYPE)
                .orElseThrow(() -> new ApiException(404, "Игра не найдена или отключена"));

        BigDecimal bet = req.betAmount();
        if (bet.compareTo(game.minBet()) < 0 || bet.compareTo(game.maxBet()) > 0) {
            throw new ApiException(400,
                    "Ставка должна быть от " + game.minBet() + " до " + game.maxBet());
        }

        if (!userDAO.decreaseBalance(userId, bet)) {
            throw new ApiException(400, "Недостаточно средств на балансе");
        }
        transactionDAO.insert(userId, bet, "BET", "Ставка в игре " + game.name());

        String side = random.nextBoolean() ? "HEADS" : "TAILS";
        boolean win = choice.equals(side);

        BigDecimal winAmount = BigDecimal.ZERO;
        if (win) {
            BigDecimal multiplier = new BigDecimal(
                    gameSettingsDAO.findValue(game.id(), "win_multiplier")
                            .orElse("2.0"));
            winAmount = bet.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
            userDAO.increaseBalance(userId, winAmount);
            transactionDAO.insert(userId, winAmount, "WIN", "Выигрыш в игре " + game.name());
        }

        gameSessionDAO.insert(userId, game.id(), bet, win ? "WIN" : "LOSS", winAmount);

        BigDecimal balance = userDAO.getBalance(userId);

        return new GameRoundResponse(game.type(), win ? "WIN" : "LOSS",
                bet, winAmount, balance, side);
    }
}