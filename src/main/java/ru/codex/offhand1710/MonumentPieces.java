package ru.codex.offhand1710;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.world.World;
import ru.codex.offhand1710.MonumentShim.Bbg;
import ru.codex.offhand1710.MonumentShim.Bbx;
import ru.codex.offhand1710.MonumentShim.Bs;
import ru.codex.offhand1710.MonumentShim.Dir;

/**
 * StructureOceanMonumentPieces of 1.12.2, converted from the game's classes with a script. The short names are those
 * of the original obfuscated classes; MonumentShim gives them the same meaning on 1.7.10. The pieces: h building,
 * j core room, k-o double rooms, p entry, q penthouse, r base piece, s/t simple rooms, u wing room, v room definition.
 */
final class MonumentPieces {

   static class a implements i {

      a() {}

      public boolean a(v x_) {
         return x_.c[Dir.f.a()] && !x_.b[Dir.f.a()].d;
      }

      public r a(Dir x_, v x_1, Random x_2) {
         x_1.d = true;
         x_1.b[Dir.f.a()].d = true;
         return new k(x_, x_1, x_2);
      }
   }

   static class b implements i {

      b() {}

      public boolean a(v x_) {
         if(x_.c[Dir.f.a()] && !x_.b[Dir.f.a()].d && x_.c[Dir.b.a()] && !x_.b[Dir.b.a()].d) {
            v x_1 = x_.b[Dir.f.a()];
            return x_1.c[Dir.b.a()] && !x_1.b[Dir.b.a()].d;
         } else {
            return false;
         }
      }

      public r a(Dir x_, v x_1, Random x_2) {
         x_1.d = true;
         x_1.b[Dir.f.a()].d = true;
         x_1.b[Dir.b.a()].d = true;
         x_1.b[Dir.f.a()].b[Dir.b.a()].d = true;
         return new l(x_, x_1, x_2);
      }
   }

   static class c implements i {

      c() {}

      public boolean a(v x_) {
         return x_.c[Dir.b.a()] && !x_.b[Dir.b.a()].d;
      }

      public r a(Dir x_, v x_1, Random x_2) {
         x_1.d = true;
         x_1.b[Dir.b.a()].d = true;
         return new m(x_, x_1, x_2);
      }
   }

   static class d implements i {

      d() {}

      public boolean a(v x_) {
         if(x_.c[Dir.c.a()] && !x_.b[Dir.c.a()].d && x_.c[Dir.b.a()] && !x_.b[Dir.b.a()].d) {
            v x_1 = x_.b[Dir.c.a()];
            return x_1.c[Dir.b.a()] && !x_1.b[Dir.b.a()].d;
         } else {
            return false;
         }
      }

      public r a(Dir x_, v x_1, Random x_2) {
         x_1.d = true;
         x_1.b[Dir.c.a()].d = true;
         x_1.b[Dir.b.a()].d = true;
         x_1.b[Dir.c.a()].b[Dir.b.a()].d = true;
         return new n(x_, x_1, x_2);
      }
   }

   static class e implements i {

      e() {}

      public boolean a(v x_) {
         return x_.c[Dir.c.a()] && !x_.b[Dir.c.a()].d;
      }

      public r a(Dir x_, v x_1, Random x_2) {
         v x_3 = x_1;
         if(!x_1.c[Dir.c.a()] || x_1.b[Dir.c.a()].d) {
            x_3 = x_1.b[Dir.d.a()];
         }

         x_3.d = true;
         x_3.b[Dir.c.a()].d = true;
         return new o(x_, x_3, x_2);
      }
   }

   static class f implements i {

      f() {}

      public boolean a(v x_) {
         return true;
      }

      public r a(Dir x_, v x_1, Random x_2) {
         x_1.d = true;
         return new s(x_, x_1, x_2);
      }
   }

   static class g implements i {

      g() {}

      public boolean a(v x_) {
         return !x_.c[Dir.e.a()] && !x_.c[Dir.f.a()] && !x_.c[Dir.c.a()] && !x_.c[Dir.d.a()] && !x_.c[Dir.b.a()];
      }

      public r a(Dir x_, v x_1, Random x_2) {
         x_1.d = true;
         return new t(x_, x_1, x_2);
      }
   }

   public static class h extends r {

      private v n;
      private v o;
      private final List<r> p = Lists.newArrayList();


      public h() {}

      public h(Random x_, int x_1, int x_2, Dir x_3) {
         super(0);
         this.a(x_3);
         Dir x_4 = this.f();
         if(true) {
            this.l = new Bbg(x_1, 39, x_2, x_1 + 58 - 1, 61, x_2 + 58 - 1);
         } else {
            this.l = new Bbg(x_1, 39, x_2, x_1 + 58 - 1, 61, x_2 + 58 - 1);
         }

         List x_5 = this.a(x_);
         this.n.d = true;
         this.p.add(new p(x_4, this.n));
         this.p.add(new j(x_4, this.o, x_));
         ArrayList x_6 = Lists.newArrayList();
         x_6.add(new b());
         x_6.add(new d());
         x_6.add(new e());
         x_6.add(new a());
         x_6.add(new c());
         x_6.add(new g());
         x_6.add(new f());
         Iterator x_7 = x_5.iterator();

         while(x_7.hasNext()) {
            v x_8 = (v)x_7.next();
            if(!x_8.d && !x_8.b()) {
               Iterator x_9 = x_6.iterator();

               while(x_9.hasNext()) {
                  i x_10 = (i)x_9.next();
                  if(x_10.a(x_8)) {
                     this.p.add(x_10.a(x_4, x_8, x_));
                     break;
                  }
               }
            }
         }

         int var15 = this.l.b;
         int var16 = this.a(9, 22);
         int var17 = this.b(9, 22);
         Iterator var18 = this.p.iterator();

         while(var18.hasNext()) {
            r x_11 = (r)var18.next();
            x_11.d().a(var16, var15, var17);
         }

         Bbg var19 = Bbg.a(this.a(1, 1), this.d(1), this.b(1, 1), this.a(23, 21), this.d(8), this.b(23, 21));
         Bbg var20 = Bbg.a(this.a(34, 1), this.d(1), this.b(34, 1), this.a(56, 21), this.d(8), this.b(56, 21));
         Bbg x_12 = Bbg.a(this.a(22, 22), this.d(13), this.b(22, 22), this.a(35, 35), this.d(17), this.b(35, 35));
         int x_13 = x_.nextInt();
         this.p.add(new u(x_4, var19, x_13++));
         this.p.add(new u(x_4, var20, x_13++));
         this.p.add(new q(x_4, x_12));
      }

      private List<v> a(Random x_) {
         v[] x_1 = new v[75];

         int x_2;
         int x_3;
         boolean x_4;
         int x_5;
         for(x_2 = 0; x_2 < 5; ++x_2) {
            for(x_3 = 0; x_3 < 4; ++x_3) {
               x_4 = false;
               x_5 = b(x_2, 0, x_3);
               x_1[x_5] = new v(x_5);
            }
         }

         for(x_2 = 0; x_2 < 5; ++x_2) {
            for(x_3 = 0; x_3 < 4; ++x_3) {
               x_4 = true;
               x_5 = b(x_2, 1, x_3);
               x_1[x_5] = new v(x_5);
            }
         }

         for(x_2 = 1; x_2 < 4; ++x_2) {
            for(x_3 = 0; x_3 < 2; ++x_3) {
               x_4 = true;
               x_5 = b(x_2, 2, x_3);
               x_1[x_5] = new v(x_5);
            }
         }

         this.n = x_1[g];

         int var8;
         int x_7;
         int x_9;
         int x_10;
         int x_11;
         for(x_2 = 0; x_2 < 5; ++x_2) {
            for(x_3 = 0; x_3 < 5; ++x_3) {
               for(int var17 = 0; var17 < 3; ++var17) {
                  x_5 = b(x_2, var17, x_3);
                  if(x_1[x_5] != null) {
                     Dir[] x_6 = Dir.values();
                     var8 = x_6.length;

                     for(x_7 = 0; x_7 < var8; ++x_7) {
                        Dir x_8 = x_6[x_7];
                        x_9 = x_2 + x_8.g();
                        x_10 = var17 + x_8.h();
                        x_11 = x_3 + x_8.i();
                        if(x_9 >= 0 && x_9 < 5 && x_11 >= 0 && x_11 < 5 && x_10 >= 0 && x_10 < 3) {
                           int x_12 = b(x_9, x_10, x_11);
                           if(x_1[x_12] != null) {
                              if(x_11 == x_3) {
                                 x_1[x_5].a(x_8, x_1[x_12]);
                              } else {
                                 x_1[x_5].a(x_8.d(), x_1[x_12]);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         v var15 = new v(1003);
         v var16 = new v(1001);
         v var18 = new v(1002);
         x_1[h].a(Dir.b, var15);
         x_1[i].a(Dir.d, var16);
         x_1[j].a(Dir.d, var18);
         var15.d = true;
         var16.d = true;
         var18.d = true;
         this.n.e = true;
         this.o = x_1[b(x_.nextInt(4), 0, 2)];
         this.o.d = true;
         this.o.b[Dir.f.a()].d = true;
         this.o.b[Dir.c.a()].d = true;
         this.o.b[Dir.f.a()].b[Dir.c.a()].d = true;
         this.o.b[Dir.b.a()].d = true;
         this.o.b[Dir.f.a()].b[Dir.b.a()].d = true;
         this.o.b[Dir.c.a()].b[Dir.b.a()].d = true;
         this.o.b[Dir.f.a()].b[Dir.c.a()].b[Dir.b.a()].d = true;
         ArrayList var19 = Lists.newArrayList();
         v[] var20 = x_1;
         var8 = x_1.length;

         for(x_7 = 0; x_7 < var8; ++x_7) {
            v var24 = var20[x_7];
            if(var24 != null) {
               var24.a();
               var19.add(var24);
            }
         }

         var15.a();
         Collections.shuffle(var19, x_);
         int var21 = 1;
         Iterator var22 = var19.iterator();

         while(var22.hasNext()) {
            v var23 = (v)var22.next();
            int var25 = 0;
            x_9 = 0;

            while(var25 < 2 && x_9 < 5) {
               ++x_9;
               x_10 = x_.nextInt(6);
               if(var23.c[x_10]) {
                  x_11 = Dir.a(x_10).d().a();
                  var23.c[x_10] = false;
                  var23.b[x_10].c[x_11] = false;
                  if(var23.a(var21++) && var23.b[x_10].a(var21++)) {
                     ++var25;
                  } else {
                     var23.c[x_10] = true;
                     var23.b[x_10].c[x_11] = true;
                  }
               }
            }
         }

         var19.add(var15);
         var19.add(var16);
         var19.add(var18);
         return var19;
      }

      public boolean a(World x_, Random x_1, Bbg x_2) {
         int x_3 = Math.max(63, 64) - this.l.b;
         this.a(x_, x_2, 0, 0, 0, 58, x_3, 58, false);
         this.a(false, 0, x_, x_1, x_2);
         this.a(true, 33, x_, x_1, x_2);
         this.b(x_, x_1, x_2);
         this.c(x_, x_1, x_2);
         this.d(x_, x_1, x_2);
         this.e(x_, x_1, x_2);
         this.f(x_, x_1, x_2);
         this.g(x_, x_1, x_2);

         int x_4;
         for(x_4 = 0; x_4 < 7; ++x_4) {
            int x_5 = 0;

            while(x_5 < 7) {
               if(x_5 == 0 && x_4 == 3) {
                  x_5 = 6;
               }

               int x_6 = x_4 * 9;
               int x_7 = x_5 * 9;

               for(int x_8 = 0; x_8 < 4; ++x_8) {
                  for(int x_9 = 0; x_9 < 4; ++x_9) {
                     this.a(x_, b, x_6 + x_8, 0, x_7 + x_9, x_2);
                     this.b(x_, b, x_6 + x_8, -1, x_7 + x_9, x_2);
                  }
               }

               if(x_4 != 0 && x_4 != 6) {
                  x_5 += 6;
               } else {
                  ++x_5;
               }
            }
         }

         for(x_4 = 0; x_4 < 5; ++x_4) {
            this.a(x_, x_2, -1 - x_4, 0 + x_4 * 2, -1 - x_4, -1 - x_4, 23, 58 + x_4, false);
            this.a(x_, x_2, 58 + x_4, 0 + x_4 * 2, -1 - x_4, 58 + x_4, 23, 58 + x_4, false);
            this.a(x_, x_2, 0 - x_4, 0 + x_4 * 2, -1 - x_4, 57 + x_4, 23, -1 - x_4, false);
            this.a(x_, x_2, 0 - x_4, 0 + x_4 * 2, 58 + x_4, 57 + x_4, 23, 58 + x_4, false);
         }

         Iterator var11 = this.p.iterator();

         while(var11.hasNext()) {
            r var12 = (r)var11.next();
            if(var12.d().a(x_2)) {
               var12.a(x_, x_1, x_2);
            }
         }

         return true;
      }

      private void a(boolean x_, int x_1, World x_2, Random x_3, Bbg x_4) {
         boolean x_5 = true;
         if(this.a(x_4, x_1, 0, x_1 + 23, 20)) {
            this.a(x_2, x_4, x_1 + 0, 0, 0, x_1 + 24, 0, 20, a, a, false);
            this.a(x_2, x_4, x_1 + 0, 1, 0, x_1 + 24, 10, 20, false);

            int x_6;
            for(x_6 = 0; x_6 < 4; ++x_6) {
               this.a(x_2, x_4, x_1 + x_6, x_6 + 1, x_6, x_1 + x_6, x_6 + 1, 20, b, b, false);
               this.a(x_2, x_4, x_1 + x_6 + 7, x_6 + 5, x_6 + 7, x_1 + x_6 + 7, x_6 + 5, 20, b, b, false);
               this.a(x_2, x_4, x_1 + 17 - x_6, x_6 + 5, x_6 + 7, x_1 + 17 - x_6, x_6 + 5, 20, b, b, false);
               this.a(x_2, x_4, x_1 + 24 - x_6, x_6 + 1, x_6, x_1 + 24 - x_6, x_6 + 1, 20, b, b, false);
               this.a(x_2, x_4, x_1 + x_6 + 1, x_6 + 1, x_6, x_1 + 23 - x_6, x_6 + 1, x_6, b, b, false);
               this.a(x_2, x_4, x_1 + x_6 + 8, x_6 + 5, x_6 + 7, x_1 + 16 - x_6, x_6 + 5, x_6 + 7, b, b, false);
            }

            this.a(x_2, x_4, x_1 + 4, 4, 4, x_1 + 6, 4, 20, a, a, false);
            this.a(x_2, x_4, x_1 + 7, 4, 4, x_1 + 17, 4, 6, a, a, false);
            this.a(x_2, x_4, x_1 + 18, 4, 4, x_1 + 20, 4, 20, a, a, false);
            this.a(x_2, x_4, x_1 + 11, 8, 11, x_1 + 13, 8, 20, a, a, false);
            this.a(x_2, d, x_1 + 12, 9, 12, x_4);
            this.a(x_2, d, x_1 + 12, 9, 15, x_4);
            this.a(x_2, d, x_1 + 12, 9, 18, x_4);
            x_6 = x_1 + (x_?19:5);
            int x_7 = x_1 + (x_?5:19);

            int x_8;
            for(x_8 = 20; x_8 >= 5; x_8 -= 3) {
               this.a(x_2, d, x_6, 5, x_8, x_4);
            }

            for(x_8 = 19; x_8 >= 7; x_8 -= 3) {
               this.a(x_2, d, x_7, 5, x_8, x_4);
            }

            for(x_8 = 0; x_8 < 4; ++x_8) {
               int x_9 = x_?x_1 + (24 - (17 - x_8 * 3)):x_1 + 17 - x_8 * 3;
               this.a(x_2, d, x_9, 5, 5, x_4);
            }

            this.a(x_2, d, x_7, 5, 5, x_4);
            this.a(x_2, x_4, x_1 + 11, 1, 12, x_1 + 13, 7, 12, a, a, false);
            this.a(x_2, x_4, x_1 + 12, 1, 11, x_1 + 12, 7, 13, a, a, false);
         }

      }

      private void b(World x_, Random x_1, Bbg x_2) {
         if(this.a(x_2, 22, 5, 35, 17)) {
            this.a(x_, x_2, 25, 0, 0, 32, 8, 20, false);

            for(int x_3 = 0; x_3 < 4; ++x_3) {
               this.a(x_, x_2, 24, 2, 5 + x_3 * 4, 24, 4, 5 + x_3 * 4, b, b, false);
               this.a(x_, x_2, 22, 4, 5 + x_3 * 4, 23, 4, 5 + x_3 * 4, b, b, false);
               this.a(x_, b, 25, 5, 5 + x_3 * 4, x_2);
               this.a(x_, b, 26, 6, 5 + x_3 * 4, x_2);
               this.a(x_, e, 26, 5, 5 + x_3 * 4, x_2);
               this.a(x_, x_2, 33, 2, 5 + x_3 * 4, 33, 4, 5 + x_3 * 4, b, b, false);
               this.a(x_, x_2, 34, 4, 5 + x_3 * 4, 35, 4, 5 + x_3 * 4, b, b, false);
               this.a(x_, b, 32, 5, 5 + x_3 * 4, x_2);
               this.a(x_, b, 31, 6, 5 + x_3 * 4, x_2);
               this.a(x_, e, 31, 5, 5 + x_3 * 4, x_2);
               this.a(x_, x_2, 27, 6, 5 + x_3 * 4, 30, 6, 5 + x_3 * 4, a, a, false);
            }
         }

      }

      private void c(World x_, Random x_1, Bbg x_2) {
         if(this.a(x_2, 15, 20, 42, 21)) {
            this.a(x_, x_2, 15, 0, 21, 42, 0, 21, a, a, false);
            this.a(x_, x_2, 26, 1, 21, 31, 3, 21, false);
            this.a(x_, x_2, 21, 12, 21, 36, 12, 21, a, a, false);
            this.a(x_, x_2, 17, 11, 21, 40, 11, 21, a, a, false);
            this.a(x_, x_2, 16, 10, 21, 41, 10, 21, a, a, false);
            this.a(x_, x_2, 15, 7, 21, 42, 9, 21, a, a, false);
            this.a(x_, x_2, 16, 6, 21, 41, 6, 21, a, a, false);
            this.a(x_, x_2, 17, 5, 21, 40, 5, 21, a, a, false);
            this.a(x_, x_2, 21, 4, 21, 36, 4, 21, a, a, false);
            this.a(x_, x_2, 22, 3, 21, 26, 3, 21, a, a, false);
            this.a(x_, x_2, 31, 3, 21, 35, 3, 21, a, a, false);
            this.a(x_, x_2, 23, 2, 21, 25, 2, 21, a, a, false);
            this.a(x_, x_2, 32, 2, 21, 34, 2, 21, a, a, false);
            this.a(x_, x_2, 28, 4, 20, 29, 4, 21, b, b, false);
            this.a(x_, b, 27, 3, 21, x_2);
            this.a(x_, b, 30, 3, 21, x_2);
            this.a(x_, b, 26, 2, 21, x_2);
            this.a(x_, b, 31, 2, 21, x_2);
            this.a(x_, b, 25, 1, 21, x_2);
            this.a(x_, b, 32, 1, 21, x_2);

            int x_3;
            for(x_3 = 0; x_3 < 7; ++x_3) {
               this.a(x_, c, 28 - x_3, 6 + x_3, 21, x_2);
               this.a(x_, c, 29 + x_3, 6 + x_3, 21, x_2);
            }

            for(x_3 = 0; x_3 < 4; ++x_3) {
               this.a(x_, c, 28 - x_3, 9 + x_3, 21, x_2);
               this.a(x_, c, 29 + x_3, 9 + x_3, 21, x_2);
            }

            this.a(x_, c, 28, 12, 21, x_2);
            this.a(x_, c, 29, 12, 21, x_2);

            for(x_3 = 0; x_3 < 3; ++x_3) {
               this.a(x_, c, 22 - x_3 * 2, 8, 21, x_2);
               this.a(x_, c, 22 - x_3 * 2, 9, 21, x_2);
               this.a(x_, c, 35 + x_3 * 2, 8, 21, x_2);
               this.a(x_, c, 35 + x_3 * 2, 9, 21, x_2);
            }

            this.a(x_, x_2, 15, 13, 21, 42, 15, 21, false);
            this.a(x_, x_2, 15, 1, 21, 15, 6, 21, false);
            this.a(x_, x_2, 16, 1, 21, 16, 5, 21, false);
            this.a(x_, x_2, 17, 1, 21, 20, 4, 21, false);
            this.a(x_, x_2, 21, 1, 21, 21, 3, 21, false);
            this.a(x_, x_2, 22, 1, 21, 22, 2, 21, false);
            this.a(x_, x_2, 23, 1, 21, 24, 1, 21, false);
            this.a(x_, x_2, 42, 1, 21, 42, 6, 21, false);
            this.a(x_, x_2, 41, 1, 21, 41, 5, 21, false);
            this.a(x_, x_2, 37, 1, 21, 40, 4, 21, false);
            this.a(x_, x_2, 36, 1, 21, 36, 3, 21, false);
            this.a(x_, x_2, 33, 1, 21, 34, 1, 21, false);
            this.a(x_, x_2, 35, 1, 21, 35, 2, 21, false);
         }

      }

      private void d(World x_, Random x_1, Bbg x_2) {
         if(this.a(x_2, 21, 21, 36, 36)) {
            this.a(x_, x_2, 21, 0, 22, 36, 0, 36, a, a, false);
            this.a(x_, x_2, 21, 1, 22, 36, 23, 36, false);

            for(int x_3 = 0; x_3 < 4; ++x_3) {
               this.a(x_, x_2, 21 + x_3, 13 + x_3, 21 + x_3, 36 - x_3, 13 + x_3, 21 + x_3, b, b, false);
               this.a(x_, x_2, 21 + x_3, 13 + x_3, 36 - x_3, 36 - x_3, 13 + x_3, 36 - x_3, b, b, false);
               this.a(x_, x_2, 21 + x_3, 13 + x_3, 22 + x_3, 21 + x_3, 13 + x_3, 35 - x_3, b, b, false);
               this.a(x_, x_2, 36 - x_3, 13 + x_3, 22 + x_3, 36 - x_3, 13 + x_3, 35 - x_3, b, b, false);
            }

            this.a(x_, x_2, 25, 16, 25, 32, 16, 32, a, a, false);
            this.a(x_, x_2, 25, 17, 25, 25, 19, 25, b, b, false);
            this.a(x_, x_2, 32, 17, 25, 32, 19, 25, b, b, false);
            this.a(x_, x_2, 25, 17, 32, 25, 19, 32, b, b, false);
            this.a(x_, x_2, 32, 17, 32, 32, 19, 32, b, b, false);
            this.a(x_, b, 26, 20, 26, x_2);
            this.a(x_, b, 27, 21, 27, x_2);
            this.a(x_, e, 27, 20, 27, x_2);
            this.a(x_, b, 26, 20, 31, x_2);
            this.a(x_, b, 27, 21, 30, x_2);
            this.a(x_, e, 27, 20, 30, x_2);
            this.a(x_, b, 31, 20, 31, x_2);
            this.a(x_, b, 30, 21, 30, x_2);
            this.a(x_, e, 30, 20, 30, x_2);
            this.a(x_, b, 31, 20, 26, x_2);
            this.a(x_, b, 30, 21, 27, x_2);
            this.a(x_, e, 30, 20, 27, x_2);
            this.a(x_, x_2, 28, 21, 27, 29, 21, 27, a, a, false);
            this.a(x_, x_2, 27, 21, 28, 27, 21, 29, a, a, false);
            this.a(x_, x_2, 28, 21, 30, 29, 21, 30, a, a, false);
            this.a(x_, x_2, 30, 21, 28, 30, 21, 29, a, a, false);
         }

      }

      private void e(World x_, Random x_1, Bbg x_2) {
         int x_3;
         if(this.a(x_2, 0, 21, 6, 58)) {
            this.a(x_, x_2, 0, 0, 21, 6, 0, 57, a, a, false);
            this.a(x_, x_2, 0, 1, 21, 6, 7, 57, false);
            this.a(x_, x_2, 4, 4, 21, 6, 4, 53, a, a, false);

            for(x_3 = 0; x_3 < 4; ++x_3) {
               this.a(x_, x_2, x_3, x_3 + 1, 21, x_3, x_3 + 1, 57 - x_3, b, b, false);
            }

            for(x_3 = 23; x_3 < 53; x_3 += 3) {
               this.a(x_, d, 5, 5, x_3, x_2);
            }

            this.a(x_, d, 5, 5, 52, x_2);

            for(x_3 = 0; x_3 < 4; ++x_3) {
               this.a(x_, x_2, x_3, x_3 + 1, 21, x_3, x_3 + 1, 57 - x_3, b, b, false);
            }

            this.a(x_, x_2, 4, 1, 52, 6, 3, 52, a, a, false);
            this.a(x_, x_2, 5, 1, 51, 5, 3, 53, a, a, false);
         }

         if(this.a(x_2, 51, 21, 58, 58)) {
            this.a(x_, x_2, 51, 0, 21, 57, 0, 57, a, a, false);
            this.a(x_, x_2, 51, 1, 21, 57, 7, 57, false);
            this.a(x_, x_2, 51, 4, 21, 53, 4, 53, a, a, false);

            for(x_3 = 0; x_3 < 4; ++x_3) {
               this.a(x_, x_2, 57 - x_3, x_3 + 1, 21, 57 - x_3, x_3 + 1, 57 - x_3, b, b, false);
            }

            for(x_3 = 23; x_3 < 53; x_3 += 3) {
               this.a(x_, d, 52, 5, x_3, x_2);
            }

            this.a(x_, d, 52, 5, 52, x_2);
            this.a(x_, x_2, 51, 1, 52, 53, 3, 52, a, a, false);
            this.a(x_, x_2, 52, 1, 51, 52, 3, 53, a, a, false);
         }

         if(this.a(x_2, 0, 51, 57, 57)) {
            this.a(x_, x_2, 7, 0, 51, 50, 0, 57, a, a, false);
            this.a(x_, x_2, 7, 1, 51, 50, 10, 57, false);

            for(x_3 = 0; x_3 < 4; ++x_3) {
               this.a(x_, x_2, x_3 + 1, x_3 + 1, 57 - x_3, 56 - x_3, x_3 + 1, 57 - x_3, b, b, false);
            }
         }

      }

      private void f(World x_, Random x_1, Bbg x_2) {
         int x_3;
         if(this.a(x_2, 7, 21, 13, 50)) {
            this.a(x_, x_2, 7, 0, 21, 13, 0, 50, a, a, false);
            this.a(x_, x_2, 7, 1, 21, 13, 10, 50, false);
            this.a(x_, x_2, 11, 8, 21, 13, 8, 53, a, a, false);

            for(x_3 = 0; x_3 < 4; ++x_3) {
               this.a(x_, x_2, x_3 + 7, x_3 + 5, 21, x_3 + 7, x_3 + 5, 54, b, b, false);
            }

            for(x_3 = 21; x_3 <= 45; x_3 += 3) {
               this.a(x_, d, 12, 9, x_3, x_2);
            }
         }

         if(this.a(x_2, 44, 21, 50, 54)) {
            this.a(x_, x_2, 44, 0, 21, 50, 0, 50, a, a, false);
            this.a(x_, x_2, 44, 1, 21, 50, 10, 50, false);
            this.a(x_, x_2, 44, 8, 21, 46, 8, 53, a, a, false);

            for(x_3 = 0; x_3 < 4; ++x_3) {
               this.a(x_, x_2, 50 - x_3, x_3 + 5, 21, 50 - x_3, x_3 + 5, 54, b, b, false);
            }

            for(x_3 = 21; x_3 <= 45; x_3 += 3) {
               this.a(x_, d, 45, 9, x_3, x_2);
            }
         }

         if(this.a(x_2, 8, 44, 49, 54)) {
            this.a(x_, x_2, 14, 0, 44, 43, 0, 50, a, a, false);
            this.a(x_, x_2, 14, 1, 44, 43, 10, 50, false);

            for(x_3 = 12; x_3 <= 45; x_3 += 3) {
               this.a(x_, d, x_3, 9, 45, x_2);
               this.a(x_, d, x_3, 9, 52, x_2);
               if(x_3 == 12 || x_3 == 18 || x_3 == 24 || x_3 == 33 || x_3 == 39 || x_3 == 45) {
                  this.a(x_, d, x_3, 9, 47, x_2);
                  this.a(x_, d, x_3, 9, 50, x_2);
                  this.a(x_, d, x_3, 10, 45, x_2);
                  this.a(x_, d, x_3, 10, 46, x_2);
                  this.a(x_, d, x_3, 10, 51, x_2);
                  this.a(x_, d, x_3, 10, 52, x_2);
                  this.a(x_, d, x_3, 11, 47, x_2);
                  this.a(x_, d, x_3, 11, 50, x_2);
                  this.a(x_, d, x_3, 12, 48, x_2);
                  this.a(x_, d, x_3, 12, 49, x_2);
               }
            }

            for(x_3 = 0; x_3 < 3; ++x_3) {
               this.a(x_, x_2, 8 + x_3, 5 + x_3, 54, 49 - x_3, 5 + x_3, 54, a, a, false);
            }

            this.a(x_, x_2, 11, 8, 54, 46, 8, 54, b, b, false);
            this.a(x_, x_2, 14, 8, 44, 43, 8, 53, a, a, false);
         }

      }

      private void g(World x_, Random x_1, Bbg x_2) {
         int x_3;
         if(this.a(x_2, 14, 21, 20, 43)) {
            this.a(x_, x_2, 14, 0, 21, 20, 0, 43, a, a, false);
            this.a(x_, x_2, 14, 1, 22, 20, 14, 43, false);
            this.a(x_, x_2, 18, 12, 22, 20, 12, 39, a, a, false);
            this.a(x_, x_2, 18, 12, 21, 20, 12, 21, b, b, false);

            for(x_3 = 0; x_3 < 4; ++x_3) {
               this.a(x_, x_2, x_3 + 14, x_3 + 9, 21, x_3 + 14, x_3 + 9, 43 - x_3, b, b, false);
            }

            for(x_3 = 23; x_3 <= 39; x_3 += 3) {
               this.a(x_, d, 19, 13, x_3, x_2);
            }
         }

         if(this.a(x_2, 37, 21, 43, 43)) {
            this.a(x_, x_2, 37, 0, 21, 43, 0, 43, a, a, false);
            this.a(x_, x_2, 37, 1, 22, 43, 14, 43, false);
            this.a(x_, x_2, 37, 12, 22, 39, 12, 39, a, a, false);
            this.a(x_, x_2, 37, 12, 21, 39, 12, 21, b, b, false);

            for(x_3 = 0; x_3 < 4; ++x_3) {
               this.a(x_, x_2, 43 - x_3, x_3 + 9, 21, 43 - x_3, x_3 + 9, 43 - x_3, b, b, false);
            }

            for(x_3 = 23; x_3 <= 39; x_3 += 3) {
               this.a(x_, d, 38, 13, x_3, x_2);
            }
         }

         if(this.a(x_2, 15, 37, 42, 43)) {
            this.a(x_, x_2, 21, 0, 37, 36, 0, 43, a, a, false);
            this.a(x_, x_2, 21, 1, 37, 36, 14, 43, false);
            this.a(x_, x_2, 21, 12, 37, 36, 12, 39, a, a, false);

            for(x_3 = 0; x_3 < 4; ++x_3) {
               this.a(x_, x_2, 15 + x_3, x_3 + 9, 43 - x_3, 42 - x_3, x_3 + 9, 43 - x_3, b, b, false);
            }

            for(x_3 = 21; x_3 <= 36; x_3 += 3) {
               this.a(x_, d, x_3, 13, 38, x_2);
            }
         }

      }
   }

   interface i {

      boolean a(v var1);

      r a(Dir var1, v var2, Random var3);
   }

   public static class j extends r {

      public j() {}

      public j(Dir x_, v x_1, Random x_2) {
         super(1, x_, x_1, 2, 2, 2);
      }

      public boolean a(World x_, Random x_1, Bbg x_2) {
         this.a(x_, x_2, 1, 8, 0, 14, 8, 14, a);
         boolean x_3 = true;
         Bs x_4 = b;
         this.a(x_, x_2, 0, 7, 0, 0, 7, 15, x_4, x_4, false);
         this.a(x_, x_2, 15, 7, 0, 15, 7, 15, x_4, x_4, false);
         this.a(x_, x_2, 1, 7, 0, 15, 7, 0, x_4, x_4, false);
         this.a(x_, x_2, 1, 7, 15, 14, 7, 15, x_4, x_4, false);

         int var7;
         for(var7 = 1; var7 <= 6; ++var7) {
            x_4 = b;
            if(var7 == 2 || var7 == 6) {
               x_4 = a;
            }

            for(int x_5 = 0; x_5 <= 15; x_5 += 15) {
               this.a(x_, x_2, x_5, var7, 0, x_5, var7, 1, x_4, x_4, false);
               this.a(x_, x_2, x_5, var7, 6, x_5, var7, 9, x_4, x_4, false);
               this.a(x_, x_2, x_5, var7, 14, x_5, var7, 15, x_4, x_4, false);
            }

            this.a(x_, x_2, 1, var7, 0, 1, var7, 0, x_4, x_4, false);
            this.a(x_, x_2, 6, var7, 0, 9, var7, 0, x_4, x_4, false);
            this.a(x_, x_2, 14, var7, 0, 14, var7, 0, x_4, x_4, false);
            this.a(x_, x_2, 1, var7, 15, 14, var7, 15, x_4, x_4, false);
         }

         this.a(x_, x_2, 6, 3, 6, 9, 6, 9, c, c, false);
         this.a(x_, x_2, 7, 4, 7, 8, 5, 8, Bs.GOLD, Bs.GOLD, false);

         for(var7 = 3; var7 <= 6; var7 += 3) {
            for(int var8 = 6; var8 <= 9; var8 += 3) {
               this.a(x_, e, var8, var7, 6, x_2);
               this.a(x_, e, var8, var7, 9, x_2);
            }
         }

         this.a(x_, x_2, 5, 1, 6, 5, 2, 6, b, b, false);
         this.a(x_, x_2, 5, 1, 9, 5, 2, 9, b, b, false);
         this.a(x_, x_2, 10, 1, 6, 10, 2, 6, b, b, false);
         this.a(x_, x_2, 10, 1, 9, 10, 2, 9, b, b, false);
         this.a(x_, x_2, 6, 1, 5, 6, 2, 5, b, b, false);
         this.a(x_, x_2, 9, 1, 5, 9, 2, 5, b, b, false);
         this.a(x_, x_2, 6, 1, 10, 6, 2, 10, b, b, false);
         this.a(x_, x_2, 9, 1, 10, 9, 2, 10, b, b, false);
         this.a(x_, x_2, 5, 2, 5, 5, 6, 5, b, b, false);
         this.a(x_, x_2, 5, 2, 10, 5, 6, 10, b, b, false);
         this.a(x_, x_2, 10, 2, 5, 10, 6, 5, b, b, false);
         this.a(x_, x_2, 10, 2, 10, 10, 6, 10, b, b, false);
         this.a(x_, x_2, 5, 7, 1, 5, 7, 6, b, b, false);
         this.a(x_, x_2, 10, 7, 1, 10, 7, 6, b, b, false);
         this.a(x_, x_2, 5, 7, 9, 5, 7, 14, b, b, false);
         this.a(x_, x_2, 10, 7, 9, 10, 7, 14, b, b, false);
         this.a(x_, x_2, 1, 7, 5, 6, 7, 5, b, b, false);
         this.a(x_, x_2, 1, 7, 10, 6, 7, 10, b, b, false);
         this.a(x_, x_2, 9, 7, 5, 14, 7, 5, b, b, false);
         this.a(x_, x_2, 9, 7, 10, 14, 7, 10, b, b, false);
         this.a(x_, x_2, 2, 1, 2, 2, 1, 3, b, b, false);
         this.a(x_, x_2, 3, 1, 2, 3, 1, 2, b, b, false);
         this.a(x_, x_2, 13, 1, 2, 13, 1, 3, b, b, false);
         this.a(x_, x_2, 12, 1, 2, 12, 1, 2, b, b, false);
         this.a(x_, x_2, 2, 1, 12, 2, 1, 13, b, b, false);
         this.a(x_, x_2, 3, 1, 13, 3, 1, 13, b, b, false);
         this.a(x_, x_2, 13, 1, 12, 13, 1, 13, b, b, false);
         this.a(x_, x_2, 12, 1, 13, 12, 1, 13, b, b, false);
         return true;
      }
   }

   public static class k extends r {

      public k() {}

      public k(Dir x_, v x_1, Random x_2) {
         super(1, x_, x_1, 2, 1, 1);
      }

      public boolean a(World x_, Random x_1, Bbg x_2) {
         v x_3 = this.k.b[Dir.f.a()];
         v x_4 = this.k;
         if(this.k.a / 25 > 0) {
            this.a(x_, x_2, 8, 0, x_3.c[Dir.a.a()]);
            this.a(x_, x_2, 0, 0, x_4.c[Dir.a.a()]);
         }

         if(x_4.b[Dir.b.a()] == null) {
            this.a(x_, x_2, 1, 4, 1, 7, 4, 6, a);
         }

         if(x_3.b[Dir.b.a()] == null) {
            this.a(x_, x_2, 8, 4, 1, 14, 4, 6, a);
         }

         this.a(x_, x_2, 0, 3, 0, 0, 3, 7, b, b, false);
         this.a(x_, x_2, 15, 3, 0, 15, 3, 7, b, b, false);
         this.a(x_, x_2, 1, 3, 0, 15, 3, 0, b, b, false);
         this.a(x_, x_2, 1, 3, 7, 14, 3, 7, b, b, false);
         this.a(x_, x_2, 0, 2, 0, 0, 2, 7, a, a, false);
         this.a(x_, x_2, 15, 2, 0, 15, 2, 7, a, a, false);
         this.a(x_, x_2, 1, 2, 0, 15, 2, 0, a, a, false);
         this.a(x_, x_2, 1, 2, 7, 14, 2, 7, a, a, false);
         this.a(x_, x_2, 0, 1, 0, 0, 1, 7, b, b, false);
         this.a(x_, x_2, 15, 1, 0, 15, 1, 7, b, b, false);
         this.a(x_, x_2, 1, 1, 0, 15, 1, 0, b, b, false);
         this.a(x_, x_2, 1, 1, 7, 14, 1, 7, b, b, false);
         this.a(x_, x_2, 5, 1, 0, 10, 1, 4, b, b, false);
         this.a(x_, x_2, 6, 2, 0, 9, 2, 3, a, a, false);
         this.a(x_, x_2, 5, 3, 0, 10, 3, 4, b, b, false);
         this.a(x_, e, 6, 2, 3, x_2);
         this.a(x_, e, 9, 2, 3, x_2);
         if(x_4.c[Dir.d.a()]) {
            this.a(x_, x_2, 3, 1, 0, 4, 2, 0, false);
         }

         if(x_4.c[Dir.c.a()]) {
            this.a(x_, x_2, 3, 1, 7, 4, 2, 7, false);
         }

         if(x_4.c[Dir.e.a()]) {
            this.a(x_, x_2, 0, 1, 3, 0, 2, 4, false);
         }

         if(x_3.c[Dir.d.a()]) {
            this.a(x_, x_2, 11, 1, 0, 12, 2, 0, false);
         }

         if(x_3.c[Dir.c.a()]) {
            this.a(x_, x_2, 11, 1, 7, 12, 2, 7, false);
         }

         if(x_3.c[Dir.f.a()]) {
            this.a(x_, x_2, 15, 1, 3, 15, 2, 4, false);
         }

         return true;
      }
   }

   public static class l extends r {

      public l() {}

      public l(Dir x_, v x_1, Random x_2) {
         super(1, x_, x_1, 2, 2, 1);
      }

      public boolean a(World x_, Random x_1, Bbg x_2) {
         v x_3 = this.k.b[Dir.f.a()];
         v x_4 = this.k;
         v x_5 = x_4.b[Dir.b.a()];
         v x_6 = x_3.b[Dir.b.a()];
         if(this.k.a / 25 > 0) {
            this.a(x_, x_2, 8, 0, x_3.c[Dir.a.a()]);
            this.a(x_, x_2, 0, 0, x_4.c[Dir.a.a()]);
         }

         if(x_5.b[Dir.b.a()] == null) {
            this.a(x_, x_2, 1, 8, 1, 7, 8, 6, a);
         }

         if(x_6.b[Dir.b.a()] == null) {
            this.a(x_, x_2, 8, 8, 1, 14, 8, 6, a);
         }

         for(int x_7 = 1; x_7 <= 7; ++x_7) {
            Bs x_8 = b;
            if(x_7 == 2 || x_7 == 6) {
               x_8 = a;
            }

            this.a(x_, x_2, 0, x_7, 0, 0, x_7, 7, x_8, x_8, false);
            this.a(x_, x_2, 15, x_7, 0, 15, x_7, 7, x_8, x_8, false);
            this.a(x_, x_2, 1, x_7, 0, 15, x_7, 0, x_8, x_8, false);
            this.a(x_, x_2, 1, x_7, 7, 14, x_7, 7, x_8, x_8, false);
         }

         this.a(x_, x_2, 2, 1, 3, 2, 7, 4, b, b, false);
         this.a(x_, x_2, 3, 1, 2, 4, 7, 2, b, b, false);
         this.a(x_, x_2, 3, 1, 5, 4, 7, 5, b, b, false);
         this.a(x_, x_2, 13, 1, 3, 13, 7, 4, b, b, false);
         this.a(x_, x_2, 11, 1, 2, 12, 7, 2, b, b, false);
         this.a(x_, x_2, 11, 1, 5, 12, 7, 5, b, b, false);
         this.a(x_, x_2, 5, 1, 3, 5, 3, 4, b, b, false);
         this.a(x_, x_2, 10, 1, 3, 10, 3, 4, b, b, false);
         this.a(x_, x_2, 5, 7, 2, 10, 7, 5, b, b, false);
         this.a(x_, x_2, 5, 5, 2, 5, 7, 2, b, b, false);
         this.a(x_, x_2, 10, 5, 2, 10, 7, 2, b, b, false);
         this.a(x_, x_2, 5, 5, 5, 5, 7, 5, b, b, false);
         this.a(x_, x_2, 10, 5, 5, 10, 7, 5, b, b, false);
         this.a(x_, b, 6, 6, 2, x_2);
         this.a(x_, b, 9, 6, 2, x_2);
         this.a(x_, b, 6, 6, 5, x_2);
         this.a(x_, b, 9, 6, 5, x_2);
         this.a(x_, x_2, 5, 4, 3, 6, 4, 4, b, b, false);
         this.a(x_, x_2, 9, 4, 3, 10, 4, 4, b, b, false);
         this.a(x_, e, 5, 4, 2, x_2);
         this.a(x_, e, 5, 4, 5, x_2);
         this.a(x_, e, 10, 4, 2, x_2);
         this.a(x_, e, 10, 4, 5, x_2);
         if(x_4.c[Dir.d.a()]) {
            this.a(x_, x_2, 3, 1, 0, 4, 2, 0, false);
         }

         if(x_4.c[Dir.c.a()]) {
            this.a(x_, x_2, 3, 1, 7, 4, 2, 7, false);
         }

         if(x_4.c[Dir.e.a()]) {
            this.a(x_, x_2, 0, 1, 3, 0, 2, 4, false);
         }

         if(x_3.c[Dir.d.a()]) {
            this.a(x_, x_2, 11, 1, 0, 12, 2, 0, false);
         }

         if(x_3.c[Dir.c.a()]) {
            this.a(x_, x_2, 11, 1, 7, 12, 2, 7, false);
         }

         if(x_3.c[Dir.f.a()]) {
            this.a(x_, x_2, 15, 1, 3, 15, 2, 4, false);
         }

         if(x_5.c[Dir.d.a()]) {
            this.a(x_, x_2, 3, 5, 0, 4, 6, 0, false);
         }

         if(x_5.c[Dir.c.a()]) {
            this.a(x_, x_2, 3, 5, 7, 4, 6, 7, false);
         }

         if(x_5.c[Dir.e.a()]) {
            this.a(x_, x_2, 0, 5, 3, 0, 6, 4, false);
         }

         if(x_6.c[Dir.d.a()]) {
            this.a(x_, x_2, 11, 5, 0, 12, 6, 0, false);
         }

         if(x_6.c[Dir.c.a()]) {
            this.a(x_, x_2, 11, 5, 7, 12, 6, 7, false);
         }

         if(x_6.c[Dir.f.a()]) {
            this.a(x_, x_2, 15, 5, 3, 15, 6, 4, false);
         }

         return true;
      }
   }

   public static class m extends r {

      public m() {}

      public m(Dir x_, v x_1, Random x_2) {
         super(1, x_, x_1, 1, 2, 1);
      }

      public boolean a(World x_, Random x_1, Bbg x_2) {
         if(this.k.a / 25 > 0) {
            this.a(x_, x_2, 0, 0, this.k.c[Dir.a.a()]);
         }

         v x_3 = this.k.b[Dir.b.a()];
         if(x_3.b[Dir.b.a()] == null) {
            this.a(x_, x_2, 1, 8, 1, 6, 8, 6, a);
         }

         this.a(x_, x_2, 0, 4, 0, 0, 4, 7, b, b, false);
         this.a(x_, x_2, 7, 4, 0, 7, 4, 7, b, b, false);
         this.a(x_, x_2, 1, 4, 0, 6, 4, 0, b, b, false);
         this.a(x_, x_2, 1, 4, 7, 6, 4, 7, b, b, false);
         this.a(x_, x_2, 2, 4, 1, 2, 4, 2, b, b, false);
         this.a(x_, x_2, 1, 4, 2, 1, 4, 2, b, b, false);
         this.a(x_, x_2, 5, 4, 1, 5, 4, 2, b, b, false);
         this.a(x_, x_2, 6, 4, 2, 6, 4, 2, b, b, false);
         this.a(x_, x_2, 2, 4, 5, 2, 4, 6, b, b, false);
         this.a(x_, x_2, 1, 4, 5, 1, 4, 5, b, b, false);
         this.a(x_, x_2, 5, 4, 5, 5, 4, 6, b, b, false);
         this.a(x_, x_2, 6, 4, 5, 6, 4, 5, b, b, false);
         v x_4 = this.k;

         for(int x_5 = 1; x_5 <= 5; x_5 += 4) {
            byte x_6 = 0;
            if(x_4.c[Dir.d.a()]) {
               this.a(x_, x_2, 2, x_5, x_6, 2, x_5 + 2, x_6, b, b, false);
               this.a(x_, x_2, 5, x_5, x_6, 5, x_5 + 2, x_6, b, b, false);
               this.a(x_, x_2, 3, x_5 + 2, x_6, 4, x_5 + 2, x_6, b, b, false);
            } else {
               this.a(x_, x_2, 0, x_5, x_6, 7, x_5 + 2, x_6, b, b, false);
               this.a(x_, x_2, 0, x_5 + 1, x_6, 7, x_5 + 1, x_6, a, a, false);
            }

            x_6 = 7;
            if(x_4.c[Dir.c.a()]) {
               this.a(x_, x_2, 2, x_5, x_6, 2, x_5 + 2, x_6, b, b, false);
               this.a(x_, x_2, 5, x_5, x_6, 5, x_5 + 2, x_6, b, b, false);
               this.a(x_, x_2, 3, x_5 + 2, x_6, 4, x_5 + 2, x_6, b, b, false);
            } else {
               this.a(x_, x_2, 0, x_5, x_6, 7, x_5 + 2, x_6, b, b, false);
               this.a(x_, x_2, 0, x_5 + 1, x_6, 7, x_5 + 1, x_6, a, a, false);
            }

            byte x_7 = 0;
            if(x_4.c[Dir.e.a()]) {
               this.a(x_, x_2, x_7, x_5, 2, x_7, x_5 + 2, 2, b, b, false);
               this.a(x_, x_2, x_7, x_5, 5, x_7, x_5 + 2, 5, b, b, false);
               this.a(x_, x_2, x_7, x_5 + 2, 3, x_7, x_5 + 2, 4, b, b, false);
            } else {
               this.a(x_, x_2, x_7, x_5, 0, x_7, x_5 + 2, 7, b, b, false);
               this.a(x_, x_2, x_7, x_5 + 1, 0, x_7, x_5 + 1, 7, a, a, false);
            }

            x_7 = 7;
            if(x_4.c[Dir.f.a()]) {
               this.a(x_, x_2, x_7, x_5, 2, x_7, x_5 + 2, 2, b, b, false);
               this.a(x_, x_2, x_7, x_5, 5, x_7, x_5 + 2, 5, b, b, false);
               this.a(x_, x_2, x_7, x_5 + 2, 3, x_7, x_5 + 2, 4, b, b, false);
            } else {
               this.a(x_, x_2, x_7, x_5, 0, x_7, x_5 + 2, 7, b, b, false);
               this.a(x_, x_2, x_7, x_5 + 1, 0, x_7, x_5 + 1, 7, a, a, false);
            }

            x_4 = x_3;
         }

         return true;
      }
   }

   public static class n extends r {

      public n() {}

      public n(Dir x_, v x_1, Random x_2) {
         super(1, x_, x_1, 1, 2, 2);
      }

      public boolean a(World x_, Random x_1, Bbg x_2) {
         v x_3 = this.k.b[Dir.c.a()];
         v x_4 = this.k;
         v x_5 = x_3.b[Dir.b.a()];
         v x_6 = x_4.b[Dir.b.a()];
         if(this.k.a / 25 > 0) {
            this.a(x_, x_2, 0, 8, x_3.c[Dir.a.a()]);
            this.a(x_, x_2, 0, 0, x_4.c[Dir.a.a()]);
         }

         if(x_6.b[Dir.b.a()] == null) {
            this.a(x_, x_2, 1, 8, 1, 6, 8, 7, a);
         }

         if(x_5.b[Dir.b.a()] == null) {
            this.a(x_, x_2, 1, 8, 8, 6, 8, 14, a);
         }

         int x_7;
         Bs x_8;
         for(x_7 = 1; x_7 <= 7; ++x_7) {
            x_8 = b;
            if(x_7 == 2 || x_7 == 6) {
               x_8 = a;
            }

            this.a(x_, x_2, 0, x_7, 0, 0, x_7, 15, x_8, x_8, false);
            this.a(x_, x_2, 7, x_7, 0, 7, x_7, 15, x_8, x_8, false);
            this.a(x_, x_2, 1, x_7, 0, 6, x_7, 0, x_8, x_8, false);
            this.a(x_, x_2, 1, x_7, 15, 6, x_7, 15, x_8, x_8, false);
         }

         for(x_7 = 1; x_7 <= 7; ++x_7) {
            x_8 = c;
            if(x_7 == 2 || x_7 == 6) {
               x_8 = e;
            }

            this.a(x_, x_2, 3, x_7, 7, 4, x_7, 8, x_8, x_8, false);
         }

         if(x_4.c[Dir.d.a()]) {
            this.a(x_, x_2, 3, 1, 0, 4, 2, 0, false);
         }

         if(x_4.c[Dir.f.a()]) {
            this.a(x_, x_2, 7, 1, 3, 7, 2, 4, false);
         }

         if(x_4.c[Dir.e.a()]) {
            this.a(x_, x_2, 0, 1, 3, 0, 2, 4, false);
         }

         if(x_3.c[Dir.c.a()]) {
            this.a(x_, x_2, 3, 1, 15, 4, 2, 15, false);
         }

         if(x_3.c[Dir.e.a()]) {
            this.a(x_, x_2, 0, 1, 11, 0, 2, 12, false);
         }

         if(x_3.c[Dir.f.a()]) {
            this.a(x_, x_2, 7, 1, 11, 7, 2, 12, false);
         }

         if(x_6.c[Dir.d.a()]) {
            this.a(x_, x_2, 3, 5, 0, 4, 6, 0, false);
         }

         if(x_6.c[Dir.f.a()]) {
            this.a(x_, x_2, 7, 5, 3, 7, 6, 4, false);
            this.a(x_, x_2, 5, 4, 2, 6, 4, 5, b, b, false);
            this.a(x_, x_2, 6, 1, 2, 6, 3, 2, b, b, false);
            this.a(x_, x_2, 6, 1, 5, 6, 3, 5, b, b, false);
         }

         if(x_6.c[Dir.e.a()]) {
            this.a(x_, x_2, 0, 5, 3, 0, 6, 4, false);
            this.a(x_, x_2, 1, 4, 2, 2, 4, 5, b, b, false);
            this.a(x_, x_2, 1, 1, 2, 1, 3, 2, b, b, false);
            this.a(x_, x_2, 1, 1, 5, 1, 3, 5, b, b, false);
         }

         if(x_5.c[Dir.c.a()]) {
            this.a(x_, x_2, 3, 5, 15, 4, 6, 15, false);
         }

         if(x_5.c[Dir.e.a()]) {
            this.a(x_, x_2, 0, 5, 11, 0, 6, 12, false);
            this.a(x_, x_2, 1, 4, 10, 2, 4, 13, b, b, false);
            this.a(x_, x_2, 1, 1, 10, 1, 3, 10, b, b, false);
            this.a(x_, x_2, 1, 1, 13, 1, 3, 13, b, b, false);
         }

         if(x_5.c[Dir.f.a()]) {
            this.a(x_, x_2, 7, 5, 11, 7, 6, 12, false);
            this.a(x_, x_2, 5, 4, 10, 6, 4, 13, b, b, false);
            this.a(x_, x_2, 6, 1, 10, 6, 3, 10, b, b, false);
            this.a(x_, x_2, 6, 1, 13, 6, 3, 13, b, b, false);
         }

         return true;
      }
   }

   public static class o extends r {

      public o() {}

      public o(Dir x_, v x_1, Random x_2) {
         super(1, x_, x_1, 1, 1, 2);
      }

      public boolean a(World x_, Random x_1, Bbg x_2) {
         v x_3 = this.k.b[Dir.c.a()];
         v x_4 = this.k;
         if(this.k.a / 25 > 0) {
            this.a(x_, x_2, 0, 8, x_3.c[Dir.a.a()]);
            this.a(x_, x_2, 0, 0, x_4.c[Dir.a.a()]);
         }

         if(x_4.b[Dir.b.a()] == null) {
            this.a(x_, x_2, 1, 4, 1, 6, 4, 7, a);
         }

         if(x_3.b[Dir.b.a()] == null) {
            this.a(x_, x_2, 1, 4, 8, 6, 4, 14, a);
         }

         this.a(x_, x_2, 0, 3, 0, 0, 3, 15, b, b, false);
         this.a(x_, x_2, 7, 3, 0, 7, 3, 15, b, b, false);
         this.a(x_, x_2, 1, 3, 0, 7, 3, 0, b, b, false);
         this.a(x_, x_2, 1, 3, 15, 6, 3, 15, b, b, false);
         this.a(x_, x_2, 0, 2, 0, 0, 2, 15, a, a, false);
         this.a(x_, x_2, 7, 2, 0, 7, 2, 15, a, a, false);
         this.a(x_, x_2, 1, 2, 0, 7, 2, 0, a, a, false);
         this.a(x_, x_2, 1, 2, 15, 6, 2, 15, a, a, false);
         this.a(x_, x_2, 0, 1, 0, 0, 1, 15, b, b, false);
         this.a(x_, x_2, 7, 1, 0, 7, 1, 15, b, b, false);
         this.a(x_, x_2, 1, 1, 0, 7, 1, 0, b, b, false);
         this.a(x_, x_2, 1, 1, 15, 6, 1, 15, b, b, false);
         this.a(x_, x_2, 1, 1, 1, 1, 1, 2, b, b, false);
         this.a(x_, x_2, 6, 1, 1, 6, 1, 2, b, b, false);
         this.a(x_, x_2, 1, 3, 1, 1, 3, 2, b, b, false);
         this.a(x_, x_2, 6, 3, 1, 6, 3, 2, b, b, false);
         this.a(x_, x_2, 1, 1, 13, 1, 1, 14, b, b, false);
         this.a(x_, x_2, 6, 1, 13, 6, 1, 14, b, b, false);
         this.a(x_, x_2, 1, 3, 13, 1, 3, 14, b, b, false);
         this.a(x_, x_2, 6, 3, 13, 6, 3, 14, b, b, false);
         this.a(x_, x_2, 2, 1, 6, 2, 3, 6, b, b, false);
         this.a(x_, x_2, 5, 1, 6, 5, 3, 6, b, b, false);
         this.a(x_, x_2, 2, 1, 9, 2, 3, 9, b, b, false);
         this.a(x_, x_2, 5, 1, 9, 5, 3, 9, b, b, false);
         this.a(x_, x_2, 3, 2, 6, 4, 2, 6, b, b, false);
         this.a(x_, x_2, 3, 2, 9, 4, 2, 9, b, b, false);
         this.a(x_, x_2, 2, 2, 7, 2, 2, 8, b, b, false);
         this.a(x_, x_2, 5, 2, 7, 5, 2, 8, b, b, false);
         this.a(x_, e, 2, 2, 5, x_2);
         this.a(x_, e, 5, 2, 5, x_2);
         this.a(x_, e, 2, 2, 10, x_2);
         this.a(x_, e, 5, 2, 10, x_2);
         this.a(x_, b, 2, 3, 5, x_2);
         this.a(x_, b, 5, 3, 5, x_2);
         this.a(x_, b, 2, 3, 10, x_2);
         this.a(x_, b, 5, 3, 10, x_2);
         if(x_4.c[Dir.d.a()]) {
            this.a(x_, x_2, 3, 1, 0, 4, 2, 0, false);
         }

         if(x_4.c[Dir.f.a()]) {
            this.a(x_, x_2, 7, 1, 3, 7, 2, 4, false);
         }

         if(x_4.c[Dir.e.a()]) {
            this.a(x_, x_2, 0, 1, 3, 0, 2, 4, false);
         }

         if(x_3.c[Dir.c.a()]) {
            this.a(x_, x_2, 3, 1, 15, 4, 2, 15, false);
         }

         if(x_3.c[Dir.e.a()]) {
            this.a(x_, x_2, 0, 1, 11, 0, 2, 12, false);
         }

         if(x_3.c[Dir.f.a()]) {
            this.a(x_, x_2, 7, 1, 11, 7, 2, 12, false);
         }

         return true;
      }
   }

   public static class p extends r {

      public p() {}

      public p(Dir x_, v x_1) {
         super(1, x_, x_1, 1, 1, 1);
      }

      public boolean a(World x_, Random x_1, Bbg x_2) {
         this.a(x_, x_2, 0, 3, 0, 2, 3, 7, b, b, false);
         this.a(x_, x_2, 5, 3, 0, 7, 3, 7, b, b, false);
         this.a(x_, x_2, 0, 2, 0, 1, 2, 7, b, b, false);
         this.a(x_, x_2, 6, 2, 0, 7, 2, 7, b, b, false);
         this.a(x_, x_2, 0, 1, 0, 0, 1, 7, b, b, false);
         this.a(x_, x_2, 7, 1, 0, 7, 1, 7, b, b, false);
         this.a(x_, x_2, 0, 1, 7, 7, 3, 7, b, b, false);
         this.a(x_, x_2, 1, 1, 0, 2, 3, 0, b, b, false);
         this.a(x_, x_2, 5, 1, 0, 6, 3, 0, b, b, false);
         if(this.k.c[Dir.c.a()]) {
            this.a(x_, x_2, 3, 1, 7, 4, 2, 7, false);
         }

         if(this.k.c[Dir.e.a()]) {
            this.a(x_, x_2, 0, 1, 3, 1, 2, 4, false);
         }

         if(this.k.c[Dir.f.a()]) {
            this.a(x_, x_2, 6, 1, 3, 7, 2, 4, false);
         }

         return true;
      }
   }

   public static class q extends r {

      public q() {}

      public q(Dir x_, Bbg x_1) {
         super(x_, x_1);
      }

      public boolean a(World x_, Random x_1, Bbg x_2) {
         this.a(x_, x_2, 2, -1, 2, 11, -1, 11, b, b, false);
         this.a(x_, x_2, 0, -1, 0, 1, -1, 11, a, a, false);
         this.a(x_, x_2, 12, -1, 0, 13, -1, 11, a, a, false);
         this.a(x_, x_2, 2, -1, 0, 11, -1, 1, a, a, false);
         this.a(x_, x_2, 2, -1, 12, 11, -1, 13, a, a, false);
         this.a(x_, x_2, 0, 0, 0, 0, 0, 13, b, b, false);
         this.a(x_, x_2, 13, 0, 0, 13, 0, 13, b, b, false);
         this.a(x_, x_2, 1, 0, 0, 12, 0, 0, b, b, false);
         this.a(x_, x_2, 1, 0, 13, 12, 0, 13, b, b, false);

         for(int x_3 = 2; x_3 <= 11; x_3 += 3) {
            this.a(x_, e, 0, 0, x_3, x_2);
            this.a(x_, e, 13, 0, x_3, x_2);
            this.a(x_, e, x_3, 0, 0, x_2);
         }

         this.a(x_, x_2, 2, 0, 3, 4, 0, 9, b, b, false);
         this.a(x_, x_2, 9, 0, 3, 11, 0, 9, b, b, false);
         this.a(x_, x_2, 4, 0, 9, 9, 0, 11, b, b, false);
         this.a(x_, b, 5, 0, 8, x_2);
         this.a(x_, b, 8, 0, 8, x_2);
         this.a(x_, b, 10, 0, 10, x_2);
         this.a(x_, b, 3, 0, 10, x_2);
         this.a(x_, x_2, 3, 0, 3, 3, 0, 7, c, c, false);
         this.a(x_, x_2, 10, 0, 3, 10, 0, 7, c, c, false);
         this.a(x_, x_2, 6, 0, 10, 7, 0, 10, c, c, false);
         byte var7 = 3;

         for(int x_4 = 0; x_4 < 2; ++x_4) {
            for(int x_5 = 2; x_5 <= 8; x_5 += 3) {
               this.a(x_, x_2, var7, 0, x_5, var7, 2, x_5, b, b, false);
            }

            var7 = 10;
         }

         this.a(x_, x_2, 5, 0, 10, 5, 2, 10, b, b, false);
         this.a(x_, x_2, 8, 0, 10, 8, 2, 10, b, b, false);
         this.a(x_, x_2, 6, -1, 7, 7, -1, 8, c, c, false);
         this.a(x_, x_2, 6, -1, 3, 7, -1, 4, false);
         this.a(x_, x_2, 6, 1, 6);
         return true;
      }
   }

   public abstract static class r extends Bbx {

      protected static final Bs a = Bs.prismarine(0);
      protected static final Bs b = Bs.prismarine(1);
      protected static final Bs c = Bs.prismarine(2);
      protected static final Bs d = b;
      protected static final Bs e = Bs.LANTERN;
      protected static final Bs f = Bs.WATER;
      protected static final int g = b(2, 0, 0);
      protected static final int h = b(2, 2, 0);
      protected static final int i = b(0, 1, 0);
      protected static final int j = b(4, 1, 0);
      protected v k;


      protected static final int b(int x_, int x_1, int x_2) {
         return x_1 * 25 + x_2 * 5 + x_;
      }

      public r() {
         super(0);
      }

      public r(int x_) {
         super(x_);
      }

      public r(Dir x_, Bbg x_1) {
         super(1);
         this.a(x_);
         this.l = x_1;
      }

      protected r(int x_, Dir x_1, v x_2, int x_3, int x_4, int x_5) {
         super(x_);
         this.a(x_1);
         this.k = x_2;
         int x_6 = x_2.a;
         int x_7 = x_6 % 5;
         int x_8 = x_6 / 5 % 5;
         int x_9 = x_6 / 25;
         if(x_1 != Dir.c && x_1 != Dir.d) {
            this.l = new Bbg(0, 0, 0, x_5 * 8 - 1, x_4 * 4 - 1, x_3 * 8 - 1);
         } else {
            this.l = new Bbg(0, 0, 0, x_3 * 8 - 1, x_4 * 4 - 1, x_5 * 8 - 1);
         }

         switch(swKey(x_1)) {
         case 1:
            this.l.a(x_7 * 8, x_9 * 4, -(x_8 + x_5) * 8 + 1);
            break;
         case 2:
            this.l.a(x_7 * 8, x_9 * 4, x_8 * 8);
            break;
         case 3:
            this.l.a(-(x_8 + x_5) * 8 + 1, x_9 * 4, x_7 * 8);
            break;
         default:
            this.l.a(x_8 * 8, x_9 * 4, x_7 * 8);
         }

      }


      protected void a(World x_, Bbg x_1, int x_2, int x_3, int x_4, int x_5, int x_6, int x_7, boolean x_8) {
         for(int x_9 = x_3; x_9 <= x_6; ++x_9) {
            for(int x_10 = x_2; x_10 <= x_5; ++x_10) {
               for(int x_11 = x_4; x_11 <= x_7; ++x_11) {
                  if(!x_8 || this.a(x_, x_10, x_9, x_11, x_1).a() != net.minecraft.block.material.Material.air) {
                     if(this.d(x_9) >= 63) {
                        this.a(x_, Bs.AIR, x_10, x_9, x_11, x_1);
                     } else {
                        this.a(x_, f, x_10, x_9, x_11, x_1);
                     }
                  }
               }
            }
         }

      }

      protected void a(World x_, Bbg x_1, int x_2, int x_3, boolean x_4) {
         if(x_4) {
            this.a(x_, x_1, x_2 + 0, 0, x_3 + 0, x_2 + 2, 0, x_3 + 8 - 1, a, a, false);
            this.a(x_, x_1, x_2 + 5, 0, x_3 + 0, x_2 + 8 - 1, 0, x_3 + 8 - 1, a, a, false);
            this.a(x_, x_1, x_2 + 3, 0, x_3 + 0, x_2 + 4, 0, x_3 + 2, a, a, false);
            this.a(x_, x_1, x_2 + 3, 0, x_3 + 5, x_2 + 4, 0, x_3 + 8 - 1, a, a, false);
            this.a(x_, x_1, x_2 + 3, 0, x_3 + 2, x_2 + 4, 0, x_3 + 2, b, b, false);
            this.a(x_, x_1, x_2 + 3, 0, x_3 + 5, x_2 + 4, 0, x_3 + 5, b, b, false);
            this.a(x_, x_1, x_2 + 2, 0, x_3 + 3, x_2 + 2, 0, x_3 + 4, b, b, false);
            this.a(x_, x_1, x_2 + 5, 0, x_3 + 3, x_2 + 5, 0, x_3 + 4, b, b, false);
         } else {
            this.a(x_, x_1, x_2 + 0, 0, x_3 + 0, x_2 + 8 - 1, 0, x_3 + 8 - 1, a, a, false);
         }

      }

      protected void a(World x_, Bbg x_1, int x_2, int x_3, int x_4, int x_5, int x_6, int x_7, Bs x_8) {
         for(int x_9 = x_3; x_9 <= x_6; ++x_9) {
            for(int x_10 = x_2; x_10 <= x_5; ++x_10) {
               for(int x_11 = x_4; x_11 <= x_7; ++x_11) {
                  if(this.a(x_, x_10, x_9, x_11, x_1) == f) {
                     this.a(x_, x_8, x_10, x_9, x_11, x_1);
                  }
               }
            }
         }

      }

      protected boolean a(Bbg x_, int x_1, int x_2, int x_3, int x_4) {
         int x_5 = this.a(x_1, x_2);
         int x_6 = this.b(x_1, x_2);
         int x_7 = this.a(x_3, x_4);
         int x_8 = this.b(x_3, x_4);
         return x_.a(Math.min(x_5, x_7), Math.min(x_6, x_8), Math.max(x_5, x_7), Math.max(x_6, x_8));
      }

      protected boolean a(World x_, Bbg x_1, int x_2, int x_3, int x_4) {
         int x_5 = this.a(x_2, x_4);
         int x_6 = this.d(x_3);
         int x_7 = this.b(x_2, x_4);
         if(x_1.b(x_5, x_6, x_7)) {
            EntityElderGuardian x_8 = new EntityElderGuardian(x_);
            x_8.setLocationAndAngles((double)x_5 + 0.5D, (double)x_6, (double)x_7 + 0.5D, 0.0F, 0.0F);
            x_8.func_110163_bv();
            x_.spawnEntityInWorld(x_8);
            return true;
         } else {
            return false;
         }
      }

   }

   public static class s extends r {

      private int n;


      public s() {}

      public s(Dir x_, v x_1, Random x_2) {
         super(1, x_, x_1, 1, 1, 1);
         this.n = x_2.nextInt(3);
      }

      public boolean a(World x_, Random x_1, Bbg x_2) {
         if(this.k.a / 25 > 0) {
            this.a(x_, x_2, 0, 0, this.k.c[Dir.a.a()]);
         }

         if(this.k.b[Dir.b.a()] == null) {
            this.a(x_, x_2, 1, 4, 1, 6, 4, 6, a);
         }

         boolean x_3 = this.n != 0 && x_1.nextBoolean() && !this.k.c[Dir.a.a()] && !this.k.c[Dir.b.a()] && this.k.c() > 1;
         if(this.n == 0) {
            this.a(x_, x_2, 0, 1, 0, 2, 1, 2, b, b, false);
            this.a(x_, x_2, 0, 3, 0, 2, 3, 2, b, b, false);
            this.a(x_, x_2, 0, 2, 0, 0, 2, 2, a, a, false);
            this.a(x_, x_2, 1, 2, 0, 2, 2, 0, a, a, false);
            this.a(x_, e, 1, 2, 1, x_2);
            this.a(x_, x_2, 5, 1, 0, 7, 1, 2, b, b, false);
            this.a(x_, x_2, 5, 3, 0, 7, 3, 2, b, b, false);
            this.a(x_, x_2, 7, 2, 0, 7, 2, 2, a, a, false);
            this.a(x_, x_2, 5, 2, 0, 6, 2, 0, a, a, false);
            this.a(x_, e, 6, 2, 1, x_2);
            this.a(x_, x_2, 0, 1, 5, 2, 1, 7, b, b, false);
            this.a(x_, x_2, 0, 3, 5, 2, 3, 7, b, b, false);
            this.a(x_, x_2, 0, 2, 5, 0, 2, 7, a, a, false);
            this.a(x_, x_2, 1, 2, 7, 2, 2, 7, a, a, false);
            this.a(x_, e, 1, 2, 6, x_2);
            this.a(x_, x_2, 5, 1, 5, 7, 1, 7, b, b, false);
            this.a(x_, x_2, 5, 3, 5, 7, 3, 7, b, b, false);
            this.a(x_, x_2, 7, 2, 5, 7, 2, 7, a, a, false);
            this.a(x_, x_2, 5, 2, 7, 6, 2, 7, a, a, false);
            this.a(x_, e, 6, 2, 6, x_2);
            if(this.k.c[Dir.d.a()]) {
               this.a(x_, x_2, 3, 3, 0, 4, 3, 0, b, b, false);
            } else {
               this.a(x_, x_2, 3, 3, 0, 4, 3, 1, b, b, false);
               this.a(x_, x_2, 3, 2, 0, 4, 2, 0, a, a, false);
               this.a(x_, x_2, 3, 1, 0, 4, 1, 1, b, b, false);
            }

            if(this.k.c[Dir.c.a()]) {
               this.a(x_, x_2, 3, 3, 7, 4, 3, 7, b, b, false);
            } else {
               this.a(x_, x_2, 3, 3, 6, 4, 3, 7, b, b, false);
               this.a(x_, x_2, 3, 2, 7, 4, 2, 7, a, a, false);
               this.a(x_, x_2, 3, 1, 6, 4, 1, 7, b, b, false);
            }

            if(this.k.c[Dir.e.a()]) {
               this.a(x_, x_2, 0, 3, 3, 0, 3, 4, b, b, false);
            } else {
               this.a(x_, x_2, 0, 3, 3, 1, 3, 4, b, b, false);
               this.a(x_, x_2, 0, 2, 3, 0, 2, 4, a, a, false);
               this.a(x_, x_2, 0, 1, 3, 1, 1, 4, b, b, false);
            }

            if(this.k.c[Dir.f.a()]) {
               this.a(x_, x_2, 7, 3, 3, 7, 3, 4, b, b, false);
            } else {
               this.a(x_, x_2, 6, 3, 3, 7, 3, 4, b, b, false);
               this.a(x_, x_2, 7, 2, 3, 7, 2, 4, a, a, false);
               this.a(x_, x_2, 6, 1, 3, 7, 1, 4, b, b, false);
            }
         } else if(this.n == 1) {
            this.a(x_, x_2, 2, 1, 2, 2, 3, 2, b, b, false);
            this.a(x_, x_2, 2, 1, 5, 2, 3, 5, b, b, false);
            this.a(x_, x_2, 5, 1, 5, 5, 3, 5, b, b, false);
            this.a(x_, x_2, 5, 1, 2, 5, 3, 2, b, b, false);
            this.a(x_, e, 2, 2, 2, x_2);
            this.a(x_, e, 2, 2, 5, x_2);
            this.a(x_, e, 5, 2, 5, x_2);
            this.a(x_, e, 5, 2, 2, x_2);
            this.a(x_, x_2, 0, 1, 0, 1, 3, 0, b, b, false);
            this.a(x_, x_2, 0, 1, 1, 0, 3, 1, b, b, false);
            this.a(x_, x_2, 0, 1, 7, 1, 3, 7, b, b, false);
            this.a(x_, x_2, 0, 1, 6, 0, 3, 6, b, b, false);
            this.a(x_, x_2, 6, 1, 7, 7, 3, 7, b, b, false);
            this.a(x_, x_2, 7, 1, 6, 7, 3, 6, b, b, false);
            this.a(x_, x_2, 6, 1, 0, 7, 3, 0, b, b, false);
            this.a(x_, x_2, 7, 1, 1, 7, 3, 1, b, b, false);
            this.a(x_, a, 1, 2, 0, x_2);
            this.a(x_, a, 0, 2, 1, x_2);
            this.a(x_, a, 1, 2, 7, x_2);
            this.a(x_, a, 0, 2, 6, x_2);
            this.a(x_, a, 6, 2, 7, x_2);
            this.a(x_, a, 7, 2, 6, x_2);
            this.a(x_, a, 6, 2, 0, x_2);
            this.a(x_, a, 7, 2, 1, x_2);
            if(!this.k.c[Dir.d.a()]) {
               this.a(x_, x_2, 1, 3, 0, 6, 3, 0, b, b, false);
               this.a(x_, x_2, 1, 2, 0, 6, 2, 0, a, a, false);
               this.a(x_, x_2, 1, 1, 0, 6, 1, 0, b, b, false);
            }

            if(!this.k.c[Dir.c.a()]) {
               this.a(x_, x_2, 1, 3, 7, 6, 3, 7, b, b, false);
               this.a(x_, x_2, 1, 2, 7, 6, 2, 7, a, a, false);
               this.a(x_, x_2, 1, 1, 7, 6, 1, 7, b, b, false);
            }

            if(!this.k.c[Dir.e.a()]) {
               this.a(x_, x_2, 0, 3, 1, 0, 3, 6, b, b, false);
               this.a(x_, x_2, 0, 2, 1, 0, 2, 6, a, a, false);
               this.a(x_, x_2, 0, 1, 1, 0, 1, 6, b, b, false);
            }

            if(!this.k.c[Dir.f.a()]) {
               this.a(x_, x_2, 7, 3, 1, 7, 3, 6, b, b, false);
               this.a(x_, x_2, 7, 2, 1, 7, 2, 6, a, a, false);
               this.a(x_, x_2, 7, 1, 1, 7, 1, 6, b, b, false);
            }
         } else if(this.n == 2) {
            this.a(x_, x_2, 0, 1, 0, 0, 1, 7, b, b, false);
            this.a(x_, x_2, 7, 1, 0, 7, 1, 7, b, b, false);
            this.a(x_, x_2, 1, 1, 0, 6, 1, 0, b, b, false);
            this.a(x_, x_2, 1, 1, 7, 6, 1, 7, b, b, false);
            this.a(x_, x_2, 0, 2, 0, 0, 2, 7, c, c, false);
            this.a(x_, x_2, 7, 2, 0, 7, 2, 7, c, c, false);
            this.a(x_, x_2, 1, 2, 0, 6, 2, 0, c, c, false);
            this.a(x_, x_2, 1, 2, 7, 6, 2, 7, c, c, false);
            this.a(x_, x_2, 0, 3, 0, 0, 3, 7, b, b, false);
            this.a(x_, x_2, 7, 3, 0, 7, 3, 7, b, b, false);
            this.a(x_, x_2, 1, 3, 0, 6, 3, 0, b, b, false);
            this.a(x_, x_2, 1, 3, 7, 6, 3, 7, b, b, false);
            this.a(x_, x_2, 0, 1, 3, 0, 2, 4, c, c, false);
            this.a(x_, x_2, 7, 1, 3, 7, 2, 4, c, c, false);
            this.a(x_, x_2, 3, 1, 0, 4, 2, 0, c, c, false);
            this.a(x_, x_2, 3, 1, 7, 4, 2, 7, c, c, false);
            if(this.k.c[Dir.d.a()]) {
               this.a(x_, x_2, 3, 1, 0, 4, 2, 0, false);
            }

            if(this.k.c[Dir.c.a()]) {
               this.a(x_, x_2, 3, 1, 7, 4, 2, 7, false);
            }

            if(this.k.c[Dir.e.a()]) {
               this.a(x_, x_2, 0, 1, 3, 0, 2, 4, false);
            }

            if(this.k.c[Dir.f.a()]) {
               this.a(x_, x_2, 7, 1, 3, 7, 2, 4, false);
            }
         }

         if(x_3) {
            this.a(x_, x_2, 3, 1, 3, 4, 1, 4, b, b, false);
            this.a(x_, x_2, 3, 2, 3, 4, 2, 4, a, a, false);
            this.a(x_, x_2, 3, 3, 3, 4, 3, 4, b, b, false);
         }

         return true;
      }
   }

   public static class t extends r {

      public t() {}

      public t(Dir x_, v x_1, Random x_2) {
         super(1, x_, x_1, 1, 1, 1);
      }

      public boolean a(World x_, Random x_1, Bbg x_2) {
         if(this.k.a / 25 > 0) {
            this.a(x_, x_2, 0, 0, this.k.c[Dir.a.a()]);
         }

         if(this.k.b[Dir.b.a()] == null) {
            this.a(x_, x_2, 1, 4, 1, 6, 4, 6, a);
         }

         for(int x_3 = 1; x_3 <= 6; ++x_3) {
            for(int x_4 = 1; x_4 <= 6; ++x_4) {
               if(x_1.nextInt(3) != 0) {
                  int x_5 = 2 + (x_1.nextInt(4) == 0?0:1);
                  this.a(x_, x_2, x_3, x_5, x_4, x_3, 3, x_4, Bs.WET_SPONGE, Bs.WET_SPONGE, false);
               }
            }
         }

         this.a(x_, x_2, 0, 1, 0, 0, 1, 7, b, b, false);
         this.a(x_, x_2, 7, 1, 0, 7, 1, 7, b, b, false);
         this.a(x_, x_2, 1, 1, 0, 6, 1, 0, b, b, false);
         this.a(x_, x_2, 1, 1, 7, 6, 1, 7, b, b, false);
         this.a(x_, x_2, 0, 2, 0, 0, 2, 7, c, c, false);
         this.a(x_, x_2, 7, 2, 0, 7, 2, 7, c, c, false);
         this.a(x_, x_2, 1, 2, 0, 6, 2, 0, c, c, false);
         this.a(x_, x_2, 1, 2, 7, 6, 2, 7, c, c, false);
         this.a(x_, x_2, 0, 3, 0, 0, 3, 7, b, b, false);
         this.a(x_, x_2, 7, 3, 0, 7, 3, 7, b, b, false);
         this.a(x_, x_2, 1, 3, 0, 6, 3, 0, b, b, false);
         this.a(x_, x_2, 1, 3, 7, 6, 3, 7, b, b, false);
         this.a(x_, x_2, 0, 1, 3, 0, 2, 4, c, c, false);
         this.a(x_, x_2, 7, 1, 3, 7, 2, 4, c, c, false);
         this.a(x_, x_2, 3, 1, 0, 4, 2, 0, c, c, false);
         this.a(x_, x_2, 3, 1, 7, 4, 2, 7, c, c, false);
         if(this.k.c[Dir.d.a()]) {
            this.a(x_, x_2, 3, 1, 0, 4, 2, 0, false);
         }

         return true;
      }
   }

   public static class u extends r {

      private int n;


      public u() {}

      public u(Dir x_, Bbg x_1, int x_2) {
         super(x_, x_1);
         this.n = x_2 & 1;
      }

      public boolean a(World x_, Random x_1, Bbg x_2) {
         if(this.n == 0) {
            int x_3;
            for(x_3 = 0; x_3 < 4; ++x_3) {
               this.a(x_, x_2, 10 - x_3, 3 - x_3, 20 - x_3, 12 + x_3, 3 - x_3, 20, b, b, false);
            }

            this.a(x_, x_2, 7, 0, 6, 15, 0, 16, b, b, false);
            this.a(x_, x_2, 6, 0, 6, 6, 3, 20, b, b, false);
            this.a(x_, x_2, 16, 0, 6, 16, 3, 20, b, b, false);
            this.a(x_, x_2, 7, 1, 7, 7, 1, 20, b, b, false);
            this.a(x_, x_2, 15, 1, 7, 15, 1, 20, b, b, false);
            this.a(x_, x_2, 7, 1, 6, 9, 3, 6, b, b, false);
            this.a(x_, x_2, 13, 1, 6, 15, 3, 6, b, b, false);
            this.a(x_, x_2, 8, 1, 7, 9, 1, 7, b, b, false);
            this.a(x_, x_2, 13, 1, 7, 14, 1, 7, b, b, false);
            this.a(x_, x_2, 9, 0, 5, 13, 0, 5, b, b, false);
            this.a(x_, x_2, 10, 0, 7, 12, 0, 7, c, c, false);
            this.a(x_, x_2, 8, 0, 10, 8, 0, 12, c, c, false);
            this.a(x_, x_2, 14, 0, 10, 14, 0, 12, c, c, false);

            for(x_3 = 18; x_3 >= 7; x_3 -= 3) {
               this.a(x_, e, 6, 3, x_3, x_2);
               this.a(x_, e, 16, 3, x_3, x_2);
            }

            this.a(x_, e, 10, 0, 10, x_2);
            this.a(x_, e, 12, 0, 10, x_2);
            this.a(x_, e, 10, 0, 12, x_2);
            this.a(x_, e, 12, 0, 12, x_2);
            this.a(x_, e, 8, 3, 6, x_2);
            this.a(x_, e, 14, 3, 6, x_2);
            this.a(x_, b, 4, 2, 4, x_2);
            this.a(x_, e, 4, 1, 4, x_2);
            this.a(x_, b, 4, 0, 4, x_2);
            this.a(x_, b, 18, 2, 4, x_2);
            this.a(x_, e, 18, 1, 4, x_2);
            this.a(x_, b, 18, 0, 4, x_2);
            this.a(x_, b, 4, 2, 18, x_2);
            this.a(x_, e, 4, 1, 18, x_2);
            this.a(x_, b, 4, 0, 18, x_2);
            this.a(x_, b, 18, 2, 18, x_2);
            this.a(x_, e, 18, 1, 18, x_2);
            this.a(x_, b, 18, 0, 18, x_2);
            this.a(x_, b, 9, 7, 20, x_2);
            this.a(x_, b, 13, 7, 20, x_2);
            this.a(x_, x_2, 6, 0, 21, 7, 4, 21, b, b, false);
            this.a(x_, x_2, 15, 0, 21, 16, 4, 21, b, b, false);
            this.a(x_, x_2, 11, 2, 16);
         } else if(this.n == 1) {
            this.a(x_, x_2, 9, 3, 18, 13, 3, 20, b, b, false);
            this.a(x_, x_2, 9, 0, 18, 9, 2, 18, b, b, false);
            this.a(x_, x_2, 13, 0, 18, 13, 2, 18, b, b, false);
            byte var8 = 9;
            boolean x_4 = true;
            boolean x_5 = true;

            int x_6;
            for(x_6 = 0; x_6 < 2; ++x_6) {
               this.a(x_, b, var8, 6, 20, x_2);
               this.a(x_, e, var8, 5, 20, x_2);
               this.a(x_, b, var8, 4, 20, x_2);
               var8 = 13;
            }

            this.a(x_, x_2, 7, 3, 7, 15, 3, 14, b, b, false);
            var8 = 10;

            for(x_6 = 0; x_6 < 2; ++x_6) {
               this.a(x_, x_2, var8, 0, 10, var8, 6, 10, b, b, false);
               this.a(x_, x_2, var8, 0, 12, var8, 6, 12, b, b, false);
               this.a(x_, e, var8, 0, 10, x_2);
               this.a(x_, e, var8, 0, 12, x_2);
               this.a(x_, e, var8, 4, 10, x_2);
               this.a(x_, e, var8, 4, 12, x_2);
               var8 = 12;
            }

            var8 = 8;

            for(x_6 = 0; x_6 < 2; ++x_6) {
               this.a(x_, x_2, var8, 0, 7, var8, 2, 7, b, b, false);
               this.a(x_, x_2, var8, 0, 14, var8, 2, 14, b, b, false);
               var8 = 14;
            }

            this.a(x_, x_2, 8, 3, 8, 8, 3, 13, c, c, false);
            this.a(x_, x_2, 14, 3, 8, 14, 3, 13, c, c, false);
            this.a(x_, x_2, 11, 5, 13);
         }

         return true;
      }
   }

   static class v {

      int a;
      v[] b = new v[6];
      boolean[] c = new boolean[6];
      boolean d;
      boolean e;
      int f;


      public v(int x_) {
         this.a = x_;
      }

      public void a(Dir x_, v x_1) {
         this.b[x_.a()] = x_1;
         x_1.b[x_.d().a()] = this;
      }

      public void a() {
         for(int x_ = 0; x_ < 6; ++x_) {
            this.c[x_] = this.b[x_] != null;
         }

      }

      public boolean a(int x_) {
         if(this.e) {
            return true;
         } else {
            this.f = x_;

            for(int x_1 = 0; x_1 < 6; ++x_1) {
               if(this.b[x_1] != null && this.c[x_1] && this.b[x_1].f != x_ && this.b[x_1].a(x_)) {
                  return true;
               }
            }

            return false;
         }
      }

      public boolean b() {
         return this.a >= 75;
      }

      public int c() {
         int x_ = 0;

         for(int x_1 = 0; x_1 < 6; ++x_1) {
            if(this.c[x_1]) {
               ++x_;
            }
         }

         return x_;
      }
   }
}
