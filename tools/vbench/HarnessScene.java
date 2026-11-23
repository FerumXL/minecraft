import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

/**
 * Plays scenes.txt in vanilla 1.12.2 (names are the obfuscated ones of 1.12.2): a flat world of seed 112, then the
 * commands, waits and screenshots. The last two arguments: the scene file and the screenshot folder.
 */
public final class HarnessScene {
    static File out;

    static void onMain(final Runnable r) throws Exception {
        bib.z().a(r).get();
    }

    static void cmd(final String c) throws Exception {
        onMain(new Runnable() {
            public void run() {
                bib.z().h.g("/" + c);
            }
        });
        Thread.sleep(120);
    }

    static void shot(final String name) throws Exception {
        onMain(new Runnable() {
            public void run() {
                bib mc = bib.z();
                bif.a(out, "s-" + name + ".png", mc.d, mc.e, mc.b());
            }
        });
        Thread.sleep(300);
    }

    public static void main(final String[] args) throws Exception {
        final File scene = new File(args[args.length - 2]);
        out = new File(args[args.length - 1]);
        final String[] real = new String[args.length - 2];
        System.arraycopy(args, 0, real, 0, real.length);
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    play(scene);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }
        });
        t.setDaemon(true);
        t.start();
        net.minecraft.client.main.Main.main(real);
    }

    static void play(File scene) throws Exception {
        while (bib.z() == null || bib.z().m == null) {
            Thread.sleep(500);
        }
        Thread.sleep(6000);
        onMain(new Runnable() {
            public void run() {
                bib.z().t.A = false;
                bib.z().a("v112", "v112", new amx(112L, ams.a(1), false, false, amz.c).a().b());
            }
        });
        while (bib.z().h == null) {
            Thread.sleep(500);
        }
        Thread.sleep(8000);
        List<String> lines = Files.readAllLines(scene.toPath(), Charset.forName("UTF-8"));
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int sp = line.indexOf(' ');
            String op = sp < 0 ? line : line.substring(0, sp);
            final String arg = sp < 0 ? "" : line.substring(sp + 1);
            if ("cmd".equals(op)) {
                cmd(arg);
            } else if ("wait".equals(op)) {
                Thread.sleep(Integer.parseInt(arg) * 50L);
            } else if ("shot".equals(op)) {
                shot(arg);
            } else if ("hud".equals(op)) {
                onMain(new Runnable() {
                    public void run() {
                        bib.z().t.av = "off".equals(arg);
                    }
                });
                Thread.sleep(200);
            } else if ("view".equals(op)) {
                onMain(new Runnable() {
                    public void run() {
                        bib.z().t.aw = Integer.parseInt(arg);
                    }
                });
                Thread.sleep(200);
            } else if ("gui".equals(op)) {
                if ("close".equals(arg)) {
                    onMain(new Runnable() {
                        public void run() {
                            bib.z().a((blk) null);
                        }
                    });
                } else {
                    onMain(new Runnable() {
                        public void run() {
                            bhy.a(18);
                        }
                    });
                }
                Thread.sleep(300);
            }
            System.out.println("[scene] " + line);
        }
        Thread.sleep(1000);
    }
}
