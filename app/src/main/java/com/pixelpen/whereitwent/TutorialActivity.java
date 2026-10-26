package com.pixelpen.whereitwent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TutorialActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        EdgeInsets.padBottom(this);   // v3.4: clear of the navigation bar

        // Load markdown text from raw resource
        TextView tv = findViewById(R.id.tv_readme);
        tv.setText(render(GuideMarkdown.parse(loadGuideMarkdown())));

        // Back via CLOSE button
        Button btnClose = findViewById(R.id.btn_close_readme);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }
    }

    // ---- v3.4: draw the Guide's Markdown (headings, dividers, bullets, bold) ----

    private static final int INK = 0xFF024265;        // app blue, as the top bars
    private static final int RULE = 0xFFCFD8DC;

    private CharSequence render(java.util.List<GuideMarkdown.Block> blocks) {
        android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
        float d = getResources().getDisplayMetrics().density;
        int f = android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
        for (GuideMarkdown.Block b : blocks) {
            int start = sb.length();
            if (b.kind == GuideMarkdown.Kind.GAP) {
                sb.append("\n");
                sb.setSpan(new android.text.style.RelativeSizeSpan(0.6f), start, sb.length(), f);
                continue;
            }
            if (b.kind == GuideMarkdown.Kind.RULE) {
                sb.append(" \n");
                sb.setSpan(new RuleSpan(RULE, Math.max(1f, d)), start, sb.length(), f);
                sb.setSpan(new android.text.style.RelativeSizeSpan(0.6f), start, sb.length(), f);
                continue;
            }
            sb.append(b.text);
            for (int[] r : b.bold) {
                sb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        start + r[0], start + r[1], f);
            }
            sb.append("\n");
            int end = sb.length();
            switch (b.kind) {
                case TITLE:
                    heading(sb, start, end, 1.4f);
                    break;
                case HEADING:
                    heading(sb, start, end, 1.2f);
                    break;
                case BULLET:
                    sb.setSpan(new android.text.style.BulletSpan(Math.round(10 * d), INK), start, end, f);
                    break;
                default:
                    break;
            }
        }
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') sb.delete(sb.length() - 1, sb.length());
        return sb;
    }

    private static void heading(android.text.SpannableStringBuilder sb, int start, int end, float size) {
        int f = android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
        sb.setSpan(new android.text.style.RelativeSizeSpan(size), start, end, f);
        sb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, f);
        sb.setSpan(new android.text.style.ForegroundColorSpan(INK), start, end, f);
    }

    /** A thin line across the card, for "---". */
    private static final class RuleSpan implements android.text.style.LineBackgroundSpan {
        private final int color;
        private final float thickness;
        RuleSpan(int color, float thickness) { this.color = color; this.thickness = thickness; }

        @Override
        public void drawBackground(android.graphics.Canvas c, android.graphics.Paint p,
                                   int left, int right, int top, int baseline, int bottom,
                                   CharSequence text, int start, int end, int lineNumber) {
            int old = p.getColor();
            p.setColor(color);
            float y = (top + bottom) / 2f;
            c.drawRect(left, y, right, y + thickness, p);
            p.setColor(old);
        }
    }

    // Reads raw/guide.md → String
    private String loadGuideMarkdown() {
        try {
            InputStream is = getResources().openRawResource(R.raw.guide);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return "Unable to load guide.";
        }
    }
}
