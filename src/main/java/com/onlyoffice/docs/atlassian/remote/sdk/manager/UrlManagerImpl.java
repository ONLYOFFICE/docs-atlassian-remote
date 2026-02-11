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

package com.onlyoffice.docs.atlassian.remote.sdk.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.docs.atlassian.remote.api.Context;
import com.onlyoffice.docs.atlassian.remote.security.RemoteAppJwtService;
import com.onlyoffice.docs.atlassian.remote.security.SecurityUtils;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.DefaultUrlManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
public class UrlManagerImpl extends DefaultUrlManager {
    private final RemoteAppJwtService remoteAppJwtService;
    private final SecurityUtils securityUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.base-url}")
    private String baseUrl;
    @Value("${app.security.ttl.default}")
    private long ttlDefault;
    @Value("${app.security.ttl.callback}")
    private long ttlCallback;

    public UrlManagerImpl(final SettingsManager settingsManager, final RemoteAppJwtService remoteAppJwtService,
                          final SecurityUtils securityUtils) {
        super(settingsManager);

        this.remoteAppJwtService = remoteAppJwtService;
        this.securityUtils = securityUtils;
    }

    @Override
    public String getFileUrl(final String fileId) {
        Context context = securityUtils.getCurrentAppContext();
        String path = "/api/v1/download/" + context.getProduct().toString().toLowerCase();

        String token = remoteAppJwtService.encode(
                securityUtils.getCurrentPrincipal().getSubject(),
                path,
                ttlDefault,
                objectMapper.convertValue(context, new TypeReference<Map<String, Object>>() { })
        ).getTokenValue();

        return baseUrl + path + "?token=" + token;
    }

    @Override
    public String getCallbackUrl(final String fileId) {
        Context context = securityUtils.getCurrentAppContext();
        String path = "/api/v1/callback/" + context.getProduct().toString().toLowerCase();

        String token = remoteAppJwtService.encode(
                securityUtils.getCurrentPrincipal().getSubject(),
                path,
                ttlCallback,
                objectMapper.convertValue(context, new TypeReference<Map<String, Object>>() { })
        ).getTokenValue();

        return baseUrl + path + "?token=" + token;
    }
}
