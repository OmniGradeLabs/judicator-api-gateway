package com.judicator.gateway.modules.identity.service.token;

import com.judicator.gateway.modules.identity.dto.token.request.TokenRequest;
import com.judicator.gateway.modules.identity.dto.token.response.TokenPair;
import com.nimbusds.jwt.SignedJWT;
import java.util.Date;

public interface TokenService {
  TokenPair generateTokenPair(TokenRequest tokenRequest);

  String generateAccessToken(TokenRequest tokenRequest);

  String generateRefreshToken(TokenRequest tokenRequest);

  SignedJWT verifyAccessToken(String token);

  SignedJWT verifyRefreshToken(String token);

  TokenRequest extractToken(String refreshToken);

  String generateRefreshToken(TokenRequest tokenRequest, Date absoluteExpiry);
}
