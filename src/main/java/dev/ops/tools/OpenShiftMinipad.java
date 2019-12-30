package dev.ops.tools;

import dev.ops.tools.midi.MidiSystemHandler;
import dev.ops.tools.oc.OpenShiftController;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;

/**
 * Main application for the OpenShift Minipad.
 */
@Command(version = "OpenShift Minipad 1.0", mixinStandardHelpOptions = true)
class OpenShiftMinipad implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenShiftMinipad.class);

    @Option(names = {"-n", "--namespace"}, defaultValue = "default", description = "the K8s namespace")
    private String namespace;

    @Option(names = {"-f", "--file"}, paramLabel = "JSON_CONFIG", description = "the configuration file", required = true)
    private File configFile;

    public static void main(String[] args) {
        CommandLine.run(new OpenShiftMinipad(), args);
    }

    @Override
    public void run() {
        LOGGER.info("Running OpenShift Minipad ...");

        MidiSystemHandler midiSystem = new MidiSystemHandler();
        midiSystem.infos();

        OpenShiftClient client = new DefaultOpenShiftClient();
        OpenShiftController openShiftController = new OpenShiftController(client, configFile);

        OpenShiftMinipadController minipadController = new OpenShiftMinipadController(midiSystem, openShiftController, namespace);
        minipadController.initialize();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown OpenShift Minipad.");
            minipadController.close();
            client.close();
            midiSystem.destroy();
        }));
    }
}
