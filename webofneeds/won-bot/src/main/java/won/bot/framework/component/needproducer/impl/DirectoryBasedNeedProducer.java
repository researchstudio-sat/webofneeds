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

package won.bot.framework.component.needproducer.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.bot.framework.component.needproducer.FileBasedNeedProducer;
import won.bot.framework.component.needproducer.NeedProducer;
import won.protocol.exception.DataIntegrityException;

/**
 * NeedProducer that is configured to read needs from a directory.
 */
public class DirectoryBasedNeedProducer implements NeedProducer {
    private static final int NOT_INITIALIZED = -1;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private File directory;

    // true if the factory should keep creating needs after having used each file, false if the factory should use each
    // file only once.
    private boolean repeat;

    // Java Regex for filtering filenames in the directory
    private String filenameFilterRegex = null;

    private File[] files;

    private int fileIndex = NOT_INITIALIZED;

    private AtomicBoolean initialized = new AtomicBoolean(false);

    private FileBasedNeedProducer fileBasedNeedProducer;

    @Override
    public synchronized Dataset create() {
        // lazy init
        initializeLazily();
        // init failed?
        if (isInitFailed())
            return null;
        // at and of file list?
        if (isAfterLastFile()) {
            if (this.isRepeat()) {
                rewind();
            } else {
                return null;
            }
        }

        // loop until we find a readable file
        while (this.fileIndex < this.files.length) {
            if (isCurrentFileReadable())
                break;
            this.fileIndex++;
        }
        int fileIndexToUse = this.fileIndex; // remember the current index

        // advance the index for next time, reset it if we've been told to repeat
        this.fileIndex++;
        rewindIfNecessary();

        return readDatasetFromFileWithIndex(fileIndexToUse);
    }

    public synchronized String getCurrentFileName() {
        return files[fileIndex].getName();
    }

    private boolean isCurrentFileReadable() {
        return this.files[this.fileIndex].isFile() && this.files[this.fileIndex].canRead();
    }

    private Dataset readDatasetFromFileWithIndex(final int fileIndexToUse) {
        try {
            // make a need from it
            if (fileIndexToUse >= this.files.length)
                return null;
            return this.fileBasedNeedProducer.readNeedFromFile(this.files[fileIndexToUse]);
        } catch (IOException e) {
            logger.error("could not read need from file {}", this.files[fileIndexToUse]);
        } catch (DataIntegrityException e) {
            logger.error("DataIntegrityException(need and sysinfo models must contain a resource of type won:Need");
        }
        return null;
    }

    private void rewindIfNecessary() {
        if (shouldRewind()) {
            rewind();
        }
    }

    private boolean shouldRewind() {
        return this.fileIndex >= this.files.length && this.repeat;
    }

    private void rewind() {
        this.fileIndex = 0;
    }

    private boolean isAfterLastFile() {
        return fileIndex > files.length;
    }

    private boolean isInitFailed() {
        if (this.fileIndex == NOT_INITIALIZED) {
            return true;
        }
        return false;
    }

    private synchronized void initializeLazily() {
        if (!initialized.get()) {
            init();
        }
    }

    @Override
    public boolean isExhausted() {
        initializeLazily();
        if (isRepeat() && files != null && files.length > 0)
            return false;
        return this.fileIndex == NOT_INITIALIZED || this.files == null || this.fileIndex >= this.files.length;
    }

    private synchronized void init() {
        if (this.initialized.get())
            return;
        if (this.directory == null) {
            logger.warn("No directory specified for DirectoryBasedNeedProducer, not reading any data.");
            return;
        }
        logger.debug("Initializing DirectoryBasedNeedProducer from directory {}", this.directory);
        this.files = directory.listFiles(createFileFilter());
        if (this.files == null || this.files.length == 0) {
            logger.info("no files found in directory {} with regex {}", this.directory, this.filenameFilterRegex);
        } else {
            logger.debug("found {} files in directory {} with regex {}",
                    new Object[] { files.length, this.directory, this.filenameFilterRegex });
        }
        rewind();
        this.initialized.set(true);
    }

    private FileFilter createFileFilter() {
        if (this.filenameFilterRegex == null)
            return TrueFileFilter.TRUE;
        return new RegexFileFilter(this.filenameFilterRegex);
    }

    public File getDirectory() {
        return directory;
    }

    public void setDirectory(final File directory) {
        this.directory = directory;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(final boolean repeat) {
        this.repeat = repeat;
    }

    public FileBasedNeedProducer getFileBasedNeedProducer() {
        return fileBasedNeedProducer;
    }

    public void setFileBasedNeedProducer(final FileBasedNeedProducer fileBasedNeedProducer) {
        this.fileBasedNeedProducer = fileBasedNeedProducer;
    }

    public String getFilenameFilterRegex() {
        return filenameFilterRegex;
    }

    public void setFilenameFilterRegex(final String filenameFilterRegex) {
        this.filenameFilterRegex = filenameFilterRegex;
    }
}
