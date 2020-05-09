import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author CHOI Chong Hing (UID: 3035564940)
 * The Peer Class
 * It contains the information of a Peer,
 * including ID, IP Address and Port
 */
class Peer {
	private final int id;
	private InetAddress ip;
	private int port;
	
	/**
	 * The Peer Constructor
	 * @param id - ID of the Peer (InetAddress Object)
	 * @param ip - IP of the Peer
	 * @param port - Port of the Peer
	 */
	public Peer(int id, InetAddress ip, int port) {
		this.id = id;
		this.ip = ip;
		this.port = port;
	}
	
	/**
	 * The Peer Constructor
	 * @param id - ID of the Peer (Byte Array)
	 * @param ip - IP of the Peer
	 * @param port - Port of the Peer
	 */
	public Peer(int id, byte[] ip, int port) {
		this.id = id;
		try {
			this.ip = InetAddress.getByAddress(ip);
		} catch (UnknownHostException e) {}
		this.port = port;
	}
	
	/**
	 * The getIP() method
	 * @return IP of the Peer in InetAddress Object
	 */
	public InetAddress getIP() {return ip;}
	
	
	/**
	 * The getByteIP() method
	 * @return IP of the Peer in Byte Array
	 */
	public byte[] getByteIP() {return ip.getAddress();}
	
	/**
	 * The getPort() method
	 * @return Port of the Peer
	 */
	public int getPort() {return port;}
	
	/**
	 * The getID() method
	 * @return ID of the Peer
	 */
	public int getID() {return id;}
	
	/**
	 * The setIP() method
	 * @param ip - IP of the Peer in Byte Array
	 */
	public void setIP(byte[] ip) {
		try {
			this.ip = InetAddress.getByAddress(ip);
		} catch (UnknownHostException e) {}
	}
	
	/**
	 * The setIP() method
	 * @param ip - IP of the Peer in InetAddress Object
	 */
	public void setIP(InetAddress ip) {
		this.ip = ip;
	}
	
	/**
	 * The setPort() method
	 * @param port - Port of the Peer
	 */
	public void setPort(int port) {
		this.port = port;
	}
}
