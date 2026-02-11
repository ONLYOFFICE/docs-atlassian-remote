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

import com.onlyoffice.docs.atlassian.remote.api.Context;
import com.onlyoffice.docs.atlassian.remote.api.Product;
import com.onlyoffice.docs.atlassian.remote.api.XForgeTokenType;
import com.onlyoffice.docs.atlassian.remote.client.confluence.ConfluenceClient;
import com.onlyoffice.docs.atlassian.remote.client.confluence.dto.ConfluenceSettings;
import com.onlyoffice.docs.atlassian.remote.client.jira.JiraClient;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraSettings;
import com.onlyoffice.docs.atlassian.remote.entity.DemoServerConnection;
import com.onlyoffice.docs.atlassian.remote.entity.DemoServerConnectionId;
import com.onlyoffice.docs.atlassian.remote.repository.DemoServerConnectionRepository;
import com.onlyoffice.docs.atlassian.remote.security.SecurityUtils;
import com.onlyoffice.docs.atlassian.remote.security.XForgeTokenRepository;
import com.onlyoffice.manager.settings.DefaultSettingsManager;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Objects;
import java.util.Optional;

import static com.onlyoffice.docs.atlassian.remote.Constants.SETTINGS_KEY;


@AllArgsConstructor
@Component
public class SettingsManagerImpl extends DefaultSettingsManager {
    private final JiraClient jiraClient;
    private final ConfluenceClient confluenceClient;
    private final XForgeTokenRepository xForgeTokenRepository;
    private final DemoServerConnectionRepository demoServerConnectionRepository;
    private final SecurityUtils securityUtils;

    @Override
    public String getSetting(final String name) {
        Context context = securityUtils.getCurrentAppContext();
        Product product = context.getProduct();

        if (name.equals("demo-start")) {
           DemoServerConnection demoServerConnection = demoServerConnectionRepository.findById(
                    DemoServerConnectionId.builder()
                            .cloudId(context.getCloudId())
                            .product(product)
                            .build()
            ).orElse(null);

           if (Objects.nonNull(demoServerConnection)) {
               return demoServerConnection.getStartDate();
           } else {
               return null;
           }
        }

        return switch (product) {
            case JIRA -> {
                try {
                    JiraSettings jiraSettings = jiraClient.getSettings(
                            SETTINGS_KEY,
                            xForgeTokenRepository.getXForgeToken(
                                    securityUtils.getCurrentXForgeSystemTokenId(),
                                    XForgeTokenType.SYSTEM
                            )
                    ).block();

                    yield Optional.ofNullable(jiraSettings.getValue().get(name))
                            .map(String::valueOf)
                            .orElse(null);
                } catch (WebClientResponseException e) {
                    if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                        yield null;
                    } else {
                        throw e;
                    }
                }
            }
            case CONFLUENCE -> {
                try {
                    ConfluenceSettings confluenceSettings = confluenceClient.getSettings(
                            SETTINGS_KEY,
                            xForgeTokenRepository.getXForgeToken(
                                    securityUtils.getCurrentXForgeSystemTokenId(),
                                    XForgeTokenType.SYSTEM
                            )
                    );

                    yield Optional.ofNullable(confluenceSettings.getValue().get(name))
                            .map(String::valueOf)
                            .orElse(null);
                } catch (WebClientResponseException e) {
                    if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                        yield null;
                    } else {
                        throw e;
                    }
                }
            }
        };
    }

    @Override
    public void setSetting(final String name, final String value) {
    }
}
