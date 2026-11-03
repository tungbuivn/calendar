package com.calendar.tbt;

import com.calendar.tbt.FamilyStore.Person;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Graph path engine: tìm đường ngắn nhất từ mốc tới đích trên đồ thị gia phả.
 *
 * <p>Cạnh huyết thống (cha/mẹ/con) có giá 1; cạnh hôn thú có giá rất cao để
 * mọi đường huyết thống luôn được ưu tiên trước đường đi qua vợ/chồng.
 * Cùng chi phí thì ưu tiên ít cạnh hôn thú, ít bước, rồi nhánh cha trước nhánh mẹ.
 */
public final class KinshipGraph {

    private static final int BLOOD_COST = 1;
    private static final int SPOUSE_COST = 100000;

    private KinshipGraph() {
    }

    public static final class Path {
        public final List<String> edges;
        public final List<Person> nodes;

        Path(List<String> edges, List<Person> nodes) {
            this.edges = edges;
            this.nodes = nodes;
        }

        public boolean hasSpouse() {
            for (String edge : edges) {
                if ("spouse".equals(edge)) return true;
            }
            return false;
        }

        public String signature() {
            StringBuilder text = new StringBuilder();
            for (String edge : edges) text.append(edge).append(' ');
            return text.toString().trim();
        }
    }

    private static final class State {
        final String id;
        final int cost;
        final int spouseCount;
        final int steps;
        final String tie;
        final boolean descended;
        final State previous;
        final String edge;
        final Person person;

        State(String id, int cost, int spouseCount, int steps, String tie,
              boolean descended, State previous, String edge, Person person) {
            this.id = id;
            this.cost = cost;
            this.spouseCount = spouseCount;
            this.steps = steps;
            this.tie = tie;
            this.descended = descended;
            this.previous = previous;
            this.edge = edge;
            this.person = person;
        }
    }

    private static final Comparator<State> ORDER = new Comparator<State>() {
        @Override
        public int compare(State left, State right) {
            int byCost = Integer.compare(left.cost, right.cost);
            if (byCost != 0) return byCost;
            int bySpouse = Integer.compare(left.spouseCount, right.spouseCount);
            if (bySpouse != 0) return bySpouse;
            int bySteps = Integer.compare(left.steps, right.steps);
            if (bySteps != 0) return bySteps;
            return left.tie.compareTo(right.tie);
        }
    };

    /** Trả null nếu không có đường đi trong gia phả. */
    public static Path bestPath(List<Person> people, String rootId, String targetId) {
        Person root = FamilyStore.find(people, rootId);
        Person target = FamilyStore.find(people, targetId);
        if (root == null || target == null) return null;
        if (root.id.equals(target.id)) return new Path(Collections.<String>emptyList(),
                Collections.singletonList(root));

        Map<String, State> best = new HashMap<>();
        PriorityQueue<State> queue = new PriorityQueue<>(ORDER);
        State start = new State(root.id, 0, 0, 0, "", false, null, null, root);
        best.put(key(root.id, false), start);
        queue.add(start);

        State found = null;
        while (!queue.isEmpty()) {
            State current = queue.poll();
            State known = best.get(key(current.id, current.descended));
            if (known != null && ORDER.compare(current, known) > 0) continue;
            if (current.id.equals(target.id)) {
                found = current;
                break;
            }
            for (State next : neighbours(people, current)) {
                if (next == null) continue;
                String key = key(next.id, next.descended);
                State old = best.get(key);
                if (old == null || ORDER.compare(next, old) < 0) {
                    best.put(key, next);
                    queue.add(next);
                }
            }
        }
        if (found == null) return null;

        List<String> edges = new ArrayList<>();
        List<Person> nodes = new ArrayList<>();
        State cursor = found;
        while (cursor != null) {
            nodes.add(cursor.person);
            if (cursor.edge != null) edges.add(cursor.edge);
            cursor = cursor.previous;
        }
        Collections.reverse(edges);
        Collections.reverse(nodes);
        return new Path(edges, nodes);
    }

    private static List<State> neighbours(List<Person> people, State current) {
        List<State> result = new ArrayList<>();
        Person person = current.person;
        // Đã đi xuống con thì không đi ngược lên cha/mẹ nữa: đường đó thực chất
        // là quan hệ hôn thú/đồng hao, phải đi qua cạnh spouse.
        if (!current.descended) {
            Person father = FamilyStore.find(people, person.fatherId);
            if (father != null) {
                result.add(childState(current, father, "father", BLOOD_COST));
            }
            Person mother = FamilyStore.find(people, person.motherId);
            if (mother != null) {
                result.add(childState(current, mother, "mother", BLOOD_COST));
            }
        }
        Person spouse = FamilyStore.find(people, person.spouseId);
        if (spouse != null) {
            result.add(childState(current, spouse, "spouse", SPOUSE_COST));
        }
        for (Person child : FamilyStore.childrenOf(people, person.id)) {
            result.add(childState(current, child, "child", BLOOD_COST));
        }
        return result;
    }

    private static State childState(State current, Person next, String edge, int cost) {
        String rank = "father".equals(edge) ? "0"
                : "mother".equals(edge) ? "1"
                : "child".equals(edge) ? "2" : "3";
        boolean descended = "spouse".equals(edge) ? false : current.descended || "child".equals(edge);
        return new State(next.id, current.cost + cost,
                current.spouseCount + ("spouse".equals(edge) ? 1 : 0),
                current.steps + 1, current.tie + rank, descended, current, edge, next);
    }

    private static String key(String id, boolean descended) {
        return id + (descended ? ":down" : ":up");
    }
}
