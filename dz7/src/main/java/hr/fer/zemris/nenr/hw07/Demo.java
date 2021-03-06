package hr.fer.zemris.nenr.hw07;

import hr.fer.zemris.bscthesis.classes.ClassType;
import hr.fer.zemris.bscthesis.dataset.Dataset;
import hr.fer.zemris.bscthesis.dataset.Sample;
import hr.fer.zemris.nenr.hw04.ea.EvolutionaryAlgorithm;
import hr.fer.zemris.nenr.hw04.ea.crossover.ArithmeticCrossover;
import hr.fer.zemris.nenr.hw04.ea.crossover.BLXACrossover;
import hr.fer.zemris.nenr.hw04.ea.crossover.UniformCrossover;
import hr.fer.zemris.nenr.hw04.ea.ga.EliminationGeneticAlgorithm;
import hr.fer.zemris.nenr.hw04.ea.initializer.RandomDoublePopulationInitializer;
import hr.fer.zemris.nenr.hw04.ea.mutation.GaussMutation;
import hr.fer.zemris.nenr.hw04.ea.selection.KTournamentSelection;
import hr.fer.zemris.nenr.hw04.ea.solution.Solution;
import hr.fer.zemris.nenr.hw07.model.NeuralNetwork;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author dbrcina
 */
public class Demo {

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Program expects 3 arguments:");
            System.out.println("    (1) path to a dataset file,");
            System.out.println("    (2) architecture,");
            System.out.println("    (3) path to a save file.");
            System.out.println("Exiting...");
            return;
        }

        double minValue = -1;
        double maxValue = 1;
        Path file = Paths.get(args[0]);
        Dataset dataset = createDataset(file);
        NeuralNetwork nn = new NeuralNetwork(args[1]);
        Random random = new Random();
        int populationSize = 50;
        double epsilon = 1e-7;
        int maxGenerations = 100_000;
        EvolutionaryAlgorithm<Solution<Double>> alg = new EliminationGeneticAlgorithm<>(
                random,
                populationSize,
                maxGenerations,
                epsilon,
                new RandomDoublePopulationInitializer(random, nn.getWeightsCount(), minValue, maxValue),
                new KTournamentSelection<>(random, 3),
                List.of(
                        new BLXACrossover(random, 0.5),
                        new UniformCrossover(random),
                        new ArithmeticCrossover()
                ),
                List.of(
                        new GaussMutation(random, 0.1, 0.02, true),
                        new GaussMutation(random, 0.2, 0.02, true),
                        new GaussMutation(random, 1, 0.02, false)
                ),
                new int[]{3, 1, 1},
                solution -> -nn.calcError(dataset, Arrays.stream(solution.getGenes()).mapToDouble(d -> d).toArray())
        );
        Solution<Double> solution = alg.run();
        double[] weights = Arrays.stream(solution.getGenes()).mapToDouble(d -> d).toArray();
        System.out.println();
        nn.statistics(dataset, weights);

        // Save weights to the file
        String save = args[2];
        System.out.println("Spremam težine u '" + save + "'...");
        try {
            Files.writeString(Paths.get(save), Arrays.stream(weights)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(" ")));
        } catch (Exception e) {
            System.out.println("Nije uspjelo!");
        }
        System.out.println("Uspješno spremljeno!");
    }

    private static Dataset createDataset(Path file) throws Exception {
        Dataset dataset = new Dataset() {
            private List<Sample> samples;

            @Override
            public void setSamples(List<Sample> samples) {
                this.samples = samples;
            }

            @Override
            public int numberOfSamples() {
                return samples.size();
            }

            @Override
            public List<Sample> shuffleSamples() {
                List<Sample> shuffled = new ArrayList<>(samples);
                Collections.shuffle(shuffled);
                return shuffled;
            }

            @Override
            public void loadDataset(Path file) throws Exception {
                List<String> lines = Files.readAllLines(file);
                samples = lines.stream()
                        .map(line -> {
                            double[] data = Arrays.stream(line.split("\\s+"))
                                    .mapToDouble(Double::parseDouble)
                                    .toArray();
                            double[] inputs = {data[0], data[1]};
                            double[] outputs = {data[2], data[3], data[4]};
                            return new Sample(inputs, outputs, ClassType.determineFor(outputs));
                        })
                        .collect(Collectors.toList());
            }

            @Override
            public Iterator<Sample> iterator() {
                return samples.iterator();
            }
        };
        dataset.loadDataset(file);
        return dataset;
    }

}
