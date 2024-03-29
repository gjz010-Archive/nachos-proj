package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.LinkedList;
import java.util.HashSet;
/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
	 private int pid=-1;
	 private int parentPID=-1;
	 private HashSet<Integer> childrenPIDs;
	 private int exitCode=0;
	 private boolean exited=false;
	private boolean crashed=false;
    public UserProcess() {
	int numPhysPages = Machine.processor().getNumPhysPages();
	pageTable = new TranslationEntry[numPhysPages];
	for (int i=0; i<numPhysPages; i++)
	    pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
	fds=new OpenFile[maxFds];
	for(int i=0;i<maxFds;i++) fds[i]=null;
	memMap=new VirtualMemoryHelper();
	pid=UserKernel.pid.allocate();
	
	crashed=false;
	exited=false;
	childrenPIDs=new HashSet<>();

    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	//System.out.println(Machine.getProcessClassName());
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
		Lib.debug(dbgProcess,"Loading");
	if (!load(name, args)){
		Lib.debug(dbgProcess,"Error!");
		return false;
	}
	    
	Lib.debug(dbgProcess,"Initializing console");
	fds[0]=UserKernel.console.openForReading();
	fds[1]=UserKernel.console.openForWriting();
	UserKernel.pid.addProcess(this.pid,this);
	mainThread=new UThread(this);
	mainThread.setName(name).fork();
	
	return true;
    }
	private UThread mainThread=null;
	private void joinMainThread(){
		//System.out.println(mainThread);
		mainThread.join();
	}
    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// for now, just assume that virtual addresses equal physical addresses
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;
	
	ArrayMapping mapping[]=memMap.getMapping(vaddr,offset,length,false);
	int amount=0;
	for(int i=0;i<mapping.length;i++){
		amount+=mapping[i].performRead(data);
	}
	/*
	int amount = Math.min(length, memory.length-vaddr);
	System.arraycopy(memory, vaddr, data, offset, amount);
	*/
	return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// for now, just assume that virtual addresses equal physical addresses
	
	ArrayMapping mapping[]=memMap.getMapping(vaddr,offset,length,true);
	int amount=0;
	for(int i=0;i<mapping.length;i++){
		amount+=mapping[i].performWrite(data);
	}
	/*
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;

	int amount = Math.min(length, memory.length-vaddr);
	System.arraycopy(data, offset, memory, vaddr, amount);
	*/
	return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
	 OpenFile executable=null;
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}
	Lib.debug(dbgProcess,"Coff");
	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}
	Lib.debug(dbgProcess,"Coff1");
	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}
	Lib.debug(dbgProcess,"Coff2");
	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}
	Lib.debug(dbgProcess,"Coff3");
	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;
	Lib.debug(dbgProcess,"Coff4");
	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	Lib.debug(dbgProcess,"Coff5");
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}
	Lib.debug(dbgProcess,"Coff6");
	//coff.close();
	//executable.close();
	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
	//ExperimentNachos
	int pages[]=UserKernel.memoryPages.malloc(numPages);
	if(pages==null){
		coff.close();
		Lib.debug(dbgProcess, "\tExperimentNachos: insufficient virtual memory");
		return false;
	}
	pageTable=new TranslationEntry[numPages];
	/*
	if (numPages > Machine.processor().getNumPhysPages()) {
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}
	*/

	// load sections
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
		int vpn = section.getFirstVPN()+i;
		pageTable[vpn]=new TranslationEntry(vpn,pages[vpn],true,section.isReadOnly(),false,false);
		// for now, just assume virtual addresses=physical addresses
		section.loadPage(i, pages[vpn]);
	    }
	}
	for(int i=0;i<=stackPages;i++){
		int index=numPages-i-1;
		pageTable[index]=new TranslationEntry(index,pages[index],true,false,false,false);
		
	}
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
		
		for(int i=0;i<pageTable.length;i++){
			Lib.debug(dbgProcess,"Free!");
			UserKernel.memoryPages.free(pageTable[i].ppn);
			
		}
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {
	if(pid!=0) return 0;
	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }
	private int allocateFD(){
		for(int i=0;i<maxFds;i++){
			if(fds[i]==null) return i;
		}
		return -1;
		
	}
	private int handleCreate(int p_name){
		String name=readVirtualMemoryString(p_name,256);
		if(name==null) return -1;
		int fd=allocateFD();
		if(fd==-1) return fd;
		OpenFile f=UserKernel.fileSystem.open(name,true);
		if(f==null) return -1;
		fds[fd]=f;
		return fd;
	}
	private int handleOpen(int p_name){
		String name=readVirtualMemoryString(p_name,256);
		if(name==null) return -1;
		int fd=allocateFD();
		if(fd==-1) return fd;
		OpenFile f=UserKernel.fileSystem.open(name,false);
		if(f==null) return -1;
		fds[fd]=f;
		return fd;
	}
	
	private int handleRead(int fd,int p_buf, int count){
		if(fd<0 || fd>=maxFds){
			Lib.debug(dbgProcess,"Bad File Descriptor!");
			return -1;
		}
		if(fds[fd]==null){
			Lib.debug(dbgProcess,"Bad or Closed File Descriptor!");
			return -1;
		}
		if(count<0){
			Lib.debug(dbgProcess,"Bad Count!");
			return -1;
		}
		byte buffer[]=new byte[count];
		OpenFile f=fds[fd];
		int rc=f.read(buffer,0,count);
		if(rc==-1){
			Lib.debug(dbgProcess,"Read Error!");
			return -1;
		}
		int cc=writeVirtualMemory(p_buf,buffer,0,rc);
		if(cc!=rc){
			Lib.debug(dbgProcess,"Write to memory error! "+cc+" "+rc);
			return -1;
		}
		return rc;
		
	}
	
	private int handleWrite(int fd, int p_buf, int count){
		if(fd<0 || fd>=maxFds) return -1;
		if(fds[fd]==null) return -1;
		if(count<0) return -1;
		byte buffer[]=new byte[count];
		if(readVirtualMemory(p_buf,buffer)!=count) return -1;
		OpenFile f=fds[fd];
		int rc=f.write(buffer,0,count);
		return rc;
		
	}
	private int handleClose(int fd){
		if(fd<0 || fd>=maxFds) return -1;
		if(fds[fd]==null) return -1;
		fds[fd].close();
		fds[fd]=null;
		return 0;
		
	}
	private int handleUnlink(int p_name){
		String name=readVirtualMemoryString(p_name,256);
		if(name==null) return -1;
		UserKernel.fileSystem.remove(name);
		return 0;
		
	}
	
	private int handleExec(int p_file, int argc, int p_argv){
		Lib.debug(dbgProcess,"Exec1");
		String file=readVirtualMemoryString(p_file,256);
		if(file==null) return -1;
		if(argc<0) return -1;
		String argv[]=new String[argc];
		Lib.debug(dbgProcess,"Exec2");
		for(int i=0;i<argc;i++){
			byte pointer[]=new byte[4];
			if(readVirtualMemory(p_argv+i*4,pointer)!=4) return -1;
			int arg_addr=(int) ((((int) pointer[0] & 0xFF) << 0)|(((int) pointer[1] & 0xFF) << 8)|(((int) pointer[2] & 0xFF) << 16) |(((int) pointer[3] & 0xFF) << 24));
			argv[i]=readVirtualMemoryString(arg_addr, 256);
			if(argv[i]==null) return -1;
			
		}
		Lib.debug(dbgProcess,"Exec3");
		UserProcess proc=forkProcess();
		if(!proc.execute(file,argv)) return -1;
		
		return proc.pid;
	}

	private int handleJoin(int pid, int p_status){
		if(!childrenPIDs.contains(pid)){
			Lib.debug(dbgProcess,"Joining non-child process!");
			return -1;
		}
		childrenPIDs.remove(pid);
		UserProcess child=UserKernel.pid.getProcess(pid);
		if(child==null){
			Lib.debug(dbgProcess,"Process not exist!");
			return -1;
		}
		if(!child.exited) child.joinMainThread();
		Lib.assertTrue(child.exited);
		int status=child.exitCode;
		int ret=child.crashed?0:1;
		Lib.debug(dbgProcess,"Return value: "+ret);
		byte arr[]=new byte[4];
		arr[0] = (byte) ((status>>0) &0xFF);
		arr[1] = (byte) ((status>>8) &0xFF);
		arr[2] = (byte) ((status>>16)&0xFF);
		arr[3] = (byte) ((status>>24)&0xFF);
		if(writeVirtualMemory(p_status, arr)!=4) return -1;
		return ret;
	}
	
	private UserProcess forkProcess(){
		UserProcess child=UserProcess.newUserProcess();
		child.parentPID=pid;
		childrenPIDs.add(child.pid);
		return child;
	}
	private void handleExit(int status){
		exited=true;
		exitCode=status;
		cleanUp();
	}
	
	private void cleanUp(){
		unloadSections();
		for(int i=0;i<maxFds;i++){
			if(fds[i]!=null) fds[i].close();
			
		}
		UserKernel.pid.removeProcess(pid);
		

		if(executable!=null) executable.close();
		UThread.finish();
	}
    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
	case syscallExit:
		handleExit(a0);
		return 0;
	case syscallExec:
		return handleExec(a0,a1,a2);
	case syscallCreate:
		return handleCreate(a0);
	case syscallOpen:
		return handleOpen(a0);
	case syscallRead:
		return handleRead(a0,a1,a2);
	case syscallWrite:
		return handleWrite(a0,a1,a2);
	case syscallClose:
		return handleClose(a0);
	case syscallUnlink:
		return handleUnlink(a0);
	case syscallJoin:
		return handleJoin(a0,a1);
	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
		return -1;
	    //Lib.assertNotReached("Unknown system call!");
	}
	//return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
		Lib.debug(dbgProcess,"Crash!");
		crashed=true;
		exited=true;
		exitCode=-1;
		cleanUp();
	    //Lib.assertNotReached("Unexpected exception");
	}
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
	
	protected final int maxFds=16;
	protected OpenFile[] fds;
	private VirtualMemoryHelper memMap;
	
	//Virtual memory helper to map array onto physical memory segments and perform write operations.
	private class VirtualMemoryHelper{
		private VirtualMemoryHelper(){
			
			
		}
		public ArrayMapping[] getMapping(int start, int offset, int count, boolean write){
			
			int firstPage=Processor.pageFromAddress(start);
			int firstOffset=Processor.offsetFromAddress(start);
			int lastPage=Processor.pageFromAddress(start+count);
			if(firstPage==lastPage){ //In-page operation
				ArrayMapping map=generateMap(firstPage, firstOffset, offset, count, write);
				if(map==null) return new ArrayMapping[0];
				ArrayMapping arr[]=new ArrayMapping[1];
				arr[0]=map;
				return arr;
				
			}else{
				LinkedList<ArrayMapping> mapping=new LinkedList<>();
				int firstPageCount=pageSize-firstOffset;
				count-=firstPageCount;
				ArrayMapping map=generateMap(firstPage, firstOffset, offset, firstPageCount, write);
				offset+=firstPageCount;
				if(map==null) return cast(mapping);
				mapping.add(map);
				for(int i=firstPage+1;i<lastPage;i++){
					count-=pageSize;
					map=generateMap(i,0,offset,pageSize,write);
					if(map==null) return cast(mapping);
					offset+=firstPageCount;
					mapping.add(map);
				}
				map=generateMap(lastPage,0,offset,count,write);
				if(map==null) return cast(mapping);
				mapping.add(map);
				return cast(mapping);
				
			}

		}
		private ArrayMapping generateMap(int page, int poffset, int offset, int count, boolean write){
			TranslationEntry entry=preparePage(page, write);
			if(entry==null) return null;
			int physPage=entry.ppn;
			int physAddr=Processor.makeAddress(physPage, poffset);
			return new ArrayMapping(physAddr, offset, count);
		}
		private TranslationEntry preparePage(int page, boolean write){
			if(page<0 || page>=UserProcess.this.numPages){
				return null;
			}
			TranslationEntry entry=UserProcess.this.pageTable[page];
			if(entry==null) return null;
			if(write){
				if(entry.readOnly) return null;
				else entry.dirty=true;
			}
			entry.used=true;
			return entry;
		}
		private ArrayMapping[] cast(LinkedList<ArrayMapping> list){
			ArrayMapping[] r=new ArrayMapping[list.size()];
			list.toArray(r);
			return r;
			
		}

	}
	private class ArrayMapping{
		private int physAddr;
		private int arrayOffset;
		private int mapCount;
		
		public ArrayMapping(int addr, int offset, int count){
			physAddr=addr;
			arrayOffset=offset;
			mapCount=count;
		}
		
		public int performWrite(byte[] src){
			byte[] memory = Machine.processor().getMemory();
			System.arraycopy(src, arrayOffset, memory, physAddr, mapCount);
			return mapCount;
			
		}
		public int performRead(byte[] target){
			byte[] memory = Machine.processor().getMemory();
			System.arraycopy(memory, physAddr, target, arrayOffset, mapCount);
			return mapCount;
		}
		
	}
}


