package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity   for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static int seq_no = 0;
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button1).setOnClickListener(new OnPTestClickListener(tv, getContentResolver()));
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            }
        catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }


        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button Send_Button = (Button) findViewById(R.id.button4);

        Send_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\t" + msg); // This is one way to display a string.
                localTextView.append("\n");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            try{
                while(true){
                    Socket socket = serverSocket.accept();
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    String messagein = in.readUTF();
                    if(messagein!=null){
                        out.writeUTF("ACK");
                        publishProgress(messagein);
                        in.close();
                        out.close();
                        socket.close();
                    }
                }
            }
            catch(Exception e){
                Log.e(TAG,"IO Exception!");
            }
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            remoteTextView.append("\n");

            //Content provider URI builder
            //Referred from https://developer.android.com/reference/android/net/Uri.Builder.html
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger1.provider");
            uriBuilder.scheme("content");
            Uri uri = uriBuilder.build();

            //Content values object
            ContentValues cv = new ContentValues();
            //Inserting Key-Value pairs
            cv.put("key", Integer.toString(seq_no));
            cv.put("value",strReceived);
            //Content resolver
            //Referred from https://developer.android.com/reference/android/content/Context.html
            getContentResolver().insert(uri,cv);
            seq_no=seq_no+1;
        }




    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String[] remote_ports = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};
                String msgToSend = msgs[0];
                //Log.e(TAG, msgs[1]);

                //Multicasting to all 5 avds
                for(int i = 0; i<remote_ports.length; i++){
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),Integer.parseInt(remote_ports[i]));
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF(msgToSend);
                    if(in.readUTF().equals("ACK")){
                        socket.close();
                        in.close();
                        out.close();
                    }
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
            return null;
        }
    }
}
