package test.net.tcp;

import com.firefly.net.tcp.SimpleTcpServer;
import com.firefly.net.tcp.codec.CharParser;

public class ServerDemo1 {

	public static void main(String[] args) {
		SimpleTcpServer server = new SimpleTcpServer();
		server.accept(connection -> {
			CharParser charParser = new CharParser();
			charParser.complete(message -> {
				String s = message.trim();
				switch (s) {
				case "quit":
					connection.write("bye!\r\n").close();
					break;
				default:
					connection.write("received message [" + s + "]\r\n");
					break;
				}
			});
			connection.receive(charParser::receive);
		}).listen("localhost", 1212);

	}
}
