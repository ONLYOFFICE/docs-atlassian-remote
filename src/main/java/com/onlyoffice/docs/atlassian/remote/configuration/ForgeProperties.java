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

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import static com.onlyoffice.docs.atlassian.remote.Constants.APP_ID_PREFIX;


@Component
@ConfigurationProperties(prefix = "forge")
@Getter
@Setter
public class ForgeProperties {
    private Map<Product, ProductConfig> products = new EnumMap<>(Product.class);

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

    public String getAppIdByProductWithoutPrefix(final Product product) {
        String appId = getAppIdByProduct(product);

        return appId.substring(APP_ID_PREFIX.length());
    }

    public String getAppIdByProduct(final Product product) {
        return products.get(product).getAppId();
    }

    @Setter
    @Getter
    public static class ProductConfig {
        private String appId;

    }
}
