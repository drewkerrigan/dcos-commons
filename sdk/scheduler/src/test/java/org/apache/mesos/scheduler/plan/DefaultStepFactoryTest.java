package org.apache.mesos.scheduler.plan;

import org.apache.curator.test.TestingServer;
import org.apache.mesos.config.ConfigStore;
import org.apache.mesos.config.DefaultTaskConfigRouter;
import org.apache.mesos.curator.CuratorStateStore;
import org.apache.mesos.offer.DefaultOfferRequirementProvider;
import org.apache.mesos.offer.InvalidRequirementException;
import org.apache.mesos.offer.OfferRequirementProvider;
import org.apache.mesos.offer.TaskUtils;
import org.apache.mesos.scheduler.DefaultScheduler;
import org.apache.mesos.specification.*;
import org.apache.mesos.state.StateStore;
import org.apache.mesos.testing.CuratorTestUtils;
import org.apache.mesos.testutils.TestConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by gabriel on 11/15/16.
 */
public class DefaultStepFactoryTest {
    private static final TaskSpec taskSpec0 =
            TestPodFactory.getTaskSpec(TestConstants.TASK_NAME + 0, TestConstants.RESOURCE_SET_ID);
    private static final TaskSpec taskSpec1 =
            TestPodFactory.getTaskSpec(TestConstants.TASK_NAME + 1, TestConstants.RESOURCE_SET_ID);
    private static final PodSpec POD_SPEC = DefaultPodSpec.newBuilder()
            .type(TestConstants.POD_TYPE)
            .count(1)
            .resources(Arrays.asList(taskSpec0.getResourceSet()))
            .tasks(Arrays.asList(taskSpec0, taskSpec1))
            .build();

    private static final PodInstance POD_INSTANCE = new DefaultPodInstance(POD_SPEC, 0);
    private static TestingServer testingServer;

    private EnvironmentVariables environmentVariables;
    private StepFactory stepFactory;
    private ConfigStore<ServiceSpec> configStore;
    private StateStore stateStore;
    private OfferRequirementProvider offerRequirementProvider;

    private static final ServiceSpec serviceSpec =
            DefaultServiceSpec.newBuilder()
                    .name(TestConstants.SERVICE_NAME)
                    .role(TestConstants.ROLE)
                    .principal(TestConstants.PRINCIPAL)
                    .apiPort(0)
                    .zookeeperConnection("foo.bar.com")
                    .pods(Arrays.asList(POD_SPEC))
                    .build();

    @BeforeClass
    public static void beforeAll() throws Exception {
        testingServer = new TestingServer();
    }

    @Before
    public void beforeEach() throws Exception {
        CuratorTestUtils.clear(testingServer);

        environmentVariables = new EnvironmentVariables();
        environmentVariables.set("EXECUTOR_URI", "");

        stateStore = new CuratorStateStore(
                "test-framework-name",
                testingServer.getConnectString());

        configStore = DefaultScheduler.createConfigStore(
                serviceSpec,
                testingServer.getConnectString(),
                Collections.emptyList());

        UUID configId = configStore.store(serviceSpec);
        configStore.setTargetConfig(configId);

        offerRequirementProvider = new DefaultOfferRequirementProvider(
                new DefaultTaskConfigRouter(),
                stateStore,
                configId);

        stepFactory = new DefaultStepFactory(configStore, stateStore, offerRequirementProvider);
    }

    @Test(expected = Step.InvalidStepException.class)
    public void testGetStepFailsOnMultipleResourceSetReferences()
            throws InvalidRequirementException, Step.InvalidStepException {

        List<String> tasksToLaunch = TaskUtils.getTaskNames(POD_INSTANCE);
        stepFactory.getStep(POD_INSTANCE, tasksToLaunch);
    }
}
