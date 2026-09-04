package com.dentalclinic.security.filter;

import com.dentalclinic.auth.service.JwtService;
import com.dentalclinic.security.entity.Role;
import com.dentalclinic.security.entity.UserRole;
import com.dentalclinic.security.repository.RolePermissionRepository;
import com.dentalclinic.security.repository.UserRoleRepository;
import com.dentalclinic.user.entity.AppUser;
import com.dentalclinic.user.entity.UserStatus;
import com.dentalclinic.user.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID userId = jwtService.extractUserId(token);

        AppUser user = appUserRepository
                .findByIdWithClinic(userId)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElse(null);

        if (user == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            List<UserRole> userRoles =
                    userRoleRepository.findAllByUserId(user.getId());

            for (UserRole userRole : userRoles) {

                Role role = userRole.getRole();

                authorities.add(
                        new SimpleGrantedAuthority(
                                "ROLE_" + role.getRoleCode()
                        )
                );

                rolePermissionRepository
                        .findAllByRoleId(role.getId())
                        .forEach(rolePermission ->
                                authorities.add(
                                        new SimpleGrantedAuthority(
                                                rolePermission
                                                        .getPermission()
                                                        .getPermissionCode()
                                        )
                                )
                        );
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            authorities
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}