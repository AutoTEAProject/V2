package com.autotea.backend.security;

import com.autotea.backend.config.GoogleAuthProperties;
import com.autotea.backend.exception.InvalidGoogleTokenException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Component
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(GoogleAuthProperties properties) throws GeneralSecurityException, IOException {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(properties.clientId()))
                .build();
    }

    public GoogleIdToken.Payload verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new InvalidGoogleTokenException("유효하지 않은 Google ID 토큰입니다.");
            }
            return idToken.getPayload();
        } catch (InvalidGoogleTokenException e) {
            throw e;
        } catch (Exception e) {
            // 서명 검증 실패(GeneralSecurityException/IOException) 외에도, 형식이 아예 잘못된
            // 토큰이 들어오면 google-api-client가 IllegalArgumentException을 던지기도 한다.
            throw new InvalidGoogleTokenException("Google ID 토큰 검증 실패: " + e.getMessage());
        }
    }
}
