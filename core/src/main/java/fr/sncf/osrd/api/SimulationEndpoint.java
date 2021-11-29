package fr.sncf.osrd.api;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fr.sncf.osrd.config.JsonConfig;
import fr.sncf.osrd.infra.InvalidInfraException;
import fr.sncf.osrd.infra.StopActionPoint.RestartTrainEvent.RestartTrainPlanned;
import fr.sncf.osrd.train.TrainSchedule;
import fr.sncf.osrd.api.InfraManager.InfraLoadException;
import fr.sncf.osrd.infra.Infra;
import fr.sncf.osrd.infra.routegraph.Route;
import fr.sncf.osrd.infra_state.routes.RouteState;
import fr.sncf.osrd.infra_state.routes.RouteStatus;
import fr.sncf.osrd.infra_state.SignalState;
import fr.sncf.osrd.railjson.parser.RJSSimulationParser;
import fr.sncf.osrd.railjson.parser.exceptions.InvalidRollingStock;
import fr.sncf.osrd.railjson.parser.exceptions.InvalidSchedule;
import fr.sncf.osrd.railjson.parser.exceptions.InvalidSuccession;
import fr.sncf.osrd.railjson.schema.RJSSimulation;
import fr.sncf.osrd.railjson.schema.common.ID;
import fr.sncf.osrd.railjson.schema.rollingstock.RJSRollingResistance;
import fr.sncf.osrd.railjson.schema.rollingstock.RJSRollingStock;
import fr.sncf.osrd.railjson.schema.schedule.RJSAllowance;
import fr.sncf.osrd.railjson.schema.schedule.RJSTrainPhase;
import fr.sncf.osrd.railjson.schema.schedule.RJSTrainSchedule;
import fr.sncf.osrd.railjson.schema.successiontable.RJSTrainSuccessionTable;
import fr.sncf.osrd.simulation.Change;
import fr.sncf.osrd.simulation.Simulation;
import fr.sncf.osrd.simulation.SimulationError;
import fr.sncf.osrd.simulation.changelog.ChangeConsumer;
import fr.sncf.osrd.simulation.changelog.ChangeConsumerMultiplexer;
import fr.sncf.osrd.train.Train;
import fr.sncf.osrd.train.events.TrainCreatedEvent;
import fr.sncf.osrd.utils.CurveSimplification;
import okio.Okio;
import org.takes.Request;
import org.takes.Response;
import org.takes.Take;
import org.takes.rq.RqPrint;
import org.takes.rs.RsJson;
import org.takes.rs.RsText;
import org.takes.rs.RsWithBody;
import org.takes.rs.RsWithStatus;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

public class SimulationEndpoint implements Take {
    private final InfraManager infraManager;

    public static final JsonAdapter<SimulationRequest> adapterRequest = new Moshi
            .Builder()
            .add(ID.Adapter.FACTORY)
            .add(RJSRollingResistance.adapter)
            .add(RJSTrainPhase.adapter)
            .add(RJSAllowance.adapter)
            .build()
            .adapter(SimulationRequest.class);

    public static final JsonAdapter<SimulationResult> adapterResult = new Moshi
            .Builder()
            .build()
            .adapter(SimulationResult.class);

    public SimulationEndpoint(InfraManager infraManager) {
        this.infraManager = infraManager;
    }

    public static Response runSimulation(Infra infra, SimulationRequest request)
            throws InvalidRollingStock, InvalidSuccession, InvalidSchedule, SimulationError {

        try {
            // load train schedules
            var rjsSimulation = new RJSSimulation(request.rollingStocks, request.trainSchedules,
                    request.trainSuccessionTables);
            var trainSchedules = RJSSimulationParser.parse(infra, rjsSimulation);
            var trainSuccessionTables = RJSSimulationParser.parseTrainSuccessionTables(rjsSimulation);

            // create the simulation and his changelog
            var changeConsumers = new ArrayList<ChangeConsumer>();
            var multiplexer = new ChangeConsumerMultiplexer(changeConsumers);
            var sim = Simulation.createFromInfraAndSuccessions(infra, trainSuccessionTables, 0, multiplexer);
            var resultLog = new ArrayResultLog(infra, sim);
            multiplexer.add(resultLog);

            // insert the train start events into the simulation
            for (var trainSchedule : trainSchedules)
                TrainCreatedEvent.plan(sim, trainSchedule);

            // run the simulation loop
            while (!sim.isSimulationOver())
                sim.step();

            // Check number of reached stops is what we expect
            resultLog.validate();

            // Simplify data
            resultLog.simplify();

            return new RsJson(new RsWithBody(adapterResult.toJson(resultLog.result)));
        } catch (Throwable ex) {
            ex.printStackTrace(System.err);
            throw ex;
        }
    }

    private File getLogDirectory() {
        var root = new File("requests_log_schedule");
        if (!root.exists()) {
            var rootCreated = root.mkdir();
            assert rootCreated;
        }
        var now = LocalDateTime.now();
        var path = new File(String.format("%s/%s_%s", root, now, new Random().nextLong()));
        assert !path.exists();
        var ok = path.mkdir();
        assert ok;
        return path.getAbsoluteFile();
    }

    private static void saveRequest(File logDirectory, SimulationRequest request)
            throws IOException {
        var path = logDirectory.getPath() + "/request.json";
        var json = adapterRequest.toJson(request);
        FileWriter fw = new FileWriter(path);
        fw.write(json);
        fw.close();
    }

    private static void saveResponse(File logDirectory, Response response, double time)
            throws IOException {
        var path = logDirectory.getPath() + "/response.txt";
        FileWriter fw = new FileWriter(path);
        fw.write(String.format("time: %fs%n", time));
        fw.write(String.format("response: %s%n", response));
        if (response instanceof RsJson) {
            var json = (RsJson) response;
            fw.write(String.format("response head:%n"));
            for (var str : json.head())
                fw.write(String.format("%s%n", str));
            fw.write(String.format("response body:%n"));
            fw.write(String.format("%s%n", new String(response.body().readAllBytes())));
        }
        fw.close();
    }

    public static void main(String[] args) {
        try {
            var infra = Infra.parseFromFile(JsonConfig.InfraType.RAILJSON, "test_bretagne/infra.json");
            var fileSource = Okio.source(Path.of("test_bretagne/request.json"));
            var bufferedSource = Okio.buffer(fileSource);
            var request = adapterRequest.fromJson(bufferedSource);
            assert request != null;
            var res = runSimulation(infra, request);
            System.out.println(res);
        } catch (InvalidInfraException | IOException | InvalidRollingStock |
                InvalidSuccession | InvalidSchedule | SimulationError e) {
            e.printStackTrace();
        }
    }

    @Override
    public Response act(Request req) throws
            IOException,
            InvalidRollingStock,
            InvalidSchedule,
            InvalidSuccession,
            SimulationError {
        // Parse request input
        var body = new RqPrint(req).printBody();
        var request = adapterRequest.fromJson(body);
        if (request == null)
            return new RsWithStatus(new RsText("missing request body"), 400);

        // load infra
        Infra infra;
        try {
            infra = infraManager.load(request.infra);
        } catch (InfraLoadException | InterruptedException e) {
            return new RsWithStatus(new RsText(
                    String.format("Error loading infrastructure '%s'%n%s", request.infra, e.getMessage())), 400);
        }
        var dir = getLogDirectory();
        saveRequest(dir, request);
        var begin = System.nanoTime();
        var res = runSimulation(infra, request);
        double time = (System.nanoTime() - begin);
        var timeSeconds = time / 1e9;
        saveResponse(dir, res, timeSeconds);
        return res;
    }



    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static final class SimulationRequest {
        /** Infra id */
        public final String infra;

        /** A list of rolling stocks involved in this simulation */
        @Json(name = "rolling_stocks")
        public List<RJSRollingStock> rollingStocks;

        /** A list of trains plannings */
        @Json(name = "train_schedules")
        public List<RJSTrainSchedule> trainSchedules;

        /** A list of trains successions tables */
        @Json(name = "train_succession_tables")
        public List<RJSTrainSuccessionTable> trainSuccessionTables;

        /** Create SimulationRequest */
        public SimulationRequest(
                String infra,
                List<RJSRollingStock> rollingStocks,
                List<RJSTrainSchedule> trainSchedules,
                List<RJSTrainSuccessionTable> trainSuccessionTables
        ) {
            this.infra = infra;
            this.rollingStocks = rollingStocks;
            this.trainSchedules = trainSchedules;
            this.trainSuccessionTables = trainSuccessionTables;
        }

        /** Create SimulationRequest with empty successions tables */
        public SimulationRequest(
                String infra,
                List<RJSRollingStock> rollingStocks,
                List<RJSTrainSchedule> trainSchedules
        ) {
            this.infra = infra;
            this.rollingStocks = rollingStocks;
            this.trainSchedules = trainSchedules;
            this.trainSuccessionTables = null;
        }
    }


    public static final class ArrayResultLog extends ChangeConsumer {
        public final SimulationResult result = new SimulationResult();
        public final Infra infra;
        public final HashMap<String, TrainSchedule> trainSchedules = new HashMap<>();
        public final Simulation sim;

        public ArrayResultLog(Infra infra, Simulation sim) {
            this.infra = infra;
            this.sim = sim;
        }

        /** Returns the train result for a given train */
        public SimulationResultTrain getTrainResult(String trainId) {
            var trainResult = result.trains.get(trainId);
            if (trainResult == null) {
                trainResult = new SimulationResultTrain();
                result.trains.put(trainId, trainResult);
            }
            return trainResult;
        }

        /** Ensures that the results are valid, throws a SimulationError otherwise */
        public void validate() throws SimulationError {
            for (var trainName : result.trains.keySet()) {
                var trainResult = result.trains.get(trainName);
                var nStopReached = trainResult.stopReaches.size();
                var trainSchedule = trainSchedules.get(trainName);
                var expectedStopReached = trainSchedule.stops.size();
                if (nStopReached != expectedStopReached) {
                    var err = String.format("Train '%s', unexpected stop number: expected %d, got %d",
                            trainName, expectedStopReached, nStopReached);
                    throw new SimulationError(err);
                }
            }
        }

        @Override
        public void changeCreationCallback(Change change) { }

        @Override
        @SuppressFBWarnings({"BC_UNCONFIRMED_CAST"})
        public void changePublishedCallback(Change change) {
            if (change.getClass() == RouteState.RouteStatusChange.class) {
                var routeStatusChange = (RouteState.RouteStatusChange) change;
                var route = infra.routeGraph.getEdge(routeStatusChange.routeIndex);
                var newStatus = routeStatusChange.newStatus;
                result.routesStatus.add(new SimulationResultRouteStatus(sim.getTime(), route, newStatus));
            } else if (change.getClass() == Train.TrainStateChange.class) {
                var trainStateChange = (Train.TrainStateChange) change;
                var trainResult = getTrainResult(trainStateChange.trainID);
                var train = trainSchedules.get(trainStateChange.trainID);
                for (var pos : trainStateChange.positionUpdates) {
                    trainResult.headPositions.add(new SimulationResultPosition(pos.time, pos.pathPosition, train));
                    var tailPathPosition = Math.max(0, pos.pathPosition - train.rollingStock.length);
                    trainResult.tailPositions.add(new SimulationResultPosition(pos.time, tailPathPosition, train));
                    trainResult.speeds.add(new SimulationResultSpeed(pos.time, pos.speed, pos.pathPosition));
                }
            } else if (change.getClass() == TrainCreatedEvent.TrainPlannedCreation.class) {
                // Cache train schedule
                var trainCreationPlanned = (TrainCreatedEvent.TrainPlannedCreation) change;
                trainSchedules.put(trainCreationPlanned.schedule.trainID, trainCreationPlanned.schedule);
                // Initial position and speed
                var train = trainCreationPlanned.schedule;
                var trainResult = getTrainResult(train.trainID);
                var creationTime = trainCreationPlanned.eventId.scheduledTime;
                trainResult.headPositions.add(new SimulationResultPosition(creationTime, 0, train));
                trainResult.tailPositions.add(new SimulationResultPosition(creationTime, 0, train));
                trainResult.speeds.add(new SimulationResultSpeed(creationTime, train.initialSpeed, 0));
            } else if (change.getClass() == SignalState.SignalAspectChange.class) {
                var aspectChange = (SignalState.SignalAspectChange) change;
                var signal = infra.signals.get(aspectChange.signalIndex).id;
                var aspects = new ArrayList<String>();
                for (var aspect : aspectChange.aspects)
                    aspects.add(aspect.id);
                result.signalChanges.add(new SimulationResultSignalChange(sim.getTime(), signal, aspects));
            } else if (change.getClass() == RestartTrainPlanned.class) {
                var stopReached = (RestartTrainPlanned) change;
                var trainResult = getTrainResult(stopReached.train.getID());
                var stopIndex = stopReached.stopIndex;
                var stopPosition = stopReached.train.schedule.stops.get(stopIndex).position;
                trainResult.stopReaches.add(new SimulationResultStopReach(sim.getTime(), stopIndex, stopPosition));
            }
        }

        private ArrayList<SimulationResultPosition> simplifyPositions(
                ArrayList<SimulationResultPosition> positions) {
            return CurveSimplification.rdp(
                    positions,
                    5.,
                    (point, start, end) -> {
                        if (Math.abs(start.time - end.time) < 0.000001)
                            return Math.abs(point.pathOffset - start.pathOffset);
                        var proj = start.pathOffset + (point.time - start.time)
                                * (end.pathOffset - start.pathOffset) / (end.time - start.time);
                        return Math.abs(point.pathOffset - proj);
                    }
            );
        }

        /** Simplifies the results using the Ramer-Douglas-Peucker algorithm */
        public ArrayResultLog simplify() {
            for (var train : result.trains.values()) {
                train.headPositions = simplifyPositions((ArrayList<SimulationResultPosition>) train.headPositions);
                train.tailPositions = simplifyPositions((ArrayList<SimulationResultPosition>) train.tailPositions);

                var speeds = (ArrayList<SimulationResultSpeed>) train.speeds;
                train.speeds = CurveSimplification.rdp(
                        speeds,
                        0.2,
                        (point, start, end) -> {
                            if (Math.abs(start.position - end.position) < 0.000001)
                                return Math.abs(point.speed - start.speed);
                            var proj = start.speed + (point.position - start.position)
                                    * (end.speed - start.speed) / (end.position - start.position);
                            return Math.abs(point.speed - proj);
                        }
                );
            }
            return this;
        }
    }

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static class SimulationResult {
        public Map<String, SimulationResultTrain> trains = new HashMap<>();
        @Json(name = "routes_status")
        public Collection<SimulationResultRouteStatus> routesStatus = new ArrayList<>();
        @Json(name = "signal_changes")
        public Collection<SimulationResultSignalChange> signalChanges = new ArrayList<>();
    }

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static class SimulationResultTrain {
        public Collection<SimulationResultSpeed> speeds = new ArrayList<>();
        @Json(name = "head_positions")
        public Collection<SimulationResultPosition> headPositions = new ArrayList<>();
        @Json(name = "tail_positions")
        public Collection<SimulationResultPosition> tailPositions = new ArrayList<>();
        @Json(name = "stop_reaches")
        public Collection<SimulationResultStopReach> stopReaches = new ArrayList<>();
    }

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static class SimulationResultSpeed {
        public final double time;
        public final double position;
        public final double speed;

        SimulationResultSpeed(double time, double speed, double position) {
            this.time = time;
            this.speed = speed;
            this.position = position;
        }
    }

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static class SimulationResultPosition {
        public final double time;
        @Json(name = "track_section")
        public final String trackSection;
        public final double offset;
        @Json(name = "path_offset")
        public final double pathOffset;

        SimulationResultPosition(double time, double pathOffset, TrainSchedule trainSchedule) {
            this.time = time;
            this.pathOffset = pathOffset;
            var location = trainSchedule.plannedPath.findLocation(pathOffset);
            this.trackSection = location.edge.id;
            this.offset = location.offset;
        }
    }

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static class SimulationResultRouteStatus {
        public final double time;
        @Json(name = "route_id")
        public final String routeId;
        public final RouteStatus status;
        @Json(name = "start_track_section")
        public final String startTrackSection;
        @Json(name = "start_offset")
        public final double startOffset;
        @Json(name = "end_track_section")
        public final String endTrackSection;
        @Json(name = "end_offset")
        public final double endOffset;

        SimulationResultRouteStatus(double time, Route route, RouteStatus status) {
            this.time = time;
            this.routeId = route.id;
            this.status = status;
            var start = route.tvdSectionsPaths.get(0).trackSections[0];
            this.startTrackSection = start.edge.id;
            this.startOffset = start.getBeginPosition();
            var lastIndex = route.tvdSectionsPaths.size() - 1;
            var lastTracks = route.tvdSectionsPaths.get(lastIndex).trackSections;
            var end = lastTracks[lastTracks.length - 1];
            this.endTrackSection = end.edge.id;
            this.endOffset = end.getEndPosition();
        }
    }

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static class SimulationResultSignalChange {
        public final double time;
        @Json(name = "signal_id")
        public final String signalId;
        public final List<String> aspects;

        SimulationResultSignalChange(double time, String signalId, List<String> aspects) {
            this.time = time;
            this.signalId = signalId;
            this.aspects = aspects;
        }
    }

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static class SimulationResultStopReach {
        public final double time;
        @Json(name = "stop_index")
        public final int stopIndex;
        public final double position;

        SimulationResultStopReach(double time, int stopIndex, double position) {
            this.time = time;
            this.stopIndex = stopIndex;
            this.position = position;
        }
    }
}

