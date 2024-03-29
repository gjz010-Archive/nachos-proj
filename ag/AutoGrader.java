// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.ag;

import nachos.machine.*;
import nachos.security.*;
import nachos.threads.*;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * The default autograder. Loads the kernel, and then tests it using
 * <tt>Kernel.selfTest()</tt>.
 */
public class AutoGrader
{
  /**
   * Allocate a new autograder.
   */
  public AutoGrader ()
  {
  }
  
  /**
   * Start this autograder. Extract the <tt>-#</tt> arguments, call
   * <tt>init()</tt>, load and initialize the kernel, and call <tt>run()</tt>.
   * 
   * @param privilege
   *          encapsulates privileged access to the Nachos machine.
   */
  public void start (Privilege privilege)
  {
    Lib.assertTrue(this.privilege == null, "start() called multiple times");
    this.privilege = privilege;
    
    String[] args = Machine.getCommandLineArguments();
    
    extractArguments(args);
    
    System.out.print(" grader");
    
    init();
    
    System.out.print("\n");
    
    kernel = (Kernel)Lib.constructObject(Config.getString("Kernel.kernel"));
    kernel.initialize(args);
    
    run();
  }
  
  private void extractArguments (String[] args)
  {
    String testArgsString = Config.getString("AutoGrader.testArgs");
    if (testArgsString == null)
    {
      testArgsString = "";
    }
    
    for (int i = 0; i < args.length;)
    {
      String arg = args[i++];
      if (arg.length() > 0 && arg.charAt(0) == '-')
      {
        if (arg.equals("-#"))
        {
          Lib.assertTrue(i < args.length, "-# switch missing argument");
          testArgsString = args[i++];
        }
      }
    }
    
    StringTokenizer st = new StringTokenizer(testArgsString, ",\n\t\f\r");
    
    while (st.hasMoreTokens())
    {
      StringTokenizer pair = new StringTokenizer(st.nextToken(), "=");
      
      Lib.assertTrue(pair.hasMoreTokens(), "test argument missing key");
      String key = pair.nextToken();
      
      Lib.assertTrue(pair.hasMoreTokens(), "test argument missing value");
      String value = pair.nextToken();
      
      testArgs.put(key, value);
    }
  }
  
  String getStringArgument (String key)
  {
    String value = (String)testArgs.get(key);
    Lib.assertTrue(value != null, "getStringArgument(" + key
      + ") failed to find key");
    return value;
  }
  
  int getIntegerArgument (String key)
  {
    try
    {
      return Integer.parseInt(getStringArgument(key));
    }
    catch (NumberFormatException e)
    {
      Lib.assertNotReached("getIntegerArgument(" + key + ") failed: "
        + "value is not an integer");
      return 0;
    }
  }
  
  boolean getBooleanArgument (String key)
  {
    String value = getStringArgument(key);
    
    if (value.equals("1") || value.toLowerCase().equals("true"))
    {
      return true;
    }
    else if (value.equals("0") || value.toLowerCase().equals("false"))
    {
      return false;
    }
    else
    {
      Lib.assertNotReached("getBooleanArgument(" + key + ") failed: "
        + "value is not a boolean");
      return false;
    }
  }
  
  long getTime ()
  {
    return privilege.stats.totalTicks;
  }
  
  void targetLevel (int targetLevel)
  {
    this.targetLevel = targetLevel;
  }
  
  void level (int level)
  {
    this.level++;
    Lib.assertTrue(level == this.level,
      "level() advanced more than one step: test jumped ahead");
    
    if (level == targetLevel)
      done();
  }
  
  private int level = 0, targetLevel = 0;
  
  void done ()
  {
    System.out.print("\nsuccess\n");
    privilege.exit(162);
  }
  
  private Hashtable<String, String> testArgs = new Hashtable<String, String>();
  
  void init ()
  {
  }
  
  void run ()
  {
    kernel.selfTest();
    kernel.run();
    kernel.terminate();
  }
  
  Privilege privilege = null;
  Kernel kernel;
  
  /**
   * Notify the autograder that the specified thread is the idle thread.
   * <tt>KThread.createIdleThread()</tt> <i>must</i> call this method before
   * forking the idle thread.
   * 
   * @param idleThread
   *          the idle thread.
   */
  public void setIdleThread (KThread idleThread)
  {
  }
  
  /**
   * Notify the autograder that the specified thread has moved to the ready
   * state. <tt>KThread.ready()</tt> <i>must</i> call this method before
   * returning.
   * 
   * @param thread
   *          the thread that has been added to the ready set.
   */
  public void readyThread (KThread thread)
  {
  }
  
  /**
   * Notify the autograder that the specified thread is now running.
   * <tt>KThread.restoreState()</tt> <i>must</i> call this method before
   * returning.
   * 
   * @param thread
   *          the thread that is now running.
   */
  public void runningThread (KThread thread)
  {
    privilege.tcb.associateThread(thread);
    currentThread = thread;
  }
  
  /**
   * Notify the autograder that the current thread has finished.
   * <tt>KThread.finish()</tt> <i>must</i> call this method before putting the
   * thread to sleep and scheduling its TCB to be destroyed.
   */
  public void finishingCurrentThread ()
  {
    privilege.tcb.authorizeDestroy(currentThread);
  }
  
  /**
   * Notify the autograder that a timer interrupt occurred and was handled by
   * software if a timer interrupt handler was installed. Called by the hardware
   * timer.
   * 
   * @param privilege
   *          proves the authenticity of this call.
   * @param time
   *          the actual time at which the timer interrupt was issued.
   */
  public void timerInterrupt (Privilege privilege, long time)
  {
    Lib.assertTrue(privilege == this.privilege, "security violation");
  }
  
  /**
   * Notify the autograder that a user program executed a syscall instruction.
   * 
   * @param privilege
   *          proves the authenticity of this call.
   * @return <tt>true</tt> if the kernel exception handler should be called.
   */
  public boolean exceptionHandler (Privilege privilege)
  {
    Lib.assertTrue(privilege == this.privilege, "security violation");
    return true;
  }
  
  /**
   * Notify the autograder that <tt>Processor.run()</tt> was invoked. This can
   * be used to simulate user programs.
   * 
   * @param privilege
   *          proves the authenticity of this call.
   */
  public void runProcessor (Privilege privilege)
  {
    Lib.assertTrue(privilege == this.privilege, "security violation");
  }
  
  /**
   * Notify the autograder that a COFF loader is being constructed for the
   * specified file. The autograder can use this to provide its own COFF loader,
   * or return <tt>null</tt> to use the default loader.
   * 
   * @param file
   *          the executable file being loaded.
   * @return a loader to use in loading the file, or <tt>null</tt> to use the
   *         default.
   */
  public Coff createLoader (OpenFile file)
  {
    return null;
  }
  
  /**
   * Request permission to send a packet. The autograder can use this to drop
   * packets very selectively.
   * 
   * @param privilege
   *          proves the authenticity of this call.
   * @return <tt>true</tt> if the packet should be sent.
   */
  public boolean canSendPacket (Privilege privilege)
  {
    Lib.assertTrue(privilege == this.privilege, "security violation");
    return true;
  }
  
  /**
   * Request permission to receive a packet. The autograder can use this to drop
   * packets very selectively.
   * 
   * @param privilege
   *          proves the authenticity of this call.
   * @return <tt>true</tt> if the packet should be delivered to the kernel.
   */
  public boolean canReceivePacket (Privilege privilege)
  {
    Lib.assertTrue(privilege == this.privilege, "security violation");
    return true;
  }
  
  boolean hasArgument (String key)
  {
    return testArgs.get(key) != null;
  }
  
  private KThread currentThread;
  private BasicTestGrader d1;
  private BoatGrader d2;
  private CoffGrader d3;
  private DonationGrader d4;
  //private FilesysGrader d5;
  private JoinGrader d6;
  private LotteryGraderS d7;
  private PriorityGrader d8;
  private PriorityGraderS1 d9;
  private ThreadGrader1 tg1;
  private ThreadGrader2 tg2;
  private ThreadGrader3 tg3;
  private ThreadGrader4 tg4;
  private ThreadGrader5 tg5;
  private ThreadGrader6 tg6;
  private ThreadGrader7 tg7;
  private ThreadGrader8 tg8;
  //private UserGrader1 ug1;
  private VMGrader vmg;
}
