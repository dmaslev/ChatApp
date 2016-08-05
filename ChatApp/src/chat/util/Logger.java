package chat.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Logger {
	
	public static String printError(Throwable throwable) {
		StringWriter out = new StringWriter();
		PrintWriter writer = new PrintWriter(out);
		throwable.printStackTrace(writer);
		return out.toString();
	}
}