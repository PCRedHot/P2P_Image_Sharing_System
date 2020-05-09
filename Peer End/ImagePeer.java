import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * @author CHOI Chong Hing (UID: 3035564940)
 * The ImagePeer Class
 * It is the peer-end application of ImageServer
 * It receives image from the server and other peers
 */
@SuppressWarnings("serial")
public class ImagePeer extends JFrame implements WindowListener{
		private Socket socket;
		private BufferedImage[] imageArray;
		private ImagePanel imageBoard;
		private ArrayList<Peer> peerList;
		private ArrayList<SubClient> subClientList;
		private ArrayList<DataOutputStream> subServerDOS;
		private DataOutputStream mainServerDOS;
		private int port = -1;
		private boolean init, login;
		private boolean[] updated;
		
		/**
		 * The main() method
		 * It creates an instance of ImagePeer and
		 * starts the ImagePeer running 
		 */
		public static void main(String[] args) {
			ImagePeer imagePeer = new ImagePeer();
			imagePeer.start();
	
		}
		
		private void start() {
			peerList = new ArrayList<>();
			subServerDOS = new ArrayList<>();
			init = true;
			login = false;
			connectToServer();
			MainServerHandler msh = new MainServerHandler(socket, this);
			Thread handlerThread = new Thread(msh);
			handlerThread.start();
		}
		
		private void connectToServer() {
			String serverIP = JOptionPane.showInputDialog(null, "Connect to server:", "localhost");
			if (serverIP == null) System.exit(0);
			try {
				socket = new Socket(serverIP, 9000);
				System.out.println("Conneceted to Main Server");
				mainServerDOS = new DataOutputStream(socket.getOutputStream());
			}catch(Exception e) {
				JFrame frame = new JFrame();
				JOptionPane.showMessageDialog(frame, new JLabel("Cannot connect to the Main Server",JLabel.CENTER), "Error", JOptionPane.INFORMATION_MESSAGE);
				System.exit(0);
			}
		}
		
		private void boardInit() {
			imageBoard = new ImagePanel();
			imageBoard.setPreferredSize(new Dimension(700, 700));
			this.add(BorderLayout.CENTER, imageBoard);
			this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			this.addWindowListener(this);
			this.setTitle("Image Client");
			this.setResizable(false);
			this.pack();
			this.setVisible(true);
		}
	
		/**
		 * The getImageBlock() method
		 * @param index - The index of the required image block
		 * @return the image block with the input index
		 */
		BufferedImage getImageBlock(int index) {return imageArray[index];}
		
		/**
		 * The getUpdate() method
		 * @param index - The index of the requited image block
		 * @return the status of the block, if it is updated
		 */
		boolean getUpdated(int index) {return updated[index];}
		
		private void updatedSet(boolean set) {
			for (int i = 0; i < updated.length; i++) {
				updated[i] = set;
			}
		}
		
		//request a block of image
		//data format: 1, index
		private void requestImageBlock(DataOutputStream dos, int indexStart, int indexEnd, boolean main) {
			try {
				dos.writeInt(1);
				dos.writeInt(indexStart);
				dos.writeInt(indexEnd);
				dos.flush();
				if (main) {
					System.out.println("Requesting " + indexStart + " to " + indexEnd + " from Main Server");
				}else {
					System.out.println("Requesting " + indexStart + " to " + indexEnd + " from Peer");
				}
				
			}catch (Exception e) {}
		}
			
		
		//receive one image block
		private synchronized void receiveImage(DataInputStream dis, boolean share) {
			try {
				int index = dis.readInt();
				int length = dis.readInt();
				byte[] data = new byte[length];
				for (int i = 0; i < length; i++) {
					data[i] = dis.readByte();
				}
			    imageArray[index] = byteArrayToBufferedImage(data);
			    updated[index] = true;
			   	this.repaint();
			   	
			   	if (share) {
			   		for (SubClient subClient : subClientList) {
			   			DataOutputStream clientDOS = subClient.getDOS();
			   			try {
			   				clientDOS.writeInt(1);
				   			clientDOS.writeInt(index);
				   			clientDOS.writeInt(length);
				   			clientDOS.write(data);
				   			clientDOS.flush();
			   			}catch (Exception E) {
			   				System.out.println("Failed to send " + index + " to SubClient " + subClient.getID());
			   			}
			   		}
			   	}
			} catch (Exception e) {}
		}
		
		//send peer info to server
		//data format: 3, port
		private void sendPeer() {
			try {
				System.out.println("Sending Peer Info, Port: " + port);
				mainServerDOS.writeInt(3);
				mainServerDOS.writeInt(port);
				mainServerDOS.flush();
			}catch (Exception e) {}
		}
		
			
		private BufferedImage byteArrayToBufferedImage(byte[] data) {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			try {
				return ImageIO.read(bais);
			} catch (IOException e) {
				System.out.println(e);
				return byteArrayToBufferedImage(data);
			}
		}
		
		
		private void connectToPeerSubServer() {
			System.out.println("Start Connecting to Peers...");
			subServerDOS.clear();
			for (Peer peer : peerList) {
				SubServerHandler ssh = null;
				try {
					System.out.println("Try connecting " + peer.getIP() + " : " + peer.getPort());
					Socket socket = new Socket(peer.getIP(), peer.getPort());
					DataOutputStream newDOS = new DataOutputStream(socket.getOutputStream());
					subServerDOS.add(newDOS);
					ssh = new SubServerHandler(socket);
					Thread sshThread = new Thread(ssh);
					sshThread.start();
				}catch (Exception e) {
					System.out.println("Connection Failed");
				}
	
			}
			
			
			if (init) {
				init = false;
				int distributeIndex =  100 / (subServerDOS.size()+1);
				int count = 0;
				for (int i = 0; i < 100; i += distributeIndex) {
					if (i == 0) {
						requestImageBlock(mainServerDOS, i, distributeIndex, true);
					}else {
						if (!((i+distributeIndex) > 100) && (count+1) != subServerDOS.size()) {
							requestImageBlock(subServerDOS.get(count++), i, i+distributeIndex, false);
						}else {
							requestImageBlock(subServerDOS.get(count++), i, 100, false);
						}
						
					}
				}
				return;
			}
			sendStandardSignal(mainServerDOS, 4);
			
		}
		
		private void checkExist(int start, int end) {
			for (int i = start; i < end; i++) {
				if (!updated[i]) {
					requestImageBlock(mainServerDOS, i, end, true);
					break;
				}
			}
		}
		
		private void requestPeerList() {
			sendStandardSignal(mainServerDOS, 2);
			updatedSet(false);
		}
		
		/**
		 * The disconnect() method
		 * It disconnects the SubClient from the SubServer by ID 
		 * @param id - ID of the SubClient
		 */
		void disconnect(int id) {
			for (int i = 0; i < subClientList.size(); i++) {
				if (subClientList.get(i).getID() == id) {
					subClientList.remove(i);
				}
			}
		}
		
		//Main Server Handler
		private class MainServerHandler implements Runnable {
			private ImagePeer imagePeer;
			private DataInputStream dis;
			private DataOutputStream dos;
			
			private MainServerHandler(Socket socket, ImagePeer imagePeer) {
				this.imagePeer = imagePeer;
				try {
					dis = new DataInputStream(socket.getInputStream());
					dos = new DataOutputStream(socket.getOutputStream());
					mainServerDOS = dos;
				}catch (Exception e) {
					System.out.println(e);
				}
				System.out.println("Client - Main Created");
				login();
			}
			
			/* (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			@Override
			public void run() {
				while (true) {
					try {
						int dataInputType = dis.readInt();
						switch(dataInputType) {
						case 0:	//login results
							receiveLoginResults();
							break;
						case 1:	//receive one image block
							receiveImage(dis, dis.readBoolean());
							break;
						case 2:	//receive one Peer
							System.out.println("Get Peer Info Signal");
							receivePeer();
							break;
						case 3:	//request peerList
							//System.out.println("Get Signal 3");
							requestPeerList();
							break;
						case 4:	//Start New Image P2P
							//System.out.println("Get Signal 4");
							connectToPeerSubServer();
							break;
						case 5:	//Clear PeerList
							//System.out.println("peerList Cleared");
							peerList.clear();
							break;
						case 6:	//Done Sending Signal
							int doneStart = dis.readInt();
							int doneEnd = dis.readInt();
							checkExist(doneStart, doneEnd);
							break;
						case 7:	//Other Peer disconnected
								//Check if all image blocks updated
							checkUpdated();
							break;
						case 8:	//Main Server closes
							JFrame frame = new JFrame();
							JOptionPane.showMessageDialog(frame, new JLabel("Main Server is closed",JLabel.CENTER), "Disconnect from Main Server", JOptionPane.INFORMATION_MESSAGE);
							break;
						default:
							break;
						}
					} catch (Exception E) {}
				}
			}
			
			private void checkUpdated() {
				for (boolean bool : updated) {
					if (!bool) {
						try {
							dos.writeInt(6);
						}catch(Exception E) {
							
						}
						break;
					}
				}
			}
			
			private void login() {
				try {
					HashAlgoSHA1 hashAlgo = new HashAlgoSHA1();
					String username = JOptionPane.showInputDialog(null, "Username", "cjli");
					if (username == null) exit();
					
					String password = hashAlgo.hash(JOptionPane.showInputDialog(null, "Password", "2396BComp"));
					if (password == null) exit();
					
					password = hashAlgo.hash(password);
					dos.writeInt(0);	//Data Type: Login Data
					String data = username + ':' + password;
					System.out.println(data);
					dos.writeUTF(data);
					dos.flush();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			private void receiveLoginResults() {
				if (login) return;
				try {
					if (dis.readBoolean()) {
						login = true;
						Thread subServerThread = new Thread(new SubServer(imagePeer));
						subServerThread.start();
						boardInit();
						requestPeerList();
					}else {
						JFrame frame = new JFrame();
						JOptionPane.showMessageDialog(frame, new JLabel("Login Fail",JLabel.CENTER), "Error", JOptionPane.INFORMATION_MESSAGE);
						login();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			//receive one peer
			private void receivePeer() {
				try {
					int id = dis.readInt();
					int length = dis.readInt();
					byte[] ip = new byte[length];
					int port = dis.readInt();
					for (int i = 0; i < length; i++) {
						ip[i] = dis.readByte();
					}
					Peer newPeer = new Peer(id, ip, port);
					peerList.add(newPeer);
					System.out.println("Received Info of Peer " + id);
				} catch (IOException e) {}
			}
			
		}
	
		//Client - subServer
		private class SubServerHandler implements Runnable {
			private DataInputStream dis;
			
			private SubServerHandler(Socket socket) {
				try {
					dis = new DataInputStream(socket.getInputStream());
				}catch (Exception e) {}
			}
			
			/* (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			@Override
			public void run() {
				while (true) {
					try {
						int dataInputType = dis.readInt();
						switch(dataInputType) {
						case 1:	//receive one image block
							receiveImage(dis, false);
							break;
						case 2:	//Failed, try again from main server
							int index = dis.readInt();
							requestImageBlock(mainServerDOS, index, index+1, true);
							break;
						case 6: //Done sending signal
							int doneStart = dis.readInt();
							int doneEnd = dis.readInt();
							checkExist(doneStart, doneEnd);
							break;
						default:
							break;
						}
					} catch (Exception E) {}
				}
			}
		}
	

		private void addSubClient(Socket socket, int id) {
			subClientList.add(new SubClient(socket, id));
		}
			
		private class SubServer implements Runnable {
			
			private ImagePeer imagePeer;
			private int idCount;
			
			private SubServer(ImagePeer imagePeer){
				this.imagePeer = imagePeer;
				this.idCount = 1;
			}
	
			/* (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			@Override
			public void run() {
				subClientList = new ArrayList<>();
				try {
					ServerSocket ss;
					ss = setServer(3000+ImageServer.idCount);
					port = ss.getLocalPort();
					System.out.println("Started Sub Server at port: " + port);
					sendPeer();
					while (true) {
						Socket s = ss.accept();
						SubClientHandler sch = new SubClientHandler(s, idCount, imagePeer);
						addSubClient(s, idCount++);
						Thread thread = new Thread(sch);
						thread.start();
					}
				} catch (Exception e) {	
				}
			}
			
			private ServerSocket setServer(int p) {
				try{
					return new ServerSocket(p);
				}catch (Exception e) {
					return setServer(p+1);
				}
			}
			
		}
		
		
		private void sendStandardSignal(DataOutputStream dos, int signal) {
			try {
				System.out.println("Sending Signal " + signal);
				dos.writeInt(signal);
				dos.flush();
			}catch (Exception e) {}
		}
		
		
		private class SubClient {
			private DataOutputStream dos;
			private final int id;
			
			private SubClient(Socket socket, int id) {
				try {
					this.dos = new DataOutputStream(socket.getOutputStream());
				}catch (Exception E) {}
				this.id = id;
			}
			
			private DataOutputStream getDOS() {
				return dos;
			}
			
			private int getID() {
				return id;
			}
		}
	
		private class ImagePanel extends JPanel{
			
			private ImagePanel() {
				imageArray = new BufferedImage[100];
				updated = new boolean[100];
				updatedSet(false);
			}
			
			/* (non-Javadoc)
			 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
			 */
			public void paintComponent(Graphics g) {
				if (imageArray[0] == null) return;
				for (int i = 0; i < 100; i++) {
					g.drawImage(imageArray[i], (i%10) * 70, ((int)(i/10)%10) * 70, null);
				}
			}
		}
		
		private void exit() {
			try {
				System.out.println("Closed");
				for (DataOutputStream dos : subServerDOS) {
					try {
						dos.writeInt(2);
						dos.flush();
					}catch(Exception e) {}
				}
				mainServerDOS.writeInt(5);
				mainServerDOS.flush();
			} catch (IOException e1) {
			}
			System.exit(0);
		}
	
	
		/* (non-Javadoc)
		 * @see java.awt.event.WindowListener#windowActivated(java.awt.event.WindowEvent)
		 */
		@Override
		public void windowActivated(WindowEvent arg0) {
		}
	
		/* (non-Javadoc)
		 * @see java.awt.event.WindowListener#windowClosed(java.awt.event.WindowEvent)
		 */
		@Override
		public void windowClosed(WindowEvent e) {
			exit();
		}
				
		/* (non-Javadoc)
		 * @see java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
		 */
		@Override
		public void windowClosing(WindowEvent e) {
		}
	
		/* (non-Javadoc)
		 * @see java.awt.event.WindowListener#windowDeactivated(java.awt.event.WindowEvent)
		 */
		@Override
		public void windowDeactivated(WindowEvent e) {
		}
	
		/* (non-Javadoc)
		 * @see java.awt.event.WindowListener#windowDeiconified(java.awt.event.WindowEvent)
		 */
		@Override
		public void windowDeiconified(WindowEvent e) {
		}
	
		/* (non-Javadoc)
		 * @see java.awt.event.WindowListener#windowIconified(java.awt.event.WindowEvent)
		 */
		@Override
		public void windowIconified(WindowEvent e) {
		}
	
		/* (non-Javadoc)
		 * @see java.awt.event.WindowListener#windowOpened(java.awt.event.WindowEvent)
		 */
		@Override
		public void windowOpened(WindowEvent e) {		
		}
}
	