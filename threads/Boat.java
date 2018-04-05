package nachos.threads;
import nachos.ag.BoatGrader;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Random;
public class Boat
{
    static BoatGrader bg;
    
	
	private static enum PType{
		NONE,ADULT,CHILD,SUCCESS;
	};
	private static PType NONE=PType.NONE;
	private static PType ADULT=PType.ADULT;
	private static PType CHILD=PType.CHILD;
	private static PType SUCCESS=PType.SUCCESS;
	static int numAdultsO=0;
	static int numChildrenO=0;
	static int numAdultsM=0;
	static int numChildrenM=0;
	static PType boatType=NONE;
	static PType edgeType=NONE;
	static int boatMount=0;
	static int boatSide=0;
	static Condition boatArriveO;
	static Condition boatArriveM;
	static boolean success=false;
	static Lock goOnBoat;
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);
	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
  	begin(1, 2, b);
  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
  	begin(3, 3, b);
  	System.out.println("\n ***Testing Boats with 1 children, 0 adults***");
  	begin(0, 1, b);
  	System.out.println("\n ***Testing Boats with 1 children, 2 adults***");
  	begin(2, 1, b);
	
	System.out.println("\n ***But nobody wants to cross the river***");
	begin(0,0,b);
	System.out.println("\n ***They want to cause some trouble***");
	begin(1,1,b);
	System.out.println("\n ***Good Driver?***");
	begin(1,100,b);
	Random r=new Random();
	System.out.println("\n ***Final Test***");
	begin(r.nextInt(100),r.nextInt(100),b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;
	numAdultsO=0;
	numChildrenO=0;
	numAdultsM=0;
	numChildrenM=0;
	boatType=NONE;
	edgeType=NONE;
	numChildrenO=children;
	numAdultsO=adults;
	boatMount=0;
	boatSide=0;
	goOnBoat=new Lock();
	boatArriveO=new Condition(goOnBoat);
	boatArriveM=new Condition(goOnBoat);
	// Instantiate global variables here
	
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.

	Runnable adultStg=new Runnable(){
		public void run(){
			AdultItinerary();
			
		}
		
		
	};
	
	Runnable childStg=new Runnable(){
		public void run(){
			ChildItinerary();
			
		}
		
	};
	Runnable r = new Runnable() {
	    public void run() {
				if(adults==1 && children==0) boatType=ADULT;
				if(children==1 && adults==0) boatType=CHILD;
				if(adults==1 && children==1){
					System.out.println("Mission Impossible.");
					return;
					
				}
				LinkedList<KThread> people=new LinkedList<>();
				for(int i=0;i<adults;i++){
					people.add(new KThread(adultStg));
					
				}
				for(int i=0;i<children;i++){
					people.add(new KThread(childStg));
					
				}
				Iterator<KThread> iter=people.iterator();
				while(iter.hasNext()){
					KThread t=iter.next();
					t.fork();
				}
				iter=people.iterator();
				while(iter.hasNext()){
					KThread t=iter.next();
					//Debug: System.out.println("RRR:"+t.getStatus());
					t.join();
					//Debug: System.out.println("SSS:"+t.getStatus());
				}
				//Debug: System.out.println("Done!");
                //SampleItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Sample Boat Thread");
        t.fork();
		t.join();
		//Debug: System.out.println(t.getStatus());

    }

    static void AdultItinerary()
    {
	bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 
	while(edgeType!=SUCCESS){
		goOnBoat.acquire();
		while(boatType==CHILD || (numAdultsO==1 && boatType!=ADULT) || boatSide!=0 || boatMount==2){
			//Debug: System.out.println("Adult sleep"+boatType.toString()+" "+numAdultsO+" "+boatMount+" "+boatSide);
			boatArriveO.sleep();
		}
		//Get the boat.
		boatMount++;
		boolean isCaptain=(boatMount==2);
		if(boatType==ADULT){
			isCaptain=true;
		}else{
			boatType=ADULT;
		}
		if(!isCaptain){
			bg.AdultRowToMolokai();
			boatArriveM.sleep();
		}else{ //O' Captain!
			//Debug: System.out.println("Adult Captain! "+boatMount);
			if(boatMount==1)bg.AdultRowToMolokai();else bg.AdultRideToMolokai();
			numAdultsO-=boatMount;
			numAdultsM+=boatMount;
			if(numAdultsO==0 && numChildrenO==0) edgeType=SUCCESS;
			else if(numAdultsO==0 && numChildrenO==1) edgeType=CHILD;
			else if(numAdultsO==1 && numChildrenO==0) edgeType=ADULT;
			boatSide=1;
			boatType=NONE;
			boatMount=0;
			//The edge case appears here.
			if(numChildrenM==0 && edgeType==CHILD){ //If the captain find that there are no one to carry that guy back, he will do it himself.
				//Debug: System.out.println("Wise adult captain!");
				numAdultsO++;
				numAdultsM--;
				bg.AdultRowToOahu();
				boatType=CHILD;
				boatSide=0;
				boatMount=0;
				boatArriveO.wakeAll();
				goOnBoat.release();
				continue;
			}else
			{
				boatArriveM.wakeAll();
				if(edgeType==SUCCESS){
					goOnBoat.release();
					return;
				}
				
				boatArriveM.sleep(); //The bad thing is that captain will not row back - but no need to worry, just imagine that the captain is tired.
				
			}

		}
		
		while(true){
			if(edgeType==SUCCESS){
				goOnBoat.release();
				return;
				
			}
			if(edgeType==CHILD || boatSide!=1){
				boatArriveM.sleep();
			}else{
				break;
			}
		}
		//Get the boat.
		//Debug: System.out.println("Is rowing valid? "+boatSide);
		bg.AdultRowToOahu();
		numAdultsM--;
		numAdultsO++;
		boatSide=0;
		boatMount=0;
		boatArriveO.wakeAll();
		goOnBoat.release();
		
	}
	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
    }

    static void ChildItinerary()
    {
	bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 
	
	while(edgeType!=SUCCESS){
		goOnBoat.acquire();
		while(boatType==ADULT || (numChildrenO==1 && boatType!=CHILD) || boatSide!=0 || boatMount==2){
			//Debug: System.out.println("Child sleep"+boatType.toString()+" "+numChildrenO+" "+boatMount+" "+boatSide);
			boatArriveO.sleep();
		}
		//Get the boat.
		boatMount++;
		boolean isCaptain=(boatMount==2);
		if(boatType==CHILD){
			isCaptain=true;
		}else{
			boatType=CHILD;
		}
		if(!isCaptain){
			bg.ChildRowToMolokai();
			boatArriveM.sleep();
		}else{ //O' Captain!
			//Debug: System.out.println("Child Captain!");
			if(boatMount==1)bg.ChildRowToMolokai();else bg.ChildRideToMolokai();
			numChildrenO-=boatMount;
			numChildrenM+=boatMount;
			if(numAdultsO==0 && numChildrenO==0) edgeType=SUCCESS;
			else if(numAdultsO==0 && numChildrenO==1) edgeType=CHILD;
			else if(numAdultsO==1 && numChildrenO==0) edgeType=ADULT;
			//Debug: System.out.println(edgeType.toString());
			boatSide=1;
			boatType=NONE;
			boatMount=0;
			//The edge case appears here.
			if(numAdultsM==0 && edgeType==ADULT){ //If the captain find that there are no one to carry that guy back, he will do it himself.
				//Debug: System.out.println("Wise child captain!");
				numChildrenO++;
				numChildrenM--;
				bg.ChildRowToOahu();
				boatType=ADULT;
				boatSide=0;
				boatMount=0;
				boatArriveO.wakeAll();
				goOnBoat.release();
				continue;
			}else
			{
				boatArriveM.wakeAll();
				if(edgeType==SUCCESS){
					goOnBoat.release();
					return;
				}
				
				boatArriveM.sleep(); //The bad thing is that captain will not row back - but no need to worry, just imagine that the captain is tired.
				
			}

		}
		
		while(true){
			if(edgeType==SUCCESS){
				goOnBoat.release();
				return;
				
			}
			if(edgeType==ADULT || boatSide!=1){
				boatArriveM.sleep();
			}else{
				
				break;
			}
		}
		//Get the boat.
		bg.ChildRowToOahu();
		numChildrenM--;
		numChildrenO++;
		boatSide=0;
		boatMount=0;
		boatArriveO.wakeAll();
		goOnBoat.release();
		
	}
	//Debug: System.out.println("WTF?");
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	//Debug: System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
}
