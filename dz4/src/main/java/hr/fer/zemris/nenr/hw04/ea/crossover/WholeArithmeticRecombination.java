package hr.fer.zemris.nenr.hw04.ea.crossover;

import hr.fer.zemris.nenr.hw04.ea.solution.DoubleArraySolution;
import hr.fer.zemris.nenr.hw04.ea.solution.Solution;

/**
 * An implementation of {@link Crossover} interface which provides <i>whole arithmetic recombination</i> of genes for
 * solutions that are modeled through doubles.
 *
 * @author dbrcina
 */
public class WholeArithmeticRecombination implements Crossover<Solution<Double>> {

    @Override
    public Solution<Double> crossover(Solution<Double> parent1, Solution<Double> parent2) {
        double[] child = new double[parent1.getNumberOfGenes()];
        for (int i = 0; i < child.length; i++) {
            child[i] = (parent1.getGeneAt(i) + parent2.getGeneAt(i)) / 2;
        }
        return new DoubleArraySolution(child);
    }

}
