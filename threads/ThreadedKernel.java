package nachos.threads;

import nachos.machine.*;
import java.util.Random;
/**
 * A multi-threaded OS kernel.
 */
public class ThreadedKernel extends Kernel {
    /**
     * Allocate a new multi-threaded kernel.
     */
    public ThreadedKernel() {
	super();
    }

    /**
     * Initialize this kernel. Creates a scheduler, the first thread, and an
     * alarm, and enables interrupts. Creates a file system if necessary.   
     */
    public void initialize(String[] args) {
	// set scheduler
	String schedulerName = Config.getString("ThreadedKernel.scheduler");
	scheduler = (Scheduler) Lib.constructObject(schedulerName);

	// set fileSystem
	String fileSystemName = Config.getString("ThreadedKernel.fileSystem");
	if (fileSystemName != null)
	    fileSystem = (FileSystem) Lib.constructObject(fileSystemName);
	else if (Machine.stubFileSystem() != null)
	    fileSystem = Machine.stubFileSystem();
	else
	    fileSystem = null;

	// start threading
	new KThread(null);

	alarm  = new Alarm();

	Machine.interrupt().enable();
    }

    /**
     * Test this kernel. Test the <tt>KThread</tt>, <tt>Semaphore</tt>,
     * <tt>SynchList</tt>, and <tt>ElevatorBank</tt> classes. Note that the
     * autograder never calls this method, so it is safe to put additional
     * tests here.
     */	
    public void selfTest() {
	KThread.selfTest();
	Semaphore.selfTest();
	SynchList.selfTest();
	if (Machine.bank() != null) {
	    ElevatorBank.selfTest();
	}
    }
    
	class RandomActor implements Runnable{
		private boolean speaker;
		private int num;
		private Communicator c;
		private Semaphore counter;
		//private int index;
		public RandomActor(boolean isSpeaker, int i, Communicator cc,Semaphore co){
			speaker=isSpeaker;
			num=i;
			c=cc;
			counter=co;
			System.out.println("Created a "+(isSpeaker?"speaker"+" with number "+num:"listener"));
		}
		public void run(){
			if(speaker){
				c.speak(num);
				
			}else{
				c.listen();
				
			}
			counter.V();
			
		}
		
	}
	class Waiter implements Runnable{
		private int timeout;
		private Alarm alm;
		public Waiter(Alarm a,int t){alm=a;timeout=t;};
		public void run(){
			alm.waitUntil(timeout);
			System.out.println(timeout);
			
		}
		
	}
    /**
     * A threaded kernel does not run user programs, so this method does
     * nothing.
     */
    public void run() {/*
		Random random=new Random();
		int N=10;
		Alarm a=new Alarm();
		Waiter[] waiter=new Waiter[N];
		for(int i=0;i<N;i++){
			waiter[i]=new Waiter(a,random.nextInt(1000)*100);
		}
		for(int i=0;i<N;i++){
			new KThread(waiter[i]).fork();
		}
		a.waitUntil(2000*100);
		
		Boat.selfTest();
		*/
		/*
		Communicator c=new Communicator();
		Semaphore cntr=new Semaphore(0);
		int N=1000;
		RandomActor[] actor=new RandomActor[N];
		for(int i=0;i<N;i++){
			actor[i]=new RandomActor(random.nextBoolean(), random.nextInt(100), c,cntr);
			
		}
		for(int i=0;i<N;i++){
			new KThread(actor[i]).fork();
			
		}
		for(int i=0;i<N;i++){
			cntr.P();
		}*/
		
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	Machine.halt();
    }

    /** Globally accessible reference to the scheduler. */
    public static Scheduler scheduler = null;
    /** Globally accessible reference to the alarm. */
    public static Alarm alarm = null;
    /** Globally accessible reference to the file system. */
    public static FileSystem fileSystem = null;

    // dummy variables to make javac smarter
    private static RoundRobinScheduler dummy1 = null;
    private static PriorityScheduler dummy2 = null;
    private static LotteryScheduler dummy3 = null;
    private static Condition2 dummy4 = null;
    private static Communicator dummy5 = null;
    private static Rider dummy6 = null;
    private static ElevatorController dummy7 = null;
}
