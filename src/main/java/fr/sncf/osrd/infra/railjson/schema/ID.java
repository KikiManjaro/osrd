package fr.sncf.osrd.infra.railjson.schema;

import com.squareup.moshi.*;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.io.IOException;

public final class ID<T extends Identified> {
    public final String id;

    public ID(String id) {
        this.id = id;
    }

    public static <T extends Identified> ID<T> from(T obj) {
        return new ID<>(obj.getID());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (obj.getClass() != ID.class)
            return false;

        var o = (ID<?>) obj;
        return id.equals(o.id);
    }

    /** A moshi adapter for ID serialization */
    public static class Adapter<T extends Identified> extends JsonAdapter<ID<T>> {
        @SuppressWarnings("rawtypes")
        public static final JsonAdapter.Factory FACTORY = new Adapter().factory();

        private JsonAdapter.Factory factory() {
            return (type, annotations, moshi) -> {
                // the raw type is the one without a type parameter
                Class<?> rawType = Types.getRawType(type);
                if (!annotations.isEmpty())
                    return null;

                // if the type of the objects to adapt isn't something the factory can produce adapters for,
                // return null to tell the frame
                if (rawType != ID.class)
                    return null;

                return this;
            };
        }

        @Override
        public ID<T> fromJson(JsonReader reader) throws IOException {
            return new ID<T>(reader.nextString());
        }

        @Override
        public void toJson(@NonNull JsonWriter writer, ID<T> value) throws IOException {
            if (value != null)
                writer.value(value.id);
            else
                writer.nullValue();
        }
    }
}
