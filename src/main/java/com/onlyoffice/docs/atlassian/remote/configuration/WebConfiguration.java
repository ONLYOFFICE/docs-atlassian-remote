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

package com.onlyoffice.docs.atlassian.remote.configuration;

import com.onlyoffice.docs.atlassian.remote.web.resolver.AccountIdArgumentResolver;
import com.onlyoffice.docs.atlassian.remote.web.resolver.FitContextArgumentResolver;
import com.onlyoffice.docs.atlassian.remote.web.resolver.ProductArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;


@Configuration
@RequiredArgsConstructor
public class WebConfiguration implements WebMvcConfigurer {
    private final FitContextArgumentResolver fitContextArgumentResolver;
    private final ProductArgumentResolver productArgumentResolver;
    private final AccountIdArgumentResolver accountIdArgumentResolver;

    @Override
    public void addArgumentResolvers(final List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.addAll(List.of(
                fitContextArgumentResolver,
                productArgumentResolver,
                accountIdArgumentResolver
        ));
    }
}
