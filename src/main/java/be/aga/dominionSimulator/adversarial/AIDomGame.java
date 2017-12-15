package be.aga.dominionSimulator.adversarial;

import java.util.ArrayList;
import java.io.Serializable;

import be.aga.dominionSimulator.DomBoard;
import be.aga.dominionSimulator.DomEngine;
import be.aga.dominionSimulator.DomGame;
import be.aga.dominionSimulator.DomPlayer;

public class AIDomGame extends DomGame{

	public AIDomGame(DomBoard aBoard, ArrayList<DomPlayer> aPlayers, DomEngine anEngine) {
		super(aBoard, aPlayers, anEngine);
		// TODO Auto-generated constructor stub
	}
	
	/**
     * Copy constructor
     */
    public AIDomGame(DomGame source) {
    	super(source);
    }
	
	public double continueAIGame() {
		double eval = 17.0;
		if (!isGameFinished()) {
			setChanged();
			notifyObservers();
			activePlayer.setPossessor(activePlayer.removePossessorTurn());
			while (!isGameFinished()) {
				if (activePlayer.equals(players.get(0))) {
					DomEngine.logPlayerIndentation = 0;
				}
				while (activePlayer.getPossessor() != null && !isGameFinished()) {
					activePlayer.takeTurn();
					activePlayer.setPossessor(activePlayer.removePossessorTurn());
				}
				activePlayer.takeTurn();
				eval = continueContinuedAIGame();
			}
		} else {
//			AIDomPlayer winner = (AIDomPlayer) determineWinners();
//			if (winner.getNature())
//				return 0.7;
			return computeHeuristics(); //TODO special case if winning?
		}
		return eval;
	}
	
	public double continueContinuedAIGame(){
		double eval;
		if (activePlayer.hasExtraOutpostTurn() && !isGameFinished()) {
			eval = activePlayer.takeTurn();
			return eval;
		}
		if (activePlayer.hasExtraMissionTurn() && !isGameFinished()) {
			activePlayer.takeTurn();
		}
		getEngine().getGameFrame()
				.hover("<html>Opponent gained: " + activePlayer.getGainedCardsText() + "</html>");
		activePlayer = activePlayer.getOpponents().get(0);
		if (!activePlayer.getPossessionTurns().isEmpty()) {
			activePlayer.setPossessor(activePlayer.removePossessorTurn());
		}
		DomEngine.logPlayerIndentation++;
		return continueAIGame();
	}
	
	public void setPlayers(ArrayList<DomPlayer> newPlayers){
		players = newPlayers;
	}
	
	public void setActivePlayer(AIDomPlayer newPlayer){
		activePlayer = newPlayer;
	}
	
	public double computeHeuristics(){
		return 555;
	}

}