/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.matcher.protocol;

import org.apache.jena.query.Dataset;

import java.net.URI;

/**
 * Handler implementation that does nothing. Useful for extending as well as
 * pull-only cases such as a simple Web application.
 */
public class NopMatcherProtocolMatcherServiceCallback implements MatcherProtocolMatcherServiceCallback {

  @Override
  public void onRegistered(final URI wonNodeUri) {
    // To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void onNewNeed(final URI wonNodeURI, URI needURI, Dataset content) {
    // To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void onNeedActivated(final URI wonNodeURI, final URI needURI) {
    // To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void onNeedDeactivated(final URI wonNodeURI, final URI needURI) {
    // To change body of implemented methods use File | Settings | File Templates.
  }
}
