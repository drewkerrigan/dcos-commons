package com.mesosphere.sdk.specification;

/**
 * A PodInstance defines a particular instance of a {@link PodSpec}. {@link PodSpec}s have an associated count, see
 * ({@link PodSpec#getCount()}).  When expanding that {@link PodSpec} to match the required count, {@link PodInstance}s
 * are generated.
 */
public interface PodInstance {

    /**
     * The specification that defines this pod instance.
     */
    PodSpec getPod();

    /**
     * The index of this pod instance. Each pod instance has a unique index, starting at zero.
     */
    int getIndex();

    default String getName() {
        return getName(getPod(), getIndex());
    }

    static String getName(PodSpec podSpec, int index) {
        return String.format("%s-%d", podSpec.getType(), index);
    }
}
