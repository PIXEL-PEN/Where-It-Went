package com.pixelpen.whereitwent;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;

public class GuideMarkdownTest {

    @Test
    public void readsTheGuidesMarkdown() {
        String md = "\n## Where It Went — Quick Guide\n\nYou log what you spend.\n\n---\n\n"
                + "### Daily Living\n\nEach category has a tag:\n\n* Fixed\n- Necessity\n\n\n"
                + "Settings → **Accounts only** opens on Accounts.\n\n---\n";
        List<GuideMarkdown.Block> b = GuideMarkdown.parse(md);
        GuideMarkdown.Kind[] kinds = new GuideMarkdown.Kind[b.size()];
        for (int i = 0; i < b.size(); i++) kinds[i] = b.get(i).kind;
        assertArrayEquals(new GuideMarkdown.Kind[]{
                GuideMarkdown.Kind.TITLE, GuideMarkdown.Kind.GAP,
                GuideMarkdown.Kind.TEXT, GuideMarkdown.Kind.GAP,
                GuideMarkdown.Kind.RULE, GuideMarkdown.Kind.GAP,
                GuideMarkdown.Kind.HEADING, GuideMarkdown.Kind.GAP,
                GuideMarkdown.Kind.TEXT, GuideMarkdown.Kind.GAP,
                GuideMarkdown.Kind.BULLET, GuideMarkdown.Kind.BULLET, GuideMarkdown.Kind.GAP,
                GuideMarkdown.Kind.TEXT, GuideMarkdown.Kind.GAP,
                GuideMarkdown.Kind.RULE}, kinds);
        assertEquals("Where It Went — Quick Guide", b.get(0).text);
        assertEquals("Daily Living", b.get(6).text);
        assertEquals("Necessity", b.get(11).text);

        GuideMarkdown.Block bold = b.get(13);
        assertEquals("Settings → Accounts only opens on Accounts.", bold.text);
        assertEquals(1, bold.bold.size());
        assertEquals("Accounts only",
                bold.text.substring(bold.bold.get(0)[0], bold.bold.get(0)[1]));
    }

    @Test
    public void unmatchedBoldMarksStayAsText() {
        GuideMarkdown.Block b = GuideMarkdown.parse("A **half bold line").get(0);
        assertEquals("A **half bold line", b.text);
        assertEquals(0, b.bold.size());
    }
}
