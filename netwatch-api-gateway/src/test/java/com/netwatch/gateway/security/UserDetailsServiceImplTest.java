package com.netwatch.gateway.security;

import com.netwatch.gateway.model.User;
import com.netwatch.gateway.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UserDetailsServiceImpl userDetailsService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@netwatch.local")
                .passwordHash("$2a$12$hashedPassword")
                .role(User.Role.ADMIN)
                .active(true)
                .build();
    }

    @Test
    void loadUserByUsername_conUsuarioActivo_retornaUserDetails() {
        when(userRepository.findByEmail("admin@netwatch.local"))
                .thenReturn(Optional.of(activeUser));

        UserDetails details = userDetailsService.loadUserByUsername("admin@netwatch.local");

        assertThat(details.getUsername()).isEqualTo("admin@netwatch.local");
        assertThat(details.getPassword()).isEqualTo("$2a$12$hashedPassword");
        assertThat(details.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_conRolAnalyst_retornaAutoridadCorrecta() {
        activeUser.setRole(User.Role.ANALYST);
        when(userRepository.findByEmail("analyst@netwatch.local"))
                .thenReturn(Optional.of(activeUser));

        UserDetails details = userDetailsService.loadUserByUsername("analyst@netwatch.local");

        assertThat(details.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_ANALYST");
    }

    @Test
    void loadUserByUsername_conUsuarioInactivo_lanzaUsernameNotFound() {
        activeUser.setActive(false);
        when(userRepository.findByEmail("admin@netwatch.local"))
                .thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() ->
                userDetailsService.loadUserByUsername("admin@netwatch.local"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("admin@netwatch.local");
    }

    @Test
    void loadUserByUsername_conEmailNoExistente_lanzaUsernameNotFound() {
        when(userRepository.findByEmail("noexiste@netwatch.local"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userDetailsService.loadUserByUsername("noexiste@netwatch.local"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
