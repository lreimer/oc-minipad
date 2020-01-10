package dev.ops.tools;

import dev.ops.tools.midi.LaunchpadColor;
import dev.ops.tools.midi.LaunchpadDevice;
import dev.ops.tools.midi.MidiSystemHandler;
import dev.ops.tools.oc.OpenShiftController;
import dev.ops.tools.oc.OpenShiftDeployment;
import dev.ops.tools.oc.OpenShiftModel;
import dev.ops.tools.oc.OpenShiftProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Launchpad controller implementation handles logic for button events and colors.
 */
public class OpenShiftMinipadController extends LaunchpadDevice {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenShiftMinipadController.class);

    private final MidiSystemHandler midiSystem;
    private final OpenShiftController openShiftController;
    private String namespace;

    public OpenShiftMinipadController(MidiSystemHandler midiSystem, OpenShiftController openShiftController, String namespace) {
        this.midiSystem = midiSystem;
        this.openShiftController = openShiftController;
        this.namespace = namespace;
    }

    public void initialize() {
        midiSystem.initialize(this);
        reset();

        openShiftController.register(s -> {
            if (Objects.equals(namespace, s)) {
                update();
            }
        });
        openShiftController.initialize();

        update();
    }

    @Override
    protected void handle(int command, int data1, int data2) {
        if (command == 176 && data2 == 127) {
            // a 1-8 button has been pressed
            LOGGER.info("Received MIDI event for 1-8 button [command={},data1={},data2={}]", command, data1, data2);

            int index = data1 - 104;
            this.namespace = openShiftController.getOpenShiftModel().getProject(index).getName();
            LOGGER.info("Selected namespace {}", this.namespace);

            update();

        } else if (command == 144 && data2 == 127) {
            boolean isAH = A_H_BUTTONS.contains(data1);
            if (isAH) {
                // a A-H button has been pressed
                LOGGER.info("Received MIDI event for A-H button [command={},data1={},data2={}]", command, data1, data2);

                int row = A_H_BUTTONS.indexOf(data1);
                OpenShiftProject openShiftProject = openShiftController.getOpenShiftModel().getProjectByName(namespace);
                if (row < openShiftProject.getDeployments().size()) {
                    OpenShiftDeployment openShiftDeployment = openShiftProject.getDeployment(row);
                    openShiftController.scale(openShiftProject, openShiftDeployment, 0);
                }
            } else {
                // a square button has been pressed
                LOGGER.info("Received MIDI event for Square button [command={},data1={},data2={}]", command, data1, data2);

                int row = data1 / 16;
                int col = data1 % 16;

                OpenShiftProject openShiftProject = openShiftController.getOpenShiftModel().getProjectByName(namespace);
                OpenShiftDeployment openShiftDeployment = openShiftProject.getDeployment(row);
                int replicas = openShiftDeployment.getPods().size();
                if (col + 1 != replicas) {
                    openShiftController.scale(openShiftProject, openShiftDeployment, col + 1);
                }
            }
        }
    }

    private void update() {
        updateNamespaceSelectors();
        updateGrid();
    }

    private void updateNamespaceSelectors() {
        OpenShiftModel openShiftModel = openShiftController.getOpenShiftModel();
        for (int i = 0; i < openShiftModel.getProjects().size(); i++) {
            OpenShiftProject ns = openShiftModel.getProject(i);
            if (Objects.equals(ns.getName(), namespace)) {
                top(i, LaunchpadColor.BRIGHT_AMBER);
            } else {
                top(i, LaunchpadColor.DARK_AMBER);
            }
        }
    }

    private void updateGrid() {
        OpenShiftProject ns = openShiftController.getOpenShiftModel().getProjectByName(namespace);
        int size = ns.getDeployments().size();
        for (int i = 0; i < size && i < 8; i++) {
            OpenShiftDeployment deployment = ns.getDeployment(i);
            LOGGER.info("Displaying DeploymentConfig {} at row {}", deployment.getName(), i);

            Collection<String> status = deployment.getPods().values();
            List<LaunchpadColor> colors = status.stream().map(LaunchpadColor::forStatus).collect(Collectors.toList());

            clearRow(i);
            colorRow(i, colors);
            right(i, LaunchpadColor.BRIGHT_RED);
        }

        // clear any unused rows
        for (int i = size; i < 8; i++) {
            clearRow(i);
        }
    }
}
