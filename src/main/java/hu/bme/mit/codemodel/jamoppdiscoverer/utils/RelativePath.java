package hu.bme.mit.codemodel.jamoppdiscoverer.utils;

import java.io.File;

public class RelativePath {

    protected static String root = new File("").getAbsolutePath() + "/";

    public static String of(String absolutePath) {
        return absolutePath.replace(root, "");
    }

    public static String rev(String relativePath) {return root + relativePath; }
}
