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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task intended to be scheduled regularly. In each execution it triggers all MonitoringStatisticsRecorders
 * it has been provided.
 */
public class MonitoringStatisticsRecorderTask implements Runnable
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private List<MonitoringStatisticsRecorder> monitoringStatisticsRecorders;
  private boolean resetMonitorAfterRecording;
  private MonitoringResetter monitoringResetter;

  @Override
  public void run()
  {
    if (this.monitoringStatisticsRecorders == null || this.monitoringStatisticsRecorders.isEmpty()){
      logger.debug("No recorders configured, not recording any monitoring statistics, ");
      return;
    }
    for(MonitoringStatisticsRecorder recorder: this.monitoringStatisticsRecorders){
      logger.debug("running monitoring stats recorder '{}'", recorder.getRecorderName());
      recorder.recordMonitoringStatistics();
      logger.debug("done monitoring stats recorder '{}'", recorder.getRecorderName());
    }
    if (this.resetMonitorAfterRecording){
      if (this.monitoringResetter != null){
        logger.debug("resetting the monitor");
        this.monitoringResetter.resetMonitoringStatistics();
      } else {
        logger.warn("MonitoringStatisticsRecorderTask is configured to reset the monitor after recording, but no MonitoringResetter has been configured");
      }
    }
  }

  public void setMonitoringStatisticsRecorders(final List<MonitoringStatisticsRecorder> monitoringStatisticsRecorders)
  {
    this.monitoringStatisticsRecorders = monitoringStatisticsRecorders;
  }

  public void setResetMonitorAfterRecording(final boolean resetMonitorAfterRecording)
  {
    this.resetMonitorAfterRecording = resetMonitorAfterRecording;
  }

  public void setMonitoringResetter(final MonitoringResetter monitoringResetter)
  {
    this.monitoringResetter = monitoringResetter;
  }
}

