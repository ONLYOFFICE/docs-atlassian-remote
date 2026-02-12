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
import com.onlyoffice.docs.atlassian.remote.security.SecurityUtils;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.model.documenteditor.config.document.Type;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.onlyoffice.service.documenteditor.config.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.ParseException;
import java.util.Map;


@Controller
@RequiredArgsConstructor
@RequestMapping("/editor")
public class EditorController {
    private final ConfigService configService;
    private final SettingsManager settingsManager;
    private final UrlManager urlManager;
    private final SecurityUtils securityUtils;

    @GetMapping(path = "/jira")
    public String editorJiraPage(
            final @RequestParam Mode mode,
            final Model model
    ) throws ParseException {
        JiraContext jiraContext = (JiraContext) securityUtils.getCurrentAppContext();

        Config config = configService.createConfig(jiraContext.getAttachmentId(), mode, Type.DESKTOP);
        model.addAttribute("config", config);
        model.addAttribute("documentServerApiUrl", urlManager.getDocumentServerApiUrl());

        model.addAttribute("sessionExpires", securityUtils.getSessionExpires());
        model.addAttribute("settings", Map.of("demo", settingsManager.isDemoActive()));

        return "editor";
    }
}
