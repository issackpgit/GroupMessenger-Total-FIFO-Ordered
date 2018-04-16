package edu.buffalo.cse.cse486586.groupmessenger2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */

//Algorithm referred from http://studylib.net/doc/7830646/isis-algorithm-for-total-ordering-of-messages

public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();

    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final ArrayList<String> remotePortList = new ArrayList<String>();
    static final HashMap<String, Boolean> portStat = new HashMap<String, Boolean>();
    static int counter =0;
    static int count =0;
    static int seqNo =0;
    static String myPort;
    static String failurePort = "";
    static final Uri mUri = OnPTestClickListener.buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
    static final int SERVER_PORT = 10000;
    static PriorityQueue<Message> holdBackQue = new PriorityQueue<Message>(100, new Comparator<Message>() {
        @Override
        public int compare(Message lhs, Message rhs)
        {
            if(lhs.priority == rhs.priority) {
                if(lhs.getPortNo()<rhs.getPortNo())
                    return -1;
                else if(lhs.getPortNo()>rhs.getPortNo())
                    return 1;
                else
                    return 0;
            }
            else if(lhs.priority<rhs.priority)
            {
                return -1;
            }
            else
                return 1;
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        remotePortList.add(REMOTE_PORT0);
        remotePortList.add(REMOTE_PORT1);
        remotePortList.add(REMOTE_PORT2);
        remotePortList.add(REMOTE_PORT3);
        remotePortList.add(REMOTE_PORT4);

        for(String item : remotePortList){
            portStat.put(item, true);
        }

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        final EditText editText1 = (EditText) findViewById(R.id.editText1);

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(new OnPTestClickListener(tv, getContentResolver()));

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText1.getText().toString() + "\n";
                editText1.setText("");
                //                tv.append("\t" + msg);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);

            }
        });

    }
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {

            String msg = strings[0];
            String id = myPort+"-"+counter;
            int maxPriority=0;
            counter++;

            String fMessage = "FirstMessage"+":"+msg+":"+counter+":"+id+":\n";

            String aMessage = "";
            Socket socket = null;

            for(String item : remotePortList){
                try {
                    if(!item.equals(failurePort)) {
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(item));
                        OutputStream out = socket.getOutputStream();
                        DataOutputStream writer1 = new DataOutputStream(out);
                        writer1.writeUTF(fMessage);
                        InputStream in = socket.getInputStream();
                        DataInputStream reader1 = new DataInputStream(in);
                        String msgRecieved = reader1.readUTF();
                        socket.setSoTimeout(500);
                        in.close();
                        reader1.close();

                        if (msgRecieved.split(":")[0].equals("ProposalMessage")) {
                            maxPriority = Math.max(maxPriority, Integer.parseInt(msgRecieved.split(":")[2]));
                            aMessage = "AgreeMessage" + ":" + msgRecieved.split(":")[1] + ":" + maxPriority + ":\n";
                        }
                        socket.close();
                    }
                }
                catch (SocketTimeoutException e){
                    Log.i("Catching STE","ISK");
                }
                catch (EOFException e){
                    int port= socket.getPort();
                    Log.i("PORT NOO ISK:",String.valueOf(port));
                    failurePort = item;
                    removeMsg(item);

                    try {

                        socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                catch (SocketException e){
                    int port= socket.getPort();
                    Log.i("PORT NOO ISK:",String.valueOf(port));
                    failurePort = item;
                    removeMsg(item);
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                }

                catch (IOException e) {
                    int port= socket.getPort();
                    Log.i("PORT NOO ISK:",String.valueOf(port));
                    failurePort = item;
                    removeMsg(item);
                    try {

                        socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }

            }

            for(String item : remotePortList){
                try{
                    if(!item.equals(failurePort)) {
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(item));
                        OutputStream out = socket.getOutputStream();
                        DataOutputStream writer1 = new DataOutputStream(out);
                        writer1.writeUTF(aMessage);
                        writer1.flush();
                        writer1.close();
                        out.close();
                        socket.close();
                    }
                } catch (SocketException e){
                    failurePort = item;
                    removeMsg(item);
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                } catch (UnknownHostException e) {
                    failurePort = item;
                    removeMsg(item);
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                } catch (IOException e) {
                    failurePort = item;
                    removeMsg(item);
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                }
            }
            return null;
        }
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @SuppressLint("LongLogTag")
        @Override
        protected Void doInBackground(ServerSocket... serverSockets) {

            ServerSocket serverSocket = serverSockets[0];
            Socket socket = null;
            try {
                while (true) {
                    socket = serverSocket.accept();
                    socket.setSoTimeout(1000);
                    InputStream in = socket.getInputStream();
                    DataInputStream reader1 = new DataInputStream(in);
                    String msgRecieved = reader1.readUTF();

                    Message serverMsg = new Message();

                    Log.i("FailurePort : ",failurePort);

                    if(msgRecieved.split(":")[0].equals("FirstMessage")){
                        ++seqNo;
                        String id = msgRecieved.split(":")[3];
                        serverMsg.id = id;
                        serverMsg.priority = seqNo;
                        serverMsg.msg = msgRecieved.split(":")[1];
                        serverMsg.deliverable = false;
                        holdBackQue.add(serverMsg);

                        String pMessage = "ProposalMessage"+":"+id+":"+seqNo+":\n";

                        Log.i("Failed Process in first msg:",failurePort);

                        OutputStream out = socket.getOutputStream();
                        DataOutputStream writer1 = new DataOutputStream(out);
                        writer1.writeUTF(pMessage);
                        in.close();
                        reader1.close();
                    }
                    else if(msgRecieved.split(":")[0].equals("AgreeMessage")){
                        seqNo = Math.max(seqNo,Integer.parseInt(msgRecieved.split(":")[2]));
                        String id = msgRecieved.split(":")[1];
                        Message temp = null;

                        Log.i("Failed Process in agree msg:",failurePort);

                        Iterator it = holdBackQue.iterator();
                        while(it.hasNext()){
                            temp = (Message) it.next();
                            if(temp.id.equals(id)) {
                                it.remove();
                                temp.priority = Integer.parseInt(msgRecieved.split(":")[2]);
                                temp.deliverable = true;
                                holdBackQue.add(temp);
                                break;
                            }
                        }
                    }

                    while(!holdBackQue.isEmpty()){
                        int index = 0;
                        Message temp = holdBackQue.peek();
                        if(temp.deliverable) {
                            String sender = temp.id.split("-")[0];
                            String counter = temp.id.split("-")[1];
                            if (sender.equals(REMOTE_PORT0)) {
                                index = 0;
                            } else if (sender.equals(REMOTE_PORT1)) {
                                index = 1;
                            } else if (sender.equals(REMOTE_PORT2)) {
                                index = 2;
                            } else if (sender.equals(REMOTE_PORT3)) {
                                index = 3;
                            } else if (sender.equals(REMOTE_PORT4)) {
                                index = 4;
                            }
                            Log.i("TAG","Sending message:"+index+":"+counter);
                            String[] out = {temp.msg + ":" + index + ":" + counter, String.valueOf(count++)};
                            publishProgress(out);
                            holdBackQue.remove(temp);
                        }
                        else
                            break;

                        }

                    socket.close();
                }
            }catch(SocketTimeoutException e){
                Log.i("STE ISK :","ISK");
                int port = socket.getLocalPort();
                System.err.println("ISK in STE");
            }catch (SocketException e){
                 System.err.println("SS in SS");
            }
            catch (IOException e) {
                System.err.println("ISK in SS IOE");
                e.printStackTrace();
            }

            return null;
        }


        protected void onProgressUpdate(String...strings) {

            TextView tv = (TextView) findViewById(R.id.textView1);
            String str = strings[0].trim();
            tv.append(str + "\t\n");
            String key = strings[1];

            ContentValues cv = new ContentValues();
            cv.put("key", key);
            cv.put("value", str);
            Log.e(TAG,cv.toString());

            getContentResolver().insert(mUri,cv);

        }

    }

    public void removeMsg(String val){

        Iterator it = holdBackQue.iterator();
        while(it.hasNext()){
            Message temp = (Message)it.next();
            if (temp.id.split("-")[0].equals(val) && !temp.deliverable){
                holdBackQue.remove(temp);
            }
        }
        
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}