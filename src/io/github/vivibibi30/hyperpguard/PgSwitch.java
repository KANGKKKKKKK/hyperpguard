package io.github.vivibibi30.hyperpguard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.Switch;

/**
 * MIUI 风格的开关。
 *
 * 系统的 Switch 在这版主题下会被裁成很小的圆角块，轨道与滑块的边界也不清楚，
 * 这里只接管绘制：轨道 49×28dp、滑块直径 20dp、关态用浅灰实底，
 * 触摸、无障碍、checked 状态仍由 Switch 负责。
 */
final class PgSwitch extends Switch {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int onColor;
    private final int offColor;
    private final int thumbColor = 0xFFFFFFFF;
    private final float density;

    PgSwitch(Context context, int onColor, int offColor) {
        super(context);
        this.onColor = onColor;
        this.offColor = offColor;
        this.density = context.getResources().getDisplayMetrics().density;
        setShowText(false);
        setText(null);
        setBackground(null);
        setPadding(0, 0, 0, 0);
        setThumbDrawable(null);
        setTrackDrawable(null);
        setMinWidth(Math.round(49 * density));
        setMinHeight(Math.round(28 * density));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float radius = h / 2f;

        paint.setColor(isChecked() ? onColor : offColor);
        canvas.drawRoundRect(0, 0, w, h, radius, radius, paint);

        // Settings 里的取值：轨道 49×28、滑块 20dp、左右与上下各留 4dp
        float pad = 4 * density;
        float thumbRadius = 10 * density;
        float cx = isChecked() ? w - pad - thumbRadius : pad + thumbRadius;
        paint.setColor(thumbColor);
        canvas.drawCircle(cx, h / 2f, thumbRadius, paint);
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        invalidate();
    }

    @Override
    public void toggle() {
        super.toggle();
        invalidate();
    }
}
