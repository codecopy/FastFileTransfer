package vis.net;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vis.DevicesList;
import vis.FileOperations;
import vis.SelectedFilesQueue;
import vis.UserDevice;
import vis.UserFile;

/**
 * 文件传输类
 * Created by Vision on 15/6/16.<br>
 * Email:Vision.lsm.2012@gmail.com
 */
public class FilesTransfer {

    public static final int SERVICE_SHARE = 1;
    public static final int SERVICE_RECEIVE = 2;


    public static final int DEIVCE_INVALID = 0;
    public static final int DEIVCE_VALID = 1;

    private final ExecutorService executorService;
    private ServerSocket mServerSocket;
    private Socket mSocket;
    /**
     * 是否在接收模式
     */
    private boolean isReceiving = false;
    private Context context;
    private Handler mHandler;
    private Message msg;
    private DevicesList<UserDevice> mDevicesList;

    public FilesTransfer(Context context, int serviceType) {
        this.context = context;
        if (SERVICE_SHARE == serviceType) {
            executorService = Executors.newFixedThreadPool(3);
        } else if (SERVICE_RECEIVE == serviceType) {
            executorService = Executors.newSingleThreadExecutor();
        } else {
            executorService = null;
        }
    }

    public void setCallbackHandler(Handler handler) {
        this.mHandler = handler;
    }

    /**
     * 发送文件，最多可同时发往3个地址
     *
     * @param devicesList        用户对象
     * @param selectedFilesQueue 要发送的文件
     */
//    public void sendFile(int index, SelectedFilesQueue<UserFile> files, UserDevice ud) {
//        executorService.execute(new Sender(index, files, ud));
//    }
    public void sendFile(DevicesList<UserDevice> devicesList, SelectedFilesQueue<UserFile> selectedFilesQueue) {
        for (int i = 0, nsize = devicesList.size(); i < nsize; i++) {
            UserDevice ud = (UserDevice) devicesList.valueAt(i);
            if (ud.state != UserDevice.TRANSFER_STATE_TRANSFERRING) {
                ud.state = UserDevice.TRANSFER_STATE_TRANSFERRING;
                executorService.execute(new Sender(ud, selectedFilesQueue));
//                sendFile(i, selectedFilesQueue, ud);
//                Log.d(this.getClass().getName(), ud.ip + ":2333->" + files.toString());
            }
        }
    }

//
//    public void sendFile(File[] files, DevicesList<UserDevice> devicesList) {
//        mDevicesList = devicesList;
//        for (int i = 0, nsize = mDevicesList.size(); i < nsize; i++) {
//            UserDevice ud = (UserDevice) mDevicesList.valueAt(i);
//            if (ud.state != UserDevice.TRANSFER_STATE_TRANSFERRING) {
//                ud.state = UserDevice.TRANSFER_STATE_TRANSFERRING;
//                sendFile(i, files, ud);
//                Log.d(this.getClass().getName(), ud.ip + ":2333->" + files.toString());
//            }
//        }
//    }

    /**
     * 接收文件
     *
     * @param dirName 文件存放位置，文件夹名
     */
    public void receiveFile(int port, String dirName) {
        File dir = new File(Environment.getExternalStorageDirectory().getPath() + dirName);
        executorService.execute(new Receiver(port, dir));
    }

    public boolean isReceiving() {
        return this.isReceiving;
    }

    public void stopReceiving() {
        this.isReceiving = false;
        try {
            if (mServerSocket != null) {
                mServerSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        executorService.shutdown();
    }

    class Receiver implements Runnable {
        private DataInputStream din = null;
        private FileOutputStream fout;
        private int length = 0;
        private byte[] inputByte = null;
        private int port;
        private File dir;
        private UserFile userFile;
        /**
         * 本次接收的文件序号
         */
        private int index;

        public Receiver(int port, File dir) {
            this.port = port;
            this.dir = dir;
        }

        @Override
        public void run() {
            isReceiving = true;
            inputByte = new byte[1024];
            try {
                mServerSocket = new ServerSocket(port);
//                mServerSocket.setSoTimeout(2000);
                while (isReceiving) {
                    try {
                        Log.d(this.getClass().getName(), "accepting the connect");
                        mSocket = mServerSocket.accept();
//                        mSocket.setSoTimeout(2000);
                        Log.d(this.getClass().getName(), "start translate");
                        din = new DataInputStream(mSocket.getInputStream());
                        userFile = new UserFile();
                        userFile.name = din.readUTF();
                        String type = FileOperations.getMIMEType(userFile.name);
                        type = type.substring(0, type.indexOf('/'));
                        if (type.equals("image")) {
                            userFile.type = UserFile.TYPE_IMAGE;
                        } else if (type.equals("video")) {
                            userFile.type = UserFile.TYPE_VIDEO;
                        } else if (type.equals("application")) {
                            userFile.type = UserFile.TYPE_APP;
                        } else if (type.equals("audio")) {
                            userFile.type = UserFile.TYPE_AUDIO;
                        } else if (type.equals("text")) {
                            userFile.type = UserFile.TYPE_TEXT;
                        } else {
                            userFile.type = UserFile.TYPE_UNKNOWN;
                        }
                        File file;
                        int i = 1;
                        while ((file = new File(dir.getPath() + "/" + userFile.name)).exists()) {
                            userFile.name = replaceLast(userFile.name, "(\\(\\d*\\))?\\.", "(" + String.valueOf(i++) + ").");
                        }
                        Log.d("isExists", file.getPath());
                        fout = new FileOutputStream(file);
                        userFile.size = din.readLong();
                        userFile.state = UserFile.TRANSFER_STATE_TRANSFERRING;
                        userFile.id = index;
                        while (true) {
                            if (din != null) {
                                length = din.read(inputByte, 0, inputByte.length);
                            }
                            if (length == -1) {
                                break;
                            }
                            fout.write(inputByte, 0, length);
                            fout.flush();
                            userFile.completed += length;
                            if (userFile.completed == userFile.size) {
                                userFile.state = UserFile.TRANSFER_STATE_FINISH;
                            }
                            msg = Message.obtain();
                            msg.obj = userFile;
                            mHandler.sendMessage(msg);
                        }
                        index++;
                        Log.d(this.getClass().getName(), "finish translate");
                    } catch (SocketTimeoutException e) {
                        Log.d("Exception", "SocketTimeoutException");
                    } catch (SocketException e) {
                        Log.d("Exception", "SocketException");
                    }
                }
                if (din != null)
                    din.close();
                if (fout != null)
                    fout.close();
                if (mSocket != null)
                    mSocket.close();
                Log.d(this.getClass().getName(), "end all thing");
            } catch (IOException e) {
                Log.d("Exception", "IOException");
            }
        }
    }

    /**
     * lang
     *
     * @param text        源文本
     * @param regex       正则匹配
     * @param replacement 匹配替换
     * @return 替换结果
     */
    public static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)" + regex + "(?!.*?" + regex + ")", replacement);
    }

    class Sender implements Runnable {
        private UserDevice ud;
        private SelectedFilesQueue<UserFile> mSelectedFilesQueue;
        private int length = 0;
        private byte[] sendByte = null;
        private Socket socket = null;
        private DataOutputStream dout = null;
        private FileInputStream fin = null;

//        private File[] files;

        private long sendLength;
        private int index;
        private int completionPercentage;

        /**
         * @param selectedFilesQueue 要发送的文件
         * @param ud                 用户对象
         */
//        public Sender(int index, File[] files, UserDevice ud) {
//            this.index = index;
//            this.files = files;
//        }
        public Sender(UserDevice ud, SelectedFilesQueue<UserFile> selectedFilesQueue) {
            this.ud = ud;
            mSelectedFilesQueue = selectedFilesQueue;
        }

        @Override
        public void run() {
            ud.fileTotal = mSelectedFilesQueue.size();
            ud.currentFile = 0;
            for (UserFile uf : mSelectedFilesQueue.data) {
                ud.currentFile++;
                ud.currentFileName = uf.name;
                sendLength = completionPercentage = 0;
//                Log.d(this.getClass().getName(), "start send files :" + file.length());
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(ud.ip, ud.port), 1000);
                    dout = new DataOutputStream(socket.getOutputStream());
                    File file = new File(uf.data);
                    fin = new FileInputStream(file);
                    sendByte = new byte[1024];
                    if (UserFile.TYPE_APP == uf.type) {
                        dout.writeUTF(uf.name + ".apk");
                    } else {
                        dout.writeUTF(uf.name);
                    }
                    dout.writeLong(file.length());
                    while ((length = fin.read(sendByte, 0, sendByte.length)) > 0) {
                        dout.write(sendByte, 0, length);
                        dout.flush();
                        sendLength += length;
                        int transferred = (int) (sendLength * 100 / file.length());
                        if (completionPercentage < transferred) {       //减少发送message
//                        Log.d("completed", String.valueOf(completionPercentage));
                            completionPercentage = transferred;
                            msg = Message.obtain();
                            msg.what = DEIVCE_VALID;
                            ud.completed = completionPercentage;
                            if (100 > completionPercentage) {
                                ud.state = UserDevice.TRANSFER_STATE_TRANSFERRING;
                            } else {
                                ud.state = UserDevice.TRANSFER_STATE_FINISH;
                            }
                            mHandler.sendMessage(msg);
                        }
//                    Log.d("sendLength:", String.valueOf(sendLength));
                    }
                } catch (IOException e) {
                    Log.d("", e.getMessage() + "这个目标设备有问题了");
                    msg = Message.obtain();
                    msg.what = DEIVCE_INVALID;
                    msg.arg1 = ud.ipInt;
                    mHandler.sendMessage(msg);
                } finally {
                    Log.d(this.getClass().getName(), "end send");
                    try {
                        if (dout != null)
                            dout.close();
                        if (fin != null)
                            fin.close();
                        if (socket != null)
                            socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
