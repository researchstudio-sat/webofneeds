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

package won.monitoring.simon;

import org.javasimon.Simon;
import org.javasimon.SimonManager;
import org.javasimon.utils.SimonUtils;
import won.monitoring.AbstractRecorder;

/**
 * MonitoringStatisticsRecorder that prints the statistics via the logger with
 * loglevel 'debug'.
 */
public class SimonSysoutStatisticsRecorder extends AbstractRecorder {

  @Override
  public void recordMonitoringStatistics() {
    Simon rootSimon = SimonManager.getRootSimon();
    if (logger.isDebugEnabled()) {
      logger.debug("Monitoring statistics: \n" + SimonUtils.simonTreeString(rootSimon));
    }
  }

}
