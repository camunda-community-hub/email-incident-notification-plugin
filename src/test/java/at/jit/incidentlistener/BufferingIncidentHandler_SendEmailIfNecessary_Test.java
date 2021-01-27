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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;

public class BufferingIncidentHandler_SendEmailIfNecessary_Test {
  @Test
  public void givenNoIncidents_whenSendEmailfNecessary_thenDontDoAnything() {
    // Given
    final BufferingIncidentHandler incidentHandler = spy(new BufferingIncidentHandler("", new Config()));

    // When
    incidentHandler.sendEmailIfNecessary();

    // Then
    verify(incidentHandler, never()).sendEmailsToRecipients();
  }

  @Test
  public void givenIncidents_whenSendEmailIfNecessary_thenSendEmail() {
    // Given
    final BufferingIncidentHandler incidentHandler = spy(new BufferingIncidentHandler("", new Config()));
    final List<IncidentInformation> incidentInfos = mock(List.class);
    when(incidentInfos.isEmpty()).thenReturn(false);
    incidentHandler.incidentInfos = incidentInfos;

    doReturn(true).when(incidentHandler).sendEmailsToRecipients();

    // When
    incidentHandler.sendEmailIfNecessary();

    // Then
    verify(incidentHandler).sendEmailsToRecipients();
    verify(incidentInfos).clear();
  }

  @Test
  public void givenIncidentsAndMailFailure_whenSendEmailIfNecessary_thenDontClearList() {
    // Given
    final BufferingIncidentHandler incidentHandler = spy(new BufferingIncidentHandler("", new Config()));
    final List<IncidentInformation> incidentInfos = mock(List.class);
    when(incidentInfos.isEmpty()).thenReturn(false);
    incidentHandler.incidentInfos = incidentInfos;

    doReturn(false).when(incidentHandler).sendEmailsToRecipients();

    // When
    incidentHandler.sendEmailIfNecessary();

    // Then
    verify(incidentHandler).sendEmailsToRecipients();
    verify(incidentInfos, never()).clear();

  }
}
