package hu.bme.mit.codemodel.jamoppdiscoverer.iterators;

import hu.bme.mit.codemodel.jamoppdiscoverer.FileIterator;
import hu.bme.mit.codemodel.jamoppdiscoverer.whitepages.pojo.Dependency;
import hu.bme.mit.codemodel.jamoppdiscoverer.whitepages.DependencyManager;

import java.io.*;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DependencyCollector implements FileIterator.Function {

    protected static Pattern IMPORT_PATTERN;
    protected static Pattern PACKAGE_PATTERN;
    protected static Pattern CLASSIFIER_PATTERN;

    protected static DependencyManager dependencyManager = null;

    public DependencyCollector() {
        PACKAGE_PATTERN = Pattern.compile("^package (.*?);");
        IMPORT_PATTERN = Pattern.compile("^\\s?(static)?\\s?import\\s(.*?);");
        CLASSIFIER_PATTERN = Pattern.compile("^([^\\*\\/]*?)(interface|class|enum)\\s(\\S*?)(\\s.*?)?$");

        try {
            dependencyManager = DependencyManager.getInstance();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void execute(File file) {
        String filePath = file.getAbsolutePath().toString().replace(new File("").getAbsolutePath().toString() + "/", "");

        String fileName = file.getName();
        if (!(fileName.endsWith(".java") || fileName.endsWith(".class") || fileName.endsWith(".jar"))) {
            return;
        }

        if (fileName.endsWith(".java")) {
            try {
                processInputStream(filePath, new FileInputStream(file));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else if (fileName.endsWith(".jar")) {
            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(file.getAbsolutePath());
                final Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    final ZipEntry entry = entries.nextElement();
                    System.out.println(entry.getName());
//                    processInputStream(filePath, zipFile.getInputStream(entry));
//                    ClassLoader cl = new URLClassLoader()

                    // http://stackoverflow.com/questions/15720822/how-to-get-names-of-classes-inside-a-jar-file
                    StringBuilder className = new StringBuilder();
                    for (String part : entry.getName().split("/")) {
                        if (className.length() != 0)
                            className.append(".");
                        className.append(part);
                        if (part.endsWith(".class"))
                            className.setLength(className.length() - ".class".length());
                    }

                    saveDependencies(filePath, className.toString(), new ArrayList<String>());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    protected void processInputStream(String filePath, InputStream inputStream) {
        String packageName = null;
        String classifierName = null;
        List<String> imports = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {

            String line = null;

            forEachLine:
            while ((line = br.readLine()) != null) {

                if (packageName == null) {
                    Matcher packageMatcher = PACKAGE_PATTERN.matcher(line);

                    if (packageMatcher.matches()) {

                        packageName = packageMatcher.group(1);

                    }

                }

                if (classifierName == null) {
                    Matcher importMatcher = IMPORT_PATTERN.matcher(line);
                    Matcher classifierMatcher = CLASSIFIER_PATTERN.matcher(line);

                    if (importMatcher.matches()) {

                        imports.add(importMatcher.group(2));

                    } else if (classifierMatcher.matches()) {

                        classifierName = classifierMatcher.group(3);

                    }

                } else {

                    break forEachLine;

                }
            }

//            imports.add(packageName + ".*");
            saveDependencies(filePath, packageName + "." + classifierName, imports);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void saveDependencies(String filePath, String FQN, List<String> imports) {
        System.out.println(filePath + " " + FQN + " " + Arrays.toString(imports.toArray()));

        Dependency d = dependencyManager.findOrCreate(filePath, FQN);
        dependencyManager.addDependencies(d, imports);
    }
}
