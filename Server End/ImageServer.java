import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * @author CHOI Chong Hing (UID: 3035564940)
 * The ImageServer Class
 * It is the server-end application of ImagePeer
 * It sends image to the different peers
 * and image blocks could be switched
 */
@SuppressWarnings("serial")
public class ImageServer extends JFrame implements WindowListener{
		//JButton, JPanel, JFrame
		private JButton loadButton;
		private ImageServerPanel imageBoard;
		
		//Peer-related
		private ArrayList<DataOutputStream> connectedOutputStreams;
		private ArrayList<Integer> connectedIDs;
		private ArrayList<Peer> peerList;
		
		//User data
		private ArrayList<User> userList;
		
		//Counts
		public static int idCount = 1;
		private int connectedCount = 0;
		
		//boolean
		private boolean disconnected;
		
		//image
		private BufferedImage[] imageArray;

		/**
		 * The main() method
		 * It creates an instance of ImageServer
		 * and start the ImageServer running
		 */
		public static void main(String[] args) {
			ImageServer imageServer = new ImageServer();
			imageServer.start();
		}
		
		private void start() {
			//Start Server
			Thread server = new Thread(new Server());
			server.start();
			//Initialise
			initialize();
			//Startup Image Request			
			loadImage();
		}
		
		private void initialize() {
			//image board initialisation
			imageBoard = new ImageServerPanel(this);
			imageBoard.setPreferredSize(new Dimension(700, 700));
			this.add(BorderLayout.CENTER, imageBoard);
							
			//button initialisation
			loadButton = new JButton("Load another image");
			loadButton.addActionListener(new loadNewImageListener());
			this.add(BorderLayout.SOUTH, loadButton);
			
			//Frame initialisation
			this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			this.addWindowListener(this);
			this.setTitle("Image Server");
			this.setResizable(false);
			this.pack();
			this.setVisible(true);
			
			//user data initialisation
			File file = new File("User.txt");
			userList = new ArrayList<>();
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				String line; 
				while ((line = br.readLine()) != null) {
				    String username = line.substring(line.indexOf(':')+1, line.indexOf(';'));
				    line = line.substring(line.indexOf(';')+1);
				    String hashedPassword = line.substring(line.indexOf(':')+1);
				    userList.add(new User(username, hashedPassword));
				}
				br.close();
			} catch (Exception e) {
				System.out.println(e);
			}			
		}
		
		private synchronized void loadImage() {
			JFrame loadFrame = new JFrame();
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Open image...");
			String[] filterArr = new String[] {
					"jpg", "jpeg", "jpe", "jif", "jfif", "jfi", "png", "webp", "tiff", "tif", "bmp", "dib", "heif", "heic", "jp2", "j2k", "jpf", "jpm", "jpx", "mj2", "svg", "svgz",
					"JPG", "JPEG", "JPE", "JIF", "JFIF", "JFI", "PNG", "WEBP", "TIFF", "TIF", "BMP", "DIB", "HEIF", "HEIC", "JP2", "J2K", "JPF", "JPM", "JPX", "MJ2", "SVG", "SVGZ"};
			fileChooser.setFileFilter(new FileNameExtensionFilter("Image file", filterArr));
			int userSelection = fileChooser.showSaveDialog(loadFrame);
			
			if (userSelection == JFileChooser.APPROVE_OPTION) {
				File imageFile = fileChooser.getSelectedFile();
				if (!imageBoard.newImage(imageFile)) {
					JFrame frame = new JFrame();
					JOptionPane.showMessageDialog(frame, new JLabel("Cannot Load Image.",JLabel.CENTER), "Error", JOptionPane.WARNING_MESSAGE);
				}
			}
			
			updateImage();
		}
		
		private void updateImage() {
			//update all clients
			connectedCount = 0;
			for (DataOutputStream dos : connectedOutputStreams) {
				try {
					dos.writeInt(3);	//Request Update PeerList
					dos.flush();
				}catch (Exception e) {}
			}
		}
		
		private void connected() {
			if (++connectedCount == connectedOutputStreams.size()) {
				int distributeIndex = connectedOutputStreams.size();
				for (int i = 0; i < 100; i+= distributeIndex) {
					for (int count = 0; count < connectedOutputStreams.size(); count++) {
						if (i+count > 100) break;
						sendImage(connectedOutputStreams.get(count), i +count, true);
					}
				}
			}
		}
		
		//Exchange two blocks
		private void exchangeImageBlocks(int index1, int index2) {
			BufferedImage temp = imageArray[index2];
			imageArray[index2] = imageArray[index1];
			imageArray[index1] = temp;
			for (DataOutputStream dos : connectedOutputStreams) {
				sendImage(dos, index1, false);
				sendImage(dos, index2, false);
			}
		}
		
		//sendImage functions
		//send index-th block to client
		//data format: 1, index, length, dataInByteArray
		private void sendImage(DataOutputStream dos, int indexStart, int indexEnd, boolean share) {
			try {
				for (int i = indexStart; i < indexEnd; i++) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					try {
						ImageIO.write(imageArray[i], "png", baos);
						baos.flush();
					} catch (Exception e) {}
					byte[] imageInByte = baos.toByteArray();
					dos.writeInt(1);
					dos.writeBoolean(share);
					dos.writeInt(i);
					dos.writeInt(imageInByte.length);
					dos.write(imageInByte);
					dos.flush();
					//System.out.println("Sending Image Block " + index + " to Client");
					baos.close();
				}
			}catch (Exception e) {}
			try {
				dos.writeInt(6);
				dos.writeInt(indexStart);
				dos.writeInt(indexEnd);
				dos.flush();
			}catch (Exception E) {}
		}
		private void sendImage(DataOutputStream dos, int index, boolean share) {
			sendImage(dos, index, index+1, share);
		}
		
		//sendPeer functions
		//send all peer in peerList to client
		private void sendPeerList(DataOutputStream dos, int id) {
			try {
				System.out.println("Sending Signal 5");
				dos.writeInt(5);
				dos.flush();
			}catch (Exception e) {}
			for (int i = 0; i < peerList.size(); i++) {
				System.out.println(peerList.get(i).getID());
				sendPeer(dos, id, i);
			}
			try {
				System.out.println("Sending Signal " + 4);
				dos.writeInt(4);
				dos.flush();
			}catch (Exception e) {}
		}
		//send index-th peer to client
		//data format: 2, id, length, port, ip
		private void sendPeer(DataOutputStream dos, int id, int index) {
			Peer peer = peerList.get(index);
			//same peer
			System.out.println("Sending " + peer.getID() + " to " + id);
			if (peer.getID() == id) return;
			try {
				dos.writeInt(2);
				dos.writeInt(peer.getID());
				byte[] ip = peer.getByteIP();
				dos.writeInt(ip.length);
				dos.writeInt(peer.getPort());
				dos.write(ip);
				dos.flush();
			} catch (IOException e) {
				System.out.println("sendPeer()");
				System.out.println(e);
			}
		}
		
		//Client
		private class ClientHandler implements Runnable {
			private Socket socket;
			private DataInputStream dis;
			private DataOutputStream dos;
			private int id;
			
			
			private ClientHandler(Socket socket) {
				this.socket = socket;
				this.id = idCount++;
				try {
					dis = new DataInputStream(socket.getInputStream());
					dos = new DataOutputStream(socket.getOutputStream());
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
						case 0:	//request login
							loginCheck();
							break;
						case 1: //request image block
							int start = dis.readInt();
							int end = dis.readInt();
							sendImage(dos, start, end, false);
							break;
						case 2:	//request peer list
							System.out.println("Request of peerList received");
							sendPeerList(dos, id);
							break;
						case 3:	//receive peer info and update
							System.out.println("Peer Info received");
							receivePeerInfo();
							break;
						case 4:	//receive connected Signal
							connected();
							break;
						case 5:	//disconnect
							disconnect();
							disconnected = true;
							break;
						case 6:	//Not full update with other peer disconnecting
							if (disconnected) {
								updateImage();
								disconnected = false;
							}
							break;
						default:
							break;
						}
					} catch (Exception E) {}
				}
			}
			
			private void loginCheck() {
				String username;
				String hashedPassword;
				String userInput;
				try {
					userInput = dis.readUTF();
					username = userInput.substring(0, userInput.indexOf(':'));
					hashedPassword = userInput.substring(userInput.indexOf(':')+1);
					System.out.println(username + ":" + hashedPassword);
					boolean match = false;
					for (int i = 0; i < userList.size(); i++) {
						User user = userList.get(i);
						if (username.equals(user.getUsername())) {
							if (user.getHashedPassword().equals(hashedPassword)) {
								match = !user.isLocked();
								if (match) user.successfulLogin();
							}else {
								match = false;
								user.failed();
							}
							break;
						}
					}
					dos.writeInt(0);
					dos.writeBoolean(match);
					dos.flush();
					if (match) {
						connectedOutputStreams.add(dos);
						connectedIDs.add(id);
					}
				} catch (Exception E) {}
			}
			
			private void disconnect() {
				for (int i = 0; i < connectedIDs.size(); i++) {
					if (connectedIDs.get(i) == id) {
						connectedIDs.remove(i);
						connectedOutputStreams.remove(i);
					}
				}
				for (int i = 0; i < peerList.size(); i++) {
					if (peerList.get(i).getID() == id) {
						peerList.remove(i);
					}
				}
				for (DataOutputStream dos : connectedOutputStreams) {
					try {
						dos.writeInt(7);
						dos.flush();
					} catch (IOException e) {
					}
				}

			}
			
			//Receive peer info
			//data format: 3, port
			private void receivePeerInfo() {
				try {
					int port = dis.readInt();
					System.out.println("Received Peer Server at Port " + port);
					peerList.add(new Peer(id, this.socket.getInetAddress(), port));
				} catch (IOException e) {
				}
			}
		
			
		}
		
		//Server
		private class Server implements Runnable {
			/* (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			@SuppressWarnings("resource")
			@Override
			public void run() {
				idCount = 1;
				connectedOutputStreams = new ArrayList<>();
				connectedIDs = new ArrayList<>();
				peerList = new ArrayList<>();
				try {
					ServerSocket ss = new ServerSocket(9000);
					while (true) {
						Socket s = ss.accept();
						Thread thread = new Thread(new ClientHandler(s));
						thread.start();
					}
				} catch (Exception e) {	
				}
			}
		}
		
		//Listener
		private class loadNewImageListener implements ActionListener{

			/* (non-Javadoc)
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(ActionEvent arg0) {
				loadImage();			
			}
			
		}
		
		//JPanel
		private class ImageServerPanel extends JPanel implements MouseListener{
			private ImageServer server;
			private int selectedIndex;
			
			
			private ImageServerPanel(ImageServer server) {
				this.server = server;
				this.addMouseListener(this);
				imageArray = new BufferedImage[100];
				selectedIndex = -1;
			}
			
			/* (non-Javadoc)
			 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
			 */
			public void paintComponent(Graphics g) {
				if (imageArray[0] == null) return;
				for (int i = 0; i < 100; i++) {
					g.drawImage(imageArray[i], (i%10) * 70, ((int)(i/10)%10) * 70, null);
					if (i == selectedIndex) {
						g.setColor(Color.GREEN);
						g.drawRect((i%10) * 70, ((int)(i/10)%10) * 70, 69, 69);
					}
				}
			}
			
			private boolean newImage(File imageFile) {
				try {
					imageArray = chop(ImageIO.read(imageFile));
				}catch(Exception e){
					return false;
				}
				server.repaint();
				return true;
			}
			
			
			private BufferedImage[] chop(BufferedImage image) {
				Image tmp = image.getScaledInstance(700, 700, Image.SCALE_SMOOTH);
				image = new BufferedImage(700, 700, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2d = image.createGraphics();
				g2d.drawImage(tmp, 0, 0, null);
				g2d.dispose();
				
				BufferedImage[] imgArray = new BufferedImage[100];
				for (int i = 0; i < 100; i++) {
					imgArray[i] = image.getSubimage((i%10) * 70, ((int)(i/10)%10) * 70, 70, 70);
				}
				return imgArray;
			}

			/* (non-Javadoc)
			 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
			 */
			@Override
			public void mouseClicked(MouseEvent arg0) {				
			}

			/* (non-Javadoc)
			 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
			 */
			@Override
			public void mouseEntered(MouseEvent arg0) {				
			}

			/* (non-Javadoc)
			 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
			 */
			@Override
			public void mouseExited(MouseEvent arg0) {				
			}

			/* (non-Javadoc)
			 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
			 */
			@Override
			public void mousePressed(MouseEvent e) {
				if (imageArray[0] == null) return;
				int index = (int)(e.getY()/70)*10 + (int)(e.getX()/70);
				System.out.println("Selected Index: " + index);
				if (selectedIndex == -1) {
					selectedIndex = index;
				}else if (selectedIndex == index){
					return;
				}else {
					exchangeImageBlocks(selectedIndex, index);
					selectedIndex = -1;
				}
				server.repaint();
			}

			/* (non-Javadoc)
			 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
			 */
			@Override
			public void mouseReleased(MouseEvent arg0) {				
			}

		}
		
		private void exit() {
			for (DataOutputStream dos : connectedOutputStreams) {
				try {
					dos.writeInt(8);
					dos.flush();
				}catch (Exception e) {}
			}
			System.exit(0);
		}

		@Override
		public void windowActivated(WindowEvent arg0) {
		}

		@Override
		public void windowClosed(WindowEvent arg0) {
			exit();
		}

		@Override
		public void windowClosing(WindowEvent arg0) {			
		}

		@Override
		public void windowDeactivated(WindowEvent arg0) {			
		}

		@Override
		public void windowDeiconified(WindowEvent arg0) {			
		}

		@Override
		public void windowIconified(WindowEvent arg0) {
		}

		@Override
		public void windowOpened(WindowEvent arg0) {			
		}

}
