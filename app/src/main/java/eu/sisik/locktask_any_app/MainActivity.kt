package eu.sisik.locktask_any_app

import android.app.ActivityOptions
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val packageList = ArrayList<PackageInfo>()

    private lateinit var adapter: PackageAdapter

    private val adminComponentName: ComponentName by lazy {
        ComponentName(this, DevAdminReceiver::class.java)
    }

    private val devicePolicyManager: DevicePolicyManager by lazy {
        getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
    }

    override fun onResume() {
        super.onResume()
        updateDeviceOwnerStatus()
    }

    private fun updateDeviceOwnerStatus() {
        if (!devicePolicyManager.isDeviceOwnerApp(packageName)) {
            supportActionBar?.title = "Device Owner not activated!"
        } else {
            supportActionBar?.title = "Device Owner active ✓"
        }
    }

    private fun initViews() {
        val tvPackageName = findViewById<TextView>(R.id.tvPackageName)
        val rvPackageList = findViewById<RecyclerView>(R.id.rvPackageList)
        val butStartLockTask = findViewById<Button>(R.id.butStartLockTask)
        val butClearDeviceOwner = findViewById<Button>(R.id.butClearDeviceOwner)

        adapter = PackageAdapter(packageList) {
            tvPackageName.text = it.packageName
        }

        rvPackageList.layoutManager = LinearLayoutManager(this)
        rvPackageList.adapter = adapter
        refreshPackageList()

        butStartLockTask.setOnClickListener {
            val pn = tvPackageName.text.toString()
            if (packageNameValid(pn)) {
                startLockTask(pn)
            } else {
                Toast.makeText(this, "Select a valid package name first", Toast.LENGTH_SHORT).show()
            }
        }

        butClearDeviceOwner.setOnClickListener {
            val pn = tvPackageName.text.toString()
            if (packageNameValid(pn)) {
                if (devicePolicyManager.isDeviceOwnerApp(pn)) {
                    devicePolicyManager.clearDeviceOwnerApp(pn)
                } else {
                    Toast.makeText(this, "Selected package is not device owner", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Toast.makeText(this, "Select a valid package name first", Toast.LENGTH_SHORT).show()
            }
            updateDeviceOwnerStatus()
        }
    }

    private fun refreshPackageList() {
        val pi = packageManager.getInstalledPackages(0)
        val sortedPi = pi.sortedBy { i -> i.packageName }
        pi.forEach { packageInfo ->
            packageInfo.packageName
        }
        packageList.clear()
        packageList.addAll(sortedPi)

        adapter.notifyDataSetChanged()
    }

    private fun packageNameValid(packageName: String): Boolean {
        try {
            packageManager.getPackageInfo(packageName, 0)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            // ignore
        }

        return false
    }

    private fun startLockTask(forPackageName: String) {
        // Whitelist package, so that it can start in Lock Task mode
        devicePolicyManager.setLockTaskPackages(adminComponentName, arrayOf(forPackageName))
        devicePolicyManager.setLockTaskFeatures(
            adminComponentName,
            DevicePolicyManager.LOCK_TASK_FEATURE_NONE
        )

        // Start package in Lock Task mode
        val options = ActivityOptions.makeBasic()
        options.setLockTaskEnabled(true)


        val i = packageManager.getLaunchIntentForPackage(forPackageName)
        startActivity(i, options.toBundle())
    }
}
