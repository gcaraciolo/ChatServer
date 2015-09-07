package br.com.chat.server;

import java.net.Socket;
import java.util.Date;
import java.util.Map;

import org.json.JSONObject;

public class Request {

	private Map<String, Connection> connections;
	private JSONObject response;
	private Socket socket;
	
	public Request(Map<String, Connection> connections, Socket socket) {
		this.connections = connections;
		this.response = new JSONObject();
		this.socket = socket;
	}
	
	public void newConnection(String from) throws Exception {		
		if (connections.containsKey(from)) {
			response.put("cmd", "RESPONSE_CONNECT");
			response.put("from", "Server");
			response.put("data", "Login já em uso, escolha outro.");			
		} else {			
			synchronized (connections) {//sincroniza o mapa, ja que será feito uma alteração no mapa e é mult-usuario
				connections.put(from, new Connection(socket));
			}			
			response.put("cmd", "RESPONSE_CONNECT");
			response.put("from", "Server");
			response.put("data", "ok");//é configurada uma mensagem de sucesso..
			
			String msg = from + " estar online";
			String command = "REQUEST_USER_JOIN";
			Message.broadcast(command, from, msg, connections);
		}
		Message.enviaMensagem(socket, response.toString());//envia a mensagem de retorno ao cliente...
	}
	
	public void getAllConnections(String from) throws Exception {
		response.put("cmd", "RESPONSE_LIST");
		response.put("from", "Server");//pre configura a mensagem de retorno		
		String data = new String();		
		for (String login : connections.keySet()) {//varre o mapa dos usuarios logados..			
			if(!login.equals(from)){//se o usuario não for o que solicitou a listagem
				data += login + "\n";
			}
		}		
		if(data.isEmpty()){//se a listagem for vazia
			data = "(lista vazia)";
		} else {//caso contrario
			data = data.substring(0, data.length() -1);//formata a resposta...
		}		
		response.put("data", data);//insere os dados na mensagem
		Message.enviaMensagem(socket, response.toString());//envia a mensagem de retorno ao cliente...
	}
	
	public void sendMessage(String from, String to, String data) throws Exception {				
		Connection connection = connections.get(to);//obtem o socket do usuario que recebera a mensagem..
		if(connection != null){//se exitir o usuario conectado		
			JSONObject jsonTo = new JSONObject();//Cria um JSON para formatar a mensagem que será enviado para o destino			
			jsonTo.put("cmd", "FORWARD_SEND");
			jsonTo.put("from", from);			
			jsonTo.put("data", data);			
			Message.enviaMensagem(connection.getSocket(), jsonTo.toString());//envia a mensagem para o destino.. observe que o socket não é o de origem pois trata-se de envio para uma outra maquina
			Utils.console(jsonTo.toString());//exibe no console do servidor apenas para debugg			
		} else {//caso não acho o login de destino			
			response.put("cmd", "RESPONSE_SEND");
			response.put("from", "Server");
			response.put("data", "Não foi possivel enviar a mensagem, verifique o login e tente novamente.");//configura uma mensagem de erro para o socket de origem
		}
		Message.enviaMensagem(socket, response.toString());//envia a mensagem de retorno ao cliente...
	}
	
	public void disconnect(String from) throws Exception {
	
		synchronized (connections) {
			connections.remove(from);//remove qm soliciou a desconexão do mapa dos usuario conectados
		}
		
		response.put("cmd", "RESPONSE_DISCONNECT");
		response.put("from", "Server");
		response.put("data", "bye");//cria uma mensagem de retorno ao usuario informando que o servidor reconheceu a desconexão..
		
		String msg = from + " estar offline";
		String command = "REQUEST_USER_QUIT";
		Message.broadcast(command, from, msg, connections);		
	}
	
	public void ping(String from) throws Exception {		
		Connection connection = connections.get(from);//pega a conexao
		connection.setDthUltimoAcesso(new Date());//atualiza a data de ultimo acesso PING
		
		response.put("cmd", "RESPONSE_PONG");
		response.put("data", "ok");
		response.put("from", "Server");//configura uma mensagem de retorno informando que o servidor reconheceu o PING
		Message.enviaMensagem(socket, response.toString());//envia a mensagem de retorno ao cliente...
	}
	
	
	
}
