/*
FS Crasher
This application is invented to crash FS syscalls as much as possible.
*/
#include "syscall.h"
#include "stdlib.h"
#include "stdio.h"
char ASSERTION_FAILED[]="Assertion Failed!";


int main(){
	char filename[]="file0.txt";
	char err1[]="Create File Failed!\n";
	int fd[16];
	int i=0;
	for(i=0;i<16;i++){
		fd[i]=creat(filename);
		if(fd[i]==-1){
			write(stdout,err1,sizeof(err1));
			write(stdout,filename,sizeof(filename));
		}else{
			write(fd[i],filename,sizeof(filename));
			close(fd[i]);
			unlink(filename);
		}
		filename[4]++;
		if(filename[4]==('9'+1)) filename[4]='a';
	}
	
	printf("Create Nullpointer Test\n");
	assert(creat(0)==-1);
	printf("Create BadFile Test!\n");
	assert(creat("!!!")==-1);
	
	filename[4]='x';
	printf("%d\n",write(0,0,0));
	assert(write(0,-114,1)==-1);
	assert(write(-1,-114,0)==-1);
	assert(write(10000,-114,514)==-1);
	halt();
	return 0;
	
}