package dev.ops.tools.oc;

import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DoneableDeployment;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Controller for the OpenShift related actions and logic. Responsible to
 * control the OpenShift model state.
 */
public class OpenShiftController implements Watcher<DeploymentConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenShiftController.class);
    private static final String OC_MINIPAD_ENABLED = "oc-minipad/enabled";

    private final OpenShiftClient client;
    private final OpenShiftModel openShiftModel;
    private Consumer<String> eventConsumer;

    public OpenShiftController(OpenShiftClient client, File configFile) {
        this.client = client;
        this.openShiftModel = OpenShiftModel.fromFile(configFile);
    }

    public void initialize() {
        for (OpenShiftProject project : openShiftModel.getProjects()) {
            initializeNamespace(project);
            client.deploymentConfigs().inNamespace(project.getName()).watch(this);
        }
    }

    private void initializeNamespace(OpenShiftProject namespace) {
        if (namespace.getMode() == OpenShiftProject.Mode.STATIC) {
            initializeStaticDeployments(namespace);
        }
    }

    private void initializeStaticDeployments(OpenShiftProject namespace) {
        List<OpenShiftDeployment> deployments = namespace.getDeployments();
        for (OpenShiftDeployment openShiftDeployment : deployments) {
            String deploymentName = openShiftDeployment.getName();
            LOGGER.info("Initializing static DeploymentConfig {}", deploymentName);

            DeploymentConfig deployment = client.deploymentConfigs().inNamespace(namespace.getName()).withName(deploymentName).get();
            PodList podList = client.pods().inNamespace(namespace.getName()).withLabels(deployment.getSpec().getSelector()).list();
            openShiftDeployment.setPodList(podList.getItems());
        }
    }

    private void addDeployment(OpenShiftProject namespace, DeploymentConfig deployment) {
        Map<String, String> labels = Optional.ofNullable(deployment.getMetadata().getLabels()).orElse(Collections.emptyMap());
        boolean enabled = Boolean.parseBoolean(labels.getOrDefault(OC_MINIPAD_ENABLED, Boolean.FALSE.toString()));
        if (!enabled) {
            LOGGER.debug("Skipping DeploymentConfig {}, not oc-minipad/enabled.", deployment.getMetadata().getName());
        }

        PodList podList = client.pods().inNamespace(namespace.getName()).withLabels(deployment.getSpec().getSelector()).list();

        OpenShiftDeployment openShiftDeployment = new OpenShiftDeployment(deployment.getMetadata().getName());
        openShiftDeployment.setPodList(podList.getItems());

        LOGGER.info("Adding DeploymentConfig {}", openShiftDeployment.getName());
        namespace.addDeployment(openShiftDeployment);
    }

    private void modifyDeployment(OpenShiftProject namespace, DeploymentConfig deployment) {
        PodList podList = client.pods().inNamespace(namespace.getName()).withLabels(deployment.getSpec().getSelector()).list();

        OpenShiftDeployment openShiftDeployment = namespace.getDeploymentByName(deployment.getMetadata().getName());

        LOGGER.info("Modify DeploymentConfig {}", openShiftDeployment.getName());
        openShiftDeployment.setPodList(podList.getItems());
    }

    private void removeDeployment(OpenShiftProject namespace, DeploymentConfig deployment) {
        String name = deployment.getMetadata().getName();
        LOGGER.info("Removing DeploymentConfig {}", name);
        namespace.removeDeployment(name);
    }

    public OpenShiftModel getOpenShiftModel() {
        return openShiftModel;
    }

    @Override
    public void eventReceived(Watcher.Action action, DeploymentConfig deployment) {
        OpenShiftProject namespace = openShiftModel.getProjectByName(deployment.getMetadata().getNamespace());
        if (Action.ADDED.equals(action)) {
            if (namespace.getMode() == OpenShiftProject.Mode.DYNAMIC) {
                addDeployment(namespace, deployment);
                eventConsumer.accept(namespace.getName());
            }
        } else if (Action.MODIFIED.equals(action)) {
            modifyDeployment(namespace, deployment);
            eventConsumer.accept(namespace.getName());
        } else if (Action.DELETED.equals(action)) {
            if (namespace.getMode() == OpenShiftProject.Mode.DYNAMIC) {
                removeDeployment(namespace, deployment);
                eventConsumer.accept(namespace.getName());
            }
        } else {
            String name = deployment.getMetadata().getName();
            LOGGER.warn("Error watching DeploymentConfig {}.", name);
        }
    }

    @Override
    public void onClose(KubernetesClientException cause) {
        // nothing to do here
    }

    public void register(Consumer<String> consumer) {
        this.eventConsumer = consumer;
    }

    public void scale(OpenShiftProject openShiftProject, OpenShiftDeployment openShiftDeployment, int replicas) {
        LOGGER.info("Scaling DeploymentConfig {} to {} replicas.", openShiftDeployment.getName(), replicas);
        RollableScalableResource<Deployment, DoneableDeployment> deployment = client.apps().deployments()
                .inNamespace(openShiftProject.getName()).withName(openShiftDeployment.getName());
        deployment.scale(replicas);
    }
}
