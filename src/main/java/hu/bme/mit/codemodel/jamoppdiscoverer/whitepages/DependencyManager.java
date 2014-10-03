package hu.bme.mit.codemodel.jamoppdiscoverer.whitepages;

import hu.bme.mit.codemodel.jamoppdiscoverer.utils.PackageName;
import hu.bme.mit.codemodel.jamoppdiscoverer.whitepages.pojo.Dependency;
import org.jongo.MongoCollection;
import org.jongo.MongoCursor;

import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DependencyManager {

    protected static DependencyManager _instance = null;
    protected MongoCollection dependencies = null;

    protected DependencyManager() throws UnknownHostException {
        dependencies = ConnectionManager.getInstance().getDependencies();
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static DependencyManager getInstance() throws UnknownHostException {
        if (_instance == null) {
            _instance = new DependencyManager();
        }

        return _instance;
    }

    // -----------------------------------------------------------------------------------------------------------------

    public Dependency find(String relativeLocation) {
        return dependencies.findOne("{ \"relativeLocation\": # }", relativeLocation).as(Dependency.class);
    }

    public Dependency findOrCreate(String FQN) {
        Dependency d = null;
        d = dependencies.findOne("{ \"FQN\": # }", FQN).as(Dependency.class);

        if (d == null) {
            d = new Dependency();
            d.setFQN(FQN);
            d.setPackageName(PackageName.of(FQN));
            dependencies.save(d);
        }

        return d;
    }

    public Dependency findOrCreate(String relativeLocation, String FQN) {
        Dependency d = findOrCreate(FQN);
        d.setRelativeLocation(relativeLocation);
        dependencies.save(d);

        return d;
    }

    public Set<Dependency> findAll(String FQN) {
        Set<Dependency> result = new HashSet<>();

        MongoCursor<Dependency> d = dependencies.find("{ \"packageName\": # }", FQN).as(Dependency.class);
        while (d.hasNext()) {
            result.add(d.next());
        }

        return result;
    }

    // -----------------------------------------------------------------------------------------------------------------

    public void addDependency(Dependency d, String dependsOnFQN) {
        d.getDependsOn().add(dependsOnFQN);
        addUsage(dependsOnFQN, d.getFQN());
        dependencies.save(d);
    }

    public void addDependencies(Dependency d, List<String> dependsOnFQNs) {
        d.getDependsOn().addAll(dependsOnFQNs);
        for (String dependsOnFQN : dependsOnFQNs) {
            addUsage(dependsOnFQN, d.getFQN());
        }
        dependencies.save(d);
    }

    public void removeDependency(Dependency d, String doesNotDependOnFQN) {
        d.getDependsOn().remove(doesNotDependOnFQN);
        removeUsage(doesNotDependOnFQN, d.getFQN());
        dependencies.save(d);
    }

    public void removeDescriptor(Dependency d) {
        for (String depOn : new HashSet<>(d.getDependsOn())) {
            removeDependency(d, depOn);
        }
        dependencies.remove("{ \"FQN\": # }", d.getFQN());
    }

    // -----------------------------------------------------------------------------------------------------------------

    protected void addUsage(String FQN, String usedByFQN) {
        Dependency d = findOrCreate(FQN);
        d.getUsedBy().add(usedByFQN);
        dependencies.save(d);
    }

    protected void removeUsage(String FQN, String notUsedByFQN) {
        Dependency d = findOrCreate(FQN);
        d.getUsedBy().remove(notUsedByFQN);
        dependencies.save(d);
    }

}
