package fr.sncf.osrd.envelope;

public enum EnvelopeAttitude {
    ACCELERATING,
    CONSTANT_SPEED,
    COASTING,
    BRAKING,
    // this attitude is only used for testing the envelope framework
    UNKNOWN,
}
