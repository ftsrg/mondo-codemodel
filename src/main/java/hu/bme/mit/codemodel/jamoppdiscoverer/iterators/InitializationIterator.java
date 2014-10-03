package hu.bme.mit.codemodel.jamoppdiscoverer.iterators;

import hu.bme.mit.codemodel.jamoppdiscoverer.ChangeProcessor;
import hu.bme.mit.codemodel.jamoppdiscoverer.FileIterator;

import java.io.File;

public class InitializationIterator implements FileIterator.Function {
    @Override
    public void execute(File file) {
        ChangeProcessor.addedNewFile(file.getAbsolutePath());
    }
}
