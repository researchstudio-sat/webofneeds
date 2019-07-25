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
package won.monitoring.simon;

import org.javasimon.Simon;
import org.javasimon.SimonManager;
import org.javasimon.utils.SimonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.monitoring.AbstractRecorder;

import java.lang.invoke.MethodHandles;

/**
 * MonitoringStatisticsRecorder that prints the statistics via the logger with
 * loglevel 'debug'.
 */
public class SimonSysoutStatisticsRecorder extends AbstractRecorder {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void recordMonitoringStatistics() {
        if (logger.isDebugEnabled()) {
            Simon rootSimon = SimonManager.getRootSimon();
            logger.debug("Monitoring statistics: \n" + SimonUtils.simonTreeString(rootSimon));
        }
    }
}
