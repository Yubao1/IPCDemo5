package com.xe.ipcdemo5

import android.content.Intent
import android.nfc.Tag
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.text.format.DateUtils.formatDateTime
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.xe.ipcservice.MyService
import java.io.*
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by 86188 on 2021/7/6.
 */
class ClientActivity : AppCompatActivity() {
    var mBtnConnect: Button? = null
    var mTvMessage: TextView? = null
    var mBtnSend: Button? = null
    var mH: Handler? = null
    var mReceiveThread: Thread? = null
    var mPrintWriter: PrintWriter? = null
    var mClientSocket: Socket? = null
    var isThreadActive: Boolean = true
    var mDefinedMessages = arrayOf("你好！", "请问你叫什么名字", "今天天气不错", "给你讲个笑话吧", "这个可以多人聊天")
    companion object {
        var MESSAGE_SOCKET_CONNECTED: Int = 1
        var UPDATE_VIEW: Int = 2
        var TAG: String = "ClientActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)
        init();
        startMyService()
    }

    fun startMyService() {
        startService(Intent(this, MyService::class.java))
    }

    fun init() {
        mBtnConnect = findViewById(R.id.btn_connect)
        mTvMessage = findViewById(R.id.tv_message)
        mBtnSend = findViewById(R.id.btn_send);
        mH = MyHandler();
        mReceiveThread = ReceiveThread();
    }

    fun onClick(v: View) {
        if (v.id == R.id.btn_connect) {
            connect(v)
        } else if (v.id == R.id.btn_send) {
            sendMessage(v)
        }
    }

    fun sendMessage(v: View) {
        mBtnSend!!.isEnabled = false
        var t: Thread = SendThread()
        t.start()

    }

    inner class SendThread : Thread() {
        override fun run() {
            super.run()
            try {
                var index: Int = Random().nextInt(mDefinedMessages.size)
                if (mPrintWriter != null) {
                    mPrintWriter!!.println(mDefinedMessages[index])
                } else {
                    Log.d(TAG,"mPrintWriter == null")
                }
            } catch (e: Exception) {

            } finally {
                mH!!.sendEmptyMessage(UPDATE_VIEW)
            }
        }
    }

    fun connect(v: View) {
        mReceiveThread!!.start()
        v.isEnabled = false
    }

    inner class MyHandler : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            if (msg!!.what == MESSAGE_SOCKET_CONNECTED) {
                var message: String = msg!!.obj as String
                var mTvContent: String = mTvMessage!!.text.toString()
                mTvContent = mTvContent + "\n" + message
                mTvMessage!!.setText(mTvContent)
            } else if (msg!!.what == UPDATE_VIEW){
                mBtnSend!!.isEnabled = true
            }
        }
    }

    inner class ReceiveThread : Thread() {
        override fun run() {
            super.run()
            var socket: Socket? = null
            while (socket == null) {
                try {
                    socket = Socket("127.0.0.1", 8083);
                    mClientSocket = socket;
                    mPrintWriter = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true);
                } catch (e: IOException) {
                    e.printStackTrace();
                }
            }
            var br: BufferedReader? = null
            try {
                br = BufferedReader(InputStreamReader(socket.getInputStream()));
                while (isThreadActive) {
                    var msg = br!!.readLine()
                    Thread.sleep(500);
                    if (msg != null) {
                        var message: Message = Message.obtain();
                        message.what = MESSAGE_SOCKET_CONNECTED
                        message.obj = msg
                        mH!!.sendMessage(message);
                    }
                }

            } catch (e: IOException) {
                e.printStackTrace();
            } catch (e: InterruptedException) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (e: IOException) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                socket.close();
            } catch (e: IOException) {
                e.printStackTrace();
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isThreadActive = false;
        mReceiveThread = null
        if (mPrintWriter != null) {
            mPrintWriter!!.close()
            mPrintWriter = null
        }
        if (mClientSocket != null) {
            try {
                mClientSocket!!.shutdownInput()
                mClientSocket!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }
}