package fr.sncf.osrd.railjson.schema.infra;

import fr.sncf.osrd.railjson.schema.common.ID;
import fr.sncf.osrd.utils.graph.EdgeEndpoint;

import java.util.Objects;

/** An identifier for a side of a specific track section */
public final class RJSTrackEndpoint {
    public ID<RJSTrackSection> section;
    public EdgeEndpoint endpoint;

    public RJSTrackEndpoint(ID<RJSTrackSection> section, EdgeEndpoint endpoint) {
        this.section = section;
        this.endpoint = endpoint;
    }

    @Override
    public int hashCode() {
        return Objects.hash(section, endpoint);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj.getClass() != RJSTrackEndpoint.class)
            return false;
        var o = (RJSTrackEndpoint) obj;
        return section.equals(o.section) && endpoint.equals(o.endpoint);
    }

    @Override
    public String toString() {
        return String.format(
                "RJSTrackSection.EndpointID { section=%s, endpoint=%s }",
                section.id, endpoint.toString()
        );
    }
}
