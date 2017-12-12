package be.aga.dominionSimulator.gui;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.AbstractButton;
import javax.swing.JButton;

import be.aga.dominionSimulator.AIDomPlayer;
import be.aga.dominionSimulator.DomEngine;
import be.aga.dominionSimulator.DomPlayer;

public class AIDomGui extends DomGui {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AIDomGui(DomEngine anEngine) {
		super(anEngine);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public ArrayList<DomPlayer> initPlayers() {
		myPlayers = new HashMap<DomPlayer, JButton>();
		ArrayList<DomPlayer> thePlayers = new ArrayList<DomPlayer>();
		for (JButton theSelector : myBotSelectors) {
			if (getSelectedPlayer(theSelector) == null)
				continue;
			DomPlayer thePlayer = new AIDomPlayer("Search-tree bot");
			int j = 0;
			for (Enumeration<AbstractButton> theEnum = myStartStateButtonGroups.get(theSelector).getElements(); theEnum
					.hasMoreElements(); j++) {
				if (theEnum.nextElement().isSelected()) {
					if (j == 1)
						thePlayer.forceStart(43);
					if (j == 2)
						thePlayer.forceStart(52);
				}
			}
			myPlayers.put(thePlayer, theSelector);
			thePlayers.add(thePlayer);
		}
		return thePlayers;
	}
}