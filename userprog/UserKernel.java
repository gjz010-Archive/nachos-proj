package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.HashMap;
/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
	super.initialize(args);

	console = new SynchConsole(Machine.console());
	memoryPages=new MemoryPageManager(Machine.processor().getNumPhysPages());
	pid=new PIDManager();
	Machine.processor().setExceptionHandler(new Runnable() {
		public void run() { exceptionHandler(); }
	    });
    }

    /**
     * Test the console device.
     */	
    public void selfTest() {
	super.selfTest();

	System.out.println("Testing the console device. Typed characters");
	System.out.println("will be echoed until q is typed.");

	char c;

	do {
	    c = (char) console.readByte(true);
	    console.writeByte(c);
	}
	while (c != 'q');

	System.out.println("");
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;
	
	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
	super.run();
	//System.out.println("UserKernel");
	UserProcess process = UserProcess.newUserProcess();
	
	String shellProgram = Machine.getShellProgramName();	
	//System.out.println(shellProgram);
	Lib.assertTrue(process.execute(shellProgram, new String[] { }));
	//System.out.println(shellProgram);
	KThread.currentThread().finish();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }

    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
	
	public static class MemoryPageManager{
		private int totalPages;
		private int freePages;
		private int lastPage=0;
		private Lock lock;
		private LinkedList<Integer> recycledPages;
		public MemoryPageManager(int physMemoryPages){
			totalPages=freePages=physMemoryPages;
			recycledPages=new LinkedList<Integer>();
			lock=new Lock();
		}
		public int[] malloc(int size){
			//System.out.println(size);
			lock.acquire();
			if(size>freePages){
				lock.release();
				return null;
			}
			freePages-=size;
			int pages[]=new int[size];
			for(int i=0;i<size;i++) pages[i]=fetchPage();
			lock.release();
			return pages;
		}
		private int fetchPage(){
			if(recycledPages.peek()!=null) return recycledPages.poll();
			else return lastPage++;
			
		}
		public void free(int page){
			lock.acquire();
			recycledPages.add(page);
			freePages++;
			if(freePages==totalPages){
				recycledPages.clear();
				
			}
			//System.out.println(freePages+" free pages left!");
			lock.release();
		}
		
	}
	public static MemoryPageManager memoryPages=null;
	
	public static class PIDManager{
		int lastPID=0;
		Lock lock;
		private HashMap<Integer, UserProcess> current;
		private HashMap<Integer, UserProcess> zombies;
		public PIDManager(){
			lock=new Lock();
			lastPID=0;
			current=new HashMap<>();
			zombies=new HashMap<>();
		}
		public int allocate(){
			lock.acquire();
			int p=lastPID++;
			lock.release();
			return p;
		}
		public void addProcess(int pid, UserProcess process){
			lock.acquire();
			//System.out.println("Adding "+pid);
			current.put(pid,process);
			lock.release();
		}
		public void removeProcess(int pid){
			lock.acquire();
			UserProcess zombie=current.get(pid);
			current.remove(pid);
			zombies.put(pid, zombie);
			if(current.size()==0) Kernel.kernel.terminate();
			lock.release();
		}
		
		public UserProcess getProcess(int pid){
			lock.acquire();
			UserProcess process=current.get(pid);
			if(process!=null){
				//System.out.println(process);
				lock.release();
				return process;
			}
			//System.out.println("Getting Zombie Process!");
			process=zombies.get(pid);
			zombies.remove(pid);
			lock.release();
			return process;
			
		}
		
	}
	public static PIDManager pid=null;
}

