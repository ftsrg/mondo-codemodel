package hu.bme.mit.codemodel.jamoppdiscoverer;

import hu.bme.mit.codemodel.jamoppdiscoverer.iterators.FileDiscoverer;
import hu.bme.mit.codemodel.jamoppdiscoverer.iterators.InitializationIterator;
import hu.bme.mit.codemodel.jamoppdiscoverer.utils.ReadStream;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;

public class CommandParser {

    protected final Options options = new Options();
    protected final CommandLineParser parser = new PosixParser();
    protected CommandLine cmd;

    protected String newFilesListPath = null;
    protected String modifiedFilesListPath = null;
    protected String deletedFilesListPath = null;
//    protected String gitOutputPath = null;

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

        FileDiscoverer fileDiscoverer = new FileDiscoverer();

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

        if(newFilesListPath == null || modifiedFilesListPath == null || deletedFilesListPath == null) {
            File f = new File("toprocess/");
            FileIterator.iterate(f, new InitializationIterator());
        }

        // ----------------------------------------------------------------------------------------------------- PROCESS

        for (String file : ChangeProcessor.getFilesToProcess()) {
            fileDiscoverer.execute(new File(file));
        }
    }

    protected void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);

        formatter.printHelp("java -jar JaMoPPDiscoverer.jar [options]", "options:", options, "", false);
        System.out.println();
    }
}
