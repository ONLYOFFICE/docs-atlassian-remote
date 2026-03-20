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

import com.onlyoffice.docs.atlassian.remote.api.BitbucketFileId;
import com.onlyoffice.docs.atlassian.remote.api.ConfluenceFileId;
import com.onlyoffice.docs.atlassian.remote.api.JiraFileId;
import com.onlyoffice.docs.atlassian.remote.security.SecurityUtils;
import com.onlyoffice.docs.atlassian.remote.web.dto.editorconfig.EditorConfigResponse;
import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.model.documenteditor.config.document.Type;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.onlyoffice.service.documenteditor.config.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.ParseException;


@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1/remote/editor-config")
public class RemoteEditorConfigController {
    private final ConfigService configService;
    private final SecurityUtils securityUtils;

    @GetMapping("/jira")
    public ResponseEntity<EditorConfigResponse> getJiraEditorConfig(
            final @RequestParam String issueId,
            final @RequestParam String attachmentId,
            final @RequestParam Mode mode
    ) throws ParseException {
        JiraFileId jiraFileId = JiraFileId.parse(
                issueId,
                attachmentId
        );

        Config config = configService.createConfig(jiraFileId.toString(), mode, Type.DESKTOP);

        return ResponseEntity.ok(
                new EditorConfigResponse(
                        config,
                        securityUtils.getSessionExpires().toEpochMilli()
                )
        );
    }

    @GetMapping("/confluence")
    public ResponseEntity<EditorConfigResponse> getConfluenceEditorConfig(
            final @RequestParam String parentId,
            final @RequestParam String attachmentId,
            final @RequestParam Mode mode
    ) throws ParseException {
        ConfluenceFileId confluenceFileId = ConfluenceFileId.parse(
                parentId,
                attachmentId
        );

        Config config = configService.createConfig(confluenceFileId.toString(), mode, Type.DESKTOP);

        return ResponseEntity.ok(
                new EditorConfigResponse(
                        config,
                        securityUtils.getSessionExpires().toEpochMilli()
                )
        );
    }

    @GetMapping("/bitbucket")
    public ResponseEntity<EditorConfigResponse> getBitbucketEditorConfig(
            final @RequestParam String repositoryId,
            final @RequestParam String commit,
            final @RequestParam String filePath,
            final @RequestParam String locale
    ) throws ParseException {
        BitbucketFileId bitbucketFileId = new BitbucketFileId(
                repositoryId,
                commit,
                filePath,
                locale
        );

        Config config = configService.createConfig(bitbucketFileId.toString(), Mode.VIEW, Type.DESKTOP);

        return ResponseEntity.ok(
                new EditorConfigResponse(
                        config,
                        securityUtils.getSessionExpires().toEpochMilli()
                )
        );
    }
}
