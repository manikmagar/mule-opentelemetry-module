package com.avioconsulting.mule.opentelemetry.internal.util;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;

import java.util.Optional;

public class ComponentsUtil {

  public static Optional<ComponentLocation> findLocation(String location,
      ConfigurationComponentLocator configurationComponentLocator) {
    return configurationComponentLocator.findAllLocations().stream().filter(cl -> cl.getLocation().equals(location))
        .findFirst();
  }

  public static boolean isSubFlow(ComponentLocation location) {
    return location.getComponentIdentifier().getIdentifier().getName().equals("sub-flow");
  }

  public static boolean isFlowRef(ComponentLocation location) {
    return location.getComponentIdentifier().getIdentifier().getName().equals("flow-ref");
  }

  public static Optional<Component> findComponent(ComponentIdentifier identifier, String location,
      ConfigurationComponentLocator configurationComponentLocator) {
    return configurationComponentLocator
        .find(identifier).stream()
        .filter(c -> c.getLocation().getLocation().equals(location)).findFirst();
  }
}
