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
package won.monitoring;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

/**
 *
 */
public class MonitoringRecorder {
    private TaskScheduler taskScheduler;
    private Trigger trigger;
    private MonitoringStatisticsRecorderTask task;

    public void setup() {
        taskScheduler.schedule(task, trigger);
    }

    public void setTaskScheduler(final TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    public void setTrigger(final Trigger trigger) {
        this.trigger = trigger;
    }

    public void setTask(final MonitoringStatisticsRecorderTask task) {
        this.task = task;
    }
}
