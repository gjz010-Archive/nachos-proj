#include "stdio.h"
#include "stdlib.h"
#include "coffgrader.h"
//
// Xiangru Chen
//
int main() {
  int i, pid, status;
   printf("Execution Test!");
  for (i = 0; i < 10; ++i) {
    printf("----- Execution fscrasher #%d -----\n",i+1);
    pid = exec("fscrasher.coff", 0, 0);
    assertTrue(pid > 0);
    assertTrue(join(pid, &status) == 1);
    assertTrue(status == 0);
  }

  for (i = 0; i < 10; ++i) {
     printf("----- Execution matmult #%d -----\n",i+1);
    pid = exec("matmult.coff", 0, 0);
    assertTrue(pid > 0);
    assertTrue(join(pid, &status) == 1);
    assertTrue(status == 0);
  }

  printf("done.\n");

  done();

  return 0;
}
