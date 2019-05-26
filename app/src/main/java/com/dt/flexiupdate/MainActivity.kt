package com.dt.flexiupdate

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), InstallStateUpdatedListener {

    override fun onStateUpdate(installState: InstallState) {
        if (installState.installStatus() == InstallStatus.DOWNLOADED) {
            showToast("download complete", Toast.LENGTH_LONG)
            showUpdateSnackbar()
        }
    }

    private lateinit var appUpdateManager: AppUpdateManager

    private val updateAvailable = MutableLiveData<Boolean>().apply { value = false }
    private var updateInfo: AppUpdateInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(this)
        appVersionTextView.text = BuildConfig.VERSION_CODE.toString()

        appUpdateManager.appUpdateInfo.addOnCompleteListener {
            showToast("GET Info complete", Toast.LENGTH_SHORT)
        }

        appUpdateManager.appUpdateInfo.addOnFailureListener {
            showToast("GET Info failed ${it.message}", Toast.LENGTH_LONG)
        }

        checkForUpdate()

        updateAvailable.observe(this, Observer {
            checkUpdateButton.setText(
                if (it) R.string.update_available else R.string.update_not_available
            )
        })

        checkUpdateButton.setOnClickListener {
            if (updateAvailable.value == true) {
                startForInAppUpdate(updateInfo)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener {
            if (it.installStatus() == InstallStatus.DOWNLOADED) {
                showToast("in onResume, download complete", Toast.LENGTH_LONG)
                showUpdateSnackbar()
            } else {
                showToast("${it.installStatus()}", Toast.LENGTH_LONG)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        showToast("request code: $requestCode, result code: $resultCode")
    }

    private fun checkForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener {
            if (it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                it.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                updateInfo = it
                updateAvailable.value = true
                showToast("Version code available ${it.availableVersionCode()}", Toast.LENGTH_LONG)
            } else {
                updateAvailable.value = false
                showToast("Update not available", Toast.LENGTH_LONG)
            }
        }
    }

    private fun startForInAppUpdate(it: AppUpdateInfo?) {
        appUpdateManager.startUpdateFlowForResult(
            it, AppUpdateType.FLEXIBLE, this, REQUEST_CODE_FLEXI_UPDATE
        )
    }

    private fun showUpdateSnackbar() {
        Snackbar
            .make(appVersionTextView, R.string.restart_to_update, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.action_restart) { appUpdateManager.completeUpdate() }
            .show()
    }

    companion object {
        private const val REQUEST_CODE_FLEXI_UPDATE = 17362
    }
}
