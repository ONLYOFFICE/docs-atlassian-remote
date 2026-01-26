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

import com.onlyoffice.docs.atlassian.remote.aop.CurrentFitContext;
import com.onlyoffice.docs.atlassian.remote.aop.CurrentProduct;
import com.onlyoffice.docs.atlassian.remote.api.FitContext;
import com.onlyoffice.docs.atlassian.remote.api.Product;
import com.onlyoffice.docs.atlassian.remote.client.confluence.ConfluenceClient;
import com.onlyoffice.docs.atlassian.remote.client.confluence.dto.ConfluenceAttachment;
import com.onlyoffice.docs.atlassian.remote.client.jira.JiraClient;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraAttachment;
import com.onlyoffice.docs.atlassian.remote.web.dto.create.CreateRequest;
import com.onlyoffice.docs.atlassian.remote.web.dto.create.CreateResponse;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.model.documenteditor.config.document.DocumentType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;


@RestController
@RequestMapping("/api/v1/remote/create")
@RequiredArgsConstructor
public class RemoteCreateController {
    private final DocumentManager documentManager;
    private final JiraClient jiraClient;
    private final ConfluenceClient confluenceClient;

    @PostMapping
    public ResponseEntity<CreateResponse> createAttachment(
            final @CurrentFitContext FitContext fitContext,
            final @CurrentProduct Product product,
            final @RequestHeader("x-forge-oauth-user") String xForgeUserToken,
            final @Valid @RequestBody CreateRequest request
    ) {
        String parentId = request.getParentId();
        String title = request.getTitle();
        DocumentType documentType = request.getDocumentType();
        String locale = request.getLocale();

        String fileExtension = documentManager.getDefaultExtension(documentType);

        InputStream newBlankFile = documentManager.getNewBlankFile(
                fileExtension,
                Locale.forLanguageTag(locale)
        );

        return switch (product) {
            case JIRA -> {
                List<JiraAttachment> newAttachments = jiraClient.createAttachment(
                        fitContext.cloudId(),
                        parentId,
                        toDataBufferFlux(newBlankFile),
                        title + "." + fileExtension,
                        xForgeUserToken
                );

                yield ResponseEntity.ok(
                        new CreateResponse(
                                String.valueOf(newAttachments.getFirst().getId()),
                                newAttachments.getFirst().getFilename()
                        )
                );
            }
            case CONFLUENCE -> {
                List<ConfluenceAttachment> newAttachments = confluenceClient.createAttachment(
                        fitContext.cloudId(),
                        parentId,
                        toDataBufferFlux(newBlankFile),
                        title + "." + fileExtension,
                        xForgeUserToken
                );

                yield ResponseEntity.ok(
                        new CreateResponse(
                                String.valueOf(newAttachments.getFirst().getId()),
                                newAttachments.getFirst().getTitle()
                        )
                );
            }
            default -> throw new UnsupportedOperationException("Unsupported product: " + product);
        };
    }

    private static Flux<DataBuffer> toDataBufferFlux(final InputStream inputStream) {
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();

        return DataBufferUtils.readInputStream(
                () -> inputStream,
                factory,
                IOUtils.DEFAULT_BUFFER_SIZE
        );
    }
}
