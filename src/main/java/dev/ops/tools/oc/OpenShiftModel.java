package dev.ops.tools.oc;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Root class for the OpenShift configuration model.
 */
public class OpenShiftModel {

    private final List<OpenShiftProject> projects = new CopyOnWriteArrayList<>();

    public List<OpenShiftProject> getProjects() {
        return projects;
    }

    public void setProjects(List<OpenShiftProject> projects) {
        this.projects.clear();
        this.projects.addAll(projects);
    }

    public OpenShiftProject getProject(int index) {
        return projects.get(index);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", OpenShiftModel.class.getSimpleName() + "[", "]")
                .add("projects=" + projects)
                .toString();
    }

    public static OpenShiftModel fromFile(File file) {
        OpenShiftModel config = new OpenShiftModel();
        try (JsonReader reader = Json.createReader(new FileInputStream(file))) {
            JsonArray jsonArray = reader.readArray();
            config.setProjects(OpenShiftProject.fromJson(jsonArray));
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading JSON config file.", e);
        }
        return config;
    }

    public OpenShiftProject getProjectByName(String name) {
        OpenShiftProject found = null;
        for (OpenShiftProject p : projects) {
            if (Objects.equals(p.getName(), name)) {
                found = p;
                break;
            }
        }
        return found;
    }
}
