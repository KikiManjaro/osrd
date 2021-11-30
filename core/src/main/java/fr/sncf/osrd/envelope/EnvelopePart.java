package fr.sncf.osrd.envelope;


import java.util.Arrays;
import java.util.Objects;

public final class EnvelopePart {
    public final EnvelopeType type;
    public final EnvelopeAttitude attitude;

    public final double[] positions;
    public final double[] speeds;

    /** Create an EnvelopePart */
    public EnvelopePart(EnvelopeType type, EnvelopeAttitude attitude, double[] positions, double[] speeds) {
        assert positions.length == speeds.length;
        assert positions.length >= 2;
        this.type = type;
        this.attitude = attitude;
        this.positions = positions;
        this.speeds = speeds;
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
        return attitude == that.attitude && type == that.type
                && Arrays.equals(positions, that.positions) && Arrays.equals(speeds, that.speeds);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type, attitude);
        result = 31 * result + Arrays.hashCode(positions);
        result = 31 * result + Arrays.hashCode(speeds);
        return result;
    }
}
