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
        /** true khi đã xếp bằng cây chuẩn (không cắt); false là fallback. */
        public final boolean tidy;

        Result(List<Row> rows, float minX, float minY, float maxX, float maxY, boolean tidy) {
            this.rows = rows;
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.tidy = tidy;
        }

        public boolean isEmpty() {
            return rows.isEmpty();
        }
    }

    /** Khoảng hở dọc tối thiểu giữa hai hàng, cùng đơn vị với khổ ô. */
    private static final float MIN_ROW_GAP = 54f;
    /** Hàng càng cao (màn mật độ cao) thì khoảng hở dọc càng rộng theo tỉ lệ này. */
    private static final float ROW_GAP_RATIO = 0.75f;

    private final Map<String, Node> byId = new HashMap<>();
    /** Khoảng hở thực tế, tự nới theo chiều cao card trên màn mật độ cao. */
    private float spouseGap = SPOUSE_GAP;
    private float familyGap = FAMILY_GAP;
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
        if (byId.isEmpty()) return new Result(Collections.<Row>emptyList(), 0, 0, 0, 0, false);

        generations.clear();
        generations.addAll(rowHeights.keySet());
        // Bậc trên xếp lên trước; người chưa rõ bậc (số rất nhỏ) tự xuống cuối.
        Collections.sort(generations, new Comparator<Integer>() {
            @Override
            public int compare(Integer left, Integer right) {
                return Integer.compare(right, left);
            }
        });
        float maxRowHeight = 0f;
        for (Float height : rowHeights.values()) {
            maxRowHeight = Math.max(maxRowHeight, height);
        }
        // Khoảng hở dọc tính theo chiều cao card: màn mật độ cao có card cao hơn
        // nên cũng cần khoảng thở lớn hơn, tránh các hàng dính sát nhau.
        float rowGap = Math.max(MIN_ROW_GAP, maxRowHeight * ROW_GAP_RATIO);
        // Card cao hơn (màn mật độ cao) thì cần khoảng hở ngang lớn hơn để thấy
        // rõ nét kẻ của từng ô và đường nối vợ/chồng.
        spouseGap = Math.max(SPOUSE_GAP, maxRowHeight * 0.28f);
        familyGap = Math.max(FAMILY_GAP, maxRowHeight * 0.42f);
        Map<Integer, Float> rowTops = new HashMap<>();
        float y = 0;
        for (int i = 0; i < generations.size(); i++) {
            int generation = generations.get(i);
            rowTops.put(generation, y);
            y += rowHeights.get(generation) + (i == generations.size() - 1 ? 0 : rowGap);
        }

        Result tidy = tidy(rowTops);
        if (tidy != null) return tidy;

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

        return result(rowTops, false);
    }

    private Result result(Map<Integer, Float> rowTops, boolean tidy) {
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
        return new Result(rows, Math.min(minX, 0), minY, Math.max(maxX, 0), maxY, tidy);
    }

    /** Một khối vợ/chồng (hoặc một người) trong cây chuẩn. */
    private static final class Block {
        Node primary;
        Node spouse;
        Block parent;
        Node childNode;
        final List<Block> children = new ArrayList<>();
        float width;
        float subtreeWidth;
        float centerX;
    }

    /**
     * Cây chuẩn: mỗi khối vợ/chồng là một nút, con xếp trái sang phải, cha mẹ
     * căn giữa trên các nhánh con. Các nhánh con không chồng lấn nên đường nối
     * không cắt nhau. Trả null nếu dữ liệu không phải dạng cây (nhiều cha/mẹ
     * khác khối, chu trình, vợ/chồng chéo...) để dùng fallback.
     */
    private Result tidy(Map<Integer, Float> rowTops) {
        List<Node> placed = new ArrayList<>();
        for (Node node : byId.values()) {
            if (node.generation != GENERATION_UNPLACED) placed.add(node);
        }
        if (placed.isEmpty()) return null;
        Collections.sort(placed, seedComparator());

        Map<String, Block> blockByPerson = new HashMap<>();
        List<Block> blocks = new ArrayList<>();
        for (Node node : placed) {
            if (blockByPerson.containsKey(node.id)) continue;
            Node spouse = node.spouseId == null ? null : byId.get(node.spouseId);
            if (spouse == node) return null;
            if (spouse != null) {
                if (spouse.generation == GENERATION_UNPLACED
                        || spouse.generation != node.generation) return null;
                if (blockByPerson.containsKey(spouse.id)) return null;
                if (spouse.spouseId != null && !spouse.spouseId.equals(node.id)) return null;
            }
            Block block = new Block();
            block.primary = node;
            block.spouse = spouse;
            block.width = node.width + (spouse == null ? 0f : spouseGap + spouse.width);
            blocks.add(block);
            blockByPerson.put(node.id, block);
            if (spouse != null) blockByPerson.put(spouse.id, block);
        }

        for (Block block : blocks) {
            Block parent = null;
            Node child = null;
            for (int i = 0; i < 2; i++) {
                Node person = i == 0 ? block.primary : block.spouse;
                if (person == null) continue;
                Block father = person.fatherId == null ? null : blockByPerson.get(person.fatherId);
                Block mother = person.motherId == null ? null : blockByPerson.get(person.motherId);
                if (father == block || mother == block) return null;
                Block candidate = null;
                if (father != null && mother != null) {
                    if (father != mother) return null;
                    candidate = father;
                } else if (father != null) {
                    candidate = father;
                } else if (mother != null) {
                    candidate = mother;
                }
                if (candidate == null) continue;
                if (candidate.primary.generation != person.generation + 1) return null;
                if (parent != null && parent != candidate) return null;
                parent = candidate;
                child = person;
            }
            if (parent != null) {
                block.parent = parent;
                block.childNode = child;
            }
        }

        for (Block block : blocks) {
            Set<Block> seen = new HashSet<>();
            Block cursor = block;
            while (cursor != null) {
                if (!seen.add(cursor)) return null;
                cursor = cursor.parent;
            }
        }

        List<Block> roots = new ArrayList<>();
        for (Block block : blocks) {
            if (block.parent == null) roots.add(block);
            else block.parent.children.add(block);
        }
        if (roots.isEmpty()) return null;

        Comparator<Block> childOrder = new Comparator<Block>() {
            @Override
            public int compare(Block left, Block right) {
                Node a = left.childNode != null ? left.childNode : left.primary;
                Node b = right.childNode != null ? right.childNode : right.primary;
                int byBirth = compareYears(a.birthYear, b.birthYear);
                if (byBirth != 0) return byBirth;
                return Integer.compare(a.order, b.order);
            }
        };
        for (Block block : blocks) Collections.sort(block.children, childOrder);

        float rootCursor = 0f;
        for (Block root : roots) {
            measure(root);
            setX(root, rootCursor);
            rootCursor += root.subtreeWidth + familyGap;
        }

        for (Block block : blocks) {
            float left = block.centerX - block.width / 2f;
            Node primary = block.primary;
            primary.x = left + primary.width / 2f;
            primary.y = rowTops.get(primary.generation) + rowHeights.get(primary.generation) / 2f;
            if (block.spouse != null) {
                block.spouse.x = left + primary.width + spouseGap + block.spouse.width / 2f;
                block.spouse.y = primary.y;
            }
        }

        List<Node> unplaced = new ArrayList<>();
        for (Node node : byId.values()) {
            if (node.generation == GENERATION_UNPLACED) unplaced.add(node);
        }
        Collections.sort(unplaced, childComparator());
        float unplacedX = 0f;
        for (Node node : unplaced) {
            node.x = unplacedX + node.width / 2f;
            node.y = rowTops.get(node.generation) + rowHeights.get(node.generation) / 2f;
            unplacedX += node.width + familyGap;
        }
        return result(rowTops, true);
    }

    private float measure(Block block) {
        float childrenWidth = 0f;
        for (int i = 0; i < block.children.size(); i++) {
            if (i > 0) childrenWidth += familyGap;
            childrenWidth += measure(block.children.get(i));
        }
        block.subtreeWidth = Math.max(block.width, childrenWidth);
        return block.subtreeWidth;
    }

    private void setX(Block block, float left) {
        block.centerX = left + block.subtreeWidth / 2f;
        float childrenWidth = 0f;
        for (int i = 0; i < block.children.size(); i++) {
            if (i > 0) childrenWidth += familyGap;
            childrenWidth += block.children.get(i).subtreeWidth;
        }
        float childLeft = left + (block.subtreeWidth - childrenWidth) / 2f;
        for (Block child : block.children) {
            setX(child, childLeft);
            childLeft += child.subtreeWidth + familyGap;
        }
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
        // Người không nối được với mốc nằm chung hàng "Chưa nối". Trong hàng đó
        // không dựng cây theo cha/mẹ/vợ/chồng nữa, nếu không cha và con cùng hàng
        // sẽ bị đè lên nhau.
        if (node.generation == GENERATION_UNPLACED) {
            float left = cursor(node.generation);
            node.x = left + node.width / 2;
            node.y = rowTops.get(node.generation) + rowHeights.get(node.generation) / 2;
            nextX.put(node.generation, left + node.width + familyGap);
            return node.x;
        }
        Node spouse = node.spouseId == null ? null : byId.get(node.spouseId);
        if (spouse != null && visited.contains(spouse.id)) spouse = null;
        if (spouse != null) visited.add(spouse.id);

        List<Node> children = childrenOf(node, spouse);
        float blockWidth = node.width + (spouse == null ? 0 : spouseGap + spouse.width);
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
            spouse.x = used + spouseGap + spouse.width / 2;
            spouse.y = centerY;
            used = spouse.x + spouse.width / 2;
        }
        nextX.put(node.generation, used + familyGap);
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
        nextX.put(generation, (value == null ? 0 : value) + width + familyGap);
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
