package com.example.backend.user;

import com.example.backend.user.User;
import com.example.backend.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User provision(String keycloakSub, String email, String displayName) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setKeycloakSub(keycloakSub);
        user.setEmail(email);
        user.setDisplayName(displayName);
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            return userRepository.findByKeycloakSub(keycloakSub)
                    .orElseThrow(() -> exception);
        }
    }
}
