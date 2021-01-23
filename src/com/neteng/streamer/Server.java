package com.neteng.streamer;


import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public class Server extends JFrame implements ActionListener {


	static String project_base_path = "/media/hossein/hossein/projects/net_eng/phas3/neteng_streamer";

	static String filename = project_base_path + "/src/com/neteng/streamer/videos/movie.mjpeg";
	static String output_file_path = project_base_path + "/src/com/neteng/streamer/output";

	static int frames_size = 0;
	int imagenb = 0;


	// RTP variables:
	DatagramSocket RTPsocket;
	DatagramPacket senddp;

	InetAddress ClientIPAddr;
	int RTP_dest_port = 0;


	JLabel label;

	static int MJPEG_TYPE = 26;
	static int FRAME_PERIOD = 50;

	Timer timer;

	// rtsp states
	final static int INIT = 0;
	final static int READY = 1;
	final static int PLAYING = 2;

	// rtsp message types
	final static int SETUP = 3;
	final static int PLAY = 4;
	final static int PAUSE = 5;
	final static int STOP = 6;


	static int state;
	Socket RTSPsocket;

	// input and output stream filters
	static BufferedReader RTSPBufferedReader;
	static BufferedWriter RTSPBufferedWriter;

	static int RTSP_ID = 123456; // ID of the RTSP session
	int RTSPSeqNb = 0; // Sequence number of RTSP messages within the session
	final static String CRLF = "\r\n";


	public Server() {

		super("Server");


		timer = new Timer(FRAME_PERIOD, this);
		timer.setInitialDelay(0);
		timer.setCoalesce(true);



		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				// stop the timer and exit
				timer.stop();
				System.exit(0);
			}
		});

		// GUI:
		label = new JLabel("Send frame #        ", JLabel.CENTER);
		getContentPane().add(label, BorderLayout.CENTER);
	}


	public static void main(String argv[]) throws Exception {
		Server server = new Server();

		int port = 2000;
		server.pack();
		server.setVisible(true);


		try {
			ServerSocket listenSocket = new ServerSocket(port);
			server.RTSPsocket = listenSocket.accept();
			listenSocket.close();
			server.ClientIPAddr = server.RTSPsocket.getInetAddress();

			state = INIT;


			RTSPBufferedReader = new BufferedReader(new InputStreamReader(
					server.RTSPsocket.getInputStream()));
			RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(
					server.RTSPsocket.getOutputStream()));


			int request_type;
			boolean done = false;
			while (!done) {
				request_type = server.parse_RTSP_request();

				if (request_type == SETUP) {
					done = true;

					state = READY;
					System.out.println("RTSP state = READY");

					server.send_RTSP_response();

					frames_size = extract_video(filename);
					System.out.println(frames_size);

					server.RTPsocket = new DatagramSocket();
				}
			}


			while (true) {
				request_type = server.parse_RTSP_request();

				if ((request_type == PLAY) && (state == READY)) {
					server.send_RTSP_response();
					server.timer.start();
					state = PLAYING;
					System.out.println("RTSP state = PLAYING");
				} else if ((request_type == PAUSE) && (state == PLAYING)) {

					server.send_RTSP_response();
					server.timer.stop();
					state = READY;
					System.out.println("RTSP state = READY");

				} else if (request_type == STOP) {
					server.send_RTSP_response();
					server.timer.stop();
					server.RTSPsocket.close();
					server.RTPsocket.close();
					System.exit(0);
				}
			}
		} catch (BindException e) {
			System.out.println("server could not be started on port " + port);
			System.exit(0);
		}

	}


	private static int extract_video(String input_file)
	{
		String program = "frame_extractor.py";
		String result = "";

		try {
			String cmd = "rm -rf " + output_file_path;
			Runtime run = Runtime.getRuntime();
			Process pr = run.exec(cmd);
			pr.waitFor();


			ProcessBuilder pb = new ProcessBuilder("python3", program, input_file, output_file_path);
			Process p = pb.start();

			p.waitFor();
			BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			while ((line = buf.readLine()) != null) {
				result += line;
			}

			System.out.println("result: " + result);
			return Integer.valueOf(result);
		}
		catch (Exception e)
		{
			return 0;
		}
	}


	public void actionPerformed(ActionEvent e) {

		if (imagenb<frames_size) {

			try {
				BufferedImage bImage = ImageIO.read(new File(output_file_path + "/img_" + imagenb + ".jpg"));
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ImageIO.write(bImage, "jpg", bos);
				byte[] buf = bos.toByteArray();


				RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb * FRAME_PERIOD, buf, buf.length);


				int packet_length = rtp_packet.getlength();


				byte[] packet_bits = new byte[packet_length];
				rtp_packet.getpacket(packet_bits);


				senddp = new DatagramPacket(packet_bits, packet_length,
						ClientIPAddr, RTP_dest_port);
				RTPsocket.send(senddp);


				label.setText("Send frame #" + imagenb);

				imagenb++;

			} catch (Exception ex) {
				System.out.println("exception: " + ex);
				System.exit(0);
			}
		}
		else {
			timer.stop();
		}
	}


	private int parse_RTSP_request() {
		int request_type = -1;
		try {
			String RequestLine = RTSPBufferedReader.readLine();
			System.out.println(RequestLine);

			StringTokenizer tokens = new StringTokenizer(RequestLine);
			String request_type_string = tokens.nextToken();

			if ((new String(request_type_string)).compareTo("SETUP") == 0)
				request_type = SETUP;
			else if ((new String(request_type_string)).compareTo("PLAY") == 0)
				request_type = PLAY;
			else if ((new String(request_type_string)).compareTo("PAUSE") == 0)
				request_type = PAUSE;
			else if ((new String(request_type_string)).compareTo("STOP") == 0)
				request_type = STOP;




			String SeqNumLine = RTSPBufferedReader.readLine();
			System.out.println(SeqNumLine);
			tokens = new StringTokenizer(SeqNumLine);
			tokens.nextToken();
			RTSPSeqNb = Integer.parseInt(tokens.nextToken());


			String LastLine = RTSPBufferedReader.readLine();
			System.out.println(LastLine);

			if (request_type == SETUP) {
				imagenb = 0;
				tokens = new StringTokenizer(LastLine);
				for (int i = 0; i < 3; i++)
					tokens.nextToken();
				RTP_dest_port = Integer.parseInt(tokens.nextToken());
			}

		} catch (Exception ex) {
			System.out.println("Exception caught: " + ex);
			System.exit(0);
		}
		return (request_type);
	}


	private void send_RTSP_response() {
		try {
			RTSPBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
			RTSPBufferedWriter.write("CSeq: " + RTSPSeqNb + CRLF);
			RTSPBufferedWriter.write("Session: " + RTSP_ID + CRLF);
			RTSPBufferedWriter.flush();
		} catch (Exception ex) {
			System.out.println("Exception caught: " + ex);
			System.exit(0);
		}
	}
}
