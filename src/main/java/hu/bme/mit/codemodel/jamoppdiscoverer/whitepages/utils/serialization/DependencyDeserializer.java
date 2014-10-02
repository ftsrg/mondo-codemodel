package hu.bme.mit.codemodel.jamoppdiscoverer.whitepages.utils.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.bme.mit.codemodel.jamoppdiscoverer.whitepages.pojo.Dependency;

import java.io.IOException;
import java.util.Set;

public class DependencyDeserializer extends JsonDeserializer<Dependency> {

    @Override
    public Dependency deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
            JsonProcessingException {

        String FQN;
        String relativeLocation;
        Set<String> dependsOn;
        Set<String> usedBy;

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jp);

//        String _id = root.path("_id").textValue();

        FQN = root.path("FQN").textValue();
        relativeLocation = root.path("location").textValue();

        TypeReference<Set<String>> type = new TypeReference<Set<String>>() {
        };

        dependsOn = mapper.readValue(root.findValue("dependsOn").traverse(), type);
        usedBy = mapper.readValue(root.findValue("usedBy").traverse(), type);

        jp.close();

        Dependency dependency = new Dependency();
//        dependency.set_id(_id);
        dependency.setFQN(FQN);
        dependency.setRelativeLocation(relativeLocation);
        dependency.setDependsOn(dependsOn);
        dependency.setUsedBy(usedBy);

        return dependency;

    }
}
