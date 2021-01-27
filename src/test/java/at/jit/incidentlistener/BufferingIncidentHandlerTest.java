/*
 * Copyright 2020 “J-IT“ IT-Dienstleistungs GesmbH, Vienna, Austria
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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.commons.lang3.time.DateUtils;
import org.camunda.bpm.engine.impl.incident.IncidentContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.IncidentEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.runtime.Incident;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sun.mail.smtp.SMTPTransport;

public class BufferingIncidentHandlerTest {
  public static final String MESSAGE = "Test message";

  @Test
  public void givenIncident_whenHandleIncident_thenAddIncidentInfoToList() {
    // Given
    final Config config = new Config();
    final BufferingIncidentHandler ruleBasedIncidentHandler = spy(new BufferingIncidentHandler("", config));

    final IncidentContext ctx = mock(IncidentContext.class);
    final IncidentEntity incEnt = mock(IncidentEntity.class);
    doReturn(incEnt).when(ruleBasedIncidentHandler).superHandleIncident(ctx, MESSAGE);

    final IncidentInformation incidentInfo = mock(IncidentInformation.class);
    doReturn(incidentInfo).when(ruleBasedIncidentHandler).createIncidentInfo(incEnt);

    // When
    final Incident actRes = ruleBasedIncidentHandler.handleIncident(ctx, MESSAGE);

    // Then
    verify(ruleBasedIncidentHandler).superHandleIncident(ctx, MESSAGE);
    assertSame(incEnt, actRes);
    assertTrue(ruleBasedIncidentHandler.incidentInfos.contains(incidentInfo));
  }

  @Test
  public void givenInputData_whenCreateIncidentInfo_thenReturnCorrectValue() throws AddressException, ParseException {
    // Given
    final IncidentEntity incEnt = mock(IncidentEntity.class);
    final ITimeProvider timeProvider = mock(ITimeProvider.class);
    final BufferingIncidentHandler ruleBasedIncidentHandler = new BufferingIncidentHandler(timeProvider, "",
        new Config());
    final String emailReceiver = "emailReceiver@provider.com";
    final String ccReceiver = "ccReceiver@provider.com";

    final ExecutionEntity execution = mock(ExecutionEntity.class);
    when(incEnt.getExecution()).thenReturn(execution);
    when(execution.getVariable(BufferingIncidentHandler.INCIDENT_NOTIFICATION_RECEIVER)).thenReturn(emailReceiver);
    when(execution.getVariable(BufferingIncidentHandler.INCIDENT_NOTIFICATION_CC)).thenReturn(ccReceiver);

    when(incEnt.getIncidentType()).thenReturn("failedJob");
    when(incEnt.getActivityId()).thenReturn("activityId");
    when(incEnt.getIncidentMessage()).thenReturn("Message with {curly braces} and a $ dollar sign");
    when(incEnt.getProcessInstanceId()).thenReturn("processInstanceId");

    final Date now = DateUtils.parseDate("2020-11-23 10:22", "yyyy-MM-dd HH:mm");
    when(timeProvider.now()).thenReturn(now);

    when(incEnt.getProcessDefinitionId()).thenReturn("processDefinitionId");

    final ProcessDefinitionEntity processDefinition = mock(ProcessDefinitionEntity.class);
    when(incEnt.getProcessDefinition()).thenReturn(processDefinition);
    when(processDefinition.getName()).thenReturn("Process Definition Name");

    // When
    final IncidentInformation actRes = ruleBasedIncidentHandler.createIncidentInfo(incEnt);

    // Then
    verify(timeProvider).now();
    assertNotNull(actRes);
    assertEquals("failedJob", actRes.getType());
    assertEquals("activityId", actRes.getActivityId());
    assertEquals("Message with curly braces and a  dollar sign", actRes.getMessage());
    assertEquals("processInstanceId", actRes.getProcessInstanceId());
    assertEquals("2020-11-23 10:22:00 CET", actRes.getTime());
    assertEquals("processDefinitionId", actRes.getProcessDefinitionId());
    assertEquals("Process Definition Name", actRes.getProcessDefinitionName());
    assertNotNull(actRes.getRecipientInfo());
    assertEquals("emailReceiver@provider.com", actRes.getRecipientInfo().getReceiver());
    assertEquals("ccReceiver@provider.com", actRes.getRecipientInfo().getCc());
  }

  @Test
  public void givenConfig_whenStartTimer_thenStartTimerWithCorrectInterval() {
    // Given
    final Config config = new Config().withIntervalMs(123L);

    final BufferingIncidentHandler ruleBasedIncidentHandler = spy(new BufferingIncidentHandler("", config));
    final IncidentTimerTask timerTask = mock(IncidentTimerTask.class);
    doReturn(timerTask).when(ruleBasedIncidentHandler).createTimerTask();

    final Timer timer = mock(Timer.class);
    doReturn(timer).when(ruleBasedIncidentHandler).createTimer();

    // When
    ruleBasedIncidentHandler.startTimer();

    // Then
    verify(ruleBasedIncidentHandler).createTimer();
    verify(ruleBasedIncidentHandler).createTimerTask();
    verify(timer).scheduleAtFixedRate(timerTask, 123L, 123L);
  }

  @Test
  public void givenMultipleRecipientInfos_whenSendMailToRecipients_thenSendEmailsToCorrectRecipients() {
    // Given
    final RecipientInfo recipientInfo1 = new RecipientInfo("receiver1", "cc1");
    final RecipientInfo recipientInfo2 = new RecipientInfo("receiver2", "cc2");
    final IncidentInformation incidentInfo1 = new IncidentInformation().withRecipientInfo(recipientInfo1);
    final IncidentInformation incidentInfo2 = new IncidentInformation().withRecipientInfo(recipientInfo1);
    final IncidentInformation incidentInfo3 = new IncidentInformation().withRecipientInfo(recipientInfo2);
    final IncidentInformation incidentInfo4 = new IncidentInformation().withRecipientInfo(recipientInfo2);
    final IncidentInformation incidentInfo5 = new IncidentInformation().withRecipientInfo(recipientInfo2);

    final BufferingIncidentHandler ruleBasedIncidentHandler = spy(new BufferingIncidentHandler("", new Config()));
    ruleBasedIncidentHandler.incidentInfos = Arrays.asList(incidentInfo1, incidentInfo2, incidentInfo3, incidentInfo4,
        incidentInfo5);

    doAnswer(invocationOnMock -> {
      final List<IncidentInformation> incidents = invocationOnMock.getArgument(0);

      if (incidents.contains(incidentInfo1) && incidents.contains(incidentInfo2)) {
        return "message for recipientInfo1";
      } else if (!incidents.contains(incidentInfo1) && !incidents.contains(incidentInfo2)
          && incidents.contains(incidentInfo3) && incidents.contains(incidentInfo4)
          && incidents.contains(incidentInfo5)) {
        return "message for recipientInfo2";
      } else {
        Assert.fail("Invalid call");
      }
      return null;
    }).when(ruleBasedIncidentHandler).composeMessage(anyList());

    doReturn(true).when(ruleBasedIncidentHandler).sendEmail(any(), anyString());

    // When
    final boolean actRes = ruleBasedIncidentHandler.sendEmailsToRecipients();

    // Then
    assertTrue(actRes);
    verify(ruleBasedIncidentHandler, times(2)).composeMessage(anyList());
    verify(ruleBasedIncidentHandler).sendEmail(recipientInfo1, "message for recipientInfo1");
    verify(ruleBasedIncidentHandler).sendEmail(recipientInfo2, "message for recipientInfo2");
  }

  @Test
  public void givenEmailSendingFailure_whenSendMailToRecipients_thenReturnFalse() {
    // Given
    final RecipientInfo recipientInfo1 = new RecipientInfo("receiver1", "cc1");
    final RecipientInfo recipientInfo2 = new RecipientInfo("receiver2", "cc2");
    final IncidentInformation incidentInfo1 = new IncidentInformation().withRecipientInfo(recipientInfo1);
    final IncidentInformation incidentInfo2 = new IncidentInformation().withRecipientInfo(recipientInfo1);
    final IncidentInformation incidentInfo3 = new IncidentInformation().withRecipientInfo(recipientInfo2);
    final IncidentInformation incidentInfo4 = new IncidentInformation().withRecipientInfo(recipientInfo2);
    final IncidentInformation incidentInfo5 = new IncidentInformation().withRecipientInfo(recipientInfo2);

    final BufferingIncidentHandler ruleBasedIncidentHandler = spy(new BufferingIncidentHandler("", new Config()));
    ruleBasedIncidentHandler.incidentInfos = Arrays.asList(incidentInfo1, incidentInfo2, incidentInfo3, incidentInfo4,
        incidentInfo5);

    doAnswer(invocationOnMock -> {
      final List<IncidentInformation> incidents = invocationOnMock.getArgument(0);

      if (incidents.contains(incidentInfo1) && incidents.contains(incidentInfo2)) {
        return "message for recipientInfo1";
      } else if (!incidents.contains(incidentInfo1) && !incidents.contains(incidentInfo2)
          && incidents.contains(incidentInfo3) && incidents.contains(incidentInfo4)
          && incidents.contains(incidentInfo5)) {
        return "message for recipientInfo2";
      } else {
        Assert.fail("Invalid call");
      }
      return null;
    }).when(ruleBasedIncidentHandler).composeMessage(anyList());

    doAnswer(new Answer() {
      private boolean firstTime = true;

      @Override
      public Object answer(InvocationOnMock invocationOnMock) {
        if (firstTime) {
          firstTime = false;
          return true;
        }
        return false;
      }
    }).when(ruleBasedIncidentHandler).sendEmail(any(), anyString());

    // When
    final boolean actRes = ruleBasedIncidentHandler.sendEmailsToRecipients();

    // Then
    assertFalse(actRes);
    verify(ruleBasedIncidentHandler, times(2)).composeMessage(anyList());
    verify(ruleBasedIncidentHandler).sendEmail(recipientInfo1, "message for recipientInfo1");
    verify(ruleBasedIncidentHandler).sendEmail(recipientInfo2, "message for recipientInfo2");
  }

  @Test
  public void givenIncidents_whenComposeMessage_thenReturnCorrectText() {
    // Given
    final IncidentInformation incident1 = new IncidentInformation().withActivityId("activityId1")
        .withMessage("Message 1").withProcessInstanceId("processInstanceId1")
        .withProcessDefinitionId("processDefinitionId1").withProcessDefinitionName("Process definition name 1")
        .withTime("2020-11-23 11:28:00 CET").withType("failedJob");
    final IncidentInformation incident2 = new IncidentInformation().withActivityId("activityId2")
        .withMessage("Message 2").withProcessInstanceId("processInstanceId2")
        .withProcessDefinitionId("processDefinitionId2").withProcessDefinitionName("Process definition name 2")
        .withTime("2020-11-23 11:29:01 CET").withType("failedJob");
    final Config config = new Config().withUrl("http://localhost:8080/camunda")
        .withMailBodyTemplate("PREFIX @INCIDENTS SUFFIX")
        .withIncidentTemplate("Activity ID: @ACTIVITY\n" + "Process Instance ID: @PROCESS_INSTANCE_ID\n"
            + "Message: @MESSAGE\n" + "Incident Type: @INCIDENT_TYPE\n" + "URL: @URL\n" + "Time: @TIME");
    final BufferingIncidentHandler ruleBasedIncidentHandler = new BufferingIncidentHandler("", config);

    // When
    final String actRes = ruleBasedIncidentHandler.composeMessage(Arrays.asList(incident1, incident2));

    // Then
    assertEquals("PREFIX Activity ID: activityId1\n" + "Process Instance ID: processInstanceId1\n"
        + "Message: Message 1\n" + "Incident Type: failedJob\n"
        + "URL: http://localhost:8080/camunda/app/cockpit/default/#/process-instance/processInstanceId1\n"
        + "Time: 2020-11-23 11:28:00 CET\n" + "Activity ID: activityId2\n" + "Process Instance ID: processInstanceId2\n"
        + "Message: Message 2\n" + "Incident Type: failedJob\n"
        + "URL: http://localhost:8080/camunda/app/cockpit/default/#/process-instance/processInstanceId2\n"
        + "Time: 2020-11-23 11:29:01 CET\n" + " SUFFIX", actRes);
  }

  @Test
  public void givenNoReceiverInProcess_whenDetermineReiceiver_thenReturnFallbackReceiver() {
    // Given
    final Config config = new Config().withFallbackMailReceiver("backupReceiver@provider.com");
    final BufferingIncidentHandler ruleBasedIncidentHandler = new BufferingIncidentHandler("", config);
    final RecipientInfo recipientInfo = new RecipientInfo("", "ccReceiver@provider.com");

    // When
    final String actRes = ruleBasedIncidentHandler.determineReceiver(recipientInfo);

    // Then
    assertEquals("backupReceiver@provider.com", actRes);
  }

  @Test
  public void givenReceiverInProcess_whenDetermineReiceiver_thenReturnReceiverInProcess() {
    // Given
    final Config config = new Config().withFallbackMailReceiver("backupReceiver@provider.com");
    final BufferingIncidentHandler ruleBasedIncidentHandler = new BufferingIncidentHandler("", config);
    final RecipientInfo recipientInfo = new RecipientInfo("receiverSpecifiedInProcessVariable@provider.com",
        "ccReceiver@provider.com");

    // When
    final String actRes = ruleBasedIncidentHandler.determineReceiver(recipientInfo);

    // Then
    assertEquals("receiverSpecifiedInProcessVariable@provider.com", actRes);
  }

  @Test
  public void givenConfig_whenSendMail_thenSendMessageWithCorrectParameters() throws MessagingException {
    // Given
    final Config config = new Config().withHost("host").withUsername("username").withPassword("password").withPort(587)
        .withMailSender("Camunda DEV <no-reply@a1.at>").withSubject("Incident on Camunda DEV");
    final BufferingIncidentHandler ruleBasedIncidentHandler = spy(new BufferingIncidentHandler("", config));

    final RecipientInfo recipientInfo = new RecipientInfo("recipient@provider.com",
        "ccRecipient1@provider.com,ccRecipient2@provider.com");

    final SMTPTransport transport = mock(SMTPTransport.class);
    doReturn(transport).when(ruleBasedIncidentHandler).getTransport(any());

    doAnswer(invocationOnMock -> {
      final Message msg = invocationOnMock.getArgument(0);
      final Address[] recipients = invocationOnMock.getArgument(1);

      assertEquals(1, msg.getFrom().length);
      assertEquals("Camunda DEV <no-reply@a1.at>", msg.getFrom()[0].toString());
      final Address[] toRecipients = msg.getRecipients(Message.RecipientType.TO);
      assertEquals(1, toRecipients.length);
      assertEquals("recipient@provider.com", toRecipients[0].toString());

      final Address[] ccRecipients = msg.getRecipients(Message.RecipientType.CC);
      assertEquals(2, ccRecipients.length);
      assertEquals("ccRecipient1@provider.com", ccRecipients[0].toString());
      assertEquals("ccRecipient2@provider.com", ccRecipients[1].toString());

      assertEquals("Incident on Camunda DEV", msg.getSubject());
      assertEquals("messageText", msg.getContent());

      return null;
    }).when(transport).sendMessage(any(), any());

    // When
    final boolean actRes = ruleBasedIncidentHandler.sendEmail(recipientInfo, "messageText");

    // Then
    assertTrue(actRes);
    verify(ruleBasedIncidentHandler).getTransport(any());
    verify(transport).connect("host", "username", "password");
    verify(transport).sendMessage(any(), any());
    verify(transport).close();
  }

  @Test
  public void givenEmailSendingFailure_whenSendEmail_thenReturnFalse() throws MessagingException {
    // Given
    final Config config = new Config().withHost("host").withUsername("username").withPassword("password").withPort(587)
        .withMailSender("Camunda DEV <no-reply@a1.at>").withSubject("Incident on Camunda DEV");
    final BufferingIncidentHandler ruleBasedIncidentHandler = spy(new BufferingIncidentHandler("", config));

    final RecipientInfo recipientInfo = new RecipientInfo("recipient@provider.com",
        "ccRecipient1@provider.com,ccRecipient2@provider.com");

    final SMTPTransport transport = mock(SMTPTransport.class);
    doReturn(transport).when(ruleBasedIncidentHandler).getTransport(any());

    doThrow(new MessagingException()).when(transport).sendMessage(any(), any());

    // When
    final boolean actRes = ruleBasedIncidentHandler.sendEmail(recipientInfo, "messageText");

    // Then
    assertFalse(actRes);
    verify(ruleBasedIncidentHandler).getTransport(any());
    verify(transport).connect("host", "username", "password");
    verify(transport).sendMessage(any(), any());
    verify(transport).close();

  }
}
