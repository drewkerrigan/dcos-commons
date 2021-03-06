package com.mesosphere.sdk.helloworld.scheduler;

import com.mesosphere.sdk.specification.*;

import java.io.File;

/**
 * Hello World Service.
 */
public class Main {
    private static final Integer COUNT = Integer.valueOf(System.getenv("HELLO_COUNT"));
    private static final Double CPUS = Double.valueOf(System.getenv("HELLO_CPUS"));

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            new DefaultService(new File(args[0]));
        } else {
            new DefaultService(DefaultServiceSpec.newBuilder()
                    .name("hello-world")
                    .principal("hello-world-principal")
                    .zookeeperConnection("master.mesos:2181")
                    .apiPort(8080)
                    .addPod(DefaultPodSpec.newBuilder()
                            .count(COUNT)
                            .addTask(DefaultTaskSpec.newBuilder()
                                    .name("hello")
                                    .goalState(GoalState.RUNNING)
                                    .commandSpec(DefaultCommandSpec.newBuilder()
                                            .value("echo hello >> hello-container-path/output && sleep 1000")
                                            .build())
                                    .resourceSet(DefaultResourceSet
                                            .newBuilder("hello-world-role", "hello-world-principal")
                                            .id("hello-resources")
                                            .cpus(CPUS)
                                            .memory(256.0)
                                            .addVolume("ROOT", 5000.0, "hello-container-path")
                                            .build()).build()).build()).build());
        }
    }
}
