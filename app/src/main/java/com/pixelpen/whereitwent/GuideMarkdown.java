package com.pixelpen.whereitwent;

import java.util.ArrayList;
import java.util.List;

/**
 * The small part of Markdown the Guide uses (v3.4): "## " and "### "
 * headings, "---" dividers, "* " or "- " bullets, paragraphs, and
 * **bold** inside a line. Pure Java, unit-tested; TutorialActivity draws
 * the blocks.
 */
public final class GuideMarkdown {

    public enum Kind { TITLE, HEADING, RULE, BULLET, TEXT, GAP }

    public static final class Block {
        public final Kind kind;
        /** Text with the Markdown marks removed. */
        public final String text;
        /** Bold ranges in {@link #text}: start, end pairs. */
        public final List<int[]> bold;
        Block(Kind kind, String text, List<int[]> bold) {
            this.kind = kind; this.text = text; this.bold = bold;
        }
    }

    private GuideMarkdown() { }

    public static List<Block> parse(String md) {
        List<Block> out = new ArrayList<>();
        if (md == null) return out;
        for (String raw : md.replace("\r", "").split("\n", -1)) {
            String line = raw.trim();
            if (line.isEmpty()) {
                // One gap between blocks, never several in a row or at the start
                if (!out.isEmpty() && out.get(out.size() - 1).kind != Kind.GAP) {
                    out.add(new Block(Kind.GAP, "", new ArrayList<>()));
                }
            } else if (line.matches("-{3,}|\\*{3,}|_{3,}")) {
                out.add(new Block(Kind.RULE, "", new ArrayList<>()));
            } else if (line.startsWith("### ")) {
                out.add(inline(Kind.HEADING, line.substring(4)));
            } else if (line.startsWith("## ") || line.startsWith("# ")) {
                out.add(inline(Kind.TITLE, line.substring(line.indexOf(' ') + 1)));
            } else if (line.startsWith("* ") || line.startsWith("- ")) {
                out.add(inline(Kind.BULLET, line.substring(2)));
            } else {
                out.add(inline(Kind.TEXT, line));
            }
        }
        while (!out.isEmpty() && out.get(out.size() - 1).kind == Kind.GAP) out.remove(out.size() - 1);
        return out;
    }

    /** Removes **bold** marks, remembering where they were. */
    private static Block inline(Kind kind, String s) {
        StringBuilder text = new StringBuilder();
        List<int[]> bold = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            int open = s.indexOf("**", i);
            int close = open < 0 ? -1 : s.indexOf("**", open + 2);
            if (open < 0 || close < 0) { text.append(s.substring(i)); break; }
            text.append(s, i, open);
            int start = text.length();
            text.append(s, open + 2, close);
            bold.add(new int[]{start, text.length()});
            i = close + 2;
        }
        return new Block(kind, text.toString().trim(), bold);
    }
}
