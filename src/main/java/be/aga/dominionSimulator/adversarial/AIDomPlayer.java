package be.aga.dominionSimulator.adversarial;

import java.util.List;
import java.security.KeyStore.Entry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import be.aga.dominionSimulator.*;
import be.aga.dominionSimulator.enums.DomCardName;
import be.aga.dominionSimulator.enums.DomCardType;
import be.aga.dominionSimulator.adversarial.DeepCopy;

/**
 * Represents the AI search bot in a simulated game.
 */
public class AIDomPlayer extends DomPlayer {
	private int currentPly;

	public AIDomPlayer(String aString, int ply) {
		super(aString);
		currentPly = ply;
	}

	@Override
	public void makeBuyDecision() {
		
	}

	private void maxValue() {
		DomBoard board = getCurrentGame().getBoard();

		List<Map.Entry<DomCardName, ArrayList<DomCard>>> list = new ArrayList<Map.Entry<DomCardName, ArrayList<DomCard>>>(
				board.entrySet());

		// Randomize cards to buy
		// TODO Sort them by ranking of how good they are
		Collections.shuffle(list);
		for (Map.Entry<DomCardName, ArrayList<DomCard>> entry : list) {
			DomCardName cardName = entry.getKey();
			if (board.isFromSeparatePile(cardName))
				continue;
			DomCost theCost = determineCostAndCheckSplitPiles(cardName);
			if (theCost == null)
				continue;
			if (getTotalAvailableCurrency().compareButIgnoreDebtTo(theCost) < 0)
				continue;
			if (cardName == DomCardName.Curse || cardName == DomCardName.Copper)
				continue;

			if (cardName.hasCardType(DomCardType.Event)) {
				if (!wantsEvent(cardName))
					continue;

				// DomGame nodeGame = (DomGame) DeepCopy.copy(getCurrentGame());

				// NodeGame node = new NodeGame(getCurrentGame(), cardName);
				// eval = alphabeta(card);
				// if eval > best_eval
				// bestcard = card
			}

		}

		if (currentPly == 0) {
			if (bestcard != null)
				resolveEvent(bestcard); // <---This finally buys whatever card
			if (debt > 0) {
				payOffDebt();
			}
			return;
		}

		// something about extramission here, check DomPlayer.java

		Iterator<DomCardName> enumKeySet = board.keySet().iterator();

		while (enumKeySet.hasNext()) {
			DomCardName currentState = enumKeySet.next();
			if (!board.get(currentState).isEmpty())
				DomEngine.addToLog("key : " + currentState + " value : " + board.get(currentState));
			else
				DomEngine.addToLog("One pile left!");
		}

		DomEngine.addToLog(name + " buys NOTHING!");

		// a bit dirty setting buysLeft to 0 to make him stop trying to buy stuff and
		// say 'buys nothing'
		// TODO maybe clean this up
		buysLeft = 0;
		
		DomGame nodeGame = (DomGame) DeepCopy.copy(getCurrentGame());
	}
}