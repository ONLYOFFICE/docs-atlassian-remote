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

package com.onlyoffice.docs.atlassian.remote.client.ds;

import com.onlyoffice.manager.url.UrlManager;
import lombok.RequiredArgsConstructor;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;


@Component
@RequiredArgsConstructor
public class DocumentServerClient {
    private final WebClient documentSeverWebClient;
    private final UrlManager urlManager;

    public Flux<DataBuffer> getFile(final String url) {
        String relativeFileUri = stripDocumentServerUrl(url);

        URI uri = createUri(
                urlManager.getInnerDocumentServerUrl(),
                relativeFileUri,
                Collections.emptyList()
        );

        return documentSeverWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToFlux(DataBuffer.class);
    }

    protected URI createUri(final String baseUrl, final String path, final List<NameValuePair> parameters) {
        try {
            URIBuilder pathUri = new URIBuilder(path);

            return new URIBuilder(baseUrl)
                    .appendPath(pathUri.getPath())
                    .addParameters(pathUri.getQueryParams())
                    .addParameters(parameters)
                    .build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    protected String stripDocumentServerUrl(final String fullUrl) {
        String documentServerUrl = urlManager.getDocumentServerUrl();

        if (fullUrl == null || documentServerUrl == null) {
            return fullUrl;
        }

        if (!fullUrl.startsWith(documentServerUrl)) {
            return fullUrl;
        }

        return fullUrl.substring(documentServerUrl.length());
    }
}
