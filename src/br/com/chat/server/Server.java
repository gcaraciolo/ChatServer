package br.com.chat.server;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
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
			servidor = new ServerSocket(12345);//Solicita ao SO a porta que irá escutar
			
			console("Iniciando servidor chat");
			//Esta thread é iniciada para ficar verificando o tempo de inatividade dos usuarios
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					
					while(true){
						
						try {
							
							List<String> listRemove = new ArrayList<>();
							
							for (String login : mapConnections.keySet()) {
								
								Connection connection = mapConnections.get(login);
								
								long millis = new Date().getTime() - connection.getDthUltimoAcesso().getTime();
										
								if((millis / 1000l) > 15l){//Caso o usuario passe 15 segundos sem enviar o PING o servidor trata de desconectar o socket
									listRemove.add(login);//adiciona o usuario na lista dos usuarios que seram removidos
									connection.getSocket().close();//fecha o socket
								}
										
							}
							
							synchronized (mapConnections) {//bloco sincronizado por se tratar de um mapa estatico
								for (String login : listRemove) {
									mapConnections.remove(login);
								}
							}
							
							for (String login : listRemove) {//envia uma mensagem de broadcast para os outros usuários informando que tal usuario ficou offline
								broadcast("REQUEST_USER_QUIT", login, login + " esta offline.");
							}
							
							Thread.sleep(15000);//15 segundos..//Dome por 15 segundos..
							
						} catch (Exception e) {
							e.printStackTrace();
						}
						
					}
				}
				
			}).start();//Inicia a Thread
			
			//Loop infinito
			while(true){
				
				Socket socket = servidor.accept();//Assim que o servidor recebe uma solicitação de algum cliente... este metodo retorna um SOCKET
				//Criando esta Thread faço com que o servidor retorne imediatamente para a escuta da porta para receber novas requisiçoes
				//e a Thread que se encarrega de tratar a requisição deste cliente....
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						
						try {
							//Crio um scanner para ler a mensagem que o usuario enviou para o servidor...
							Scanner stream = new Scanner(socket.getInputStream());
							
							while(stream.hasNextLine()){//O processo ficara parado neste ponto ate que tenha uma mensagem para ser lida
								
								String request = stream.nextLine();//ler a mensagem e salva na variavel
								//Exibe no console do servidor apenas para debugg
								console(socket.getInetAddress().getHostAddress() + "(" +socket.getPort() +") - " + request);
								//Trata a requisição e recebe uma resposta
								String response = doRequest(request);
								
								if(response != null && !response.isEmpty()){//se a resposta for relevante..
									console(response);//exibe no console do servidor apenas apara debugg
									enviaMensagem(socket, response);//envia a mensagem de retorno ao cliente...
								}
							}
							
						} catch (Exception e) {
							
							String msg = null;
							
							if(e.getMessage() != null){
								msg = e.getMessage();
							} else {
								msg = "ERRO NA REQUISICAO DE " + socket.getInetAddress().getHostAddress();
							}
							console(msg);
						}
						
					}
					/**
					 * Método de tratamento das requisições
					 * @param args
					 * @return
					 * @throws JSONException
					 * @throws IOException
					 */
					private String doRequest(String args) throws JSONException, IOException {
						//Cria um JSON dando um parser na mensagem enviada pelo cliente...
						JSONObject request = new JSONObject(args);
						//Prepara um JSON para formatar a resposta..
						JSONObject response = new JSONObject();
						//Obtem o HEAD da requisição
						String cmd = request.getString("cmd");						
						
						if(cmd.equals("REQUEST_CONNECT")){//Se for uma requisição de conexão..
							
							String from = request.getString("from");//Obtem o login de quem deseja se conectar..
							
							if(mapConnections.containsKey(from)){//se no mapa ja exitir este login
								response.put("cmd", "RESPONSE_CONNECT");
								response.put("from", "Server");
								response.put("data", "Login já em uso, escolha outro.");//é configurada uma mensagem de erro
								
							}  else {//caso contrario
								
								synchronized (mapConnections) {//sincroniza o mapa, ja que será feito uma alteração no mapa e é mult-usuario
									mapConnections.put(from, new Connection(socket));//insere no mapa a conexao para o determinado login
								}
								
								response.put("cmd", "RESPONSE_CONNECT");
								response.put("from", "Server");
								response.put("data", "ok");//é configurada uma mensagem de sucesso..
								
								broadcast("REQUEST_USER_JOIN", from, from + " esta online.");//envia para todo os logados informando que o novo usuario acabou de ficar ON
							}
							
						} else if(cmd.equals("REQUEST_LIST")){//Se for uma requisicao de SABER A LISTAGEM DOS USUARIO ONLINE
							
							response.put("cmd", "RESPONSE_LIST");
							response.put("from", "Server");//pre configura a mensagem de retorno
							
							String from = request.getString("from");
							
							String data = new String();
							
							for (String login : mapConnections.keySet()) {//varre o mapa dos usuarios logados..
								
								if(!login.equals(from)){//se o usuario não for o que solicitou a listagem
									data += login + "\n";//insere na listagem...
								}
							}
							
							if(data.isEmpty()){//se a listagem for vazia
								data = "(lista vazia)";
							} else {//caso contrario
								data = data.substring(0, data.length() -1);//formata a resposta...
							}
							
							response.put("data", data);//insere os dados na mensagem
							
						} else if(cmd.equals("REQUEST_SEND")){//se for uma requisicao de ENVIO DE MENSAGEM PARA ALGUM USUARIO
							
							String to = request.getString("to");//descobre pra quem deve-se enviar a mensagem
							
							Connection connection = mapConnections.get(to);//obtem o socket do usuario que recebera a mensagem..
							
							String data = request.getString("data");//obtem a mensagem 
							
							if(connection != null){//se exitir o usuario conectado
								
								String from = request.getString("from");//configura o campo de ORIGEM da mensagem
								
								JSONObject jsonTo = new JSONObject();//Cria um JSON para formatar a mensagem que será enviado para o destino
								
								jsonTo.put("cmd", "FORWARD_SEND");
								jsonTo.put("from", from);
								
								jsonTo.put("data", data);
								
								enviaMensagem(connection.getSocket(), jsonTo.toString());//envia a mensagem para o destino.. observe que o socket não é o de origem pois trata-se de envio para uma outra maquina
								
								console(jsonTo.toString());//exibe no console do servidor apenas para debugg
								
							} else {//caso não acho o login de destino
								
								response.put("cmd", "RESPONSE_SEND");
								response.put("from", "Server");
								response.put("data", "Não foi possivel enviar a mensagem, verifique o login e tente novamente.");//configura uma mensagem de erro para o socket de origem
								
							}
							
						} else if(cmd.equals("REQUEST_DISCONNECT")){//se for uma soliciação de desconexão
							
							String from = request.getString("from");//obtem quem solicitou
							
							synchronized (mapConnections) {//sincroniza o mapa já que será uma alteração.
								mapConnections.remove(from);//remove qm soliciou a desconexão do mapa dos usuario conectados
							}
							
							response.put("cmd", "RESPONSE_DISCONNECT");
							response.put("from", "Server");
							response.put("data", "bye");//cria uma mensagem de retorno ao usuario informando que o servidor reconheceu a desconexão..
							
							broadcast("REQUEST_USER_QUIT", from, from + " esta offline.");//anuncia para todos os outros que tal usuario foi desconectado
							
						} else if(cmd.equals("REQUEST_PING")){//se for uma solicitação de ping
							
							String from = request.getString("from");//obtem qm solicitou
							
							Connection connection = mapConnections.get(from);//pega a conexao
							connection.setDthUltimoAcesso(new Date());//atualiza a data de ultimo acesso PING
							
							response.put("cmd", "RESPONSE_PONG");
							response.put("data", "ok");
							response.put("from", "Server");//configura uma mensagem de retorno informando que o servidor reconheceu o PING 
							
						}
						//Caso o JSON de retorno esteja vazio é retornado null, caso contario é gerado a STRING no padrao JSON
						return response.length() == 0 ? null : response.toString();
					}
					
				}).start();//inicia a Thread
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	/**
	 * Método auxiliar utilizado para enviar uma mensagem para todos os usuarios conectados
	 * @param cmd
	 * @param notLogin
	 * @param menssage
	 * @throws JSONException
	 * @throws IOException
	 */
	private static void broadcast(String cmd, String notLogin, String menssage) throws JSONException, IOException {
		
		JSONObject requestTo = new JSONObject();
		
		for (String login : mapConnections.keySet()) {
			
			if(!login.equals(notLogin)){
				Connection connection = mapConnections.get(login);
				
				requestTo.put("cmd", cmd);
				requestTo.put("from", "Server");
				requestTo.put("data", menssage);
				
				enviaMensagem(connection.getSocket(), requestTo.toString());
			}
			
		}
	}
	//Métodos auxiliares..
	private static void enviaMensagem(Socket socket, String menssage) throws IOException {
		PrintStream ps = new PrintStream(socket.getOutputStream(), true);
		ps.println(menssage);
	}
	
	private static void console(String str){
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:mm");
		
		System.out.println("[" + sdf.format(new Date())+ "]: " + str);
	}
	
}
