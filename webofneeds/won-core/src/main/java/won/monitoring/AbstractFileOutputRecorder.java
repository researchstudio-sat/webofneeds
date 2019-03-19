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

package won.monitoring;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitoring statistics recorder that can be configured to write new files conforming to a specified filename pattern
 * into a specified directory. If no directory is specified, a temp directory is created.
 */
public abstract class AbstractFileOutputRecorder extends AbstractRecorder {
    private static final String TEMPDIR_PREFIX = "monitoringStatsCSV";
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private File outputDirectory;
    /**
     * DateFormat pattern for creating the filename for the date it is written.
     */
    private String outfilePattern = "'monitoring-'yyyy-MM-dd'T'HH-mm-ss'.log'";

    protected String createOutFilename() {
        SimpleDateFormat format = new SimpleDateFormat(outfilePattern);
        return format.format(new Date());
    }

    protected File createOutFileObject() throws IOException {
        if (this.outputDirectory == null) {
            this.outputDirectory = Files.createTempDirectory(TEMPDIR_PREFIX).toFile();
            logger.info("created temporary directory for monitoring output: {}", this.outputDirectory);
        } else {
            if (!this.outputDirectory.exists()) {
                boolean success = this.outputDirectory.createNewFile();
                if (success) {
                    logger.info("created temporary directory for monitoring output: {}", this.outputDirectory);
                }
            }
        }
        File newOutfile = new File(this.outputDirectory, createOutFilename());
        logger.debug("writing monitoring statistics to file {}", newOutfile);
        return newOutfile;
    }

    protected File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(final File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    protected String getOutfilePattern() {
        return outfilePattern;
    }

    public void setOutfilePattern(final String outfilePattern) {
        this.outfilePattern = outfilePattern;
    }
}
