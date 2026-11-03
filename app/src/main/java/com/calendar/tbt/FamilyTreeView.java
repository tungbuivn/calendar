package com.calendar.tbt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.calendar.tbt.FamilyStore.Person;
import com.calendar.tbt.FamilyStore.Relation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sơ đồ gia phả vẽ tay trên Canvas: kéo để dời khung, chụm hai ngón để phóng,
 * chạm một người để lấy làm mốc, chạm hai lần hoặc giữ lâu để sửa. Nối quan hệ
 * bằng nút "Quan hệ" trong màn hình. Khổ ô tính theo độ dài tên thật nên không
 * còn cảnh tên bị cắt.
 */
public class FamilyTreeView extends View {

    public interface Listener {
        /** Chạm một lần: đổi người đứng làm mốc tính vai vế. */
        void onPickRoot(Person person);

        /** Chạm hai lần hoặc giữ lâu: mở hộp sửa người. */
        void onEditPerson(Person person);

        /** Thả ô đang kéo lên một ô khác: xin nối quan hệ giữa hai người. */
        void onLinkPersons(Person from, Person to);
    }

    /** Giới hạn phóng: đủ nhỏ để coi cả gia phả, đủ to để đọc tên trên màn nhỏ. */
    private static final float MAX_SCALE = 3f;
    private static final float MIN_SCALE = 0.18f;
    /** Số đoạn nhỏ để xấp xỉ góc bo khi dò giao điểm. */
    private static final int CURVE_STEPS = 18;
    /** Bán kính cầu vượt tại giao điểm. */
    private static final float BRIDGE_RADIUS_DP = 7f;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint termPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint avatarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint avatarTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    private final Matrix inverse = new Matrix();
    private final float[] point = new float[2];
    private final List<FamilyTreeLayout.Node> nodes = new ArrayList<>();
    private final Map<String, FamilyTreeLayout.Node> byId = new HashMap<>();
    private final List<FamilyTreeLayout.Row> rows = new ArrayList<>();
    private final GestureDetector gestures;
    private final ScaleGestureDetector scaler;
    private final float density = getResources().getDisplayMetrics().density;

    private Listener listener;
    private float scale = 1f;
    private float translateX;
    private float translateY;
    private float contentMinX;
    private float contentMinY;
    private float contentMaxX;
    private float contentMaxY;
    private float fitScale = 1f;
    private boolean needsFit = true;

    private FamilyTreeLayout.Node pressNode;
    private FamilyTreeLayout.Node linkSource;
    private FamilyTreeLayout.Node linkTarget;
    private float linkX;
    private float linkY;

    /** Một ô nhớ kèm chữ phụ đề (vai vế · năm sinh) đã tính lúc dựng mô hình. */
    private static final class Item {
        final Person person;
        final String sub;

        Item(Person person, String sub) {
            this.person = person;
            this.sub = sub;
        }
    }

    public FamilyTreeView(Context context) {
        this(context, null);
    }

    public FamilyTreeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gestures = new GestureDetector(context, new Gesture());
        scaler = new ScaleGestureDetector(context, new Scale());
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1.2f));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(1.6f));
        linePaint.setColor(color(R.color.primaryLightColor));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        namePaint.setTextSize(sp(13));
        namePaint.setFakeBoldText(true);
        termPaint.setTextSize(sp(11));
        bandPaint.setTextSize(sp(11));
        bandPaint.setFakeBoldText(true);
        avatarPaint.setStyle(Paint.Style.FILL);
        avatarTextPaint.setTextSize(sp(11));
        avatarTextPaint.setTextAlign(Paint.Align.CENTER);
        avatarTextPaint.setFakeBoldText(true);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Dựng lại sơ đồ theo gia phả hiện tại; gọi mỗi lần dữ liệu đổi. */
    public void setModel(List<Person> people, List<Relation> relations, String rootId) {
        // Giữ nguyên zoom/pan khi dữ liệu đổi (đổi mốc, sửa người...): neo người
        // vừa chọn ở đúng vị trí màn hình cũ thay vì fit lại từ đầu.
        FamilyTreeLayout.Node oldAnchor = null;
        float anchorScreenX = 0;
        float anchorScreenY = 0;
        if (getWidth() > 0 && !nodes.isEmpty()) {
            oldAnchor = byId.get(rootId);
            if (oldAnchor != null) {
                anchorScreenX = oldAnchor.x * scale + translateX;
                anchorScreenY = oldAnchor.y * scale + translateY;
            }
        }
        nodes.clear();
        byId.clear();
        rows.clear();
        int order = 0;
        for (Person person : people) {
            Kinship.Result result = Kinship.of(people, relations, rootId, person.id);
            int generation = result.related && result.generation != FamilyStore.GENERATION_UNKNOWN
                    ? result.generation : FamilyTreeLayout.GENERATION_UNPLACED;
            String sub = subLabel(person, result);
            FamilyTreeLayout.Node node = new FamilyTreeLayout.Node(person.id);
            node.generation = generation;
            node.order = order++;
            node.birthYear = person.birthYear;
            node.spouseId = person.spouseId;
            node.fatherId = person.fatherId;
            node.motherId = person.motherId;
            node.isRoot = person.id.equals(rootId);
            node.tag = new Item(person, sub);
            node.width = Math.max(dp(126), Math.max(namePaint.measureText(person.name),
                    termPaint.measureText(sub)) + dp(52));
            node.height = dp(54);
            nodes.add(node);
            byId.put(node.id, node);
        }
        // Card cùng một khổ để tâm khối vợ/chồng, chấm đỏ và nhánh con thẳng hàng.
        float uniformWidth = 0f;
        for (FamilyTreeLayout.Node node : nodes) {
            uniformWidth = Math.max(uniformWidth, node.width);
        }
        for (FamilyTreeLayout.Node node : nodes) {
            node.width = uniformWidth;
        }
        FamilyTreeLayout.Result layout = FamilyTreeLayout.layout(nodes);
        contentMinX = layout.minX;
        contentMinY = layout.minY;
        contentMaxX = layout.maxX;
        contentMaxY = layout.maxY;
        rows.addAll(layout.rows);
        if (getWidth() > 0) {
            FamilyTreeLayout.Node newAnchor = byId.get(rootId);
            if (oldAnchor != null && newAnchor != null) {
                translateX = anchorScreenX - newAnchor.x * scale;
                translateY = anchorScreenY - newAnchor.y * scale;
                needsFit = false;
            } else {
                needsFit = false;
                fitToContent();
            }
        } else {
            needsFit = true;
        }
        invalidate();
    }

    private static String subLabel(Person person, Kinship.Result result) {
        StringBuilder text = new StringBuilder(result.related ? result.term : "chưa nối");
        if (person.birthYear > 0) text.append(" · ").append(person.birthYear);
        return text.toString();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (needsFit && w > 0) {
            needsFit = false;
            fitToContent();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (nodes.isEmpty()) return;
        applyMatrix();
        int save = canvas.save();
        canvas.concat(matrix);
        drawBands(canvas);
        drawLinks(canvas);
        for (FamilyTreeLayout.Node node : nodes) drawNode(canvas, node);
        drawLinkDraft(canvas);
        canvas.restoreToCount(save);
    }

    private void applyMatrix() {
        matrix.reset();
        matrix.setScale(scale, scale);
        matrix.postTranslate(translateX, translateY);
    }

    private void drawBands(Canvas canvas) {
        for (int i = 0; i < rows.size(); i++) {
            FamilyTreeLayout.Row row = rows.get(i);
            String label = row.generation == FamilyTreeLayout.GENERATION_UNPLACED
                    ? "Chưa rõ bậc" : Kinship.band(row.generation);
            fillPaint.setColor(color(i % 2 == 0 ? R.color.backgroundCard : R.color.monthTonal));
            fillPaint.setAlpha(70);
            canvas.drawRect(contentMinX - dp(8), row.yTop - dp(6),
                    contentMaxX + dp(8), row.yTop + row.height + dp(6), fillPaint);
            fillPaint.setAlpha(255);
            bandPaint.setColor(color(R.color.textSecondary));
            canvas.drawText(label, contentMinX - dp(4), row.yTop - dp(10), bandPaint);
        }
    }

    /** Vợ chồng nối bằng một đoạn ngang; cha mẹ – con bằng nhánh vuông góc, giao nhau thì nhảy cầu. */
    private void drawLinks(Canvas canvas) {
        // Lượt 1: thu thập nhánh con (đen), chia làn ngang để các đoạn ngang
        // không đè lên nhau, rồi mới vẽ.
        List<Branch> branches = new ArrayList<>();
        Set<String> drawn = new HashSet<>();
        for (FamilyTreeLayout.Node node : nodes) {
            if (node.generation == FamilyTreeLayout.GENERATION_UNPLACED) continue;
            if (drawn.contains(node.id)) continue;
            FamilyTreeLayout.Node spouse = node.spouseId == null ? null : byId.get(node.spouseId);
            if (spouse != null && spouse.generation == FamilyTreeLayout.GENERATION_UNPLACED) spouse = null;
            drawn.add(node.id);
            if (spouse != null) drawn.add(spouse.id);
            List<FamilyTreeLayout.Node> children = childrenOf(node, spouse);
            if (!children.isEmpty()) branches.add(newBranch(node, spouse, children));
        }
        assignBuses(branches);
        List<List<float[]>> drawnBranches = new ArrayList<>();
        for (Branch branch : branches) {
            drawChildBranches(canvas, branch, drawnBranches);
        }

        // Lượt 2: đường vợ/chồng (đỏ) vẽ sau cùng.
        drawn.clear();
        for (FamilyTreeLayout.Node node : nodes) {
            if (node.generation == FamilyTreeLayout.GENERATION_UNPLACED) continue;
            if (drawn.contains(node.id)) continue;
            FamilyTreeLayout.Node spouse = node.spouseId == null ? null : byId.get(node.spouseId);
            if (spouse != null && spouse.generation == FamilyTreeLayout.GENERATION_UNPLACED) spouse = null;
            drawn.add(node.id);
            if (spouse != null) {
                drawn.add(spouse.id);
                drawCouple(canvas, node, spouse);
            }
        }
    }

    private static final class Branch {
        FamilyTreeLayout.Node node;
        FamilyTreeLayout.Node spouse;
        List<FamilyTreeLayout.Node> children;
        float junctionX;
        float parentBottom;
        float childTop;
        float spanLeft;
        float spanRight;
        float busY;
    }

    private Branch newBranch(FamilyTreeLayout.Node node, FamilyTreeLayout.Node spouse,
                             List<FamilyTreeLayout.Node> children) {
        Branch branch = new Branch();
        branch.node = node;
        branch.spouse = spouse;
        branch.children = children;
        branch.junctionX = coupleJunctionX(node, spouse);
        branch.parentBottom = node.y + node.height / 2f;
        if (spouse != null) {
            branch.parentBottom = Math.max(branch.parentBottom, spouse.y + spouse.height / 2f);
        }
        float top = Float.MAX_VALUE;
        float left = branch.junctionX;
        float right = branch.junctionX;
        for (FamilyTreeLayout.Node child : children) {
            top = Math.min(top, child.y - child.height / 2f);
            left = Math.min(left, child.x);
            right = Math.max(right, child.x);
        }
        branch.childTop = top;
        branch.spanLeft = left;
        branch.spanRight = right;
        return branch;
    }

    /**
     * Chia làn cho các đoạn ngang: hai nhánh có khoảng x chồng nhau sẽ nằm ở
     * làn khác nhau, nên các đoạn nằm ngang không bao giờ đè lên nhau.
     */
    private void assignBuses(List<Branch> branches) {
        Map<Integer, List<Branch>> groups = new HashMap<>();
        for (Branch branch : branches) {
            List<Branch> group = groups.get(branch.node.generation);
            if (group == null) {
                group = new ArrayList<>();
                groups.put(branch.node.generation, group);
            }
            group.add(branch);
        }
        for (List<Branch> group : groups.values()) {
            java.util.Collections.sort(group, (a, b) -> Float.compare(a.spanLeft, b.spanLeft));
            List<Float> rightEdges = new ArrayList<>();
            for (Branch branch : group) {
                float gap = Math.max(1f, branch.childTop - branch.parentBottom);
                float base = branch.parentBottom + gap * 0.38f;
                int channel = -1;
                for (int i = 0; i < rightEdges.size(); i++) {
                    if (branch.spanLeft > rightEdges.get(i) + dp(4)) {
                        channel = i;
                        break;
                    }
                }
                if (channel < 0) {
                    channel = rightEdges.size();
                    rightEdges.add(0f);
                }
                rightEdges.set(channel, branch.spanRight);
                float y = base + channel * dp(5);
                float maxY = branch.childTop - dp(4);
                if (y > maxY) y = maxY;
                branch.busY = y;
            }
        }
    }

    private static float coupleJunctionX(FamilyTreeLayout.Node node, FamilyTreeLayout.Node spouse) {
        if (spouse == null) return node.x;
        FamilyTreeLayout.Node first = node.x <= spouse.x ? node : spouse;
        FamilyTreeLayout.Node second = node.x <= spouse.x ? spouse : node;
        return (first.x + first.width / 2f + second.x - second.width / 2f) / 2f;
    }

    private static final int TYPE_DIAGONAL = 0;
    private static final int TYPE_HORIZONTAL = 1;
    private static final int TYPE_VERTICAL = 2;

    /** Vẽ nhánh cha mẹ – con kiểu vuông góc, có nửa đường tròn tại giao điểm. */
    private void drawChildBranches(Canvas canvas, Branch branch,
                                   List<List<float[]>> drawnBranches) {
        FamilyTreeLayout.Node node = branch.node;
        FamilyTreeLayout.Node spouse = branch.spouse;
        List<FamilyTreeLayout.Node> children = branch.children;
        float junctionX = branch.junctionX;
        float junctionY = spouse == null ? node.y + node.height / 2 : node.y;
        // Làn ngang đã được chia sẵn; không ép phải nằm giữa đáy ô và đỉnh ô.
        float busY = branch.busY;

        // Dựng nhánh vuông góc (có bo góc) thành các đoạn nhỏ để dò giao điểm.
        List<float[]> segments = new ArrayList<>();
        Set<Integer> starts = new HashSet<>();
        starts.add(segments.size());
        addSegment(segments, junctionX, junctionY, junctionX, busY);
        for (FamilyTreeLayout.Node child : children) {
            starts.add(segments.size());
            float top = child.y - child.height / 2;
            float span = Math.abs(child.x - junctionX);
            if (span < 0.5f) {
                addSegment(segments, junctionX, busY, child.x, top);
                continue;
            }
            float direction = child.x > junctionX ? 1f : -1f;
            float corner = Math.min(dp(14), Math.min(span / 2f, (top - busY) / 2f));
            if (corner <= 1f) {
                addSegment(segments, junctionX, busY, child.x, busY);
                addSegment(segments, child.x, busY, child.x, top);
                continue;
            }
            addSegment(segments, junctionX, busY, child.x - direction * corner, busY);
            sampleQuad(segments, child.x - direction * corner, busY,
                    child.x, busY, child.x, busY + corner);
            addSegment(segments, child.x, busY + corner, child.x, top);
        }

        // Giao điểm với nhánh trước: {chỉ số đoạn, t, x, y}.
        List<float[]> bridges = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            float[] segment = segments.get(i);
            for (List<float[]> previous : drawnBranches) {
                for (float[] other : previous) {
                    float[] hit = segmentIntersection(segment, other);
                    if (hit == null) continue;
                    float hx = hit[1];
                    float hy = hit[2];
                    if (Math.hypot(hx - junctionX, hy - junctionY) <= dp(14)) continue;
                    bridges.add(new float[]{i, hit[0], hx, hy});
                }
            }
        }
        java.util.Collections.sort(bridges, (a, b) -> Float.compare(a[1], b[1]));

        Path path = new Path();
        boolean started = false;
        for (int i = 0; i < segments.size(); i++) {
            float[] s = segments.get(i);
            float sx = s[0], sy = s[1], ex = s[2], ey = s[3];
            float dx = ex - sx, dy = ey - sy;
            float length = (float) Math.hypot(dx, dy);
            if (length < 0.01f) continue;
            float ux = dx / length, uy = dy / length;
            if (starts.contains(i)) {
                path.moveTo(sx, sy);
                started = true;
            } else if (!started) {
                path.moveTo(sx, sy);
                started = true;
            }
            for (float[] bridge : bridges) {
                if ((int) bridge[0] != i) continue;
                float t = bridge[1];
                if (t <= 0.02f || t >= 0.98f) continue;
                float reach = Math.min(t, 1f - t) * length;
                float radius = Math.min(dp(BRIDGE_RADIUS_DP), reach * 0.6f);
                if (radius < dp(1.5f)) continue;
                float bx = sx + dx * t;
                float by = sy + dy * t;
                float startX = bx - ux * radius;
                float startY = by - uy * radius;
                float endX = bx + ux * radius;
                float endY = by + uy * radius;
                path.lineTo(startX, startY);
                int type = (int) s[4];
                if (type == TYPE_HORIZONTAL) {
                    // Đường ngang: nửa vòng tròn vòng lên trên, đúng kiểu _____A____.
                    RectF oval = new RectF(bx - radius, by - radius, bx + radius, by + radius);
                    if (dx > 0) path.arcTo(oval, 180, 180);
                    else path.arcTo(oval, 0, -180);
                } else if (type == TYPE_VERTICAL) {
                    // Đường dọc: nửa vòng tròn vòng sang phải.
                    RectF oval = new RectF(bx - radius, by - radius, bx + radius, by + radius);
                    if (dy > 0) path.arcTo(oval, 270, 180);
                    else path.arcTo(oval, 90, -180);
                } else {
                    // Góc bo xiên: hiếm gặp giao điểm, dùng cung mềm cùng hướng.
                    float nx = -uy, ny = ux;
                    float handle = (4f / 3f) * radius;
                    path.cubicTo(startX + nx * handle, startY + ny * handle,
                            endX + nx * handle, endY + ny * handle,
                            endX, endY);
                }
            }
            path.lineTo(ex, ey);
        }
        canvas.drawPath(path, linePaint);
        drawnBranches.add(segments);
    }

    private static void addSegment(List<float[]> out, float x1, float y1, float x2, float y2) {
        if (Math.abs(x2 - x1) < 0.01f && Math.abs(y2 - y1) < 0.01f) return;
        int type = TYPE_DIAGONAL;
        if (Math.abs(y2 - y1) < 0.01f) {
            type = TYPE_HORIZONTAL;
        } else if (Math.abs(x2 - x1) < 0.01f) {
            type = TYPE_VERTICAL;
        }
        out.add(new float[]{x1, y1, x2, y2, type});
    }

    /** Xấp xỉ góc bo (Bezier bậc hai) thành các đoạn nhỏ. */
    private static void sampleQuad(List<float[]> out, float x0, float y0, float cx, float cy,
                                   float x1, float y1) {
        float px = x0;
        float py = y0;
        for (int i = 1; i <= CURVE_STEPS; i++) {
            float t = i / (float) CURVE_STEPS;
            float mt = 1f - t;
            float x = mt * mt * x0 + 2f * mt * t * cx + t * t * x1;
            float y = mt * mt * y0 + 2f * mt * t * cy + t * t * y1;
            addSegment(out, px, py, x, y);
            px = x;
            py = y;
        }
    }

    /** Giao hai đoạn thẳng; trả {t trên đoạn a, x, y} hoặc null. */
    private static float[] segmentIntersection(float[] a, float[] b) {
        float ax = a[0], ay = a[1], bx = a[2], by = a[3];
        float cx = b[0], cy = b[1], dx = b[2], dy = b[3];
        float r1x = bx - ax, r1y = by - ay;
        float r2x = dx - cx, r2y = dy - cy;
        float denominator = r1x * r2y - r1y * r2x;
        if (Math.abs(denominator) < 1e-4f) return null;
        float t = ((cx - ax) * r2y - (cy - ay) * r2x) / denominator;
        float u = ((cx - ax) * r1y - (cy - ay) * r1x) / denominator;
        if (t <= 0.02f || t >= 0.98f || u <= 0.02f || u >= 0.98f) return null;
        return new float[]{t, ax + t * r1x, ay + t * r1y};
    }

    private void drawCouple(Canvas canvas, FamilyTreeLayout.Node left, FamilyTreeLayout.Node right) {
        FamilyTreeLayout.Node first = left.x <= right.x ? left : right;
        FamilyTreeLayout.Node second = left.x <= right.x ? right : left;
        strokePaint.setColor(color(R.color.secondaryColor));
        strokePaint.setStrokeWidth(dp(2f));
        canvas.drawLine(first.x + first.width / 2, first.y,
                second.x - second.width / 2, second.y, strokePaint);
        strokePaint.setStrokeWidth(dp(1.2f));
        // Chấm đỏ ở giữa khe hai ô: đúng điểm nhánh con màu đen bám vào.
        fillPaint.setColor(color(R.color.secondaryColor));
        float dotX = (first.x + first.width / 2f + second.x - second.width / 2f) / 2f;
        canvas.drawCircle(dotX, left.y, dp(3.5f), fillPaint);
    }


    private List<FamilyTreeLayout.Node> childrenOf(FamilyTreeLayout.Node left,
                                                  FamilyTreeLayout.Node right) {
        List<FamilyTreeLayout.Node> result = new ArrayList<>();
        for (FamilyTreeLayout.Node candidate : nodes) {
            if (candidate.generation == FamilyTreeLayout.GENERATION_UNPLACED) continue;
            if (candidate.id.equals(left.id)) continue;
            if (right != null && candidate.id.equals(right.id)) continue;
            boolean child = left.id.equals(candidate.fatherId) || left.id.equals(candidate.motherId)
                    || (right != null && (right.id.equals(candidate.fatherId)
                    || right.id.equals(candidate.motherId)));
            if (child) result.add(candidate);
        }
        return result;
    }

    private void drawNode(Canvas canvas, FamilyTreeLayout.Node node) {
        Item item = (Item) node.tag;
        Person person = item.person;
        RectF rect = new RectF(node.x - node.width / 2, node.y - node.height / 2,
                node.x + node.width / 2, node.y + node.height / 2);
        boolean selected = node.isRoot || node == linkSource || node == linkTarget;
        float radius = dp(12);
        fillPaint.setColor(color(person.deceased ? R.color.deceasedCard : R.color.backgroundCard));
        canvas.drawRoundRect(rect, radius, radius, fillPaint);
        strokePaint.setColor(color(selected ? R.color.primaryDarkColor
                : person.deceased ? R.color.deceasedCardBorder : R.color.cardBorder));
        strokePaint.setStrokeWidth(dp(selected ? 2.4f : 1.2f));
        canvas.drawRoundRect(rect, radius, radius, strokePaint);
        strokePaint.setStrokeWidth(dp(1.2f));

        float avatarX = rect.left + dp(20);
        avatarPaint.setColor(avatarColor(person, node.isRoot));
        canvas.drawCircle(avatarX, node.y, dp(13), avatarPaint);
        avatarTextPaint.setColor(color(R.color.textLight));
        canvas.drawText(person.initial(), avatarX,
                node.y - (avatarTextPaint.ascent() + avatarTextPaint.descent()) / 2,
                avatarTextPaint);

        float textLeft = rect.left + dp(38);
        namePaint.setColor(color(person.deceased ? R.color.deceasedText : R.color.textPrimary));
        canvas.drawText(person.name, textLeft, node.y - dp(3), namePaint);
        termPaint.setColor(color(node.isRoot ? R.color.primaryDarkColor : R.color.secondaryColor));
        canvas.drawText(item.sub, textLeft, node.y + dp(14), termPaint);
    }

    private int avatarColor(Person person, boolean isRoot) {
        if (isRoot) return color(R.color.primaryColor);
        if (person.deceased) return color(R.color.deceasedAvatar);
        if (person.isMale()) return color(R.color.mondayColor);
        if (person.isFemale()) return color(R.color.secondaryColor);
        return color(R.color.textSecondary);
    }

    private void drawLinkDraft(Canvas canvas) {
        if (linkSource == null) return;
        strokePaint.setColor(color(R.color.accentColor));
        strokePaint.setStrokeWidth(dp(2.4f));
        canvas.drawLine(linkSource.x, linkSource.y, linkX, linkY, strokePaint);
        canvas.drawCircle(linkX, linkY, dp(5), strokePaint);
        if (linkTarget != null && linkTarget != linkSource) {
            RectF rect = new RectF(linkTarget.x - linkTarget.width / 2 - dp(4),
                    linkTarget.y - linkTarget.height / 2 - dp(4),
                    linkTarget.x + linkTarget.width / 2 + dp(4),
                    linkTarget.y + linkTarget.height / 2 + dp(4));
            canvas.drawRoundRect(rect, dp(14), dp(14), strokePaint);
        }
        strokePaint.setStrokeWidth(dp(1.2f));
    }

    // ---- cảm ứng ----------------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaler.onTouchEvent(event);
        gestures.onTouchEvent(event);
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (linkSource != null) finishLink();
            pressNode = null;
        }
        return true;
    }

    private void finishLink() {
        FamilyTreeLayout.Node from = linkSource;
        FamilyTreeLayout.Node to = linkTarget;
        linkSource = null;
        linkTarget = null;
        invalidate();
        if (listener == null || from == null || to == null || from == to) return;
        listener.onLinkPersons(((Item) from.tag).person, ((Item) to.tag).person);
    }

    private FamilyTreeLayout.Node nodeAt(float screenX, float screenY) {
        applyMatrix();
        if (!matrix.invert(inverse)) return null;
        point[0] = screenX;
        point[1] = screenY;
        inverse.mapPoints(point);
        for (int i = nodes.size() - 1; i >= 0; i--) {
            FamilyTreeLayout.Node node = nodes.get(i);
            if (Math.abs(point[0] - node.x) <= node.width / 2
                    && Math.abs(point[1] - node.y) <= node.height / 2) return node;
        }
        return null;
    }

    private final class Gesture extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            pressNode = nodeAt(event.getX(), event.getY());
            return true;
        }

        @Override
        public void onLongPress(MotionEvent event) {
            FamilyTreeLayout.Node node = nodeAt(event.getX(), event.getY());
            if (node == null || listener == null) return;
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            listener.onEditPerson(((Item) node.tag).person);
        }

        @Override
        public boolean onScroll(MotionEvent first, MotionEvent current,
                                float distanceX, float distanceY) {
            if (linkSource != null) {
                linkX = current.getX();
                linkY = current.getY();
                toWorld();
                linkTarget = nodeAt(current.getX(), current.getY());
                invalidate();
                return true;
            }
            if (scaler.isInProgress()) return true;
            translateX -= distanceX;
            translateY -= distanceY;
            invalidate();
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            FamilyTreeLayout.Node node = nodeAt(event.getX(), event.getY());
            if (node != null && listener != null) {
                listener.onPickRoot(((Item) node.tag).person);
            }
            return node != null;
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            FamilyTreeLayout.Node node = nodeAt(event.getX(), event.getY());
            if (node == null) {
                fitToContent();
                invalidate();
                return true;
            }
            if (listener != null) listener.onEditPerson(((Item) node.tag).person);
            return true;
        }
    }

    private final class Scale extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            float worldX = (focusX - translateX) / scale;
            float worldY = (focusY - translateY) / scale;
            float next = Math.max(Math.min(scale * detector.getScaleFactor(), MAX_SCALE),
                    Math.max(MIN_SCALE, fitScale * 0.5f));
            scale = next;
            translateX = focusX - worldX * scale;
            translateY = focusY - worldY * scale;
            invalidate();
            return true;
        }
    }

    /** Đổi điểm thả về tọa độ thế giới để vẽ nét nối không phụ thuộc độ phóng. */
    private void toWorld() {
        applyMatrix();
        if (!matrix.invert(inverse)) return;
        point[0] = linkX;
        point[1] = linkY;
        inverse.mapPoints(point);
        linkX = point[0];
        linkY = point[1];
    }

    private void fitToContent() {
        float width = Math.max(dp(1), contentMaxX - contentMinX);
        float height = Math.max(dp(1), contentMaxY - contentMinY);
        float availableWidth = Math.max(dp(1), getWidth() - dp(24));
        float availableHeight = Math.max(dp(1), getHeight() - dp(36));
        fitScale = Math.max(MIN_SCALE, Math.min(availableWidth / width, 1f));
        scale = Math.min(fitScale, Math.max(availableHeight / height, MIN_SCALE));
        fitScale = scale;
        translateX = (availableWidth - width * scale) / 2 - contentMinX * scale + dp(12);
        translateY = dp(28) - contentMinY * scale;
    }

    private int color(int resId) {
        return ContextCompat.getColor(getContext(), resId);
    }

    private float dp(float value) {
        return value * density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
