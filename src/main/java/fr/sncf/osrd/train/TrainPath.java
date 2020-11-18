package fr.sncf.osrd.train;

import fr.sncf.osrd.infra.graph.EdgeDirection;
import fr.sncf.osrd.infra.topological.TopoEdge;
import fr.sncf.osrd.util.CryoList;

public class TrainPath {
    public class PathElement {
        public final TopoEdge edge;
        public final EdgeDirection direction;

        public PathElement(TopoEdge edge, EdgeDirection direction) {
            this.edge = edge;
            this.direction = direction;
        }
    }

    final CryoList<PathElement> edges;
    final CryoList<TrainStop> stops;

    /**
     * Creates a container to hold the path some train will follow
     * @param edges the list of edges the train will travel through
     * @param stops the list of stops
     */
    public TrainPath(CryoList<PathElement> edges, CryoList<TrainStop> stops) {
        this.edges = edges;
        this.stops = stops;
        edges.freeze();
        stops.freeze();
    }
}
