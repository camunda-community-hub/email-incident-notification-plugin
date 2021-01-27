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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class RecipientInfo {
  private final String receiver;
  private final String cc;

  public RecipientInfo(final String receiver, final String cc) {
    this.receiver = receiver;
    this.cc = cc;
  }

  public String getReceiver() {
    return receiver;
  }

  public String getCc() {
    return cc;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;

    if (o == null || getClass() != o.getClass())
      return false;

    RecipientInfo that = (RecipientInfo) o;

    return new EqualsBuilder().append(receiver, that.receiver).append(cc, that.cc).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(receiver).append(cc).toHashCode();
  }
}
