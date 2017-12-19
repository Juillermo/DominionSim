package be.aga.dominionSimulator.adversarial;

import java.util.ArrayList;
import java.io.Serializable;

import be.aga.dominionSimulator.DomBoard;
import be.aga.dominionSimulator.DomCost;
import be.aga.dominionSimulator.DomEngine;
import be.aga.dominionSimulator.DomGame;
import be.aga.dominionSimulator.DomPlayer;
import be.aga.dominionSimulator.enums.DomCardType;

public class AIDomGame extends DomGame {

	private AIDomPlayer goodGuy = null;

	public AIDomGame(DomBoard aBoard, ArrayList<DomPlayer> aPlayers, DomEngine anEngine) {
		super(aBoard, aPlayers, anEngine);
	}

	/**
	 * Copy constructor
	 * 
	 * @throws Exception
	 */
	public AIDomGame(DomGame source) {
		super(source);

		for (DomPlayer player : players) {
			AIDomPlayer robotizedPlayer = (AIDomPlayer) player;
			if (robotizedPlayer.getNature()) {
				goodGuy = robotizedPlayer;
				break;
			}
		}

		if (goodGuy == null)
			try {
				throw new Exception("Good guy not found. Everybody is evil in this world!");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	public double continueAIGame() {
		double eval = 17.0;
		if (!isGameFinished()) {
			setChanged();
			notifyObservers();
			activePlayer.setPossessor(activePlayer.removePossessorTurn());
			if (activePlayer.equals(players.get(0))) {
				DomEngine.logPlayerIndentation = 0;
			}
			while (activePlayer.getPossessor() != null && !isGameFinished()) {
				eval = activePlayer.takeTurn();
				activePlayer.setPossessor(activePlayer.removePossessorTurn());
				return eval;
			}
			eval = activePlayer.takeTurn();
		} else {
			// AIDomPlayer winner = (AIDomPlayer) determineWinners();
			// if (winner.getNature())
			// return 0.7;
			AIDomPlayer robotizedPlayer = (AIDomPlayer) activePlayer;
			robotizedPlayer.setDecisionTree(new ArrayList<String>());
			eval = computeHeuristics(); // TODO special case in heuristics if winning?
		}
		return eval;
	}

	public double continueContinuedAIGame() {
		if (activePlayer.hasExtraOutpostTurn() && !isGameFinished()) {
			return activePlayer.takeTurn();
		}
		if (activePlayer.hasExtraMissionTurn() && !isGameFinished()) {
			return activePlayer.takeTurn();
		}
		getEngine().getGameFrame().hover("<html>Opponent gained: " + activePlayer.getGainedCardsText() + "</html>");
		activePlayer = activePlayer.getOpponents().get(0);
		if (!activePlayer.getPossessionTurns().isEmpty()) {
			activePlayer.setPossessor(activePlayer.removePossessorTurn());
		}
		AIDomPlayer robotizedPlayer = (AIDomPlayer) activePlayer;
		//DomEngine.logPlayerIndentation = robotizedPlayer.getCurrentPly();
		return continueAIGame();
	}

	public void setPlayers(ArrayList<DomPlayer> newPlayers) {
		players = newPlayers;
	}

	public void setActivePlayer(AIDomPlayer newPlayer) {
		activePlayer = newPlayer;
	}

	public double computeHeuristics() {
		int theNumber = players.size() < 3 ? 8 : 12;
		double gameProgress = ((double) board.getGainsNeededToEndGame()) / theNumber;
		
//		double deckSize = goodGuy.getDeck().countAllCards(); // Good
//		double moneyInDeck = goodGuy.getTotalMoneyInDeck(); // Regu (needs to revise if the cards have it implemented
//		double perro = goodGuy.getDeckSize(); // BAD (is for remaining deck)
//		DomCost cato = goodGuy.getTotalPotentialCurrency(); // Regu
		
		double goodguyPurchasingHealth = ((double) goodGuy.getTotalMoneyInDeck()) / goodGuy.getDeck().countAllCards();
//		double goodguyVictoryBloat = ((double) goodGuy.getDeck().count(DomCardType.Victory)) / goodGuy.getDeck().countAllCards();
		double goodguyVictoryPoints = goodGuy.countVictoryPoints();
		
		double bestBadguyPurchasingHealth = 0;
//		double bestBadguyVictoryBloat = 1;
		double bestBadguyVictoryPoints = 0;
		for (DomPlayer player : players) {
			if (player != goodGuy) {
				double badguyPurchasingHealth = ((double) player.getDeck().getTotalMoney()) / player.getDeck().countAllCards();
//				double badguyVictoryBloat = ((double) player.getDeck().count(DomCardType.Victory)) / player.getDeck().countAllCards();
				double badguyVictoryPoints = player.countVictoryPoints();
				
				if (badguyPurchasingHealth >= bestBadguyPurchasingHealth)
					bestBadguyPurchasingHealth = badguyPurchasingHealth;
//				if (badguyVictoryBloat <= bestBadguyVictoryBloat)
//					bestBadguyVictoryBloat = badguyVictoryBloat;
				if (badguyVictoryPoints >= bestBadguyVictoryPoints)
					bestBadguyVictoryPoints = badguyVictoryPoints;
			}
		}
		
		double PurchasingHealthScore = goodguyPurchasingHealth / bestBadguyPurchasingHealth;
//		double VictoryBloat = bestBadguyVictoryBloat / goodguyVictoryBloat;
		double VictoryPointsScore = goodguyVictoryPoints / bestBadguyVictoryPoints;
		return gameProgress*PurchasingHealthScore + (1-gameProgress)*VictoryPointsScore;
	}

}