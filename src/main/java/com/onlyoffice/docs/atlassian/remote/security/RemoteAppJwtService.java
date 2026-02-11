/**
 *
 * (c) Copyright Ascensio System SIA 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.onlyoffice.docs.atlassian.remote.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.onlyoffice.docs.atlassian.remote.api.ConfluenceContext;
import com.onlyoffice.docs.atlassian.remote.api.Context;
import com.onlyoffice.docs.atlassian.remote.api.JiraContext;
import com.onlyoffice.docs.atlassian.remote.api.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@Service
public class RemoteAppJwtService {
    private final NimbusJwtEncoder nimbusJwtEncoder;
    private final NimbusJwtDecoder nimbusJwtDecoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.security.ttl.default}")
    private long ttlDefault;

    public RemoteAppJwtService(final @Value("${app.security.secret}") String secret) {
        SecretKey secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "RAW");
        JWKSource<SecurityContext> jwkSource = new ImmutableSecret<>(secret.getBytes(StandardCharsets.UTF_8));

        List<OAuth2TokenValidator<Jwt>> validators = List.of(
                new JwtTimestampValidator(),
                new JwtClaimValidator<>(JwtClaimNames.SUB, (String sub) -> Objects.nonNull(sub) && !sub.isBlank()),
                new JwtClaimValidator<>("context",
                        (Map<String, Object> contextAsMap) -> {
                            try {
                                if (contextAsMap == null || !contextAsMap.containsKey("product")) {
                                    return false;
                                }

                                String product = (String) contextAsMap.get("product");

                                switch (Product.valueOf(product)) {
                                    case JIRA:
                                        objectMapper.convertValue(contextAsMap, JiraContext.class);
                                        return true;
                                    case CONFLUENCE:
                                        objectMapper.convertValue(contextAsMap, ConfluenceContext.class);
                                        return true;
                                    default:
                                        return false;
                                }
                            } catch (Exception e) {
                                return false;
                            }

                        }
                )
        );

        this.nimbusJwtDecoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        nimbusJwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));

        this.nimbusJwtEncoder = new NimbusJwtEncoder(jwkSource);
    }

    public Jwt encode(final String subject, final String audience, final long lifeTimeInMinutes,
                      final Map<String, Object> context) {
        return encode(
                subject,
                audience,
                Instant.now(),
                TimeUnit.MINUTES.toSeconds(lifeTimeInMinutes),
                context
        );
    }

    public Jwt encode(final String subject, final String audience, final Instant now, final long lifeTimeInSeconds,
                      final Map<String, Object> context) {
        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        Instant expiry = now.plusSeconds(lifeTimeInSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject)
                .audience(List.of(audience))
                .issuedAt(now)
                .expiresAt(expiry)
                .claim("context", context)
                .build();

        JwtEncoderParameters parameters = JwtEncoderParameters.from(header, claims);

        return nimbusJwtEncoder.encode(parameters);
    }

    public Jwt decode(final String token, final String audience) {
        Jwt jwt = nimbusJwtDecoder.decode(token);

        JwtAudienceValidator jwtAudienceValidator = new JwtAudienceValidator(audience);

        OAuth2TokenValidatorResult result = jwtAudienceValidator.validate(jwt);
        if (result.hasErrors()) {
            Collection<OAuth2Error> errors = result.getErrors();
            String validationErrorString = this.getJwtValidationExceptionMessage(errors);

            throw new JwtValidationException(validationErrorString, errors);
        } else {
            return jwt;
        }
    }

    private String getJwtValidationExceptionMessage(final Collection<OAuth2Error> errors) {
        for (OAuth2Error oAuth2Error : errors) {
            if (StringUtils.hasLength(oAuth2Error.getDescription())) {
                return String.format(
                        "An error occurred while attempting to decode the Jwt: %s",
                        oAuth2Error.getDescription()
                );
            }
        }

        return "Unable to validate Jwt";
    }

    public URI signUri(final URI uri, final String accountId, final Context context) {
        return signUri(uri, accountId, context, ttlDefault);
    }

    public URI signUri(final URI uri, final String accountId, final Context context, final long timeToLive) {
        String token = encode(
                accountId,
                uri.getPath(),
                timeToLive,
                objectMapper.convertValue(context, new TypeReference<Map<String, Object>>() { })
        ).getTokenValue();


        return UriComponentsBuilder.fromUri(uri)
                .queryParam("token", token)
                .build()
                .toUri();
    }
}
