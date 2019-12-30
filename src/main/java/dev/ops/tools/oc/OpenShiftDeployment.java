package dev.ops.tools.oc;

import io.fabric8.kubernetes.api.model.Pod;

import javax.json.JsonArray;
import javax.json.JsonString;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * The K8s deployment configuration model.
 */
public class OpenShiftDeployment {

    private final String name;
    private final Map<String, String> pods = new TreeMap<>();

    public OpenShiftDeployment(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getPods() {
        return pods;
    }

    public static List<OpenShiftDeployment> fromJson(JsonArray jsonArray) {
        return jsonArray.getValuesAs((Function<JsonString, OpenShiftDeployment>) jsonValue -> new OpenShiftDeployment(jsonValue.getString()));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", OpenShiftDeployment.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .toString();
    }

    public void setPodList(List<Pod> items) {
        pods.clear();
        for (Pod pod : items) {
            boolean isTerminating = pod.getMetadata().getDeletionTimestamp() != null;
            if (!isTerminating) {
                pods.put(pod.getMetadata().getName(), pod.getStatus().getPhase());
            }
        }
    }
}
