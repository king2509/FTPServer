
import com.king.service.MD5;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class Test {

    public static void main(String[] args) throws IOException, DocumentException {
        ServerSocket serverSocket = new ServerSocket(3000);
        Socket socket = serverSocket.accept();
        String str = "hello";
        OutputStream out = socket.getOutputStream();
        out.write(str.getBytes());
        out.flush();
        out.close();
    }
}

