package hu.bme.mit.codemodel.jamoppdiscoverer.utils;

import java.io.File;

public class InitializationIterator implements FileIterator.Function {
    @Override
    public void execute(File file) {
        ChangeProcessor.addedNewFile(file.getAbsolutePath());
    }
}
