#include "stdio.h"
#include "syscall.h"
#define NULL 0
int main(){
    int fscrasher=exec("fscrasher.coff",0,NULL);
    int join_result=join(fscrasher,NULL);
    printf("------> %d %d\n",fscrasher, join_result);
    return 0;
}
