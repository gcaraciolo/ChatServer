package br.com.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.json.JSONException;
import org.json.JSONObject;

public class Server {
	//Mapa que ira guardar toda as conexoes e seus devidos logins
	private static Map<String, Connection> mapConnections = new TreeMap<String, Connection>();
	
	public static void main(String[] args) {
		
		ServerSocket servidor;
		try {
			int port = 12345;
			servidor = new ServerSocket(port);//Solicita ao SO a porta que irá escutar
		
			Utils.console("Iniciando servidor chat na porta: " + port);
			//Esta thread é iniciada para ficar verificando o tempo de inatividade dos usuarios
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					// intervalo de 15 segundos
					while(true){						
						try {	
							//Caso o usuario passe 15 segundos sem enviar o PING o servidor trata de desconectar o socket
							List<String> listRemove = new ArrayList<>();							
							for (String login : mapConnections.keySet()) {								
								Connection connection = mapConnections.get(login);								
								long millis = new Date().getTime() - connection.getDthUltimoAcesso().getTime();								
								if((millis / 1000l) > 15l){
									listRemove.add(login);
									connection.getSocket().close();
								}										
							}							
							synchronized (mapConnections) {
								for (String login : listRemove) {
									mapConnections.remove(login);
								}
							}							
							//envia uma mensagem de broadcast para os outros usuários informando que tal usuario ficou offline
							for (String login : listRemove) {
								String msg = login + " estar offline";
								String cmd = "REQUEST_USER_QUIT";
								Message.broadcast(cmd, login, msg, mapConnections);
							}							
							Thread.sleep(150000);							
						} catch (Exception e) {
							e.printStackTrace();
						}						
					}
				}				
			}).start();
		
			while(true){
				Socket socket = servidor.accept();
				
				//Criando esta Thread faço com que o servidor retorne imediatamente para a escuta da porta para receber novas requisiçoes
				//e a Thread que se encarrega de tratar a requisição deste cliente....
				new Thread(new Runnable() {					
					@Override
					public void run() {						
						try {
							Scanner stream = new Scanner(socket.getInputStream());							
							while(stream.hasNextLine()){	
								String nextRequest = stream.nextLine();
								JSONObject request = new JSONObject(nextRequest);		
								doRequest(socket, request);
							}
							
						} catch (Exception e) {							
							String msg = null;							
							if(e.getMessage() != null){
								msg = e.getMessage();
							} else {
								msg = "ERRO NA REQUISICAO DE " + socket.getInetAddress().getHostAddress();
							}
							Utils.console(msg);
						} finally {
//							stream.close();
						}
					}					
				}).start();//inicia a Thread
			}			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Método de tratamento das requisições
	 * @param args
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	public static void doRequest(Socket socket, JSONObject requestAsJSON) throws JSONException, IOException {		
		Utils.console(socket.getInetAddress().getHostAddress() + "(" +socket.getPort() +") - " + requestAsJSON);		
		String cmd = requestAsJSON.getString("cmd");		
		Request request = new Request(mapConnections, socket);
		String from = requestAsJSON.getString("from");//Obtem o login de quem deseja se conectar..
		try {
			switch (cmd) {
			case "REQUEST_CONNECT": 
				request.newConnection(from);								
				break;
			case "REQUEST_LIST": 	
				request.getAllConnections(from);			
				break;				
			case "REQUEST_SEND": {
				String to = requestAsJSON.getString("to");
				String data = requestAsJSON.getString("data");//obtem a mensagem
				request.sendMessage(from, to, data);
				break;
			}									
			case "REQUEST_DISCONNECT":
				request.disconnect(from);
				break;
			case "REQUEST_PING":
				request.ping(from);
				break;			
			default:
				break;
			}		
		} catch (Exception e) {
			Utils.console(e.getMessage());
		}
	}	
}
