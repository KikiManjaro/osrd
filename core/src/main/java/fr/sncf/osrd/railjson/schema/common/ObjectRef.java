package fr.sncf.osrd.railjson.schema.common;

public class ObjectRef<T extends Identified> {
    public ID<T> id;
    public String type;

    public ObjectRef(ID<T> id, String type) {
        this.id = id;
        this.type = type;
    }

    public ObjectRef(String id, String type) {
        this(new ID<>(id), type);
    }
}
