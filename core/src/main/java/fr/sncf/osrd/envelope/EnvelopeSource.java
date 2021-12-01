package fr.sncf.osrd.envelope;

public enum EnvelopeSource {
    /** The train reached its max rated speed */
    TRAIN_LIMIT,
    /** The train the max rated speed of the track */
    TRACK_LIMIT,
    /** The train slows does because it doesn't have enough power to overcome counteracting forces */
    MAX_EFFORT,
    /** The train is gaining speed */
    ACCELERATION,
    /** The train is loosing speed because the brakes are applied */
    BRAKING,
}
