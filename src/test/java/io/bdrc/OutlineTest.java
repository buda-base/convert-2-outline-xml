package io.bdrc;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import io.bdrc.tools.outlines.Convert2OutlineXML;

public class OutlineTest {

	@Test
	public void testW8LS32723() throws IOException {
		final ClassLoader classLoader = OutlineTest.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("Outline-W8LS32723.csv");
        Convert2OutlineXML.extended = true;
        Convert2OutlineXML.process(inputStream, "O8LS32723", System.out, "subjectCollection", "text", "me");
	}
	
}
