package test.codec.http2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;

import com.firefly.client.http2.ClientHTTPHandler;
import com.firefly.client.http2.HTTP2Client;
import com.firefly.client.http2.HTTPClientConnection;
import com.firefly.codec.http2.model.HostPortHttpField;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpScheme;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.concurrent.FuturePromise;
import com.firefly.utils.function.Action6;
import com.firefly.utils.function.Func4;
import com.firefly.utils.function.Func5;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP2ClientTLSDemo2 {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
		final HTTP2Configuration http2Configuration = new HTTP2Configuration();
		http2Configuration.getTcpConfiguration().setTimeout(60 * 1000);
		http2Configuration.setSecureConnectionEnabled(true);
		HTTP2Client client = new HTTP2Client(http2Configuration);

		FuturePromise<HTTPClientConnection> promise = new FuturePromise<>();
		client.connect("127.0.0.1", 6655, promise);

		final HTTPClientConnection httpConnection = promise.get();

		ClientHTTPHandler handler = new ClientHTTPHandler.Adapter() {

			FileChannel fc = FileChannel.open(Paths.get("D:/favicon.ico"), StandardOpenOption.WRITE,
					StandardOpenOption.CREATE);

			@Override
			public boolean content(ByteBuffer item, Request request, Response response, HTTPOutputStream output,
					HTTPConnection connection) {
				try {
					fc.write(item);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return false;
			}

			@Override
			public boolean messageComplete(Request request, Response response, HTTPOutputStream output,
					HTTPConnection connection) {
				log.info("client received frame: {}, {}, {}", response.getStatus(), response.getReason(),
						response.getFields());
				try {
					fc.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return true;
			}
		};

		HttpFields fields = new HttpFields();
		fields.put(HttpHeader.USER_AGENT, "Firefly Client 1.0");

		if (httpConnection.getHttpVersion() == HttpVersion.HTTP_2) {
			httpConnection.send(new MetaData.Request("GET", HttpScheme.HTTP, new HostPortHttpField("127.0.0.1:6677"),
					"/favicon.ico", HttpVersion.HTTP_1_1, fields), handler);

			httpConnection.send(
					new MetaData.Request("GET", HttpScheme.HTTP, new HostPortHttpField("127.0.0.1:6677"), "/index",
							HttpVersion.HTTP_1_1, fields),
					new ClientHTTPHandler.Adapter().messageComplete(new Func4<Request, Response, HTTPOutputStream, HTTPConnection, Boolean>(){

						@Override
						public Boolean call(Request t1, Response resp, HTTPOutputStream t3, HTTPConnection t4) {
							System.out.println("message complete: " + resp.getStatus() + "|" + resp.getReason());
							return true;
						}
						
					}).content(new Func5<ByteBuffer, Request, Response, HTTPOutputStream, HTTPConnection, Boolean>() {

						@Override
						public Boolean call(ByteBuffer buffer, Request t2, Response t3, HTTPOutputStream t4,
								HTTPConnection t5) {
							System.out.println(BufferUtils.toString(buffer, StandardCharsets.UTF_8));
							return false;
						}
						
					}).badMessage(new Action6<Integer, String, Request, Response, HTTPOutputStream, HTTPConnection>() {

						@Override
						public void call(Integer errCode, String reason, Request t3, Response t4, HTTPOutputStream t5,
								HTTPConnection t6) {
							System.out.println("error: " + errCode + "|" + reason);
						}
						
					}));
		}
	}

}
