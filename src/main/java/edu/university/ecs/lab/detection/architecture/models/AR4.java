package edu.university.ecs.lab.detection.architecture.models;

import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;

import edu.university.ecs.lab.common.models.ir.Endpoint;
import edu.university.ecs.lab.common.models.ir.JClass;
import edu.university.ecs.lab.common.models.ir.Microservice;
import edu.university.ecs.lab.common.models.ir.MicroserviceSystem;
import edu.university.ecs.lab.common.models.ir.RestCall;
import edu.university.ecs.lab.common.models.enums.ClassRole;
import edu.university.ecs.lab.delta.models.Delta;
import edu.university.ecs.lab.delta.models.enums.ChangeType;
import lombok.Data;

/**
 * Architectural Rule 4 Class: Floating endpoint
 */
@Data
public class AR4 extends AbstractAR {

    /**
     * Architectural rule 4 details
     */ 
    protected static final String TYPE = "Architectural Rule 4";
    protected static final String NAME = "Floating endpoint";
    protected static final String DESC = "No rest calls reference this endpoint. This endpoint is now unused by any other microservice";
    
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
    public JsonObject getMetaData() {
        return metaData;
    }

    /**
     * Scan and compare old microservice system and new microservice system to identify last call removal
     * 
     * @param delta change between old commit and new microservice systems
     * @param oldSystem old commit of microservice system
     * @param newSystem new commit of microservice system
     * @return list of endpoints that were once called but are no longer called
     */
    public static List<AR4> scan(Delta delta, MicroserviceSystem oldSystem, MicroserviceSystem newSystem) {
        List<AR4> archRules = new ArrayList<>();

        // If we are not removing or modifying a service
        JClass jClass = delta.getChangeType().equals(ChangeType.MODIFY) ? delta.getClassChange() : oldSystem.findClass(delta.getOldPath());

        if (delta.getChangeType().equals(ChangeType.ADD) || !jClass.getClassRole().equals(ClassRole.SERVICE)) {
            return archRules;
        }

        // Get endpoints that do not have any calls
        Set<Endpoint> uncalledEndpoints = getEndpointsWithNoCalls(newSystem);

        List<RestCall> restCalls = new ArrayList<>();

        if (delta.getChangeType().equals(ChangeType.MODIFY)) {
            restCalls = getRemovedRestCalls(delta, jClass);
        } else if (delta.getChangeType().equals(ChangeType.DELETE)) {
            restCalls = jClass.getRestCalls();
        }

        Endpoint removeEndpoint = null;
        // For each restCall removed
        for (RestCall restCall : restCalls) {
            // TODO this approach picks the first rest call that no longer calls an uncalled endpoint
            endpointLoop:
            {
                // For endpoint with no call
                for (Endpoint endpoint : uncalledEndpoints) {
                    // If we match, they once called but no longer call
                    if (RestCall.matchEndpoint(restCall, endpoint)) {
                        AR4 archRule4 = new AR4();
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.add("RestCall", restCall.toJsonObject());
                        archRule4.setMetaData(jsonObject);
                        archRule4.setOldCommitID(oldSystem.getCommitID());
                        archRule4.setNewCommitID(newSystem.getCommitID());
                        archRules.add(archRule4);
                        removeEndpoint = endpoint;
                        break endpointLoop;
                    }
                }
            }

            // Update endpoint loop as we potentially found a match
            if(Objects.nonNull(removeEndpoint)) {
                uncalledEndpoints.remove(removeEndpoint);
            }
            removeEndpoint = null;
        }


        return archRules;
    }

    /**
     * This method collects rest calls that were modified and are no longer present
     * in the new system.
     *
     * @param delta delta change associated
     * @param oldClass the delta changed class from the oldSystem
     * @return a set of rest calls
     */
    private static List<RestCall> getRemovedRestCalls(Delta delta, JClass oldClass){
        List<RestCall> removedRestCalls = new ArrayList<>();

        // For the restCalls in delta
        for(RestCall modifiedRestCall: delta.getClassChange().getRestCalls()){
            outer: {
                // For the restCalls in oldClass
                for(RestCall existingRestCall: oldClass.getRestCalls()){
                    // If they match we do not have a removed call
                    if(existingRestCall.equals(modifiedRestCall)){
                        break outer;
                    }
                }

                // If we loop through them all and find no match add them to the removed calls
                removedRestCalls.add(modifiedRestCall);
            }
        }

        return removedRestCalls;
    }

    /**
     * This method generates a list of endpoints that have no rest calls
     *
     * @param newSystem the new system to check for endpoints
     * @return a list of endpoints
     */
    private static Set<Endpoint> getEndpointsWithNoCalls(MicroserviceSystem newSystem) {
        Set<Endpoint> endpointsNoRC = new HashSet<>();

        for (Microservice microservice : newSystem.getMicroservices()) {
            for (JClass controller : microservice.getControllers()) {
                for (JClass service : microservice.getServices()) {
                    for (Endpoint endpoint : controller.getEndpoints()) {
                        boolean hasRestCall = false;
                        for (RestCall restCall : service.getRestCalls()) {
                            if (RestCall.matchEndpoint(restCall, endpoint)) {
                                hasRestCall = true;
                                break;
                            }
                        }
                        if (!hasRestCall) {
                            endpointsNoRC.add(endpoint);
                        }
                    }
                }
            }
        }

        return endpointsNoRC;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Scan and compare old microservice system and new microservice system to identify last call removal
     * 
     * @param oldSystem old commit of microservice system
     * @param newSystem new commit of microservice system
     * @return list of endpoints that have no calls in new commit
     */
    public static List<AR4> scan2(MicroserviceSystem oldSystem, MicroserviceSystem newSystem) {
        List<AR4> archRules = new ArrayList<>();

        // If we are not removing or modifying a service

        // Get endpoints that do not have any calls
        Set<Endpoint> allEndpoints = newSystem.getMicroservices().stream().flatMap(microservice -> microservice.getEndpoints().stream()).collect(Collectors.toSet());


        for(Endpoint endpoint: allEndpoints){
            if(!findMatch(endpoint, newSystem)){
                AR4 archRule4 = new AR4();
                JsonObject jsonObject = new JsonObject();
                jsonObject.add("Endpoint", endpoint.toJsonObject());
                archRule4.setMetaData(jsonObject);
                archRule4.setOldCommitID(oldSystem.getCommitID());
                archRule4.setNewCommitID(newSystem.getCommitID());
                archRules.add(archRule4);
            }
        }


        return archRules;
    }

    /**
     * Check for modified/deleted endpoint in new system
     * 
     * @param endpoint to find in new system
     * @param newSystem commit to search in for endpoint
     * @return true if a REST call is found to match the endpoint, false otherwise
     */ 
    private static boolean findMatch(Endpoint endpoint, MicroserviceSystem newSystem) {
        for (Microservice microservice : newSystem.getMicroservices()) {
            for (RestCall restcall : microservice.getRestCalls()) {
                if (RestCall.matchEndpoint(restcall, endpoint)) {
                    return true;
                }

            }
        }
        return false;
    }
}
