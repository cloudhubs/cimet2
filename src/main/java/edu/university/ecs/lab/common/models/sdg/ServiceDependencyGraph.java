package edu.university.ecs.lab.common.models.sdg;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.university.ecs.lab.common.models.ir.*;
import edu.university.ecs.lab.common.models.serialization.JsonSerializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents a network graph for a microservice system.
 * This class holds the details of the nodes and edges that make up the graph.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceDependencyGraph implements JsonSerializable {
    /**
     * Represents the name of the network graph
     */
    private String label;
    /**
     * Holds the timestamp of the current Network graph 
     * (i.e the commit ID that the Network graph represents)
     */
    private String timestamp;
    /**
     * Whether the edges are interpreseted as directed (default true)
     */
    private boolean directed;
    /**
     * Whether several edges between cource and target are allowed, distinguished by the endpoint
     */
    private boolean multigraph;
    /**
     * List of all nodes present in the data (must be unique)
     */
    private Set<String> nodes;
    /**
     * List of Edge objects that represent the communication between nodes
     */
    private Set<EndpointCallEdge> edges;

    /**
     * see {@link JsonSerializable#toJsonObject()}
     */
    @Override
    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();

        Gson gson1 = new Gson();
        Gson gson2 = new Gson();

        String nodesArray = gson1.toJson(nodes);
        String edgeArray = gson2.toJson(edges);

        jsonObject.addProperty("label", label);
        jsonObject.addProperty("timestamp", timestamp);
        jsonObject.addProperty("directed", directed);
        jsonObject.addProperty("multigraph", multigraph);
        jsonObject.addProperty("nodes", nodesArray);
        jsonObject.addProperty("edges", edgeArray);

        return jsonObject;
    }

    /**
     * Creates the network graph from a given MicroserviceSystem.
     *
     * @param microserviceSystem the microservice system to build the graph from.
     */
    public void createGraph(MicroserviceSystem microserviceSystem) {
        this.label = "Test";
        this.timestamp = microserviceSystem.getCommitID();

        List<RestCall> restCalls = new ArrayList<>();

        for (Microservice microservice : microserviceSystem.getMicroservices()) {
            for (JClass service : microservice.getServices()) {
                restCalls.addAll(service.getMethodCalls().stream()
                        .filter(methodCall -> methodCall instanceof RestCall)
                        .map(methodCall -> (RestCall) methodCall)
                        .collect(Collectors.toList()));
            }
        }

        List<Endpoint> endpoints = new ArrayList<>();

        for (Microservice microservice : microserviceSystem.getMicroservices()) {
            for (JClass controller : microservice.getControllers()) {
                endpoints.addAll(controller.getMethods().stream()
                        .filter(methods -> methods instanceof Endpoint)
                        .map(methods -> (Endpoint) methods)
                        .collect(Collectors.toList()));
            }
        }

        List<EndpointCallEdge> edgesList = new ArrayList<>();
        this.nodes = new HashSet<>();

        for (RestCall restCall : restCalls) {
            for (Endpoint endpoint : endpoints) {
                if (restCall.getUrl().equals(endpoint.getUrl()) && restCall.getHttpMethod().equals(endpoint.getHttpMethod())) {
                    edgesList.add(new EndpointCallEdge(restCall.getMicroserviceName(), endpoint.getMicroserviceName(), endpoint.getUrl(), 0));
                    this.nodes.add(endpoint.getMicroserviceName());
                    this.nodes.add(restCall.getMicroserviceName());
                }
            }
        }

        this.edges = new HashSet<>();

        Map<EndpointCallEdge, Long> edgeDuplicateMap = edgesList.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        this.edges = edgeDuplicateMap.entrySet().stream().map(entry -> {
            EndpointCallEdge edge = entry.getKey();
            edge.setWeight(Math.toIntExact(entry.getValue()));
            return edge;
        }).collect(Collectors.toSet());


    }
}
