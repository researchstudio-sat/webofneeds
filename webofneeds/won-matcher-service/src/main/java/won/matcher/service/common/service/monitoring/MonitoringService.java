package won.matcher.service.common.service.monitoring;

import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by hfriedrich on 09.10.2015.
 */
@Component
@Scope("singleton")
public class MonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String ATOM_HINT_STOPWATCH = "AtomReceivedUntilFirstHintSent";
    private Map<String, Map<String, Split>> stopWatchSplits = new HashMap<>();
    @Value("${matcher.service.monitoring}")
    private boolean monitoringEnabled;

    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

    public void startClock(String stopWatchName, String splitName) {
        if (isMonitoringEnabled()) {
            Map<String, Split> splits = stopWatchSplits.get(stopWatchName);
            if (splits == null) {
                splits = new HashMap<>();
                stopWatchSplits.put(stopWatchName, splits);
            }
            if (splits.get(splitName) != null) {
                logger.warn("Split '{}' in stopwatch {} already set for monitoring start event", splitName,
                                stopWatchName);
                return;
            }
            Stopwatch stopwatch = SimonManager.getStopwatch(stopWatchName);
            Split split = stopwatch.start();
            splits.put(splitName, split);
        }
    }

    public void stopClock(String stopWatchName, String splitName) {
        if (isMonitoringEnabled()) {
            Map<String, Split> splits = stopWatchSplits.get(stopWatchName);
            if (splits == null) {
                logger.warn("No stopwatch '{}' found for monitoring end event", stopWatchName);
                return;
            }
            Split split = splits.get(splitName);
            if (split == null) {
                logger.warn("No split '{}' in stopwatch '{}' found for monitoring end event", splitName, stopWatchName);
                return;
            }
            split.stop();
            // splits.remove(monitoringEvent.getSplitName());
        }
    }
}
