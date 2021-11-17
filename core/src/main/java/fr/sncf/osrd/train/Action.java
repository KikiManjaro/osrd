package fr.sncf.osrd.train;

import java.util.Objects;

public class Action {

    private static final Action COAST;

    static {
        COAST = new Action(ActionType.COASTING);
    }

    public final ActionType type;
    /**
     * Encodes the force the driver decided to apply, in newton.
     * It can be nan in case the action does not involve any force.
     * */
    final double force;

    public enum ActionType {
        MAINTAIN_SPEED,
        ACCELERATION,
        COASTING,
        BRAKING;
    }

    /**
     * Create a new force-less action.
     * @param type the kind of action
     */
    private Action(ActionType type) {
        this.type = type;
        this.force = Double.NaN;
    }

    /**
     * Create a new force action.
     * @param type the force to apply
     * @param force the force associated with the action
     */
    private Action(ActionType type, double force) {
        assert !Double.isNaN(force);
        this.type = type;
        this.force = force;
    }

    /**
     * Gets the braking force
     * @return the braking force, or 0 if the action doesn't brake
     */
    public double brakingForce() {
        if (type != ActionType.BRAKING)
            return 0.0;
        return force;
    }

    /**
     * Gets the traction force
     * @return the traction force, or 0 if the action doesn't brake
     */
    public double tractionForce() {
        if (type != ActionType.ACCELERATION && type != ActionType.MAINTAIN_SPEED)
            return 0.0;
        return force;
    }

    public static Action accelerate(double force) {
        assert force > 0;
        return new Action(ActionType.ACCELERATION, force);
    }

    public static Action brake(double force) {
        assert force < 0.;
        return new Action(ActionType.BRAKING, force);
    }

    public static Action maintain(double force) {
        return new Action(ActionType.MAINTAIN_SPEED, force);
    }

    public static Action coast() {
        return COAST;
    }

    // region STD_OVERRIDES

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Action))
            return false;
        var o = (Action) obj;
        return this.type == o.type && this.force == o.force;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, force);
    }

    @Override
    public String toString() {
        return String.format("Action { type=%s, force=%f }",
                type.toString(), force);
    }

    // endregion
}
