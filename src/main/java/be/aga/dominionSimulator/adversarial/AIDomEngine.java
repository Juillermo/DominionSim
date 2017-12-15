package be.aga.dominionSimulator.adversarial;

import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JOptionPane;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

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
		myGui = new AIDomGui(this);
		myGui.setVisible(true);
	}
	
	public AIDomEngine(DomEngine source) {
		super(source);
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
	
	@Override
	public void loadSystemBots() {
		try {
			InputSource src = new InputSource(getClass().getResourceAsStream("DomBots.xml"));
			// InputSource src = new InputSource(new FileInputStream(new File("..."));
			XMLHandler saxHandler = new XMLHandler();
			XMLReader rdr = XMLReaderFactory.createXMLReader();
			rdr.setContentHandler(saxHandler);
			rdr.parse(src);
			bots = saxHandler.getBots();
		} catch (Exception e) {
			bots = new ArrayList<DomPlayer>();
			DomPlayer dumbBot = new AIDomPlayer("Big Money Ultimate");
			bots.add(dumbBot);
			// TODO: Update this message since this requires Java 1.8.
//			JOptionPane.showMessageDialog(myGui,
//					"You'll need to download Java 1.6 at www.java.com to runSimulation this program!!!");
		}
		Collections.sort(bots);
	}
}