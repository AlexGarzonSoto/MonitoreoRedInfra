package com.netwatch.gateway.security;

import com.netwatch.gateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Integración de la tabla users con el sistema de autenticación de Spring Security.
 *
 * Aunque el flujo principal usa JWT (JwtAuthFilter), Spring Security requiere
 * un UserDetailsService para su auto-configuración de DaoAuthenticationProvider.
 * También lo usaría si se habilitara autenticación Basic o Form Login.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.netwatch.gateway.model.User user = userRepository.findByEmail(email)
                .filter(com.netwatch.gateway.model.User::isActive)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado o inactivo: " + email));

        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }
}
