package com.autotea.backend.service;

import com.autotea.backend.domain.User;
import com.autotea.backend.dto.AuthResponse;
import com.autotea.backend.repository.UserRepository;
import com.autotea.backend.security.GoogleTokenVerifier;
import com.autotea.backend.security.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(idToken);
        String googleSub = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");
        String displayName = (name != null && !name.isBlank()) ? name : email;

        User user = userRepository.findByGoogleSub(googleSub)
                .orElseGet(() -> new User(email, googleSub, displayName, picture));
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPictureUrl(picture);
        user.setLastLoginAt(Instant.now());
        user = userRepository.save(user);

        String token = jwtService.issue(user.getId(), user.getEmail());
        return AuthResponse.of(token, user);
    }
}
