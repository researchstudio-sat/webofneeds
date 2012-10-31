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

package won.server.model;

import java.net.URI;
import java.util.Collection;

/**
 *
 */
public class Need
{
  /* The URI of the need */
  private URI URI;
  /* The state of the need */
  private NeedState state;
  /* The owner protocol endpoint URI where the owner of the need can be reached */
  private URI ownerURI;

  public URI getURI()
  {
    return URI;
  }

  public void setURI(final URI URI)
  {
    this.URI = URI;
  }

  public NeedState getState()
  {
    return state;
  }

  public void setState(final NeedState state)
  {
    this.state = state;
  }

  public URI getOwnerURI()
  {
    return ownerURI;
  }

  public void setOwnerURI(final URI ownerURI)
  {
    this.ownerURI = ownerURI;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (this == o) return true;
    if (!(o instanceof Need)) return false;

    final Need need = (Need) o;

    if (URI != null ? !URI.equals(need.URI) : need.URI != null) return false;
    if (ownerURI != null ? !ownerURI.equals(need.ownerURI) : need.ownerURI != null) return false;
    if (state != need.state) return false;

    return true;
  }

  @Override
  public int hashCode()
  {
    int result = URI != null ? URI.hashCode() : 0;
    result = 31 * result + (state != null ? state.hashCode() : 0);
    result = 31 * result + (ownerURI != null ? ownerURI.hashCode() : 0);
    return result;
  }
}
