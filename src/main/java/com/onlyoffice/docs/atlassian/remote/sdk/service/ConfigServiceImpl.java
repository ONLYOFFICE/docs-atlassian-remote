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

package com.onlyoffice.docs.atlassian.remote.sdk.service;

import com.onlyoffice.docs.atlassian.remote.Constants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.docs.atlassian.remote.api.ConfluenceContext;
import com.onlyoffice.docs.atlassian.remote.api.Context;
import com.onlyoffice.docs.atlassian.remote.api.JiraContext;
import com.onlyoffice.docs.atlassian.remote.api.XForgeTokenType;
import com.onlyoffice.docs.atlassian.remote.client.confluence.ConfluenceClient;
import com.onlyoffice.docs.atlassian.remote.client.confluence.dto.ConfluenceAttachment;
import com.onlyoffice.docs.atlassian.remote.client.confluence.dto.ConfluenceLinks;
import com.onlyoffice.docs.atlassian.remote.client.confluence.dto.ConfluenceOperation;
import com.onlyoffice.docs.atlassian.remote.client.confluence.dto.ConfluenceUser;
import com.onlyoffice.docs.atlassian.remote.client.jira.JiraClient;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraAttachment;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraPermission;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraPermissions;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraPermissionsKey;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraUser;
import com.onlyoffice.docs.atlassian.remote.security.SecurityUtils;
import com.onlyoffice.docs.atlassian.remote.security.XForgeTokenRepository;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.common.User;
import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.model.documenteditor.config.EditorConfig;
import com.onlyoffice.model.documenteditor.config.document.Permissions;
import com.onlyoffice.model.documenteditor.config.document.ReferenceData;
import com.onlyoffice.model.documenteditor.config.document.Type;
import com.onlyoffice.model.documenteditor.config.editorconfig.Customization;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.onlyoffice.model.documenteditor.config.editorconfig.customization.Close;
import com.onlyoffice.service.documenteditor.config.DefaultConfigService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@Component
public class ConfigServiceImpl extends DefaultConfigService {
    private final JiraClient jiraClient;
    private final ConfluenceClient confluenceClient;
    private final XForgeTokenRepository xForgeTokenRepository;
    private final SecurityUtils securityUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConfigServiceImpl(final DocumentManager documentManager,
                             final UrlManager urlManager,
                             final JwtManager jwtManager,
                             final SettingsManager settingsManager, final JiraClient jiraClient,
                             final ConfluenceClient confluenceClient,
                             final XForgeTokenRepository xForgeTokenRepository,
                             final SecurityUtils securityUtils) {
        super(documentManager, urlManager, jwtManager, settingsManager);

        this.xForgeTokenRepository = xForgeTokenRepository;
        this.jiraClient = jiraClient;
        this.confluenceClient = confluenceClient;
        this.securityUtils = securityUtils;
    }

    @Override
    public Config createConfig(final String fileId, final Mode mode, final Type type) {
        Context context = securityUtils.getCurrentAppContext();

        switch (context.getProduct()) {
            case JIRA:
                preloadJiraResources(context.getCloudId(), ((JiraContext) context).getIssueId(), fileId);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported product: " + context.getProduct());
        }

        return super.createConfig(fileId, mode, type);
    }

    @Override
    public EditorConfig getEditorConfig(final String fileId, final Mode mode, final Type type) {
        EditorConfig editorConfig = super.getEditorConfig(fileId, mode, type);

        Context context = securityUtils.getCurrentAppContext();

        switch (context.getProduct()) {
            case JIRA:
                JiraUser jiraUser = jiraClient.getUser(
                        context.getCloudId(),
                        xForgeTokenRepository.getXForgeToken(
                                securityUtils.getCurrentXForgeUserTokenId(),
                                XForgeTokenType.USER
                        )
                ).block();

                editorConfig.setLang(jiraUser.getLocale());

                return editorConfig;
            case CONFLUENCE:
                ConfluenceUser confluenceUser = confluenceClient.getUser(
                        context.getCloudId().toString(),
                        xForgeTokenRepository.getXForgeToken(
                                securityUtils.getCurrentXForgeUserTokenId(),
                                XForgeTokenType.USER
                        )
                ).block();

                editorConfig.setLang(confluenceUser.getLocale());

                return editorConfig;
            default:
                throw new UnsupportedOperationException("Unsupported product: " + context.getProduct());
        }
    }

    @Override
    public ReferenceData getReferenceData(final String fileId) {
        Context context = securityUtils.getCurrentAppContext();

        return switch (context.getProduct()) {
            case JIRA -> super.getReferenceData(fileId);
            case CONFLUENCE -> ReferenceData.builder()
                    .instanceId(securityUtils.getCurrentXForgeSystemTokenId())
                    .fileKey(fileId)
                    .build();
            default -> throw new UnsupportedOperationException("Unsupported product: " + context.getProduct());
        };
    }

    @Override
    public Permissions getPermissions(final String fileId) {
        Context context = securityUtils.getCurrentAppContext();

        switch (context.getProduct()) {
            case JIRA:
                JiraContext jiraContext = (JiraContext) context;

                JiraAttachment jiraAttachment = jiraClient.getAttachment(
                        jiraContext.getCloudId(),
                        fileId,
                        xForgeTokenRepository.getXForgeToken(
                                securityUtils.getCurrentXForgeUserTokenId(),
                                XForgeTokenType.USER
                        )
                ).block();

                JiraPermissions jiraPermissions = jiraClient.getIssuePermissions(
                        jiraContext.getCloudId(),
                        jiraContext.getIssueId(),
                        List.of(
                                JiraPermissionsKey.CREATE_ATTACHMENTS,
                                JiraPermissionsKey.DELETE_OWN_ATTACHMENTS,
                                JiraPermissionsKey.DELETE_ALL_ATTACHMENTS
                        ),
                        xForgeTokenRepository.getXForgeToken(
                                securityUtils.getCurrentXForgeUserTokenId(),
                                XForgeTokenType.USER
                        )
                ).block();

                JiraPermission createAttachments = jiraPermissions.getPermissions()
                        .get(JiraPermissionsKey.CREATE_ATTACHMENTS);

                JiraPermission deleteAttachments;

                if (jiraAttachment.getAuthor().getAccountId()
                        .equals(securityUtils.getCurrentAccountId())) {
                    deleteAttachments = jiraPermissions.getPermissions()
                            .get(JiraPermissionsKey.DELETE_OWN_ATTACHMENTS);
                } else {
                    deleteAttachments = jiraPermissions.getPermissions()
                            .get(JiraPermissionsKey.DELETE_ALL_ATTACHMENTS);
                }

                return Permissions.builder()
                        .edit(createAttachments.isHavePermission() && deleteAttachments.isHavePermission())
                        .build();
            case CONFLUENCE:
                ConfluenceContext confluenceContext = (ConfluenceContext) context;

                ConfluenceAttachment confluenceAttachment = confluenceClient.getAttachment(
                        confluenceContext.getCloudId(),
                        fileId,
                        xForgeTokenRepository.getXForgeToken(
                                securityUtils.getCurrentXForgeUserTokenId(),
                                XForgeTokenType.USER
                        )
                ).block();

                Map<String, Object> operations = confluenceAttachment.getOperations();
                boolean canEdit = false;
                if (!operations.isEmpty()) {
                    List<ConfluenceOperation> permittedOperations = objectMapper.convertValue(
                            operations.get("results"),
                            new TypeReference<List<ConfluenceOperation>>() { }
                    );

                    canEdit = permittedOperations.stream()
                            .anyMatch(operation -> "update".equals(operation.getOperation())
                                    && "attachment".equals(operation.getTargetType()));
                }

                return Permissions.builder()
                        .edit(canEdit)
                        .build();
            default:
                throw new UnsupportedOperationException("Unsupported product: " + context.getProduct());
        }
    }

    @Override
    public Customization getCustomization(final String fileId) {
        Context context = securityUtils.getCurrentAppContext();
        Customization customization = super.getCustomization(fileId);

        switch (context.getProduct()) {
            case JIRA:
                customization.setClose(
                        Close.builder()
                                .visible(true)
                                .build()
                );
                break;
            case CONFLUENCE:
                customization.getGoback().setRequestClose(true);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported product: " + context.getProduct());
        }

        return customization;
    }

    @Override
    public User getUser() {
        Context context = securityUtils.getCurrentAppContext();

        switch (context.getProduct()) {
            case JIRA:
                JiraUser jiraUser = jiraClient.getUser(
                        context.getCloudId(),
                        xForgeTokenRepository.getXForgeToken(
                                securityUtils.getCurrentXForgeUserTokenId(),
                                XForgeTokenType.USER
                        )
                ).block();

                return User.builder()
                        .id(jiraUser.getAccountId())
                        .name(jiraUser.getDisplayName())
                        .image(jiraUser.getAvatarUrls().get("24x24"))
                        .build();
            case CONFLUENCE:
                ConfluenceUser confluenceUser = confluenceClient.getUser(
                        context.getCloudId().toString(),
                        xForgeTokenRepository.getXForgeToken(
                                securityUtils.getCurrentXForgeUserTokenId(),
                                XForgeTokenType.USER
                        )
                ).block();

                ConfluenceLinks links = confluenceUser.get_links();
                String baseUrl = links.getBase();
                if (baseUrl.endsWith(links.getContext())) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - links.getContext().length());
                }

                return User.builder()
                        .id(confluenceUser.getAccountId())
                        .name(confluenceUser.getDisplayName())
                        .image(baseUrl + confluenceUser.getProfilePicture().getPath())
                        .build();
            default:
                throw new UnsupportedOperationException("Unsupported product: " + context.getProduct());
        }
    }

    private void preloadJiraResources(final UUID cloudId, final String issueId, final String attachmentId) {
        String xForgeUserToken = xForgeTokenRepository.getXForgeToken(
                securityUtils.getCurrentXForgeUserTokenId(),
                XForgeTokenType.USER
        );
        String xForgeSystemToken = xForgeTokenRepository.getXForgeToken(
                securityUtils.getCurrentXForgeSystemTokenId(),
                XForgeTokenType.SYSTEM
        );

        Mono.zip(
                jiraClient.getUser(cloudId, xForgeUserToken),
                jiraClient.getAttachment(cloudId, attachmentId, xForgeUserToken),
                jiraClient.getIssuePermissions(
                        cloudId,
                        issueId,
                        List.of(
                                JiraPermissionsKey.CREATE_ATTACHMENTS,
                                JiraPermissionsKey.DELETE_OWN_ATTACHMENTS,
                                JiraPermissionsKey.DELETE_ALL_ATTACHMENTS
                        ),
                        xForgeUserToken
                ),
                jiraClient.getSettings(Constants.SETTINGS_KEY, xForgeSystemToken)
        ).block();
    }
}
