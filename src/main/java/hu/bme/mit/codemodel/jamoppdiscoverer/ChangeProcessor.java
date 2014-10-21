package hu.bme.mit.codemodel.jamoppdiscoverer;

import hu.bme.mit.codemodel.jamoppdiscoverer.iterators.DependencyCollector;
import hu.bme.mit.codemodel.jamoppdiscoverer.utils.PackageName;
import hu.bme.mit.codemodel.jamoppdiscoverer.utils.RelativePath;
import hu.bme.mit.codemodel.jamoppdiscoverer.whitepages.pojo.Dependency;
import hu.bme.mit.codemodel.jamoppdiscoverer.whitepages.DependencyManager;

import java.io.File;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ChangeProcessor {

    /**
     * The set of files to be processed in the second iteration. Beside every modification, the relevant files also need
     * to be reprocessed. This includes every file, that depends on the modified ones and every one in the same package.
     */
    protected static Set<String> filesToProcess = new HashSet<>();

    protected static Set<String> deletedFiles = new HashSet<>();

    protected static DependencyCollector dependencyCollector = new DependencyCollector();

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Process a newly added file.
     *
     * When a new file is added, the dependency information should be saved in the database. The dependencies already in
     * the system had been processed processed, and the new ones will be processed in this iteration.
     *
     * The only file to be processed is the one added.
     *
     * @param filePath
     *            The path of the newly added file.
     */
    public static void addedNewFile(String filePath) {
        dependencyCollector.execute(new File(filePath));
        filesToProcess.add(filePath);

        System.out.println("Added a new file: " + RelativePath.of(filePath));
    }

    /**
     * Process a deleted file.
     *
     * When a file is deleted, its dependency information should be deleted, and every file, that depends on it should
     * be reprocessed.
     *
     * @param filePath
     *            The path of the deleted file.
     */
    public static void deletedFile(String filePath) {
        deletedFiles.add(filePath);

        DependencyManager dm = null;
        try {
            dm = DependencyManager.getInstance();
            Dependency dependency = dm.find(RelativePath.of(filePath));

            if (dependency == null) {
                System.out.println("File is not in the database.\t" + RelativePath.of(filePath));
                return;
            }

            Set<String> usedBy = dependency.getUsedBy();
            filesToProcess.addAll(usedBy);
            dm.removeDescriptor(dependency);

            // process the files in the same package
            for (Dependency dInPackage : dm.findAll(PackageName.of(dependency.getFQN()))) {
                filesToProcess.add(dInPackage.getRelativeLocation());
            }

            System.out.println("Deleted the file: " + RelativePath.of(filePath));
            System.out.println("\tNeeds to be reprocessed:\t" + Arrays.toString(usedBy.toArray()));

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * Process a modified file.
     *
     * When a file is modified, all of its dependencies should be deleted and reprocessed. Every file, that depends on
     * it should be also reprocessed.
     *
     * @param filePath
     *            The path of the modified file.
     */
    public static void modifiedFile(String filePath) {
        deletedFile(filePath); // marks the files depending on the file to be reprocessed
        addedNewFile(filePath); // updates the dependencies of the file
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static Set<String> getFilesToProcess() {
        return filesToProcess;
    }

    public static Set<String> getDeletedFiles() {
        return deletedFiles;
    }
}
