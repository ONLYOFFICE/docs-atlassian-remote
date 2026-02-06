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

import com.onlyoffice.docs.atlassian.remote.api.ConfluenceContentReference;
import com.onlyoffice.docs.atlassian.remote.api.ConfluenceContext;
import com.onlyoffice.docs.atlassian.remote.api.Context;
import com.onlyoffice.docs.atlassian.remote.api.XForgeTokenType;
import com.onlyoffice.docs.atlassian.remote.client.confluence.ConfluenceClient;
import com.onlyoffice.docs.atlassian.remote.client.confluence.dto.ConfluenceAttachment;
import com.onlyoffice.docs.atlassian.remote.sdk.Utils;
import com.onlyoffice.docs.atlassian.remote.security.RemoteAppJwtService;
import com.onlyoffice.docs.atlassian.remote.security.SecurityUtils;
import com.onlyoffice.docs.atlassian.remote.security.XForgeTokenRepository;
import com.onlyoffice.docs.atlassian.remote.util.RemoteAppUrlProvider;
import com.onlyoffice.docs.atlassian.remote.web.dto.referencedata.ReferenceDataRequest;
import com.onlyoffice.docs.atlassian.remote.web.dto.referencedata.ReferenceDataResponse;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.documenteditor.config.document.ReferenceData;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/remote/reference-data")
@RequiredArgsConstructor
public class RemoteReferenceDataController {
    private final ConfluenceClient confluenceClient;
    private final DocumentManager documentManager;
    private final RemoteAppJwtService remoteAppJwtService;
    private final RemoteAppUrlProvider remoteAppUrlProvider;
    private final XForgeTokenRepository xForgeTokenRepository;
    private final SettingsManager settingsManager;
    private final JwtManager jwtManager;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<ReferenceDataResponse> referenceData(
            final @RequestParam String parentId,
            final @Valid @RequestBody ReferenceDataRequest request
    ) {
        Context context = securityUtils.getCurrentAppContext();

        ReferenceDataResponse referenceDataResponse = switch (context.getProduct()) {
            case CONFLUENCE -> {
                yield createConfluenceReferenceData(parentId, request);
            }
            default -> throw new UnsupportedOperationException("Unsupported product: " + context.getProduct());

        };

        if (settingsManager.isSecurityEnabled()) {
            String token = jwtManager.createToken(referenceDataResponse);
            referenceDataResponse.setToken(token);
        }

        return ResponseEntity.ok(referenceDataResponse);
    }

    private ReferenceDataResponse createConfluenceReferenceData(final String parentId,
                                                                final ReferenceDataRequest request) {
        Context context = securityUtils.getCurrentAppContext();
        ReferenceData referenceData = request.getReferenceData();

        String attachmentId = null;
        if (!Objects.isNull(referenceData)) {
            String currentInstanceId = securityUtils.getCurrentXForgeSystemTokenId();

            if (currentInstanceId.equals(referenceData.getInstanceId())) {
                attachmentId = referenceData.getFileKey();
            }
        }

        String xForgeUserToken = xForgeTokenRepository.getXForgeToken(
                securityUtils.getCurrentXForgeUserTokenId(),
                XForgeTokenType.USER
        );
        ConfluenceAttachment confluenceAttachment = null;
        if (!Objects.isNull(attachmentId) && !attachmentId.isEmpty()) {
            try {
                confluenceAttachment = confluenceClient.getAttachment(
                        context.getCloudId(),
                        attachmentId,
                        xForgeUserToken
                );
            } catch (WebClientResponseException e) {
                if (!HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                    throw e;
                }
            }
        }

        if (Objects.isNull(confluenceAttachment)) {
            ConfluenceContentReference confluenceContentReference = ConfluenceContentReference.parse(parentId);
            confluenceAttachment = confluenceClient.getAttachmentsForContent(
                    context.getCloudId(),
                    confluenceContentReference.getContentType(),
                    confluenceContentReference.getId(),
                    request.getPath(),
                    xForgeUserToken
            ).getFirst();
        }

        if (!Objects.isNull(confluenceAttachment)) {
            referenceData = ReferenceData.builder()
                    .fileKey(confluenceAttachment.getId())
                    .instanceId(securityUtils.getCurrentXForgeSystemTokenId())
                    .build();
        }

        String documentName = confluenceAttachment.getTitle();
        ConfluenceContext confluenceContext = ConfluenceContext.builder()
                .product(context.getProduct())
                .cloudId(context.getCloudId())
                .environmentId(context.getEnvironmentId())
                .parentId(parentId)
                .attachmentId(confluenceAttachment.getId())
                .build();


        return ReferenceDataResponse.builder()
                .key(Utils.createConfluenceDocumentKey(context.getCloudId(), confluenceAttachment))
                .fileType(documentManager.getExtension(documentName))
                .path(documentName)
                .referenceData(referenceData)
                .url(remoteAppJwtService.signUri(
                        remoteAppUrlProvider.getDownloadUrl(context.getProduct()),
                        securityUtils.getCurrentAccountId(),
                        confluenceContext
                ).toString())
                .build();
    }
}
