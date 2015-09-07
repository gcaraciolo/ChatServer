package br.com.chat.server;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

	public static void console(String str){		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:mm");		
		System.out.println("[" + sdf.format(new Date())+ "]: " + str);
	}
	
}
