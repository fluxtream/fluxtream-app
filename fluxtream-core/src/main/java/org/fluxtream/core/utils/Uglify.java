package org.fluxtream.core.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

import org.apache.commons.io.FileUtils;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import com.yahoo.platform.yui.compressor.JavaScriptCompressor;


public class Uglify {

	public static void main(String[] args) {
		try {
			String js = FileUtils.readFileToString(new File("src/main/webapp/js/jquery1.6.1.min.js"));
			js += FileUtils.readFileToString(new File("src/main/webapp/js/raphael.js"));
			js += FileUtils.readFileToString(new File("src/main/webapp/js/plugins.js"));
			js += FileUtils.readFileToString(new File("src/main/webapp/js/datepicker.js"));
			js += FileUtils.readFileToString(new File("src/main/webapp/js/fluxtream.js"));
			JavaScriptCompressor compressor = new JavaScriptCompressor(new StringReader(js), new YuiCompressorErrorReporter());
			FileWriter fw = new FileWriter("src/main/webapp/js/flx-min.js");
			compressor.compress(fw, 0, false, false, false, false);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	static class YuiCompressorErrorReporter implements ErrorReporter {
		public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {}
		public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {}
		public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
			return new EvaluatorException(message+":line="+line+":lineSource="+lineSource+":lineOffset="+lineOffset);
		}
	}
}
