package hu.bme.mit.codemodel.jamoppdiscoverer.whitepages.utils.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import hu.bme.mit.codemodel.jamoppdiscoverer.whitepages.pojo.Dependency;

import java.io.IOException;

public class DependencySerializer extends JsonSerializer<Dependency> {

    @Override
    public void serializeWithType(Dependency dependency, JsonGenerator generator, SerializerProvider provider, TypeSerializer serializer) throws IOException, JsonProcessingException {
        serializer.writeTypePrefixForScalar(dependency, generator);
        serialize(dependency, generator, provider);
        serializer.writeTypeSuffixForScalar(dependency, generator);
    }
    
    
    @Override
    public void serialize(Dependency dependency, JsonGenerator generator, SerializerProvider provider) throws IOException,
            JsonProcessingException {

        generator.writeStartObject();
//        generator.writeObjectId(dependency.get_id());
        generator.writeStringField("FQN", dependency.getFQN());
        generator.writeStringField("location", dependency.getRelativeLocation());
        generator.writeObjectField("dependsOn", dependency.getDependsOn());
        generator.writeObjectField("usedBy", dependency.getUsedBy());
        generator.writeEndObject();
        
        generator.close();
        
    }

}
