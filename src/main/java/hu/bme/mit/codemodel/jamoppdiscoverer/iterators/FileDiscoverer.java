package hu.bme.mit.codemodel.jamoppdiscoverer.iterators;

import com.hp.hpl.jena.rdf.model.ResourceFactory;

import hu.bme.mit.codemodel.jamoppdiscoverer.FileIterator;
import hu.bme.mit.codemodel.jamoppdiscoverer.utils.PackageName;
import hu.bme.mit.codemodel.jamoppdiscoverer.whitepages.pojo.Dependency;
import hu.bme.mit.codemodel.jamoppdiscoverer.whitepages.DependencyManager;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.emftext.language.java.JavaClasspath;
import org.emftext.language.java.JavaPackage;
import org.emftext.language.java.resource.java.mopp.JavaResourceFactory;
import org.openrdf.rio.*;

import java.io.*;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileDiscoverer implements FileIterator.Function {

    protected static final Pattern PACKAGENAME_PATTERN = Pattern.compile("^(.*)\\..*?$");
    protected static DependencyManager dependencyManager = null;
    protected String rootPath = new File("").getAbsolutePath() + "/";
    protected String INPUT_DIRECTORY = "toprocess/";

    public FileDiscoverer() {

        try {
            dependencyManager = DependencyManager.getInstance();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        // init
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION,
                new XMIResourceFactoryImpl());

        // init JaMoPP -------------------------------------------------------------------------------------------------
        EPackage.Registry.INSTANCE.put(JavaPackage.eNS_URI, JavaPackage.eINSTANCE);

        Resource.Factory.Registry registry = Resource.Factory.Registry.INSTANCE;
        Map<String, Object> extensionToFactoryMap = registry.getExtensionToFactoryMap();
        Resource.Factory factory = new JavaResourceFactory();
        extensionToFactoryMap.put("java", factory);
        extensionToFactoryMap.put("class", factory);
    }

    @Override
    public void execute(File file) {

        if (!file.getName().endsWith(".java")) {
            return;
        }

        String absoluteFilePath = file.getAbsolutePath();
        String relativeFilePath = absoluteFilePath.replace(rootPath, "");

        // ---------------------------------------------------------------------------------- ResourceSet initialization

        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getLoadOptions().put(JavaClasspath.OPTION_USE_LOCAL_CLASSPATH, Boolean.TRUE);

        JavaClasspath cp = JavaClasspath.get(resourceSet);
        cp.registerStdLib();
        cp.registerSourceOrClassFileFolder(URI.createFileURI(rootPath + INPUT_DIRECTORY));

        Resource target = resourceSet.getResource(URI.createFileURI(new File(rootPath + relativeFilePath).getAbsolutePath()), true);

        // --------------------------------------------------------------------------------------- Dependency resolution

        System.out.println("\n\nProcessing file: " + relativeFilePath +
                "\n====================================================================================================");

        resolveDependencies(resourceSet, cp, relativeFilePath);

        resolveTargetReferences(resourceSet, target);
        resolveTargetReferences(resourceSet, target);

        // ------------------------------------------------------------------------------------------------------ Export

//        exportXMIResource(resourceSet, target);

        exportTTL(target);

        convertTTL(target);

    }

    protected void resolveDependencies(ResourceSet resourceSet, JavaClasspath cp, String relativeFilePath) {
        Set<String> dependencies;
        Dependency d = dependencyManager.find(relativeFilePath);
        if (d != null) {
            dependencies = d.getDependsOn();

            // load every class in the package
            // TODO not only the found dependency has FQN
            Matcher matcher = PACKAGENAME_PATTERN.matcher(d.getFQN());
            if (matcher.matches()) {
                for (Dependency dependency : dependencyManager.findAll(matcher.group(1))) {
                    dependencies.add(dependency.getFQN());
                }
            }
        } else {
            dependencies = new HashSet<>();
        }

        for (String dependency : dependencies) {
            if (dependency.contains("*")) {
                for(Dependency dInPackage : dependencyManager.findAll(PackageName.of(dependency))) {
                    dependencies.add(dInPackage.getFQN());
                }
            }
        }

        System.out.println("\t\t[INFO]\tDependencies: " + Arrays.toString(dependencies.toArray()));

        for (String dependencyFQN : dependencies) {
            String dependencyLocation = dependencyManager.findOrCreate(dependencyFQN).getRelativeLocation();
            File dependency = new File(rootPath + dependencyLocation);

            if (dependency.exists() && dependency.isFile()) {

                if (dependency.getName().endsWith(".jar")) {
                    System.out.println("\t\t[INFO]\tAdding JAR: " + dependencyLocation);
                    cp.registerClassifierJar(URI.createFileURI(dependency.getAbsolutePath()));
                } else if (dependency.getName().endsWith(".java")) {
                    System.out.println("\t\t[INFO]\tLoading: " + dependencyLocation);
                    resourceSet.getResource(URI.createFileURI(dependency.getAbsolutePath()), true);
//                    for (Resource r : resourceSet.getResources()) {
//                        System.out.println(r.getURI());
//                    }
//                    System.out.println("---------------------------");
                } else {
                    System.out.println("\t[ERROR]\t\tNot .jar and not .java: " + dependencyLocation);
                }
            } else {
                System.out.println("\t\t[INFO]\t■ Did not find: " + dependencyFQN + " at " + dependencyLocation);
            }
        }
    }

    protected void resolveTargetReferences(ResourceSet resourceSet, Resource target) {
        System.out.println("\n" +
                "\t\t[INFO]\tResolving references. Did not find the following: ");

        long references = 0;
        long notResolved = 0;

        TreeIterator<EObject> contents = target.getAllContents();
        while (contents.hasNext()) {
            InternalEObject o = (InternalEObject) contents.next();

            for (EObject ref : o.eCrossReferences()) {
                references++;

                ref = EcoreUtil.resolve(ref, resourceSet);
                if (ref.eIsProxy()) {
                    notResolved++;
                    System.out.println("\t\t\t\t" + ((InternalEObject) ref).eProxyURI().toString().replace("file:" + rootPath, "\t"));
                }
            }
        }

        System.out.println("\t\t[INFO]\tNumber of references: " + references + ", not resolved: " + notResolved);
    }

    protected String exportTTL(Resource target) {
        String outputPath = target.getURI().toString().replace("toprocess", "export").replace("file:", "");

        try {
            File outputFile = new File(outputPath + ".nt");
            outputFile.getParentFile().mkdirs();
            outputFile.createNewFile();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        try (PrintWriter w = new PrintWriter(outputPath + ".nt", "UTF-8")) {

            w.println("@prefix java: <https://www.java.com/> .\n" +
                    "@prefix xannotations: <http://www.emftext.org/java/annotations#/> .\n" +
                    "@prefix xarrays: <http://www.emftext.org/java/arrays#/> .\n" +
                    "@prefix xclassifiers: <http://www.emftext.org/java/classifiers#/> .\n" +
                    "@prefix xcommons: <http://www.emftext.org/java/commons#/> .\n" +
                    "@prefix xcontainers: <http://www.emftext.org/java/containers#/> .\n" +
                    "@prefix xexpressions: <http://www.emftext.org/java/expressions#/> .\n" +
                    "@prefix xgenerics: <http://www.emftext.org/java/generics#/> .\n" +
                    "@prefix ximports: <http://www.emftext.org/java/imports#/> .\n" +
                    "@prefix xinstantiations: <http://www.emftext.org/java/instantiations#/> .\n" +
                    "@prefix xliterals: <http://www.emftext.org/java/literals#/> .\n" +
                    "@prefix xmembers: <http://www.emftext.org/java/members#/> .\n" +
                    "@prefix xmodifiers: <http://www.emftext.org/java/modifiers#/> .\n" +
                    "@prefix xoperators: <http://www.emftext.org/java/operators#/> .\n" +
                    "@prefix xparameters: <http://www.emftext.org/java/parameters#/> .\n" +
                    "@prefix xreferences: <http://www.emftext.org/java/references#/> .\n" +
                    "@prefix xstatements: <http://www.emftext.org/java/statements#/> .\n" +
                    "@prefix xtypes: <http://www.emftext.org/java/types#/> .\n" +
                    "@prefix xvariables: <http://www.emftext.org/java/variables#/> .\n");

            TreeIterator<EObject> allContents = target.getAllContents();

            while (allContents.hasNext()) {
//                System.out.println("\n\n");

                EObject _object = allContents.next();
                InternalEObject object = (InternalEObject) _object;

                String objectURI = EcoreUtil.getURI(object).toString();
//                System.out.println(objectURI);

                // ------------------------------------------------------------------------------------------- CLASS

                EClass eC = object.eClass();
                String eCURI = EcoreUtil.getURI(eC).toString();
//                System.out.println("[ " + eC.getName() + " ]\t" + eCURI);

                w.println("<" + objectURI + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + eCURI + "> .");

                // -------------------------------------------------------------------------------------- SUPERCLASS

                for (EClass _eclass : object.eClass().getEAllSuperTypes()) {
                    String eclassURI = EcoreUtil.getURI(_eclass).toString();
//                    System.out.println("[ " + _eclass.getName() + " ]\t" + eclassURI);

                    w.println("<" + objectURI + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + eclassURI + "> .");
                }

                // --------------------------------------------------------------------------------------- ATTRIBUTE

                // attributeFor:
                for (EAttribute _attribute : object.eClass().getEAllAttributes()) {
                    String attributeURI = EcoreUtil.getURI(_attribute).toString();
                    Object attribute = object.eGet(_attribute);
                    if (attribute == null) {
                        continue; // attributeFor;
                    }
//                    String attributeValue = attribute.toString();
//                    String attributeType = EcoreUtil.getURI(_attribute.getEAttributeType()).toString();
//                    System.out.println("\t" + _attribute.getName() + ":\t" + attributeValue + "\t" + attributeType);

                    String attributeString = ResourceFactory.createTypedLiteral(attribute).toString();
                    w.println("<" + objectURI + "> <" + attributeURI + "> '''" + attributeString.replace("^^", "'''^^<") + "> .");
                }

                // --------------------------------------------------------------------------------------- REFERENCE

//                System.out.println();

                for (EReference _reference : object.eClass().getEAllReferences()) {

                    String referenceType = EcoreUtil.getURI(_reference).toString();
//                    System.out.println("\t" + _reference.getName() + "\t" + referenceType);

                    if (_reference.isMany()) {
                        EList<EObject> references = (EList<EObject>) object.eGet(_reference);

                        for (EObject ref : references) {
                            String referenceURI = EcoreUtil.getURI(ref).toString();
//                            System.out.println("\t\t->\t" + referenceURI);

                            w.println("<" + objectURI + "> <" + referenceType + "> <" + referenceURI + "> .");
                        }
                    } else {
                        EObject ref = (EObject) object.eGet(_reference);
                        if (ref != null) {
                            String referenceURI = EcoreUtil.getURI(ref).toString();
//                            System.out.println("\t\t->\t" + EcoreUtil.getURI(ref));

                            w.println("<" + objectURI + "> <" + referenceType + "> <" + referenceURI + "> .");
                        }
                    }

                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputPath;
    }

    protected void exportXMIResource(ResourceSet resourceSet, Resource target) {
        String outputPath = target.getURI().toString().replace("toprocess", "export").replace("file:", "");
        Map<Object, Object> options = new HashMap<>();
        options.put(XMLResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);

        try {
            File outputFile = new File(outputPath + ".nt");
            outputFile.getParentFile().mkdirs();
            outputFile.createNewFile();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        Resource xmi = resourceSet.createResource(URI.createFileURI(outputPath + ".xmi"));
        for (EObject o : target.getContents()) {
            xmi.getContents().add(EcoreUtil.copy(o));
        }
        try {
            xmi.save(options);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void convertTTL(Resource target) {
        String outputPath = target.getURI().toString().replace("toprocess", "export").replace("file:", "");

        try {
            File outputFile = new File(outputPath + ".nt");
            outputFile.getParentFile().mkdirs();
            outputFile.createNewFile();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        try {
            File input = new File(outputPath + ".nt");
            File output = new File(outputPath + ".ttl");

            RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
            RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE,
                    new FileOutputStream(output));
            rdfParser.setRDFHandler(rdfWriter);
            rdfParser.parse(new FileInputStream(input), "");
        } catch (IOException | RDFHandlerException | RDFParseException e) {
            e.printStackTrace();
        }
    }
}