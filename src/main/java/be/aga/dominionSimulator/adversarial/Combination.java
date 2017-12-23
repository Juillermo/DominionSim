package be.aga.dominionSimulator.adversarial;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import be.aga.dominionSimulator.enums.DomCardName;

public class Combination {

	private EnumMap<DomCardName, Integer> cardsMap;
	private int r;
	private Set<EnumMap<DomCardName, Integer>> solution = new HashSet<EnumMap<DomCardName, Integer>>();
	private ArrayList<DomCardName> result = new ArrayList<DomCardName>();

	public Combination(EnumMap<DomCardName, Integer> cardsMap, int r) {
		this.cardsMap = cardsMap;
		this.r = r;
		for (int i = 0; i < r; i++)
			result.add(null);
	}

	public Set<EnumMap<DomCardName, Integer>> combinations() {
		subcombinations(new ArrayList<DomCardName>(cardsMap.keySet()), 0);
		return solution;
	}

	private void subcombinations(List<DomCardName> options, int index_r) {
		// if (r < index_r) {
		// ArrayList<DomCardName> newResult = new ArrayList<DomCardName>();
		// for (DomCardName item : result)
		// newResult.add(item);
		// set.add(newResult);
		// return;
		// }

		for (int i = index_r; i <= r; i++) {
			if (i > index_r)
				result.set(i - 1, options.get(0));
			if (options.size() > 1) {
				subcombinations(options.subList(1, options.size()), i);
			} else if (options.size() == 1) {
				fillWith(options.get(0), i);
				return;
			} else {
				try {
					throw new Exception("Combinations: no options available");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		fillWith(options.get(0), index_r);
		return;
		// for (int i = 0; i < options.length; i++){
		// result[result.length - r] = options[i];
		// subcombinations(options, r-1, result, set);
		// }
	}

	private void fillWith(DomCardName option, int index) {
		ArrayList<DomCardName> newResult = new ArrayList<DomCardName>();
		for (DomCardName item : result)
			newResult.add(item);

		for (int i = index + 1; i <= r; i++)
			newResult.set(i - 1, option);
		
		for(DomCardName cardName : cardsMap.keySet()) {
			int count = 0;
			for(DomCardName cardNameResult : newResult) {
				if(cardName == cardNameResult)
					count++;
			}
			if(count > cardsMap.get(cardName)) return;
		}
		
		EnumMap<DomCardName, Integer> diffCards1 = new EnumMap<DomCardName, Integer>(DomCardName.class);
		for (DomCardName card : newResult)
			if (!diffCards1.containsKey(card))
				diffCards1.put(card, 1);
			else
				diffCards1.put(card, diffCards1.get(card) + 1);
		
		solution.add(diffCards1);
	}
}