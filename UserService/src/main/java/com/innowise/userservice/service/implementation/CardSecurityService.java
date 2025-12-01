package com.innowise.userservice.service.implementation;

import com.innowise.userservice.dao.interfaces.CardDao;
import com.innowise.userservice.entities.CardEntity;
import com.innowise.userservice.entities.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("cardSecurityService")
@RequiredArgsConstructor
public class CardSecurityService {

    private final CardDao cardDao;


    public boolean isCardOwner(Long cardId, Authentication authentication) {
        CardEntity card = cardDao.getById(cardId).orElse(null);
        if (card == null) {
            return false;
        }

        Object details = authentication.getDetails();
        if (!(details instanceof Map)) {
            return false;
        }

        Map<String, Object> detailMap = (Map<String, Object>) details;
        Long userId = (Long) detailMap.get("userId");

        if (userId == null) {
            return false;
        }

        return card.getUser() != null && card.getUser().getId().equals(userId);
    }
}