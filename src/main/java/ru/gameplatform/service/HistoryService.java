package ru.gameplatform.service;

import org.springframework.stereotype.Service;
import ru.gameplatform.dao.GameSessionDAO;
import ru.gameplatform.dao.TransactionDAO;
import ru.gameplatform.dto.PageResponse;
import ru.gameplatform.dto.SessionHistoryItem;
import ru.gameplatform.dto.TransactionHistoryItem;
import ru.gameplatform.exception.ApiException;

@Service
public class HistoryService {

    private final GameSessionDAO gameSessionDAO;
    private final TransactionDAO transactionDAO;

    public HistoryService(GameSessionDAO gameSessionDAO,
                          TransactionDAO transactionDAO) {
        this.gameSessionDAO = gameSessionDAO;
        this.transactionDAO = transactionDAO;
    }

    public PageResponse<SessionHistoryItem> sessions(int userId, String game, String result,
                                                     int page, int size) {
        validatePage(page, size);
        return gameSessionDAO.findHistory(userId, game, result, page, size);
    }

    public PageResponse<TransactionHistoryItem> transactions(int userId, String type, String from, String to,
                                                             int page, int size) {
        validatePage(page, size);
        return transactionDAO.findHistory(userId, type, from, to, page, size);
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new ApiException(400, "Параметр page должен быть 0 или больше");
        }
        if (size < 1 || size > 100) {
            throw new ApiException(400, "Параметр size должен быть от 1 до 100");
        }
    }
}