package org.apache.mesos.specification;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.mesos.Protos;

/**
 * Spec for defining a Container.
 */
public interface ContainerSpec {
    @JsonProperty("imageName")
    String getImageName();
}
