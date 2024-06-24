package edu.university.ecs.lab.detection.architecture.models;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

import edu.university.ecs.lab.common.models.Annotation;
import edu.university.ecs.lab.common.models.JClass;
import edu.university.ecs.lab.common.models.Method;
import edu.university.ecs.lab.common.models.enums.ClassRole;
import edu.university.ecs.lab.delta.models.Delta;
import edu.university.ecs.lab.delta.models.enums.ChangeType;
import edu.university.ecs.lab.detection.architecture.models.enums.Scope;
import lombok.Data;

@Data
public class UseCase7 extends UseCase {
    protected static final String NAME = "Affected endpoint due to data access logic update";
    protected static final Scope SCOPE = Scope.METHOD;
    protected static final String DESC = "A repository method was modified and now causes inconsistent results";
    protected JsonObject metaData;

    private UseCase7() {}

    @Override
    public List<? extends UseCase> checkUseCase() {
        // This method should return the list of UseCase3 instances relevant to UseCase7 logic, if any.
        ArrayList<UseCase3> useCases = new ArrayList<>();
        return useCases;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESC;
    }

    @Override
    public Scope getScope() {
        return SCOPE;
    }

    @Override
    public double getWeight() {
        return 0;
    }

    @Override
    public JsonObject getMetaData() {
        return metaData;
    }
    
    public static List<UseCase7> scan(Delta delta, JClass jClass) {
        List<UseCase7> useCases = new ArrayList<>();

        if (!jClass.getClassRole().equals(ClassRole.REPOSITORY) || !delta.getChangeType().equals(ChangeType.MODIFY)) {
            return useCases;
        }
        

        for (Method methodOld : jClass.getMethods()) {
            for (Method methodNew : delta.getClassChange().getMethods()) {
                if (methodOld.getID().equals(methodNew.getID()) && !methodOld.equals(methodNew)) {
                    for (Annotation annotationOld : methodOld.getAnnotations()) {
                        if (annotationOld.getName().equals("Query")) {
                            for (Annotation annotationNew : methodNew.getAnnotations()) {
                                if (annotationNew.getName().equals("Query") && !annotationNew.getContents().equals(annotationOld.getContents())) {
                                    UseCase7 useCase7 = new UseCase7();
                                    JsonObject jsonObject = new JsonObject();
                                    jsonObject.add("Method Declaration", methodNew.toJsonObject());
                                    useCase7.setMetaData(jsonObject);
                                    useCases.add(useCase7);
                                }
                            }
                        }
                    }
                }
            }
        }

        return useCases;
    }
}