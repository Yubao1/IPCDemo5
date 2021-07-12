package com.xe.ipcservice

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.*

/**
 * Created by 86188 on 2021/7/6.
 */
class MyService: Service() {
    private var mIsServiceDestoryed = false
    private val TAG = "MyService"
    override fun onBind(intent: Intent?): IBinder {
        return null!!
    }

    override fun onDestroy() {
        super.onDestroy()
        mIsServiceDestoryed = true
    }

    override fun onCreate() {
        super.onCreate()
        Thread(TcpServer()).start()
    }

    private fun recevi(client: Socket) {
        var inB: BufferedReader? = null
        var out: PrintWriter? = null
        try {
            inB = BufferedReader(InputStreamReader(client.getInputStream()))
            out = PrintWriter(BufferedWriter(OutputStreamWriter(client.getOutputStream())), true)
            var msg: String = ""
            while (!mIsServiceDestoryed) {
                Thread.sleep(50)
                msg = inB!!.readLine()
                if (msg != null) {
                    var s: String = "服务器端收到消息，正准备发送回去------" + msg
                    Log.d(TAG, s)
                    out.println(s)
                } else {
                    Log.d(TAG, "msg == null")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            if (out != null) {
                out.close()
            }
            if (inB != null) {
                try {
                    inB.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
    }

    internal inner class TcpServer : Runnable {
        override fun run() {
            var serverSocket: ServerSocket? = null
            try {
                serverSocket = ServerSocket(8083)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            while (!mIsServiceDestoryed) {
                try {
                    val client = serverSocket!!.accept()
                    recevi(client)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
    }
}