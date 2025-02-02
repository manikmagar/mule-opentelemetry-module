package com.avioconsulting.mule.opentelemetry.internal.config;

import com.avioconsulting.mule.opentelemetry.api.config.ExporterConfiguration;
import com.avioconsulting.mule.opentelemetry.api.config.OpenTelemetryResource;
import com.avioconsulting.mule.opentelemetry.api.config.SpanProcessorConfiguration;
import com.avioconsulting.mule.opentelemetry.api.config.TraceLevelConfiguration;
import com.avioconsulting.mule.opentelemetry.api.config.metrics.CustomMetricInstrumentDefinition;
import com.avioconsulting.mule.opentelemetry.internal.operations.OpenTelemetryOperations;
import com.avioconsulting.mule.opentelemetry.internal.connection.OpenTelemetryConnection;
import com.avioconsulting.mule.opentelemetry.internal.connection.OpenTelemetryConnectionProvider;
import com.avioconsulting.mule.opentelemetry.internal.notifications.listeners.MetricEventNotificationListener;
import com.avioconsulting.mule.opentelemetry.internal.notifications.listeners.MuleMessageProcessorNotificationListener;
import com.avioconsulting.mule.opentelemetry.internal.notifications.listeners.MulePipelineMessageNotificationListener;
import com.avioconsulting.mule.opentelemetry.internal.processor.MuleNotificationProcessor;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.*;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Operations(OpenTelemetryOperations.class)
@ConnectionProviders(OpenTelemetryConnectionProvider.class)
@Configuration
public class OpenTelemetryExtensionConfiguration implements Startable, Stoppable, OpenTelemetryConfiguration {

  public static final String PROP_MULE_OTEL_TRACING_DISABLED = "mule.otel.tracing.disabled";
  public static final String PROP_MULE_OTEL_METRICS_DISABLED = "mule.otel.metrics.disabled";
  private final Logger logger = LoggerFactory.getLogger(OpenTelemetryExtensionConfiguration.class);

  @RefName
  private String configName;

  @Parameter
  @Optional(defaultValue = "false")
  @Summary("Turn off tracing for this application.")
  private boolean turnOffTracing;

  @Parameter
  @Optional(defaultValue = "false")
  @Summary("Turn off Metrics for this application.")
  private boolean turnOffMetrics;

  /**
   * Open Telemetry Resource Configuration. System or Environment Variables will
   * override this configuration. See Documentation for variable details.
   */
  @ParameterGroup(name = "Resource")
  @Placement(order = 10)
  @Summary("Open Telemetry Resource Configuration. System or Environment Variables will override this configuration.")
  private OpenTelemetryResource resource;

  /**
   * Open Telemetry Exporter Configuration. System or Environment Variables will
   * override this configuration. See Documentation for variable details.
   */
  @ParameterGroup(name = "Exporter")
  @Placement(order = 20)
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  private ExporterConfiguration exporterConfiguration;

  @ParameterGroup(name = "Trace Levels")
  @Placement(order = 30)
  private TraceLevelConfiguration traceLevelConfiguration;

  @ParameterGroup(name = "Span Processor")
  @Placement(order = 40, tab = "Tracer Settings")
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  private SpanProcessorConfiguration spanProcessorConfiguration;

  @Parameter
  @Optional
  @NullSafe
  @Placement(order = 501, tab = "Metrics")
  @DisplayName("Custom Metric Instruments")
  @Summary("List of instruments for capturing custom metrics")
  private List<CustomMetricInstrumentDefinition> customMetricInstruments;
  private Map<String, CustomMetricInstrumentDefinition> metricInstrumentDefinitionMap;

  public List<CustomMetricInstrumentDefinition> getCustomMetricInstruments() {
    return customMetricInstruments;
  }

  @Override
  public Map<String, CustomMetricInstrumentDefinition> getMetricInstrumentDefinitionMap() {
    if (metricInstrumentDefinitionMap == null) {
      boolean usesReservedKeyWords = getCustomMetricInstruments().stream()
          .map(CustomMetricInstrumentDefinition::getMetricName).anyMatch(name -> name.startsWith("otel."));
      if (usesReservedKeyWords)
        throw new MuleRuntimeException(I18nMessageFactory
            .createStaticMessage("Instrument names cannot use reserved namespaces - otel.*"));
      metricInstrumentDefinitionMap = getCustomMetricInstruments().stream()
          .collect(Collectors.toMap(CustomMetricInstrumentDefinition::getMetricName, Function.identity()));

    }
    return metricInstrumentDefinitionMap;
  }

  @Override
  public boolean isTurnOffTracing() {
    return System.getProperties().containsKey(PROP_MULE_OTEL_TRACING_DISABLED) ? Boolean
        .parseBoolean(System.getProperty(PROP_MULE_OTEL_TRACING_DISABLED)) : turnOffTracing;
  }

  @Override
  public boolean isTurnOffMetrics() {
    return System.getProperties().containsKey(PROP_MULE_OTEL_METRICS_DISABLED) ? Boolean
        .parseBoolean(System.getProperty(PROP_MULE_OTEL_METRICS_DISABLED)) : turnOffMetrics;
  }

  public OpenTelemetryExtensionConfiguration setTurnOffTracing(boolean turnOffTracing) {
    this.turnOffTracing = turnOffTracing;
    return this;
  }

  public OpenTelemetryExtensionConfiguration setTurnOffMetrics(boolean turnOffMetrics) {
    this.turnOffMetrics = turnOffMetrics;
    return this;
  }

  @Override
  public OpenTelemetryResource getResource() {
    return resource;
  }

  public OpenTelemetryExtensionConfiguration setResource(OpenTelemetryResource resource) {
    this.resource = resource;
    return this;
  }

  @Override
  public ExporterConfiguration getExporterConfiguration() {
    return exporterConfiguration;
  }

  public OpenTelemetryExtensionConfiguration setExporterConfiguration(ExporterConfiguration exporterConfiguration) {
    this.exporterConfiguration = exporterConfiguration;
    return this;
  }

  @Override
  public TraceLevelConfiguration getTraceLevelConfiguration() {
    return traceLevelConfiguration;
  }

  public OpenTelemetryExtensionConfiguration setTraceLevelConfiguration(
      TraceLevelConfiguration traceLevelConfiguration) {
    this.traceLevelConfiguration = traceLevelConfiguration;
    return this;
  }

  @Override
  public SpanProcessorConfiguration getSpanProcessorConfiguration() {
    return spanProcessorConfiguration;
  }

  public OpenTelemetryExtensionConfiguration setSpanProcessorConfiguration(
      SpanProcessorConfiguration spanProcessorConfiguration) {
    this.spanProcessorConfiguration = spanProcessorConfiguration;
    return this;
  }

  public OpenTelemetryExtensionConfiguration setCustomMetricInstruments(
      List<CustomMetricInstrumentDefinition> customMetricInstruments) {
    this.customMetricInstruments = customMetricInstruments;
    return this;
  }

  @Override
  public String getConfigName() {
    return configName;
  }

  @Inject
  NotificationListenerRegistry notificationListenerRegistry;

  @Inject
  MuleNotificationProcessor muleNotificationProcessor;

  @Override
  public void start() throws MuleException {
    // This phase is too early to initiate OpenTelemetry SDK. It fails with
    // unresolved Otel dependencies.
    // To defer the SDK initialization, MuleNotificationProcessor accepts a supplier
    // that isn't accessed unless needed.
    // Reaching to an actual notification processor event would mean all
    // dependencies are loaded. That is when supplier
    // fetches the connection.
    // This is unconventional way of Connection handling in custom extensions. There
    // are no operations or sources involved.
    // Adding it here gives an opportunity to use Configuration parameters for
    // initializing the SDK. A future use case.
    // TODO: Find another way to inject connections.
    if (disableTelemetry()) {
      logger.warn("Tracing and Metrics is disabled. OpenTelemetry will be turned off for config '{}'.",
          getConfigName());
      // Is there a better way to let runtime trigger the configuration shutdown
      // without stopping the application?
      // Raising an exception here will make runtime invoke the stop method
      // but it will kill the application as well, so can't do that here.
      // For now, let's skip the initialization of tracing related components and
      // processors.
      return;
    }
    logger.info("Initiating otel config - '{}'", getConfigName());
    muleNotificationProcessor.init(OpenTelemetryConnection
        .getInstance(new OpenTelemetryConfigWrapper(this)),
        getTraceLevelConfiguration());
    notificationListenerRegistry.registerListener(
        new MuleMessageProcessorNotificationListener(muleNotificationProcessor));
    notificationListenerRegistry.registerListener(
        new MulePipelineMessageNotificationListener(muleNotificationProcessor));
    notificationListenerRegistry.registerListener(new MetricEventNotificationListener(muleNotificationProcessor),
        ext -> ext.getAction().getNamespace().equals("OPENTELEMETRY"));
  }

  private boolean disableTelemetry() {
    return isTurnOffTracing() && isTurnOffMetrics();
  }

  @Override
  public void stop() throws MuleException {
    if (isTurnOffTracing()) {
      logger.info("{} is set to true. Configuration '{}' has been stopped.", PROP_MULE_OTEL_TRACING_DISABLED,
          getConfigName());
    }
  }
}
