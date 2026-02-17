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

package com.onlyoffice.docs.atlassian.remote.configuration;

import com.onlyoffice.docs.atlassian.remote.api.Product;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;


@Component
@ConfigurationProperties(prefix = "forge")
@Getter
@Setter
public class ForgeProperties {
    private Map<Product, ProductConfig> products = new EnumMap<>(Product.class);
    private TokenConfig token;

    public Product getProductByAppId(final String appId) {
        if (Objects.isNull(appId)) {
            return null;
        }

        return products.entrySet()
                .stream()
                .filter(productConfig -> appId.equals(productConfig.getValue().getAppId()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    @Setter
    @Getter
    public static class ProductConfig {
        private String appId;
    }

    @Setter
    @Getter
    public static class TokenConfig {
        private TokenTypeConfig system;
        private TokenTypeConfig user;
    }

    @Setter
    @Getter
    public static class TokenTypeConfig {
        private String header;
        private Duration refreshThreshold;
    }
}
