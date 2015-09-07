package br.com.chat.server;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class Message {

	//Métodos auxiliares..
		public static void enviaMensagem(Socket socket, String message) throws IOException {
			Utils.console(message);//exibe no console do servidor apenas apara debugg
			PrintStream ps = new PrintStream(socket.getOutputStream(), true);
			ps.println(message);
		}
	
		/**
		 * Método auxiliar utilizado para enviar uma mensagem para todos os usuarios conectados
		 * @param cmd
		 * @param notLogin
		 * @param menssage
		 * @throws JSONException
		 * @throws IOException
		 */
		public static void broadcast(String command, String notLogin, String menssage, Map<String, Connection> mapConnections) throws JSONException, IOException {
			
			JSONObject requestTo = new JSONObject();
			Utils.console("BROADCAST");
			for (String login : mapConnections.keySet()) {
				
				if(!login.equals(notLogin)){
					Connection connection = mapConnections.get(login);
					
					requestTo.put("cmd", command);
					requestTo.put("from", "Server");
					requestTo.put("data", menssage);
					
					Message.enviaMensagem(connection.getSocket(), requestTo.toString());
				}
				
			}
		}	
}
