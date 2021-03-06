package be.aga.dominionSimulator.cards;

import be.aga.dominionSimulator.DomCard;
import be.aga.dominionSimulator.DomCost;
import be.aga.dominionSimulator.enums.DomCardName;

import java.util.ArrayList;
import java.util.Collections;

public class RemodelCard extends DomCard {
    public RemodelCard () {
      super( DomCardName.Remodel);
    }

    public void play() {
      if (owner.getCardsInHand().isEmpty())
    	return;
      if (owner.isHumanOrPossessedByHuman()) {
          handleHumanPlayer();
      } else {
            DomCard theCardToTrash = owner.findCardToRemodel(this, 2);
            if (theCardToTrash == null) {
                //this is needed when card is played with Throne Room effect or Golem
                Collections.sort(owner.getCardsInHand(), SORT_FOR_TRASHING);
                theCardToTrash = owner.getCardsInHand().get(0);
            }
            owner.trash(owner.removeCardFromHand(theCardToTrash));
            DomCost theMaxCostOfCardToGain = new DomCost(theCardToTrash.getCoinCost(owner.getCurrentGame()) + 2, theCardToTrash.getPotionCost());
            DomCardName theDesiredCard = owner.getDesiredCard(theMaxCostOfCardToGain, false);
            if (theDesiredCard == null)
                theDesiredCard = owner.getCurrentGame().getBestCardInSupplyFor(owner, null, theMaxCostOfCardToGain);
            if (theDesiredCard != null)
                owner.gain(theDesiredCard);
        }
    }

    private void handleHumanPlayer() {
        ArrayList<DomCardName> theChooseFrom = new ArrayList<DomCardName>();
        for (DomCard theCard : owner.getCardsInHand())
            theChooseFrom.add(theCard.getName());
        DomCard theCardToRemodel = owner.getCardsFromHand(owner.getEngine().getGameFrame().askToSelectOneCard("Select card to " + this.getName().toString(), theChooseFrom, "Mandatory!")).get(0);
        owner.trash(owner.removeCardFromHand(theCardToRemodel));
        theChooseFrom = new ArrayList<DomCardName>();
        for (DomCardName theCard : owner.getCurrentGame().getBoard().keySet()) {
            if (theCardToRemodel.getCost(owner.getCurrentGame()).add(new DomCost(2,0)).compareTo(theCard.getCost(owner.getCurrentGame()))>=0
                    && owner.getCurrentGame().countInSupply(theCard)>0)
                theChooseFrom.add(theCard);
        }
        if (theChooseFrom.isEmpty())
            return;
        owner.gain(owner.getEngine().getGameFrame().askToSelectOneCard("Select card to gain from "+this.getName().toString(), theChooseFrom, "Mandatory!"));
    }

    @Override
    public boolean wantsToBePlayed() {
      return owner.findCardToRemodel(this, 2)!=null;
   }
}