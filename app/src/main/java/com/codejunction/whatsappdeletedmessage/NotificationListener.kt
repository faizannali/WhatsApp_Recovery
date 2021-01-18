package com.codejunction.whatsappdeletedmessage

import android.content.Context
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.io.File
import java.text.DateFormat
import java.util.*

class NotificationListener : NotificationListenerService() {

    lateinit var prefrences:SharedPreferences

    override fun onNotificationPosted(sbn: StatusBarNotification?) {

        if (sbn?.packageName == "com.whatsapp") {

            val date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date())
            val sender = sbn.notification.extras.getString("android.title")
            val msg = sbn.notification.extras.getString("android.text")

            File(this.filesDir, "msgLog.txt").appendText("$date | $sender: $msg\n")

//            prefrences=getSharedPreferences("My_Pref",Context.MODE_PRIVATE)
//            var editor = prefrences.edit()
//            editor.putString("sender",sender)
//            editor.putString("msg",msg)
//            editor.apply()
//            editor.commit()
        }
    }

}
