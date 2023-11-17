import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.spi.LifeCycle
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import net.logstash.logback.composite.FormattedTimestampJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventFormattedTimestampJsonProvider;
import net.logstash.logback.composite.loggingevent.LogLevelJsonProvider;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;

import static ch.qos.logback.classic.Level.DEBUG

appender("FILE", FileAppender) {
  file = "/var/log/ind-app/hsaUppdatering.log"
  append = true
  encoder(LoggingEventCompositeJsonEncoder) {
    LoggingEventJsonProviders aProviders = new LoggingEventJsonProviders()
	FormattedTimestampJsonProvider timestampProvider = new LoggingEventFormattedTimestampJsonProvider ()
	timestampProvider.timeZone = "CET"
	aProviders.addTimestamp(timestampProvider)
		
	aProviders.addLogLevel(new LogLevelJsonProvider())
    aProviders.addMessage(new MessageJsonProvider())

	providers = aProviders
  }
}

logger("org.hibernate", ERROR)
logger("org.hibernate.cache", ERROR)

root(DEBUG, ["FILE"])