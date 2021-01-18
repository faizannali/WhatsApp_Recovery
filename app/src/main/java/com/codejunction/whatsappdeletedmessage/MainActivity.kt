package com.codejunction.whatsappdeletedmessage

import android.Manifest
import android.app.*
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.*
import com.google.android.gms.ads.formats.NativeAdOptions
import com.google.android.gms.ads.formats.UnifiedNativeAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdCallback
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

private const val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0

class MainActivity : AppCompatActivity() {

    private val msgLogFileName = "msgLog.txt"
    private val whatsDeleted = File(
        Environment.getExternalStorageDirectory(),
        "WhatsRecovery${File.separator}WhatsRecovery Images"
    )

    private val checkEmoji = String(Character.toChars(0x2714))
    private val crossEmoji = String(Character.toChars(0x274C))

    private lateinit var rewardedAd: RewardedAd



    override fun onCreate(savedInstanceState: Bundle?) {
        loadAds()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //this is my ad code
//        rewardedAd = RewardedAd(this,
//            "ca-app-pub-3940256099942544/5224354917")

        //loadAds()
        showAds()




//
//        myAd.setOnClickListener {
//            showAds()
//        }



    // Widgets
        val msgLogStatus = findViewById<TextView>(R.id.msg_log_status)
        val imgDirStatus = findViewById<TextView>(R.id.img_dir_status)
        val msgLogClrBtn = findViewById<Button>(R.id.msg_log_clr_btn)
        val imgDirDelBtn = findViewById<Button>(R.id.img_dir_del_btn)
        val medObsSwitch = findViewById<SwitchMaterial>(R.id.med_obs_switch)
        val notificationListenerSwitch = findViewById<SwitchMaterial>(R.id.notification_listener_switch)
        val test = findViewById<RelativeLayout>(R.id.test)

        // TextView
        msgLogStatus.text = getString(
            R.string.msg_log_status_str,
            if (File(this.filesDir, msgLogFileName).exists()) checkEmoji else crossEmoji
        )
        imgDirStatus.text = getString(
            R.string.img_dir_status_str,
            if (whatsDeleted.exists()) checkEmoji else crossEmoji
        )

        // Button
        msgLogClrBtn.setOnClickListener {
            showAds()
            val intent = Intent(this, MsgLogViewerActivity::class.java)
            startActivity(intent)
        }

        imgDirDelBtn.setOnClickListener{
            AlertDialogHelper.showDialog(
                this@MainActivity,
                getString(R.string.del_backup_img),
                getString(R.string.del_backup_img_confirm),
                getString(R.string.yes),
                getString(R.string.cancel),
                DialogInterface.OnClickListener { _, _ ->
                    try {
                        deleteRecursive(whatsDeleted)
                    } catch (e: Exception) {
                        Toast.makeText(
                            applicationContext,
                            getString(R.string.del_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }

        setting.setOnClickListener {
            openSetting()
        }

        // Notification Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            createNotificationChannel(
                "mediaObserver",
                "Media Observer",
                "Watches the default WhatsApp directories for new media",
                NotificationManager.IMPORTANCE_LOW
            )
        } else {
            @Suppress("DEPRECATION")
            createNotificationChannel(
                "mediaObserver",
                "Media Observer",
                "Watches the default WhatsApp directories for new media",
                Notification.PRIORITY_LOW
            )
        }

        // Request Storage Permission
        requestPermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
        )

        // Media Observer Service
        val mediaObserverService = Intent(this, MediaObserverService::class.java)
        startService(mediaObserverService)

        medObsSwitch.isChecked = isServiceRunning(MediaObserverService::class.java)
        medObsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startService(mediaObserverService)
                Toast.makeText(applicationContext, getString(R.string.started), Toast.LENGTH_SHORT).show()
            }
            else {
                stopService(mediaObserverService)
                Toast.makeText(applicationContext, getString(R.string.stopped), Toast.LENGTH_SHORT).show()
            }
        }

        notificationListenerSwitch.isChecked = isServiceRunning(NotificationListener::class.java)
        notificationListenerSwitch.isClickable = false
        test.setOnClickListener {
            if (notificationListenerSwitch.isChecked) {
                AlertDialogHelper.showDialog(
                    this@MainActivity,
                    "Turn off",
                    "Click Setting Button > WhatsApp Recovery > Turn Off",
                    getString(R.string.ok),
                    null,
                    DialogInterface.OnClickListener { dialog, _ -> dialog.cancel() }
                )
            }
            else {
                AlertDialogHelper.showDialog(
                    this@MainActivity,
                    "Turn on",
                    "Click Setting Button > WhatsApp Recovery > Allow",
                    getString(R.string.ok),
                    null,
                    DialogInterface.OnClickListener { dialog, _ -> dialog.cancel() }
                )
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun createNotificationChannel(id: String, name: String, desc: String, importance: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(id, name, importance)
            mChannel.description = desc
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    private fun createBackups() {
        if (!whatsDeleted.exists()) {
            if (!whatsDeleted.mkdirs())
                Toast.makeText(
                    applicationContext, getString(R.string.create_backup_dir_failed),
                    Toast.LENGTH_SHORT
                ).show()
        }
        if (!File(this.filesDir, msgLogFileName).exists()) {
            if (!File(this.filesDir, msgLogFileName).createNewFile())
                Toast.makeText(
                    applicationContext, getString(R.string.create_msg_log_failed),
                    Toast.LENGTH_SHORT
                ).show()
        }
    }

    @Suppress("SameParameterValue")
    private fun requestPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                permission
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(permission), requestCode
            )
        } else {
            createBackups()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.create_backup_dir),
                    Toast.LENGTH_SHORT
                ).show()
                createBackups()
            } else {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.allow_storage_permission_msg),
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }
    }

    @Suppress("DEPRECATION")
    private fun <T> Context.isServiceRunning(service: Class<T>): Boolean {
        return (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == service.name }
    }

    private fun deleteRecursive(f: File) {
        if (f.isDirectory) {
            for (child in f.listFiles()) {
                if (!child.deleteRecursively())
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.unable_to_delete, child.toString()),
                        Toast.LENGTH_SHORT
                    ).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_toggle_theme -> {

                val uri: Uri = Uri.parse("http://instagram.com/faizann_ali/")


                val i = Intent(Intent.ACTION_VIEW, uri)

                i.setPackage("com.instagram.android")

                try {
                    startActivity(i)
                } catch (e: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://instagram.com/faizann_ali")
                        )
                    )
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openSetting(){
        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }

    private fun showAds(){

        if (rewardedAd.isLoaded) {
            val activityContext: Activity = this@MainActivity
            val adCallback = object: RewardedAdCallback() {
                override fun onRewardedAdOpened() {
                    // Ad opened.
                    Toast.makeText(activityContext,"Ads is opened",Toast.LENGTH_LONG).show()
                }
                override fun onRewardedAdClosed() {
                    // Ad closed.
                    Toast.makeText(activityContext,"Ads is Closed",Toast.LENGTH_LONG).show()
                    rewardedAd=loadAds()
                    Log.i("faizan","Ads closed and re-loaded")
                }

                override fun onUserEarnedReward(@NonNull reward: RewardItem) {
                    // User earned reward.
                    Toast.makeText(activityContext,"Reward is given",Toast.LENGTH_LONG).show()
                }
                override fun onRewardedAdFailedToShow(adError: AdError) {
                    // Ad failed to display.
                    Toast.makeText(activityContext,"Failed to show Ads",Toast.LENGTH_LONG).show()
                }
            }
            rewardedAd.show(activityContext, adCallback)
            Log.i("faizan","Ads started")
        }
        else {
            Toast.makeText(this@MainActivity,"Ads is not loaded yet",Toast.LENGTH_LONG).show()
        }

    }

    private fun loadAds(): RewardedAd {
        rewardedAd = RewardedAd(this,
            "ca-app-pub-3940256099942544/5224354917")
        val adLoadCallback = object: RewardedAdLoadCallback() {
            override fun onRewardedAdLoaded() {
                // Ad successfully loaded.
                Toast.makeText(this@MainActivity,"Ads loaded Success",Toast.LENGTH_LONG).show()
            }
            override fun onRewardedAdFailedToLoad(adError: LoadAdError) {
                Toast.makeText(this@MainActivity,"Ads loaded failed",Toast.LENGTH_LONG).show()
            }
        }
        rewardedAd.loadAd(AdRequest.Builder().build(), adLoadCallback)
        Log.i("faizan","Ad loaded")
        return rewardedAd
        Log.i("faizan","Again loaded")
    }

    override fun onPause() {
        super.onPause()
        Log.i("faizan","Activity Pause")
    }

    override fun onResume() {
        super.onResume()
        Log.i("faizan","Activity Resume")
    }

    override fun onRestart() {
        super.onRestart()
        Log.i("faizan","Activity Restarted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("faizan","Activity Destroyed")
    }

    override fun onStart() {
        super.onStart()
        Log.i("faizan","Activity started")
    }


}
