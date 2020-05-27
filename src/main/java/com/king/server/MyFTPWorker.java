package com.king.server;
import com.king.service.MD5XmlParser;
import com.king.service.MySQLDemo;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;


/**
 * Class For a FTP server
 * One thread for one socket connection
 * @author Zejin Huang
 */



class MyFTPWorker extends Thread{
    private final boolean DEBUG = true;

    // default username & password
    private String USERNAME = null;
    private String PASSWORD = null;

    private final String remoteHost = "127,0,0,1";

    // bufferSize: set for stream buffer
    private final int bufferSize = 4096;

    /* Control Socket Settings
     * writer: used for transfer control commands
     * quitCMDLoop: flag to quit commands loop
     */
    private Socket controlSocket;
    private BufferedWriter writer = null;
    private BufferedReader reader = null;
    private boolean quitCMDLoop = false;

    /**
     * userStatus
     * 1. Entered username, but not entered password yet
     * 2. Logged in
     */
    private enum userStatus {ENTEREDUSER, LOGGEDIN};
    private userStatus currentStatus;

    // file dir
    private String rootDir;
    private String currentDir;

    // FTP dataSocket
    private Socket dataSocket;

    // required by rename method(rnfm, rnto)
    private String oldFilename;

    // File transferring protocol
    private enum TYPE {ASCII, BIN};
    private TYPE type = null;

    // reset point
    private long point;

    MySQLDemo mySQLDemo = null;


    // Worker initiation
    MyFTPWorker(Socket socket){
        this.controlSocket = socket;
        rootDir = "F:/root";
        currentDir = rootDir;
        mySQLDemo = new MySQLDemo();
    }

    // Thread main function, run cmd loop
    public void run() {
        System.out.println("Thread starts to run!");
        try {
            writer = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));

            System.out.println("FTP server opens!");
            sendMSGToClient("220 FTP server opens!");

            if(quitCMDLoop) {
                System.out.println("TRUE");
            } else {
                System.out.println("FALSE");
            }
            while(!quitCMDLoop) {
                executeCMD(reader.readLine());
            }
        } catch (IOException e){
            e.getStackTrace();
        } finally {
            try {
                writer.close();
                reader.close();
                controlSocket.close();
                debugOutput("Socket closed and worked stopped");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     *
     * @param request containing commands & extra information
     */
    private void executeCMD(String request) {
        if(DEBUG)
            System.out.println(request);
        int index = request.indexOf(" ");
        String cmd = "";
        String args = "";
        if(index != -1) {
            cmd = request.substring(0, index);
            args = request.substring(index + 1);
        } else {
            cmd = request;
        }
        try {
            switch (cmd) {
                case "USER":
                    handleUSER(args);
                    break;

                case "PASS":
                    handlePASS(args);
                    break;

                case "PWD":
                    handlePWD();
                    break;

                case "MKD":
                    handleMKD(args);
                    break;

                case "DELE":
                    handleDELE(args);
                    break;

                case "CWD":
                    handleCWD(args);
                    break;

                case "RMD":
                    handleRMD(args);
                    break;

                case "CDUP":
                    handleCDUP();
                    break;

                case "RETR":
                    handleRETR(args);
                    break;
                case "RETRP":
                    try{
                        handleRETRP(args);
                    } catch (IOException e) {
                        System.out.println("IO exception!!!!!!!!!!!");
                        e.getStackTrace();
                    }
                    break;
                case "QUIT": {
                    handleQUIT();
                    break;
                }

                case "TYPE":
                    handleTYPE(args);
                    break;

                case "SIZE":
                    handleSize(args);
                    break;

                case "PASV":
                    handlePASV();
                    break;

                case "MPASV":
                    handleMPASV(args);
                    break;

                case "RNFR":
                    handleRNFR(args);
                    break;
                case "RNTO":
                    handleRNTO(args);
                    break;

                case "LIST":
                    handleLIST();
                    break;

                case "NLST":
                    handleNLST();
                    break;

                case "STOR":
                    handleSTOR(args);
                    break;

                case "REST":
                    handleREST(args);
                    break;

                default:{
                    sendMSGToClient("550 wait for fulfilling");
                }
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.getStackTrace();
        }
    }

    private void handleRETRP(String fileDetails) throws IOException {
        System.out.println(fileDetails);
        String[] details  = fileDetails.split(" ");
        String filename = details[0];
        long start = Long.parseLong(details[1]);
        long length = Long.parseLong(details[2]);
        sendMSGToClient("125 Data transfer starts.");
        OutputStream outputStream = dataSocket.getOutputStream();
        BufferedOutputStream out = new BufferedOutputStream(outputStream);
        FileInputStream inputStream = new FileInputStream(new File(filename));
        inputStream.skip(start-1);
        BufferedInputStream input = new BufferedInputStream(inputStream);
        byte[] buffer = new byte[bufferSize];
        long sentBytes = 0;
        long curLen = 0;
        long unsentBytes = length;
        while ((curLen = input.read(buffer)) > 0 && unsentBytes >= bufferSize) {
            out.write(buffer);
            sentBytes += curLen;
            unsentBytes = length - sentBytes;
            System.out.print(String.format("Transferred: %d / %d\r", sentBytes, length));
        }
        System.out.println(unsentBytes);
        int cur = input.read(buffer, 0, (int)unsentBytes);
        if(cur > 0) {
            out.write(buffer, 0, (int) unsentBytes);
            sentBytes += cur;
            System.out.println(String.format("Transferred: %d / %d\r", sentBytes, length));
        }
        out.flush();
        out.close();
        input.close();
        dataSocket.close();
        sendMSGToClient("226 Transfer complete.");
    }

    private void handleUSER(String username) throws IOException{
        USERNAME = username;
        boolean hasUsername = mySQLDemo.hasUsername(username);
        if(currentStatus == userStatus.LOGGEDIN) {
            sendMSGToClient("530 user has logged in");
        } else if(hasUsername) {
            currentStatus = userStatus.ENTEREDUSER;
            // 密码错误
            sendMSGToClient("331 PASS required");
        } else {
            sendMSGToClient("530 wrong username");
        }
    }

    private void handlePASS(String pass) throws IOException{
        PASSWORD = pass;
        boolean isValidPass = mySQLDemo.isValidPassword(USERNAME, PASSWORD);
        if(currentStatus == userStatus.LOGGEDIN) {
            sendMSGToClient("530 user has logged in");
        } else if(currentStatus == userStatus.ENTEREDUSER) {
            if(isValidPass) {
                currentStatus = userStatus.LOGGEDIN;
                //用户名错误
                sendMSGToClient("230 Logged in successfully");
            }
            else
                sendMSGToClient("530 Wrong password");
        } else {
            sendMSGToClient("230 Logged in successfully");
        }
    }

    // Return current working directory
    private void handlePWD() throws IOException {
        // Check whether user logs in
        if(isLoggedIn())
            sendMSGToClient(String.format("%d \"%s\" is current directory",257,currentDir));
    }

    /**
     *
     * @param dir change directory to "dir", make sure dir specified exists first.
     * @throws IOException Exception may occur with sendMSGToClient
     */
    private void handleCWD(String dir) throws IOException {
        if(isLoggedIn()) {
            // Check whether dir exists
            ArrayList<String> dirnames = getCurDirnames();
            if (!dirnames.contains(dir)) {
                sendMSGToClient("550 No such directory");
                return;
            } else {
                currentDir = currentDir + "/" + dir;
            }

            sendMSGToClient("250 current directory: " + currentDir);
        }
    }

    /**
     *
     * @param newDir A new directory made in server
     * @throws IOException Exception with sendMSGTOClient
     */
    private void handleMKD(String newDir) throws IOException {
        if(isLoggedIn()) {
            File file = new File(currentDir + "/" + newDir);
            // Check if file exists
            if(!file.exists()) {
                boolean isMKDSuccess = file.mkdir();
                if(isMKDSuccess)
                    sendMSGToClient(String.format("257 \"%s\" created.",newDir));
                else
                    sendMSGToClient(String.format("450 Failed to create \"%S\"", newDir));
            } else {
                sendMSGToClient("550 Cannot create a file when that file already exists.");
            }
        }
    }

    // Change into upper directory
    private void handleCDUP() throws IOException{
        //No upper directory as root directory
        if(currentDir.equals(rootDir)) {
            sendMSGToClient("250 Already in root directory.");
            return;
        }

        File file = new File(currentDir);
        currentDir = file.getParent();
        sendMSGToClient("250 Current directory is " + currentDir);
    }

    /**
     * 删除指定文件，而非目录
     * @param filename 被删除文件名
     * @throws IOException 网络传输异常时报错
     */
    private void handleDELE(String filename) throws IOException{
        // Check whether file exists
        ArrayList<String> filenames = getCurFilenames();
        if(!filenames.contains(filename))
            sendMSGToClient("550 No such file");
        else {
            File file = new File(currentDir + "/" + filename);
            if(file.delete())
                sendMSGToClient(String.format("%d Delete \"%s\" successfully", 250, filename));
            else {
                sendMSGToClient("550 Failed to delete " + filename);
            }
        }
    }

    /**
     *
     * @param dirName directory to be deleted
     * @throws IOException
     */
    private void handleRMD(String dirName) throws IOException{
        ArrayList<String> dirNames = getCurDirnames();
        System.out.println(dirNames);
        if(!dirNames.contains(dirName)) {
            sendMSGToClient("550 No such file or directory.");
        } else {
            File file = new File(currentDir + "/" + dirName);

            // In case that dirName specified is a file, not a directory.
            if(!file.isDirectory()) {
                sendMSGToClient("550 " + dirName +" is a file, not a directory.");
                return;
            }
            boolean isDelete = file.delete();
            if(isDelete)
                sendMSGToClient("250 Removed target directory successfully.");
            else
                sendMSGToClient("550 Failed to remove target directory.");
        }
    }

    // Quit command loop by setting quitCMDLoop as "true"
    private void handleQUIT() throws IOException{
        if(isLoggedIn()) {
            sendMSGToClient("221 The connection closed");
            quitCMDLoop = true;
        }
    }

    /**
     *
     * @param filename target file requesting its size
     * @throws IOException sendMSGToClient
     */
    private void handleSize(String filename) throws IOException{
        if(isLoggedIn()) {
            File file = new File(String.join("/", currentDir, filename));
            if(!file.exists()) {
                sendMSGToClient("450 No such file.");
            } else if(file.isDirectory()) {
                sendMSGToClient("450 " + filename + "is a directory which has no size property.");
            } else {
                long size = file.length();
                sendMSGToClient("213 "+size);
            }
        }
    }

    private void handleMPASV(String fileInfos) throws IOException, InterruptedException, ExecutionException {
        if(isLoggedIn()) {
            String[] infos = fileInfos.split(" ");
            String rawFilename = infos[0];

            int threadNum = Integer.parseInt(infos[1]);
            StringBuilder ports = new StringBuilder();
            ServerSocket[] serverSockets = new ServerSocket[threadNum];

            for(int i=0; i<threadNum; i++) {
                serverSockets[i] = new ServerSocket(0);
                ports.append(serverSockets[i].getLocalPort());
                ports.append(",");
            }

            System.out.println("Ports created: " + ports.toString());
            sendMSGToClient(ports.toString());

            Socket[] sockets = new Socket[threadNum];
            Thread[] threads = new Thread[threadNum];

            for(int i=0; i<threadNum; i++) {
                sockets[i] = serverSockets[i].accept();
                threads[i] = new Thread(new SendFileTask(sockets[i]));
                threads[i].start();
            }

            for(int i=0; i<threadNum; i++)
                threads[i].join();

            System.out.println("All sockets have established & finalized successfully");

            MD5XmlParser md5XmlParser = new MD5XmlParser(currentDir + "/" + rawFilename);

            sendMSGToClient(md5XmlParser.readMD5());
        }
    }



    class SendFileTask implements Runnable{
        private Socket socket;

        SendFileTask(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            BufferedReader reader = null;
            BufferedOutputStream socketOutput = null;
            BufferedInputStream dataInput = null;
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String[] infos = reader.readLine().split(" ");
                String rawFilename = infos[0];
                long startIndex = Long.parseLong(infos[1]);
                long len = Long.parseLong(infos[2]);


                // 数据输出流：从服务器中读出文件的部分数据传至客户端
                socketOutput = new BufferedOutputStream(socket.getOutputStream());

                File file = new File(currentDir+"/" + rawFilename);

                String threadName = Thread.currentThread().getName();
                if(file.exists()) {
                    FileInputStream fileInputStream = new FileInputStream(file);
                    fileInputStream.skip(startIndex);
                    dataInput = new BufferedInputStream(fileInputStream);
                    byte[] buffer = new byte[bufferSize];

                    long sentBytes = 0;
                    long unsentBytes = len;
                    int readCnt = -1;

                    // 可以被读满
                    while((readCnt=dataInput.read(buffer)) > 0 && unsentBytes >= bufferSize) {
                        socketOutput.write(buffer, 0, readCnt);
                        unsentBytes -= readCnt;
                        sentBytes += readCnt;

                        System.out.print(String.format("%s Transferred: %d / %d\r", threadName,sentBytes, len));
                    }
                    // 发送最后剩余的部分unsentBytes
                    socketOutput.write(buffer, 0, (int) unsentBytes);
                    //unsentBytes -= readCnt;
                    sentBytes += readCnt;
                    System.out.print(String.format("%s Transferred: %d / %d\r", threadName,sentBytes, len));
                    socketOutput.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if(reader != null) reader.close();
                    if(socketOutput != null) socketOutput.close();
                    if(dataInput != null) dataInput.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    // Entering PASV Mode
    private void handlePASV() throws IOException{
        if(isLoggedIn()) {
            // serverSocket allocate port automatically with initial port as 0
            // Port will be -1 when without any initial value
            ServerSocket serverSocket = new ServerSocket(0);
            // Divide port number to port1 & port2
            int port1 = serverSocket.getLocalPort() / 256;
            int port2 = serverSocket.getLocalPort() % 256;
            System.out.println("PASV port: " + serverSocket.getLocalPort());
            sendMSGToClient(String.format("227 Entering Passive Mode (" + remoteHost + ",%d,%d)", port1, port2));
            dataSocket = serverSocket.accept();
            System.out.println("成功建立连接！！！！！！！！！！！！！！！！！！！！！！");
        }
    }

    /**
     *处理客户端上传文件的请求
     * @param filename 客户端上传的文件
     * @throws IOException
     */
    private void handleSTOR(String filename) throws IOException{
        if(isLoggedIn()) {
            if (dataSocket == null)
                sendMSGToClient("450 PASV command needed first.");
            return;
        }

        File file = new File(currentDir + "/" + filename);

        // establish data transferring link
        BufferedInputStream in = new BufferedInputStream(dataSocket.getInputStream());
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        sendMSGToClient("125 Data connection already open; Transfer starting.");

        byte[] buffer = new byte[bufferSize];
        while (in.read(buffer) != -1) {
            out.write(buffer);
        }
        out.flush();
        out.close();
        in.close();
        sendMSGToClient("226 Transfer complete.");
        dataSocket.close();
    }

    private void handleRNFR(String oldFilename) throws IOException {
        File file = new File(String.join("/",currentDir, oldFilename));
        if(!file.exists())
            sendMSGToClient("550 No such file or directory.");
        else {
            sendMSGToClient("350 Requested file action pending further information");
            this.oldFilename = oldFilename;
        }
    }

    // Waiting to deal with file existing problem
    private void handleRNTO(String newFilename) throws IOException{
        File file = new File(String.join("/",currentDir,oldFilename));
        File newFile = new File(newFilename);
        if(newFile.exists()) {
            sendMSGToClient("550 target filename exists.");
            return;
        }
        boolean flag = file.renameTo(new File(newFilename));
        if(flag)
            sendMSGToClient("250 RNTO COMMAND successful.");
        else
            sendMSGToClient("550 Failed to accomplish RNTO command");
    }


    // 获得系统默认的换行符
    private final String separator = System.getProperty("line.separator");

    /**
     *
     * @throws IOException
     */
    private void handleLIST() throws IOException {
        if(dataSocket == null) {
            sendMSGToClient("550 PASV Mode needed first.");
            return;
        }

        File[] files = new File(currentDir).listFiles();
        if(files != null) {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(dataSocket.getOutputStream(), StandardCharsets.UTF_8);
            BufferedWriter writer = new BufferedWriter(outputStreamWriter);
            sendMSGToClient("125 Data connection already open; Transfer starting.");
            String datetimePattern = "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat datetimeFormat = new SimpleDateFormat(datetimePattern);
            for (File file : files) {
                String filename = file.getName();

                Date fileDate = new Date(file.lastModified());
                String lastModifiedTime = datetimeFormat.format(fileDate);

                // 判断文件类型
                String fileType = file.isFile() ? "FILE" : "DIR";

                // 目录时，设置目录大小为0
                String fileSize = "" + (file.isFile() ? file.length() : 0);

                String line = String.join("\t", filename, lastModifiedTime, fileType, fileSize);

                System.out.println(line);

                writer.write(line + separator);
            }
            writer.flush();
            writer.close();
            sendMSGToClient("226 Transfer complete.");
            System.out.println("226 Transfer complete.");
            dataSocket.close();
        } else {
            sendMSGToClient("550 Empty Directory.");
        }
    }

    /**
     *获得当前目录下所有文件、目录的名字
     * @param
     * @throws IOException
     */
    private void handleNLST() throws IOException {
        if(dataSocket == null) {
            sendMSGToClient("550 PASV Mode needed first.");
            return;
        }
        System.out.println("执行NLST");
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(dataSocket.getOutputStream(), StandardCharsets.UTF_8);
        BufferedWriter writer = new BufferedWriter(outputStreamWriter);
        sendMSGToClient("125 Data connection already open; Transfer starting.");
        for (String filename : getAllFilenames()) {
            writer.write(filename + separator);
        }
        writer.flush();
        writer.close();
        sendMSGToClient("226 Transfer complete.");
        System.out.println("226 Transfer complete.");
        dataSocket.close();
    }

    /**
     *
     * @param type Transferring Type. ASCII: text, BIN: binary file except text
     * @throws IOException
     */
    private void handleTYPE(String type) throws IOException{
        if(type.equals("A"))
            this.type = TYPE.ASCII;
        else if(type.equals("I"))
            this.type = TYPE.BIN;
        else
            sendMSGToClient("550 Unknown transfer type. Only Type: A/I available.");
        sendMSGToClient("200 Change mode into TYPE " + type);
        System.out.println("Type: " + this.type);
    }

    private void handleREST(String point) throws IOException{
        this.point = Long.parseLong(point);
        sendMSGToClient("350 Restarting at "+point+". Send STOR or RETR to initiate transfer.");
    }

    private void handleRETR(String filename) throws IOException{
        ArrayList<String> filenames = getCurFilenames();
        if(!filenames.contains(filename)) {
            sendMSGToClient("550-The system cannot find the file specified.");
            return;
        }
        if(type == TYPE.BIN) {
            sendMSGToClient("125 Data transfer starts.");

            OutputStream outputStream = dataSocket.getOutputStream();
            BufferedOutputStream out = new BufferedOutputStream(outputStream);

            FileInputStream inputStream = new FileInputStream(new File(currentDir + "/" + filename));

            System.out.println("point!: "+this.point);
            inputStream.skip(this.point);

            BufferedInputStream input = new BufferedInputStream(inputStream);

            byte[] buffer = new byte[bufferSize];
            int len = -1;
            while ((len = input.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            out.flush();
            out.close();
            input.close();
            dataSocket.close();
            sendMSGToClient("226 Transfer complete.");
        }
        if(type == TYPE.ASCII) {
            sendMSGToClient("125 Data transfer starts.");

            FileInputStream inputStream = new FileInputStream(new File(currentDir + "/" +filename));
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "gbk");

            // 写入文本流设定为gbk编码
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(dataSocket.getOutputStream(), "gbk");
            BufferedWriter out = new BufferedWriter(outputStreamWriter);

            BufferedReader input = new BufferedReader(inputStreamReader);

            String line = null;
            while((line = input.readLine()) != null) {
                //System.out.println(line);
                out.write(line + separator);
            }
            out.flush();
            out.close();
            input.close();
            dataSocket.close();
            sendMSGToClient("226 Transfer complete.");
        }
        MD5XmlParser md5XmlParser = new MD5XmlParser(currentDir + "/" + filename);
        String md5 = md5XmlParser.readMD5();
        System.out.println("md5: " + md5);
        // 发送文件的md5值
        sendMSGToClient(md5);
    }



    void sendMSGToClient(String msg) throws IOException{
        if(DEBUG)
            System.out.println(msg);
        writer.write(msg + separator);
        writer.flush();
    }

    private boolean isLoggedIn() throws IOException{
        if(currentStatus != userStatus.LOGGEDIN) {
            sendMSGToClient("530 You have not logged in!");
            return false;
        }
        return true;
    }

    private ArrayList<String> getAllFilenames() {
        File[] files = new File(currentDir).listFiles();
        ArrayList<String> filenames = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                filenames.add(f.getName());
            }
        }
        return filenames;
    }

    private ArrayList<String> getCurDirnames() {
        File[] files = new File(currentDir).listFiles();
        ArrayList<String> dirNames = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                if(f.isDirectory())
                    dirNames.add(f.getName());
            }
        }
        return dirNames;
    }

    private ArrayList<String> getCurFilenames() {
        File[] files = new File(currentDir).listFiles();
        ArrayList<String> fileNames = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                if(f.isFile())
                    fileNames.add(f.getName());
            }
        }
        return fileNames;
    }

    private void debugOutput(String str) {
        System.out.println(str);
    }

}

