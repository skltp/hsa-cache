import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import net.logstash.logback.composite.FormattedTimestampJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventFormattedTimestampJsonProvider;
import net.logstash.logback.composite.loggingevent.LogLevelJsonProvider;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;
import net.logstash.logback.composite.loggingevent.StackTraceJsonProvider;

import static ch.qos.logback.classic.Level.DEBUG


appender("STDOUT", ConsoleAppender) {
    encoder(LoggingEventCompositeJsonEncoder) {
        LoggingEventJsonProviders aProviders = new LoggingEventJsonProviders()
        FormattedTimestampJsonProvider timestampProvider = new LoggingEventFormattedTimestampJsonProvider ()
        aProviders.addTimestamp(timestampProvider)

        aProviders.addLogLevel(new LogLevelJsonProvider())
        aProviders.addMessage(new MessageJsonProvider())
        aProviders.addStackTrace(new StackTraceJsonProvider());

        providers = aProviders
    }
}

// Use file only or stdout for easier debugging
//root(DEBUG, ["FILE"])
root(DEBUG, ["STDOUT"])
