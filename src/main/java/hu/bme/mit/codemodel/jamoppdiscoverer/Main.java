package hu.bme.mit.codemodel.jamoppdiscoverer;

import hu.bme.mit.codemodel.jamoppdiscoverer.utils.DependencyCollector;
import hu.bme.mit.codemodel.jamoppdiscoverer.utils.FileDiscoverer;
import hu.bme.mit.codemodel.jamoppdiscoverer.utils.FileIterator;

import java.io.File;

public class Main {

    public static void main(String[] args) {

        File f = new File("toprocess/");
//        File dm = new File("toprocess/src/view/graphic/Main.java");
        File dm = new File("toprocess/");

        FileIterator.iterate(f, new DependencyCollector());
        FileIterator.iterate(dm, new FileDiscoverer());
    }
}
