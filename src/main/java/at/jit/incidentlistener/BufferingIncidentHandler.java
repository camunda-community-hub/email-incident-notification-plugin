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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.impl.incident.DefaultIncidentHandler;
import org.camunda.bpm.engine.impl.incident.IncidentContext;
import org.camunda.bpm.engine.impl.persistence.entity.IncidentEntity;
import org.camunda.bpm.engine.runtime.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.smtp.SMTPTransport;

public class BufferingIncidentHandler extends DefaultIncidentHandler {
  public static final String TIME_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss z";
  static final String INCIDENT_NOTIFICATION_RECEIVER = "incidentNotificationReceiver";
  static final String INCIDENT_NOTIFICATION_CC = "incidentNotificationCc";
  private final static Logger LOGGER = LoggerFactory.getLogger(BufferingIncidentHandler.class);
  private final Config config;
  private final ITimeProvider timeProvider;
  List<IncidentInformation> incidentInfos = new CopyOnWriteArrayList<>();

  BufferingIncidentHandler(final ITimeProvider timeProvider, final String type, final Config config) {
    super(type);
    this.timeProvider = timeProvider;
    this.config = config;
  }

  public BufferingIncidentHandler(final String type, final Config config) {
    this(new DefaultTimeProvider(), type, config);
  }

  @Override
  public String getIncidentHandlerType() {
    return type;
  }

  IncidentEntity superHandleIncident(final IncidentContext context, String message) {
    return (IncidentEntity) super.handleIncident(context, message);
  }

  @Override
  public synchronized Incident handleIncident(final IncidentContext ctx, final String message) {
    final IncidentEntity incEnt = superHandleIncident(ctx, message);
    this.incidentInfos.add(createIncidentInfo(incEnt));
    return incEnt;
  }

  IncidentInformation createIncidentInfo(final IncidentEntity incEnt) {
    final String emailReceiver = (String) incEnt.getExecution().getVariable(INCIDENT_NOTIFICATION_RECEIVER);
    final String emailCc = (String) incEnt.getExecution().getVariable(INCIDENT_NOTIFICATION_CC);
    return new IncidentInformation().withType(incEnt.getIncidentType()).withActivityId(incEnt.getActivityId())
        .withMessage(clean(incEnt.getIncidentMessage())).withProcessInstanceId(incEnt.getProcessInstanceId())
        .withTime(new SimpleDateFormat(TIME_FORMAT_STRING).format(timeProvider.now()))
        .withProcessDefinitionId(incEnt.getProcessDefinitionId())
        .withProcessDefinitionName(incEnt.getProcessDefinition().getName())
        .withRecipientInfo(new RecipientInfo(emailReceiver, emailCc));
  }

  private String clean(String input) {
    if (input == null) {
      return "";
    }
    String result = input.replaceAll("\\{", "");
    result = result.replaceAll("\\}", "");
    result = result.replaceAll("\\$", "");
    return result;
  }

  public void startTimer() {

    createTimer().scheduleAtFixedRate(createTimerTask(), config.getIntervalMs(), config.getIntervalMs());
  }

  IncidentTimerTask createTimerTask() {
    return new IncidentTimerTask(this);
  }

  Timer createTimer() {
    return new Timer();
  }

  public synchronized void sendEmailIfNecessary() {
    if (!(incidentInfos.isEmpty())) {
      if (sendEmailsToRecipients()) {
        incidentInfos.clear();
      }
    }
  }

  synchronized boolean sendEmailsToRecipients() {
    boolean success = true;
    final Map<RecipientInfo, List<IncidentInformation>> incidentInfosByRecipientInfo = new HashMap<>();

    for (final IncidentInformation curIncident : incidentInfos) {
      final RecipientInfo recipientInfo = curIncident.getRecipientInfo();
      List<IncidentInformation> targetList = incidentInfosByRecipientInfo.get(recipientInfo);
      if (targetList == null) {
        targetList = new ArrayList<>();
        incidentInfosByRecipientInfo.put(recipientInfo, targetList);
      }
      targetList.add(curIncident);
    }
    for (final Map.Entry<RecipientInfo, List<IncidentInformation>> curEntry : incidentInfosByRecipientInfo.entrySet()) {
      final RecipientInfo recipientInfo = curEntry.getKey();
      final List<IncidentInformation> incidents = curEntry.getValue();

      final String messageText = composeMessage(incidents);
      final boolean emailSent = sendEmail(recipientInfo, messageText);
      if (!emailSent) {
        success = false;
      }
    }
    return success;
  }

  String composeMessage(final List<IncidentInformation> incidents) {
    final StringBuilder incidentDescriptions = new StringBuilder();
    for (final IncidentInformation curIncident : incidents) {
      incidentDescriptions.append(composeIncidentDescription(curIncident));
      incidentDescriptions.append("\n");
    }
    return config.getMailBodyTemplate().replaceAll("@INCIDENTS", incidentDescriptions.toString());
  }

  private String composeIncidentDescription(final IncidentInformation incident) {
    String incidentText = config.getIncidentTemplate();
    incidentText = incidentText.replaceAll("@ACTIVITY", incident.getActivityId());
    incidentText = incidentText.replaceAll("@PROCESS_INSTANCE_ID", incident.getProcessInstanceId());
    incidentText = incidentText.replaceAll("@MESSAGE", incident.getMessage());
    incidentText = incidentText.replaceAll("@INCIDENT_TYPE", incident.getType());
    incidentText = incidentText.replaceAll("@URL", String.format("%s/app/cockpit/default/#/process-instance/%s",
        config.getUrl(), incident.getProcessInstanceId()));
    incidentText = incidentText.replaceAll("@TIME", incident.getTime());
    return incidentText;
  }

  boolean sendEmail(final RecipientInfo recipientInfo, final String messageText) {
    final Properties mailConfig = new Properties();
    SMTPTransport transport = null;
    String response = "";

    mailConfig.put("mail.smtp.host", config.getHost());
    mailConfig.put("mail.smtp.auth", "true");
    mailConfig.put("mail.smtp.port", Integer.toString(config.getPort()));
    mailConfig.put("mail.smtp.starttls.enable", "true");

    Session session = Session.getInstance(mailConfig, null);
    Message msg = new MimeMessage(session);
    try {
      msg.setFrom(new InternetAddress(config.getMailSender()));
      msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(determineReceiver(recipientInfo), false));
      if (StringUtils.isNotBlank(recipientInfo.getCc())) {
        msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(recipientInfo.getCc(), false));

      }

      msg.setSubject(config.getSubject());
      msg.setText(messageText);

      transport = (SMTPTransport) getTransport(session);
      transport.connect(config.getHost(), config.getUsername(), config.getPassword());
      transport.sendMessage(msg, msg.getAllRecipients());

      response = transport.getLastServerResponse();

      LOGGER.info(String.format("Received response from server: '%s'", response));
      return true;
    } catch (final MessagingException exception) {
      LOGGER.error(String.format("An error occurred, while sending incident report: '%s'", messageText), exception);
      return false;
    } finally {
      if (transport != null) {
        try {
          transport.close();
        } catch (final MessagingException exception) {
          LOGGER.error(String.format("An error occurred, while sending incident report: '%s'", messageText), exception);
        }
      }
    }
  }

  Transport getTransport(Session session) throws NoSuchProviderException {
    return session.getTransport("smtp");
  }

  String determineReceiver(RecipientInfo recipientInfo) {
    String receiver = recipientInfo.getReceiver();
    if (StringUtils.isBlank(receiver)) {
      receiver = config.getFallbackMailReceiver();
      LOGGER.info(String
          .format("Using fallback receiver information '%s' because the process-related receiver is blank", receiver));
    }
    return receiver;
  }
}
