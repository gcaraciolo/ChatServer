package br.com.chat.server;

import java.net.Socket;
import java.util.Date;
/**
 * Classe para controle de tempo de inatividade do usuario
 * @author thiago
 *
 */
public class Connection {
	
	private Socket socket;
	private Date dthUltimoAcesso;
	
	public Connection(Socket socket, Date dthUltimoAcesso) {
		super();
		this.socket = socket;
		this.dthUltimoAcesso = dthUltimoAcesso;
	}
	
	public Connection(Socket socket) {
		super();
		this.socket = socket;
		this.dthUltimoAcesso = new Date();
	}

	public Socket getSocket() {
		return socket;
	}
	public Date getDthUltimoAcesso() {
		return dthUltimoAcesso;
	}
	public void setDthUltimoAcesso(Date dthUltimoAcesso) {
		this.dthUltimoAcesso = dthUltimoAcesso;
	}
	
}
