package hr.fer.zemris.fuzzy;

import hr.fer.zemris.fuzzy.defuzzify.COADefuzzifier;
import hr.fer.zemris.fuzzy.defuzzify.Defuzzifier;
import hr.fer.zemris.fuzzy.function.IBinaryFunction;
import hr.fer.zemris.fuzzy.implication.Implication;
import hr.fer.zemris.fuzzy.implication.Mamdani;
import hr.fer.zemris.fuzzy.set.IFuzzySet;
import hr.fer.zemris.fuzzy.system.FuzzySystem;
import hr.fer.zemris.fuzzy.system.KormiloFuzzySystem;
import hr.fer.zemris.fuzzy.system.Rule;
import hr.fer.zemris.fuzzy.util.Debug;
import hr.fer.zemris.fuzzy.util.Operations;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class P1 {

    public static void main(String[] args) {
        // Biramo način dekodiranja neizrazitosti:
        Defuzzifier def = new COADefuzzifier();

        // Stvaranje oba sustava:
        // Grade se baze pravila i sve se inicijalizira
        Implication implication = new Mamdani(true);
        IBinaryFunction tNorm = Operations.zadehAnd();
        IBinaryFunction sNorm = Operations.zadehOr();
        FuzzySystem fsKormilo = new KormiloFuzzySystem(implication, tNorm, sNorm, def);
        List<Rule> rules = fsKormilo.getRules();
        System.out.println("Baza pravila za kormilo:");
        for (int i = 0; i < rules.size(); i++) {
            System.out.println((i + 1) + ". " + rules.get(i).getDescription());
        }

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("Izaberi pravilo ('kraj' za kraj): ");
            String line = sc.nextLine();
            if (line.toLowerCase().equals("kraj")) {
                System.out.println("Izlazim...");
                break;
            }
            int ruleNumber = Integer.parseInt(line);
            Rule rule = rules.get(ruleNumber - 1);
            System.out.print("Unesi L D LK DK V S: ");
            double[] values = Arrays.stream(sc.nextLine().split("\\s+"))
                    .mapToDouble(Double::parseDouble)
                    .toArray();
            IFuzzySet result = rule.apply(values, implication, tNorm);
            int a = (int) def.defuzzify(result);
            Debug.print(result, "Rezulat");
            System.out.println(a);
        }
        sc.close();
    }

}
