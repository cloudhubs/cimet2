package edu.university.ecs.lab.detection.antipatterns.services;

import edu.university.ecs.lab.common.models.ir.Method;
import edu.university.ecs.lab.common.models.sdg.MethodDependencyGraph;
import edu.university.ecs.lab.detection.antipatterns.models.CyclicDependency;

import java.util.*;

/**
 * Service class for detecting cyclic dependencies in a method network graph.
 */
public class CyclicDependencyMethodLevelService {

    private List<List<String>> allCycles = new ArrayList<>();
    private Set<Method> visited = new HashSet<>();
    private List<String> recStack = new ArrayList<>();
    private Map<String, String> parentMap = new HashMap<>();
    private MethodDependencyGraph graph = null;

    /**
     * Finds all cyclic dependencies in the given network graph.
     * 
     * @param graph the Microservice System to analyze
     * @return a CyclicDependency object representing all detected cycles
     */
    public CyclicDependency findCyclicDependencies(MethodDependencyGraph graph) {
        allCycles = new ArrayList<>();
        visited = new HashSet<>();
        recStack = new ArrayList<>();
        parentMap = new HashMap<>();
        this.graph = graph;

        // Find cycles for each node in method dependency graph
        graph.vertexSet().stream().filter(node -> !visited.contains(node)).forEach(this::findCycles);

        return new CyclicDependency(allCycles);
    }

    /**
     * Checks if there is a cycle starting from the current node.
     *
     * @param currentNode        the current node to check
     */
    private void findCycles(Method currentNode) {
        // Add current node to visited list
        visited.add(currentNode);

        // Check if current microservice is different from last microservice and add to recStack if switched
        String currentNodeMS = currentNode.getMicroserviceName();
        boolean switched_service = recStack.isEmpty() || !currentNodeMS.equals(recStack.get(recStack.size() - 1));
        if (switched_service) {
            recStack.add(currentNodeMS);
        }

        // Iterate through each node in adjacency list
        this.graph.getAdjacency(currentNode).forEach(neighbor -> {
            String neighborMS = neighbor.getMicroserviceName();
            
            // Check if adjacent node is on the same microservice
            boolean switching_service = !neighborMS.equals(currentNodeMS);

            // If adjacent node has not already been checked and microservices are different, 
            // add current microservice and neigbor microservice to parent map. Otherwise, use 
            // parent map to reconstruct cycle path and add to allCycles list.
            if (!visited.contains(neighbor)) {
                if (switching_service) {
                    parentMap.put(neighborMS, currentNodeMS);
                }
                findCycles(neighbor);
            } else {
                if (switching_service && recStack.contains(neighborMS)) {
                    List<String> cyclePath = reconstructCyclePath(neighborMS, currentNodeMS);
                    allCycles.add(cyclePath);
                }
            }
        });
        if (switched_service) {
            recStack.remove(recStack.size()-1);
        }
    }

    /**
     * Reconstructs the cycle path from startNode to currentNode using the parentMap.
     * 
     * @param startNode  the start node of the cycle
     * @param currentNode the current node to reconstruct path to
     * @return the list of nodes representing the cycle path
     */
    private List<String> reconstructCyclePath(String startNode, String currentNode) {
        List<String> fullCyclePath = new ArrayList<>();
        String node = currentNode;

        // Iterate through each node in parent map until startNode is reached, adding each to 
        // the cycle path
        fullCyclePath.add(startNode);
        while (node != null && !node.equals(startNode)) {
            fullCyclePath.add(node);
            node = parentMap.get(node);
        }
        fullCyclePath.add(startNode);

        return fullCyclePath;
    }
}
