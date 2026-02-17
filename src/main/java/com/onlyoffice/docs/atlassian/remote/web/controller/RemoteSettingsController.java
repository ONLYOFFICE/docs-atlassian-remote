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

import com.onlyoffice.docs.atlassian.remote.api.Context;
import com.onlyoffice.docs.atlassian.remote.entity.DemoServerConnection;
import com.onlyoffice.docs.atlassian.remote.entity.DemoServerConnectionId;
import com.onlyoffice.docs.atlassian.remote.service.DemoServerConnectionService;
import com.onlyoffice.docs.atlassian.remote.security.SecurityUtils;
import com.onlyoffice.docs.atlassian.remote.web.dto.settings.SettingsResponse;
import com.onlyoffice.utils.ConfigurationUtils;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
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
    private final SecurityUtils securityUtils;
    private final DemoServerConnectionService demoServerConnectionService;

    @GetMapping
    public ResponseEntity<SettingsResponse> getSettings() throws ParseException {
        Context context = securityUtils.getCurrentAppContext();
        DemoServerConnectionId demoServerConnectionId = DemoServerConnectionId.builder()
                .cloudId(context.getCloudId())
                .product(context.getProduct())
                .build();

        DemoServerConnection demoServerConnection = demoServerConnectionService.findById(demoServerConnectionId);

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
    public ResponseEntity<SettingsResponse> saveSettings() throws ParseException {
        Context context = securityUtils.getCurrentAppContext();
        DemoServerConnectionId demoServerConnectionId = DemoServerConnectionId.builder()
                .cloudId(context.getCloudId())
                .product(context.getProduct())
                .build();

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        DemoServerConnection demoServerConnection;
        try {
            demoServerConnection = demoServerConnectionService.create(
                    demoServerConnectionId,
                    dateFormat.format(new Date())
            );
        } catch (EntityExistsException | DataIntegrityViolationException e) {
            demoServerConnection = demoServerConnectionService.findById(demoServerConnectionId);
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
