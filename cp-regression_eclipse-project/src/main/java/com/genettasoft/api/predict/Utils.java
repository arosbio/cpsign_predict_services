package com.genettasoft.api.predict;

public class Utils {
	
	private static final int MAX_NUM_STACK_TO_LOGG = 10;
	
	public static double roundTo3digits(double val){
		return Math.round(val*1000.0)/1000.0;
	}

	public static String getStackTrace(Throwable e) {
		StringBuilder sb = new StringBuilder();
		StackTraceElement[] stack = e.getStackTrace();
		
		sb.append(e.getClass());
		sb.append(": ");
		sb.append(e.getMessage());
		for (int i=0; i<MAX_NUM_STACK_TO_LOGG && i<stack.length; i++) {
			sb.append('\n');
			sb.append('\t');
			sb.append(stack[i]);
		}
		
		return sb.toString();
	}
}
