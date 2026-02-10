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

import com.onlyoffice.docs.atlassian.remote.api.JiraContext;
import com.onlyoffice.docs.atlassian.remote.api.XForgeTokenType;
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

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;


@Controller
@RequiredArgsConstructor
@RequestMapping("/editor")
public class EditorController {
    private final ConfigService configService;
    private final SettingsManager settingsManager;
    private final UrlManager urlManager;
    private final XForgeTokenRepository xForgeTokenRepository;

    @Value("${app.editor-session.time-until-expiration}")
    private long editorSessionTimeUntilExpiration;


    @GetMapping(path = "/jira")
    public String editorJiraPage(
            final @RequestParam Mode mode,
            final Model model
    ) throws ParseException {
        JiraContext jiraContext = (JiraContext) SecurityUtils.getCurrentAppContext();

        Config config = configService.createConfig(jiraContext.getAttachmentId(), mode, Type.DESKTOP);
        model.addAttribute("config", config);
        model.addAttribute("documentServerApiUrl", urlManager.getDocumentServerApiUrl());

        model.addAttribute("sessionExpires", getSessionExpires());
        model.addAttribute("settings", Map.of("demo", settingsManager.isDemoActive()));

        return "editor";
    }

    @GetMapping(path = "/jira", params = "format=json")
    public ResponseEntity<EditorResponse> editorJiraData(final @RequestParam Mode mode) throws ParseException {
        JiraContext jiraContext = (JiraContext) SecurityUtils.getCurrentAppContext();

        Config config = configService.createConfig(jiraContext.getAttachmentId(), mode, Type.DESKTOP);

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

        minInstant =  minInstant.minus(editorSessionTimeUntilExpiration, ChronoUnit.MINUTES);

        return minInstant.toEpochMilli();
    }
}
