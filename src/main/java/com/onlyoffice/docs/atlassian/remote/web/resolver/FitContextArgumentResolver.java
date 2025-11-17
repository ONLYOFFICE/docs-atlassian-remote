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

package com.onlyoffice.docs.atlassian.remote.web.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.docs.atlassian.remote.aop.CurrentFitContext;
import com.onlyoffice.docs.atlassian.remote.api.FitContext;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;


@Component
public class FitContextArgumentResolver implements HandlerMethodArgumentResolver {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supportsParameter(final MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentFitContext.class)
                && parameter.getParameterType().equals(FitContext.class);
    }

    @Override
    public Object resolveArgument(final MethodParameter parameter,
                                  final ModelAndViewContainer mavContainer,
                                  final NativeWebRequest webRequest,
                                  final WebDataBinderFactory binderFactory) {

        Object principal = webRequest.getUserPrincipal();
        if (!(principal instanceof JwtAuthenticationToken token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT not found");
        }

        Jwt jwt = token.getToken();
        Map<String, Object> contextMap = jwt.getClaimAsMap("context");
        if (Objects.isNull(contextMap)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing 'context' claim");
        }

        Object cloudIdRaw = contextMap.get("cloudId");
        if (Objects.isNull(cloudIdRaw)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing 'cloudId' in context claim");
        }

        try {
            UUID.fromString(cloudIdRaw.toString());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid 'cloudId' format, must be UUID");
        }

        return objectMapper.convertValue(contextMap, FitContext.class);
    }
}
