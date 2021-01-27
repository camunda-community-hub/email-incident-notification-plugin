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

public class IncidentInformation {
  private String type;
  private String activityId;
  private String message;
  private String processInstanceId;
  private String time;
  private String processDefinitionId;
  private String processDefinitionName;
  private RecipientInfo recipientInfo;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getActivityId() {
    return activityId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getProcessInstanceId() {
    return processInstanceId;
  }

  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time;
  }

  public IncidentInformation withType(String type) {
    this.type = type;
    return this;
  }

  public IncidentInformation withActivityId(String activityId) {
    this.activityId = activityId;
    return this;
  }

  public IncidentInformation withMessage(String message) {
    this.message = message;
    return this;
  }

  public IncidentInformation withProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
    return this;
  }

  public IncidentInformation withTime(String time) {
    this.time = time;
    return this;
  }

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  public void setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  public String getProcessDefinitionName() {
    return processDefinitionName;
  }

  public void setProcessDefinitionName(String processDefinitionName) {
    this.processDefinitionName = processDefinitionName;
  }

  public IncidentInformation withProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
    return this;
  }

  public IncidentInformation withProcessDefinitionName(String processDefinitionName) {
    this.processDefinitionName = processDefinitionName;
    return this;
  }

  public IncidentInformation withRecipientInfo(RecipientInfo recipientInfo) {
    this.recipientInfo = recipientInfo;
    return this;
  }

  public RecipientInfo getRecipientInfo() {
    return recipientInfo;
  }

  public void setRecipientInfo(RecipientInfo recipientInfo) {
    this.recipientInfo = recipientInfo;
  }
}
