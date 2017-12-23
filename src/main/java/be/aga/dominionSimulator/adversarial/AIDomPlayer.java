package be.aga.dominionSimulator.adversarial;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
	private static final int maxPly = 5; // Each drawing process and buying decision by each player adds a ply
	private boolean goodGuy = true;
	private ArrayList<String> decisionTree = new ArrayList<String>();
	private static int timeComplexity;
	private static int outputLevel = 5;

	static final long[] FACTORIALS = new long[] { 1l, 1l, 2l, 6l, 24l, 120l, 720l, 5040l, 40320l, 362880l, 3628800l,
			39916800l, 479001600l, 6227020800l, 87178291200l, 1307674368000l, 20922789888000l, 355687428096000l,
			6402373705728000l, 121645100408832000l, 2432902008176640000l };

	public AIDomPlayer(String aString) {
		super(aString);
	}

	/**
	 * Copy constructor
	 */
	public AIDomPlayer(DomPlayer source, DomGame newGame, DomEngine newEngine) {
		super(source, newGame, newEngine);
		if (!(source instanceof AIDomPlayer)) {
			currentPly = 0;
			setNotHuman();
		} else {
			AIDomPlayer AIsource = (AIDomPlayer) source;
			currentPly = AIsource.currentPly;
			goodGuy = AIsource.goodGuy;
		}
	}

	@Override
	public double makeBuyDecision() {

		output(name + " at ply " + currentPly + " (buying ply), with $" + availableCoins + " to spend.");

		EnumMap<DomCardName, ArrayList<DomCard>> board = getCurrentGame().getBoard().clone();
		DomCardName bestCard = null;
		double bestEval = 0.0;
		double worstEval = 100;
		ArrayList<String> bestDecisionTree = new ArrayList<String>();

		ArrayList<DomCard> noCards = new ArrayList<DomCard>();
		noCards.add(new DomCard(DomCardName.NoCard));
		board.put(DomCardName.NoCard, noCards);
		Set<Map.Entry<DomCardName, ArrayList<DomCard>>> buyOptionsSet = board.entrySet();

		// Randomize cards to buy, for (potentially) more efficient alphaBeta algorithm
		// TODO Sort them by ranking of how good they are (by cost?)
		List<Map.Entry<DomCardName, ArrayList<DomCard>>> buyOptions = new ArrayList<Map.Entry<DomCardName, ArrayList<DomCard>>>(
				buyOptionsSet);
		Collections.shuffle(buyOptions);

		for (Map.Entry<DomCardName, ArrayList<DomCard>> entry : buyOptions) {
			DomCardName cardName = entry.getKey();

			if (checkWhetherBuyable(cardName)) {
				output(name + currentPly + " evaluates buying " + cardName);
				DomEngine.haveToLog = false;

				ArrayList<String> decisionTree = new ArrayList<String>();
				double eval = alphaBeta(cardName, decisionTree);

				if (currentPly == 0)
					DomEngine.haveToLog = true;
				output("Which scores " + eval + " with time complexity " + timeComplexity + " and decision tree "
						+ decisionTree);

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

		output(name + currentPly + " decides to buy " + bestCard + ".");

		if (currentPly == 0) {

			if (bestCard == DomCardName.NoCard)
				buysLeft = 0;
			else
				continueBuyDecision(bestCard);

			return 0.0;

		} else {
			setDecisionTree(bestDecisionTree);

			if (goodGuy)
				return bestEval;
			else
				return worstEval;
		}
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

	private double continueTurn() {
		doNightPhase();
		double eval = doCleanUpPhase();
		if (currentPly == 0) {
			continueContinuingTurn();
			return 0.0;
		} else {
			return eval;
		}
	}

	private void continueContinuingTurn() {
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
		AIDomGame nodeGame = generateNodeGame();
		AIDomPlayer father = nodeGame.getActiveAIPlayer();

		for (DomPlayer player : nodeGame.getPlayers()) {
			AIDomPlayer robotizedPlayer = (AIDomPlayer) player;

			String decision = buyingCard.toString() + father.getCurrentPly();
			if (goodGuy)
				decision = "g" + decision;
			else
				decision = "b" + decision;
			robotizedPlayer.decisionTree.add(decision);
		}

		if (buyingCard == DomCardName.NoCard) {
			father.buysLeft = 0;
		} else
			father.continueBuyDecision(buyingCard);
		father.continueBuyPhase(System.currentTimeMillis());

		double eval;
		if (father.currentPly < maxPly) {
			eval = father.continueTurn();
			comingDecisionTree.addAll(nodeGame.getActiveAIPlayer().getDecisionTree());
		} else {
			eval = nodeGame.computeHeuristics();
			comingDecisionTree.addAll(father.getDecisionTree());
		}

		return eval;
	}

	/**
	 * @param nCardsToDraw
	 */
	public double drawCards(int nCardsToDraw) {
		if (minusOneCardToken) {
			nCardsToDraw--;
			minusOneCardToken = false;
		}

		if (currentPly == 0) {
			ArrayList<DomCard> theDrawnCards = deck.getTopCards(nCardsToDraw);
			cardsInHand.addAll(theDrawnCards);

			output(this + " draws " + theDrawnCards.size() + " cards");
			showHand();
			return 0.0;
		} else {

			if (deck.getDrawDeck().size() < nCardsToDraw)
				if (!handleShuffling(nCardsToDraw)) {
					continueCleanUpPhase();
					continueContinuingTurn();
					return getCurrentAIGame().continueContinuedAIGame();
				}

			output(name + " at ply " + currentPly + " (drawing ply).");

			if (currentPly + 3 < maxPly) {
				setKnownTopCards(getKnownTopCards() - nCardsToDraw);
				EnumMap<DomCardName, Integer> drawDeckMap = arraylistToEnumset(deck.getDrawDeck());
				Set<EnumMap<DomCardName, Integer>> drawingCombinations = getDrawingCombinations(nCardsToDraw,
						drawDeckMap);

				double aggEval = 0;
				for (EnumMap<DomCardName, Integer> hand : drawingCombinations) {

					double probability = 1.0 / binomialCoefficient(deck.getDrawDeckSize(), nCardsToDraw);

					for (DomCardName cardName : hand.keySet())
						probability *= binomialCoefficient(drawDeckMap.get(cardName), hand.get(cardName));

					AIDomGame nodeGame = generateNodeGame();
					AIDomPlayer father = nodeGame.getActiveAIPlayer();

					output(this + " draws " + hand + ", with probability " + probability);

					father.getHandSetFromDeck(hand);
					father.continueCleanUpPhase();
					father.continueContinuingTurn();

					aggEval += probability * nodeGame.continueContinuedAIGame();

					output("Which scores " + aggEval);
				}

				return aggEval;

			} else if (currentPly + 1 < maxPly) {
				// The cards drawn here will not be used, since there won't be another buying
				// phase for this playeroutput("The cards here won't be used");
				for (DomPlayer player : getCurrentGame().getPlayers()) {
					AIDomPlayer robotizedPlayer = (AIDomPlayer) player;
					robotizedPlayer.increasePly();
				}
				return getCurrentAIGame().continueContinuedAIGame();
			} else {
				// The cards drawn here will not be used, since there won't be another buying
				// phase for this player
				output("Reached last ply = " + (currentPly + 1));
				return getCurrentAIGame().computeHeuristics();
			}

		}
	}

	@Override
	protected double drawHandForNextTurn() {
		for (DomCard theCard : cardsInPlay)
			if (theCard.getName() == DomCardName.Outpost)
				return drawCards(3);

		return drawCards(5);
		// TODO: Handle expeditions and rivers gift
		// for (int i=0;i<expeditionsActivated;i++)
		// drawCards(2);
		// if(river$sGiftActive)
		// drawCards(1);
	}

	@Override
	protected double doCleanUpPhase() {
		setPhase(DomPhase.CleanUp);
		while (!boons.isEmpty())
			getCurrentGame().getBoard().returnBoon(boons.remove(0));

		for (DomCard theEncampment : mySetAsideEncampments)
			returnToSupply(theEncampment);

		mySetAsideEncampments.clear();
		cardsToStayInPlay.clear();
		handleHerbalists();
		handleSchemes();
		discardAll();
		discard(deck.getPutAsideCards());

		double eval = drawHandForNextTurn();

		if (currentPly == 0) {
			continueCleanUpPhase();
			return 0.0;
		} else
			return eval;
	}

	private void continueCleanUpPhase() {
		if (savedCard != null)
			cardsInHand.add(savedCard);
		savedCard = null;
		setPhase(null);
		// reset variables needed for total money checking in other player's turns
		availableCoins = 0;
		availablePotions = 0;
		if (getCardsGainedLastTurn().isEmpty() && getCurrentGame().getBoard().isLandmarkActive(DomCardName.Baths)) {
			int theVP = getCurrentGame().getBoard().removeVPFrom(DomCardName.Baths, 2);
			if (theVP > 0) {
				if (DomEngine.haveToLog)
					DomEngine.addToLog(this + " takes VP from " + DomCardName.Baths.toHTML());
				addVP(theVP);
			}
		}
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
			if (game.countInSupply(cardName) == 0 && cardName != DomCardName.NoCard) {
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

	private AIDomGame generateNodeGame() {
		AIDomGame nodeGame = new AIDomGame(getCurrentGame());

		ArrayList<DomPlayer> originalPlayers = nodeGame.getPlayers();
		ArrayList<DomPlayer> nodePlayers = new ArrayList<DomPlayer>();
		AIDomPlayer father = (AIDomPlayer) nodeGame.getActivePlayer();

		for (DomPlayer player : originalPlayers) {

			AIDomPlayer robotizedPlayer = (AIDomPlayer) player;

			if (robotizedPlayer != father)
				if (currentPly == 0) {
					robotizedPlayer.setBadGuy();
					timeComplexity = 0;
				}

			robotizedPlayer.decisionTree.addAll(this.getDecisionTree());
			robotizedPlayer.increasePly();
			nodePlayers.add(robotizedPlayer);
		}
		nodeGame.setPlayers(nodePlayers);

		timeComplexity++;
		return nodeGame;
	}

	private void output(String aString) {
		String intro = new String(new char[currentPly]).replace("\0", "x ");
		if (currentPly <= outputLevel)
			System.out.println(intro + aString);
		if (currentPly == 0)
			DomEngine.addToLog(aString);
	}

	private void getHandSetFromDeck(EnumMap<DomCardName, Integer> hand) {
		for (DomCardName aCardName : hand.keySet()) {
			int amount = hand.get(aCardName);
			for (DomCard aCard : deck.getDrawDeck())
				if (aCard.getName() == aCardName) {
					cardsInHand.add(aCard);
					amount--;
					if (amount == 0)
						break;
				}
			for (DomCard aCard : cardsInHand)
				deck.getDrawDeck().remove(aCard);
			if (amount != 0)
				try {
					throw new Exception("DrawCardsException: Some drawing cards not found");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		showHand();
	}

	private Set<EnumMap<DomCardName, Integer>> getDrawingCombinations(int aI, EnumMap<DomCardName, Integer> diffCards) {
		output("Different cards in deck: " + diffCards);

		Combination combinator = new Combination(diffCards, aI);
		Set<EnumMap<DomCardName, Integer>> combinationsSet = combinator.combinations();

		output(combinationsSet.size() + " possible combinations to draw: " + combinationsSet);
		output("Number of branches = " + combinationsSet.size());
		return combinationsSet;
	}

	private static final EnumMap<DomCardName, Integer> arraylistToEnumset(ArrayList<DomCard> arrayList) {
		EnumMap<DomCardName, Integer> diffCards = new EnumMap<DomCardName, Integer>(DomCardName.class);
		for (DomCard card : arrayList)
			if (!diffCards.containsKey(card.getName()))
				diffCards.put(card.getName(), 1);
			else
				diffCards.put(card.getName(), diffCards.get(card.getName()) + 1);
		return diffCards;
	}

	private boolean handleShuffling(int nCardsToDraw) {
		ArrayList<DomCard> drawDeck = deck.getDrawDeck();
		setKnownTopCards(getKnownTopCards() - drawDeck.size());

		if (drawDeck.size() > 0) {
			for (DomCard aCard : drawDeck)
				cardsInHand.add(aCard);
			for (DomCard aCard : cardsInHand)
				drawDeck.remove(aCard);
		}

		if (deck.getDiscardPile().isEmpty()) {
			return false;
		} else {
			drawDeck.addAll(deck.getDiscardPile());
			deck.getDiscardPile().clear();
			deck.shuffle();
			nCardsToDraw -= drawDeck.size();
		}
		return true;
	}

	private void increasePly() {
		currentPly++;
	}

	public int getCurrentPly() {
		return currentPly;
	}

	public void setCurrentPly(int currentPly) {
		this.currentPly = currentPly;
	}

	public AIDomGame getCurrentAIGame() {
		return (AIDomGame) getCurrentGame();
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

	public ArrayList<String> getDecisionTree() {
		return decisionTree;
	}

	public void setDecisionTree(ArrayList<String> decisionTree) {
		this.decisionTree = decisionTree;
	}

	/**
	 * Returns an exact representation of the
	 * <a href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
	 * Coefficient</a>, "{@code n choose k}", the number of {@code k}-element
	 * subsets that can be selected from an {@code n}-element set.
	 * <p>
	 * <Strong>Preconditions</strong>:
	 * <ul>
	 * <li>{@code 0 <= k <= n } (otherwise {@code MathIllegalArgumentException} is
	 * thrown)</li>
	 * <li>The result is small enough to fit into a {@code long}. The largest value
	 * of {@code n} for which all coefficients are {@code  < Long.MAX_VALUE} is 66.
	 * If the computed value exceeds {@code Long.MAX_VALUE} a
	 * {@code MathArithMeticException} is thrown.</li>
	 * </ul>
	 * </p>
	 *
	 * @param n
	 *            the size of the set
	 * @param k
	 *            the size of the subsets to be counted
	 * @return {@code n choose k}
	 * @throws NotPositiveException
	 *             if {@code n < 0}.
	 * @throws NumberIsTooLargeException
	 *             if {@code k > n}.
	 * @throws MathArithmeticException
	 *             if the result is too large to be represented by a long integer.
	 */
	public static long binomialCoefficient(final int n, final int k) {
		if ((n == k) || (k == 0)) {
			return 1;
		}
		if ((k == 1) || (k == n - 1)) {
			return n;
		}
		// Use symmetry for large k
		if (k > n / 2) {
			return binomialCoefficient(n, n - k);
		}

		// We use the formula
		// (n choose k) = n! / (n-k)! / k!
		// (n choose k) == ((n-k+1)*...*n) / (1*...*k)
		// which could be written
		// (n choose k) == (n-1 choose k-1) * n / k
		long result = 1;
		if (n <= 61) {
			// For n <= 61, the naive implementation cannot overflow.
			int i = n - k + 1;
			for (int j = 1; j <= k; j++) {
				result = result * i / j;
				i++;
			}
		} else {
			try {
				throw new Exception("BinomialException: Number too big");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;
	}
}