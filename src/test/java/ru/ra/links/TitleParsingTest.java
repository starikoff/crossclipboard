package ru.ra.links;

import static org.junit.Assert.*;

import org.junit.Test;

public class TitleParsingTest {
    @Test
    public void testNull() {
        assertNull(TitleExtractor.extractTitle(null));
    }

    @Test
    public void testEmpty() {
        assertNull(TitleExtractor.extractTitle(""));
    }

    @Test
    public void testNoHead() {
        assertNull(
            TitleExtractor.extractTitle("<html><body>foo bar</body></html>"));
    }

    @Test
    public void testNoTitle() {
        assertNull(TitleExtractor.extractTitle(
            "<html><head><link rel='aaa'></head><body>foo bar</body></html>"));
    }

    @Test
    public void testNoHeadEndTitlePresent() {
        String title = "The Title | The Journal";
        assertEquals(title,
            TitleExtractor.extractTitle("<html><head><link rel='aaa'><title>"
                + title + "</title><body>foo bar</body></html>"));
        assertEquals(title, TitleExtractor.extractTitle(
            "<html><head><link rel='aaa'><title>" + title + "</title>"));
    }

    @Test
    public void testNoTitleEnd() {
        String title = "The Title | The Journal";
        assertEquals(title,
            TitleExtractor.extractTitle("<html><head><link rel='aaa'><title>"
                + title + "<body>foo bar</body></html>"));
        assertEquals(title, TitleExtractor
            .extractTitle("<html><head><link rel='aaa'><title>" + title));
    }

    @Test
    public void testNoHeadEndNoTitle() {
        assertNull(TitleExtractor.extractTitle(
            "<html><head><link rel='aaa'><body>foo bar</body></html>"));
        assertNull(TitleExtractor.extractTitle("<html><head><link rel='aaa'>"));
        assertNull(TitleExtractor.extractTitle("<html><head>"));
    }

    @Test
    public void testTitle() {
        String title = "The Title | News";
        assertEquals(title,
            TitleExtractor.extractTitle("<html><head><link rel='aaa'><title>"
                + title + "</title></head><body>foo bar</body></html>"));
    }

    @Test
    public void testTitleCased() {
        String title = "The Title | News";
        assertEquals(title,
            TitleExtractor
                .extractTitle("<HTML><HEAD><LINK REL='aaa'><TITLE attr='val'>"
                    + title + "</TITLE></HEAD><BODY>foo bar</BODY></HTML>"));
    }

    @Test
    public void testCharsetExtraction() {
        assertEquals("UTF-8", TitleExtractor.getCharset(
            "<html><head><meta charset=\"UTF-8\"><title>foo</title></head><body></body></html>"));
        assertEquals("UTF-8", TitleExtractor.getCharset(
            "<html><head><meta charset=UTF-8><title>foo</title></head><body></body></html>"));
        assertEquals("UTF-8", TitleExtractor.getCharset(
            "<html><head><meta charset=UTF-8/><title>foo</title></head><body></body></html>"));
        assertEquals("utf-8", TitleExtractor.getCharset(
            "<html><head><META CHARSET=\"utf-8\" /><title>foo</title></head><body></body></html>"));
        assertEquals("UTF-8", TitleExtractor.getCharset(
            "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"><title>foo</title></head><body></body></html>"));
    }
}
