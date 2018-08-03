package com.boeing.cas.supa.ground.utils;

public class UsernamePolicyEnforcer {

	public static final String ERROR_USERNAME_FAILED_KEY = "INVALID_USERNAME_FORMAT";
	public static final String ERROR_USERNAME_FAILED_DESCRIPTION = new StringBuilder()
			.append("Username must be between 3 to 64 characters in length, and use only lower-case alphabets. ")
			.append("It must begin with an alphabet, followed by a period (\".\"), or one or more alphanumeric characters. ")
			.append("The period can be used anywhere within the username except at the beginning or immediately preceding the @domain portion of the username.")
			.toString();

	private static final String PATTERN_USERNAME_REGEX = new StringBuilder('^')
			// the string should contain 3 to 10 chars
			.append("(?=.{3,64}$)")
			// the string should start with a lowercase ASCII letter
			.append("[a-z]")
			// then followed by zero or more lowercase ASCII letters or/and digits
			.append("[a-z0-9]*")
			// an optional sequence of a period (".") followed with 1 or more lowercase ASCII letters
			// or/and digits (that + means you can't have . at the end of the string and ? guarantees
			// the period can only appear once in the string)
			.append("(?:\\.[a-z0-9]+)?")
			.append('$')
		.toString();
	
	public static boolean validate(String username) {
		return username.matches(PATTERN_USERNAME_REGEX);
	}
}
