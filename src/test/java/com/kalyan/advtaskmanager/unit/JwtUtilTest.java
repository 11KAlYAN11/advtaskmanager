package com.kalyan.advtaskmanager.unit;

import com.kalyan.advtaskmanager.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * JwtUtilTest — UNIT TESTS
 *
 * No Spring context loaded. Tests the JWT utility class in isolation.
 * Covers: token generation, claim extraction, validation, expiry.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@DisplayName("JWT Utility — Unit Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    // Same secret used in application-test.properties
    private static final String SECRET =
            "testSecretKeyForJUnitTestsPurposeOnlyDoNotUseInProd123";
    private static final long EXPIRATION = 3_600_000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Inject @Value fields manually (no Spring context here)
        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION);
    }

    // ─── TC-JWT-001 ───────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-JWT-001 | generateToken returns non-null, non-empty string")
    void generateToken_returnsNonBlankToken() {
        String token = jwtUtil.generateToken("admin@gmail.com", "ADMIN");

        assertThat(token)
                .isNotNull()
                .isNotBlank()
                .contains("."); // JWT has 3 dot-separated parts
    }

    // ─── TC-JWT-002 ───────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-JWT-002 | extractEmail returns the email used during generation")
    void extractEmail_returnsCorrectSubject() {
        String email = "user@test.com";
        String token = jwtUtil.generateToken(email, "USER");

        assertThat(jwtUtil.extractEmail(token)).isEqualTo(email);
    }

    // ─── TC-JWT-003 ───────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-JWT-003 | extractRole returns the role embedded in claims")
    void extractRole_returnsCorrectRole() {
        String token = jwtUtil.generateToken("admin@gmail.com", "ADMIN");

        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    // ─── TC-JWT-004 ───────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-JWT-004 | isTokenValid returns true for matching email and fresh token")
    void isTokenValid_returnsTrueForValidToken() {
        String token = jwtUtil.generateToken("user@test.com", "USER");

        assertThat(jwtUtil.isTokenValid(token, "user@test.com")).isTrue();
    }

    // ─── TC-JWT-005 ───────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-JWT-005 | isTokenValid returns false when email does not match token subject")
    void isTokenValid_returnsFalseForWrongEmail() {
        String token = jwtUtil.generateToken("real@test.com", "USER");

        assertThat(jwtUtil.isTokenValid(token, "attacker@evil.com")).isFalse();
    }

    // ─── TC-JWT-006 ───────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-JWT-006 | Expired token throws exception on parsing")
    void expiredToken_throwsExceptionOnParse() {
        // Generate a token that is already expired (expiry = -1 ms)
        JwtUtil shortLived = new JwtUtil();
        ReflectionTestUtils.setField(shortLived, "secretKey", SECRET);
        ReflectionTestUtils.setField(shortLived, "expiration", -1L); // expired immediately

        String expiredToken = shortLived.generateToken("user@test.com", "USER");

        // Attempting to extract email from expired token must throw
        assertThatThrownBy(() -> jwtUtil.extractEmail(expiredToken))
                .isInstanceOf(Exception.class);
    }

    // ─── TC-JWT-007 ───────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-JWT-007 | Tampered token signature causes parsing to throw")
    void tamperedToken_throwsExceptionOnParse() {
        String token = jwtUtil.generateToken("user@test.com", "USER");

        // Corrupt the signature (last segment)
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "INVALIDSIGNATURE";

        assertThatThrownBy(() -> jwtUtil.extractEmail(tampered))
                .isInstanceOf(Exception.class);
    }

    // ─── TC-JWT-008 ───────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-JWT-008 | Two tokens for same user are not identical (different iat)")
    void twoTokensForSameUser_areUnique() throws InterruptedException {
        String token1 = jwtUtil.generateToken("user@test.com", "USER");
        Thread.sleep(1_000); // ensure different issuedAt timestamps
        String token2 = jwtUtil.generateToken("user@test.com", "USER");

        assertThat(token1).isNotEqualTo(token2);
    }

    // ─── TC-JWT-009 ───────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-JWT-009 | Token generated with ADMIN role extracts ADMIN correctly")
    void adminRoleToken_extractsAdminRole() {
        String token = jwtUtil.generateToken("admin@gmail.com", "ADMIN");

        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("admin@gmail.com");
        assertThat(jwtUtil.isTokenValid(token, "admin@gmail.com")).isTrue();
    }
}

