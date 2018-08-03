package com.boeing.cas.supa.ground.utils;

public class PasswordPolicyEnforcer {

	public static final String ERROR_PASSWORD_FAILED_KEY = "INVALID_PASSWORD_FORMAT";
	public static final String ERROR_PASSWORD_FAILED_DESCRIPTION = new StringBuilder()
			.append("Password must contain 8 to 16 characters, which must comprise of at least three out of the four character classes:\n")
			.append("- Uppercase alphabets A to Z\n")
			.append("- Lowercase alphabets a to z\n")
			.append("- Numerals 0 to 9\n")
			.append("- Symbols @ # $ % ^ & * - _ ! + = [ ] { } | \\ : ‘ , . ? / ` ~ “ ( ) ;")
			.toString();

	private static final String PATTERN_PASSWORD_REGEX = new StringBuilder('^')
			// combination of lowercase, uppercase and numbers, 8 to 16 characters in length
			.append("((?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])).{8,16}")
			.append('|') // -or-
			// combination of lowercase, uppercase and symbols, 8 to 16 characters in length
			.append("((?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&*\\-_!+=\\[\\]{}|\\:‘,.?\\/`~“\\(\\);])).{8,16}")
			.append('|') // -or-
			// combination of lowercase, numbers and symbols, 8 to 16 characters in length
			.append("((?=.*[a-z])(?=.*[0-9])(?=.*[@#$%^&*\\-_!+=\\[\\]{}|\\:‘,.?\\/`~“\\(\\);])).{8,16}")
			.append('|') // -or-
			// combination of uppercase, numbers and symbols 8 to 16 characters in length
			.append("((?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&*\\-_!+=\\[\\]{}|\\:‘,.?\\/`~“\\(\\);])).{8,16}")
			.append('$')
			.toString();
	
	public static boolean validate(String password) {
		return password.matches(PATTERN_PASSWORD_REGEX);
	}
}
