package ru.codex.offhand1710.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Where the advancements of one tab stand: the column is the depth in the tree, the row is found by the tidy tree
 * algorithm of Reingold and Tilford in the linear form of Buchheim, Juenger and Leipert, the one 1.12 uses
 * (class "t" of 1.12.2.jar). Written from the description of the algorithm; the few places where 1.12 departs from
 * the paper are kept and marked, because they decide the rows.
 *
 * No Minecraft classes here, so that the layout can be run and checked without the game.
 */
public final class TreeLayout {
    private TreeLayout() {
    }

    public static class Node {
        public Node parent;
        public final List<Node> kids = new ArrayList<Node>();
        /** Column, from 0 at the root. */
        public int depth;
        /** Row, fractional: a parent stands in the middle between its first and last child. */
        public float row;

        private Node before;
        private int number;
        private Node ancestor;
        private Node thread;
        private float mod;
        private float change;
        private float shift;

        private Node left() {
            return thread != null ? thread : (kids.isEmpty() ? null : kids.get(0));
        }

        private Node right() {
            // 1.12 looks at the thread first on both sides
            return thread != null ? thread : (kids.isEmpty() ? null : kids.get(kids.size() - 1));
        }
    }

    public static void run(Node root) {
        prepare(root, null, 1);
        rows(root);
        float least = settle(root, 0.0F, 0, root.row);
        if (least < 0.0F) {
            lower(root, -least);
        }
    }

    private static void prepare(Node n, Node before, int number) {
        n.before = before;
        n.number = number;
        n.ancestor = n;
        n.thread = null;
        n.mod = 0.0F;
        n.change = 0.0F;
        n.shift = 0.0F;
        n.row = -1.0F;
        Node last = null;
        for (int i = 0; i < n.kids.size(); i++) {
            Node k = n.kids.get(i);
            k.parent = n;
            prepare(k, last, i + 1);
            last = k;
        }
    }

    /** Bottom up: rows relative to the parent. */
    private static void rows(Node v) {
        if (v.kids.isEmpty()) {
            v.row = v.before != null ? v.before.row + 1.0F : 0.0F;
            return;
        }
        Node fallback = null;
        for (Node k : v.kids) {
            rows(k);
            fallback = separate(k, fallback == null ? k : fallback);
        }
        spread(v);
        float middle = (v.kids.get(0).row + v.kids.get(v.kids.size() - 1).row) / 2.0F;
        if (v.before != null) {
            v.row = v.before.row + 1.0F;
            v.mod = v.row - middle;
        } else {
            v.row = middle;
        }
    }

    /** Pushes the subtree of v away from the subtrees of its earlier siblings, one row apart at every depth. */
    private static Node separate(Node v, Node fallback) {
        if (v.before == null) {
            return fallback;
        }
        Node innerNew = v;
        Node outerNew = v;
        Node innerOld = v.before;
        Node outerOld = v.parent.kids.get(0);
        float sumInnerNew = v.mod;
        float sumOuterNew = v.mod;
        float sumInnerOld = innerOld.mod;
        float sumOuterOld = outerOld.mod;
        while (innerOld.right() != null && innerNew.left() != null) {
            innerOld = innerOld.right();
            innerNew = innerNew.left();
            outerOld = outerOld.left();
            outerNew = outerNew.right();
            outerNew.ancestor = v;
            float gap = innerOld.row + sumInnerOld - (innerNew.row + sumInnerNew) + 1.0F;
            if (gap > 0.0F) {
                Node from = innerOld.ancestor != null && v.parent.kids.contains(innerOld.ancestor) ? innerOld.ancestor : fallback;
                move(from, v, gap);
                sumInnerNew += gap;
                sumOuterNew += gap;
            }
            sumInnerOld += innerOld.mod;
            sumInnerNew += innerNew.mod;
            sumOuterOld += outerOld.mod;
            sumOuterNew += outerNew.mod;
        }
        if (innerOld.right() != null && outerNew.right() == null) {
            outerNew.thread = innerOld.right();
            outerNew.mod += sumInnerOld - sumOuterNew;
        } else {
            // In the paper these two cases are independent; in 1.12 the second is only reached from here.
            if (innerNew.left() != null && outerOld.left() == null) {
                outerOld.thread = innerNew.left();
                outerOld.mod += sumInnerNew - sumOuterOld;
            }
            fallback = v;
        }
        return fallback;
    }

    private static void move(Node from, Node to, float gap) {
        float between = (float) (to.number - from.number);
        if (between != 0.0F) {
            to.change -= gap / between;
            from.change += gap / between;
        }
        to.shift += gap;
        to.row += gap;
        to.mod += gap;
    }

    /** Hands out the shifts collected by move() to the children in between. */
    private static void spread(Node v) {
        float shift = 0.0F;
        float change = 0.0F;
        for (int i = v.kids.size() - 1; i >= 0; i--) {
            Node k = v.kids.get(i);
            k.row += shift;
            k.mod += shift;
            change += k.change;
            shift += k.shift + change;
        }
    }

    /** Top down: absolute rows and depths; returns the least row. */
    private static float settle(Node v, float add, int depth, float least) {
        v.row += add;
        v.depth = depth;
        if (v.row < least) {
            least = v.row;
        }
        for (Node k : v.kids) {
            least = settle(k, add + v.mod, depth + 1, least);
        }
        return least;
    }

    private static void lower(Node v, float by) {
        v.row += by;
        for (Node k : v.kids) {
            lower(k, by);
        }
    }

    /**
     * The place of an advancement id in the hash table the 1.12 client keeps the received advancements in:
     * ResourceLocation.hashCode is 31 * namespace + path, HashMap folds the upper half in, and with the 488
     * advancements of 1.12.2 the table has 1024 buckets. The order of the tabs and of the children is the order of
     * walking that table, there is no other rule; checked against a screenshot of 1.12.2 (tabs adventure, end,
     * nether, husbandry, story).
     */
    public static int bucket(String id) {
        int colon = id.indexOf(':');
        String space = colon < 0 ? "minecraft" : id.substring(0, colon);
        String path = colon < 0 ? id : id.substring(colon + 1);
        int h = 31 * space.hashCode() + path.hashCode();
        return (h ^ (h >>> 16)) & 1023;
    }

    /**
     * The order in which 1.12 builds advancements out of that table: it walks the table again and again, each time
     * taking those whose parent is already built. ids and parents run in parallel; a parent of null is a root.
     * Returns indexes into the lists; an advancement whose parent never turns up is left out.
     */
    public static List<Integer> order(final List<String> ids, List<String> parents) {
        List<Integer> waiting = new ArrayList<Integer>();
        for (int i = 0; i < ids.size(); i++) {
            waiting.add(Integer.valueOf(i));
        }
        Collections.sort(waiting, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return bucket(ids.get(a.intValue())) - bucket(ids.get(b.intValue()));
            }
        });
        List<Integer> out = new ArrayList<Integer>();
        Set<String> built = new HashSet<String>();
        while (!waiting.isEmpty()) {
            List<Integer> rest = new ArrayList<Integer>();
            for (Integer i : waiting) {
                String parent = parents.get(i.intValue());
                if (parent == null || built.contains(parent)) {
                    out.add(i);
                    built.add(ids.get(i.intValue()));
                } else {
                    rest.add(i);
                }
            }
            if (rest.size() == waiting.size()) {
                break;
            }
            waiting = rest;
        }
        return out;
    }
}
