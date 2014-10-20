package hu.bme.mit.codemodel.jamoppdiscoverer.iterators;

import com.hp.hpl.jena.rdf.model.ResourceFactory;
import hu.bme.mit.codemodel.jamoppdiscoverer.FileIterator;
import hu.bme.mit.codemodel.jamoppdiscoverer.benchmark.DiscoveryBenchmarkResults;
import hu.bme.mit.codemodel.jamoppdiscoverer.utils.PackageName;
import hu.bme.mit.codemodel.jamoppdiscoverer.utils.RelativePath;
import hu.bme.mit.codemodel.jamoppdiscoverer.whitepages.DependencyManager;
import hu.bme.mit.codemodel.jamoppdiscoverer.whitepages.pojo.Dependency;
import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.emf.common.notify.Notifier;
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
import org.emftext.language.java.resource.java.IJavaOptions;
import org.emftext.language.java.resource.java.mopp.JavaResourceFactory;
import org.openrdf.rio.*;

import java.io.*;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileDiscoverer implements FileIterator.Function {

    protected static final int DEBUG_LEVEL = -9001;
    
    protected static final boolean EXPORT_CLASS = true;
    protected static final boolean EXPORT_SUPERCLASSES = true;
    protected static final boolean EXPORT_ATTRIBUTES = false;
    protected static final boolean EXPORT_REFERENCES = true;


    protected static DependencyManager dependencyManager = null;
    protected final JavaClasspath cp;
    protected String rootPath = new File("").getAbsolutePath() + "/";
    protected String INPUT_DIRECTORY = "toprocess/";
    protected DiscoveryBenchmarkResults dbr;
    protected String packageName;
    protected ResourceSet resourceSet = new ResourceSetImpl();
    // these Resources are required to be always available
    protected Set<Resource> basePackageResources = new HashSet<>();
    protected Long packageSize = 0L;
    protected Long packageCount = 0L;
    protected static Pattern literalPattern = Pattern.compile("^(.*?)(\\^\\^.*)$");

//    protected static ResourceSet cprs = new ResourceSetImpl();
//    protected static JavaClasspath scp = JavaClasspath.get(cprs);

    public FileDiscoverer(String packageName) {

        this.packageName = packageName;

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


        // init ResourceSet
        resourceSet.getLoadOptions().put(JavaClasspath.OPTION_USE_LOCAL_CLASSPATH, Boolean.TRUE);
//        resourceSet.getLoadOptions().put(JavaClasspath.OPTION_REGISTER_STD_LIB, Boolean.TRUE);

        // TODO make this configurable
        resourceSet.getLoadOptions().put(IJavaOptions.DISABLE_LAYOUT_INFORMATION_RECORDING, Boolean.TRUE);

        cp = JavaClasspath.get(resourceSet);
//        scp.registerStdLib();
//
//        Map<String, List<String>> packageClassifierMap = scp.getPackageClassifierMap();
//        for(String p : packageClassifierMap.keySet()) {
//            cp.getPackageClassifierMap().put(p, new ArrayList<String>(packageClassifierMap.get(p)));
//        }
//
//        resourceSet.getURIConverter().getURIMap().putAll(cprs.getURIConverter().getURIMap());

//        javaClasspath.registerStdLib();
//        resourceSet.getjavaClasspath.getDefaultImports();
//        cp.registerStdLib();
//        cp.registerSourceOrClassFileFolder(URI.createFileURI(rootPath + INPUT_DIRECTORY));


        loadPackageResources();
    }

    protected boolean resolveAllProxies(ResourceSet rs, int resourcesProcessedBefore) {
        boolean failure = false;
        List<EObject> eobjects = new LinkedList<EObject>();
        for (Iterator<Notifier> i = rs.getAllContents(); i.hasNext(); ) {
            Notifier next = i.next();
            if (next instanceof EObject) {
                eobjects.add((EObject) next);
            }
        }
        int resourcesProcessed = rs.getResources().size();
        if (resourcesProcessed == resourcesProcessedBefore) {
            return true;
        }

        if (DEBUG_LEVEL > 0) System.out.println("Resolving cross-references of " + eobjects.size()
                + " EObjects.");
        int resolved = 0;
        int notResolved = 0;
        int eobjectCnt = 0;
        for (EObject next : eobjects) {
            eobjectCnt++;
            if (eobjectCnt % 1000 == 0) {
                if (DEBUG_LEVEL > 0) System.out.println(eobjectCnt + "/" + eobjects.size()
                        + " done: Resolved " + resolved + " crossrefs, "
                        + notResolved + " crossrefs could not be resolved.");
            }

            InternalEObject nextElement = (InternalEObject) next;
            for (EObject crElement : nextElement.eCrossReferences()) {
                crElement = EcoreUtil.resolve(crElement, rs);
                if (crElement.eIsProxy()) {
                    failure = true;
                    notResolved++;
                    if (DEBUG_LEVEL > 0) System.out
                            .println("Can not find referenced element in classpath: "
                                    + ((InternalEObject) crElement).eProxyURI());
                } else {
                    resolved++;
                }
            }
        }

        if (DEBUG_LEVEL > 0) System.out.println(eobjectCnt + "/" + eobjects.size()
                + " done: Resolved " + resolved + " crossrefs, " + notResolved
                + " crossrefs could not be resolved.");

        //call this method again, because the resolving might have triggered loading
        //of additional resources that may also contain references that need to be resolved.
        return !failure && resolveAllProxies(rs, resourcesProcessed);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Load all the Compilation Units found in the given package into the packageResultSet.
     */
    private void loadPackageResources() {
        if (DEBUG_LEVEL > 0) System.out.println("\t\t[INFO]\tLoading package: " + packageName);

        // TODO not only the found dependency has FQN
        for (Dependency dependency : dependencyManager.findAll(packageName)) {
            Resource res = loadResourceIntoResourceSet(resourceSet, cp, dependency.getFQN(), dependency.getRelativeLocation());

            if (res != null) {
                basePackageResources.add(res);

                TreeIterator<EObject> allContents = res.getAllContents();
                while (allContents.hasNext()) {
                    allContents.next();
                }
            }

            packageSize += new File(dependency.getRelativeLocation()).length();
            packageCount++;
        }

        resolveAllProxies(resourceSet, 0);
    }

    @Override
    public void execute(File file) {

        if (!file.getName().endsWith(".java")) {
            return;
        }

        dbr = new DiscoveryBenchmarkResults(file);


        for (int i = 0; i < packageCount - 1; i++) {
            dbr.addDependencySize(0L);
        }
        dbr.addDependencySize(packageSize);


        String absoluteFilePath = file.getAbsolutePath();
        String relativeFilePath = absoluteFilePath.replace(rootPath, "");

        // ---------------------------------------------------------------------------------- ResourceSet initialization


        dbr.start(DiscoveryBenchmarkResults.Type.FILE_DISCOVERY);
        Resource target = resourceSet.getResource(URI.createFileURI(absoluteFilePath), true);
        TreeIterator<EObject> allContents = target.getAllContents();
        while (allContents.hasNext()) {
            allContents.next();
        }
        dbr.tick();

        // --------------------------------------------------------------------------------------- Dependency resolution

        if (DEBUG_LEVEL > 0) System.out.println("\n\nProcessing file: " + relativeFilePath +
                "\n====================================================================================================");

        dbr.start(DiscoveryBenchmarkResults.Type.LOAD_DEPENDENCY);
        resolveDependencies(resourceSet, cp, relativeFilePath);
        dbr.tick();

        dbr.start(DiscoveryBenchmarkResults.Type.DEPENDENCY_RESOLUTION);
//        resolveTargetReferences(resourceSet, target);
        resolveAllProxies(resourceSet, 0);

//        if (!resolveAllProxies(resourceSet, 0)) {
//            System.err.println("Resolution of some Proxies failed...");
////            Iterator<Notifier> it = resourceSet.getAllContents();
////            while (it.hasNext()) {
////                Notifier next = it.next();
////                if (next instanceof EObject) {
////                    EObject o = (EObject) next;
////                    if (o.eIsProxy()) {
////                        try {
////                            it.remove();
////                        } catch (UnsupportedOperationException e) {
////                            e.printStackTrace();
////                        }
////                    }
////                }
////            }
//        }


        dbr.tick();

        // ------------------------------------------------------------------------------------------------------ Export

//        dbr.start(DiscoveryBenchmarkResults.Type.XMI_EXPORT);
//        exportXMIResource(resourceSet, target);
//        dbr.tick();

        dbr.start(DiscoveryBenchmarkResults.Type.NT_EXPORT);
        exportTTL(target);
        dbr.tick();

        dbr.start(DiscoveryBenchmarkResults.Type.TTL_CONVERT);
        convertTTL(target);
        dbr.tick();

        if (DEBUG_LEVEL > -1) System.out.println("\t\t[STAT]\t" + dbr.toString());
    }

    protected void resolveDependencies(ResourceSet resourceSet, JavaClasspath cp, String relativeFilePath) {

        unloadUnneededResources();

        Set<String> dependencies;
        Dependency d = dependencyManager.find(relativeFilePath);
        if (d != null) {
            dependencies = d.getDependsOn();
        } else {
            dependencies = new HashSet<>();
        }


        Set<String> asteriskDependencies = new HashSet<>();
        for (String dependency : dependencies) {
            if (dependency.contains("*")) {
                for (Dependency dInPackage : dependencyManager.findAll(PackageName.of(dependency))) {
                    asteriskDependencies.add(dInPackage.getFQN());
                }
            }
        }
        dependencies.addAll(asteriskDependencies);


        if (DEBUG_LEVEL > 0) System.out.println("\t\t[INFO]\tDependencies: " + Arrays.toString(dependencies.toArray()));

        for (String dependencyFQN : dependencies) {
            String dependencyLocation = dependencyManager.findOrCreate(dependencyFQN).getRelativeLocation();
            loadResourceIntoResourceSet(resourceSet, cp, dependencyFQN, dependencyLocation);

            dbr.addDependencySize(new File(dependencyLocation).length());
        }
    }

    private void unloadUnneededResources() {

        Set<Resource> resourcesToRemove = new HashSet<>();

        for (Resource resource : resourceSet.getResources()) {
            resourcesToRemove.add(resource);
        }

        resourcesToRemove.removeAll(basePackageResources);

        ResourceSet trash = new ResourceSetImpl();
        for (Resource resource : resourcesToRemove) {
            trash.getResources().add(resource);
        }
    }

    private Resource loadResourceIntoResourceSet(ResourceSet resourceSet, JavaClasspath cp, String dependencyFQN, String dependencyLocation) {

        Resource res = null;

        File dependency = new File(RelativePath.rev(dependencyLocation));

        if (dependency.exists() && dependency.isFile()) {

            if (dependency.getName().endsWith(".jar")) {
                if (DEBUG_LEVEL > 0) System.out.println("\t\t[INFO]\tAdding JAR: " + dependencyLocation);
                cp.registerClassifierJar(URI.createFileURI(dependency.getAbsolutePath()));
            } else if (dependency.getName().endsWith(".java")) {
                if (DEBUG_LEVEL > 0) System.out.println("\t\t[INFO]\tLoading: " + dependencyLocation);
                res = resourceSet.getResource(URI.createFileURI(dependency.getAbsolutePath()), false);
//                    for (Resource r : resourceSet.getResources()) {
//                        if (DEBUG_LEVEL > 0) System.out.println(r.getURI());
//                    }
//                    if (DEBUG_LEVEL > 0) System.out.println("---------------------------");
            } else {
                if (DEBUG_LEVEL > 0) System.out.println("\t[ERROR]\t\tNot .jar and not .java: " + dependencyLocation);
            }
        } else {
            if (DEBUG_LEVEL > 0)
                System.out.println("\t\t[INFO]\t■ Did not find: " + dependencyFQN + " at " + dependencyLocation);
        }

        return res;
    }

    protected void resolveTargetReferences(ResourceSet resourceSet, Resource target) {
        if (DEBUG_LEVEL > 0) System.out.println("\n" +
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
                    if (DEBUG_LEVEL > 0)
                        System.out.println("\t\t\t\t" + ((InternalEObject) ref).eProxyURI().toString().replace("file:" + rootPath, "\t"));
                }
            }
        }

        if (DEBUG_LEVEL > 0)
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
//                if (DEBUG_LEVEL > 0) System.out.println("\n\n");

                EObject _object = allContents.next();
                InternalEObject object = (InternalEObject) _object;

                String objectURI = EcoreUtil.getURI(object).toString();
//                if (DEBUG_LEVEL > 0) System.out.println(objectURI);

                // ------------------------------------------------------------------------------------------- CLASS

                if (EXPORT_CLASS) {
                    EClass eC = object.eClass();
                    String eCURI = EcoreUtil.getURI(eC).toString();
//                if (DEBUG_LEVEL > 0) System.out.println("[ " + eC.getName() + " ]\t" + eCURI);

                    w.println("<" + objectURI + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + eCURI + "> .");
                }

                // -------------------------------------------------------------------------------------- SUPERCLASS

                if (EXPORT_SUPERCLASSES) {
                    for (EClass _eclass : object.eClass().getEAllSuperTypes()) {
                        String eclassURI = EcoreUtil.getURI(_eclass).toString();
//                    if (DEBUG_LEVEL > 0) System.out.println("[ " + _eclass.getName() + " ]\t" + eclassURI);

                        w.println("<" + objectURI + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + eclassURI + "> .");
                    }
                }

                // --------------------------------------------------------------------------------------- ATTRIBUTE

                if (EXPORT_ATTRIBUTES) {
                    // attributeFor:
                    for (EAttribute _attribute : object.eClass().getEAllAttributes()) {
                        String attributeURI = EcoreUtil.getURI(_attribute).toString();
                        Object attribute = object.eGet(_attribute);
                        if (attribute == null) {
                            continue; // attributeFor;
                        }
//                    String attributeValue = attribute.toString();
//                    String attributeType = EcoreUtil.getURI(_attribute.getEAttributeType()).toString();
//                    if (DEBUG_LEVEL > 0) System.out.println("\t" + _attribute.getName() + ":\t" + attributeValue + "\t" + attributeType);

                        String attributeString = ResourceFactory.createTypedLiteral(attribute).toString();
                        String toSaveString = "";

                        Matcher matcher = literalPattern.matcher(attributeString);

                        if (matcher.matches()) {
                            toSaveString = "'''" + StringEscapeUtils.escapeJava(matcher.group(1)) + "'''" + matcher.group(2).replace("^^", "^^<") + ">";
                        } else {
                            toSaveString = "'''" + attributeString.replace("^^", "'''^^<") + ">";
                        }

                        w.println("<" + objectURI + "> <" + attributeURI + "> " + toSaveString + " .");
                    }
                }

                // --------------------------------------------------------------------------------------- REFERENCE

                if (EXPORT_REFERENCES) {
//                if (DEBUG_LEVEL > 0) System.out.println();

                    for (EReference _reference : object.eClass().getEAllReferences()) {

                        String referenceType = EcoreUtil.getURI(_reference).toString();
//                    if (DEBUG_LEVEL > 0) System.out.println("\t" + _reference.getName() + "\t" + referenceType);

                        if (_reference.isMany()) {
                            EList<EObject> references = (EList<EObject>) object.eGet(_reference);

                            for (EObject ref : references) {
                                String referenceURI = EcoreUtil.getURI(ref).toString();
//                            if (DEBUG_LEVEL > 0) System.out.println("\t\t->\t" + referenceURI);

                                w.println("<" + objectURI + "> <" + referenceType + "> <" + referenceURI + "> .");
                            }
                        } else {
                            EObject ref = (EObject) object.eGet(_reference);
                            if (ref != null) {
                                String referenceURI = EcoreUtil.getURI(ref).toString();
//                            if (DEBUG_LEVEL > 0) System.out.println("\t\t->\t" + EcoreUtil.getURI(ref));

                                w.println("<" + objectURI + "> <" + referenceType + "> <" + referenceURI + "> .");
                            }
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

            input.delete();
        } catch (IOException | RDFHandlerException | RDFParseException e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    public DiscoveryBenchmarkResults getBenchmarkResults() {
        return dbr;
    }
}