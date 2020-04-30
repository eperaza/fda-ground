package com.boeing.cas.supa.ground.utils;

import java.security.SecureRandom;

public class ActivationCodeGenerator {

	static final String AB = "34679ACDEFGHJKMNPQRTUVWXY";
	static SecureRandom rnd = new SecureRandom();

	public static String randomString( int len ){
		StringBuilder sb = new StringBuilder( len );
		for( int i = 0; i < len; i++ )
			sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
		return sb.toString();
	}
}
