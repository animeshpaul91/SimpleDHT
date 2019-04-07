package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.w3c.dom.NodeList;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;


public class SimpleDhtProvider extends ContentProvider {

    //Variables that we will need
    private static final String TAG = SimpleDhtProvider.class.getSimpleName();
    private static String ePort = null; //Emulator Port
    private static String myPorthash = null; //Hash value of myport
    private static final int SERVER_PORT = 10000;
    private static final String first_node = "5554";
    boolean isAlone;
    boolean serverreq;
    private static final String key_field = "key";
    private static final String  value_field = "value";
    private static final String my_dht = "@";
    private static final String all_dht = "*";

    private static final String NJ = "NJ"; //Literal for Node Join event
    private static final String UN = "UN"; //Literal to update neighbor
    private static final String F = "F"; //Literal to forward a key value
    private static final String G = "G"; //Literal to get value for key request
    private static final String GA = "GA"; //Literal to get all keys
    private static final String D = "D"; //Literal to delete a key
    private static final String DA = "DA"; //Literal to delete all keys
    private static Uri provideruri = Uri.parse("content://edu.buffalo.cse.cse486586.simpledht.provider"); //URI
    private static String prevNode; //Keeps track of predecessor for this node
    private static String nextNode; //Keeps track of successor for this node
    private static String sourceNode; //Keeps track of source Node
    List<String> fileList; //stores/maps keys for this Node
    List<Node> nodeList; //This is the ring

    private class Node
    {
        String port, hash;
        String succ = null, pred = null;

        Node(String port, String hash)
        {
            this.port = port;
            this.hash = hash;
        }

        public String getPort()
        {
            return this.port;
        }

        public String getHash()
        {
            return this.hash;
        }

        public  String getSucc()
        {
            return this.succ;
        }

        public String getPred()
        {
            return this.pred;
        }

        @Override
        public String toString()
        {
            return ("NodeID: "+this.port+" Pred: "+this.getPred()+" Succ: "+this.getSucc());
        }
    }

    public void sendmsgCT(String msg)
    {
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        String currentQuery = selection;
        if (isAlone) //Phase 1 of grader
        {
            if (currentQuery.equals(my_dht) || currentQuery.equals(all_dht)) //* or @ is same for a single node
            {
                for (String key: fileList)
                {
                    getContext().deleteFile(key); //Deletes the file from avd
                }
                fileList.clear();
                Log.d(TAG, "Main_Delete: "+ePort+" All Files from Avd are Deleted");
                return 1;
            }

            else //for a particular key(!= @ or !=*)
            {
                boolean fileFound = false;
                for (String key: fileList)
                {
                    if (key.equals(currentQuery))
                    {
                        getContext().deleteFile(key);
                        fileFound = true;
                        break;
                    }
                }

                if (fileFound)
                {
                    fileList.remove(currentQuery);
                    Log.d(TAG, "Main_Delete: "+ePort+" File with Key= "+currentQuery+" is deleted");
                    return 1;
                }
                return 0;
            }
        }

        else //for next phases
        {

        }

        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        String key, value, keyhash = null;
        key = values.getAsString(key_field);
        value = values.getAsString(value_field);
        FileOutputStream fileOutputStream;

        try {
            keyhash = genHash(key);
        }
        catch (NoSuchAlgorithmException e)
        {
            Log.e(TAG, "Main_Insert: "+ePort+" No Such Algorithm Exception Occurred");
            e.printStackTrace();
        }
        if (isAlone) //Phase 1 where app gets installed in only 1 avd
        {
            try
            {
                fileOutputStream = getContext().openFileOutput(key, Context.MODE_PRIVATE);
                fileOutputStream.write(value.getBytes());
                Log.d(TAG, "Main_Insert: "+ePort+" Insertion Successful "+values.toString());
                fileList.add(key); //Mapping this key to this Node
                Log.d(TAG, "Main_Insert: "+ePort+" FileList with me is: "+fileList.toString());
            }
            catch (IOException e)
            {
                Log.e(TAG, "Main_Insert: " +ePort+ " IO Exception Occurred");
                Log.e(TAG, e.getMessage());
                Log.e(TAG, Log.getStackTraceString(e));
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "Main_Insert: " +ePort+ " Nullpointer Exception Occurred");
                e.printStackTrace();
            }
            return uri;
        }

        else //for next phases
        {
            try {
                String nexthash = genHash(nextNode);
                String prevhash = genHash(prevNode);
                if (myPorthash.compareTo(prevhash) > 0)
                {
                    Log.d(TAG, prevhash+";"+myPorthash+";"+keyhash);
                    if (keyhash.compareTo(prevhash) > 0 && keyhash.compareTo(myPorthash) <= 0) //this key is in my scope
                    {
                        try {
                            fileOutputStream = getContext().openFileOutput(key, Context.MODE_PRIVATE);
                            fileOutputStream.write(value.getBytes());
                            fileList.add(key);
                            Log.d(TAG, "FileList: "+fileList);
                        }
                        catch (NullPointerException e)
                        {
                            Log.e(TAG, "Main_Insert: " +ePort+ " Nullpointer Exception Occurred");
                            e.printStackTrace();
                        }

                        catch (IOException e)
                        {
                            Log.e(TAG, "Main_Insert: " +ePort+ " IO Exception Occurred");
                            e.printStackTrace();
                        }
                    }

                    else //forward to next Node
                    {
                        String msgtoclient = F+";"+key+";"+value;
                        sendmsgCT(msgtoclient);
                    }
                }

                else
                {
                    if (keyhash.compareTo(prevhash) > 0 || keyhash.compareTo(myPorthash) <= 0) //Condition between last node and first node. Even then this key is in my scope
                    {
                        try{
                            fileOutputStream = getContext().openFileOutput(key, Context.MODE_PRIVATE);
                            fileOutputStream.write(value.getBytes());
                            fileList.add(key);
                            Log.d(TAG, "FileList: "+fileList);
                        }

                        catch (NullPointerException e)
                        {
                            Log.e(TAG, "Main_Insert: " +ePort+ " Nullpointer Exception Occurred");
                            e.printStackTrace();
                        }

                        catch (IOException e)
                        {
                            Log.e(TAG, "Main_Insert: " +ePort+ " IO Exception Occurred");
                            e.printStackTrace();
                        }
                    }

                    else //forward to next node. This part may be redundant
                    {
                        String msgtoclient = F+";"+key+";"+value;
                        sendmsgCT(msgtoclient);
                    }
                }
            }

            catch (NoSuchAlgorithmException e)
            {
                Log.e(TAG, "Main_Insert: "+ePort+" No Such Algorithm Exception Occurred");
                e.printStackTrace();
            }
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        ePort = String.valueOf(Integer.parseInt(portStr)); //Emulator Port ( eg.5554)
        //myPort = String.valueOf((Integer.parseInt(portStr) * 2)); //Redirection Port ( eg.11108)
        Log.d(TAG, "My Port is: "+ePort);

        fileList = new ArrayList<String>();
        isAlone = false;
        serverreq = false; //Flags are false for other Nodes

        try
        {
            myPorthash = genHash(ePort);
        }
        catch (NoSuchAlgorithmException e)
        {
            Log.e(TAG, "Main_Oncreate: "+ePort+" No Such Algorithm Exception Occurred");
            e.printStackTrace();
        }

        if (ePort.equals(first_node)) //First Node gets added to chord
        {
            isAlone = true; //Flag is true for my Node
            Node initial_node = new Node(ePort, myPorthash);
            nodeList = new ArrayList<Node>(); //Maintain a state at Node 5554
            nodeList.add(initial_node); //Node 5554 first joins the chord
        }

        try{
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        }
        catch (IOException e)
        {
            Log.e(TAG, "Main_Oncreate: "+ePort+" Can't Create a ServerSocket");
            e.printStackTrace();
            return false;
        }

        if (!ePort.equals(first_node)) //All other nodes send node join request to first node
        {
            sendmsgCT(NJ+";"); //All Other nodes send request to join the chord via their Client Tasks
        }

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub
        String currentQuery = selection;
        FileInputStream fileInputStream;
        String[] columns = {key_field, value_field};
        MatrixCursor mc = new MatrixCursor(columns);
        String message = new String();

        if (isAlone) //Phase 1 of grader
        {
            if (currentQuery.equals(my_dht) || currentQuery.equals(all_dht)) //@ and * mean the same thing in this context
            {
                try {
                    for (String key : fileList) //for all keys in my avd
                    {
                        fileInputStream = getContext().openFileInput(key);
                        if (fileInputStream !=null)
                        {
                            BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream));
                            message = br.readLine();
                            String[] row = {key, message};
                            mc.addRow(row);
                            br.close();
                            fileInputStream.close();
                            Log.d(TAG, "Main_Query: "+ePort+" All keys are queried Successfully");
                        }
                    }
                }

                catch (FileNotFoundException e)
                {
                    Log.e(TAG, "Main_Query: "+ePort+" Unable to Open file");
                    e.printStackTrace();
                }

                catch (IOException e)
                {
                    Log.e(TAG, "Main_Query: "+ePort+" IO Exception Occurred");
                    e.printStackTrace();
                }

                return mc;
            }

            else //for a particular key value
            {
                try
                {
                    fileInputStream = getContext().openFileInput(currentQuery);
                    if (fileInputStream != null)
                    {
                        BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream));
                        message = br.readLine();
                        String[] row = {currentQuery, message};
                        mc.addRow(row);
                        br.close();
                        fileInputStream.close();
                        Log.d(TAG, "Main_Query: "+ePort+" Key= "+currentQuery+" queried successfully");
                    }
                }

                catch (FileNotFoundException e)
                {
                    Log.e(TAG, "Main_Query: "+ePort+" Unable to Open file");
                    e.printStackTrace();
                }

                catch (IOException e)
                {
                    Log.e(TAG, "Main_Query: "+ePort+" IO Exception Occurred");
                    e.printStackTrace();
                }

                return mc;
            }
        }

        else // Next Phases
        {
           if (currentQuery.equals(my_dht)) //Selection of @ in self node
           {
               try
               {
                   for (String key: fileList)
                   {
                       fileInputStream = getContext().openFileInput(key);
                       if (fileInputStream!=null)
                       {
                           BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream));
                           message = br.readLine();
                           String row[] = {key, message};
                           mc.addRow(row);
                           br.close();
                           fileInputStream.close();
                       }
                   }
                   Log.d(TAG, "Main_Query: "+ePort+" All keys in my Node are queried Successfully");
               }

               catch (FileNotFoundException e)
               {
                   Log.e(TAG, "Main_Query: "+ePort+" Unable to Open file");
               }

               catch (IOException e)
               {
                   Log.e(TAG, "Main_Query: "+ePort+" IO Exception Occurred");
               }

               return mc;
           }

           else
           {
               if (currentQuery.equals(all_dht))
               {
                   //Write Logic of * here
               }
           }
        }

        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }


    //Server Task starts here
    private class ServerTask extends AsyncTask<ServerSocket, String, Void>
    {
        @Override
        protected Void doInBackground(ServerSocket... sockets)
        {
            Log.d(TAG, "Server: "+ePort+" In Server Task");
            ServerSocket serverSocket = sockets[0];
            String msgfromclient, msgtoclient, ack[];
            String [] pieces;

            while (true)
            {
                try {
                    Socket server = serverSocket.accept();
                    DataInputStream in = new DataInputStream(server.getInputStream());
                    DataOutputStream out = new DataOutputStream(server.getOutputStream());
                    msgfromclient = in.readUTF();
                    pieces = msgfromclient.split(";");
                    out.writeUTF("ACK;"+ePort);
                    out.flush();
                    out.close();
                    in.close();

                    if (pieces[0].equals(NJ)) //A new Node asks for joining
                    {
                        Log.d(TAG, "Server: "+ePort+" NJ Message Received From: "+pieces[1]);
                        if (ePort.equals(first_node)) //First Node Processes the Join request
                        {
                            try
                            {
                                isAlone = false;
                                boolean isInserted = false;
                                String hash = genHash(pieces[1]); //pieces[1] will be local emulator port of the sender eg. 5554
                                Node this_node = new Node(pieces[1], hash); //instatiate a new Node Object
                                //Node Insertion Logic
                                int n = nodeList.size();
                                for (int i=0; i<n; i++)
                                {
                                    Node node = nodeList.get(i);
                                    int l = hash.compareTo(node.getHash());
                                    if (l < 0) {
                                        if (n == 1) //base case where the ring contains 5554 only
                                        {
                                            this_node.succ = node.port;
                                            this_node.pred = node.port;
                                            node.succ = this_node.port;
                                            node.pred = this_node.port;
                                            nodeList.add(i, this_node); //Add this_node before existing node.
                                            isInserted = true;
                                            break;
                                        }

                                        else { //there is more than one node in ring

                                            if (i == 0) //this_node is lexicographically smallest
                                            {
                                                this_node.pred = nodeList.get(n - 1).port; //Assign predecessor to last node
                                                nodeList.get(n - 1).succ = this_node.port;
                                                //nodeList.get(0).pred = this_node.node;
                                            } else //this node is to be inserted in between the list
                                            {
                                                this_node.pred = nodeList.get(i - 1).port;
                                                nodeList.get(i - 1).succ = this_node.port;
                                            }
                                            this_node.succ = node.port; //In any case
                                            node.pred = this_node.port;
                                            nodeList.add(i, this_node);
                                            isInserted = true;
                                            break;

                                        }
                                    }
                                }
                                if (!isInserted) // hash was found to be alphabetically higher than all nodes. Will be added at end of the nodelist
                                {
                                    this_node.pred = nodeList.get(n-1).port;
                                    nodeList.get(n-1).succ = this_node.port;
                                    this_node.succ = nodeList.get(0).port;
                                    nodeList.get(0).pred = this_node.port;
                                    nodeList.add(this_node); //Just append the node
                                }
                                sendmsgCT(UN+";"); //Update Other Nodes about their neighbours
                            }
                            catch (NoSuchAlgorithmException e)
                            {
                                Log.e(TAG, "Server: "+ePort+" No Such Algorithm Exception Occurred");
                                e.printStackTrace();
                            }
                        }
                    }

                    else
                    {
                        if (pieces[0].equals(UN))
                        {
                            prevNode = pieces[1]; //Set Updated Neighbors
                            nextNode = pieces[2];
                        }

                        else
                        {
                            if (pieces[0].equals(F))
                            {
                                Log.d(TAG, "Server: "+ePort+" Received Forwarded Message: "+msgfromclient+" from: "+prevNode);
                                ContentValues contentValues = new ContentValues();
                                contentValues.put(key_field, pieces[1]);
                                contentValues.put(value_field, pieces[2]);
                                getContext().getContentResolver().insert(provideruri, contentValues);
                            }
                        }

                    }

                }
                catch (IOException e)
                {
                    Log.e(TAG, "Server: " +ePort+ " IO Exception Occurred");
                    e.printStackTrace();
                }
            }
        }
    }

    //Client Task starts here
    private class ClientTask extends AsyncTask<String, Void, String> //Capable of returning a string type
    {
        @Override
        protected String doInBackground(String... msgs)
        {
            String[] pieces = msgs[0].split(";");
            String [] ack;
            if (pieces[0].equals(NJ))
            {
                if (!ePort.equals(first_node))
                {
                    try
                    {
                        Socket client = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(first_node) * 2);
                        DataInputStream in = new DataInputStream(client.getInputStream());
                        DataOutputStream out = new DataOutputStream(client.getOutputStream());
                        String msgToServer = NJ + ";" + ePort;
                        out.writeUTF(msgToServer);
                        out.flush();
                        ack = in.readUTF().split(";");
                        if (!ack[0].equals("ACK"))
                            Log.e(TAG,"Client: "+ePort+" Did not receive Ack");
                        out.close();
                        in.close();
                        client.close();
                    }

                    catch (SocketException e)
                    {
                        Log.e(TAG, "Client: "+ePort+" Socket Exception Occurred");
                        isAlone = true;
                        Node my_node = new Node(ePort, myPorthash);
                        nodeList = new ArrayList<Node>();
                        nodeList.add(my_node); //add mynode to Ring
                    }

                    catch (EOFException e)
                    {
                        Log.e(TAG, "Client: "+ePort+" EOF Exception Occurred");
                        isAlone = true;
                        Node my_node = new Node(ePort, myPorthash);
                        nodeList = new ArrayList<Node>();
                        nodeList.add(my_node); //add mynode to Ring
                    }

                    catch (UnknownHostException e)
                    {
                        Log.e(TAG, "Client: "+ePort+" UnknownHost Exception Occurred");
                    }

                    catch (IOException e)
                    {
                        Log.e(TAG, "Client: "+ePort+" IO Exception Occurred");
                    }

                    catch (Exception e)
                    {
                        Log.e(TAG, "Client: "+ePort+" Other Exception Occurred");
                        isAlone = true;
                        Node my_node = new Node(ePort, myPorthash);
                        nodeList = new ArrayList<Node>();
                        nodeList.add(my_node); //add mynode to Ring
                    }
                }
            }

            else {

                if (pieces[0].equals(UN)) //Update Neighbour Message
                {
                    for (Node node: nodeList) {
                        String msgtoServer = UN + ";" + node.getPred() + ";" + node.getSucc();
                        try {
                            Socket client = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(node.getPort()) * 2);
                            DataInputStream in = new DataInputStream(client.getInputStream());
                            DataOutputStream out = new DataOutputStream(client.getOutputStream());
                            out.writeUTF(msgtoServer);
                            out.flush();
                            ack = in.readUTF().split(";");
                            if (!ack[0].equals("ACK"))
                                Log.e(TAG, "Client: " + ePort + " Did not receive Ack");
                            out.close();
                            in.close();
                            client.close();
                        } catch (UnknownHostException e) {
                            Log.e(TAG, "Client: " + ePort + " UnknownHost Exception Occurred");
                        } catch (IOException e) {
                            Log.e(TAG, "Client: " + ePort + " IO Exception Occurred");
                        }
                    }
                }

                else
                {
                    if (pieces[0].equals(F)) //Forward a Message to the next avd
                    {
                        try
                        {
                            Log.d(TAG, "Client: "+ePort+" Forwarding Message: "+msgs[0]+" to: "+nextNode);
                            Socket client = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(nextNode) * 2);
                            DataInputStream in = new DataInputStream(client.getInputStream());
                            DataOutputStream out = new DataOutputStream(client.getOutputStream());
                            out.writeUTF(msgs[0]);
                            out.flush();
                            ack = in.readUTF().split(";");
                            if (!ack[0].equals("ACK"))
                                Log.e(TAG, "Client: " + ePort + " Did not receive Ack");
                            out.close();
                            in.close();
                            client.close();
                        }
                        catch (UnknownHostException e) {
                            Log.e(TAG, "Client: " + ePort + " UnknownHost Exception Occurred");
                        } catch (IOException e) {
                            Log.e(TAG, "Client: " + ePort + " IO Exception Occurred");
                        }
                    }
                }
            }

            return null;
        }
    }
}
