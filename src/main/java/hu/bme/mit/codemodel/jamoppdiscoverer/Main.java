package hu.bme.mit.codemodel.jamoppdiscoverer;

import hu.bme.mit.codemodel.jamoppdiscoverer.iterators.FileDiscoverer;
import hu.bme.mit.codemodel.jamoppdiscoverer.iterators.InitializationIterator;

import java.io.File;

public class Main {

    public static void main(String[] args) {

        File f = new File("toprocess/");
        FileIterator.iterate(f, new InitializationIterator());
//        File dm = new File("toprocess/src/view/graphic/Main.java");
//        File dm = new File("toprocess/");
//
//        FileIterator.iterate(f, new DependencyCollector());
//        FileIterator.iterate(dm, new FileDiscoverer());

//        ChangeProcessor.addedNewFile(new File("toprocess/src/view/graphic/Main.java").getAbsolutePath());
//        ChangeProcessor.modifiedFile(new File("toprocess/src/view/graphic/Main.java").getAbsolutePath());
//        ChangeProcessor.deletedFile(new File("toprocess/src/view/graphic/Main.java").getAbsolutePath());

        System.out.println("\n\n");

        FileDiscoverer fileDiscoverer = new FileDiscoverer();

        for (String file : ChangeProcessor.getFilesToProcess()) {
            fileDiscoverer.execute(new File(file));
        }

    }
}
