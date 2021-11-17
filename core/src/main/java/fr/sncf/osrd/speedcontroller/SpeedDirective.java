package fr.sncf.osrd.speedcontroller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;

public final class SpeedDirective {
    public double allowedSpeed;
    public boolean isCoasting;
    public boolean isBraking;

    /** Creates a new speed directive */
    public SpeedDirective(double allowedSpeed) {
        this.allowedSpeed = allowedSpeed;
        this.isCoasting = false;
        this.isBraking = false;
    }

    public static SpeedDirective getMax() {
        return new SpeedDirective(Double.POSITIVE_INFINITY);
    }

    /** Creates a speed directive indicating coasting over its range */
    public static SpeedDirective getCoastingDirective() {
        var directive = new SpeedDirective(Double.NaN);
        directive.isCoasting = true;
        return directive;
    }

    /** Creates a speed directive indicating braking over its range */
    public static SpeedDirective getBrakingDirective(double speed) {
        var directive = new SpeedDirective(speed);
        directive.isBraking = true;
        return directive;
    }

    /**
     * Combine with another speed limit
     * @param directive the speed limit to merge into the current one
     */
    public void mergeWith(SpeedDirective directive) {
        if (directive.isCoasting)
            isCoasting = true;
        //TODO : correct this
        else if (directive.allowedSpeed < allowedSpeed) {
            allowedSpeed = directive.allowedSpeed;
            isBraking = directive.isBraking;
        }
    }

    @Override
    @SuppressFBWarnings({"FE_FLOATING_POINT_EQUALITY"})
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != SpeedDirective.class)
            return false;
        var other = (SpeedDirective) obj;
        return allowedSpeed == other.allowedSpeed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowedSpeed);
    }
}
