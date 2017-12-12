package be.aga.dominionSimulator.adversarial;

import java.util.ArrayList;
import java.util.Observable;

import be.aga.dominionSimulator.DomBoard;
import be.aga.dominionSimulator.DomCost;
import be.aga.dominionSimulator.DomEngine;
import be.aga.dominionSimulator.DomGame;
import be.aga.dominionSimulator.DomPlayer;
import be.aga.dominionSimulator.cards.*;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import be.aga.dominionSimulator.enums.DomCardName;
import be.aga.dominionSimulator.enums.DomCardType;
import be.aga.dominionSimulator.enums.DomPhase;

public class AIDomGame extends DomGame {

	public AIDomGame(DomBoard aBoard, ArrayList<DomPlayer> aPlayers, DomEngine anEngine) {
		super(aBoard, aPlayers, anEngine);
		// TODO Auto-generated constructor stub
	}
	
	public void continueHumanGame() {
		if (!isGameFinished()) {
			setChanged();
			notifyObservers();
			if (activePlayer.isHuman() && !activePlayer.hasExtraOutpostTurn() && !activePlayer.hasExtraMissionTurn()) {
				activePlayer = activePlayer.getOpponents().get(0);
				DomEngine.logPlayerIndentation++;
			}
			activePlayer.setPossessor(activePlayer.removePossessorTurn());
			while (!isGameFinished() && (!activePlayer.isHumanOrPossessedByHuman()
					|| (activePlayer.isHuman() && activePlayer.getPossessor() != null))) {
				if (activePlayer.equals(players.get(0))) {
					DomEngine.logPlayerIndentation = 0;
				}
				while (activePlayer.getPossessor() != null && !isGameFinished()) {
					activePlayer.takeTurn();
					activePlayer.setPossessor(activePlayer.removePossessorTurn());
				}
				if (!activePlayer.isHuman()) {
					activePlayer.takeTurn();
					if (activePlayer.hasExtraOutpostTurn() && !isGameFinished()) {
						activePlayer.takeTurn();
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
				}
			}
			if (!isGameFinished()) {
				if (activePlayer.equals(players.get(0))) {
					DomEngine.logPlayerIndentation = 0;
				}
				initHumanOrPossessedPlayer();
			} else {
				myEngine.doEndOfHumanGameStuff();
			}
		} else {
			myEngine.doEndOfHumanGameStuff();
		}
		setChanged();
		notifyObservers();
	}
	
	private void setPlayers(ArrayList<AIDomPlayer> newPlayers){
		players = newPlayers;
	}

}