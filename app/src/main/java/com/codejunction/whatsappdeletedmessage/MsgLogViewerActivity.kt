package com.codejunction.whatsappdeletedmessage

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdCallback
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.android.synthetic.main.activity_msg_log_viewer.*
import java.io.File
import java.io.PrintWriter

class MsgLogViewerActivity : AppCompatActivity() {

    private val msgLogFileName = "msgLog.txt"
    private lateinit var rewardedAd: RewardedAd

    override fun onCreate(savedInstanceState: Bundle?) {
        loadAds()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_msg_log_viewer)

        msg_log_recycler_view.adapter = MsgLogAdapter(readFile(File(this.filesDir, msgLogFileName)))

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        msg_log_recycler_view.layoutManager=layoutManager
        msg_log_recycler_view.setHasFixedSize(true)

        swipe_refresh_layout.setOnRefreshListener {
            refreshMsgLog()
            swipe_refresh_layout.isRefreshing = false
        }
    }

    private fun readFile(fileName: File): List<String> = fileName.bufferedReader().readLines().asReversed()

    private fun refreshMsgLog() {
        msg_log_recycler_view.adapter = MsgLogAdapter(readFile(File(this.filesDir, msgLogFileName)))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_msg_log_viewer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_refresh -> {
                refreshMsgLog()
                return true
            }
            R.id.action_clear -> {
                AlertDialogHelper.showDialog(
                    this@MsgLogViewerActivity,
                    getString(R.string.clear_msg_log),
                    getString(R.string.clear_msg_log_confirm),
                    getString(R.string.yes),
                    getString(R.string.cancel),
                    DialogInterface.OnClickListener { _, _ ->
                        try {
                            PrintWriter(File(this.filesDir, msgLogFileName)).use { out ->
                                out.println(
                                    ""
                                )
                            }
                            Toast.makeText(
                                applicationContext,
                                getString(R.string.cleared),
                                Toast.LENGTH_SHORT
                            ).show()
                            refreshMsgLog()
                        } catch (e: Exception) {
                            Toast.makeText(
                                applicationContext,
                                getString(R.string.clear_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadAds(): RewardedAd {
        rewardedAd = RewardedAd(this,
            "ca-app-pub-3940256099942544/5224354917")
        val adLoadCallback = object: RewardedAdLoadCallback() {
            override fun onRewardedAdLoaded() {
                // Ad successfully loaded.
                Toast.makeText(this@MsgLogViewerActivity,"Ads loaded Success",Toast.LENGTH_LONG).show()
            }
            override fun onRewardedAdFailedToLoad(adError: LoadAdError) {
                Toast.makeText(this@MsgLogViewerActivity,"Ads loaded failed",Toast.LENGTH_LONG).show()
            }
        }
        rewardedAd.loadAd(AdRequest.Builder().build(), adLoadCallback)
        Log.i("faizan","Ad loaded")
        return rewardedAd
        Log.i("faizan","Again loaded")
    }

    private fun showAds(){

        if (rewardedAd.isLoaded) {
            val activityContext: Activity = this@MsgLogViewerActivity
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
            Toast.makeText(this@MsgLogViewerActivity,"Ads is not loaded yet",Toast.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        showAds()
    }
}