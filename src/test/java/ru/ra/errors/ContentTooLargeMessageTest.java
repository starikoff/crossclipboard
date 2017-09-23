package ru.ra.errors;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

import ru.ra.errors.ContentTooLargeException;

public class ContentTooLargeMessageTest {
	@Test
	public void test1() {
		Locale.setDefault(Locale.ENGLISH);
		assertEquals("Size limit is 100 kB, while you provided 214.5 kB",
				ContentTooLargeException.message(100 * 1024, 215 * 1024 - 512));
	}
}
