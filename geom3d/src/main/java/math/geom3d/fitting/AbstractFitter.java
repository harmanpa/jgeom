/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package math.geom3d.fitting;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;

/**
 *
 * @author peter
 * @param <T>
 * @param <X>
 */
public class AbstractFitter<T, X> {

    private final int nParameters;
    private final Function<double[], T> constructor;
    private final Function<T, double[]> destructor;
    private final BiFunction<T, X, Double> assessor;
    private final Function<List<X>, T> guesser;

    protected AbstractFitter(int nParameters,
            Function<double[], T> constructor,
            Function<T, double[]> destructor,
            BiFunction<T, X, Double> assessor,
            Function<List<X>, T> guesser) {
        this.nParameters = nParameters;
        this.constructor = constructor;
        this.destructor = destructor;
        this.assessor = assessor;
        this.guesser = guesser;
    }

    public T fit(List<X> target, T initial) {

        // the target is to have all points at the specified radius from the center
        double[] prescribedErrors = new double[target.size()];
        Arrays.fill(prescribedErrors, 0.0);

        // least squares problem to solve : modeled radius should be close to target radius
        LeastSquaresProblem problem = new LeastSquaresBuilder().
                start(destructor.apply(initial)).
                model(fj(
                        f(constructor, assessor, target),
                        1e-3,
                        target.size(),
                        nParameters)
                ).
                target(prescribedErrors).
                lazyEvaluation(false).
                maxEvaluations(1000).
                maxIterations(1000).
                build();
        LeastSquaresOptimizer.Optimum optimum = new LevenbergMarquardtOptimizer().optimize(problem);
        System.out.println("RMS: " + optimum.getRMS());
        System.out.println("evaluations: " + optimum.getEvaluations());
        System.out.println("iterations: " + optimum.getIterations());
        return constructor.apply(optimum.getPoint().toArray());
    }

    public T fit(List<X> target) {
        return fit(target, guesser.apply(target));
    }

    private MultivariateVectorFunction f(Function<double[], T> constructor, BiFunction<T, X, Double> assessor, List<X> target) {
        return (double[] x) -> {
            double[] out = new double[target.size()];
            T obj = constructor.apply(x);
            for (int j = 0; j < target.size(); j++) {
                out[j] = assessor.apply(obj, target.get(j));
            }
            return out;
        };
    }

    private MultivariateJacobianFunction fj(MultivariateVectorFunction f, double increment, int d, int p) {
        return (RealVector x) -> {
            RealVector value = new ArrayRealVector(d);
            RealMatrix jacobian = new Array2DRowRealMatrix(d, p);
            double[] a = f.value(x.toArray());
            for (int j = 0; j < d; j++) {
                value.setEntry(j, a[j]);
            }
            jacobian(f, jacobian, x, increment, d, p);
            return new Pair<>(value, jacobian);
        };
    }

    private void jacobian(MultivariateVectorFunction f, RealMatrix jacobian, RealVector x, double increment, int d, int p) {
        for (int i = 0; i < p; i++) {
            double v = x.getEntry(i);
            x.setEntry(i, v + increment / 2);
            double[] a = f.value(x.toArray());
            x.setEntry(i, v - increment / 2);
            double[] b = f.value(x.toArray());
            for (int j = 0; j < d; j++) {
                jacobian.setEntry(j, i, (a[j] - b[j]) / increment);
            }
            x.setEntry(i, v);
        }
    }
}
