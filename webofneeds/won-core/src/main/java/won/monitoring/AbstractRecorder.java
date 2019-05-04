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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple base class for recorders.
 */
public abstract class AbstractRecorder implements MonitoringStatisticsRecorder {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private String recorderName;

    protected AbstractRecorder() {
        this.recorderName = getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    public void setRecorderName(final String recorderName) {
        this.recorderName = recorderName;
    }

    public String getRecorderName() {
        return recorderName;
    }
}
