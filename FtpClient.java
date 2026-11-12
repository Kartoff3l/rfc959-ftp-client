import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;


public class FtpClient {
    
    private Socket controlSocket;
    private BufferedReader reader;
    private PrintWriter writer;

    //the control connection
    public void connect(String server, int port) throws Exception {
        this.controlSocket = new Socket(server, port);
        this.reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
        this.writer = new PrintWriter(controlSocket.getOutputStream(), true);
        System.out.println("Connection established to " + server + "!");
        readResponse();
    }
    //helper method to read server responses, including multiline responses
    public String readResponse() throws Exception {
        String response = reader.readLine();
        System.out.println(response);

        //multiline response (from RFC 959)
        if (response != null && response.length() >= 4 && response.charAt(3) == '-') {
            String statusCode = response.substring(0, 3);
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                response = line; //check for the last line
                if (line.startsWith(statusCode + " ")) {
                    break; 
                }
            }
        }
        return response; //return the final line to parse like PASV
    }

    //anonymous login
    public void loginAnonymous() throws Exception{
        writer.println("USER anonymous");
        readResponse();

        writer.println("PASS pass@example.com");
        readResponse();
    }

    //custom login
    public void loginCustom(String username, String password) throws Exception{
        writer.println("USER " + username);
        readResponse();

        writer.println("PASS " + password);
        readResponse();
    }

    public Socket enterPassiveMode() throws Exception{
        writer.println("PASV");
        String reply = readResponse();
        
        int start = reply.indexOf('(');
        int end = reply.indexOf(')');
        String dataToken = reply.substring(start + 1, end);

        String[] parts = dataToken.split(",");

        String dataIp = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
        int p1 = Integer.parseInt(parts[4]);
        int p2 = Integer.parseInt(parts[5]);
        int dataPort = (p1 * 256) + p2;

        return new Socket(dataIp, dataPort);
    }
    
    public void downloadFile(String filename) throws Exception{
        Socket dataSocket = enterPassiveMode();
        writer.println("RETR " + filename);

        //fix the problem accidentally type the wrong filename
        String reply = readResponse();
        if (reply != null && reply.startsWith("550")){
            dataSocket.close();
            throw new Exception("Download rejected: " + reply);
        }
        InputStream in = null;
        FileOutputStream fileOut = null;

        try{
            in = dataSocket.getInputStream();
            fileOut = new FileOutputStream("downloaded_" + filename);
            byte[] buffer = new byte[4069];
            int bytesRead;

            while((bytesRead = in.read(buffer)) != -1){
                fileOut.write(buffer, 0, bytesRead);
            }
        } catch(Exception e){
            System.out.println("Download failed: " + e.getMessage());
            throw e;
        } finally{
            if(fileOut != null){
                try{ fileOut.close();} catch(Exception e){}
            }
            if (in != null){
                try{ in.close();}catch ( Exception e){}
            }
            if (dataSocket != null){
                try{dataSocket.close();} catch(Exception e){}
            }
        }
        readResponse();
    }

    public void uploadFile(String filename) throws Exception{
        Socket dataSocket = enterPassiveMode();
        writer.println("STOR " + filename);
        String reply = readResponse();
        if (reply != null && reply.startsWith("550")){
            dataSocket.close();
            throw new Exception("Upload rejected: " + reply);
        }

        OutputStream out = null;
        FileInputStream fileIn = null;
        try{
            out = dataSocket.getOutputStream();
            fileIn = new FileInputStream(filename);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while((bytesRead = fileIn.read(buffer)) != -1){
                out.write(buffer, 0, bytesRead);
            }
        } catch(Exception e){
            System.out.println("Upload failed: " + e.getMessage());
            throw e;
        } finally{
            if (fileIn != null){
                try{ fileIn.close();} catch(Exception e){}
            }
            if (out != null){
                try{ out.close();} catch(Exception e){}
            }
            if (dataSocket != null){
                try{ dataSocket.close();} catch(Exception e){}
            }
        }
        readResponse();
    }
    //helper method
    public void sendCommand(String command) throws Exception{
        writer.println(command);
        readResponse();
    }

    public void printWorkingDirectory() throws Exception{
        sendCommand("PWD");
    }

    public void changeDirectory(String path) throws Exception{
        sendCommand("CWD " + path);
    }

    public void makeDirectory(String path) throws Exception{
        sendCommand("MKD " + path);
    }

    public void removeDirectory(String path) throws Exception{
        sendCommand("RMD " + path);
    }

    public void deleteFile(String filename) throws Exception{
        sendCommand("DELE " + filename);
    }

    public void disconnect() throws Exception{
        sendCommand("QUIT");
    }

    public void listDirectory() throws Exception{
        Socket dataSocket = enterPassiveMode();
        sendCommand("LIST");

        BufferedReader dataReader = null;
        
        try{
            dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
            String line;
            while((line = dataReader.readLine()) != null){
                System.out.println(line);
            }
        } catch(Exception e){
            System.out.println("Failed to list directory: " + e.getMessage());
            throw e;
        } finally{
            if (dataReader != null){
                try{ dataReader.close();} catch(Exception e){}
            }
            if (dataSocket != null){
                try{ dataSocket.close();} catch(Exception e){}
            }
        }
        readResponse();
    }
    public static void main(String[] args) {
        FtpClient client = new FtpClient();
        Scanner scanner = new Scanner(System.in);
        boolean isRunning = true;

        System.out.println("=== JAVA FTP CLI STARTED ===");
        System.out.println("Available commands: connect <ip> <port>, login, ls, cd <dir>, get <file>, put <file>, quit");

        while (isRunning) {
            System.out.print("\nftp> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            // Split the input into the command and the argument (e.g., "get" and "README")
            String[] parts = input.split(" ", 2);
            String command = parts[0].toLowerCase();
            String argument = parts.length > 1 ? parts[1] : "";

            try {
                switch (command) {
                    case "connect":
                        if (argument.isEmpty()) {
                            System.out.println("Usage: connect <server> <port>");
                            break;
                        }
                        String[] serverArgs = argument.split(" ");
                        String ip = serverArgs[0];
                        int port = (serverArgs.length > 1) ? Integer.parseInt(serverArgs[1]) : 21;
                        
                        client.connect(ip, port);
                        break;

                    case "login":
                        if (argument.isEmpty() || argument.equalsIgnoreCase("anonymous")) {
                            System.out.println("Logging in as anonymous...");
                            client.loginAnonymous();
                        } else {
                            // Split the argument into username and password
                            String[] loginArgs = argument.split(" ");
                            
                            if (loginArgs.length == 2) {
                                client.loginCustom(loginArgs[0], loginArgs[1]);
                            } else {
                                System.out.println("Usage: login <username> <password>");
                                System.out.println("   Or: login anonymous");
                            }
                        }
                        break;

                    case "ls":
                        client.listDirectory();
                        break;

                    case "mkd":
                        if (argument.isEmpty()) System.out.println("Usage: mkdir <directory_name>");
                        else client.makeDirectory(argument);
                        break;

                    case "rmd":
                        if (argument.isEmpty()) System.out.println("Usage: rmdir <directory_name>");
                        else client.removeDirectory(argument);
                        break;

                    case "dele":
                        if (argument.isEmpty()) System.out.println("Usage: delete <filename>");
                        else client.deleteFile(argument);
                        break;

                    case "cd":
                        if (argument.isEmpty()) System.out.println("Usage: cd <directory>");
                        else client.changeDirectory(argument);
                        break;

                    case "get":
                        if (argument.isEmpty()) System.out.println("Usage: get <filename>");
                        else client.downloadFile(argument);
                        break;

                    case "put":
                        if (argument.isEmpty()) System.out.println("Usage: put <filename>");
                        else client.uploadFile(argument);
                        break;
                        
                    case "pwd":
                        client.printWorkingDirectory();
                        break;

                    case "quit":
                        client.disconnect();
                        isRunning = false;
                        System.out.println("Goodbye!");
                        break;

                    default:
                        System.out.println("Unknown command: " + command);
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        
        scanner.close();
    }
}
