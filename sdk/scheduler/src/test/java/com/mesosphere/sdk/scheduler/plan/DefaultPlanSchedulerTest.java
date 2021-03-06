package com.mesosphere.sdk.scheduler.plan;

import com.mesosphere.sdk.offer.*;
import com.mesosphere.sdk.scheduler.TaskKiller;
import com.mesosphere.sdk.specification.DefaultServiceSpec;
import com.mesosphere.sdk.specification.PodInstance;
import com.mesosphere.sdk.specification.PodSpec;
import com.mesosphere.sdk.specification.yaml.YAMLServiceSpecFactory;
import org.apache.mesos.Protos.*;
import org.apache.mesos.Protos.Offer.Operation;
import org.apache.mesos.SchedulerDriver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link DefaultPlanScheduler}.
 */
public class DefaultPlanSchedulerTest {

    private static final List<Offer> OFFERS = Arrays.asList(Offer.newBuilder()
            .setId(OfferID.newBuilder().setValue("offerid").build())
            .setFrameworkId(FrameworkID.newBuilder().setValue("frameworkid").build())
            .setSlaveId(SlaveID.newBuilder().setValue("slaveid").build())
            .setHostname("hello")
            .build());
    private static final List<OfferRecommendation> RECOMMENDATIONS =
            Arrays.asList(new OfferRecommendation() {
                @Override public Operation getOperation() { return null; }
                @Override public Offer getOffer() { return null; }
            });
    private static final List<OfferID> ACCEPTED_IDS =
            Arrays.asList(OfferID.newBuilder().setValue("offer").build());

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Mock private OfferAccepter mockOfferAccepter;
    @Mock private OfferEvaluator mockOfferEvaluator;
    @Mock private SchedulerDriver mockSchedulerDriver;
    @Mock private TaskKiller mockTaskKiller;
    private PodInstanceRequirement podInstanceRequirement;

    private DefaultPlanScheduler scheduler;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        environmentVariables.set("PORT0", "8080");
        scheduler = new DefaultPlanScheduler(mockOfferAccepter, mockOfferEvaluator, mockTaskKiller);

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("valid-minimal.yml").getFile());
        DefaultServiceSpec serviceSpec = YAMLServiceSpecFactory
                .generateServiceSpec(YAMLServiceSpecFactory.generateRawSpecFromYAML(file));

        PodSpec podSpec = serviceSpec.getPods().get(0);
        PodInstance podInstance = new DefaultPodInstance(podSpec, 0);
        List<String> tasksToLaunch = TaskUtils.getTaskNames(podInstance);
        podInstanceRequirement = new PodInstanceRequirement(podInstance, tasksToLaunch);
    }

    @Test
    public void testNullParams() {
        assertTrue(scheduler.resourceOffers(null, OFFERS, Arrays.asList(new TestStep())).isEmpty());
        assertTrue(scheduler.resourceOffers(mockSchedulerDriver, null, Arrays.asList(new TestStep())).isEmpty());
        assertTrue(scheduler.resourceOffers(mockSchedulerDriver, OFFERS, null).isEmpty());
        verifyZeroInteractions(mockOfferAccepter, mockSchedulerDriver);
    }

    @Test
    public void testNonPendingStep() {
        TestStep step = new TestStep();
        step.setStatus(Status.PREPARED);
        assertTrue(scheduler.resourceOffers(mockSchedulerDriver, OFFERS, Arrays.asList(step)).isEmpty());
        assertTrue(step.isPrepared());
    }

    @Test
    public void testStartNoRequirement() {
        TestStep step = new TestStep();
        step.setStatus(com.mesosphere.sdk.scheduler.plan.Status.PENDING);
        assertTrue(scheduler.resourceOffers(mockSchedulerDriver, OFFERS, Arrays.asList(step)).isEmpty());
        assertTrue(step.isPrepared());
    }

    @Test
    public void testEvaluateNoRecommendations() throws InvalidRequirementException {
        TestOfferStep step = new TestOfferStep(podInstanceRequirement);
        step.setStatus(Status.PENDING);
        when(mockOfferEvaluator.evaluate(podInstanceRequirement, OFFERS)).thenReturn(new ArrayList<>());

        assertTrue(scheduler.resourceOffers(mockSchedulerDriver, OFFERS, Arrays.asList(step)).isEmpty());
        assertTrue(step.operations.isEmpty());
        verify(mockOfferEvaluator).evaluate(podInstanceRequirement, OFFERS);
        assertTrue(step.isPrepared());
    }

    @Test
    public void testEvaluateNoAcceptedOffers() throws InvalidRequirementException {
        TestOfferStep step = new TestOfferStep(podInstanceRequirement);
        step.setStatus(Status.PENDING);
        when(mockOfferEvaluator.evaluate(podInstanceRequirement, OFFERS)).thenReturn(RECOMMENDATIONS);
        when(mockOfferAccepter.accept(mockSchedulerDriver, RECOMMENDATIONS)).thenReturn(new ArrayList<>());

        assertTrue(scheduler.resourceOffers(mockSchedulerDriver, OFFERS, Arrays.asList(step)).isEmpty());
        assertTrue(step.operations.isEmpty());
        verify(mockOfferAccepter).accept(mockSchedulerDriver, RECOMMENDATIONS);
        assertTrue(step.isPrepared());
    }

    @Test
    public void testEvaluateAcceptedOffers() throws InvalidRequirementException {
        TestOfferStep step = new TestOfferStep(podInstanceRequirement);
        step.setStatus(Status.PENDING);
        when(mockOfferEvaluator.evaluate(podInstanceRequirement, OFFERS)).thenReturn(RECOMMENDATIONS);
        when(mockOfferAccepter.accept(mockSchedulerDriver, RECOMMENDATIONS)).thenReturn(ACCEPTED_IDS);

        assertEquals(ACCEPTED_IDS, scheduler.resourceOffers(mockSchedulerDriver, OFFERS, Arrays.asList(step)));
        assertFalse(step.operations.isEmpty());
        assertTrue(step.isStarting());
    }

    private static class TestOfferStep extends TestStep {
        private final PodInstanceRequirement podInstanceRequirement;
        private Collection<Operation> operations;

        private TestOfferStep(PodInstanceRequirement podInstanceRequirement) {
            super();
            this.podInstanceRequirement = podInstanceRequirement;
            this.operations = Collections.emptyList();
        }

        @Override
        public Optional<PodInstanceRequirement> start() {
            super.start();
            if (podInstanceRequirement == null) {
                return Optional.empty();
            } else {
                return Optional.of(podInstanceRequirement);
            }
        }

        @Override
        public void updateOfferStatus(Collection<Operation> operations) {
            super.updateOfferStatus(operations);
            this.operations = operations;
        }
    }
}
