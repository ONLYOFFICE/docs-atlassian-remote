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

package com.onlyoffice.docs.atlassian.remote.web.controller;

import com.onlyoffice.docs.atlassian.remote.api.ConfluenceContext;
import com.onlyoffice.docs.atlassian.remote.api.Context;
import com.onlyoffice.docs.atlassian.remote.api.JiraContext;
import com.onlyoffice.docs.atlassian.remote.api.Product;
import com.onlyoffice.docs.atlassian.remote.api.XForgeTokenType;
import com.onlyoffice.docs.atlassian.remote.client.confluence.ConfluenceClient;
import com.onlyoffice.docs.atlassian.remote.client.confluence.dto.ConfluenceAttachment;
import com.onlyoffice.docs.atlassian.remote.client.confluence.dto.ConfluenceSettings;
import com.onlyoffice.docs.atlassian.remote.client.confluence.dto.ConfluenceUser;
import com.onlyoffice.docs.atlassian.remote.security.SecurityUtils;
import com.onlyoffice.docs.atlassian.remote.security.XForgeTokenRepository;
import com.onlyoffice.docs.atlassian.remote.web.dto.editor.EditorResponse;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.model.documenteditor.config.document.Type;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.onlyoffice.service.documenteditor.config.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;


@Controller
@RequiredArgsConstructor
@RequestMapping("/editor")
public class EditorController {
    private final ConfigService configService;
    private final SettingsManager settingsManager;
    private final UrlManager urlManager;
    private final XForgeTokenRepository xForgeTokenRepository;
    private final ConfluenceClient confluenceClient;

    @Value("${app.editor-session.time-until-expiration}")
    private long editorSessionTimeUntilExpiration;


    @GetMapping({"jira", "confluence"})
    public String editorPage(
            final @RequestParam Mode mode,
            final Model model
    ) throws ParseException {
        Context context = SecurityUtils.getCurrentAppContext();
        Product product = context.getProduct();

        Config config = switch (product) {
            case JIRA -> {
                JiraContext jiraContext = (JiraContext) context;

                yield configService.createConfig(jiraContext.getAttachmentId(), mode, Type.DESKTOP);
            }
            case CONFLUENCE -> {
                ConfluenceContext confluenceContext = (ConfluenceContext) context;

                preloadConfluenceResources(confluenceContext.getCloudId(), confluenceContext.getAttachmentId());

                yield configService.createConfig(confluenceContext.getAttachmentId(), mode, Type.DESKTOP);
            }
            default -> throw new UnsupportedOperationException("Unsupported product: " + context.getProduct());
        };

        model.addAttribute("config", config);
        model.addAttribute("documentServerApiUrl", urlManager.getDocumentServerApiUrl());

        model.addAttribute("sessionExpires", getSessionExpires());
        model.addAttribute("settings", Map.of("demo", settingsManager.isDemoActive()));

        return "editor";
    }

    @GetMapping(path = {"jira", "confluence"}, params = "format=json")
    public ResponseEntity<EditorResponse> editorData(final @RequestParam Mode mode) throws ParseException {
        Context context = SecurityUtils.getCurrentAppContext();
        Product product = context.getProduct();

        Config config = switch (product) {
            case JIRA -> {
                JiraContext jiraContext = (JiraContext) context;

                yield configService.createConfig(jiraContext.getAttachmentId(), mode, Type.DESKTOP);
            }
            case CONFLUENCE -> {
                ConfluenceContext confluenceContext = (ConfluenceContext) context;

                preloadConfluenceResources(confluenceContext.getCloudId(), confluenceContext.getAttachmentId());

                yield configService.createConfig(confluenceContext.getAttachmentId(), mode, Type.DESKTOP);
            }
            default -> throw new UnsupportedOperationException("Unsupported product: " + context.getProduct());
        };

        return ResponseEntity.ok(new EditorResponse(config, getSessionExpires()));
    }

    private long getSessionExpires() throws ParseException {
        Instant xForgeSystemTokenExpiration = xForgeTokenRepository.getXForgeTokenExpiration(
                SecurityUtils.getCurrentXForgeSystemTokenId(),
                XForgeTokenType.SYSTEM
        );

        Instant xForgeUserTokenExpiration = xForgeTokenRepository.getXForgeTokenExpiration(
                SecurityUtils.getCurrentXForgeUserTokenId(),
                XForgeTokenType.USER
        );

        Instant minInstant = xForgeSystemTokenExpiration.compareTo(xForgeUserTokenExpiration) <= 0
                ? xForgeSystemTokenExpiration : xForgeUserTokenExpiration;

        minInstant.minus(editorSessionTimeUntilExpiration, ChronoUnit.MINUTES);

        return minInstant.toEpochMilli();
    }

    private void preloadConfluenceResources(final UUID cloudId, final String attachmentId) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        String xForgeUserToken = xForgeTokenRepository.getXForgeToken(
                SecurityUtils.getCurrentXForgeUserTokenId(),
                XForgeTokenType.USER
        );
        String xForgeSystemToken = xForgeTokenRepository.getXForgeToken(
                SecurityUtils.getCurrentXForgeSystemTokenId(),
                XForgeTokenType.SYSTEM
        );

        CompletableFuture<ConfluenceUser> confluenceUser = runWithRequestContext(
                requestAttributes,
                () ->
                        confluenceClient.getUser(cloudId.toString(), xForgeUserToken)
        );

        CompletableFuture<ConfluenceAttachment> confluenceAttachment = runWithRequestContext(
                requestAttributes,
                () ->
                        confluenceClient.getAttachment(
                                cloudId,
                                attachmentId,
                                xForgeUserToken
                        )
        );

        CompletableFuture<ConfluenceSettings> confluenceSettings = runWithRequestContext(
                requestAttributes,
                () ->
                        confluenceClient.getSettings(
                                "onlyoffice-docs.settings",
                                xForgeSystemToken
                        )
        );

        CompletableFuture.allOf(confluenceUser, confluenceAttachment, confluenceSettings).join();
    }

    private <T> CompletableFuture<T> runWithRequestContext(
            final RequestAttributes requestAttributes,
            final Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            try {
                return supplier.get();
            } finally {
                RequestContextHolder.resetRequestAttributes();
            }
        });
    }
}
