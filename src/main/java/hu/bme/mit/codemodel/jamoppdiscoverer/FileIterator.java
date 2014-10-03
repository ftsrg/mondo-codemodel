package hu.bme.mit.codemodel.jamoppdiscoverer;

import java.io.File;

public class FileIterator {

    public static void iterate(File f, Function function) {

        if (f.exists()) {
            if (f.isFile()) {

                function.execute(f);

            } else {

                File[] files = f.listFiles();
                for (int i = 0; i < files.length; i++) {
                    iterate(files[i], function);
                }

            }
        }
    }

    public interface Function {
        public void execute(File file);
    }

}
