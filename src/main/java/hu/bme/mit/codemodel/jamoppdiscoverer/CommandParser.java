package hu.bme.mit.codemodel.jamoppdiscoverer;

import com.google.common.collect.HashMultimap;
import hu.bme.mit.bigmodel.fourstore.FourStoreDriverCrud;
import hu.bme.mit.codemodel.jamoppdiscoverer.iterators.FileDiscoverer;
import hu.bme.mit.codemodel.jamoppdiscoverer.iterators.InitializationIterator;
import hu.bme.mit.codemodel.jamoppdiscoverer.utils.ReadStream;
import hu.bme.mit.codemodel.jamoppdiscoverer.utils.RelativePath;
import hu.bme.mit.codemodel.jamoppdiscoverer.whitepages.DependencyManager;
import hu.bme.mit.codemodel.jamoppdiscoverer.whitepages.pojo.Dependency;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CommandParser {

    protected final Options options = new Options();
    protected final CommandLineParser parser = new PosixParser();
    protected CommandLine cmd;

    protected String newFilesListPath = null;
    protected String modifiedFilesListPath = null;
    protected String deletedFilesListPath = null;
    protected static final int NTHREDS = 2;
    //    protected String gitOutputPath = null;

    protected static FourStoreDriverCrud fourstore = new FourStoreDriverCrud("trainbenchmark_cluster", true);

    // -----------------------------------------------------------------------------------------------------------------

    public void initOptions() {
        options.addOption("help", false, "Displays this text.");
        options.addOption("new", true, "Path of the file containing the paths of new files, separated with new lines.");
        options.addOption("modified", true, "Path of the file containing the paths of modified files, separated with new lines.");
        options.addOption("deleted", true, "Path of the file containing the paths of deleted files, separated with new lines.");
//        options.addOption("git", true, "Path of the file containing the output of git.");
    }

    public void processArguments(String[] args) {

        if (Arrays.asList(args).contains("-help")) {
            printHelp();
            System.exit(0);
        }

        try {
            cmd = parser.parse(options, args);

            newFilesListPath = cmd.getOptionValue("new");
            modifiedFilesListPath = cmd.getOptionValue("modified");
            deletedFilesListPath = cmd.getOptionValue("deleted");
//        gitOutputPath = cmd.getOptionValue("git");
        } catch (ParseException e) {
            printHelp();
        }
    }

    public void execute() {

        long dgStart = System.currentTimeMillis();
        processChanges();
        System.out.println("[DepGraph] " + (System.currentTimeMillis() - dgStart));



        processFiles();
    }

    protected void processFiles() {
        // ----------------------------------------------------------------------------------------------------- PROCESS

        try {

            long agsStart = System.currentTimeMillis();

            DependencyManager dm = DependencyManager.getInstance();

            // MultiMap of packages, and the files inside them
            HashMultimap<String, String> packageAndFiles = HashMultimap.create();

            for (String path : ChangeProcessor.getFilesToProcess()) {
                String relativePath = RelativePath.of(path);

                Dependency dependency = dm.find(relativePath);
                if (dependency != null) {
                    String packageName = dependency.getPackageName();

                    if (!"".equals(packageName)) {
                        packageAndFiles.get(packageName).add(relativePath);
                    }
                } else {
                    packageAndFiles.get("").add(relativePath);
                }
            }

            // processing packages separately
            Set<String> packages = packageAndFiles.keys().elementSet();

//            for (String p : packages) {
//                FileDiscoverer packageDiscoverer = new FileDiscoverer(p);
//
//                for (String file : packageAndFiles.get(p)) {
//                    packageDiscoverer.execute(new File(file));
//                }
//            }



            ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);

            for (String p : packages) {

                ParserProcess process = new ParserProcess(p, packageAndFiles.get(p));
                executor.execute(process);

            }

            // This will make the executor accept no new threads
            // and finish all existing threads in the queue
            executor.shutdown();
            // Wait until all threads are finish
            executor.awaitTermination(9999999, TimeUnit.DAYS);


            System.out.println("[ASG] " + (System.currentTimeMillis() - agsStart));
            long importStart = System.currentTimeMillis();


            executor = Executors.newFixedThreadPool(NTHREDS);
            for (String p : packages) {
                for (String file : packageAndFiles.get(p)) {
                    UpdaterProcess process = new UpdaterProcess(file);
                    executor.execute(process);
                }
            }

            // This will make the executor accept no new threads
            // and finish all existing threads in the queue
            executor.shutdown();
            // Wait until all threads are finish
            executor.awaitTermination(9999999, TimeUnit.DAYS);


            for (String f : ChangeProcessor.getDeletedFiles()) {
                removeFromGraph(f);
            }

            System.out.println("[ImportTime] " + (System.currentTimeMillis() - importStart));

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void removeFromGraph(String filePath) {
        File newTurtle = new File(filePath.replace("toprocess", "export") + ".ttl");
        if (newTurtle.exists()) {
            try {
                fourstore.deleteTriples(newTurtle.getAbsolutePath());
                System.out.println("Removed: " + newTurtle.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void processChanges() {
        // --------------------------------------------------------------------------------------------------------- NEW

        if (newFilesListPath != null) {
            try {
                for (String file : ReadStream.asLines(new FileInputStream(newFilesListPath))) {
                    ChangeProcessor.addedNewFile(file);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // -----------------------------------------------------------------------------------------------------MODIFIED

        if (modifiedFilesListPath != null) {
            try {
                for (String file : ReadStream.asLines(new FileInputStream(modifiedFilesListPath))) {
                    ChangeProcessor.modifiedFile(file);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // ------------------------------------------------------------------------------------------------------ DELETE

        if (deletedFilesListPath != null) {
            try {
                for (String file : ReadStream.asLines(new FileInputStream(deletedFilesListPath))) {
                    ChangeProcessor.deletedFile(file);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // ----------------------------------------------------------------------------------------------------- DEFAULT

        if (newFilesListPath == null && modifiedFilesListPath == null && deletedFilesListPath == null) {
            File f = new File("toprocess/");
            FileIterator.iterate(f, new InitializationIterator());
        }
    }

    protected void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);

        formatter.printHelp("java -jar JaMoPPDiscoverer.jar [options]", "options:", options, "", false);
        System.out.println();
    }

    protected class ParserProcess implements Runnable {

        protected String packageName = null;
        protected Set<String> files = null;

        public ParserProcess(String packageName, Set<String> files) {
            this.packageName = packageName;
            this.files = files;
        }

        @Override
        public void run() {
            FileDiscoverer packageDiscoverer = new FileDiscoverer(packageName);

            for (String file : files) {
                packageDiscoverer.execute(new File(file));
            }

        }
    }

    protected class UpdaterProcess implements Runnable {

        protected String filePath = null;

        public UpdaterProcess(String filePath) {
            this.filePath = filePath;
        }

        @Override
        public void run() {
            File oldTurtle = new File(filePath.replace("toprocess", "export") + "-old.ttl");
            if (oldTurtle.exists()) {
                try {
                    fourstore.deleteTriples(oldTurtle.getAbsolutePath());
                    System.out.println("Removed: " + oldTurtle.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            File newTurtle = new File(filePath.replace("toprocess", "export") + ".ttl");
            if (newTurtle.exists()) {
                try {
                    fourstore.load(newTurtle.getAbsolutePath());
                    System.out.println("Added: " + newTurtle.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
