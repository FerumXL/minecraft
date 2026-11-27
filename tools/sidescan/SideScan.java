import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Finds what a dedicated server would miss: references from the common classes of the mod to classes, methods and
 * fields that Forge 1.7.10 marks @SideOnly(Side.CLIENT) (FML strips them on a server).
 *
 * java -cp asm-debug-all.jar;. SideScan forgeBin.jar build/classes/main ru/codex/offhand1710/client/
 */
public final class SideScan {
    private static final String SIDE_ONLY = "Lcpw/mods/fml/relauncher/SideOnly;";
    private static final Set<String> CLIENT_CLASSES = new HashSet<String>();
    private static final Set<String> CLIENT_MEMBERS = new HashSet<String>();
    /** name + desc of client-only methods: a mod method with the same one overrides it and runs only on a client. */
    private static final Set<String> CLIENT_SIGS = new HashSet<String>();

    public static void main(String[] args) throws IOException {
        ZipFile forge = new ZipFile(args[0]);
        for (Enumeration<? extends ZipEntry> e = forge.entries(); e.hasMoreElements();) {
            ZipEntry z = e.nextElement();
            if (z.getName().endsWith(".class")) {
                InputStream in = forge.getInputStream(z);
                collect(read(in));
                in.close();
            }
        }
        forge.close();
        Set<String> found = new TreeSet<String>();
        scan(new File(args[1]), args[1].length() + 1, args[2], found);
        for (String f : found) {
            System.out.println(f);
        }
        System.out.println(found.size() + " client-only references in common classes");
    }

    private static ClassNode read(InputStream in) throws IOException {
        ClassNode node = new ClassNode();
        new ClassReader(in).accept(node, ClassReader.SKIP_FRAMES);
        return node;
    }

    private static boolean client(java.util.List<AnnotationNode> list) {
        if (list != null) {
            for (AnnotationNode a : list) {
                if (SIDE_ONLY.equals(a.desc) && a.values != null && String.valueOf(java.util.Arrays.asList(((String[]) a.values.get(1)))).contains("CLIENT")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void collect(ClassNode c) {
        if (client(c.visibleAnnotations) || client(c.invisibleAnnotations)) {
            CLIENT_CLASSES.add(c.name);
        }
        for (MethodNode m : c.methods) {
            if (client(m.visibleAnnotations) || client(m.invisibleAnnotations)) {
                CLIENT_MEMBERS.add(c.name + "." + m.name + m.desc);
                CLIENT_SIGS.add(m.name + m.desc);
            }
        }
        for (FieldNode f : c.fields) {
            if (client(f.visibleAnnotations) || client(f.invisibleAnnotations)) {
                CLIENT_MEMBERS.add(c.name + "." + f.name);
            }
        }
    }

    private static void scan(File dir, int cut, String clientPackage, Set<String> found) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                scan(f, cut, clientPackage, found);
            } else if (f.getName().endsWith(".class")) {
                String rel = f.getPath().substring(cut).replace('\\', '/');
                if (rel.startsWith(clientPackage)) {
                    continue;
                }
                FileInputStream in = new FileInputStream(f);
                ClassNode c = read(in);
                in.close();
                if (client(c.visibleAnnotations) || client(c.invisibleAnnotations)) {
                    continue;
                }
                for (MethodNode m : c.methods) {
                    if (client(m.visibleAnnotations) || client(m.invisibleAnnotations) || CLIENT_SIGS.contains(m.name + m.desc)) {
                        continue;
                    }
                    for (AbstractInsnNode a = m.instructions.getFirst(); a != null; a = a.getNext()) {
                        String hit = null;
                        if (a instanceof MethodInsnNode) {
                            MethodInsnNode mi = (MethodInsnNode) a;
                            hit = CLIENT_CLASSES.contains(mi.owner) ? mi.owner : CLIENT_MEMBERS.contains(mi.owner + "." + mi.name + mi.desc) ? mi.owner + "." + mi.name : null;
                        } else if (a instanceof FieldInsnNode) {
                            FieldInsnNode fi = (FieldInsnNode) a;
                            hit = CLIENT_CLASSES.contains(fi.owner) ? fi.owner : CLIENT_MEMBERS.contains(fi.owner + "." + fi.name) ? fi.owner + "." + fi.name : null;
                        } else if (a instanceof TypeInsnNode) {
                            hit = CLIENT_CLASSES.contains(((TypeInsnNode) a).desc) ? ((TypeInsnNode) a).desc : null;
                        } else if (a instanceof LdcInsnNode && ((LdcInsnNode) a).cst instanceof Type) {
                            String n = ((Type) ((LdcInsnNode) a).cst).getInternalName();
                            hit = CLIENT_CLASSES.contains(n) ? n : null;
                        }
                        if (hit != null) {
                            found.add(c.name + "." + m.name + " -> " + hit);
                        }
                    }
                }
            }
        }
    }
}
