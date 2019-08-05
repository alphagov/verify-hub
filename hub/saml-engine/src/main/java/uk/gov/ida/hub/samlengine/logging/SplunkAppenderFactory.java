package uk.gov.ida.hub.samlengine.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.splunk.logging.HttpEventCollectorLogbackAppender;
import io.dropwizard.logging.AbstractAppenderFactory;
import io.dropwizard.logging.async.AsyncAppenderFactory;
import io.dropwizard.logging.filter.LevelFilterFactory;
import io.dropwizard.logging.layout.LayoutFactory;

import javax.validation.constraints.NotNull;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonTypeName("splunk")
public class SplunkAppenderFactory extends AbstractAppenderFactory<ILoggingEvent> {
    @JsonProperty
    @NotNull
    private String url;

    @JsonProperty
    @NotNull
    private String token;

    @JsonProperty
    @NotNull
    private String source;

    @JsonProperty
    @NotNull
    private String sourceType;

    @JsonProperty
    @NotNull
    private String index;

    @JsonProperty
    private Long batchSizeCount = 1L;

    @Override
    public Appender<ILoggingEvent> build(LoggerContext context, String applicationName, LayoutFactory<ILoggingEvent> layoutFactory, LevelFilterFactory<ILoggingEvent> levelFilterFactory, AsyncAppenderFactory<ILoggingEvent> asyncAppenderFactory) {
        HttpEventCollectorLogbackAppender<ILoggingEvent> appender = new HttpEventCollectorLogbackAppender<>();
        checkNotNull(context);

        appender.setUrl(url);
        appender.setToken(token);
        appender.setSource(source);
        appender.setSourcetype(sourceType);
        appender.setIndex(index);
        appender.setbatch_size_count(batchSizeCount.toString());
        appender.setLayout(buildLayout(context, layoutFactory));
        appender.addFilter(levelFilterFactory.build(threshold));
        appender.setHttpProxyHost(System.getProperty("http.proxyHost"));
        appender.setHttpProxyPort(getProxyPortIntFromSystem());
        appender.start();

        return wrapAsync(appender, asyncAppenderFactory, context);
    }

    private int getProxyPortIntFromSystem() {
        return (System.getProperty("http.proxyPort") != null) ? Integer.parseInt(System.getProperty("http.proxyPort")) : 0;
    }
}
