package uz.programmer.courseBot.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Date;

@Component
public class JWTUtil {

    private String secret = "Aefef";

    public String generateToken(String token) {
        return JWT.create()
                .withSubject("ProgrammerUz")
                .withClaim("token", token)
                .withIssuedAt(new Date())
                .withIssuer("https://www.moykachi.uz/api")
                .withExpiresAt(Date.from(ZonedDateTime.now().plusHours(5).toInstant()))
                .sign(Algorithm.HMAC256(secret));
    }

    public String validateTokenAndRetrieveData(String token) {
        JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(secret))
                .withIssuer("https://www.moykachi.uz/api")
                .withSubject("ProgrammerUz")
                .build();
        DecodedJWT jwt = jwtVerifier.verify(token);
        return jwt.getClaim("token").asString();
    }
}
