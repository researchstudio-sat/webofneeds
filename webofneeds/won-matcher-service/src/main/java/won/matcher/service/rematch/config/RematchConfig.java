package won.matcher.service.rematch.config;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

@Configuration
@PropertySource("file:${WON_CONFIG_DIR}/matcher-service.properties")
public class RematchConfig {
    @Value("${rematcher.rematchInterval}")
    long rematchInterval;

    public RematchConfig() {
    }

    public FiniteDuration getRematchInterval() {
        return Duration.create(rematchInterval, TimeUnit.MILLISECONDS);
    }
}
