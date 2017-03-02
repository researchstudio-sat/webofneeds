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

package won.protocol.repository.rdfstorage.impl;

import org.apache.jena.query.Dataset;
import org.apache.jena.tdb.TDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;

/**
 * Rdf storage service using jena tdb.
 */
@Deprecated
public class DirectoryBasedRdfStorageImpl extends AbstractOneDatasetPerThreadRdfStorageService
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private static final String TEMPDIR_PREFIX = "jena-tdb-tmpdir-";
  public File directory;

  public void setDirectory(final File directory) {
    this.directory = directory;
  }

  @Override
  protected Dataset createDataset(){
    assert directory != null : "directory was not determined. Did you call init()?";
    return TDBFactory.createDataset(directory.getAbsolutePath());
  }

  public void init(){
    determineDatasetDirectory();
  }

  private void determineDatasetDirectory() {
    if (directory == null){
      logger.info("no directory specified for jena tdb store, creating new temp dir for the store");
      try {
        directory = Files.createTempDirectory(TEMPDIR_PREFIX).toFile();
      } catch (IOException e) {
        throw new IllegalStateException("could not create temp dir",e);
      }
    }
    logger.info("using directory {} for jena tdb store", directory);
    assert directory. isDirectory() : new MessageFormat("cannot use {0}: not a directory name").format
      (directory.toString());
    if (!directory.exists()){
      try {
        directory.createNewFile();
      } catch (IOException e) {
        throw new IllegalStateException("could not create dir",e);
      }
    }
    assert directory.canWrite() : new MessageFormat("specified directory {0} not writable").format(directory.toString());
  }


}
