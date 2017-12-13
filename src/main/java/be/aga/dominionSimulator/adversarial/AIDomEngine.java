package be.aga.dominionSimulator.adversarial;

import java.util.ArrayList;
import java.util.Collections;

import be.aga.dominionSimulator.*;
import be.aga.dominionSimulator.gui.AIDomGui;
import be.aga.dominionSimulator.gui.DomGameFrame;

/**
 * Alternative engine for AI simulation
 * <p>
 * Contains the {@code main} method that launches the simulator GUI.
 */
public class AIDomEngine extends DomEngine {
	
	public AIDomEngine() {
		loadSystemBots();
		createSimpleCardStrategiesBots();
		loadCurrentUserBots();
		AIDomGui theGui = new AIDomGui(this);
		myGui = theGui;
		myGui.setVisible(true);
	}

	@Override
	public void startHumanGame(DomPlayer theHumanPlayer, String delay) {
		myLog = new StringBuilder();
		logPlayerIndentation = 0;
		logIndentation = 0;
		ArrayList<DomPlayer> thePlayers = myGui.initPlayers();
		theHumanPlayer.setBuyRules((ArrayList<DomBuyRule>) thePlayers.get(0).getBuyRules().clone());
		theHumanPlayer.setStartState(thePlayers.get(0).getStartState());
		theHumanPlayer.setShelters(thePlayers.get(0).getShelters());
		thePlayers.add(0, theHumanPlayer);
		if (!myGui.getOrderBoxSelected())
			Collections.shuffle(thePlayers);
		emptyPilesEndingCount = 0;
		players.clear();
		players.addAll(thePlayers);
		DomBoard theBoard = null;
		haveToLog = false;
		currentGame = new AIDomGame(theBoard, players, this);
		haveToLog = true;
		setGameFrame(new DomGameFrame(this, delay));
		myGameFrame.setVisible(true);
		currentGame.startUpHumanGame();
	}
	
	public static void main(String[] args) {
		new AIDomEngine();
	}
}