/*

   1. Name/Date:  James Valles, May, 2019

   2. Java version used:  1.8

   3. Precise command-line compilation examples / instructions:

    e.g.:
    Compile:
    > javac MyWebServer.java


   4. Precise examples / instructions to run this program:

    Compile:
    > javac MyWebServer.java

    Run:
    > java MyWebServer
    Open FireFox Browser. Enter url: http://localhost:2540/ or http://localhost:2540 to get server's root directory

    *This program was written and successfully runs and tested on  Mac Computer*

   5. List of files needed for running the program.
    a. MyWebServer.java

    Additional files submitted as part of assignment requirement:
    b. http-streams.txt
    c. serverlog.txt
    d. checklist-mywebserver.html
    e. Not included in .zip TTI Doc Submitted

   6. Notes:
    This program was written and successfully runs and has been tested on  Mac Computer. When testing everything worked. All parent directory
    links, directory, folder, and retrieval of dog.txt and cat.html, as well as addnum form calculation. If you have any questions when grading,
    please feel free to reach out for additional clarification: james@jamesvalles. Thank you so much for your hard work grading. This was an awesome
    assignment that allowed me to build a fully functioning multi-threaded Web Server.
*/

//This will retrieve the I/O libraries, socket, server socket libraries

import java.io.BufferedReader; //used for buffer reader
import java.io.IOException; // exception handling for IO
import java.io.InputStreamReader;//used for reading in data from stream
import java.io.PrintStream; //used to print data out from stream
import java.net.ServerSocket; //this is server socket waiting for incoming connection
import java.net.Socket; //socket
import java.io.*; //used for IO
import java.nio.file.Files; //used for getting file directory
import java.nio.file.Paths; //used for getting file paths


class WorkerThread extends Thread {

  //Socket is a class from java.net. Socket is an endpoint.  (java object). Used for communication.
  Socket sock;

  //Boolean used so that it print outgoing response once, while in while loop.
  Boolean printed = true;

  //Constructor for WorkerThread thread, where a Socket is passed in assigned to sock.
  WorkerThread(Socket s) {
    sock = s;
  }

  //In this case, run() will run the process for retrieving the in and out streams from the socket (end point of communication).
  //Output stream used for sending data, input stream used for reading data.
  public void run() {
    PrintStream out = null; //Initializing PrintStream so that we can write (send) data, flushes automatically
    BufferedReader in = null; //Initializing BufferedReader so that we can read in data
    printed = true; //Initializes printed variable to true (reset) each time worker thread started.


     /*creating a new bufferedreader (character input stream -> text. reads characters stores in internal buffer to reduce i/o operations)
      using an InputStreamReader (reads bytes and decodes them into characters) sock.getInputStream() retrieving InputStream from socket, so
      that we can use InputStreamReader in BufferedReader to read in the data received from socket getInputStream() -> InputStream, which has
      methods such as read, close, skip, reset, mark, etc. out = new PrintStream(sock.getOutputStream());
      in = new BufferedReader(new InputStreamReader(sock.getInputStream())); */
    try {
      //sock.getOutputStream() retrieving outpit stream from socket. This is the data we want to write.
      //getOutStream() -> OutputStream, which has the following methods available for use: flush, close, write
      out = new PrintStream(sock.getOutputStream());

      //sock.getInputStream() retrieving InputStream from socket, so that we can use InputStreamReader in BufferedReader to read in the data received from socket
      //getInputStream() -> InputStream, which has methods such as read, close, skip, reset, mark, etc.
      //creating a new bufferedreader (character input stream -> text. reads characters stores in internal buffer to reduce i/o operations) using an InputStreamReader (reads bytes and decodes them into characters)

      in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

      String socketData;
      // System.out.println("*** Request ***");
      socketData = in.readLine();

      //Creating a new HttpRequest object
      HttpRequest request = new HttpRequest(socketData);

      //Used to store the file math and headerMime to be sent out to process
      String file;
      String contentHeader = "";

      //If socketdata is not empty, then set incoming request data
      if (socketData != null) {
        request.setIncomingRequestData();
      }

      //Create a new HttpResponse object. This class handles gathering everything together that is needed to be sent to browser.
      HttpResponse response = new HttpResponse(request);

      //ignore /favicon.ico
      try {
        if (!response.getFile().equals("/favicon.ico")) {

          //Getting the response
          file = response.getFile().substring(1);
          contentHeader = response.getResponseHeader(file);

          //Sending out the content header followed by /r/n
          out.println(contentHeader);
          out.println("\r\n");

          //Print response message to console that response was processed
          if (printed) {
            response.printResponseToConsole();
            printed = false;
          }

          System.out.println("\n*** Processed for this Request ***");

          //If page or file cannot be found, print this, else print send the message
          if (response.getMessage() == null) {
            out.println("This item could not be found.");
          } else {
            out.println(response.getMessage());

          }

          System.out.println(socketData);

          //While loop to read in lines and printing the request to console
          do {
            socketData = in.readLine();

            if (socketData != null) {
              System.out.println(socketData);
              System.out.flush();
              //Closing socket
              sock.close();
            }
          } while (true);


        }
      } catch (Exception e) {
        System.out.println("Waiting for another request...");
      }
    } catch (IOException x) {
      System.out.println("--");
    }


  }
}

//This is the WebServer Class, which will establish connection between sever and client (Browser)
public class MyWebServer {

  //control switch added, but never used
  public static boolean control = true;

  //This is the main method
  public static void main(String a[]) throws IOException {
    int queueLength = 6; //used as a setting needed for a socket, tells OS if we have six simultaneous
    // connections, keep first 6 and discard the rest, queue of requests os should process
    int port = 2540;   // this is the port number, will ask OS to use this port and web server will listen on this port 2540.

    Socket socket;
    //Creating a new Server socket using queueLength and port
    ServerSocket serverSocket = new ServerSocket(port, queueLength);
    //Initial message that prints to screen
    System.out.println("James Valles' Web Server is listening at port 2540.\n");

    while (control) {
      //initially the server will have a serversocket and not a socket, it will wait for an incoming client connection requests
      //once a request comes in it will accepted, a socket object is created on the server side.

      socket = serverSocket
          .accept(); //saves the return value of accepting new connection from client
      new WorkerThread(socket)
          .start(); //create a new worker thread (multi-threaded server) and pass it a socket, then start it
    }
  }
}

//This class will handle the HttpRequest and will create a request object to be used in the ResponseClass
class HttpRequest {

  //Member variables initialized
  private String sockdata;
  private String requestMethod;
  private String filePath;
  private String protocol;

  //HttpRequest constructor, passing in sockdata from socket
  HttpRequest(String socketData) {
    this.sockdata = socketData;
  }

  //Parsing the sockdata, getting the request method (whether its a GET or POST, etc. The File path ie: /sub-a and lastly the protocol: HTTP/1.1
  public void setIncomingRequestData() {
    String[] incomingResponse = sockdata.split(" "); //splitting the sockdata by white spaces
    this.requestMethod = incomingResponse[0]; //getting the first element from string array assigning to request Method
    this.filePath = incomingResponse[1]; //getting the second element from string array assigning to filePath
    this.protocol = incomingResponse[2]; //getting the third element from string array assigning to protocol
  }

  //Returns filePath
  public String getFilePath() {
    return filePath;
  }

  //Returns Sockdata
  public String getSockdata() {
    return sockdata;
  }

  //Returns method request, GET/POST
  public String getRequestMethod() {
    return requestMethod;
  }

  //Returns protocol ie: HTTP1.1
  public String getProtocol() {
    return protocol;
  }
}

//This class represents the response, and will use the request object to send back a response to the browser
class HttpResponse {

  //Initializing member variables
  private HttpRequest request; //is the Request Object
  private String file;
  private String socketData; //Sockdata
  private String fileExtension; //the extension of the file ie: html or txt
  private String message; //the actual full message to be sent to browser, could be html, or just txt depending on file extension
  private String protocol; // the protocol ie: HTTP1.1
  private String requestMethod; //is this a GET/POST, the request method
  private String size; //The size of the content-length

  //This is the constructor for HTTPResponse, which takes in HTTPRequestObject
  HttpResponse(HttpRequest request) {
    this.request = request; //getting the actual request object
    this.file = request.getFilePath(); //getting file path
    this.socketData = request.getSockdata(); //getting sockdata
    this.protocol = request.getProtocol(); //getting the protocol
    this.requestMethod = request.getRequestMethod(); //getting the request method
  }

  //This method returns tne Response header which will be sent to the browser first before the message is sent. It will take in the file path ie: /sub-a
  public String getResponseHeader(String filePath) {

    //If sock data is not empty perform the following
    if (socketData != null) {

      //Is this a directory if so, set the file extension to html, create a new directory object
      if (Files.isDirectory(Paths.get(filePath))) {
        fileExtension = "html";
        //get the root directory
        if (filePath.length() == 0) {
          filePath = "./";
        }
        //Create new directory object, which will create an html page and page dynamically inject directory information using StringBuilder
        Directory directory = new Directory(filePath);

        //This is a directory
        System.out.println("\n*This is a directory. Loading directory page: " + filePath + "*");
        //This is the actual content that is sent after the content header is sent first, this is what will be displayed on browser
        //after invoking getDirectoryPage
        message = directory.getDirectoryPage(filePath);
        //getting the file's content-length
        this.size = String.valueOf(message.getBytes().length);

        //If the incoming request is a form fake-cgi, etc. do this, set file extension to html, and create new formprocessor object
      } else if (filePath.contains("fake-cgi")) {
        fileExtension = "html";
        //Creating a new form processor object
        FormProcessor formProcessor = new FormProcessor(filePath);
        //This is the actual content that is sent after the content header is sent first, this is what will be displayed on browser, after invoking
        //addnum method
        message = formProcessor.addnums();
        //getting the file's content-length
        this.size = String.valueOf(message.getBytes().length);
        //System.out.println("This is: " + filePath);
      } else {
        //Or do this if neither form, or directory, but is a static file

        // System.out.println("this is not a directory, but a filePath");
        //   System.out.println(socketData);

        //get the file path extension, split
        String[] extension = filePath.split("\\.");
        try {
          if (filePath.contains("fake-cgi")) {
            fileExtension = "html";
          } else {
            fileExtension = extension[1];
          }
          //This is the actual content that is sent after the content header is sent first, this is what will be displayed on browser, after invoking
          //readAllBytes, will get the file and will read in all bytes of that file, save to message variable and this is what will be sent to browser
          message = new String(Files.readAllBytes(Paths.get(filePath)));
          this.size = String.valueOf(message.getBytes().length);
        } catch (Exception e) {
          //If file is not found print this
          System.out.println("This item could not be found.");
        }
      }
    }
    //This is the content header
    return protocol + " 200 OK Content-Length:" + size + " Content-Type: text/" + fileExtension;
  }


  //This method is used to print response to console.
  public void printResponseToConsole() {
    System.out.println("\n*** Outgoing Response Processed***");
    System.out.println(protocol + " 200 OK");
    System.out.println("Content-Length: " + size);
    System.out.println("Content-Type: text/" + fileExtension);
    System.out.println("Response for Request: " + file);
    System.out.println("Output: " + message);

  }

  //This returns the httpRequest
  public HttpRequest getRequest() {
    return request;
  }

  //This returns file
  public String getFile() {
    return file;
  }

  //This returns socketdata from request
  public String getSocketData() {
    return socketData;
  }

  //This returns file extension
  public String getFileExtension() {
    return fileExtension;
  }

  //This sets file extension
  public void setFileExtension(String fileExtension) {
    this.fileExtension = fileExtension;
  }

  //This returns the actual message what is sent to browsser
  public String getMessage() {
    return message;
  }

  //This is the protocol
  public String getProtocol() {
    return protocol;
  }

  //This is returns request method
  public String getRequestMethod() {
    return requestMethod;
  }
}

//This class handles creating the html directory page
class Directory {

  //Creating member variables
  String filepath; //the current file path
  String message; //The message send to browser. This is the actual html directory page

  //Constructor takes in the file path
  Directory(String filepath) {
    this.filepath = filepath;
  }

  //Calls the readDirectory function using the file path
  public String getDirectoryPage(String filepath) {
    return readDirectory(filepath);
  }

  //This code was provided by Dr. Elliott and has been greatly modified, added string builder etc.
  public static String readDirectory(String filePath) {

    StringBuilder stringBuilder = new StringBuilder();

    //Creating new instance of file class, using path to file
    File fileClass = new File(filePath);
    //Getting list of files in the directory
    File[] files = fileClass.listFiles();

    //creatimg the html page, will append html, body, pre tags
    stringBuilder.append("<html>");
    stringBuilder.append("<body>");
    stringBuilder.append("<pre>");

    //This is the heading title on directory page
    //if root print this, else, print
    if (filePath.equals("./")) {
      stringBuilder.append("<h1>Index of " + filePath + "</h1><br>");
    } else {
      stringBuilder.append("<h1>Index of /" + filePath + "</h1><br>");
    }
    //if not root directory create parent directory
    if (!filePath.equals("./")) {

      try {
        String currentPartentPath = fileClass.getCanonicalPath()
            .substring(System.getProperty("user.dir").length())
            .substring(0, fileClass.toString().lastIndexOf("/") + 1);
        //This is the root
        if (currentPartentPath.length() == 0) {
          currentPartentPath = "/";
        }
        //Create parent directory
        stringBuilder.append(
            "<a href=\"" + currentPartentPath + "\">" + "Parent Directory"
                + "</a> <br><br>"); //this is the parent directory link
      } catch (Exception e) {
        System.out.println("Invalid Directory"); //directory does not exist
      }
    }

    for (int i = 0; i < files.length; i++) {
      if (files[i].isDirectory()) {
        //if is a directory create  DIR link to directory
        try {
          stringBuilder.append(
              "[DIR] " + "<a href=\"" + files[i].getCanonicalPath()
                  .substring(System.getProperty("user.dir").length()) + "\">" + files[i]
                  .getName()
                  + "</a> <br>");
          //System.out.println("[DIR] " + stringFile[i]);
        } catch (Exception e) {

        }
        //if this is an actual file, create TXT link to file
      } else if (files[i].isFile()) {
        try {
          stringBuilder.append(
              "[TXT] " + "<a href=\"" + files[i].getCanonicalPath()
                  .substring(System.getProperty("user.dir").length()) + "\">"
                  + files[i].getName() + "</a> <br>");
          // System.out.println("[TXT] " + stringFile[i]);
        } catch (Exception e) {

        }

      }
    }
    //Add closing tags for pre, body, html
    stringBuilder.append("</pre>");
    stringBuilder.append("</body>");
    stringBuilder.append("</html>");

    //Return the full html code for directory page as a string
    return stringBuilder.toString();
  }


}

//This is the form processor class, which will handle the addnum page
class FormProcessor {

  //These are the member variables
  String filePath; //the file path
  String name; //the input name
  int number1; //the first number
  int number2; //the second number
  int result;  //the final calculated result

  //This is the constructor for the form takes in file path
  FormProcessor(String filePath) {
    this.filePath = filePath;
  }

  //This method will split the file path ie: addnums.fake-cgi?person=John&num1=4&num2=5 and will split by & and = to retrieve values
  public String addnums() {
    String nameString; //the input name
    String num1; //the first number
    String num2; //the second number

    //Creating a new String builder to create result html page
    StringBuilder stringBuilder = new StringBuilder();
    //Creating a string array split by &
    String[] values = filePath.split("&");
    nameString = values[0]; //this is the name
    num1 = values[1]; //this is the first number
    num2 = values[2]; //this is the second number

    //Splitting by =
    name = nameString.split("=")[1]; //this is the final name
    number1 = Integer.parseInt(
        num1.split("=")[1]); //this is the final number 1, using parse to get int, instead of string
    number2 = Integer.parseInt(
        num2.split("=")[1]); //this is the final number 2, using parse to get int, instead of string
    this.result = number1 + number2; //this is the result of the calculation

    //creating the html form, opening tags, html, body, b, h4
    stringBuilder.append("<html><body>");
    stringBuilder.append("<b><h4>");
    //Appending the result
    stringBuilder.append(
        "Dear " + name + ", " + "the sum of " + number1 + " and " + number2 + " is " + this.result
            + ".");

    //These are the closing tags, b, h4, html, body
    stringBuilder.append("</b></h4>");
    stringBuilder.append("</html></body>");

    //Sending result html page to response object, so that it can be sent to browser, after content header is sent first
    return stringBuilder.toString();


  }

  //Gets file path
  public String getFilePath() {
    return filePath;
  }

  //Get input Name
  public String getName() {
    return name;
  }

  //Gets Number 1
  public int getNumber1() {
    return number1;
  }

  //Gets Number 2
  public int getNumber2() {
    return number2;
  }

  //Gets the final calculated result
  public int getResult() {
    return result;
  }
}