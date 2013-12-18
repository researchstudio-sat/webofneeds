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

package won.bot.generator.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.generator.FileBasedNeedFactory;
import won.protocol.util.NeedModelBuilder;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * User: fkleedorfer
 * Date: 17.12.13
 */
public class MailFileNeedFactory implements FileBasedNeedFactory
{
  private final Logger logger = LoggerFactory.getLogger(getClass());


  @Override
  public Model readNeedFromFile(final File file) throws IOException
  {
    FileInputStream fis = new FileInputStream(file);
    NeedModelBuilder needModelBuilder = new NeedModelBuilder();
    try {
      MimeMessage mail = new MimeMessage(null, fis);
      needModelBuilder.setTitle(mail.getSubject());
      needModelBuilder.setDescription(mail.getContent().toString());
      return needModelBuilder.build();
    } catch (MessagingException e) {
      logger.debug("could not parse email from file {} ", file,e);
    } finally {
      if (fis != null) fis.close();
    }
    return null;
  }
}
