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

import com.onlyoffice.docs.atlassian.remote.api.BitbucketContext;
import com.onlyoffice.docs.atlassian.remote.api.BitbucketFileId;
import com.onlyoffice.docs.atlassian.remote.api.ConfluenceContext;
import com.onlyoffice.docs.atlassian.remote.api.ConfluenceFileId;
import com.onlyoffice.docs.atlassian.remote.api.Context;
import com.onlyoffice.docs.atlassian.remote.api.JiraContext;
import com.onlyoffice.docs.atlassian.remote.api.JiraFileId;
import com.onlyoffice.docs.atlassian.remote.api.Product;
import com.onlyoffice.docs.atlassian.remote.api.XForgeTokenType;
import com.onlyoffice.docs.atlassian.remote.client.confluence.ConfluenceClient;
import com.onlyoffice.docs.atlassian.remote.client.confluence.dto.ConfluenceContent;
import com.onlyoffice.docs.atlassian.remote.configuration.ForgeProperties;
import com.onlyoffice.docs.atlassian.remote.security.RemoteAppJwtService;
import com.onlyoffice.docs.atlassian.remote.security.SecurityUtils;
import com.onlyoffice.docs.atlassian.remote.security.XForgeTokenRepository;
import com.onlyoffice.docs.atlassian.remote.util.RemoteAppUrlProvider;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.DefaultUrlManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;


@Component
public class UrlManagerImpl extends DefaultUrlManager {
    private final ConfluenceClient confluenceClient;
    private final XForgeTokenRepository xForgeTokenRepository;
    private final RemoteAppUrlProvider remoteAppUrlProvider;
    private final RemoteAppJwtService remoteAppJwtService;
    private final ForgeProperties forgeProperties;
    private final SecurityUtils securityUtils;

    @Value("${app.security.ttl.callback}")
    private long ttlCallback;

    public UrlManagerImpl(final SettingsManager settingsManager,
                          final ConfluenceClient confluenceClient,
                          final XForgeTokenRepository xForgeTokenRepository,
                          final RemoteAppUrlProvider remoteAppUrlProvider,
                          final RemoteAppJwtService remoteAppJwtService,
                          final ForgeProperties forgeProperties,
                          final SecurityUtils securityUtils
    ) {
        super(settingsManager);

        this.xForgeTokenRepository = xForgeTokenRepository;
        this.remoteAppUrlProvider = remoteAppUrlProvider;
        this.confluenceClient = confluenceClient;
        this.remoteAppJwtService = remoteAppJwtService;
        this.forgeProperties = forgeProperties;
        this.securityUtils = securityUtils;
    }

    @Override
    public String getFileUrl(final String fileId) {
        Context context = securityUtils.getCurrentAppContext();

        return remoteAppJwtService.signUri(
                remoteAppUrlProvider.getDownloadUrl(context.getProduct()),
                securityUtils.getCurrentAccountId(),
                buildTokenContext(context, fileId)
        ).toString();
    }

    @Override
    public String getCallbackUrl(final String fileId) {
        Context context = securityUtils.getCurrentAppContext();

        return remoteAppJwtService.signUri(
                remoteAppUrlProvider.getCallbackUrl(context.getProduct()),
                securityUtils.getCurrentAccountId(),
                buildTokenContext(context, fileId),
                ttlCallback
        ).toString();
    }

    @Override
    public String getGobackUrl(final String fileId) {
        Context context = securityUtils.getCurrentAppContext();
        Product product = context.getProduct();

        switch (product) {
            case JIRA, BITBUCKET -> {
                return null;
            }
            case CONFLUENCE -> {
                ConfluenceFileId confluenceFileId = ConfluenceFileId.parse(fileId);

                ConfluenceContent content = confluenceClient.getContent(
                        context.getCloudId(),
                        confluenceFileId.getParentContentType(),
                        confluenceFileId.getParentId(),
                        xForgeTokenRepository.getXForgeToken(
                                securityUtils.getCurrentXForgeUserTokenId(),
                                XForgeTokenType.USER
                        )
                ).block();

                Optional<String> blogsFilter = Optional.ofNullable(
                        "blogpost".equals(confluenceFileId.getParentContentType()) ? "blogs" : null);

                return UriComponentsBuilder
                        .fromUriString(content.get_links().getBase())
                        .path(content.get_links().getWebui().replaceAll("(/spaces/~[^/]+).*", "$1"))
                        .path("/apps/{appId}/{environmentId}/onlyoffice")
                        .queryParamIfPresent("filter", blogsFilter)
                        .queryParam("parentId", confluenceFileId.getParentId())
                        .buildAndExpand(
                                forgeProperties.getAppIdByProductWithoutPrefix(product),
                                context.getEnvironmentId()
                        )
                        .toUriString();
            }
            default -> throw new UnsupportedOperationException("Unsupported product: " + context.getProduct());
        }
    }

    private Context buildTokenContext(final Context context, final String fileId) {
        return switch (context.getProduct()) {
            case JIRA -> {
                JiraFileId jiraFileId = JiraFileId.parse(fileId);

                yield JiraContext.builder()
                        .product(context.getProduct())
                        .cloudId(context.getCloudId())
                        .environmentId(context.getEnvironmentId())
                        .issueId(jiraFileId.getIssueId())
                        .attachmentId(jiraFileId.getAttachmentId())
                        .build();
            }
            case CONFLUENCE -> {
                ConfluenceFileId confluenceFileId = ConfluenceFileId.parse(fileId);

                yield ConfluenceContext.builder()
                        .product(context.getProduct())
                        .cloudId(context.getCloudId())
                        .environmentId(context.getEnvironmentId())
                        .parentId(confluenceFileId.getParentContentType() + ":" + confluenceFileId.getParentId())
                        .attachmentId(confluenceFileId.getAttachmentId())
                        .build();
            }
            case BITBUCKET -> {
                BitbucketFileId bitbucketFileId = BitbucketFileId.parse(fileId);

                yield BitbucketContext.builder()
                        .product(context.getProduct())
                        .cloudId(context.getCloudId())
                        .environmentId(context.getEnvironmentId())
                        .repositoryId(bitbucketFileId.getRepositoryId())
                        .fileId(bitbucketFileId.getCommit() + ":" + bitbucketFileId.getFilePath())
                        .locale(bitbucketFileId.getLocale())
                        .build();
            }
            default -> throw new UnsupportedOperationException("Unsupported product: " + context.getProduct());
        };
    }
}
