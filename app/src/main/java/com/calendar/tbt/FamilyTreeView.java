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
 * chạm một người để lấy làm mốc, chạm hai lần để sửa, giữ lâu rồi kéo ô này thả lên
 * ô khác để nối quan hệ. Khổ ô tính theo độ dài tên thật nên không còn cảnh tên bị cắt.
 */
public class FamilyTreeView extends View {

    public interface Listener {
        /** Chạm một lần: đổi người đứng làm mốc tính vai vế. */
        void onPickRoot(Person person);

        /** Chạm hai lần: mở hộp sửa người. */
        void onEditPerson(Person person);

        /** Thả ô đang kéo lên một ô khác: xin nối quan hệ giữa hai người. */
        void onLinkPersons(Person from, Person to);
    }

    /** Giới hạn phóng: đủ nhỏ để coi cả gia phả, đủ to để đọc tên trên màn nhỏ. */
    private static final float MAX_SCALE = 3f;
    private static final float MIN_SCALE = 0.18f;

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
        FamilyTreeLayout.Result layout = FamilyTreeLayout.layout(nodes);
        contentMinX = layout.minX;
        contentMinY = layout.minY;
        contentMaxX = layout.maxX;
        contentMaxY = layout.maxY;
        rows.addAll(layout.rows);
        needsFit = true;
        if (getWidth() > 0) {
            needsFit = false;
            fitToContent();
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

    /** Vợ chồng nối bằng một đoạn ngang; cha mẹ – con bằng đường khuỷu xuống tận từng đứa. */
    private void drawLinks(Canvas canvas) {
        Set<String> drawn = new HashSet<>();
        for (FamilyTreeLayout.Node node : nodes) {
            if (drawn.contains(node.id)) continue;
            FamilyTreeLayout.Node spouse = node.spouseId == null ? null : byId.get(node.spouseId);
            drawn.add(node.id);
            if (spouse != null) drawn.add(spouse.id);
            List<FamilyTreeLayout.Node> children = childrenOf(node, spouse);
            if (spouse != null) drawCouple(canvas, node, spouse, children.isEmpty());
            if (children.isEmpty()) continue;
            float junctionX = spouse == null ? node.x : (node.x + spouse.x) / 2;
            float junctionY = node.y + node.height / 2;
            float childTop = Float.MAX_VALUE;
            for (FamilyTreeLayout.Node child : children) {
                childTop = Math.min(childTop, child.y - child.height / 2);
            }
            float branchY = (junctionY + childTop) / 2;
            Path path = new Path();
            path.moveTo(junctionX, junctionY);
            path.lineTo(junctionX, branchY);
            for (FamilyTreeLayout.Node child : children) {
                elbow(path, junctionX, branchY, child.x, child.y - child.height / 2);
                path.moveTo(junctionX, branchY);
            }
            canvas.drawPath(path, linePaint);
        }
    }

    private void drawCouple(Canvas canvas, FamilyTreeLayout.Node left, FamilyTreeLayout.Node right,
                            boolean hasChildren) {
        FamilyTreeLayout.Node first = left.x <= right.x ? left : right;
        FamilyTreeLayout.Node second = left.x <= right.x ? right : left;
        strokePaint.setColor(color(R.color.secondaryColor));
        strokePaint.setStrokeWidth(dp(2f));
        canvas.drawLine(first.x + first.width / 2, first.y,
                second.x - second.width / 2, second.y, strokePaint);
        strokePaint.setStrokeWidth(dp(1.2f));
        if (hasChildren) return;
        fillPaint.setColor(color(R.color.secondaryColor));
        canvas.drawCircle((left.x + right.x) / 2, left.y, dp(3.5f), fillPaint);
    }

    /** Khuỷu bo góc: đi ngang tới cột của đứa con rồi mới xuống, ra dáng cành cây. */
    private void elbow(Path path, float fromX, float fromY, float toX, float toY) {
        float span = Math.abs(toX - fromX);
        if (span < 0.5f) {
            path.lineTo(toX, toY);
            return;
        }
        float direction = toX > fromX ? 1 : -1;
        float corner = Math.min(dp(14), span / 2);
        path.lineTo(toX - direction * corner, fromY);
        path.quadTo(toX, fromY, toX, fromY + corner);
        path.lineTo(toX, toY);
    }

    private List<FamilyTreeLayout.Node> childrenOf(FamilyTreeLayout.Node left,
                                                  FamilyTreeLayout.Node right) {
        List<FamilyTreeLayout.Node> result = new ArrayList<>();
        for (FamilyTreeLayout.Node candidate : nodes) {
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
            if (pressNode == null || linkSource != null) return;
            linkSource = pressNode;
            linkTarget = null;
            linkX = event.getX();
            linkY = event.getY();
            toWorld();
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            invalidate();
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
