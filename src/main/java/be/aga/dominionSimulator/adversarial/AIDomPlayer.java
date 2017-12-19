package be.aga.dominionSimulator.adversarial;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import be.aga.dominionSimulator.*;
import be.aga.dominionSimulator.enums.DomCardName;
import be.aga.dominionSimulator.enums.DomCardType;
import be.aga.dominionSimulator.enums.DomPhase;
import be.aga.dominionSimulator.cards.DonateCard;
import be.aga.dominionSimulator.cards.Mountain_PassCard;

/**
 * Represents the AI search bot in a simulated game.
 */
public class AIDomPlayer extends DomPlayer {
	private int currentPly = 0;
	private static final int maxPly = 4; // Not including number of players
	private boolean goodGuy = true;
	private ArrayList<String> decisionTree = new ArrayList<String>();

	public AIDomPlayer(String aString) {
		super(aString);
	}

	/**
	 * Copy constructor
	 */
	public AIDomPlayer(DomPlayer source, DomGame newGame, DomEngine newEngine) {
		super(source, newGame, newEngine);
		if (!(source instanceof AIDomPlayer)) {
			currentPly = 1;
			goodGuy = false;
			setNotHuman();
		} else {
			AIDomPlayer AIsource = (AIDomPlayer) source;
			currentPly = AIsource.currentPly + 1;
			goodGuy = AIsource.goodGuy;
		}
	}

	@Override
	public double makeBuyDecision() {
		String intro = new String(new char[currentPly]).replace("\0", "x ");
		if (currentPly == 0)
			DomEngine.addToLog(intro + name + " at ply " + currentPly + ".");
		System.out.println(intro + name + " at ply " + currentPly + ".");

		DomBoard board = getCurrentGame().getBoard();
		DomCardName bestCard = null;
		double bestEval = 0.0;
		double worstEval = 100;
		ArrayList<String> bestDecisionTree = new ArrayList<String>();

		// Randomize cards to buy
		// TODO Sort them by ranking of how good they are
		List<Map.Entry<DomCardName, ArrayList<DomCard>>> list = new ArrayList<Map.Entry<DomCardName, ArrayList<DomCard>>>(
				board.entrySet());
		Collections.shuffle(list);
		for (Map.Entry<DomCardName, ArrayList<DomCard>> entry : list) {
			DomCardName cardName = entry.getKey();

			if (checkWhetherBuyable(cardName)) {
				System.out.println(intro + name + currentPly + " evaluates buying " + cardName);
				if (currentPly == 0)
					DomEngine.addToLog(intro + name + currentPly + " evaluates buying " + cardName);

				DomEngine.haveToLog = false;
				ArrayList<String> decisionTree = new ArrayList<String>();
				double eval = alphaBeta(cardName, decisionTree);
				if (currentPly == 0)
					DomEngine.haveToLog = true;
				System.out.println(intro + "Which scores " + eval + " with " + decisionTree);
				if (currentPly == 0)
					DomEngine.addToLog(intro + "Which scores " + eval + " with " + decisionTree);
				if (goodGuy && eval > bestEval) {
					bestEval = eval;
					bestCard = cardName;
					bestDecisionTree = decisionTree;
				} else if (!goodGuy && eval < worstEval) {
					worstEval = eval;
					bestCard = cardName;
					bestDecisionTree = decisionTree;
				}
			}
		}

		// Not buying any card
		if (currentPly == 0)
			DomEngine.addToLog(intro + name + currentPly + " evaluates not buying anything");
		System.out.println(intro + name + currentPly + " evaluates not buying anything");
		DomEngine.haveToLog = false;
		ArrayList<String> decisionTree = new ArrayList<String>();
		double eval = alphaBeta(null, decisionTree);
		if (currentPly == 0)
			DomEngine.haveToLog = true;
		if (currentPly == 0)
			DomEngine.addToLog(intro + "Which scores " + eval + " with " + decisionTree);
		System.out.println(intro + "Which scores " + eval + " with " + decisionTree);
		if (goodGuy && eval > bestEval) {
			bestEval = eval;
			bestCard = null;
			bestDecisionTree = decisionTree;
		} else if (!goodGuy && eval < worstEval) {
			worstEval = eval;
			bestCard = null;
			bestDecisionTree = decisionTree;
		}

		if (currentPly == 0) {
			if (bestCard != null) {
				DomEngine.addToLog(intro + name + currentPly + " decides to buy " + bestCard + ".");
				continueBuyDecision(bestCard);
				return 0.0;
			}
		} else {
			// if (bestCard != null)
			// DomEngine.addToLog(intro + name + currentPly + " decides to buy " + bestCard
			// + " in the sub-branch.");
			// else
			// DomEngine.addToLog(intro + name + currentPly + " decides not to buy anything
			// in the sub-branch.");
			this.setDecisionTree(bestDecisionTree);
			if (goodGuy) {
				return bestEval;
			} else
				return worstEval;
		}

		if (currentPly == 0)
			DomEngine.addToLog(intro + name + currentPly + " decides to buy NOTHING!");

		// a bit dirty setting buysLeft to 0 to make him stop trying to buy
		// stuff and
		// say 'buys nothing'
		// TODO maybe clean this up
		buysLeft = 0;
		return 0.0;
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

	private double doBuyPhase() {
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
			double bestEval = makeBuyDecision();
			if (currentPly == 0) {
				continueBuyPhase(theTime);
				return 0.0;
			} else {
				return bestEval;
			}
		}
		return -17;
	}

	private double continueBuyPhase(long theTime) {
		buysLeft--;
		if (isVillaTriggered()) {
			setVillaTriggered(false);
			if (DomEngine.haveToLog)
				DomEngine.addToLog(
						name + " triggers " + DomCardName.Villa.toHTML() + " and moves back to the action phase");
			doActionPhase();
			double bestEval = doBuyPhase();
			if (currentPly != 0)
				return bestEval;
		}
		while (buysLeft > 0) {
			if (debt > 0)
				payOffDebt();
			if (debt > 0) {
				if (DomEngine.haveToLog)
					DomEngine.addToLog(name + " has $" + debt + " in debt left so can't buy cards or events");
				break;
			}
			double bestEval = makeBuyDecision();
			if (currentPly == 0) {
				continueBuyPhase(theTime);
				return 0.0;
			} else {
				return bestEval;
			}
		}
		if (coinTokensToAdd > 0) {
			addCoinTokens(coinTokensToAdd);
			coinTokensToAdd = 0;
		}
		handleWineMerchants();
		buyTime += System.currentTimeMillis() - theTime;
		return 0.0;
	}

	@Override
	public double takeTurn() {
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
		double bestEval = doBuyPhase();
		if (currentPly == 0) {
			continueTurn();
			return 0.0;
		} else {
			return bestEval;
		}
	}

	public void continueTurn() {
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

	private double alphaBeta(DomCardName buyingCard, ArrayList<String> comingDecisionTree) {
		AIDomGame nodeGame = new AIDomGame(getCurrentGame());

		ArrayList<DomPlayer> originalPlayers = nodeGame.getPlayers();
		ArrayList<DomPlayer> nodePlayers = new ArrayList<DomPlayer>();
		AIDomPlayer father = (AIDomPlayer) nodeGame.getActivePlayer();

		for (DomPlayer player : originalPlayers) {

			AIDomPlayer robotizedPlayer = (AIDomPlayer) player;

			if (robotizedPlayer != father)
				if (currentPly == 0) {
					robotizedPlayer.setBadGuy();
				}

			robotizedPlayer.decisionTree.addAll(this.getDecisionTree());
			if (buyingCard != null) {
				robotizedPlayer.decisionTree.add(buyingCard.toString() + father.getCurrentPly());
			} else {
				robotizedPlayer.decisionTree.add("Nothing" + father.getCurrentPly());
			}
			nodePlayers.add(robotizedPlayer);
		}

		nodeGame.setPlayers(nodePlayers);

		if (buyingCard != null)
			father.continueBuyDecision(buyingCard);
		father.continueBuyPhase(System.currentTimeMillis());
		father.continueTurn();

		double eval;
		if (father.currentPly != maxPly) {
			eval = nodeGame.continueContinuedAIGame();
			
			AIDomPlayer robotizedPlayer = (AIDomPlayer) nodeGame.getActivePlayer();
			comingDecisionTree.addAll(robotizedPlayer.getDecisionTree());
		} else {
			
			eval = nodeGame.computeHeuristics();
			comingDecisionTree.addAll(father.getDecisionTree());
		}

		return eval;
	}

	private void increasePly() {
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

		System.out.println(board);
	}

	private void setNotHuman() {
		isHuman = false;
	}

	public boolean getNature() {
		return goodGuy;
	}

	private void setBadGuy() {
		goodGuy = false;
	}

	private boolean checkWhetherBuyable(DomCardName cardName) {
		DomBoard board = getCurrentGame().getBoard();

		if (board.isFromSeparatePile(cardName))
			return false;
		DomCost theCost = determineCostAndCheckSplitPiles(cardName);
		if (theCost == null)
			return false;
		if (getTotalAvailableCurrency().compareButIgnoreDebtTo(theCost) < 0)
			return false;
		if (cardName == DomCardName.Curse || cardName == DomCardName.Copper)
			return false;

		if (cardName.hasCardType(DomCardType.Event)) {
			if (!wantsEvent(cardName))
				return false;
		}

		if (!hasExtraMissionTurn()) {
			if (game.countInSupply(cardName) == 0) {
				if (DomEngine.haveToLog)
					DomEngine.addToLog(cardName + " is no more available to buy");
				return false;
			}
			if (suicideIfBuys(cardName)) {
				if (DomEngine.haveToLog)
					DomEngine.addToLog(
							"<FONT style=\"BACKGROUND-COLOR: red\">SUICIDE!</FONT> Can not buy " + cardName.toHTML());
				return false;
			}
			if (forbiddenCardsToBuy.contains(cardName))
				return false;

			if (cardName == DomCardName.Grand_Market && !getCardsFromPlay(DomCardName.Copper).isEmpty())
				return false;

			if (!isHumanOrPossessedByHuman() && coinTokens > 0
					&& getDesiredCard(getAvailableCurrencyWithoutTokens(), false) != cardName
					&& checkIfWantsToHoardCoinTokens() && !wants(DomCardName.Gardens)) {
				return false;
			}

			return true;
		}
		return false;
	}

	public int getCurrentPly() {
		return currentPly;
	}

	public void setCurrentPly(int currentPly) {
		this.currentPly = currentPly;
	}

	public ArrayList<String> getDecisionTree() {
		return decisionTree;
	}

	public void setDecisionTree(ArrayList<String> decisionTree) {
		this.decisionTree = decisionTree;
	}
}