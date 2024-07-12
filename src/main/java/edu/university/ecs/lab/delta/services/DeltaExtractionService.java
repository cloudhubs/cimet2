package edu.university.ecs.lab.delta.services;

import com.google.gson.JsonObject;
import edu.university.ecs.lab.common.config.Config;
import edu.university.ecs.lab.common.config.ConfigUtil;
import edu.university.ecs.lab.common.error.Error;
import edu.university.ecs.lab.common.models.ir.JClass;
import edu.university.ecs.lab.common.models.ir.Microservice;
import edu.university.ecs.lab.common.models.ir.MicroserviceSystem;
import edu.university.ecs.lab.common.services.GitService;
import edu.university.ecs.lab.common.utils.FileUtils;
import edu.university.ecs.lab.common.utils.JsonReadWriteUtils;
import edu.university.ecs.lab.common.utils.SourceToObjectUtils;
import edu.university.ecs.lab.delta.models.Delta;
import edu.university.ecs.lab.delta.models.SystemChange;
import edu.university.ecs.lab.delta.models.enums.ChangeType;
import org.eclipse.jgit.diff.DiffEntry;

import javax.json.Json;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Service for extracting the differences between two commits of a repository.
 * This class does cleaning of output so not all changes will be reflected in
 * the Delta output file.
 */
public class DeltaExtractionService {

    /**
     * Config object representing the contents of the config file
     */
    private final Config config;

    /**
     * GitService instance for interacting with the local repository
     */
    private final GitService gitService;

    /**
     * The old commit for comparison
     */
    private final String commitOld;

    /**
     * The new commit for comparison
     */
    private final String commitNew;

    /**
     * The old IR for validating delta file changes
     */
    private final MicroserviceSystem oldSystem;

    /**
     * System change object that will be returned
     */
    private SystemChange systemChange;

    /**
     * Represents a list of diff entries that are related to pom.xml add or delete
     */
    private final List<DiffEntry> pomDiffs;

    /**
     * The type of change that is made
     */
    private ChangeType changeType;


    /**
     * Constructor for the DeltaExtractionService
     *
     * @param configPath path to the config file
     * @param commitOld old commit for comparison
     * @param commitNew new commit for comparison
     */
    public DeltaExtractionService(String configPath, String oldIRPath, String commitOld, String commitNew) {
        this.config = ConfigUtil.readConfig(configPath);
        this.gitService = new GitService(configPath);
        this.commitOld = commitOld;
        this.commitNew = commitNew;
        this.oldSystem = JsonReadWriteUtils.readFromJSON(oldIRPath, MicroserviceSystem.class);
        pomDiffs = new ArrayList<>();
    }

    /**
     * Generates Delta file representing changes between commitOld and commitNew
     */
    public void generateDelta() {
        List<DiffEntry> differences = null;

        // Ensure we start at commitOld
        gitService.resetLocal(commitOld);

        try {
            differences = gitService.getDifferences(commitOld, commitNew);

        } catch (Exception e) {
            Error.reportAndExit(Error.GIT_FAILED);
        }

        // Advance the local commit for parsing
        gitService.resetLocal(commitNew);

        // process/write differences to delta output
        processDelta(differences);

    }

    /**
     * Process the differences between the local and remote repository and write the differences to a
     * file.
     *
     * @param diffEntries the list of differences extracted by GitService
     */
//    public void processDelta2(List<DiffEntry> diffEntries) {
//        List<DiffEntry> pomDiffs = new ArrayList<>();
//
//        // Set up a new SystemChangeObject
//        SystemChange systemChange = new SystemChange();
//        systemChange.setOldCommit(commitOld);
//        systemChange.setNewCommit(commitNew);
//
//        // process each difference
//        for (DiffEntry entry : diffEntries) {
//
//            String path = entry.getChangeType().equals(DiffEntry.ChangeType.ADD) ? entry.getOldPath() : entry.getNewPath();
//
//            outer:
//            {
//                // Filter out nested poms
//
//                // If we are a pom
//                if (entry.getOldPath().endsWith("/pom.xml") || entry.getNewPath().endsWith("/pom.xml")) {
//                    if(entry.getChangeType().equals(DiffEntry.ChangeType.ADD)) {
//                        DiffEntry remove = null;
//
//                        // For all existing pomEntry's in this delta
//                        for (DiffEntry pomEntry : pomDiffs) {
//
//                            // If we have an existing entry that is more specific than the current entry
//                            if (pomEntry.getNewPath().replace("/pom.xml", "").startsWith(path.replace("/pom.xml", ""))) {
//                                // Skip it
//                                break outer;
//
//                            // If the current entry is more specific than an existing entry
//                            } else if (path.replace("/pom.xml", "").startsWith(pomEntry.getNewPath().replace("/pom.xml", ""))) {
//                                // Remove the old entry
//                                remove = pomEntry;
//                            }
//
//                        }
//
//
//                        // Check existing microservices if our entry is more specific as well
//                        for (Microservice microservice : oldSystem.getMicroservices()) {
//
//                            // If we find a match, orphanize and redistribute those classes naturally
//                            if (("/" + path.replace("/pom.xml", "")).startsWith(microservice.getPath())) {
//                                 oldSystem.orphanizeAndAdopt(microservice);
//                                 oldSystem.getMicroservices().remove(microservice);
//                                 break;
//                            }
//
//                        }
//
//
//                        // If we remove a pom diff, delete the delta entry that already passed
//                        if(remove != null) {
//                            // Remove from current poms
//                            pomDiffs.remove(remove);
//                            // Remove delta entry
//                            Iterator<Delta> deltaIter = systemChange.getChanges().iterator();
//                            while (deltaIter.hasNext()) {
//                                Delta d = deltaIter.next();
//                                if((d.getOldPath() == null && remove.getOldPath().equals("/dev/null")
//                                    || d.getOldPath() != null && d.getOldPath().equals("/" + remove.getOldPath())) &&
//                                        (d.getNewPath() == null && remove.getNewPath().equals("/dev/null")
//                                                || d.getNewPath() != null && d.getNewPath().equals("/" + remove.getNewPath()))) {
//                                    deltaIter.remove();
//                                    break;
//                                }
//                            }
//                        }
//
//                        pomDiffs.add(entry);
//
//
//                    // If we are modifying or deleting an existing
//                    } else {
//                        boolean match = false;
//                        // If we try to delete or modify a pom that doesnt exist because it was filtered out
//                        for (Microservice microservice : oldSystem.getMicroservices()) {
//                            if (microservice.getPath().equals("/" + path.replace("/pom.xml", ""))) {
//                                match = true;
//                                break;
//                            }
//
//                        }
//
//                        // If there was no match, it doesn't exist
//                        if(!match) {
//                            // Skip
//                            break outer;
//                        }
//
//                    }
//
//
//                }
//
//                // If its not a java file and doesnt end with pom.xml
//
//
//                // If paths doesnt end with java or (path doesnt end with java or pom)
//                if (!path.endsWith(".java") && !path.endsWith("pom.xml")) {
//                    continue;
//                }
//
//                String oldPath = "";
//                String newPath = "";
//
//                if (DiffEntry.ChangeType.DELETE.equals(entry.getChangeType())) {
//                    oldPath = FileUtils.GIT_SEPARATOR + entry.getOldPath();
//                    newPath = null;
//
//                } else if (DiffEntry.ChangeType.ADD.equals(entry.getChangeType())) {
//                    oldPath = null;
//                    newPath = FileUtils.GIT_SEPARATOR + entry.getNewPath();
//
//                } else {
//                    oldPath = FileUtils.GIT_SEPARATOR + entry.getOldPath();
//                    newPath = FileUtils.GIT_SEPARATOR + entry.getNewPath();
//
//                }
//
//                //TODO BAD -- If we modify/delete a file that isn't present in the old system (was skipped because it has no annotation)
//                // Add get's skipped when we parse returns null
//                if (entry.getChangeType().equals(DiffEntry.ChangeType.DELETE) && !path.endsWith("pom.xml")) {
//                    JClass jClass = oldSystem.findClass(oldPath);
//                    if (jClass == null) {
//                        continue;
//                    }
//                }
//
//
//                // Get the class, if we are a delete the file for parsing no longer exists
//                // If we are a pom.xml we cannot parse
//                JClass jClass = null;
//                ChangeType changeType = ChangeType.fromDiffEntry(entry);
//                if (!path.endsWith("pom.xml")) {
//
//                    if (!entry.getChangeType().equals(DiffEntry.ChangeType.DELETE)) {
//
//                        jClass = SourceToObjectUtils.parseClass(new File(FileUtils.gitPathToLocalPath(newPath, config.getRepoName())), config, "");
//
//                        // If we try to parse and it is still null, for ADD we will skip
//
//
//                        // For MODIFY we will let pass since it might be modifying a previously accepted file
//
//                    }
//
//                    // WORKAROUND, RENAME MODIFY'S THAT WITH NULL CLASSCHANGE (UNPARSABLE) TO DELETE
//                    if (entry.getChangeType().equals(DiffEntry.ChangeType.MODIFY)) {
//                        if (jClass == null && oldSystem.findClass(oldPath) == null) {
//                            continue;
//                        } else if (jClass == null && oldSystem.findClass(oldPath) != null) {
//                            changeType = ChangeType.DELETE;
//                        } else if (jClass != null && oldSystem.findClass(oldPath) == null) {
//                            changeType = ChangeType.ADD;
//                        }
//                        // LIKEWISE IF WE "MODIFYING" A NON_PRESENT CLASS BECAUSE IT WAS INITIALLY UNPARSABLE
//                    } else if (entry.getChangeType().equals(DiffEntry.ChangeType.ADD) && jClass == null) {
//                        continue;
//                    } else if (entry.getChangeType().equals(DiffEntry.ChangeType.DELETE) && oldSystem.findClass(oldPath) == null) {
//                        continue;
//                    }
//
//                }
//
//
//                systemChange.getChanges().add(new Delta(oldPath, newPath, changeType, jClass));
//
//            }
//        }
//
//        JsonReadWriteUtils.writeToJSON("./output/Delta.json", systemChange);
//
//        System.out.println("Delta extracted: from " + commitOld + " to " + commitNew + " at ./output/Delta.json");
//
//    }


    public void processDelta(List<DiffEntry> diffEntries) {
        // Set up a new SystemChangeObject
        systemChange = new SystemChange();
        systemChange.setOldCommit(commitOld);
        systemChange.setNewCommit(commitNew);
        JsonObject data = null;


        // process each difference
        for (DiffEntry entry : diffEntries) {
            String path = entry.getChangeType().equals(DiffEntry.ChangeType.ADD) ? entry.getOldPath() : entry.getNewPath();

            // Guard condition, skip invalid files
            if(!FileUtils.isValidFile(path)) {
               continue;
            }

            // Setup oldPath, newPath for Delta
            String oldPath = "";
            String newPath = "";

            if (DiffEntry.ChangeType.DELETE.equals(entry.getChangeType())) {
                oldPath = FileUtils.GIT_SEPARATOR + entry.getOldPath();
                newPath = null;

            } else if (DiffEntry.ChangeType.ADD.equals(entry.getChangeType())) {
                oldPath = null;
                newPath = FileUtils.GIT_SEPARATOR + entry.getNewPath();

            } else {
                oldPath = FileUtils.GIT_SEPARATOR + entry.getOldPath();
                newPath = FileUtils.GIT_SEPARATOR + entry.getNewPath();

            }


            ChangeType changeType = ChangeType.fromDiffEntry(entry);

            // Special check for configuration file manipulations only
            if(FileUtils.isConfigurationFile(path)) {
                configurationChange(entry, path);
            } else {

                switch(changeType) {
                    case ADD:
                        data = add(path);
                        break;
                    case MODIFY:
                        data = modify(entry, path, oldPath);
                        break;
                    case DELETE:
                        data = delete(path);
                }
            }

            // If data is null we hit a skip condition
            if(data == null) {
                continue;
            }


            systemChange.getChanges().add(new Delta(oldPath, newPath, changeType, data));
        }

        // Output the system changes
        JsonReadWriteUtils.writeToJSON("./output/Delta.json", systemChange);

        // Report
        System.out.println("Delta extracted: from " + commitOld + " to " + commitNew + " at ./output/Delta.json");

    }

    /**
     * This method parses a newly added file
     *
     * @param path the file path that will be parsed
     * @return null if the JClass is unparsable or a data object representing a parsed JClass or config file
     */
    private JsonObject add(String path) {
        JsonObject jsonObject = new JsonObject();
        if(FileUtils.isConfigurationFile(path)) {
            jsonObject.add("config", SourceToObjectUtils.parseConfigurationFile(new File(path)));
        } else {
            JClass jClass = SourceToObjectUtils.parseClass(new File(FileUtils.gitPathToLocalPath(path, config.getRepoName())), config, "");
            if(jClass == null) {
                return null;
            }
            jsonObject.add("jClass", jClass.toJsonObject());
        }

        return jsonObject;
    }

    /**
     * This method parses modified files and handles additional logic related to orphan management
     *
     * [!] Note: Due to the management of orphans we will do special checks here.
     *
     * @param diffEntry
     * @param path the file path that will be parsed
     * @return
     */
    private JsonObject modify(DiffEntry diffEntry, String path, String oldPath) {
        JsonObject jsonObject = new JsonObject();

        if(FileUtils.isConfigurationFile(path)) {
            jsonObject.add("config", SourceToObjectUtils.parseConfigurationFile(new File(path)));
        } else {
            JClass jClass = SourceToObjectUtils.parseClass(new File(FileUtils.gitPathToLocalPath(path, config.getRepoName())), config, "");

            // Similar to add check, but if we couldn't parse and the class exists in old system we must allow it
            if (jClass == null && oldSystem.findClass(oldPath) == null) {
                return null;
            // If the class is unparsable and the class exists in the old system we must delete it now
            } else if (jClass == null && oldSystem.findClass(oldPath) != null) {
                changeType = ChangeType.DELETE;
            // If the class is parsable and the class doesn't exist in the old system we must add it now
            } else if (jClass != null && oldSystem.findClass(oldPath) == null) {
                changeType = ChangeType.ADD;
            }

            jsonObject.add("jClass", jClass.toJsonObject());
        }


        return jsonObject;
    }

    /**
     * This method does no parsing but validates that the class to be deleted does exist
     *
     * @param oldPath the path before the change (new path is /dev/null)
     * @return null if it does not exist or empty data object if it does
     */
    private JsonObject delete(String oldPath) {
        return oldSystem.findClass(oldPath) == null ? null : new JsonObject();
    }

    /**
     * This method handles manipulations pom only and does additional logic regarding
     * additions/removals of microservices which is based around pom manipulation
     *
     * @param entry the diffentry representing the pom change
     * @param path the classPath that will be parsed
     * @return
     */
    private JsonObject configurationChange(DiffEntry entry, String path) {
        // Special manipulation for poms, they control creation/deletion of microservices
        // If we are modifying the pom, the microservice remains, let's just update the files for it
        if(path.endsWith("pom.xml") && !entry.getChangeType().equals(DiffEntry.ChangeType.MODIFY)) {
            // If we add a pom
            if(entry.getChangeType().equals(DiffEntry.ChangeType.ADD)) {
                DiffEntry remove = null;

                // For all existing pomEntry's in this delta
                for (DiffEntry pomEntry : pomDiffs) {

                    // If we have an existing entry that is more specific than the current entry
                    if (pomEntry.getNewPath().replace("/pom.xml", "").startsWith(path.replace("/pom.xml", ""))) {
                        return null;

                        // If the current entry is more specific than an existing entry
                    } else if (path.replace("/pom.xml", "").startsWith(pomEntry.getNewPath().replace("/pom.xml", ""))) {
                        // Remove the old entry
                        remove = pomEntry;
                    }

                }


                // Check existing microservices if our entry is more specific as well
                for (Microservice microservice : oldSystem.getMicroservices()) {

                    // If we find a match, orphanize and redistribute those classes naturally
                    if (("/" + path.replace("/pom.xml", "")).startsWith(microservice.getPath())) {
                        oldSystem.orphanizeAndAdopt(microservice);
                        oldSystem.getMicroservices().remove(microservice);
                        break;
                    }

                }


                // If we remove a pom diff, delete the delta entry that already passed
                if(remove != null) {
                    // Remove from current poms
                    pomDiffs.remove(remove);
                    // Remove delta entry
                    Iterator<Delta> deltaIter = systemChange.getChanges().iterator();
                    while (deltaIter.hasNext()) {
                        Delta d = deltaIter.next();
                        if((d.getOldPath() == null && remove.getOldPath().equals("/dev/null")
                                || d.getOldPath() != null && d.getOldPath().equals("/" + remove.getOldPath())) &&
                                (d.getNewPath() == null && remove.getNewPath().equals("/dev/null")
                                        || d.getNewPath() != null && d.getNewPath().equals("/" + remove.getNewPath()))) {
                            deltaIter.remove();
                            break;
                        }
                    }
                }

                // Add the current entry
                pomDiffs.add(entry);


                // If we are modifying or deleting an existing pom
            } else {
                boolean match = false;
                // If we try to delete or modify a pom that doesnt exist because it was filtered out
                for (Microservice microservice : oldSystem.getMicroservices()) {
                    if (microservice.getPath().equals("/" + path.replace("/pom.xml", ""))) {
                        match = true;
                        break;
                    }

                }

                // If there was no match, it doesn't exist
                // TODO is this possible?
                if(!match) {
                    // Skip
                    return null;
                }

            }
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("pom", SourceToObjectUtils.parseConfigurationFile(new File(path)));

        return jsonObject;

    }


}
