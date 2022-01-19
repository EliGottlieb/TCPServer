import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;


public class TCPServer {
        public static void main(String[] args) throws Exception {
            String working = System.getProperty("user.dir"); //working variable is set to the current working directory
            BufferedReader configReader1 = new BufferedReader(new FileReader(new File(working + "/esg58/config.txt")));
            int port = Integer.parseInt(configReader1.readLine().split("--")[1]); //reads port from config file
            ServerSocket welcomeSocket = new ServerSocket(port); //welcomeSocket will listen to inputted port
            while (true) { //this format of multithreading is adapted from the sample multithreaded server provided
                System.out.println("Listening...");
                Socket communicationSocket = welcomeSocket.accept(); //Welcome socket connects the client socket to the connection socket
                SocketThread st = new SocketThread(communicationSocket);
                st.start();
            }
        }

        public static class SocketThread extends Thread {
            private final Socket cs;

            public SocketThread(Socket communicationSocket) {
                cs = communicationSocket;
            }
            public void run() {
                //General structure of run(): a try around a while loop which runs while(true) if config file persistence value supports it
                //While loop will exit on error in connection, parsing, a nonpersistent connection, a nonpersistent config value, or timeout (given a persistent config value)
                BufferedReader clientInput = null;
                DataOutputStream toClient = null;
                System.out.println("----Connection established----");
                boolean isKeepAlive = true;
                //Before the while loop, the directory is retrieved from the config file, as it will not be changing throughout a persistent connection
                //The persistence true/false value from config is also retrieved from config file
                //streams for both input and output are established
                try {
                    String working = System.getProperty("user.dir");
                    BufferedReader configReader = new BufferedReader(new FileReader(new File(working + "/esg58/config.txt")));
                    configReader.readLine();
                    String dir = configReader.readLine().split("--")[1];
                    boolean isPersistent = configReader.readLine().split("--")[1].equals("true");
                    int idleTime = Integer.parseInt(configReader.readLine().split("--")[1]);
                    clientInput = new BufferedReader(new InputStreamReader(cs.getInputStream())); //reader for input from client
                    toClient = new DataOutputStream(cs.getOutputStream()); //creates stream out to client

                    while (isKeepAlive) {
                        //the timeout time for the socket is reset at the beginning of each request (if persistent) so as to only
                        // timeout if the socket is idle waiting for the next request for too long
                        if(isPersistent){cs.setSoTimeout(idleTime);}

                        //Variables are set and reset in between requests given a persistent connection
                        boolean fourOhFour; //fourOhFour will be used as a flag. If the URL or filePath is invalid, the flag will be true
                        String cookieString = "";
                        int cookieValue = 0;

                        //reads 1st line of request to store and verify filePath
                        String input = clientInput.readLine();
                        String fileName = input.split(" ")[1].split("/")[2];
                        fourOhFour = !(input.split(" ")[1].split("/")[1].equals("esg58"));
                        String filePath = dir + "/" + fileName;

                        //checks if the fileName being requested is valid if is false. If fourOhFour is already true there is no need to check this as the request is already bad
                        String[] temp = {"visits.html", "test1.html", "test2.html"};
                        boolean validName = false;
                        if (!fourOhFour) {
                            for (String s : temp) {
                                if (fileName.equals(s)) {
                                    validName = true;
                                    break;
                                }
                            }
                            fourOhFour = !validName;
                        }

                        //reads through the rest request, parsing for the cookie header (if present) and a connection header
                        //the cookies portion may not find any cookies or too many cookies
                        input = clientInput.readLine();
                        while (input != null && !(input.equals(""))) { //reads until the end of the headers of the request
                            if (input.split(":")[0].equals("Connection")) { //searching for connection header
                                isKeepAlive = input.split(":")[1].contains("keep-alive");
                            }
                            if (input.split(":")[0].equals("Cookie")) { //searching for the cookie header
                                for (int i = 0; i < input.split(":")[1].split(";").length; i++) {
                                    if (input.split(":")[1].split(";")[i].contains("esg58")) {
                                        cookieString = input.split(":")[1].split(";")[i].split("=")[1];
                                        cookieValue = Integer.parseInt(cookieString);
                                    }
                                }
                            }
                            System.out.println(input);
                            input = clientInput.readLine();
                        }

                        //an if, else if, else chain for a 404 response, a dynamic http response (visits), and a static http response (test1,test2) respectively
                        //if filePath or fileName are invalid, send 404 Not Found
                        if (fourOhFour) {
                            fileName = "404.html";
                            filePath = dir + "/" + "404.html";
                            File html = new File (filePath);
                            toClient.writeBytes("HTTP/1.1 404 Not Found" + '\n');
                            if(!isPersistent){toClient.writeBytes("Connection: close" + '\n');}
                            toClient.writeBytes("Content-Length: " + html.length() + '\n');
                            toClient.writeBytes("\n");
                            DataInputStream htmlReader = new DataInputStream(new FileInputStream(html));
                            byte[] fileBytes = new byte[(int) html.length()];
                            htmlReader.readFully(fileBytes);
                            toClient.write(fileBytes);
                        }

                        //if there was no Cookie header in the request, the Set-Cookie line will make sure there are cookies in the response
                        //the body of the response is hard coded here into the server. The correct cookie value is dynamically found using the request and then inserted into the response body
                        else if (fileName.equals("visits.html")) {
                            cookieValue++; //We wait to increment cookie until after the point we would return 404
                            File html = new File(filePath);
                            toClient.writeBytes("HTTP/1.1 200 OK" + '\n');
                            toClient.writeBytes("Set-Cookie: " + "esg58_visits=" + cookieValue + "; Path=" + filePath + '\n');
                            if(!isPersistent){toClient.writeBytes("Connection: close" + '\n');}
                            String cookieLine = "<p>"+ cookieValue +"</p> \n";
                            String body = "<!DOCTYPE html>\n<html>\n<head>\n<link rel=\"icon\" href=\"data:;\">\n</head>\n<body>\n<p>Number of times your browser has viewed this site: </p>\n"+cookieLine+"</body>\n</html>\n";
                            int testLen = body.length();
                            toClient.writeBytes("Content-Length: " + testLen + '\n');
                            toClient.writeBytes("\n");
                            byte[] fileBytes = body.getBytes();
                            toClient.write(fileBytes);

                            //if there was no Cookie header in the request, the Set-Cookie line will make sure there are cookies in the response
                            //writing the body of the response consists of reading static html files and then writing them to the client
                        } else {
                            cookieValue++; //We wait to increment cookie until after the point we would return 404
                            File html = new File(filePath);
                            toClient.writeBytes("HTTP/1.1 200 OK" + '\n');
                            toClient.writeBytes("Set-Cookie: " + "esg58_visits=" + cookieValue + "; Path=" + filePath + '\n');
                            if(!isPersistent){toClient.writeBytes("Connection: close" + '\n');}
                            toClient.writeBytes("Content-Length: " + html.length() + '\n');
                            toClient.writeBytes("\n");
                            DataInputStream htmlReader = new DataInputStream(new FileInputStream(html));
                            byte[] fileBytes = new byte[(int) html.length()];
                            htmlReader.readFully(fileBytes);
                            toClient.write(fileBytes);
                        }
                        System.out.println(fileName + " has been processed");
                        if(!isPersistent) //if the server does not support persistent connection, the while loop is broken and the connection is closed
                        {break;}
                        System.out.println("----Continuing Connection----");
                    }
                    clientInput.close();
                    toClient.close();
                    cs.close();
                    System.out.println("Closing Connection");
                }
                catch(SocketTimeoutException t) //Catches a timeout and closes the connection
                {
                    try {
                        clientInput.close();
                        toClient.close();
                        cs.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("----Connection timed out----");
                }
                catch (Exception ex) { //General catch for a random request that could cause an out of bounds or other various errors
                    try {
                        clientInput.close();
                        toClient.close();
                        cs.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("----Connection to client was lost----");
                }
            }
        }
    }






