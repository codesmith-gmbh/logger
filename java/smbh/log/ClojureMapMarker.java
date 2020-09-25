package smbh.log;

import clojure.java.api.Clojure;
import clojure.lang.IDeref;
import clojure.lang.IFn;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.argument.StructuredArgument;
import net.logstash.logback.marker.LogstashMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

public class ClojureMapMarker extends LogstashMarker implements StructuredArgument {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClojureMapMarker.class);

    private static final IFn REQUIRE = Clojure.var("clojure.core", "require");
    private static final IFn NAME = Clojure.var("clojure.core", "name");
    private static final IFn PR_STR = Clojure.var("clojure.core", "pr-str");
    private static final IFn GENERATE_STRING;

    static {
        REQUIRE.invoke(Clojure.read("cheshire.core"));
        GENERATE_STRING = Clojure.var("cheshire.core", "generate-string");
    }

    private final Map<?, ?> map;

    public ClojureMapMarker(Map<?, ?> map) {
        super("CLJ_MAP_MARKER");
        this.map = map;
    }

    @Override
    public void writeTo(JsonGenerator generator) throws IOException {
        if (map != null) {
            map.forEach((k, v) -> {
                try {
                    generator.writeFieldName((String) NAME.invoke(k));
                    Object value = v instanceof IDeref ? ((IDeref) v).deref() : v;
                    try {
                        generator.writeRawValue((String) GENERATE_STRING.invoke(value));
                    } catch (Exception e) {
                        LOGGER.warn("Serialization error", e);
                        generator.writeString((String) PR_STR.invoke(value));
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    public String toString() {
        final StringBuilder builder = new StringBuilder();
        if (map != null) {
            map.forEach((k, v) -> {
                builder.append((String) NAME.invoke(k));
                builder.append(" ");
                Object value = v instanceof IDeref ? ((IDeref) v).deref() : v;
                try {
                    builder.append((String) GENERATE_STRING.invoke(value));
                } catch (Exception e) {
                    LOGGER.warn("Serialization error", e);
                    builder.append((String) PR_STR.invoke(value));
                }
                builder.append(", ");
            });
        }
        return builder.toString();
    }

}
