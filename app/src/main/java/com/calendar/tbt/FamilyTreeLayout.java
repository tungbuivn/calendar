package com.calendar.tbt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Xếp vị trí cho sơ đồ gia phả: mỗi hàng thế hệ một dải, vợ chồng đứng sát nhau thành
 * một khối, cha mẹ đứng giữa ngay trên đàn con của mình. Thuật toán kiểu Reingold–Tilford
 * rút gọn — con cái xếp lần lượt theo con trỏ của hàng nên không bao giờ chồng nhau,
 * còn cha mẹ được căn giữa trên hai đứa con đầu/cuối, dồn sang phải nếu hết chỗ.
 *
 * <p>Không dùng lớp Android nào để xếp cây test được bằng JUnit thuần; khổ ô
 * (theo tên thật của người) do View đo rồi đưa vào.
 */
public final class FamilyTreeLayout {

    /** Chỗ hở giữa hai gia đình trong cùng một hàng. */
    public static final float FAMILY_GAP = 34f;
    /** Chỗ hở giữa hai người vừa chồng vợ. */
    public static final float SPOUSE_GAP = 16f;
    /** Bậc của người chưa rõ vai vế: số rất nhỏ nên hàng của họ tự xuống đáy sơ đồ. */
    public static final int GENERATION_UNPLACED = Integer.MIN_VALUE / 2;

    /** Một người trong sơ đồ: liên kết cấu trúc + khổ ô, tọa độ là kết quả. */
    public static final class Node {
        public final String id;
        /** dương là bậc trên (ông bà), âm là bậc dưới (con cháu). */
        public int generation;
        /** thứ tự khai trong gia phả, dùng làm fallback khi chưa có năm sinh. */
        public int order;
        public int birthYear;
        public String spouseId;
        public String fatherId;
        public String motherId;
        public float width;
        public float height;
        /** Người đứng khấn để View bo đậm ô. */
        public boolean isRoot;
        /** Tâm ô sau khi xếp. */
        public float x;
        public float y;
        /** Người tham chiếu cho View khỏi tra lại. */
        public Object tag;

        public Node(String id) {
            this.id = id == null ? "" : id;
        }
    }

    /** Dải thế hệ đã xếp, để View kẻ nền và ghi nhãn hàng. */
    public static final class Row {
        public final int generation;
        public final float yTop;
        public final float height;

        Row(int generation, float yTop, float height) {
            this.generation = generation;
            this.yTop = yTop;
            this.height = height;
        }
    }

    /** Kết quả xếp: các dải hàng, viền nội dung và thứ tự xếp để View dựng đường nối. */
    public static final class Result {
        public final List<Row> rows;
        public final float minX;
        public final float minY;
        public final float maxX;
        public final float maxY;

        Result(List<Row> rows, float minX, float minY, float maxX, float maxY) {
            this.rows = rows;
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        public boolean isEmpty() {
            return rows.isEmpty();
        }
    }

    private static final float ROW_GAP = 54f;

    private final Map<String, Node> byId = new HashMap<>();
    private final Map<Integer, Float> nextX = new HashMap<>();
    private final Set<String> visited = new HashSet<>();
    private final Map<Integer, Float> rowHeights = new HashMap<>();
    private final List<Integer> generations = new ArrayList<>();

    private FamilyTreeLayout() {
    }

    public static Result layout(List<Node> nodes) {
        return new FamilyTreeLayout().run(nodes);
    }

    private Result run(List<Node> input) {
        for (Node node : input) {
            if (node == null || node.id.isEmpty()) continue;
            byId.put(node.id, node);
            Float height = rowHeights.get(node.generation);
            rowHeights.put(node.generation, height == null ? node.height : Math.max(height, node.height));
        }
        if (byId.isEmpty()) return new Result(Collections.<Row>emptyList(), 0, 0, 0, 0);

        generations.clear();
        generations.addAll(rowHeights.keySet());
        // Bậc trên xếp lên trước; người chưa rõ bậc (số rất nhỏ) tự xuống cuối.
        Collections.sort(generations, new Comparator<Integer>() {
            @Override
            public int compare(Integer left, Integer right) {
                return Integer.compare(right, left);
            }
        });
        Map<Integer, Float> rowTops = new HashMap<>();
        float y = 0;
        for (int i = 0; i < generations.size(); i++) {
            int generation = generations.get(i);
            rowTops.put(generation, y);
            y += rowHeights.get(generation) + (i == generations.size() - 1 ? 0 : ROW_GAP);
        }

        List<Node> seeds = new ArrayList<>(byId.values());
        Collections.sort(seeds, seedComparator());
        for (Node seed : seeds) {
            if (!visited.contains(seed.id)) place(seed, rowTops);
        }
        for (Node node : byId.values()) {
            if (!visited.contains(node.id)) {
                // Người không đặt được vì chuỗi nối bị vòng: xếp tạm vào cuối hàng của họ.
                visited.add(node.id);
                node.x = nextX.get(node.generation) == null ? 0 : nextX.get(node.generation);
                node.x += node.width / 2;
                node.y = rowTops.get(node.generation) + rowHeights.get(node.generation) / 2;
                bump(node.generation, node.width);
            }
        }

        List<Row> rows = new ArrayList<>();
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        for (Integer generation : generations) {
            float top = rowTops.get(generation);
            rows.add(new Row(generation, top, rowHeights.get(generation)));
            for (Node node : byId.values()) {
                if (node.generation != generation) continue;
                minX = Math.min(minX, node.x - node.width / 2);
                maxX = Math.max(maxX, node.x + node.width / 2);
            }
        }
        float minY = 0;
        float maxY = topOfLastRow(rowTops) + lastRowHeight();
        return new Result(rows, Math.min(minX, 0), minY, Math.max(maxX, 0), maxY);
    }

    private float topOfLastRow(Map<Integer, Float> rowTops) {
        float top = 0;
        for (Integer generation : generations) top = Math.max(top, rowTops.get(generation));
        return top;
    }

    private float lastRowHeight() {
        return rowHeights.get(generations.get(generations.size() - 1));
    }

    /**
     * Đặt một người (kèm bạn đời) và toàn bộ con cháu phía dưới, trả về tâm ngang
     * của khối vợ chồng để cha mẹ căn giữa trên đàn con.
     */
    private float place(Node node, Map<Integer, Float> rowTops) {
        if (visited.contains(node.id)) return node.x;
        visited.add(node.id);
        Node spouse = node.spouseId == null ? null : byId.get(node.spouseId);
        if (spouse != null && visited.contains(spouse.id)) spouse = null;
        if (spouse != null) visited.add(spouse.id);

        List<Node> children = childrenOf(node, spouse);
        float blockWidth = node.width + (spouse == null ? 0 : SPOUSE_GAP + spouse.width);
        float cursor = cursor(node.generation);
        float left = cursor;

        if (children.isEmpty()) {
            left = cursor;
        } else {
            float first = Float.MAX_VALUE;
            float last = -Float.MAX_VALUE;
            for (Node child : children) {
                float center = place(child, rowTops);
                first = Math.min(first, center);
                last = Math.max(last, center);
            }
            float desired = (first + last) / 2;
            left = Math.max(cursor, desired - blockWidth / 2);
        }

        float centerY = rowTops.get(node.generation) + rowHeights.get(node.generation) / 2;
        node.x = left + node.width / 2;
        node.y = centerY;
        float used = left + node.width;
        if (spouse != null) {
            spouse.x = used + SPOUSE_GAP + spouse.width / 2;
            spouse.y = centerY;
            used = spouse.x + spouse.width / 2;
        }
        nextX.put(node.generation, used + FAMILY_GAP);
        return spouse == null ? node.x : (node.x + spouse.x) / 2;
    }

    /** Con của một trong hai người, chưa đặt, xếp theo năm sinh rồi theo thứ tự khai. */
    private List<Node> childrenOf(Node left, Node right) {
        List<Node> result = new ArrayList<>();
        for (Node candidate : byId.values()) {
            if (visited.contains(candidate.id)) continue;
            boolean child = isChildOf(candidate, left)
                    || (right != null && isChildOf(candidate, right));
            if (child) result.add(candidate);
        }
        Collections.sort(result, childComparator());
        return result;
    }

    private static boolean isChildOf(Node candidate, Node parent) {
        if (parent == null) return false;
        return parent.id.equals(candidate.fatherId) || parent.id.equals(candidate.motherId);
    }

    private float cursor(int generation) {
        Float value = nextX.get(generation);
        return value == null ? 0 : value;
    }

    private void bump(int generation, float width) {
        Float value = nextX.get(generation);
        nextX.put(generation, (value == null ? 0 : value) + width + FAMILY_GAP);
    }

    /** Hàng trên trước, trong hàng thì năm sinh nhỏ trước — giống thứ tự gia phả chép tay. */
    private Comparator<Node> seedComparator() {
        return new Comparator<Node>() {
            @Override
            public int compare(Node left, Node right) {
                int byGeneration = Integer.compare(right.generation, left.generation);
                if (byGeneration != 0) return byGeneration;
                int byBirth = compareYears(left.birthYear, right.birthYear);
                if (byBirth != 0) return byBirth;
                return Integer.compare(left.order, right.order);
            }
        };
    }

    private Comparator<Node> childComparator() {
        return new Comparator<Node>() {
            @Override
            public int compare(Node left, Node right) {
                int byBirth = compareYears(left.birthYear, right.birthYear);
                if (byBirth != 0) return byBirth;
                return Integer.compare(left.order, right.order);
            }
        };
    }

    private static int compareYears(int left, int right) {
        if (left == right) return 0;
        if (left <= 0) return 1;
        if (right <= 0) return -1;
        return Integer.compare(left, right);
    }
}
