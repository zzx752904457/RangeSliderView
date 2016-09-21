package githubzzx752904457.com.rangesliderview.widgets;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Shader;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;

import java.util.concurrent.TimeUnit;

import githubzzx752904457.com.rangesliderview.R;
import githubzzx752904457.com.rangesliderview.interfaces.AnimateMethod;

public class RangeSliderView extends View {

    private static final String TAG = RangeSliderView.class.getSimpleName();

    private static final long RIPPLE_ANIMATION_DURATION_MS = TimeUnit.MILLISECONDS.toMillis(700);

    private static final int DEFAULT_PAINT_STROKE_WIDTH = 5;

    private static final int DEFAULT_FILLED_COLOR = Color.parseColor("#FFA500");

    private static final int DEFAULT_EMPTY_COLOR = Color.parseColor("#C3C3C3");

    private static final float DEFAULT_BAR_HEIGHT_PERCENT = 0.10f;

    private static final float DEFAULT_SLOT_RADIUS_PERCENT = 0.125f;

    private static final float DEFAULT_SLIDER_RADIUS_PERCENT = 0.25f;

    private static final int DEFAULT_RANGE_COUNT = 5;

    private static final int DEFAULT_HEIGHT_IN_DP = 50;

    protected Paint paint;

    protected Paint ripplePaint;

    protected float radius;

    protected float slotRadius;

    private int currentIndex;

    private float currentSlidingX;

    private float currentSlidingY;

    private float selectedSlotX;

    private float selectedSlotY;

    private boolean gotSlot = false;

    private float[] slotPositions;

    private int filledColor = DEFAULT_FILLED_COLOR;

    private int emptyColor = DEFAULT_EMPTY_COLOR;

    private float barHeightPercent = DEFAULT_BAR_HEIGHT_PERCENT;

    private int rangeCount = DEFAULT_RANGE_COUNT;

    private int barHeight;

    private OnSlideListener listener;

    private float rippleRadius = 0.0f;

    private float downX;

    private float downY;

    private Path innerPath = new Path();

    private Path outerPath = new Path();

    private float slotRadiusPercent = DEFAULT_SLOT_RADIUS_PERCENT;

    private float sliderRadiusPercent = DEFAULT_SLIDER_RADIUS_PERCENT;

    private int layoutHeight;

    private boolean drawImg;

    private int imgResource;

    public RangeSliderView(Context context) {
        this(context, null);
    }

    public RangeSliderView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public RangeSliderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RangeSliderView);
            TypedArray sa = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.layout_height});
            try {
                layoutHeight = sa.getLayoutDimension(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT);
                rangeCount = a.getInt(
                        R.styleable.RangeSliderView_rangeCount, DEFAULT_RANGE_COUNT);
                filledColor = a.getColor(
                        R.styleable.RangeSliderView_filledColor, DEFAULT_FILLED_COLOR);
                emptyColor = a.getColor(
                        R.styleable.RangeSliderView_emptyColor, DEFAULT_EMPTY_COLOR);
                barHeightPercent = a.getFloat(
                        R.styleable.RangeSliderView_barHeightPercent, DEFAULT_BAR_HEIGHT_PERCENT);
                barHeightPercent = a.getFloat(
                        R.styleable.RangeSliderView_barHeightPercent, DEFAULT_BAR_HEIGHT_PERCENT);
                slotRadiusPercent = a.getFloat(
                        R.styleable.RangeSliderView_slotRadiusPercent, DEFAULT_SLOT_RADIUS_PERCENT);
                sliderRadiusPercent = a.getFloat(
                        R.styleable.RangeSliderView_sliderRadiusPercent, DEFAULT_SLIDER_RADIUS_PERCENT);
            } finally {
                a.recycle();
                sa.recycle();
            }
        }

        setBarHeightPercent(barHeightPercent);
        setRangeCount(rangeCount);
        setSlotRadiusPercent(slotRadiusPercent);
        setSliderRadiusPercent(sliderRadiusPercent);

        slotPositions = new float[rangeCount];
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(DEFAULT_PAINT_STROKE_WIDTH);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ripplePaint.setStrokeWidth(2.0f);
        ripplePaint.setStyle(Paint.Style.FILL_AND_STROKE);

        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                getViewTreeObserver().removeOnPreDrawListener(this);

                // 获取新的高度后更新半径
                updateRadius(getHeight());

                // 重新计算画的位置
                preComputeDrawingPosition();

                return true;
            }
        });
        currentIndex = 0;
    }

    private void updateRadius(int height) {
        barHeight = (int) (height * barHeightPercent);
        radius = height * sliderRadiusPercent;
        slotRadius = height * slotRadiusPercent;
    }

    public int getRangeCount() {
        return rangeCount;
    }

    public void setRangeCount(int rangeCount) {
        if (rangeCount < 2) {
            throw new IllegalArgumentException("rangeCount must be >= 2");
        }
        this.rangeCount = rangeCount;
    }

    public float getBarHeightPercent() {
        return barHeightPercent;
    }

    public void setBarHeightPercent(float percent) {
        if (percent <= 0.0 || percent > 1.0) {
            throw new IllegalArgumentException("Bar height percent must be in (0, 1]");
        }
        this.barHeightPercent = percent;
    }

    public float getSlotRadiusPercent() {
        return slotRadiusPercent;
    }

    public void setSlotRadiusPercent(float percent) {
        if (percent <= 0.0 || percent > 1.0) {
            throw new IllegalArgumentException("Slot radius percent must be in (0, 1]");
        }
        this.slotRadiusPercent = percent;
    }

    public float getSliderRadiusPercent() {
        return sliderRadiusPercent;
    }

    public void setSliderRadiusPercent(float percent) {
        if (percent <= 0.0 || percent > 1.0) {
            throw new IllegalArgumentException("Slider radius percent must be in (0, 1]");
        }
        this.sliderRadiusPercent = percent;
    }

    @AnimateMethod
    public void setRadius(final float radius) {
        rippleRadius = radius;
        if (rippleRadius > 0) {
            RadialGradient radialGradient = new RadialGradient(
                    downX,
                    downY,
                    rippleRadius * 3,
                    Color.BLACK,
                    Color.TRANSPARENT,
                    Shader.TileMode.MIRROR
            );
            ripplePaint.setShader(radialGradient);
        }
        invalidate();
    }

    public void setOnSlideListener(OnSlideListener listener) {
        this.listener = listener;
    }

    /**
     * Perform all the calculation before drawing, should only run once
     */
    private void preComputeDrawingPosition() {
        int w = getWidthWithPadding();
        int h = getHeightWithPadding();

        /** 每两个点之间的空间 */
        int spacing = w / rangeCount;

        /** 垂直居中显示 */
        int y = getPaddingTop() + h / 2;
        currentSlidingY = y;
        selectedSlotY = y;
        int x = getPaddingLeft() + (spacing / 2);

        /** 保存每个位置点的坐标 */
        for (int i = 0; i < rangeCount; ++i) {
            slotPositions[i] = x;
            if (i == currentIndex) {
                currentSlidingX = x;
                selectedSlotX = x;
            }
            x += spacing;
        }
    }

    //设置初始化时的坐标
    public void setInitialIndex(int index) {
        if (index < 0 || index >= rangeCount) {
            throw new IllegalArgumentException("Attempted to set index=" + index + " out of range [0," + rangeCount + "]");
        }
        currentIndex = index;
        currentSlidingX = selectedSlotX = slotPositions[currentIndex];
        invalidate();
    }

    public int getFilledColor() {
        return filledColor;
    }

    public void setFilledColor(int filledColor) {
        this.filledColor = filledColor;
        invalidate();
    }

    public int getEmptyColor() {
        return emptyColor;
    }

    public void setEmptyColor(int emptyColor) {
        this.emptyColor = emptyColor;
        invalidate();
    }

    /**
     * 设置拖动的图片资源,若不设置默认为画圆
     *
     * @param imgResource
     */
    public void setIcon(int imgResource) {
        drawImg = true;
        this.imgResource = imgResource;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    private int measureHeight(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        int result;
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            final int height;
            if (layoutHeight == ViewGroup.LayoutParams.WRAP_CONTENT) {
                height = dpToPx(getContext(), DEFAULT_HEIGHT_IN_DP);
            } else if (layoutHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
                height = getMeasuredHeight();
            } else {
                height = layoutHeight;
            }
            result = height + getPaddingTop() + getPaddingBottom() + (2 * DEFAULT_PAINT_STROKE_WIDTH);
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    /**
     * @param measureSpec int measure spec to use
     * @return int pixel size
     */
    private int measureWidth(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        int result;
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = specSize + getPaddingLeft() + getPaddingRight() + (2 * DEFAULT_PAINT_STROKE_WIDTH) + (int) (2 * radius);
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    private void updateCurrentIndex() {
        float min = Float.MAX_VALUE;
        int j = 0;
        for (int i = 0; i < rangeCount; ++i) {
            float dx = Math.abs(currentSlidingX - slotPositions[i]);
            if (dx < min) {
                min = dx;
                j = i;
            }
        }
        if (j != currentIndex) {
            if (listener != null) {
                listener.onSlide(j);
            }
        }
        currentIndex = j;
        currentSlidingX = slotPositions[j];
        selectedSlotX = currentSlidingX;
        downX = currentSlidingX;
        downY = currentSlidingY;
        animateRipple();
        invalidate();
    }

    private void animateRipple() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "radius", 0, radius);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.setDuration(RIPPLE_ANIMATION_DURATION_MS);
        animator.start();
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                rippleRadius = 0;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float y = event.getY();
        float x = event.getX();
        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //判断是否点击在滑块范围内
                gotSlot = isInSelectedSlot(x, y);
                downX = x;
                downY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                //要点击滑块才能拖动
                if (gotSlot) {
                    if (x >= slotPositions[0] && x <= slotPositions[rangeCount - 1]) {
                        currentSlidingX = x;
                        currentSlidingY = y;
                        invalidate();
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (gotSlot) {
                    gotSlot = false;
                    currentSlidingX = x;
                    currentSlidingY = y;
                    updateCurrentIndex();
                }
                break;
        }
        return true;
    }

    private boolean isInSelectedSlot(float x, float y) {
        return
                selectedSlotX - radius <= x && x <= selectedSlotX + radius &&
                        selectedSlotY - radius <= y && y <= selectedSlotY + radius;
    }

    private void drawEmptySlots(Canvas canvas) {
        paint.setColor(emptyColor);
        int h = getHeightWithPadding();
        int y = getPaddingTop() + (h >> 1);
        for (int i = 0; i < rangeCount; ++i) {
            canvas.drawCircle(slotPositions[i], y, slotRadius, paint);
        }
    }

    public int getHeightWithPadding() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    public int getWidthWithPadding() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private void drawFilledSlots(Canvas canvas) {
        paint.setColor(filledColor);
        int h = getHeightWithPadding();
        int y = getPaddingTop() + (h >> 1);
        for (int i = 0; i < rangeCount; ++i) {
            if (slotPositions[i] <= currentSlidingX) {
                canvas.drawCircle(slotPositions[i], y, slotRadius, paint);
            }
        }
    }

    private void drawBar(Canvas canvas, int from, int to, int color) {
        paint.setColor(color);
        int h = getHeightWithPadding();
        int half = (barHeight >> 1);
        int y = getPaddingTop() + (h >> 1);
        canvas.drawRect(from, y - half, to, y + half, paint);
    }

//    private void drawRippleEffect(Canvas canvas) {
//        if (rippleRadius != 0) {
//            canvas.save();
//            ripplePaint.setColor(Color.GRAY);
//            outerPath.reset();
//            outerPath.addCircle(downX, downY, rippleRadius, Path.Direction.CW);
//            canvas.clipPath(outerPath);
//            innerPath.reset();
//            innerPath.addCircle(downX, downY, rippleRadius / 3, Path.Direction.CW);
//            canvas.clipPath(innerPath, Region.Op.DIFFERENCE);
//            canvas.drawCircle(downX, downY, rippleRadius, ripplePaint);
//            canvas.restore();
//        }
//    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidthWithPadding();
        int h = getHeightWithPadding();
        int spacing = w / rangeCount;
        int border = (spacing >> 1);
        int x0 = getPaddingLeft() + border;
        int y0 = getPaddingTop() + (h >> 1);

        /**画还没有选中的圆 */
        drawEmptySlots(canvas);
        /** 画已经选中的圆 */
        drawFilledSlots(canvas);

        /**画还没有选中的横线 */
        drawBar(canvas, (int) slotPositions[0], (int) (slotPositions[rangeCount - 1] + slotRadius * 4), emptyColor);

        /**画已经选中的横线 */
        drawBar(canvas, (int) (x0 - slotRadius * 4), (int) (currentSlidingX), filledColor);


        if (drawImg) {
            /** 画拖动的图片 */
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), imgResource);
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            // 设置想要的大小
            int newWidth = (int) (radius * 3);
            int newHeight = (int) (radius * 3);
            // 计算缩放比例
            float scaleWidth = ((float) newWidth) / width;
            float scaleHeight = ((float) newHeight) / height;
            // 取得想要缩放的matrix参数
            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);
            // 得到新的图片
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix,
                    true);
            canvas.drawBitmap(bitmap, currentSlidingX - bitmap.getWidth() / 2, y0 - bitmap.getHeight() / 2, paint);
            bitmap.recycle();
        } else {
            /** 画拖动的圆 */
            paint.setColor(filledColor);
            canvas.drawCircle(currentSlidingX, y0, radius * 1.5f, paint);
        }


//        drawRippleEffect(canvas);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.saveIndex = this.currentIndex;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.currentIndex = ss.saveIndex;
    }

    static class SavedState extends BaseSavedState {
        int saveIndex;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.saveIndex = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.saveIndex);
        }

        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    /**
     * @param context
     * @param px
     * @return
     */
    static int pxToDp(final Context context, final float px) {
        return (int) (px / context.getResources().getDisplayMetrics().density);
    }

    /**
     * @param context
     * @param dp
     * @return
     */
    static int dpToPx(final Context context, final float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    /**
     * 拖动滑块的回调
     */
    public interface OnSlideListener {

        /**
         * 当滑块拖动到新的点时调用
         *
         * @param index 取值范围是 [0, rangeCount - 1]
         */
        void onSlide(int index);
    }
}
