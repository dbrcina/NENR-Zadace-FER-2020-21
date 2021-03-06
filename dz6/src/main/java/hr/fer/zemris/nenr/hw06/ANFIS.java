package hr.fer.zemris.nenr.hw06;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author dbrcina
 */
public class ANFIS {

    private static final Function<double[], Double> MEMBERSHIP_FUNCTION = array -> {
        double x = array[0];
        double a = array[1];
        double b = array[2];
        return 1 / (1 + Math.exp(b * (x - a)));
    };

    private int numberOfRules;
    private double[] a;
    private double[] b;
    private double[] c;
    private double[] d;
    private double[] p;
    private double[] q;
    private double[] r;
    private boolean stochastic = true;
    private int epochs = (int) 1e6;
    private double tol = 0.02;
    private double etaXY = 1e-4;
    private double etaZ = 3e-5;
    private boolean writeError = false;
    private int writeErrorNEpochs = 100;
    private String errorFile;

    private ANFIS() {
    }

    public double predict(double x, double y) {
        double[] tmp = new double[numberOfRules];
        return predict(x, y, tmp, tmp, tmp, tmp);
    }

    public ANFIS fit(double[][] samples) {
        double[] ak = new double[numberOfRules];
        double[] bk = new double[numberOfRules];
        double[] alphaK = new double[numberOfRules];
        double[] ck = new double[numberOfRules];
        double[] dk = new double[numberOfRules];
        double[] betaK = new double[numberOfRules];
        double[] productK = new double[numberOfRules];
        double[] pk = new double[numberOfRules];
        double[] qk = new double[numberOfRules];
        double[] rk = new double[numberOfRules];
        double[] zk = new double[numberOfRules];

        double[][][] batches = prepareBatches(samples);
        boolean reachedTol = false;
        StringJoiner sj = new StringJoiner(" ");
        for (int epoch = 0; epoch < epochs; epoch++) {
            double error = 0.0;

            for (double[][] batch : batches) {
                for (double[] sample : batch) {
                    double x = sample[0];
                    double y = sample[1];
                    double f = sample[2];
                    double output = predict(x, y, alphaK, betaK, productK, zk);
                    double outputError = f - output;
                    error += outputError * outputError;
                    double productSum = Arrays.stream(productK).sum();
                    double productSumSquared = productSum * productSum;
                    for (int i = 0; i < numberOfRules; i++) {
                        double nominator = 0.0;
                        for (int j = 0; j < numberOfRules; j++) {
                            if (j != i) {
                                nominator += productK[j] * (zk[i] - zk[j]);
                            }
                        }
                        double fraction = nominator / productSumSquared;
                        double abCommon = outputError * fraction * betaK[i] * alphaK[i] * (1 - alphaK[i]);
                        ak[i] += abCommon * b[i];
                        bk[i] += abCommon * (a[i] - x);
                        double cdCommon = outputError * fraction * alphaK[i] * betaK[i] * (1 - betaK[i]);
                        ck[i] += cdCommon * d[i];
                        dk[i] += cdCommon * (c[i] - y);
                        pk[i] += outputError * productK[i] * x / productSum;
                        qk[i] += outputError * productK[i] * y / productSum;
                        rk[i] += outputError * productK[i] / productSum;
                    }
                }
                for (int i = 0; i < numberOfRules; i++) {
                    a[i] += etaXY * ak[i];
                    ak[i] = 0;
                    b[i] += etaXY * bk[i];
                    bk[i] = 0;
                    c[i] += etaXY * ck[i];
                    ck[i] = 0;
                    d[i] += etaXY * dk[i];
                    dk[i] = 0;
                    p[i] += etaZ * pk[i];
                    pk[i] = 0;
                    q[i] += etaZ * qk[i];
                    qk[i] = 0;
                    r[i] += etaZ * rk[i];
                    rk[i] = 0;
                }
            }
            error /= (2 * samples.length);
            if ((epoch + 1) % writeErrorNEpochs == 0) {
                sj.add(String.valueOf(error));
            }
            reachedTol = exitCondition(error, epoch);
            if (reachedTol) break;
        }

        if (!reachedTol) {
            System.out.println("Reached maximum number of epochs (" + epochs + "). Exiting...");
        }
        if (writeError) {
            try {
                System.out.println("Saving errors through epochs into " + errorFile + " file");
                Files.writeString(Paths.get(errorFile), sj.toString());
                System.out.println("Saved successfully!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    private double predict(
            double x, double y, double[] alpha, double[] beta, double[] product, double[] z) {
        double output = 0.0;
        double productSum = 0.0;
        for (int i = 0; i < numberOfRules; i++) {
            double alphaI = MEMBERSHIP_FUNCTION.apply(new double[]{x, a[i], b[i]});
            double betaI = MEMBERSHIP_FUNCTION.apply(new double[]{y, c[i], d[i]});
            double productI = alphaI * betaI;
            productSum += productI;
            double zi = p[i] * x + q[i] * y + r[i];
            output += productI * zi;
            alpha[i] = alphaI;
            beta[i] = betaI;
            product[i] = productI;
            z[i] = zi;
        }
        return output / productSum;
    }

    private double[][][] prepareBatches(double[][] samples) {
        int batchSize = stochastic ? 1 : samples.length;
        int capacity = samples.length / batchSize;
        double[][][] batches = new double[capacity][batchSize][samples[0].length];
        int offset = 0;
        for (int i = 0; i < capacity; i++) {
            double[][] batch = batches[i];
            for (int j = 0; j < batch.length; j++) {
                batch[j] = samples[offset++];
            }
        }
        return batches;
    }

    private boolean exitCondition(double error, int epoch) {
        boolean exit = error <= tol;
        if (epoch == 0 || exit || (epoch + 1) % 1000 == 0) {
            System.out.println("Epoch " + (epoch + 1) + ": error = " + error);
            if (exit) {
                System.out.println("Found closest error! Exiting...");
            }
        }
        return exit;
    }

    public void writeParamsToFiles() {
        try {
            System.out.println("Saving optimal parameters into files...");
            Files.writeString(Paths.get("a.txt"),
                    Arrays.stream(a).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
            Files.writeString(Paths.get("b.txt"),
                    Arrays.stream(b).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
            Files.writeString(Paths.get("c.txt"),
                    Arrays.stream(c).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
            Files.writeString(Paths.get("d.txt"),
                    Arrays.stream(d).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
            Files.writeString(Paths.get("p.txt"),
                    Arrays.stream(p).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
            Files.writeString(Paths.get("q.txt"),
                    Arrays.stream(q).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
            Files.writeString(Paths.get("r.txt"),
                    Arrays.stream(r).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
            System.out.println("Parameters saved successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ANFIS init(int numberOfRules, Consumer<double[]> action) {
        this.numberOfRules = numberOfRules;
        a = new double[numberOfRules];
        b = new double[numberOfRules];
        c = new double[numberOfRules];
        d = new double[numberOfRules];
        p = new double[numberOfRules];
        q = new double[numberOfRules];
        r = new double[numberOfRules];
        if (action != null) {
            action.accept(a);
            action.accept(b);
            action.accept(c);
            action.accept(d);
            action.accept(p);
            action.accept(q);
            action.accept(r);
        }
        return this;
    }

    public ANFIS setStochastic(boolean stochastic) {
        this.stochastic = stochastic;
        return this;
    }

    public ANFIS setEpochs(int epochs) {
        this.epochs = epochs;
        return this;
    }

    public ANFIS setTol(double tol) {
        this.tol = tol;
        return this;
    }

    public ANFIS setEtaXY(double etaXY) {
        this.etaXY = etaXY;
        return this;
    }

    public ANFIS setEtaZ(double etaZ) {
        this.etaZ = etaZ;
        return this;
    }

    public ANFIS setWriteError(boolean writeError) {
        this.writeError = writeError;
        return this;
    }

    public ANFIS setWriteErrorNEpochs(int writeErrorNEpochs) {
        this.writeErrorNEpochs = writeErrorNEpochs;
        return this;
    }

    public ANFIS setErrorFile(String errorFile) {
        this.errorFile = errorFile;
        return this;
    }

    public static ANFIS builder() {
        return new ANFIS();
    }

}
