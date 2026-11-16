package ru.gameplatform.service;

import ru.gameplatform.dto.GameRoundResponse;
import ru.gameplatform.dto.PlayCoinFlipRequest;
import ru.gameplatform.exception.ApiException;
import ru.gameplatform.model.Game;
import ru.gameplatform.repository.GameDao;
import ru.gameplatform.repository.GameSessionDao;
import ru.gameplatform.repository.GameSettingsDao;
import ru.gameplatform.repository.TransactionDao;
import ru.gameplatform.repository.UserDao;
import ru.gameplatform.util.JdbcTransaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Random;

public class CoinFlipService {

    private static final String GAME_TYPE = "COINFLIP";

    private final Random random = new SecureRandom();
    private final UserDao userDao = new UserDao();
    private final GameDao gameDao = new GameDao();
    private final GameSettingsDao gameSettingsDao = new GameSettingsDao();
    private final GameSessionDao gameSessionDao = new GameSessionDao();
    private final TransactionDao transactionDao = new TransactionDao();

    public GameRoundResponse play(int userId, PlayCoinFlipRequest req) {

        if (req == null || req.betAmount() == null || req.choice() == null) {
            throw new ApiException(400, "Необходимо указать betAmount и choice");
        }

        String choice = req.choice().trim().toUpperCase();
        if (!choice.equals("HEADS") && !choice.equals("TAILS")) {
            throw new ApiException(400, "choice должен быть HEADS или TAILS");
        }

        Game game = gameDao.findByType(GAME_TYPE)
                .orElseThrow(() -> new ApiException(404, "Игра не найдена или отключена"));

        BigDecimal bet = req.betAmount();
        if (bet.compareTo(game.minBet()) < 0 || bet.compareTo(game.maxBet()) > 0) {
            throw new ApiException(400,
                    "Ставка должна быть от " + game.minBet() + " до " + game.maxBet());
        }

        return JdbcTransaction.execute(conn -> {

            if (!userDao.decreaseBalance(conn, userId, bet)) {
                throw new ApiException(400, "Недостаточно средств на балансе");
            }
            transactionDao.insert(conn, userId, bet, "BET",
                    "Ставка в игре " + game.name());

            String side = random.nextBoolean() ? "HEADS" : "TAILS";
            boolean win = choice.equals(side);

            BigDecimal winAmount = BigDecimal.ZERO;
            if (win) {
                BigDecimal multiplier = new BigDecimal(
                        gameSettingsDao.findValue(conn, game.id(), "win_multiplier")
                                .orElse("2.0"));
                winAmount = bet.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
                userDao.increaseBalance(conn, userId, winAmount);
                transactionDao.insert(conn, userId, winAmount, "WIN",
                        "Выигрыш в игре " + game.name());
            }

            gameSessionDao.insert(conn, userId, game.id(), bet,
                    win ? "WIN" : "LOSS", winAmount);

            BigDecimal balance = userDao.getBalance(conn, userId);

            return new GameRoundResponse(game.type(), win ? "WIN" : "LOSS",
                    bet, winAmount, balance, side);
        });
    }
}