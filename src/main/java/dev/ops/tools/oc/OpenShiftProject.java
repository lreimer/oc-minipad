package dev.ops.tools.oc;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * The OpenShift project configuration model.
 */
public class OpenShiftProject {

    private final String name;
    private final Mode mode;
    private final List<OpenShiftDeployment> deployments;

    public OpenShiftProject(String name, Mode mode) {
        this.name = name;
        this.mode = mode;
        this.deployments = new CopyOnWriteArrayList<>();
    }

    public String getName() {
        return name;
    }

    public Mode getMode() {
        return mode;
    }

    public List<OpenShiftDeployment> getDeployments() {
        return deployments;
    }

    public OpenShiftDeployment getDeployment(int index) {
        return deployments.get(index);
    }

    public void setDeployments(List<OpenShiftDeployment> deployments) {
        this.deployments.clear();
        this.deployments.addAll(deployments);
    }

    public void addDeployment(OpenShiftDeployment openShiftDeployment) {
        deployments.add(openShiftDeployment);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", OpenShiftProject.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("mode=" + mode)
                .add("deployments=" + deployments)
                .toString();
    }

    public static List<OpenShiftProject> fromJson(JsonArray jsonArray) {
        return jsonArray.getValuesAs((Function<JsonObject, OpenShiftProject>) jsonObject -> {
            String name = jsonObject.getString("project", "default");
            Mode mode = Mode.valueOf(jsonObject.getString("mode", Mode.DYNAMIC.name()));

            OpenShiftProject ns = new OpenShiftProject(name, mode);
            if (Mode.STATIC == mode) {
                List<OpenShiftDeployment> deployments = OpenShiftDeployment.fromJson(jsonObject.getJsonArray("deployments"));
                ns.setDeployments(deployments);
            }

            return ns;
        });
    }

    public OpenShiftDeployment getDeploymentByName(String name) {
        OpenShiftDeployment found = null;
        for (OpenShiftDeployment deployment : deployments) {
            if (Objects.equals(deployment.getName(), name)) {
                found = deployment;
                break;
            }
        }
        return found;
    }

    public void removeDeployment(String name) {
        OpenShiftDeployment deployment = getDeploymentByName(name);
        deployments.remove(deployment);
    }

    public enum Mode {
        DYNAMIC, STATIC
    }
}
