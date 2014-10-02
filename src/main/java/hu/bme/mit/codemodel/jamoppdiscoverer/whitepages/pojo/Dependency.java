package hu.bme.mit.codemodel.jamoppdiscoverer.whitepages.pojo;

import org.jongo.marshall.jackson.oid.ObjectId;

import java.util.HashSet;
import java.util.Set;

public class Dependency {

    @ObjectId
    protected String _id;

    protected String packageName = "";

    protected String FQN = "";

    protected String relativeLocation = "";

    protected Set<String> usedBy = new HashSet<String>();

    protected Set<String> dependsOn = new HashSet<String>();

    // -----------------------------------------------------------------------------------------------------------------

//    public void set_id(String _id) {
//        this._id = _id;
//    }
//
//    public String get_id() {
//        return _id;
//    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getFQN() {
        return FQN;
    }

    public void setFQN(String FQN) {
        this.FQN = FQN;
    }

    public String getRelativeLocation() {
        return relativeLocation;
    }

    public void setRelativeLocation(String relativeLocation) {
        this.relativeLocation = relativeLocation;
    }

    public Set<String> getUsedBy() {
        return usedBy;
    }

    public void setUsedBy(Set<String> usedBy) {
        this.usedBy = usedBy;
    }

    public Set<String> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(Set<String> dependsOn) {
        this.dependsOn = dependsOn;
    }
}