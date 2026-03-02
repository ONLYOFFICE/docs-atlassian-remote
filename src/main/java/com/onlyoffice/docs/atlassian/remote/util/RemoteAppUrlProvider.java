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


package com.onlyoffice.docs.atlassian.remote.util;

import com.onlyoffice.docs.atlassian.remote.api.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;


@Component
public class RemoteAppUrlProvider {
    @Value("${app.base-url}")
    private String baseUrl;

    public URI getDownloadUrl(final Product product) {
        return UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/api/v1/download/" + product.toString().toLowerCase())
                .build()
                .toUri();
    }

    public URI getCallbackUrl(final Product product) {
        return UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/api/v1/callback/" + product.toString().toLowerCase())
                .build()
                .toUri();
    }
}
