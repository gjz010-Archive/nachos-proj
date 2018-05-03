/*
FS Crasher
This application is invented to crash FS syscalls as much as possible.
*/
#include "syscall.h"
#include "stdlib.h"
#include "stdio.h"

char buffer[1024];
int main(){
    printf("FScrasher started.\n");
    int source=open("fscrasher.c");
    printf("%d\n",source);
    int size=0;
    while((size=read(source,buffer,100))!=0){
        buffer[size]=0;
        printf("[%d] %s\n",size,buffer);
    }
	halt();
    int i;
    for(i=0;i<1000;i++){
        int tmp=-1;
        tmp=open("fscrasher.c");
        close(tmp);
    }
    int bad_file=-1;
    bad_file=open("bad_file_test.txt");
    printf("Bad File:%d\n",bad_file);
    int new_file=-1;
    new_file=creat("new_file_test.txt");
    printf("%d\n",new_file);
    int file1=open("new_file_test.txt");
    unlink("new_file_test.txt");
    write(new_file, buffer,50);
    int file2=open("new_file_test.txt");
    printf("%d %d\n",file1, file2);
    close(new_file);
    printf("%d\n",read(file1, buffer, 100));
    printf("%d\n",read(file2, buffer, 100));
    close(file1);
	return 0;
	
}
