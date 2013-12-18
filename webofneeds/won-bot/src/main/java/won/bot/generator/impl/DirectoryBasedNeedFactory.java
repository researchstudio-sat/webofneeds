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
import won.bot.generator.NeedFactory;

import java.io.File;
import java.io.IOException;

/**
 * NeedFactory that is configured to read needs from a directory.
 */
public class DirectoryBasedNeedFactory implements NeedFactory
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private File directory;

  // true if the factory should keep creating needs after having used each file, false if the factory should use each file only once.
  private boolean repeat;

  private File[] files;

  private int fileIndex = -1;

  private FileBasedNeedFactory fileBasedNeedFactory;

  private void init(){
    if (fileIndex != -1) return; //already initialized
    this.files = directory.listFiles();
    this.fileIndex = 0;
  }

  @Override
  public Model create()
  {
    if (fileIndex > files.length) {
      if (this.isRepeat()) {
        this.fileIndex = 0;
      } else {
        return null;
      }
    }
    int fileIndexToUse = this.fileIndex;
    this.fileIndex++;
    try {
      return this.fileBasedNeedFactory.readNeedFromFile(this.files[fileIndexToUse]);
    } catch (IOException e) {
      logger.debug("could not read need from file {}", this.files[fileIndexToUse]);
    }
    return null;
  }

  public File getDirectory()
  {
    return directory;
  }

  public void setDirectory(final File directory)
  {
    this.directory = directory;
  }

  public boolean isRepeat()
  {
    return repeat;
  }

  public void setRepeat(final boolean repeat)
  {
    this.repeat = repeat;
  }

  public FileBasedNeedFactory getFileBasedNeedFactory()
  {
    return fileBasedNeedFactory;
  }

  public void setFileBasedNeedFactory(final FileBasedNeedFactory fileBasedNeedFactory)
  {
    this.fileBasedNeedFactory = fileBasedNeedFactory;
  }
}
