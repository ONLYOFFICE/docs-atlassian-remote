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

package com.onlyoffice.docs.atlassian.remote.sdk.manager;

import com.onlyoffice.docs.atlassian.remote.api.Context;
import com.onlyoffice.docs.atlassian.remote.api.JiraContext;
import com.onlyoffice.docs.atlassian.remote.api.XForgeTokenType;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraAttachment;
import com.onlyoffice.docs.atlassian.remote.client.jira.JiraClient;
import com.onlyoffice.docs.atlassian.remote.security.SecurityUtils;
import com.onlyoffice.docs.atlassian.remote.security.XForgeTokenRepository;
import com.onlyoffice.manager.document.DefaultDocumentManager;
import com.onlyoffice.manager.settings.SettingsManager;
import org.springframework.stereotype.Component;


@Component
public class DocumentManagerImpl extends DefaultDocumentManager {
    private final JiraClient jiraClient;
    private final XForgeTokenRepository xForgeTokenRepository;

    public DocumentManagerImpl(final SettingsManager settingsManager, final JiraClient jiraClient,
                               final XForgeTokenRepository xForgeTokenRepository) {
        super(settingsManager);

        this.jiraClient = jiraClient;
        this.xForgeTokenRepository = xForgeTokenRepository;
    }

    @Override
    public String getDocumentKey(final String fileId, final boolean embedded) {
        Context context = SecurityUtils.getCurrentAppContext();

        return String.format(
                "%s_%s_%s",
                context.getProduct(),
                context.getCloudId(),
                fileId
        );
    }

    @Override
    public String getDocumentName(final String fileId) {
        Context context = SecurityUtils.getCurrentAppContext();

        switch (context.getProduct()) {
            case JIRA:
                JiraAttachment attachment = getJiraAttachment(fileId);

                return attachment.getFilename();
            default:
                throw new UnsupportedOperationException("Unsupported product: " + context.getProduct());
        }
    }

    private JiraAttachment getJiraAttachment(final String attachmentId) {
        JiraContext jiraContext = (JiraContext) SecurityUtils.getCurrentAppContext();

        return jiraClient.getAttachment(
                jiraContext.getCloudId(),
                attachmentId,
                xForgeTokenRepository.getXForgeToken(SecurityUtils.getCurrentXForgeUserTokenId(), XForgeTokenType.USER)
        );
    }
}
