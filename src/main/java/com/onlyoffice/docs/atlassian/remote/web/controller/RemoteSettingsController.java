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

package com.onlyoffice.docs.atlassian.remote.web.controller;

import com.onlyoffice.docs.atlassian.remote.aop.CurrentFitContext;
import com.onlyoffice.docs.atlassian.remote.aop.CurrentProduct;
import com.onlyoffice.docs.atlassian.remote.api.FitContext;
import com.onlyoffice.docs.atlassian.remote.api.Product;
import com.onlyoffice.docs.atlassian.remote.entity.DemoServerConnection;
import com.onlyoffice.docs.atlassian.remote.entity.DemoServerConnectionId;
import com.onlyoffice.docs.atlassian.remote.repository.DemoServerConnectionRepository;
import com.onlyoffice.docs.atlassian.remote.web.dto.settings.SettingsResponse;
import com.onlyoffice.utils.ConfigurationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/remote/settings")
public class RemoteSettingsController {
    private final DemoServerConnectionRepository demoServerConnectionRepository;

    @GetMapping
    public ResponseEntity<SettingsResponse> getSettings(
            final @CurrentFitContext FitContext fitContext,
            final @CurrentProduct Product product
    ) throws ParseException {
        DemoServerConnectionId demoServerConnectionId = DemoServerConnectionId.builder()
                .cloudId(fitContext.cloudId())
                .product(product)
                .build();

        DemoServerConnection demoServerConnection = demoServerConnectionRepository.findById(demoServerConnectionId)
                .orElse(null);

        if (Objects.isNull(demoServerConnection)) {
            return ResponseEntity.ok(new SettingsResponse(
                    true,
                    null,
                    null
            ));
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        Date startDemo = dateFormat.parse(demoServerConnection.getStartDate());

        Calendar endDemo = Calendar.getInstance();
        endDemo.setTime(startDemo);
        endDemo.add(Calendar.DATE, ConfigurationUtils.getDemoTrialPeriod());

        return ResponseEntity.ok(new SettingsResponse(
                endDemo.after(Calendar.getInstance()),
                startDemo.getTime(),
                endDemo.getTimeInMillis()
        ));
    }

    @PostMapping
    public ResponseEntity<SettingsResponse> saveSettings(
            final @AuthenticationPrincipal Jwt principal,
            final @CurrentFitContext FitContext fitContext,
            final @CurrentProduct Product product
    ) throws ParseException {
        DemoServerConnectionId demoServerConnectionId = DemoServerConnectionId.builder()
                .cloudId(fitContext.cloudId())
                .product(product)
                .build();

        DemoServerConnection demoServerConnection = demoServerConnectionRepository.findById(demoServerConnectionId)
                .orElse(null);

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        if (Objects.isNull(demoServerConnection)) {
            Date date = new Date();

            demoServerConnection = demoServerConnectionRepository.save(
                    DemoServerConnection.builder()
                            .id(demoServerConnectionId)
                            .startDate(dateFormat.format(date))
                            .build()
            );
        }

        Date startDemo = dateFormat.parse(demoServerConnection.getStartDate());

        Calendar endDemo = Calendar.getInstance();
        endDemo.setTime(startDemo);
        endDemo.add(Calendar.DATE, ConfigurationUtils.getDemoTrialPeriod());

        return ResponseEntity.ok(new SettingsResponse(
                endDemo.after(Calendar.getInstance()),
                startDemo.getTime(),
                endDemo.getTimeInMillis()
        ));
    }
}
