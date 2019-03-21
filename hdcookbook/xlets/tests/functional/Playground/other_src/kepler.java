
/**
 * Compute the coordiates of an orbit, using Kepler's
 * laws of planetary motion.  I got the formulae from
 * http://en.wikipedia.org/wiki/Kepler%27s_Laws_of_Planetary_Motion ,
 * and the algorithm to solve Kepler's equation from
 * http://www.venus-2004.de/kepler/kepler.html .
 * <p>
 * I ran this with "javac kepler.java ; java kepler > /tmp/out" and
 * then I pasted /tmp/out into ../src/main_show.txt
 **/
public class kepler  {

    public final static double P = 360.0;       // Period
    public final static double ecc = 0.4;       // Eccentricity of orbit
    public final static double p = 400.0;       // semi-latus rectum
        // Yeah, it's really called "semi-latus rectum".  It's a measure
        // of the size of an ellipse.

    public static void main(String[] args) {
        for (int i = 1; i <= P; i++) {
            double M = (2.0 * Math.PI * i) / P;         // mean anomaly
            double E = eccentricAnomaly(M);
            double theta = trueAnomaly(E);
            double r = p / (1.0 + ecc * Math.cos(theta));
            long x = -Math.round(r * Math.cos(theta));
            long y = Math.round(r * Math.sin(theta));
            System.out.print("( " + x + " " + y + " ) ");
            if (i % 4 == 0) {
                System.out.println();
            }
        }
    }

    public static double eccentricAnomaly(double M) {
        double delta = 1.0/10000.0;     // Accurate to that much of a radian
        double E;
        if (ecc < 0.8) {
            E = M;
        } else {
            E = Math.PI;
        }
        double F = E - ecc*Math.sin(M) - M;
            // Numerically solve Kepler's equation for E:
            //    M = E - ecc * sin(E)
        while (Math.abs(F) > delta) {
            E = E - F / (1.0 - ecc * Math.cos(E));
            F = E - ecc*Math.sin(M) - M;
        }
        return E;
    }

    public static double trueAnomaly(double E) {
        double f = Math.sqrt((1.0 + ecc) / (1.0 - ecc));
        double E2 = E / 2.0;
        return 2.0 * Math.atan2(f * Math.sin(E2), Math.cos(E2));
    }

}
