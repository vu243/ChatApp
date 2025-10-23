package com.nhv.chatapp.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Service
public class JWTService {
    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGN_KEY ;

    public String generateToken(String username){
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(username)
                .issuer("chatapp.com")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()))
                .build();
        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwSObject = new JWSObject(header,payload);
        try {
            jwSObject.sign(new MACSigner(SIGN_KEY.getBytes()));
            return jwSObject.serialize();
        }catch (JOSEException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public String extractUsername(String token) throws ParseException, JOSEException {
        try{
            SignedJWT signedJWT = SignedJWT.parse(token);
            MACVerifier verifier = new MACVerifier(SIGN_KEY.getBytes());
            if(!signedJWT.verify(verifier)){
                log.error(token+" is not a valid JWT token");
                return null;
            }
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            Date expiration = claimsSet.getExpirationTime();
            if(expiration.before(new Date())){
                log.error(token+" is expired");
                return null;
            }
            return claimsSet.getSubject();
        }catch (Exception e){
            log.error(e.getMessage());
            return null;
        }
    }
}
