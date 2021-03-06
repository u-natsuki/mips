#include <cstdio>
#include <cstdlib>
#include <cmath>
#include <climits>
#include <cfloat>
#include <map>
#include <utility>
#include <set>
#include <iostream>
#include <memory>
#include <string>
#include <vector>
#include <algorithm>

using namespace std;

#include <stdio.h>
#include <stdlib.h>
#include <fenv.h>
#include <limits.h>
#include <float.h>
#include "util.h"
#include "table.h"

unsigned int generate_x(unsigned int a) {
  fi x,aa;
  aa.ival=MAN_TO_FLOAT(a << 13);
  aa.ival &= ~((1<<13)-1);
  x.fval=1/aa.fval;
  aa.ival |=(1<<13)-1;
  x.fval+=1/aa.fval;
  x.fval/=2;

  return MANTISSA(x.ival) >> 1;
}

unsigned int table_const(unsigned int k) {
  ll x = generate_x(k);
  int a0 = MANTISSA(k << 13) >> 13;
  return 2*x-(a0*x*x>>33);
}

unsigned int table_inc(unsigned int k) {
  ll x = generate_x(k);
  // 桁は 45, k <= 423 のとき 46
  // 13 bit のこしてシフト => 誤りの最大値が 5 ulp に拡大
  return x*x >> 33;
}

unsigned int finv(unsigned a){
  int key = (a >> 13) & 0x3ff;
  int a1=MANTISSA(a)&((1<<13)-1);
  int e=EXP(a);

  D(printf("key: %x ", key));

  // 初期状態で 23 桁のみ
  ll b=const_table[key];

  D(printf("const: %x inc: %x lower: %llx ", const_table[key], inc_table[key], (ll)a1*(ll)inc_table[key]));

  b -= (a1*inc_table[key])>>13;

  D(printf("b:%llx ", b));

  // ここは適当かどうか自信がない
  int be = - e - 1;
  b<<=1;

  unsigned int answer;

  D(printf("be: %x\n", be + 127));

  answer = a&(1LL<<31LL);
  answer |= ((a & 0x7fffff) == 0 ? be + 128 : be+127)<<23;
  answer |= (a & 0x7fffff) == 0 ? 0 : b&((1<<23)-1);
  return answer;
}

void test(FILE * fp, unsigned int a) {
  union IntAndFloat i, res, ref;

  i.ival = a;

  ref.fval = 1 / i.fval;
  res.ival = finv(i.ival);


  if (! (is_normal(a) && is_normal(ref.ival))) {
    return ;
  }

  // generate testcase for verilog
  if (TESTCASE) {
    fprintf(fp, "%08x %08x\n", a, res.ival);
  }

  if (! in_ulp(ref.ival, res.ival, 6)) {
    printf("a: %x\n", a);
    printf("  expected: ");
    print_float(ref.ival);
    printf("  %f\n", ref.fval);

    printf("    actual: ");
    print_float(res.ival);
    printf("  %f\n", res.fval);
    printf("upl %d\n", ulp(ref.ival, res.ival));
    if (TESTCASE) {
      exit(1);
    }
  }
}

ll sumDiff(int k0, unsigned x_const_diff, unsigned int x_inc_diff)
{
  ll diff = 0;
  fi input, answer;
  unsigned int a;

  const_table[k0] += x_const_diff;
  inc_table[k0] += x_inc_diff;

  for (int i = 0; i < (1 << 13) - 1; i ++) {
    a = MAN_TO_FLOAT((k0 << 13) + i);
    input.ival = a;

    answer.fval = 1 / input.fval;
    ll expected = answer.ival;
    ll actual = finv(a);

    diff += (expected - actual)*(expected - actual);
    if (diff > (1ll << 31ll)) {
      break;
    }
  }

  const_table[k0] -= x_const_diff;
  inc_table[k0] -= x_inc_diff;

  return diff;
}

int main(int argc, char *argv[])
{
  FILE * fp, * f_out = fopen("finv.vec", "w");
  char files[6][30] = {"random.vec"};
  ui i, a;

  read_tables();

  for (i = 0; i < 1; i++) {
    fp = fopen(files[i], "r");
    while (fscanf(fp, "%x", &a) != EOF) {
      test(f_out, a);
    }
    fclose(fp);
  }

  return 0;
}
