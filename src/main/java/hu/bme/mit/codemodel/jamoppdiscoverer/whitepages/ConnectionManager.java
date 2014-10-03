package hu.bme.mit.codemodel.jamoppdiscoverer.whitepages;

import com.fasterxml.jackson.databind.MapperFeature;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import hu.bme.mit.codemodel.jamoppdiscoverer.whitepages.utils.serialization.SerializationModule;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.marshall.jackson.JacksonMapper;

import java.net.UnknownHostException;

public class ConnectionManager {

    protected static DB db = null;
    protected static Jongo jongo = null;
    protected static MongoCollection dependencies = null;

    protected static ConnectionManager _instance = null;

    // -----------------------------------------------------------------------------------------------------------------

    protected ConnectionManager() throws UnknownHostException {
        db = new MongoClient().getDB("jamopp");
        jongo = new Jongo(db, new JacksonMapper.Builder().registerModule(new SerializationModule())
                .enable(MapperFeature.AUTO_DETECT_GETTERS).build());
        dependencies = jongo.getCollection("dependencies");

    }

    public static ConnectionManager getInstance() throws UnknownHostException {
        if (_instance == null) {
            _instance = new ConnectionManager();
        }

        return _instance;
    }

    public MongoCollection getDependencies() {
        return dependencies;
    }
}
