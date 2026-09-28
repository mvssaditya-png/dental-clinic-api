package com.dentalclinic.security;

import com.dentalclinic.auth.service.JwtService;
import com.dentalclinic.security.filter.JwtAuthenticationFilter;
import com.dentalclinic.security.repository.*;
import com.dentalclinic.user.entity.*;
import com.dentalclinic.user.repository.AppUserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    static final String SECRET = "test-only-signing-key-at-least-thirty-two-characters-long";
    JwtService jwt;
    AppUserRepository users;
    UserRoleRepository roles;
    RolePermissionRepository permissions;
    JwtAuthenticationFilter filter;
    @BeforeEach void setup() {
        jwt = new JwtService(); ReflectionTestUtils.setField(jwt,"jwtSecret",SECRET);
        ReflectionTestUtils.setField(jwt,"jwtExpirationMs",60000L);
        users=mock(AppUserRepository.class); roles=mock(UserRoleRepository.class); permissions=mock(RolePermissionRepository.class);
        filter=new JwtAuthenticationFilter(jwt,users,roles,permissions);
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    // Synthetic credentials are confined to isolated automated tests, never live API calls.
    String token(String subject, long expires, String key) {
        return Jwts.builder().subject(subject).expiration(new Date(expires))
            .signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8))).compact();
    }
    MockHttpServletRequest request(String token) {
        var request=new MockHttpServletRequest(); request.addHeader("Authorization","Bearer "+token); return request;
    }
    void expectAnonymous(String token) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("stale",null,List.of()));
        var chain=mock(FilterChain.class);
        doAnswer(invocation->{ assertNull(SecurityContextHolder.getContext().getAuthentication()); return null; }).when(chain).doFilter(any(),any());
        filter.doFilter(request(token),new MockHttpServletResponse(),chain);
        verify(chain).doFilter(any(),any());
    }
    @Test void malformedExpiredWrongSignatureAndInvalidSubjectsContinueAnonymous() throws Exception {
        long future=System.currentTimeMillis()+60000;
        for(String token:List.of("malformed", "", token(UUID.randomUUID().toString(),System.currentTimeMillis()-1000,SECRET),
            token(UUID.randomUUID().toString(),future,"different-test-signing-key-at-least-thirty-two-characters"),
            token("not-a-uuid",future,SECRET),token(null,future,SECRET))) {
            expectAnonymous(token);
        }
        verifyNoInteractions(users,roles,permissions);
    }
    @Test void expirationBetweenValidationAndExtractionContinuesAnonymous() throws Exception {
        jwt=mock(JwtService.class); filter=new JwtAuthenticationFilter(jwt,users,roles,permissions);
        when(jwt.isTokenValid("race")).thenReturn(true);
        when(jwt.extractSubject("race")).thenThrow(new io.jsonwebtoken.ExpiredJwtException(null,null,"expired"));
        expectAnonymous("race"); verifyNoInteractions(users,roles,permissions);
    }
    @Test void missingOrInactiveUserContinuesAnonymous() throws Exception {
        UUID id=UUID.randomUUID(); String token=token(id.toString(),System.currentTimeMillis()+60000,SECRET);
        when(users.findByIdWithClinic(id)).thenReturn(Optional.empty()); expectAnonymous(token);
        when(users.findByIdWithClinic(id)).thenReturn(Optional.of(AppUser.builder().id(id).status(UserStatus.INACTIVE).build())); expectAnonymous(token);
        verifyNoInteractions(roles,permissions);
    }
    @Test void validActiveUserAuthenticates() throws Exception {
        UUID id=UUID.randomUUID(); var user=AppUser.builder().id(id).status(UserStatus.ACTIVE).build();
        when(users.findByIdWithClinic(id)).thenReturn(Optional.of(user));
        filter.doFilter(request(token(id.toString(),System.currentTimeMillis()+60000,SECRET)),new MockHttpServletResponse(),mock(FilterChain.class));
        assertSame(user,SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
    }
    @Test void repositoryFailureIsNotSwallowed() {
        UUID id=UUID.randomUUID(); var failure=new org.springframework.dao.DataAccessResourceFailureException("database unavailable");
        when(users.findByIdWithClinic(id)).thenThrow(failure);
        var chain=mock(FilterChain.class);
        assertSame(failure,assertThrows(org.springframework.dao.DataAccessResourceFailureException.class,()->
            filter.doFilter(request(token(id.toString(),System.currentTimeMillis()+60000,SECRET)),new MockHttpServletResponse(),chain)));
        verifyNoInteractions(chain);
    }
    @Test void downstreamIllegalArgumentExceptionIsNotTreatedAsBadToken() throws Exception {
        UUID id=UUID.randomUUID(); when(users.findByIdWithClinic(id)).thenReturn(Optional.of(AppUser.builder().id(id).status(UserStatus.ACTIVE).build()));
        var failure=new IllegalArgumentException("downstream business failure"); var chain=mock(FilterChain.class);
        doThrow(failure).when(chain).doFilter(any(),any());
        assertSame(failure,assertThrows(IllegalArgumentException.class,()->filter.doFilter(
            request(token(id.toString(),System.currentTimeMillis()+60000,SECRET)),new MockHttpServletResponse(),chain)));
    }
}
