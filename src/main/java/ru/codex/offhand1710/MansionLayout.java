package ru.codex.offhand1710;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** WoodlandMansionPieces of 1.12.2, converted from the game's classes with a script; the short names are the original ones. */
final class MansionLayout {



   public static void a(Object x_, et x_1, atm x_2, List<i> x_3, Random x_4) {
      c x_5 = new c(x_4);
      d x_6 = new d(x_, x_4);
      x_6.a(x_1, x_2, x_3, x_5);
   }

   static class a extends b {

      a() {
      }

      public String a(Random x_) {
         return "1x1_a" + (x_.nextInt(5) + 1);
      }

      public String b(Random x_) {
         return "1x1_as" + (x_.nextInt(4) + 1);
      }

      public String a(Random x_, boolean x_1) {
         return "1x2_a" + (x_.nextInt(9) + 1);
      }

      public String b(Random x_, boolean x_1) {
         return "1x2_b" + (x_.nextInt(5) + 1);
      }

      public String c(Random x_) {
         return "1x2_s" + (x_.nextInt(2) + 1);
      }

      public String d(Random x_) {
         return "2x2_a" + (x_.nextInt(4) + 1);
      }

      public String e(Random x_) {
         return "2x2_s1";
      }
   }

   abstract static class b {

      b() {}

      public abstract String a(Random var1);

      public abstract String b(Random var1);

      public abstract String a(Random var1, boolean var2);

      public abstract String b(Random var1, boolean var2);

      public abstract String c(Random var1);

      public abstract String d(Random var1);

      public abstract String e(Random var1);
   }

   static class c {

      private final Random a;
      private final g b;
      private final g c;
      private final g[] d;
      private final int e;
      private final int f;


      public c(Random x_) {
         this.a = x_;
         boolean x_1 = true;
         this.e = 7;
         this.f = 4;
         this.b = new g(11, 11, 5);
         this.b.a(this.e, this.f, this.e + 1, this.f + 1, 3);
         this.b.a(this.e - 1, this.f, this.e - 1, this.f + 1, 2);
         this.b.a(this.e + 2, this.f - 2, this.e + 3, this.f + 3, 5);
         this.b.a(this.e + 1, this.f - 2, this.e + 1, this.f - 1, 1);
         this.b.a(this.e + 1, this.f + 2, this.e + 1, this.f + 3, 1);
         this.b.a(this.e - 1, this.f - 1, 1);
         this.b.a(this.e - 1, this.f + 2, 1);
         this.b.a(0, 0, 11, 1, 5);
         this.b.a(0, 9, 11, 11, 5);
         this.a(this.b, this.e, this.f - 2, fa.e, 6);
         this.a(this.b, this.e, this.f + 3, fa.e, 6);
         this.a(this.b, this.e - 2, this.f - 1, fa.e, 3);
         this.a(this.b, this.e - 2, this.f + 2, fa.e, 3);

         while(this.a(this.b)) {
            ;
         }

         this.d = new g[3];
         this.d[0] = new g(11, 11, 5);
         this.d[1] = new g(11, 11, 5);
         this.d[2] = new g(11, 11, 5);
         this.a(this.b, this.d[0]);
         this.a(this.b, this.d[1]);
         this.d[0].a(this.e + 1, this.f, this.e + 1, this.f + 1, 8388608);
         this.d[1].a(this.e + 1, this.f, this.e + 1, this.f + 1, 8388608);
         this.c = new g(this.b.b, this.b.c, 5);
         this.b();
         this.a(this.c, this.d[2]);
      }

      public static boolean a(g x_, int x_1, int x_2) {
         int x_3 = x_.a(x_1, x_2);
         return x_3 == 1 || x_3 == 2 || x_3 == 3 || x_3 == 4;
      }

      public boolean a(g x_, int x_1, int x_2, int x_3, int x_4) {
         return (this.d[x_3].a(x_1, x_2) & '\uffff') == x_4;
      }

      
      public fa b(g x_, int x_1, int x_2, int x_3, int x_4) {
         fa[] var6 = fa.horizontal();
         int var7 = var6.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            fa x_5 = var6[var8];
            if(this.a(x_, x_1 + x_5.g(), x_2 + x_5.i(), x_3, x_4)) {
               return x_5;
            }
         }

         return null;
      }

      private void a(g x_, int x_1, int x_2, fa x_3, int x_4) {
         if(x_4 > 0) {
            x_.a(x_1, x_2, 1);
            x_.a(x_1 + x_3.g(), x_2 + x_3.i(), 0, 1);

            fa x_6;
            for(int x_5 = 0; x_5 < 8; ++x_5) {
               x_6 = fa.b(this.a.nextInt(4));
               if(x_6 != x_3.d() && (x_6 != fa.f || !this.a.nextBoolean())) {
                  int x_7 = x_1 + x_3.g();
                  int x_8 = x_2 + x_3.i();
                  if(x_.a(x_7 + x_6.g(), x_8 + x_6.i()) == 0 && x_.a(x_7 + x_6.g() * 2, x_8 + x_6.i() * 2) == 0) {
                     this.a(x_, x_1 + x_3.g() + x_6.g(), x_2 + x_3.i() + x_6.i(), x_6, x_4 - 1);
                     break;
                  }
               }
            }

            fa var10 = x_3.e();
            x_6 = x_3.f();
            x_.a(x_1 + var10.g(), x_2 + var10.i(), 0, 2);
            x_.a(x_1 + x_6.g(), x_2 + x_6.i(), 0, 2);
            x_.a(x_1 + x_3.g() + var10.g(), x_2 + x_3.i() + var10.i(), 0, 2);
            x_.a(x_1 + x_3.g() + x_6.g(), x_2 + x_3.i() + x_6.i(), 0, 2);
            x_.a(x_1 + x_3.g() * 2, x_2 + x_3.i() * 2, 0, 2);
            x_.a(x_1 + var10.g() * 2, x_2 + var10.i() * 2, 0, 2);
            x_.a(x_1 + x_6.g() * 2, x_2 + x_6.i() * 2, 0, 2);
         }
      }

      private boolean a(g x_) {
         boolean x_1 = false;

         for(int x_2 = 0; x_2 < x_.c; ++x_2) {
            for(int x_3 = 0; x_3 < x_.b; ++x_3) {
               if(x_.a(x_3, x_2) == 0) {
                  byte x_4 = 0;
                  int var7 = x_4 + (a(x_, x_3 + 1, x_2)?1:0);
                  var7 += a(x_, x_3 - 1, x_2)?1:0;
                  var7 += a(x_, x_3, x_2 + 1)?1:0;
                  var7 += a(x_, x_3, x_2 - 1)?1:0;
                  if(var7 >= 3) {
                     x_.a(x_3, x_2, 2);
                     x_1 = true;
                  } else if(var7 == 2) {
                     byte x_5 = 0;
                     int var8 = x_5 + (a(x_, x_3 + 1, x_2 + 1)?1:0);
                     var8 += a(x_, x_3 - 1, x_2 + 1)?1:0;
                     var8 += a(x_, x_3 + 1, x_2 - 1)?1:0;
                     var8 += a(x_, x_3 - 1, x_2 - 1)?1:0;
                     if(var8 <= 1) {
                        x_.a(x_3, x_2, 2);
                        x_1 = true;
                     }
                  }
               }
            }
         }

         return x_1;
      }

      private void b() {
         ArrayList x_ = Lists.newArrayList();
         g x_1 = this.d[1];

         int x_3;
         int x_5;
         for(int x_2 = 0; x_2 < this.c.c; ++x_2) {
            for(x_3 = 0; x_3 < this.c.b; ++x_3) {
               int x_4 = x_1.a(x_3, x_2);
               x_5 = x_4 & 983040;
               if(x_5 == 131072 && (x_4 & 2097152) == 2097152) {
                  x_.add(new rr(Integer.valueOf(x_3), Integer.valueOf(x_2)));
               }
            }
         }

         if(x_.isEmpty()) {
            this.c.a(0, 0, this.c.b, this.c.c, 5);
         } else {
            rr var13 = (rr)x_.get(this.a.nextInt(x_.size()));
            x_3 = x_1.a(((Integer)var13.a()).intValue(), ((Integer)var13.b()).intValue());
            x_1.a(((Integer)var13.a()).intValue(), ((Integer)var13.b()).intValue(), x_3 | 4194304);
            fa var14 = this.b(this.b, ((Integer)var13.a()).intValue(), ((Integer)var13.b()).intValue(), 1, x_3 & '\uffff');
            x_5 = ((Integer)var13.a()).intValue() + var14.g();
            int x_6 = ((Integer)var13.b()).intValue() + var14.i();

            for(int x_7 = 0; x_7 < this.c.c; ++x_7) {
               for(int x_8 = 0; x_8 < this.c.b; ++x_8) {
                  if(!a(this.b, x_8, x_7)) {
                     this.c.a(x_8, x_7, 5);
                  } else if(x_8 == ((Integer)var13.a()).intValue() && x_7 == ((Integer)var13.b()).intValue()) {
                     this.c.a(x_8, x_7, 3);
                  } else if(x_8 == x_5 && x_7 == x_6) {
                     this.c.a(x_8, x_7, 3);
                     this.d[2].a(x_8, x_7, 8388608);
                  }
               }
            }

            ArrayList var15 = Lists.newArrayList();
            fa[] var16 = fa.horizontal();
            int var10 = var16.length;

            for(int var11 = 0; var11 < var10; ++var11) {
               fa x_9 = var16[var11];
               if(this.c.a(x_5 + x_9.g(), x_6 + x_9.i()) == 0) {
                  var15.add(x_9);
               }
            }

            if(var15.isEmpty()) {
               this.c.a(0, 0, this.c.b, this.c.c, 5);
               x_1.a(((Integer)var13.a()).intValue(), ((Integer)var13.b()).intValue(), x_3);
            } else {
               fa var17 = (fa)var15.get(this.a.nextInt(var15.size()));
               this.a(this.c, x_5 + var17.g(), x_6 + var17.i(), var17, 4);

               while(this.a(this.c)) {
                  ;
               }

            }
         }
      }

      private void a(g x_, g x_1) {
         ArrayList x_2 = Lists.newArrayList();

         int x_3;
         for(x_3 = 0; x_3 < x_.c; ++x_3) {
            for(int x_4 = 0; x_4 < x_.b; ++x_4) {
               if(x_.a(x_4, x_3) == 2) {
                  x_2.add(new rr(Integer.valueOf(x_4), Integer.valueOf(x_3)));
               }
            }
         }

         Collections.shuffle(x_2, this.a);
         x_3 = 10;
         Iterator var19 = x_2.iterator();

         while(var19.hasNext()) {
            rr x_5 = (rr)var19.next();
            int x_6 = ((Integer)x_5.a()).intValue();
            int x_7 = ((Integer)x_5.b()).intValue();
            if(x_1.a(x_6, x_7) == 0) {
               int x_8 = x_6;
               int x_9 = x_6;
               int x_10 = x_7;
               int x_11 = x_7;
               int x_12 = 65536;
               if(x_1.a(x_6 + 1, x_7) == 0 && x_1.a(x_6, x_7 + 1) == 0 && x_1.a(x_6 + 1, x_7 + 1) == 0 && x_.a(x_6 + 1, x_7) == 2 && x_.a(x_6, x_7 + 1) == 2 && x_.a(x_6 + 1, x_7 + 1) == 2) {
                  x_9 = x_6 + 1;
                  x_11 = x_7 + 1;
                  x_12 = 262144;
               } else if(x_1.a(x_6 - 1, x_7) == 0 && x_1.a(x_6, x_7 + 1) == 0 && x_1.a(x_6 - 1, x_7 + 1) == 0 && x_.a(x_6 - 1, x_7) == 2 && x_.a(x_6, x_7 + 1) == 2 && x_.a(x_6 - 1, x_7 + 1) == 2) {
                  x_8 = x_6 - 1;
                  x_11 = x_7 + 1;
                  x_12 = 262144;
               } else if(x_1.a(x_6 - 1, x_7) == 0 && x_1.a(x_6, x_7 - 1) == 0 && x_1.a(x_6 - 1, x_7 - 1) == 0 && x_.a(x_6 - 1, x_7) == 2 && x_.a(x_6, x_7 - 1) == 2 && x_.a(x_6 - 1, x_7 - 1) == 2) {
                  x_8 = x_6 - 1;
                  x_10 = x_7 - 1;
                  x_12 = 262144;
               } else if(x_1.a(x_6 + 1, x_7) == 0 && x_.a(x_6 + 1, x_7) == 2) {
                  x_9 = x_6 + 1;
                  x_12 = 131072;
               } else if(x_1.a(x_6, x_7 + 1) == 0 && x_.a(x_6, x_7 + 1) == 2) {
                  x_11 = x_7 + 1;
                  x_12 = 131072;
               } else if(x_1.a(x_6 - 1, x_7) == 0 && x_.a(x_6 - 1, x_7) == 2) {
                  x_8 = x_6 - 1;
                  x_12 = 131072;
               } else if(x_1.a(x_6, x_7 - 1) == 0 && x_.a(x_6, x_7 - 1) == 2) {
                  x_10 = x_7 - 1;
                  x_12 = 131072;
               }

               int x_13 = this.a.nextBoolean()?x_8:x_9;
               int x_14 = this.a.nextBoolean()?x_10:x_11;
               int x_15 = 2097152;
               if(!x_.b(x_13, x_14, 1)) {
                  x_13 = x_13 == x_8?x_9:x_8;
                  x_14 = x_14 == x_10?x_11:x_10;
                  if(!x_.b(x_13, x_14, 1)) {
                     x_14 = x_14 == x_10?x_11:x_10;
                     if(!x_.b(x_13, x_14, 1)) {
                        x_13 = x_13 == x_8?x_9:x_8;
                        x_14 = x_14 == x_10?x_11:x_10;
                        if(!x_.b(x_13, x_14, 1)) {
                           x_15 = 0;
                           x_13 = x_8;
                           x_14 = x_10;
                        }
                     }
                  }
               }

               for(int x_16 = x_10; x_16 <= x_11; ++x_16) {
                  for(int x_17 = x_8; x_17 <= x_9; ++x_17) {
                     if(x_17 == x_13 && x_16 == x_14) {
                        x_1.a(x_17, x_16, 1048576 | x_15 | x_12 | x_3);
                     } else {
                        x_1.a(x_17, x_16, x_12 | x_3);
                     }
                  }
               }

               ++x_3;
            }
         }

      }
   }

   static class d {

      private final Object a;
      private final Random b;
      private int c;
      private int d;


      public d(Object x_, Random x_1) {
         this.a = x_;
         this.b = x_1;
      }

      public void a(et x_, atm x_1, List<i> x_2, c x_3) {
         e x_4 = new e();
         x_4.b = x_;
         x_4.a = x_1;
         x_4.c = "wall_flat";
         e x_5 = new e();
         this.a(x_2, x_4);
         x_5.b = x_4.b.b(8);
         x_5.a = x_4.a;
         x_5.c = "wall_window";
         if(!x_2.isEmpty()) {
            ;
         }

         g x_6 = x_3.b;
         g x_7 = x_3.c;
         this.c = x_3.e + 1;
         this.d = x_3.f + 1;
         int x_8 = x_3.e + 1;
         int x_9 = x_3.f;
         this.a(x_2, x_4, x_6, fa.d, this.c, this.d, x_8, x_9);
         this.a(x_2, x_5, x_6, fa.d, this.c, this.d, x_8, x_9);
         e x_10 = new e();
         x_10.b = x_4.b.b(19);
         x_10.a = x_4.a;
         x_10.c = "wall_window";
         boolean x_11 = false;

         int x_13;
         for(int x_12 = 0; x_12 < x_7.c && !x_11; ++x_12) {
            for(x_13 = x_7.b - 1; x_13 >= 0 && !x_11; --x_13) {
               if(MansionLayout.c.a(x_7, x_13, x_12)) {
                  x_10.b = x_10.b.a(x_1.a(fa.d), 8 + (x_12 - this.d) * 8);
                  x_10.b = x_10.b.a(x_1.a(fa.f), (x_13 - this.c) * 8);
                  this.b(x_2, x_10);
                  this.a(x_2, x_10, x_7, fa.d, x_13, x_12, x_13, x_12);
                  x_11 = true;
               }
            }
         }

         this.a(x_2, x_.b(16), x_1, x_6, x_7);
         this.a(x_2, x_.b(27), x_1, x_7, (g)null);
         if(!x_2.isEmpty()) {
            ;
         }

         b[] var33 = new b[]{new a(), new f(), new h()};

         for(x_13 = 0; x_13 < 3; ++x_13) {
            et x_14 = x_.b(8 * x_13 + (x_13 == 2?3:0));
            g x_15 = x_3.d[x_13];
            g x_16 = x_13 == 2?x_7:x_6;
            String x_17 = x_13 == 0?"carpet_south":"carpet_south_2";
            String x_18 = x_13 == 0?"carpet_west":"carpet_west_2";

            for(int x_19 = 0; x_19 < x_16.c; ++x_19) {
               for(int x_20 = 0; x_20 < x_16.b; ++x_20) {
                  if(x_16.a(x_20, x_19) == 1) {
                     et x_21 = x_14.a(x_1.a(fa.d), 8 + (x_19 - this.d) * 8);
                     x_21 = x_21.a(x_1.a(fa.f), (x_20 - this.c) * 8);
                     x_2.add(new i(this.a, "corridor_floor", x_21, x_1));
                     if(x_16.a(x_20, x_19 - 1) == 1 || (x_15.a(x_20, x_19 - 1) & 8388608) == 8388608) {
                        x_2.add(new i(this.a, "carpet_north", x_21.a(x_1.a(fa.f), 1).a(), x_1));
                     }

                     if(x_16.a(x_20 + 1, x_19) == 1 || (x_15.a(x_20 + 1, x_19) & 8388608) == 8388608) {
                        x_2.add(new i(this.a, "carpet_east", x_21.a(x_1.a(fa.d), 1).a(x_1.a(fa.f), 5).a(), x_1));
                     }

                     if(x_16.a(x_20, x_19 + 1) == 1 || (x_15.a(x_20, x_19 + 1) & 8388608) == 8388608) {
                        x_2.add(new i(this.a, x_17, x_21.a(x_1.a(fa.d), 5).a(x_1.a(fa.e), 1), x_1));
                     }

                     if(x_16.a(x_20 - 1, x_19) == 1 || (x_15.a(x_20 - 1, x_19) & 8388608) == 8388608) {
                        x_2.add(new i(this.a, x_18, x_21.a(x_1.a(fa.e), 1).a(x_1.a(fa.c), 1), x_1));
                     }
                  }
               }
            }

            String var34 = x_13 == 0?"indoors_wall":"indoors_wall_2";
            String var35 = x_13 == 0?"indoors_door":"indoors_door_2";
            ArrayList var36 = Lists.newArrayList();

            for(int x_22 = 0; x_22 < x_16.c; ++x_22) {
               for(int x_23 = 0; x_23 < x_16.b; ++x_23) {
                  boolean x_24 = x_13 == 2 && x_16.a(x_23, x_22) == 3;
                  if(x_16.a(x_23, x_22) == 2 || x_24) {
                     int x_25 = x_15.a(x_23, x_22);
                     int x_26 = x_25 & 983040;
                     int x_27 = x_25 & '\uffff';
                     x_24 = x_24 && (x_25 & 8388608) == 8388608;
                     var36.clear();
                     if((x_25 & 2097152) == 2097152) {
                        fa[] x_28 = fa.horizontal();
                        int x_29 = x_28.length;

                        for(int x_30 = 0; x_30 < x_29; ++x_30) {
                           fa x_31 = x_28[x_30];
                           if(x_16.a(x_23 + x_31.g(), x_22 + x_31.i()) == 1) {
                              var36.add(x_31);
                           }
                        }
                     }

                     fa var37 = null;
                     if(!var36.isEmpty()) {
                        var37 = (fa)var36.get(this.b.nextInt(var36.size()));
                     } else if((x_25 & 1048576) == 1048576) {
                        var37 = fa.b;
                     }

                     et var38 = x_14.a(x_1.a(fa.d), 8 + (x_22 - this.d) * 8);
                     var38 = var38.a(x_1.a(fa.f), -1 + (x_23 - this.c) * 8);
                     if(MansionLayout.c.a(x_16, x_23 - 1, x_22) && !x_3.a(x_16, x_23 - 1, x_22, x_13, x_27)) {
                        x_2.add(new i(this.a, var37 == fa.e?var35:var34, var38, x_1));
                     }

                     et var39;
                     if(x_16.a(x_23 + 1, x_22) == 1 && !x_24) {
                        var39 = var38.a(x_1.a(fa.f), 8);
                        x_2.add(new i(this.a, var37 == fa.f?var35:var34, var39, x_1));
                     }

                     if(MansionLayout.c.a(x_16, x_23, x_22 + 1) && !x_3.a(x_16, x_23, x_22 + 1, x_13, x_27)) {
                        var39 = var38.a(x_1.a(fa.d), 7);
                        var39 = var39.a(x_1.a(fa.f), 7);
                        x_2.add(new i(this.a, var37 == fa.d?var35:var34, var39, x_1.a(atm.b)));
                     }

                     if(x_16.a(x_23, x_22 - 1) == 1 && !x_24) {
                        var39 = var38.a(x_1.a(fa.c), 1);
                        var39 = var39.a(x_1.a(fa.f), 7);
                        x_2.add(new i(this.a, var37 == fa.c?var35:var34, var39, x_1.a(atm.b)));
                     }

                     if(x_26 == 65536) {
                        this.a(x_2, var38, x_1, var37, var33[x_13]);
                     } else {
                        fa var40;
                        if(x_26 == 131072 && var37 != null) {
                           var40 = x_3.b(x_16, x_23, x_22, x_13, x_27);
                           boolean var41 = (x_25 & 4194304) == 4194304;
                           this.a(x_2, var38, x_1, var40, var37, var33[x_13], var41);
                        } else if(x_26 == 262144 && var37 != null && var37 != fa.b) {
                           var40 = var37.e();
                           if(!x_3.a(x_16, x_23 + var40.g(), x_22 + var40.i(), x_13, x_27)) {
                              var40 = var40.d();
                           }

                           this.a(x_2, var38, x_1, var40, var37, var33[x_13]);
                        } else if(x_26 == 262144 && var37 == fa.b) {
                           this.a(x_2, var38, x_1, var33[x_13]);
                        }
                     }
                  }
               }
            }
         }

      }

      private void a(List<i> x_, e x_1, g x_2, fa x_3, int x_4, int x_5, int x_6, int x_7) {
         int x_8 = x_4;
         int x_9 = x_5;
         fa x_10 = x_3;

         do {
            if(!MansionLayout.c.a(x_2, x_8 + x_3.g(), x_9 + x_3.i())) {
               this.c(x_, x_1);
               x_3 = x_3.e();
               if(x_8 != x_6 || x_9 != x_7 || x_10 != x_3) {
                  this.b(x_, x_1);
               }
            } else if(MansionLayout.c.a(x_2, x_8 + x_3.g(), x_9 + x_3.i()) && MansionLayout.c.a(x_2, x_8 + x_3.g() + x_3.f().g(), x_9 + x_3.i() + x_3.f().i())) {
               this.d(x_, x_1);
               x_8 += x_3.g();
               x_9 += x_3.i();
               x_3 = x_3.f();
            } else {
               x_8 += x_3.g();
               x_9 += x_3.i();
               if(x_8 != x_6 || x_9 != x_7 || x_10 != x_3) {
                  this.b(x_, x_1);
               }
            }
         } while(x_8 != x_6 || x_9 != x_7 || x_10 != x_3);

      }

      private void a(List<i> x_, et x_1, atm x_2, g x_3, g x_4) {
         int x_5;
         int x_6;
         et x_7;
         boolean x_8;
         et x_9;
         for(x_5 = 0; x_5 < x_3.c; ++x_5) {
            for(x_6 = 0; x_6 < x_3.b; ++x_6) {
               x_7 = x_1.a(x_2.a(fa.d), 8 + (x_5 - this.d) * 8);
               x_7 = x_7.a(x_2.a(fa.f), (x_6 - this.c) * 8);
               x_8 = x_4 != null && MansionLayout.c.a(x_4, x_6, x_5);
               if(MansionLayout.c.a(x_3, x_6, x_5) && !x_8) {
                  x_.add(new i(this.a, "roof", x_7.b(3), x_2));
                  if(!MansionLayout.c.a(x_3, x_6 + 1, x_5)) {
                     x_9 = x_7.a(x_2.a(fa.f), 6);
                     x_.add(new i(this.a, "roof_front", x_9, x_2));
                  }

                  if(!MansionLayout.c.a(x_3, x_6 - 1, x_5)) {
                     x_9 = x_7.a(x_2.a(fa.f), 0);
                     x_9 = x_9.a(x_2.a(fa.d), 7);
                     x_.add(new i(this.a, "roof_front", x_9, x_2.a(atm.c)));
                  }

                  if(!MansionLayout.c.a(x_3, x_6, x_5 - 1)) {
                     x_9 = x_7.a(x_2.a(fa.e), 1);
                     x_.add(new i(this.a, "roof_front", x_9, x_2.a(atm.d)));
                  }

                  if(!MansionLayout.c.a(x_3, x_6, x_5 + 1)) {
                     x_9 = x_7.a(x_2.a(fa.f), 6);
                     x_9 = x_9.a(x_2.a(fa.d), 6);
                     x_.add(new i(this.a, "roof_front", x_9, x_2.a(atm.b)));
                  }
               }
            }
         }

         if(x_4 != null) {
            for(x_5 = 0; x_5 < x_3.c; ++x_5) {
               for(x_6 = 0; x_6 < x_3.b; ++x_6) {
                  x_7 = x_1.a(x_2.a(fa.d), 8 + (x_5 - this.d) * 8);
                  x_7 = x_7.a(x_2.a(fa.f), (x_6 - this.c) * 8);
                  x_8 = MansionLayout.c.a(x_4, x_6, x_5);
                  if(MansionLayout.c.a(x_3, x_6, x_5) && x_8) {
                     if(!MansionLayout.c.a(x_3, x_6 + 1, x_5)) {
                        x_9 = x_7.a(x_2.a(fa.f), 7);
                        x_.add(new i(this.a, "small_wall", x_9, x_2));
                     }

                     if(!MansionLayout.c.a(x_3, x_6 - 1, x_5)) {
                        x_9 = x_7.a(x_2.a(fa.e), 1);
                        x_9 = x_9.a(x_2.a(fa.d), 6);
                        x_.add(new i(this.a, "small_wall", x_9, x_2.a(atm.c)));
                     }

                     if(!MansionLayout.c.a(x_3, x_6, x_5 - 1)) {
                        x_9 = x_7.a(x_2.a(fa.e), 0);
                        x_9 = x_9.a(x_2.a(fa.c), 1);
                        x_.add(new i(this.a, "small_wall", x_9, x_2.a(atm.d)));
                     }

                     if(!MansionLayout.c.a(x_3, x_6, x_5 + 1)) {
                        x_9 = x_7.a(x_2.a(fa.f), 6);
                        x_9 = x_9.a(x_2.a(fa.d), 7);
                        x_.add(new i(this.a, "small_wall", x_9, x_2.a(atm.b)));
                     }

                     if(!MansionLayout.c.a(x_3, x_6 + 1, x_5)) {
                        if(!MansionLayout.c.a(x_3, x_6, x_5 - 1)) {
                           x_9 = x_7.a(x_2.a(fa.f), 7);
                           x_9 = x_9.a(x_2.a(fa.c), 2);
                           x_.add(new i(this.a, "small_wall_corner", x_9, x_2));
                        }

                        if(!MansionLayout.c.a(x_3, x_6, x_5 + 1)) {
                           x_9 = x_7.a(x_2.a(fa.f), 8);
                           x_9 = x_9.a(x_2.a(fa.d), 7);
                           x_.add(new i(this.a, "small_wall_corner", x_9, x_2.a(atm.b)));
                        }
                     }

                     if(!MansionLayout.c.a(x_3, x_6 - 1, x_5)) {
                        if(!MansionLayout.c.a(x_3, x_6, x_5 - 1)) {
                           x_9 = x_7.a(x_2.a(fa.e), 2);
                           x_9 = x_9.a(x_2.a(fa.c), 1);
                           x_.add(new i(this.a, "small_wall_corner", x_9, x_2.a(atm.d)));
                        }

                        if(!MansionLayout.c.a(x_3, x_6, x_5 + 1)) {
                           x_9 = x_7.a(x_2.a(fa.e), 1);
                           x_9 = x_9.a(x_2.a(fa.d), 8);
                           x_.add(new i(this.a, "small_wall_corner", x_9, x_2.a(atm.c)));
                        }
                     }
                  }
               }
            }
         }

         for(x_5 = 0; x_5 < x_3.c; ++x_5) {
            for(x_6 = 0; x_6 < x_3.b; ++x_6) {
               x_7 = x_1.a(x_2.a(fa.d), 8 + (x_5 - this.d) * 8);
               x_7 = x_7.a(x_2.a(fa.f), (x_6 - this.c) * 8);
               x_8 = x_4 != null && MansionLayout.c.a(x_4, x_6, x_5);
               if(MansionLayout.c.a(x_3, x_6, x_5) && !x_8) {
                  et x_10;
                  if(!MansionLayout.c.a(x_3, x_6 + 1, x_5)) {
                     x_9 = x_7.a(x_2.a(fa.f), 6);
                     if(!MansionLayout.c.a(x_3, x_6, x_5 + 1)) {
                        x_10 = x_9.a(x_2.a(fa.d), 6);
                        x_.add(new i(this.a, "roof_corner", x_10, x_2));
                     } else if(MansionLayout.c.a(x_3, x_6 + 1, x_5 + 1)) {
                        x_10 = x_9.a(x_2.a(fa.d), 5);
                        x_.add(new i(this.a, "roof_inner_corner", x_10, x_2));
                     }

                     if(!MansionLayout.c.a(x_3, x_6, x_5 - 1)) {
                        x_.add(new i(this.a, "roof_corner", x_9, x_2.a(atm.d)));
                     } else if(MansionLayout.c.a(x_3, x_6 + 1, x_5 - 1)) {
                        x_10 = x_7.a(x_2.a(fa.f), 9);
                        x_10 = x_10.a(x_2.a(fa.c), 2);
                        x_.add(new i(this.a, "roof_inner_corner", x_10, x_2.a(atm.b)));
                     }
                  }

                  if(!MansionLayout.c.a(x_3, x_6 - 1, x_5)) {
                     x_9 = x_7.a(x_2.a(fa.f), 0);
                     x_9 = x_9.a(x_2.a(fa.d), 0);
                     if(!MansionLayout.c.a(x_3, x_6, x_5 + 1)) {
                        x_10 = x_9.a(x_2.a(fa.d), 6);
                        x_.add(new i(this.a, "roof_corner", x_10, x_2.a(atm.b)));
                     } else if(MansionLayout.c.a(x_3, x_6 - 1, x_5 + 1)) {
                        x_10 = x_9.a(x_2.a(fa.d), 8);
                        x_10 = x_10.a(x_2.a(fa.e), 3);
                        x_.add(new i(this.a, "roof_inner_corner", x_10, x_2.a(atm.d)));
                     }

                     if(!MansionLayout.c.a(x_3, x_6, x_5 - 1)) {
                        x_.add(new i(this.a, "roof_corner", x_9, x_2.a(atm.c)));
                     } else if(MansionLayout.c.a(x_3, x_6 - 1, x_5 - 1)) {
                        x_10 = x_9.a(x_2.a(fa.d), 1);
                        x_.add(new i(this.a, "roof_inner_corner", x_10, x_2.a(atm.c)));
                     }
                  }
               }
            }
         }

      }

      private void a(List<i> x_, e x_1) {
         fa x_2 = x_1.a.a(fa.e);
         x_.add(new i(this.a, "entrance", x_1.b.a(x_2, 9), x_1.a));
         x_1.b = x_1.b.a(x_1.a.a(fa.d), 16);
      }

      private void b(List<i> x_, e x_1) {
         x_.add(new i(this.a, x_1.c, x_1.b.a(x_1.a.a(fa.f), 7), x_1.a));
         x_1.b = x_1.b.a(x_1.a.a(fa.d), 8);
      }

      private void c(List<i> x_, e x_1) {
         x_1.b = x_1.b.a(x_1.a.a(fa.d), -1);
         x_.add(new i(this.a, "wall_corner", x_1.b, x_1.a));
         x_1.b = x_1.b.a(x_1.a.a(fa.d), -7);
         x_1.b = x_1.b.a(x_1.a.a(fa.e), -6);
         x_1.a = x_1.a.a(atm.b);
      }

      private void d(List<i> x_, e x_1) {
         x_1.b = x_1.b.a(x_1.a.a(fa.d), 6);
         x_1.b = x_1.b.a(x_1.a.a(fa.f), 8);
         x_1.a = x_1.a.a(atm.d);
      }

      private void a(List<i> x_, et x_1, atm x_2, fa x_3, b x_4) {
         atm x_5 = atm.a;
         String x_6 = x_4.a(this.b);
         if(x_3 != fa.f) {
            if(x_3 == fa.c) {
               x_5 = x_5.a(atm.d);
            } else if(x_3 == fa.e) {
               x_5 = x_5.a(atm.c);
            } else if(x_3 == fa.d) {
               x_5 = x_5.a(atm.b);
            } else {
               x_6 = x_4.b(this.b);
            }
         }

         et x_7 = bcj.a(new et(1, 0, 0), ary.a, x_5, 7, 7);
         x_5 = x_5.a(x_2);
         x_7 = x_7.a(x_2);
         et x_8 = x_1.a(x_7.p(), 0, x_7.r());
         x_.add(new i(this.a, x_6, x_8, x_5));
      }

      private void a(List<i> x_, et x_1, atm x_2, fa x_3, fa x_4, b x_5, boolean x_6) {
         et x_7;
         if(x_4 == fa.f && x_3 == fa.d) {
            x_7 = x_1.a(x_2.a(fa.f), 1);
            x_.add(new i(this.a, x_5.a(this.b, x_6), x_7, x_2));
         } else if(x_4 == fa.f && x_3 == fa.c) {
            x_7 = x_1.a(x_2.a(fa.f), 1);
            x_7 = x_7.a(x_2.a(fa.d), 6);
            x_.add(new i(this.a, x_5.a(this.b, x_6), x_7, x_2, ary.b));
         } else if(x_4 == fa.e && x_3 == fa.c) {
            x_7 = x_1.a(x_2.a(fa.f), 7);
            x_7 = x_7.a(x_2.a(fa.d), 6);
            x_.add(new i(this.a, x_5.a(this.b, x_6), x_7, x_2.a(atm.c)));
         } else if(x_4 == fa.e && x_3 == fa.d) {
            x_7 = x_1.a(x_2.a(fa.f), 7);
            x_.add(new i(this.a, x_5.a(this.b, x_6), x_7, x_2, ary.c));
         } else if(x_4 == fa.d && x_3 == fa.f) {
            x_7 = x_1.a(x_2.a(fa.f), 1);
            x_.add(new i(this.a, x_5.a(this.b, x_6), x_7, x_2.a(atm.b), ary.b));
         } else if(x_4 == fa.d && x_3 == fa.e) {
            x_7 = x_1.a(x_2.a(fa.f), 7);
            x_.add(new i(this.a, x_5.a(this.b, x_6), x_7, x_2.a(atm.b)));
         } else if(x_4 == fa.c && x_3 == fa.e) {
            x_7 = x_1.a(x_2.a(fa.f), 7);
            x_7 = x_7.a(x_2.a(fa.d), 6);
            x_.add(new i(this.a, x_5.a(this.b, x_6), x_7, x_2.a(atm.b), ary.c));
         } else if(x_4 == fa.c && x_3 == fa.f) {
            x_7 = x_1.a(x_2.a(fa.f), 1);
            x_7 = x_7.a(x_2.a(fa.d), 6);
            x_.add(new i(this.a, x_5.a(this.b, x_6), x_7, x_2.a(atm.d)));
         } else if(x_4 == fa.d && x_3 == fa.c) {
            x_7 = x_1.a(x_2.a(fa.f), 1);
            x_7 = x_7.a(x_2.a(fa.c), 8);
            x_.add(new i(this.a, x_5.b(this.b, x_6), x_7, x_2));
         } else if(x_4 == fa.c && x_3 == fa.d) {
            x_7 = x_1.a(x_2.a(fa.f), 7);
            x_7 = x_7.a(x_2.a(fa.d), 14);
            x_.add(new i(this.a, x_5.b(this.b, x_6), x_7, x_2.a(atm.c)));
         } else if(x_4 == fa.e && x_3 == fa.f) {
            x_7 = x_1.a(x_2.a(fa.f), 15);
            x_.add(new i(this.a, x_5.b(this.b, x_6), x_7, x_2.a(atm.b)));
         } else if(x_4 == fa.f && x_3 == fa.e) {
            x_7 = x_1.a(x_2.a(fa.e), 7);
            x_7 = x_7.a(x_2.a(fa.d), 6);
            x_.add(new i(this.a, x_5.b(this.b, x_6), x_7, x_2.a(atm.d)));
         } else if(x_4 == fa.b && x_3 == fa.f) {
            x_7 = x_1.a(x_2.a(fa.f), 15);
            x_.add(new i(this.a, x_5.c(this.b), x_7, x_2.a(atm.b)));
         } else if(x_4 == fa.b && x_3 == fa.d) {
            x_7 = x_1.a(x_2.a(fa.f), 1);
            x_7 = x_7.a(x_2.a(fa.c), 0);
            x_.add(new i(this.a, x_5.c(this.b), x_7, x_2));
         }

      }

      private void a(List<i> x_, et x_1, atm x_2, fa x_3, fa x_4, b x_5) {
         byte x_6 = 0;
         byte x_7 = 0;
         atm x_8 = x_2;
         ary x_9 = ary.a;
         if(x_4 == fa.f && x_3 == fa.d) {
            x_6 = -7;
         } else if(x_4 == fa.f && x_3 == fa.c) {
            x_6 = -7;
            x_7 = 6;
            x_9 = ary.b;
         } else if(x_4 == fa.c && x_3 == fa.f) {
            x_6 = 1;
            x_7 = 14;
            x_8 = x_2.a(atm.d);
         } else if(x_4 == fa.c && x_3 == fa.e) {
            x_6 = 7;
            x_7 = 14;
            x_8 = x_2.a(atm.d);
            x_9 = ary.b;
         } else if(x_4 == fa.d && x_3 == fa.e) {
            x_6 = 7;
            x_7 = -8;
            x_8 = x_2.a(atm.b);
         } else if(x_4 == fa.d && x_3 == fa.f) {
            x_6 = 1;
            x_7 = -8;
            x_8 = x_2.a(atm.b);
            x_9 = ary.b;
         } else if(x_4 == fa.e && x_3 == fa.c) {
            x_6 = 15;
            x_7 = 6;
            x_8 = x_2.a(atm.c);
         } else if(x_4 == fa.e && x_3 == fa.d) {
            x_6 = 15;
            x_9 = ary.c;
         }

         et x_10 = x_1.a(x_2.a(fa.f), x_6);
         x_10 = x_10.a(x_2.a(fa.d), x_7);
         x_.add(new i(this.a, x_5.d(this.b), x_10, x_8, x_9));
      }

      private void a(List<i> x_, et x_1, atm x_2, b x_3) {
         et x_4 = x_1.a(x_2.a(fa.f), 1);
         x_.add(new i(this.a, x_3.e(this.b), x_4, x_2, ary.a));
      }
   }

   static class e {

      public atm a;
      public et b;
      public String c;


      e() {}
   }

   static class f extends b {

      f() {
      }

      public String a(Random x_) {
         return "1x1_b" + (x_.nextInt(4) + 1);
      }

      public String b(Random x_) {
         return "1x1_as" + (x_.nextInt(4) + 1);
      }

      public String a(Random x_, boolean x_1) {
         return x_1?"1x2_c_stairs":"1x2_c" + (x_.nextInt(4) + 1);
      }

      public String b(Random x_, boolean x_1) {
         return x_1?"1x2_d_stairs":"1x2_d" + (x_.nextInt(5) + 1);
      }

      public String c(Random x_) {
         return "1x2_se" + (x_.nextInt(1) + 1);
      }

      public String d(Random x_) {
         return "2x2_b" + (x_.nextInt(5) + 1);
      }

      public String e(Random x_) {
         return "2x2_s1";
      }
   }

   static class g {

      private final int[][] a;
      private final int b;
      private final int c;
      private final int d;


      public g(int x_, int x_1, int x_2) {
         this.b = x_;
         this.c = x_1;
         this.d = x_2;
         this.a = new int[x_][x_1];
      }

      public void a(int x_, int x_1, int x_2) {
         if(x_ >= 0 && x_ < this.b && x_1 >= 0 && x_1 < this.c) {
            this.a[x_][x_1] = x_2;
         }

      }

      public void a(int x_, int x_1, int x_2, int x_3, int x_4) {
         for(int x_5 = x_1; x_5 <= x_3; ++x_5) {
            for(int x_6 = x_; x_6 <= x_2; ++x_6) {
               this.a(x_6, x_5, x_4);
            }
         }

      }

      public int a(int x_, int x_1) {
         return x_ >= 0 && x_ < this.b && x_1 >= 0 && x_1 < this.c?this.a[x_][x_1]:this.d;
      }

      public void a(int x_, int x_1, int x_2, int x_3) {
         if(this.a(x_, x_1) == x_2) {
            this.a(x_, x_1, x_3);
         }

      }

      public boolean b(int x_, int x_1, int x_2) {
         return this.a(x_ - 1, x_1) == x_2 || this.a(x_ + 1, x_1) == x_2 || this.a(x_, x_1 + 1) == x_2 || this.a(x_, x_1 - 1) == x_2;
      }
   }

   static class h extends f {

      h() {
      }
   }


    /** The small part of the 1.12.2 API the layout uses: EnumFacing, Rotation, Mirror, BlockPos, Tuple. */
    enum fa {
        a, b, c, d, e, f;

        int g() {
            return this == e ? -1 : (this == f ? 1 : 0);
        }

        int i() {
            return this == c ? -1 : (this == d ? 1 : 0);
        }

        fa d() {
            return values()[ordinal() ^ 1];
        }

        fa e() {
            return horizontal()[(index() + 3) % 4];
        }

        fa f() {
            return horizontal()[(index() + 1) % 4];
        }

        int index() {
            return this == c ? 0 : (this == f ? 1 : (this == d ? 2 : 3));
        }

        /** North, east, south, west. */
        static fa[] horizontal() {
            return new fa[] {c, f, d, e};
        }

        /** EnumFacing.getHorizontal: south, west, north, east. */
        static fa b(int index) {
            return new fa[] {d, e, c, f}[Math.abs(index % 4)];
        }
    }

    enum atm {
        a, b, c, d;

        fa a(fa facing) {
            return fa.horizontal()[(facing.index() + ordinal()) % 4];
        }

        atm a(atm other) {
            return values()[(ordinal() + other.ordinal()) % 4];
        }
    }

    enum ary {
        a, b, c
    }

    static final class et {
        final int x;
        final int y;
        final int z;

        et(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        et a() {
            return new et(x, y + 1, z);
        }

        et b() {
            return new et(x, y - 1, z);
        }

        et b(int n) {
            return new et(x, y + n, z);
        }

        et c(int n) {
            return new et(x, y - n, z);
        }

        et a(fa facing) {
            return a(facing, 1);
        }

        et a(fa facing, int n) {
            return new et(x + facing.g() * n, y, z + facing.i() * n);
        }

        et a(int dx, int dy, int dz) {
            return new et(x + dx, y + dy, z + dz);
        }

        et a(atm rotation) {
            switch (rotation) {
                case b:
                    return new et(-z, y, x);
                case c:
                    return new et(-x, y, -z);
                case d:
                    return new et(z, y, -x);
                default:
                    return this;
            }
        }

        int p() {
            return x;
        }

        int q() {
            return y;
        }

        int r() {
            return z;
        }
    }

    static final class bcj {
        /** Template.getZeroPositionWithTransform. */
        static et a(et pos, ary mirror, atm rotation, int sizeX, int sizeZ) {
            int i = sizeX - 1;
            int j = sizeZ - 1;
            int k = mirror == ary.c ? i : 0;
            int l = mirror == ary.b ? j : 0;
            switch (rotation) {
                case d:
                    return pos.a(l, 0, i - k);
                case b:
                    return pos.a(j - l, 0, k);
                case c:
                    return pos.a(i - k, 0, j - l);
                default:
                    return pos.a(k, 0, l);
            }
        }
    }

    static final class rr<A, B> {
        private final A first;
        private final B second;

        rr(A first, B second) {
            this.first = first;
            this.second = second;
        }

        A a() {
            return first;
        }

        B b() {
            return second;
        }
    }

    /** One placed template. */
    static final class i {
        final String name;
        final et pos;
        final atm rotation;
        final ary mirror;

        i(Object unused, String name, et pos, atm rotation) {
            this(unused, name, pos, rotation, ary.a);
        }

        i(Object unused, String name, et pos, atm rotation, ary mirror) {
            this.name = name;
            this.pos = pos;
            this.rotation = rotation;
            this.mirror = mirror;
        }
    }
}
