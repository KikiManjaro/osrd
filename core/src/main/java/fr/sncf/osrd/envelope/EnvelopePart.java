package fr.sncf.osrd.envelope;


import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Objects;

public final class EnvelopePart {
    /** Tracks what created this envelope part */
    public final EnvelopeSource source;
    /** The shape of this envelope part */
    public final EnvelopeAttitude attitude;

    /** Whether the train can actually follow this envelope part
     * TODO(Giuliana): find a better name
     */
    public final boolean physicallyAccurate;

    /** A list of N spacial offsets */
    public final double[] positions;
    /** A list of N speeds, one per position */
    public final double[] speeds;

    /** A list of N - 1 time deltas between positions */
    private double[] cachedTimes;

    /** Creates an EnvelopePart */
    @SuppressFBWarnings({"EI_EXPOSE_REP2"})
    public EnvelopePart(
            EnvelopeSource source,
            EnvelopeAttitude attitude,
            boolean physicallyAccurate,
            double[] positions,
            double[] speeds,
            double[] cachedTimes
    ) {
        assert positions.length == speeds.length;
        assert positions.length >= 2;
        assert cachedTimes == null || cachedTimes.length == positions.length - 1;
        this.source = source;
        this.attitude = attitude;
        this.physicallyAccurate = physicallyAccurate;

        this.positions = positions;
        this.speeds = speeds;
        this.cachedTimes = cachedTimes;
    }

    public EnvelopePart(
            EnvelopeSource source,
            EnvelopeAttitude attitude,
            boolean physicallyAccurate,
            double[] positions,
            double[] speeds
    ) {
        this(source, attitude, physicallyAccurate, positions, speeds, null);
    }

    private static double[] computeTimes(double[] positions, double[] speeds) {
        var times = new double[positions.length - 1];
        for (int i = 0; i < positions.length - 1; i++) {
            double positionDelta = positions[i + 1] - positions[i];
            double averageSpeed = (speeds[i] + speeds[i + 1]) / 2;
            times[i] = positionDelta / averageSpeed;
        }
        return times;
    }

    /** Returns the time deltas between positions */
    @SuppressFBWarnings({"EI_EXPOSE_REP"})
    public double[] getTimes() {
        if (this.cachedTimes == null)
            this.cachedTimes = computeTimes(positions, speeds);
        return this.cachedTimes;
    }

    /** Given a position return the index of the interval contain this position */
    public int getPosIndex(double pos) {
        assert pos >= getBegin();
        assert pos <= getEnd();
        for (int i = 1; i < positions.length; i++)
            if (pos <= positions[i])
                return i - 1;
        throw new RuntimeException("position out of bounds");
    }

    /** Given a position return the interpolated speed */
    public double interpolateSpeed(double position) {
        var i = getPosIndex(position);
        var lastPos = positions[i];
        var nextPos = positions[i + 1];
        var lastSpeed = speeds[i];
        var nextSpeed = speeds[i + 1];

        var delta = nextPos - lastPos;
        var offset = position - lastPos;
        return lastSpeed + (nextSpeed - lastSpeed) * offset / delta;
    }

    public double getBegin() {
        return positions[0];
    }

    public double getEnd() {
        return positions[positions.length - 1];
    }

    public int size() {
        return positions.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnvelopePart that = (EnvelopePart) o;
        return attitude == that.attitude && source == that.source
                && Arrays.equals(positions, that.positions) && Arrays.equals(speeds, that.speeds);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(source, attitude);
        result = 31 * result + Arrays.hashCode(positions);
        result = 31 * result + Arrays.hashCode(speeds);
        return result;
    }
}
