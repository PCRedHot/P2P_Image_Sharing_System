import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import javax.imageio.ImageIO;


/**
 * @author CHOI Chong Hing (UID: 3035564940)
 * The SubClientHandler Class
 * It handles the operations between the SubServer and SubClient
 */
class SubClientHandler implements Runnable {
	private DataInputStream dis;
	private DataOutputStream dos;
	private ImagePeer imagePeer;
	private final int id;
	
	/**
	 * The SubClientHandler Constructor
	 * @param socket - socket connected to the SubClient
	 * @param id - ID of the SubClient
	 * @param imagePeer - ImagePeer Object where this instance belongs
	 */
	SubClientHandler(Socket socket, int id, ImagePeer imagePeer) {
		this.imagePeer = imagePeer;
		this.id = id;
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
				case 1: //request image block
					int start = dis.readInt();
					int end = dis.readInt();
					System.out.println("Receive Request from " + start + " to " + end);
					sendImage(start, end);
					break;
				case 2:	//disconnect
					imagePeer.disconnect(id);
					break;
				default:
					break;
				}
			} catch (Exception E) {}
		}
	}
	
	
	//send index-th block to client
	//data format: 1, index, length, dataInByteArray
	private void sendImage(int indexStart, int indexEnd) {
		try {
			for (int i = indexStart; i < indexEnd; i++) {
				if (!imagePeer.getUpdated(i)) {
					break;
				}
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try {
					ImageIO.write(imagePeer.getImageBlock(i), "png", baos);
					baos.flush();
				} catch (Exception e) {}
				byte[] imageInByte = baos.toByteArray();
				dos.writeInt(1);
				dos.writeInt(i);
				dos.writeInt(imageInByte.length);
				dos.write(imageInByte);
				dos.flush();
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
}