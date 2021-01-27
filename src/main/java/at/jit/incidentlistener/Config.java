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

public class Config {
  private Long intervalMs;
  private String url;
  private String fallbackMailReceiver;
  private String username;
  private String password;
  private String subject;
  private String host;
  private Integer port;
  private String mailSender;
  private String mailBodyTemplate;
  private String incidentTemplate;

  public Config withIntervalMs(Long intervalMs) {
    this.intervalMs = intervalMs;
    return this;
  }

  public Config withUrl(String url) {
    this.url = url;
    return this;
  }

  public Config withFallbackMailReceiver(String fallbackMailReceiver) {
    this.fallbackMailReceiver = fallbackMailReceiver;
    return this;
  }

  public Config withUsername(String username) {
    this.username = username;
    return this;
  }

  public Config withPassword(String password) {
    this.password = password;
    return this;
  }

  public Config withSubject(String subject) {
    this.subject = subject;
    return this;
  }

  public Config withHost(String host) {
    this.host = host;
    return this;
  }

  public Config withPort(Integer port) {
    this.port = port;
    return this;
  }

  public Config withMailSender(String mailSender) {
    this.mailSender = mailSender;
    return this;
  }

  public Config withMailBodyTemplate(String mailBodyTemplate) {
    this.mailBodyTemplate = mailBodyTemplate;
    return this;
  }

  public Config withIncidentTemplate(String incidentTemplate) {
    this.incidentTemplate = incidentTemplate;
    return this;
  }

  public Long getIntervalMs() {
    return intervalMs;
  }

  public String getUrl() {
    return url;
  }

  public String getFallbackMailReceiver() {
    return fallbackMailReceiver;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getSubject() {
    return subject;
  }

  public String getHost() {
    return host;
  }

  public Integer getPort() {
    return port;
  }

  public String getMailSender() {
    return mailSender;
  }

  public String getMailBodyTemplate() {
    return mailBodyTemplate;
  }

  public String getIncidentTemplate() {
    return incidentTemplate;
  }
}
