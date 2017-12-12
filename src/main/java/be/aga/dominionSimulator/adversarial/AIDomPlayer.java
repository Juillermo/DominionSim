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
import be.aga.dominionSimulator.enums.DomPhase;
import be.aga.dominionSimulator.adversarial.DeepCopy;
import be.aga.dominionSimulator.cards.DonateCard;
import be.aga.dominionSimulator.cards.Mountain_PassCard;

/**
 * Represents the AI search bot in a simulated game.
 */
public class AIDomPlayer extends DomPlayer {
	private int currentPly;
	private boolean realPlayer;
	private boolean goodGuy;

	public AIDomPlayer(String aString, int ply) {
		super(aString);
		currentPly = ply;
		realPlayer = true;
		goodGuy = true;
	}

	@Override
	public void makeBuyDecision() {
		DomBoard board = getCurrentGame().getBoard();
		DomCardName bestCard = null;
		double bestEval = 0.0;

		// Randomize cards to buy
		// TODO Sort them by ranking of how good they are
		List<Map.Entry<DomCardName, ArrayList<DomCard>>> list = new ArrayList<Map.Entry<DomCardName, ArrayList<DomCard>>>(
				board.entrySet());
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
			}

			if (!hasExtraMissionTurn()) {
				if (game.countInSupply(cardName) == 0) {
					// if (DomEngine.haveToLog) DomEngine.addToLog( aCardName +
					// " is no more available to buy");
					continue;
				}
				if (suicideIfBuys(cardName)) {
					if (DomEngine.haveToLog)
						DomEngine.addToLog("<FONT style=\"BACKGROUND-COLOR: red\">SUICIDE!</FONT> Can not buy "
								+ cardName.toHTML());
					continue;
				}
				if (forbiddenCardsToBuy.contains(cardName))
					continue;

				if (cardName == DomCardName.Grand_Market && !getCardsFromPlay(DomCardName.Copper).isEmpty())
					continue;

				if (!isHumanOrPossessedByHuman() && coinTokens > 0
						&& getDesiredCard(getAvailableCurrencyWithoutTokens(), false) != cardName
						&& checkIfWantsToHoardCoinTokens() && !wants(DomCardName.Gardens)) {
					continue;
				}

				double eval = alphaBeta(cardName);
				if (eval > bestEval)
					bestCard = cardName;
			}

		}

		if (currentPly == 0) {
			if (bestCard != null) {
				continueBuyDecision(bestCard);
			}
		} else {
			// return bestEval;
		}

		DomEngine.addToLog(name + " buys NOTHING!");

		// a bit dirty setting buysLeft to 0 to make him stop trying to buy
		// stuff and
		// say 'buys nothing'
		// TODO maybe clean this up
		buysLeft = 0;
	}

	private void continueBuyDecision(DomCardName aCard) {
		if (aCard.hasCardType(DomCardType.Event)) {

			resolveEvent(aCard);

			if (debt > 0)
				payOffDebt();
			return;

		} else {

			buy(game.takeFromSupply(aCard));

			coinTokensToAdd += getCardsFromPlay(DomCardName.Merchant_Guild).size();
			return;
		}
	}

	private double alphaBeta(DomCardName buyingCard) {
		AIDomGame nodeGame = (AIDomGame) DeepCopy.copy(getCurrentGame());

		ArrayList<DomPlayer> originalPlayers = nodeGame.getPlayers();
		ArrayList<AIDomPlayer> nodePlayers = new ArrayList<AIDomPlayer>();
		AIDomPlayer father = (AIDomPlayer) nodeGame.getActivePlayer();

		for (DomPlayer player : originalPlayers) {
			
			AIDomPlayer robotizedPlayer = (AIDomPlayer) player;
			
			if (robotizedPlayer != father)
				if (currentPly == 0){
					robotizedPlayer.setNotHuman();					
					robotizedPlayer.setBadGuy();
				}
			
			robotizedPlayer.increasePly();
			nodePlayers.add(robotizedPlayer);
		}
		
		nodeGame.setPlayers(nodePlayers);

		// Continue playing with the father using continue buy
		// Need to change from human game to simulated game??

		return 0.0;
	}

	@Override
	public void takeTurn() {
		// for(DomCard theCard : getDeck().getAllCards()) {
		// if (theCard.owner==null) {
		// System.out.println("Error, cards in deck with null owner "+ theCard);

		// System.out.println(getDeck());
		// }
		// }
		initializeTurn();
		handleTeachers();
		handleGuides();
		resolveHorseTraders();
		resolveDurationEffects();
		resolveCardsToSummon();
		resolvePrincedCards();
		resolveRatcatchers();
		handleTransmogrify();
		handleDelayedBoons();
		doActionPhase();
		doBuyPhase();
		continueTurn();
	}

	public void continueTurn() {
		continueBuyPhase();
		doNightPhase();
		doCleanUpPhase();

		// actually this is not part of the turn so we set Possessor to null
		possessor = null;
		if (donateTriggered)
			DonateCard.trashStuff(this);
		if (getCurrentGame().isAuctionTriggered()) {
			Mountain_PassCard.doTheAuction(this);
			getCurrentGame().setAuctionTriggered(false);
		}
		// TODO moved from buy phase to here... ok?
		updateVPCurve(false);
		// TODO needed fixing
		actionsLeft = 1;
		getCurrentGame().setPreviousTurnTakenBy(this);
	}

	private void doBuyPhase() {
		long theTime = System.currentTimeMillis();
		setPhase(DomPhase.Buy);
		if (getCurrentGame().getBoard().isLandmarkActive(DomCardName.Arena)
				&& !getCardsFromHand(DomCardType.Action).isEmpty()) {
			if (!getCardsFromHand(DomCardType.Action).get(0).hasCardType(DomCardType.Treasure)) {
				discard(removeCardFromHand(getCardsFromHand(DomCardType.Action).get(0)));
				int theVP = getCurrentGame().getBoard().removeVPFrom(DomCardName.Arena, 2);
				if (theVP > 0) {
					if (DomEngine.haveToLog)
						DomEngine.addToLog(this + " takes VP from " + DomCardName.Arena.toHTML());
					addVP(theVP);
				}
			}
		}
		if (!isInBuyRules(DomCardName.Alms) || getTotalPotentialCurrency().compareTo(new DomCost(4, 0)) > 0)
			playTreasures();

		if (DomEngine.haveToLog) {
			if (previousPlayedCardName != null) {
				DomEngine.addToLog(name + " plays " + (sameCardCount + 1) + " " + previousPlayedCardName.toHTML()
						+ (sameCardCount > 0 ? "s" : ""));
				previousPlayedCardName = null;
				sameCardCount = 0;
			}
			showBuyStatus();
		}
		coinTokensToAdd = 0;
		updateMoneyCurve();

		while (buysLeft > 0) {
			if (debt > 0)
				payOffDebt();
			if (debt > 0) {
				if (DomEngine.haveToLog)
					DomEngine.addToLog(name + " has $" + debt + " in debt left so can't buy cards or events");
				break;
			}
			makeBuyDecision();
			continueBuyPhase();
			buysLeft--;
			if (isVillaTriggered()) {
				setVillaTriggered(false);
				if (DomEngine.haveToLog)
					DomEngine.addToLog(
							name + " triggers " + DomCardName.Villa.toHTML() + " and moves back to the action phase");
				doActionPhase();
				doBuyPhase();
			}
		}
	}

	private void continueBuyPhase() {
		buysLeft--;
		while (buysLeft > 0) {
			if (debt > 0)
				payOffDebt();
			if (debt > 0) {
				if (DomEngine.haveToLog)
					DomEngine.addToLog(name + " has $" + debt + " in debt left so can't buy cards or events");
				break;
			}
			makeBuyDecision();
			buysLeft--;
			if (isVillaTriggered()) {
				setVillaTriggered(false);
				if (DomEngine.haveToLog)
					DomEngine.addToLog(
							name + " triggers " + DomCardName.Villa.toHTML() + " and moves back to the action phase");
				doActionPhase();
				doBuyPhase();
			}
		}
		if (coinTokensToAdd > 0) {
			addCoinTokens(coinTokensToAdd);
			coinTokensToAdd = 0;
		}
		handleWineMerchants();
		buyTime += System.currentTimeMillis() - theTime;
	}

	private void increasePly(){
		currentPly++;
	}
	
	private void displayBoard() {
		DomBoard board = getCurrentGame().getBoard();
		Iterator<DomCardName> enumKeySet = board.keySet().iterator();

		while (enumKeySet.hasNext()) {
			DomCardName currentState = enumKeySet.next();
			if (!board.get(currentState).isEmpty())
				DomEngine.addToLog("key : " + currentState + " value : " + board.get(currentState));
			else
				DomEngine.addToLog("One pile left!");
		}
	}

	private void setNotHuman() {
		isHuman = false;
	}

	private boolean getNature() {
		return goodGuy;
	}

	private void setBadGuy() {
		goodGuy = false;
	}
}