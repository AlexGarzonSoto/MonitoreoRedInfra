package com.netwatch.gateway.service;

import com.netwatch.gateway.model.User;
import com.netwatch.gateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + id));
    }

    @Transactional
    public User create(String email, String rawPassword, User.Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("El email ya está registrado: " + email);
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .active(true)
                .build();
        log.info("Usuario creado: email={}, role={}", email, role);
        return userRepository.save(user);
    }

    @Transactional
    public User changeRole(UUID id, User.Role newRole) {
        User user = findById(id);
        user.setRole(newRole);
        log.info("Rol cambiado: userId={}, newRole={}", id, newRole);
        return userRepository.save(user);
    }

    @Transactional
    public void deactivate(UUID id) {
        User user = findById(id);
        user.setActive(false);
        userRepository.save(user);
        log.info("Usuario desactivado: {}", id);
    }
}
