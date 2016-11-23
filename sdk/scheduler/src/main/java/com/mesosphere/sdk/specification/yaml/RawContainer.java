package com.mesosphere.sdk.specification.yaml;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;

/**
 * Raw YAML container.
 */
public class RawContainer {
    private final String imageName;
    private final WriteOnceLinkedHashMap<String, RawRLimit> rlimits;

    @JsonCreator
    public RawContainer(
            @JsonProperty("image-name") String imageName,
            @JsonProperty("rlimits") WriteOnceLinkedHashMap<String, RawRLimit> rlimits) {
        this.imageName = imageName;
        this.rlimits = rlimits;
    }

    public String getImageName() {
        return imageName;
    }

    public LinkedHashMap<String, RawRLimit> getRLimits() {
        return rlimits;
    }
}
