package be.aga.dominionSimulator.cards;

import be.aga.dominionSimulator.DomBuyRule;
import be.aga.dominionSimulator.DomCard;
import be.aga.dominionSimulator.DomEngine;
import be.aga.dominionSimulator.DomPlayer;
import be.aga.dominionSimulator.enums.DomCardName;

import java.util.ArrayList;

public class GladiatorCard extends DomCard {
    public GladiatorCard() {
      super( DomCardName.Gladiator);
    }

    public void play() {
      owner.addAvailableCoins(2);
      if (owner.getOpponents().isEmpty() || owner.getCardsInHand().isEmpty()) {
          owner.addAvailableCoins(1);
          DomCard theGladiator = owner.getCurrentGame().takeFromSupply(DomCardName.Gladiator);
          if (theGladiator != null)
              owner.trash(theGladiator);
          return;
      }
      DomCard theChosenCard = owner.getCardsInHand().get(0);
      DomPlayer theLeftOpponent = owner.getOpponents().get(0);
      if (owner.isHumanOrPossessedByHuman()) {
          ArrayList<DomCardName> theChooseFrom = new ArrayList<DomCardName>();
          for (DomCard theCard : owner.getCardsInHand()) {
              theChooseFrom.add(theCard.getName());
          }
          theChosenCard = owner.getCardsFromHand(owner.getEngine().getGameFrame().askToSelectOneCard("Reveal a card", theChooseFrom, "Mandatory!")).get(0);
      } else {
          int theMinCount = theLeftOpponent.countInDeck(theChosenCard.getName());
          for (DomCard theCard : owner.getCardsInHand()) {
              int theCount = theLeftOpponent.countInDeck(theCard.getName());
              if (theCount < theMinCount) {
                  theMinCount = theCount;
                  theChosenCard = theCard;
              }
          }
      }
  	  if (DomEngine.haveToLog) DomEngine.addToLog( owner + " reveals a "+theChosenCard);
      if (theLeftOpponent.getCardsFromHand(theChosenCard.getName()).isEmpty()) {
          if (DomEngine.haveToLog) DomEngine.addToLog(theLeftOpponent + " reveals nothing");
          owner.addAvailableCoins(1);
          DomCard theGladiator = owner.getCurrentGame().takeFromSupply(DomCardName.Gladiator);
          if (theGladiator != null)
              owner.trash(theGladiator);
      } else {
          if (theLeftOpponent.isHuman()) {
              if (!owner.getEngine().getGameFrame().askPlayer("<html>Reveal " + theChosenCard.getName().toHTML() +"?</html>", "Resolving " + this.getName().toString())){
                  if (DomEngine.haveToLog) DomEngine.addToLog(theLeftOpponent + " reveals nothing");
                  owner.addAvailableCoins(1);
                  DomCard theGladiator = owner.getCurrentGame().takeFromSupply(DomCardName.Gladiator);
                  if (theGladiator != null)
                      owner.trash(theGladiator);
              } else {
                  if (DomEngine.haveToLog) DomEngine.addToLog(theLeftOpponent + " also reveals a " + theChosenCard);
              }
          } else {
              if (DomEngine.haveToLog) DomEngine.addToLog(theLeftOpponent + " also reveals a " + theChosenCard);
          }
      }
    }
}