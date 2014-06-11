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

package won.protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * User: fkleedorfer
 * Date: 05.12.13
 */
public class Config
{
  // system property key to find the config directory
  public static final String SYSPROPKEY_WON_CONFIG_DIR = "WON_CONFIG_DIR";

  public static final String PROPERTIES_FILE_MATCHER = "matcher.properties";

  public static InputStream getInputStreamForConfigFile(String filename) throws FileNotFoundException{
    String configDir = System.getProperty(Config.SYSPROPKEY_WON_CONFIG_DIR);
    if (configDir == null) throw new IllegalArgumentException("system property '" + Config
      .SYSPROPKEY_WON_CONFIG_DIR+"' should point to the configuration dir but is null");
    File configDirFile = new File(configDir);
    if (!configDirFile.exists()) throw new IllegalArgumentException("won config dir '" + configDir +"' defined by system property '" + Config.SYSPROPKEY_WON_CONFIG_DIR+"' does not exist");
    if (!configDirFile.canRead()) throw new IllegalArgumentException("won config dir '" + configDir +"' defined by system property '" + Config.SYSPROPKEY_WON_CONFIG_DIR+"' is not readable");
    File configFile = new File(configDirFile,filename);
    if (!configFile.exists()) throw new IllegalArgumentException("won config file '" + filename + " expected at  dir '" + configDir +"' defined by system property '" + Config.SYSPROPKEY_WON_CONFIG_DIR+"' does not exist");
    if (!configFile.canRead()) throw new IllegalArgumentException("won config file '" + filename + " expected at  dir '" + configDir +"' defined by system property '" + Config.SYSPROPKEY_WON_CONFIG_DIR+"' is not readable");
    return new FileInputStream(configFile);
  }
}
