********************************************************************************
|                        An P2P Image Sharing Programme                        |
|                         Created by CHOI CHONG HING                           |
********************************************************************************
This README.txt file contains all the operations of the programme.
For TAs or people already know the programme, you may read the parts start
with (*)

This programme consists 8 files (including User.txt)
*For Server-end, the following files are needed:
	ImageServer.java	(Main programme file)
	Peer.java
	User.java
	User.txt

*For Peer-end, the following files are needed:
	ImagePeer.java		(Main programme file)
	Peer.java
	SubClientHandler.java
	Hash.java
	HashAlgoSHA256.java

1. Connecting to Main Server
When ImageServer.java starts running, the Main Server will start to run
It will be followed by a window allow user to choose an image file
It will then be stored in the Server and ready to send to Peers when they connect
You should be able to see the image on the GUI

If the ImagePeer.java starts running, it will ask for the IP of the Main Server
Then, it will try to connect, and a window will pop-up if fails

If successful, it will further ask for the username and password
You may use one of the following three to login:
	Username:	Password:
	mqpeng		Comp2396B
	cjli		2396BComp
	cbchan		HelloWorld0
After 3 consecutive unsuccessful logins, the account will be locked, even with
correct password, login will be denied.

*This programme is only for demonstration, after the Main Server is re-started, 
all the locked accounts will be unlocked, and the counting will reset to 0
*If you want to speed up the login process, you may go to ImagePeer.java line 330
and modify the two input dialogs in the login() method

A Peer connected to the Main Server will automatically download image from Main
Server and active Peers.
**If two Peer connect to the Main Server at a short period of time, they will
not download from each other, and so it should be okay to have several Peers 
initialise at the same time
**But still, it is possible to encounter data loss under extreme situation:
During a testing, 9 Peers are created at the same time, the last Peer has one
image block of data loss.
**However, most of the data loss will be automatically fixed by the self-checking
mechanism. Upon any data loss detected, a request will be sent to the Main Server
and download data from Main Server

2. Changing Image or Switching Blocks in Main Server
In Main Server, you can change the image by clicking the button at the bottom
Also, you may switch two blocks' position
*The first block that has be chosen will be highlighted with green edge
*If you want to cancel the selection, click on the selected block again

Any change in the Main Server will automatically send to all Peers, and they
will make changes at the same time.


*3. Peer Disconnect
When the window of the Peer is closed, it will automatically inform the Main
Server and do follow up actions.

**In the case of disconnect during the image update, every Peer will scan the
image if it is updated. If no, the whole P2P download will be start over
**So it is okay to disconnect Peer at anytime

*4. Main Server Disconnect
Whenever the Main Server disconnect, a signal will be sent to all Peers, and
a dialog will pop-up informing the users
