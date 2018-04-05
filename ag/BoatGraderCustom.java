package nachos.ag;
import nachos.machine.*;
public class BoatGraderCustom {

	int boatSide=0;
	int psg=0;
	int type=0;
	int aO,cO,aM,cM;
    /**
     * BoatGrader consists of functions to be called to show that
     * your solution is properly synchronized. This version simply
     * prints messages to standard out, so that you can watch it.
     * You cannot submit this file, as we will be using our own
     * version of it during grading.

     * Note that this file includes all possible variants of how
     * someone can get from one island to another. Inclusion in
     * this class does not imply that any of the indicated actions
     * are a good idea or even allowed.
     */
	
	//NEW ADDITION FOR 2014
	//MUST BE CALLED AT THE START OF CHILDITINERARY!
	public void initializeChild(){
		System.out.println("A child has forked.");
		cO++;
	}
	
	//NEW ADDITION FOR 2014
	//MUST BE CALLED AT THE START OF ADULTITINERARY!
	public void initializeAdult(){
		System.out.println("An adult as forked.");
		aO++;
	}

    /* ChildRowToMolokai should be called when a child pilots the boat
       from Oahu to Molokai */
    public void ChildRowToMolokai() {
	System.out.println("**Child rowing to Molokai.");
	Lib.assertTrue(boatSide==0);
	psg=0;
	type=0;
	boatSide=1;
	Lib.assertTrue(cO>0);
	cO--;cM++;
    }

    /* ChildRowToOahu should be called when a child pilots the boat
       from Molokai to Oahu*/
    public void ChildRowToOahu() {
	System.out.println("**Child rowing to Oahu.");
	Lib.assertTrue(boatSide==1);
	psg=0;
	type=0;
	boatSide=0;
	Lib.assertTrue(cM>0);
	cM--;cO++;
    }

    /* ChildRideToMolokai should be called when a child not piloting
       the boat disembarks on Molokai */
    public void ChildRideToMolokai() {
	System.out.println("**Child arrived on Molokai as a passenger.");
	Lib.assertTrue(boatSide==1);
	Lib.assertTrue(type==0);
	Lib.assertTrue(psg==0);
	psg++;
	Lib.assertTrue(cO>0);
	cO--;cM++;
    }

    /* ChildRideToOahu should be called when a child not piloting
       the boat disembarks on Oahu */
    public void ChildRideToOahu() {
	System.out.println("**Child arrived on Oahu as a passenger.");
	Lib.assertTrue(boatSide==0);
	Lib.assertTrue(type==0);
	Lib.assertTrue(psg==0);
	psg++;
	Lib.assertTrue(cM>0);
	cM--;cO++;
    }

    /* AdultRowToMolokai should be called when a adult pilots the boat
       from Oahu to Molokai */
    public void AdultRowToMolokai() {
	System.out.println("**Adult rowing to Molokai.");
	Lib.assertTrue(boatSide==0);
	psg=0;
	type=1;
	boatSide=1;
	Lib.assertTrue(aO>0);
	aO--;aM++;
    }

    /* AdultRowToOahu should be called when a adult pilots the boat
       from Molokai to Oahu */
    public void AdultRowToOahu() {
	System.out.println("**Adult rowing to Oahu.");
	Lib.assertTrue(boatSide==1);
	psg=0;
	type=1;
	boatSide=0;
	Lib.assertTrue(aM>0);
	aM--;aO++;
    }

    /* AdultRideToMolokai should be called when an adult not piloting
       the boat disembarks on Molokai */
    public void AdultRideToMolokai() {
	System.out.println("**Adult arrived on Molokai as a passenger.");
	Lib.assertTrue(boatSide==1);
	Lib.assertTrue(type==1);
	Lib.assertTrue(psg==0);
	psg++;
	Lib.assertTrue(aO>0);
	aO--;aM++;
    }

    /* AdultRideToOahu should be called when an adult not piloting
       the boat disembarks on Oahu */
    public void AdultRideToOahu() {
	System.out.println("**Adult arrived on Oahu as a passenger.");
	Lib.assertTrue(boatSide==0);
	Lib.assertTrue(type==1);
	Lib.assertTrue(psg==0);
	psg++;
	Lib.assertTrue(aM>0);
	aM--;aO++;
    }
	
	public void grade(int a, int c){
		Lib.assertTrue(aM==a);
		Lib.assertTrue(cM==c);
		
		
	}
}




