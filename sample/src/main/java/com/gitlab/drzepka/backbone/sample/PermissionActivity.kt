package com.gitlab.drzepka.backbone.sample

import android.Manifest
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.gitlab.drzepka.backbone.permission.PermissionBundle
import com.gitlab.drzepka.backbone.permission.PermissionManager

/**
 * This activity shows example usage of the [PermissionManager] class.
 */
class PermissionActivity : AppCompatActivity() {

    // Create an instance of permission manager
    private val permissionManager by lazy { PermissionManager(activity = this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        // Add a bundle to permission manager (camera permission is added to the Android manifest)
        permissionManager.addPermissions(PermissionBundles.CAMERA)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // This call must be forwarded to the permission manager
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onRequestClick(view: View) {
        // Request the permissions
        permissionManager.requestPermissions {
            // Check if all bundles' permissions have been granted
            if (permissionManager.allGranted()) {
                // Permissions are granted, you may now proceed...
                Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show()
            } else {
                // Permission denied, display snackbar with information about how to grant the permission
                permissionManager.displaySnackbar(findViewById(android.R.id.content))
            }
        }
    }

    private object PermissionBundles {
        val CAMERA = PermissionBundle("an example rationale", "title", Manifest.permission.CAMERA)
    }
}