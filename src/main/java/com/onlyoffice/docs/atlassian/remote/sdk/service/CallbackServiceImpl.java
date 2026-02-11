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

import com.onlyoffice.docs.atlassian.remote.api.ConfluenceContentReference;
import com.onlyoffice.docs.atlassian.remote.api.ConfluenceContext;
import com.onlyoffice.docs.atlassian.remote.api.Context;
import com.onlyoffice.docs.atlassian.remote.api.JiraContext;
import com.onlyoffice.docs.atlassian.remote.api.XForgeTokenType;
import com.onlyoffice.docs.atlassian.remote.client.confluence.ConfluenceClient;
import com.onlyoffice.docs.atlassian.remote.client.ds.DocumentServerClient;
import com.onlyoffice.docs.atlassian.remote.client.jira.JiraClient;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraAttachment;
import com.onlyoffice.docs.atlassian.remote.security.SecurityUtils;
import com.onlyoffice.docs.atlassian.remote.security.XForgeTokenRepository;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.service.documenteditor.callback.DefaultCallbackService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;


@Component
public class CallbackServiceImpl extends DefaultCallbackService {
    private final DocumentServerClient documentServerClient;
    private final JiraClient jiraClient;
    private final ConfluenceClient confluenceClient;
    private final XForgeTokenRepository xForgeTokenRepository;
    private final SecurityUtils securityUtils;


    public CallbackServiceImpl(final JwtManager jwtManager,
                               final SettingsManager settingsManager,
                               final DocumentServerClient documentServerClient,
                               final JiraClient jiraClient,
                               final ConfluenceClient confluenceClient,
                               final XForgeTokenRepository xForgeTokenRepository,
                               final SecurityUtils securityUtils) {
        super(jwtManager, settingsManager);

        this.documentServerClient = documentServerClient;
        this.jiraClient = jiraClient;
        this.confluenceClient = confluenceClient;
        this.xForgeTokenRepository = xForgeTokenRepository;
        this.securityUtils = securityUtils;
    }

    @Override
    public void handlerSave(final Callback callback, final String fileId) throws Exception {
        Context context = securityUtils.getCurrentAppContext();
        String url = callback.getUrl();

        switch (context.getProduct()) {
            case JIRA:
                JiraContext jiraContext = (JiraContext) context;

                JiraAttachment jiraAttachment = jiraClient.getAttachment(
                        jiraContext.getCloudId(),
                        jiraContext.getAttachmentId(),
                        xForgeTokenRepository.getXForgeToken(
                                securityUtils.getCurrentXForgeUserTokenId(),
                                XForgeTokenType.USER
                        )
                ).block();

                Flux<DataBuffer> file = documentServerClient.getFile(url);

                jiraClient.createAttachment(
                        jiraContext.getCloudId(),
                        jiraContext.getIssueId(),
                        file,
                        jiraAttachment.getFilename(),
                        xForgeTokenRepository.getXForgeToken(
                                securityUtils.getCurrentXForgeUserTokenId(),
                                XForgeTokenType.USER
                        )
                );

                jiraClient.deleteAttachment(
                        jiraContext.getCloudId(),
                        jiraContext.getAttachmentId(),
                        xForgeTokenRepository.getXForgeToken(
                                securityUtils.getCurrentXForgeUserTokenId(),
                                XForgeTokenType.USER
                        )
                );
                break;
            case CONFLUENCE:
                ConfluenceContext confluenceContext = (ConfluenceContext) context;
                ConfluenceContentReference confluenceContentReference = ConfluenceContentReference.parse(
                        confluenceContext.getParentId());

                Flux<DataBuffer> newFile = documentServerClient.getFile(url);

                confluenceClient.updateAttachmentData(
                        confluenceContext.getCloudId(),
                        confluenceContentReference.getId(),
                        confluenceContext.getAttachmentId(),
                        newFile,
                        xForgeTokenRepository.getXForgeToken(
                                securityUtils.getCurrentXForgeUserTokenId(),
                                XForgeTokenType.USER
                        )
                );

                break;
            default:
                throw new UnsupportedOperationException("Unsupported product: " + context.getProduct());
        }
    }
}
