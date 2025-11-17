/**
 *
 * (c) Copyright Ascensio System SIA 2025
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

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.onlyoffice.docs.atlassian.remote.api.XForgeTokenType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;


@Component
@RequiredArgsConstructor
public class XForgeTokenRepository {
    private final RedisTemplate<String, String> redisXForgeTokensTemplate;

    public String getXForgeToken(final String key, final XForgeTokenType xForgeTokenType) {
        return redisXForgeTokensTemplate.opsForValue()
                .get(xForgeTokenType.getValue() + "::" + key);
    }

    public Instant getXForgeTokenExpiration(final String key, final XForgeTokenType xForgeTokenType)
            throws ParseException {
        String token = getXForgeToken(key, xForgeTokenType);

        if (Objects.isNull(token)) {
            return null;
        }

        JWT jwt = JWTParser.parse(token);
        JWTClaimsSet claimsSet = jwt.getJWTClaimsSet();

        return claimsSet.getExpirationTime().toInstant();
    }

    public void saveXForgeToken(final String key, final String token, final XForgeTokenType xForgeTokenType)
            throws ParseException {
        JWT jwt = JWTParser.parse(token);
        JWTClaimsSet claimsSet = jwt.getJWTClaimsSet();

        redisXForgeTokensTemplate.opsForValue().set(
                xForgeTokenType.getValue() + "::" + key,
                token,
                Duration.between(Instant.now(), claimsSet.getExpirationTime().toInstant())
        );
    }
}
