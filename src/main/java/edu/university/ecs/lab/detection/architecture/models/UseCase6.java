package edu.university.ecs.lab.detection.architecture.models;

import com.google.gson.JsonObject;
import edu.university.ecs.lab.common.models.ir.JClass;
import edu.university.ecs.lab.common.models.ir.Method;
import edu.university.ecs.lab.common.models.ir.MethodCall;
import edu.university.ecs.lab.common.models.ir.MicroserviceSystem;
import edu.university.ecs.lab.common.models.enums.ClassRole;
import edu.university.ecs.lab.delta.models.Delta;
import edu.university.ecs.lab.delta.models.enums.ChangeType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class UseCase6 extends AbstractUseCase {
    protected static final String TYPE = "UseCase6";
    protected static final String NAME = "Affected endpoint due to business logic update";
    protected static final String DESC = "A service method was modified and now causes inconsistent results for calling endpoints";
    private String oldCommitID;
    private String newCommitID;
    protected JsonObject metaData;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESC;
    }

    @Override
    public double getWeight() {
        return 0;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public JsonObject getMetaData() {
        return metaData;
    }

    public static List<UseCase6> scan(Delta delta, MicroserviceSystem oldSystem, MicroserviceSystem newSystem) {


        JClass jClass = oldSystem.findClass(delta.getOldPath());

        if(!delta.getChangeType().equals(ChangeType.MODIFY) || !jClass.getClassRole().equals(ClassRole.SERVICE)) {
            return new ArrayList<>();
        }

        List<UseCase6> useCases = new ArrayList<>();
        Set<Method> affectedMethods = delta.getClassChange().getMethods();
        Method removeMethod = null;

        // For each methodCall added
        for(MethodCall methodCall : getNewMethodCalls(jClass, delta.getClassChange())) {
            outer:
            {
                for (Method method : affectedMethods) {
                    // If the called object is the same as the return type of the same method
                    // TODO This will report the first instance
                    // TODO This currently does not check if the affected method is called by an endpoint and actually used, flows needed
                    if (method.getReturnType().equals(methodCall.getObjectType())
                            && method.getName().equals(methodCall.getCalledFrom())) {
                        removeMethod = method;

                        UseCase6 useCase6 = new UseCase6();
                        JsonObject jsonObject = new JsonObject();

//                        jsonObject.addProperty("AffectedMethod", method.getID());
//                        jsonObject.addProperty("MethodCall", methodCall.getID());
                        useCase6.setOldCommitID(oldSystem.getCommitID());
                        useCase6.setNewCommitID(newSystem.getCommitID());

                        useCase6.setMetaData(jsonObject);
                        useCases.add(useCase6);

                        break outer;
                    }
                }
            }

            if(Objects.nonNull(removeMethod)) {
                affectedMethods.remove(removeMethod);
                removeMethod = null;
            }
        }

        return useCases;
    }

    /**
     * This method gets new method calls not present in oldClass
     *
     * @param oldClass
     * @param newClass
     * @return
     */
    private static Set<MethodCall> getNewMethodCalls(JClass oldClass, JClass newClass) {
        return newClass.getMethodCalls().stream().filter(methodCall -> !oldClass.getMethodCalls().contains(methodCall)).collect(Collectors.toSet());
    }
}
