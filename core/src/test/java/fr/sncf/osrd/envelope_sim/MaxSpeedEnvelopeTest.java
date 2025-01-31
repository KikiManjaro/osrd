package fr.sncf.osrd.envelope_sim;

import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.sncf.osrd.envelope.Envelope;
import fr.sncf.osrd.envelope.EnvelopePart;
import fr.sncf.osrd.envelope_sim.pipelines.MaxSpeedEnvelope;
import fr.sncf.osrd.train.TestTrains;
import org.junit.jupiter.api.Test;


public class MaxSpeedEnvelopeTest {
    @Test
    public void testFlat() {
        var testRollingStock = TestTrains.REALISTIC_FAST_TRAIN;
        var testPath = new FlatPath(10000, 0);
        var stops = new double[] { 8500 };

        var flatMRSP = Envelope.make(
                        EnvelopePart.generateTimes(null, new double[] { 0, 10000 }, new double[] { 44.4, 44.4})
                );
        var maxSpeedEnvelope = MaxSpeedEnvelope.from(testRollingStock, testPath, stops, flatMRSP);
        assertTrue(maxSpeedEnvelope.spaceContinuous);

    }
}
