package ru.gameplatform.service;

import ru.gameplatform.dto.GameRoundResponse;
import ru.gameplatform.dto.PlayRouletteRequest;
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
import java.util.Set;

public class RouletteService {

    private static final String GAME_TYPE = "ROULETTE";
    private static final Set<Integer> RED_NUMBERS = Set.of(
            1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);

    private final Random random = new SecureRandom();
    private final UserRepository userRepository = new UserRepository();
    private final GameRepository gameRepository = new GameRepository();
    private final GameSettingsRepository gameSettingsRepository = new GameSettingsRepository();
    private final GameSessionRepository gameSessionRepository = new GameSessionRepository();
    private final TransactionRepository transactionRepository = new TransactionRepository();

    public GameRoundResponse play(int userId, PlayRouletteRequest req) {

        if (req == null || req.betAmount() == null || req.betType() == null) {
            throw new ApiException(400, "Необходимо указать betAmount и betType");
        }

        String betType = req.betType().trim().toUpperCase();
        Integer number = req.number();

        if (betType.equals("NUMBER")) {
            if (number == null || number < 0 || number > 36) {
                throw new ApiException(400, "Для ставки NUMBER укажи число от 0 до 36");
            }
        } else if (!betType.equals("RED") && !betType.equals("BLACK")) {
            throw new ApiException(400, "betType должен быть RED, BLACK или NUMBER");
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

            int resultNumber = random.nextInt(37);
            String resultColor = colorOf(resultNumber);

            boolean win;
            BigDecimal multiplier;

            if (betType.equals("NUMBER")) {
                win = resultNumber == number;
                multiplier = new BigDecimal(gameSettingsRepository
                        .findValue(conn, game.id(), "number_multiplier")
                        .orElse("36.0"));
            } else {
                win = betType.equals(resultColor);
                multiplier = new BigDecimal(gameSettingsRepository
                        .findValue(conn, game.id(), "color_multiplier")
                        .orElse("2.0"));
            }

            BigDecimal winAmount = BigDecimal.ZERO;
            if (win) {
                winAmount = bet.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
                userRepository.increaseBalance(conn, userId, winAmount);
                transactionRepository.insert(conn, userId, winAmount, "WIN",
                        "Выигрыш в игре " + game.name());
            }

            gameSessionRepository.insert(conn, userId, game.id(), bet,
                    win ? "WIN" : "LOSS", winAmount);

            BigDecimal balance = userRepository.getBalance(conn, userId);
            String detail = resultNumber + " " + resultColor;

            return new GameRoundResponse(game.type(), win ? "WIN" : "LOSS",
                    bet, winAmount, balance, detail);
        });
    }

    private String colorOf(int n) {
        if (n == 0) {
            return "GREEN";
        }
        return RED_NUMBERS.contains(n) ? "RED" : "BLACK";
    }
}