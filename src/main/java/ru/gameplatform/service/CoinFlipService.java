package ru.gameplatform.service;

import ru.gameplatform.dto.GameRoundResponse;
import ru.gameplatform.dto.PlayCoinFlipRequest;
import ru.gameplatform.exception.ApiException;
import ru.gameplatform.model.Game;
import ru.gameplatform.repository.GameRepository;
import ru.gameplatform.repository.GameSessionRepository;
import ru.gameplatform.repository.GameSettingsRepository;
import ru.gameplatform.repository.TransactionRepository;
import ru.gameplatform.repository.UserRepository;
import ru.gameplatform.util.JdbcTransaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Random;

public class CoinFlipService {

    private static final String GAME_TYPE = "COINFLIP";

    private final Random random = new SecureRandom();
    private final UserRepository userRepository = new UserRepository();
    private final GameRepository gameRepository = new GameRepository();
    private final GameSettingsRepository gameSettingsRepository = new GameSettingsRepository();
    private final GameSessionRepository gameSessionRepository = new GameSessionRepository();
    private final TransactionRepository transactionRepository = new TransactionRepository();

    public GameRoundResponse play(int userId, PlayCoinFlipRequest req) {

        if (req == null || req.betAmount() == null || req.choice() == null) {
            throw new ApiException(400, "Необходимо указать betAmount и choice");
        }

        String choice = req.choice().trim().toUpperCase();
        if (!choice.equals("HEADS") && !choice.equals("TAILS")) {
            throw new ApiException(400, "choice должен быть HEADS или TAILS");
        }

        Game game = gameRepository.findByType(GAME_TYPE)
                .orElseThrow(() -> new ApiException(404, "Игра не найдена или отключена"));

        BigDecimal bet = req.betAmount();
        if (bet.compareTo(game.minBet()) < 0 || bet.compareTo(game.maxBet()) > 0) {
            throw new ApiException(400,
                    "Ставка должна быть от " + game.minBet() + " до " + game.maxBet());
        }

        return JdbcTransaction.execute(conn -> {

            if (!userRepository.decreaseBalance(conn, userId, bet)) {
                throw new ApiException(400, "Недостаточно средств на балансе");
            }
            transactionRepository.insert(conn, userId, bet, "BET",
                    "Ставка в игре " + game.name());

            String side = random.nextBoolean() ? "HEADS" : "TAILS";
            boolean win = choice.equals(side);

            BigDecimal winAmount = BigDecimal.ZERO;
            if (win) {
                BigDecimal multiplier = new BigDecimal(
                        gameSettingsRepository.findValue(conn, game.id(), "win_multiplier")
                                .orElse("2.0"));
                winAmount = bet.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
                userRepository.increaseBalance(conn, userId, winAmount);
                transactionRepository.insert(conn, userId, winAmount, "WIN",
                        "Выигрыш в игре " + game.name());
            }

            gameSessionRepository.insert(conn, userId, game.id(), bet,
                    win ? "WIN" : "LOSS", winAmount);

            BigDecimal balance = userRepository.getBalance(conn, userId);

            return new GameRoundResponse(game.type(), win ? "WIN" : "LOSS",
                    bet, winAmount, balance, side);
        });
    }
}