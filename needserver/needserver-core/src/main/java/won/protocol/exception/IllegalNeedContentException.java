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

package won.protocol.exception;


import java.text.MessageFormat;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class IllegalNeedContentException extends WonProtocolException
{
  //TODO: replace string by type of content enventually chosen!
  private String needContent;

  //TODO: replace string by type of content enventually chosen!
  public IllegalNeedContentException(final String needContent)
  {
    super(MessageFormat.format("Need content '{0}' is not allowed.", needContent));
    this.needContent = needContent;
  }

  public IllegalNeedContentException(final Throwable cause, final String needContent)
  {
    super(new MessageFormat("Need content '{0}' is not allowed.").format(needContent),cause);
    this.needContent = needContent;
  }
}
