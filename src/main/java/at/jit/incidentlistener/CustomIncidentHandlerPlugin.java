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

import static java.util.Arrays.asList;

import java.util.List;

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.incident.IncidentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomIncidentHandlerPlugin extends AbstractProcessEnginePlugin {
  private final static Logger LOGGER = LoggerFactory.getLogger(CustomIncidentHandlerPlugin.class);

  private String intervalMs;
  private String url;
  private String fallbackMailReceiver;
  private String username;
  private String password;
  private String subject;
  private String host;
  private String port;
  private String mailSender;
  private String mailBodyTemplate;
  private String incidentTemplate;

  @Override
  public void preInit(final ProcessEngineConfigurationImpl processEngineConfig) {
    final Config config = new Config().withIntervalMs(Long.parseLong(intervalMs)).withUrl(url)
        .withFallbackMailReceiver(fallbackMailReceiver).withUsername(username).withPassword(password)
        .withSubject(subject).withHost(host).withPort(Integer.parseInt(port)).withMailSender(mailSender)
        .withMailBodyTemplate(mailBodyTemplate).withIncidentTemplate(incidentTemplate);
    final BufferingIncidentHandler failedJobHandler = createFailedJobIncidentHandler(config);
    final BufferingIncidentHandler failedExternalTaskHandler = createFailedExternalTaskIncidentHandler(config);
    final List<IncidentHandler> incidentListeners = asList(failedJobHandler, failedExternalTaskHandler);
    processEngineConfig.setCustomIncidentHandlers(incidentListeners);

    incidentListeners.stream().map(incidentHandler -> (BufferingIncidentHandler) incidentHandler)
        .forEach(incidentListener -> incidentListener.startTimer());
  }

  FailedExternalTaskIncidentHandler createFailedExternalTaskIncidentHandler(final Config config) {
    return new FailedExternalTaskIncidentHandler(config);
  }

  FailedJobIncidentHandler createFailedJobIncidentHandler(final Config config) {
    return new FailedJobIncidentHandler(config);
  }

  public String getIntervalMs() {
    return intervalMs;
  }

  public void setIntervalMs(String intervalMs) {
    this.intervalMs = intervalMs;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getFallbackMailReceiver() {
    return fallbackMailReceiver;
  }

  public void setFallbackMailReceiver(String fallbackMailReceiver) {
    this.fallbackMailReceiver = fallbackMailReceiver;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
  }

  public String getMailSender() {
    return mailSender;
  }

  public void setMailSender(String mailSender) {
    this.mailSender = mailSender;
  }

  public String getMailBodyTemplate() {
    return mailBodyTemplate;
  }

  public void setMailBodyTemplate(String mailBodyTemplate) {
    this.mailBodyTemplate = mailBodyTemplate;
  }

  public String getIncidentTemplate() {
    return incidentTemplate;
  }

  public void setIncidentTemplate(String incidentTemplate) {
    this.incidentTemplate = incidentTemplate;
  }
}
