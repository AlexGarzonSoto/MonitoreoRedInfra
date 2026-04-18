package com.netwatch.gateway.service;

import com.netwatch.gateway.model.User;
import com.netwatch.gateway.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    private User existingUser;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(userId)
                .email("analyst@netwatch.local")
                .passwordHash("$2a$12$hashedPassword")
                .role(User.Role.ANALYST)
                .active(true)
                .build();
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_retornaPaginaDeUsuarios() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(existingUser)));

        Page<User> result = userService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findAll(pageable);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_conIdExistente_retornaUsuario() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        User result = userService.findById(userId);

        assertThat(result.getEmail()).isEqualTo("analyst@netwatch.local");
    }

    @Test
    void findById_conIdInexistente_lanzaNoSuchElement() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(userId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(userId.toString());
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_conEmailNuevo_creaYGuardaUsuario() {
        when(userRepository.existsByEmail("nuevo@netwatch.local")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("$2a$12$hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        User result = userService.create("nuevo@netwatch.local", "SecurePass123!", User.Role.VIEWER);

        assertThat(result.getEmail()).isEqualTo("nuevo@netwatch.local");
        assertThat(result.getRole()).isEqualTo(User.Role.VIEWER);
        assertThat(result.isActive()).isTrue();
        assertThat(result.getPasswordHash()).isEqualTo("$2a$12$hash");
        verify(passwordEncoder).encode("SecurePass123!");
    }

    @Test
    void create_conEmailDuplicado_lanzaIllegalArgument() {
        when(userRepository.existsByEmail("duplicado@netwatch.local")).thenReturn(true);

        assertThatThrownBy(() ->
                userService.create("duplicado@netwatch.local", "pass", User.Role.ANALYST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicado@netwatch.local");

        verify(userRepository, never()).save(any());
    }

    // ── changeRole ────────────────────────────────────────────────────────────

    @Test
    void changeRole_actualizaRolDelUsuario() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        User result = userService.changeRole(userId, User.Role.ADMIN);

        assertThat(result.getRole()).isEqualTo(User.Role.ADMIN);
        verify(userRepository).save(existingUser);
    }

    @Test
    void changeRole_conIdInexistente_lanzaNoSuchElement() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeRole(userId, User.Role.ADMIN))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── deactivate ────────────────────────────────────────────────────────────

    @Test
    void deactivate_marcaUsuarioComoInactivo() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        userService.deactivate(userId);

        assertThat(existingUser.isActive()).isFalse();
        verify(userRepository).save(existingUser);
    }

    @Test
    void deactivate_conIdInexistente_lanzaNoSuchElement() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deactivate(userId))
                .isInstanceOf(NoSuchElementException.class);
    }
}
