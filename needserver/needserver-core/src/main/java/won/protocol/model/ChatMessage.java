/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.protocol.model;

import java.net.URI;
import java.util.Date;

/**
 * User: fkleedorfer
 * Date: 31.10.12
 */
public class ChatMessage
{
  /* the date the message was first seen in the local system */
  private Date creationDate;

  /* the URI of the need that sent the message */
  private URI originatorURI;

  /* the URI of the local connection */
  private URI localConnectionURI;

  /* the message */
  private String message;

  public Date getCreationDate()
  {
    return creationDate;
  }

  public void setCreationDate(final Date creationDate)
  {
    this.creationDate = creationDate;
  }

  public URI getOriginatorURI()
  {
    return originatorURI;
  }

  public void setOriginatorURI(final URI originatorURI)
  {
    this.originatorURI = originatorURI;
  }

  public URI getLocalConnectionURI()
  {
    return localConnectionURI;
  }

  public void setLocalConnectionURI(final URI localConnectionURI)
  {
    this.localConnectionURI = localConnectionURI;
  }

  public String getMessage()
  {
    return message;
  }

  public void setMessage(final String message)
  {
    this.message = message;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (this == o) return true;
    if (!(o instanceof ChatMessage)) return false;

    final ChatMessage that = (ChatMessage) o;

    if (creationDate != null ? !creationDate.equals(that.creationDate) : that.creationDate != null) return false;
    if (localConnectionURI != null ? !localConnectionURI.equals(that.localConnectionURI) : that.localConnectionURI != null)
      return false;
    if (message != null ? !message.equals(that.message) : that.message != null) return false;
    if (originatorURI != null ? !originatorURI.equals(that.originatorURI) : that.originatorURI != null) return false;

    return true;
  }

  @Override
  public int hashCode()
  {
    int result = creationDate != null ? creationDate.hashCode() : 0;
    result = 31 * result + (originatorURI != null ? originatorURI.hashCode() : 0);
    result = 31 * result + (localConnectionURI != null ? localConnectionURI.hashCode() : 0);
    result = 31 * result + (message != null ? message.hashCode() : 0);
    return result;
  }
}
