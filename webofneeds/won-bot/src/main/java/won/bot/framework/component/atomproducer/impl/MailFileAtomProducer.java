/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.bot.framework.component.atomproducer.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.apache.jena.query.Dataset;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.component.atomproducer.FileBasedAtomProducer;
import won.protocol.util.DefaultAtomModelWrapper;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * User: fkleedorfer Date: 17.12.13
 */
public class MailFileAtomProducer implements FileBasedAtomProducer {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public synchronized Dataset readAtomFromFile(final File file) throws IOException {
        logger.debug("processing as mail file: {} ", file);
        try (FileInputStream fis = new FileInputStream(file)) {
            DefaultAtomModelWrapper atomModelWrapper = new DefaultAtomModelWrapper("no:uri");
            MimeMessage emailMessage = new MimeMessage(null, fis);
            MimeMessageParser parser = new MimeMessageParser(emailMessage);
            parser.parse();
            atomModelWrapper.setTitle(parser.getSubject());
            String content = null;
            if (parser.hasPlainContent()) {
                content = parser.getPlainContent();
            } else if (parser.hasHtmlContent()) {
                Document doc = Jsoup.parse(parser.getHtmlContent());
                content = doc.text();
            }
            if (content != null) {
                atomModelWrapper.setDescription(content);
            }
            logger.debug("mail subject          : {}", parser.getSubject());
            logger.debug("mail has plain content: {}", parser.hasPlainContent());
            logger.debug("mail has html content : {}", parser.hasHtmlContent());
            logger.debug("mail has attachments  : {}", parser.hasAttachments());
            logger.debug("mail plain content    : {}", StringUtils.abbreviate(parser.getPlainContent(), 200));
            logger.debug("mail html content     : {}", StringUtils.abbreviate(parser.getHtmlContent(), 200));
            return atomModelWrapper.copyDataset();
        } catch (Exception e) {
            logger.error("could not parse email from file {} ", file, e);
        }
        return null;
    }
}
