package com.ezhang.pop.core;

public class StringUtils {
	public static String Join(String[] strings, String sep) {
		StringBuilder builder = new StringBuilder();
		for (String s : strings) {
			builder.append(s);
			builder.append(sep);
		}
		builder.delete(builder.length() - sep.length(), builder.length());
		return builder.toString();
	}
}
