/*
 * Copyright 2021 “J-IT“ IT-Dienstleistungs GesmbH, Vienna, Austria
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
 */

package at.jit.incidentlistener;

import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.incident.IncidentHandler;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class CustomIncidentHandlerPluginTest {
    public static final String INTERVAL_MS = "1000";
    public static final String PORT = "587";
    public static final String URL = "http://localhost:8080/camunda";
    public static final String FALLBACK_MAIL_RECEIVER = "fallbackreceiver@provider.com";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String SUBJECT = "subject";
    public static final String HOST = "smpt.gmail.com";
    public static final String MAIL_SENDER = "mailsender@provider.com";
    public static final String MAIL_BODY_TEMPLATE = "mailBodyTemplate";
    public static final String INCIDENT_TEMPLATE = "incidentTemplate";

    @Test
    public void givenConfig_whenPreInit_thenCallTheRightMethods() {
        // Given
        final CustomIncidentHandlerPlugin customIncidentHandlerPlugin = spy(new CustomIncidentHandlerPlugin());
        final ProcessEngineConfigurationImpl processEngineConfig = mock(ProcessEngineConfigurationImpl.class);
        final FailedJobIncidentHandler failedJobHandler = mock(FailedJobIncidentHandler.class);
        final FailedExternalTaskIncidentHandler failedExternalTaskHandler = mock(FailedExternalTaskIncidentHandler.class);

        doAnswer(inv -> {
            final Config actConfig = inv.getArgument(0);
            assertConfigCorrect(actConfig);
            return failedJobHandler;
        }).when(customIncidentHandlerPlugin).createFailedJobIncidentHandler(any());

        doAnswer(inv -> {
            final Config actConfig = inv.getArgument(0);
            assertConfigCorrect(actConfig);
            return failedExternalTaskHandler;
        }).when(customIncidentHandlerPlugin).createFailedExternalTaskIncidentHandler(any());

        doAnswer(inv -> {
            final List<IncidentHandler> incidentListeners = inv.getArgument(0);
            assertSame(failedJobHandler, incidentListeners.get(0));
            assertSame(failedExternalTaskHandler, incidentListeners.get(1));
            return null;
        }).when(processEngineConfig).setCustomIncidentHandlers(any());

        customIncidentHandlerPlugin.setIntervalMs(INTERVAL_MS);
        customIncidentHandlerPlugin.setPort(PORT);
        customIncidentHandlerPlugin.setUrl(URL);
        customIncidentHandlerPlugin.setFallbackMailReceiver(FALLBACK_MAIL_RECEIVER);
        customIncidentHandlerPlugin.setUsername(USERNAME);
        customIncidentHandlerPlugin.setPassword(PASSWORD);
        customIncidentHandlerPlugin.setSubject(SUBJECT);
        customIncidentHandlerPlugin.setHost(HOST);
        customIncidentHandlerPlugin.setMailSender(MAIL_SENDER);
        customIncidentHandlerPlugin.setMailBodyTemplate(MAIL_BODY_TEMPLATE);
        customIncidentHandlerPlugin.setIncidentTemplate(INCIDENT_TEMPLATE);

        // When
        customIncidentHandlerPlugin.preInit(processEngineConfig);

        // Then
        assertNotNull(failedJobHandler);
        assertNotNull(failedExternalTaskHandler);

        verify(customIncidentHandlerPlugin).createFailedExternalTaskIncidentHandler(any());
        verify(customIncidentHandlerPlugin).createFailedExternalTaskIncidentHandler(any());
        verify(processEngineConfig).setCustomIncidentHandlers(any());
        verify(failedJobHandler).startTimer();
        verify(failedExternalTaskHandler).startTimer();
    }

    private void assertConfigCorrect(final Config actConfig) {
        assertEquals(Long.parseLong(INTERVAL_MS), (long) actConfig.getIntervalMs());
        assertEquals(Integer.parseInt(PORT), (int) actConfig.getPort());
        assertEquals(URL, actConfig.getUrl());
        assertEquals(FALLBACK_MAIL_RECEIVER, actConfig.getFallbackMailReceiver());
        assertEquals(USERNAME, actConfig.getUsername());
        assertEquals(PASSWORD, actConfig.getPassword());
        assertEquals(SUBJECT, actConfig.getSubject());
        assertEquals(HOST, actConfig.getHost());
        assertEquals(MAIL_BODY_TEMPLATE, actConfig.getMailBodyTemplate());
        assertEquals(INCIDENT_TEMPLATE, actConfig.getIncidentTemplate());
    }
}