package fr.sncf.osrd.envelope;

import com.carrotsearch.hppc.DoubleArrayList;

public final class EnvelopePartBuilder {
    private final DoubleArrayList positions = new DoubleArrayList();
    private final DoubleArrayList speeds = new DoubleArrayList();
    private final DoubleArrayList times = new DoubleArrayList();
    private final EnvelopeSource type;
    private final EnvelopeAttitude attitude;
    private final boolean physicallyAccurate;

    /** Prepares an envelope builder */
    public EnvelopePartBuilder(EnvelopeSource type, EnvelopeAttitude attitude, boolean physicallyAccurate) {
        this.type = type;
        this.attitude = attitude;
        this.physicallyAccurate = physicallyAccurate;
    }

    public boolean isEmpty() {
        return positions.isEmpty();
    }

    /** Add a point to the envelope part */
    public void add(double position, double speed) {
        if (!positions.isEmpty()) {
            var lastPos = positions.get(positions.size() - 1);
            var lastSpeed = speeds.get(positions.size() - 1);
            if (position == lastPos && speed == lastSpeed)
                return;
            assert position > lastPos;
        }
        positions.add(position);
        speeds.add(speed);
    }

    /** Add a point to the envelope part */
    public void add(double position, double speed, double timeDelta) {
        this.add(position, speed);
        times.add(timeDelta);
    }

    /** Creates an envelope part */
    public EnvelopePart build() {
        double [] times = null;
        if (this.times.size() != 0)
            times = this.times.toArray();
        return new EnvelopePart(type, attitude, physicallyAccurate, positions.toArray(), speeds.toArray(), times);
    }
}
