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
package won.owner.web.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/rest/needphoto")
public class RestNeedPhotoController {
    final Logger logger = LoggerFactory.getLogger(getClass());

    public RestNeedPhotoController() {
    }

    @ResponseBody
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity uploadPhoto(@RequestParam("photo") MultipartFile photo,
                    @RequestParam("unique") String uniqueKey, @RequestParam("selected") String selected) {
        File tempDir = new File(uniqueKey);
        if (!tempDir.exists())
            tempDir.mkdir();
        File photoTempFile = new File(tempDir,
                        selected + "." + FilenameUtils.getExtension(photo.getOriginalFilename()));
        try {
            logger.info("Saving file to " + photoTempFile.getAbsolutePath());
            photo.transferTo(photoTempFile);
            return new ResponseEntity(HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
            // log.error("An error occurred.", e);
        }
    }

    @ResponseBody
    @RequestMapping(value = "/{unique}/{photoNum}/", method = RequestMethod.GET, produces = {
                    MediaType.IMAGE_JPEG_VALUE })
    public byte[] getImage(@PathVariable("unique") String unique, @PathVariable("photoNum") String photoNum) {
        File photoFile = new File(unique + File.separator + photoNum);
        if (photoFile.exists()) {
            try {
                return IOUtils.toByteArray(new FileInputStream(photoFile));
            } catch (IOException e) {
                logger.error("Uploading error", e);
            }
        }
        return new byte[0];
    }
}
