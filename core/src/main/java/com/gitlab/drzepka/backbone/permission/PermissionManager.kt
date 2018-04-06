@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.gitlab.drzepka.backbone.permission

import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat.requestPermissions
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.ViewGroup
import com.gitlab.drzepka.backbone.R
import java.util.*
import kotlin.collections.ArrayList

/**
 * This class provides a simple interface to manage device's **runtime** permissions (SDK >= Marshmallow). On lower APIs
 * this manager behaves like all permission have been granted.
 *
 * Permission manager operates on bundles. For more information see the [PermissionBundle] class.
 *
 * Usage:
 * 1. Create new instance of PermissionManager. In constructor arguments pass an activity or fragment instance,
 * depending on from where you will be calling the manager instance (passing both instances at once is not allowed).
 * 2. Add new permission bundles with the [addPermissions] method.
 * 3. Run [requestPermissions] method.
 * 4. After all requests are done, the lambda function you supplied into the method in step *3* will fire.
 * 5. Now you can check if a specific permission is granted with the [allGranted] method.
 *
 * **NOTE:** don't forget to hooke the [onRequestPermissionsResult] method in order to this class to work.
 */
class PermissionManager(val activity: Activity? = null, val fragment: Fragment? = null) {

    companion object {
        /** Request code used in the [displaySnackbar] method. */
        const val ACTIVITY_REQUEST = 100
        /** Id of the rationale title string. */
        const val STRING_RATIONALE_TITLE = 0
        /** Id of the rationale suffix string. */
        const val STRING_RATIONALE_SUFFIX = 1

        private const val TAG = "PermissinManager"
        private val STRINGS = ArrayList<Int>()

        init {
            // STRING_RATIONALE_TITLE
            STRINGS.add(R.string.permission_manager_rationale_title)
            // STRING_RATIONALE_SUFFIX
            STRINGS.add(R.string.permission_manager_rationale_suffix)
        }
    }

    private var initialBundles = ArrayList<PermissionBundle>()
    private var permissions = ArrayList<PermissionBundle>()
    private var requestId: Int = 0
    private var resultListener: () -> Unit = {}

    private var rationaleQueue = PriorityQueue<PermissionBundle>()
    private var insideListener = false

    /**
     * Adds a new [PermissionBundle]s and optionally clears old entries.
     */
    fun addPermissions(vararg permissions: PermissionBundle, clearOld: Boolean = false) {
        if (clearOld)
            initialBundles.clear()
        initialBundles.addAll(permissions)
    }

    /**
     * Creates a permission request. After the request is finished, you should the [allGranted] method in order to know
     * whether specific permission has been granted. This should be done in handler, passed as a parameter.
     *
     * In order to this method to work, you must forward the
     * [Activity.onRequestPermissionsResult] or [Fragment.onRequestPermissionsResult] call here.
     * @param [onResult] function that will be executed when request is finished.
     */
    fun requestPermissions(onResult: () -> Unit) {
        permissions = ArrayList(initialBundles)
        doRequestPermissions(onResult)
    }

    /**
     * Returns whether all permissions are granted. Passing bundles will run the check only on permissions defined there.
     */
    fun allGranted(vararg bundles: PermissionBundle): Boolean {
        val context: Context = activity ?: fragment?.context!!
        val subject = if (bundles.isNotEmpty()) bundles else initialBundles.toTypedArray()

        return subject.flatMap { it.permissions.asList() }.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check whether specified permission is denied permanently - it cannot be requested again. On APIs lower than
     * Marshmallow always returns `true`. When there are multiple permissions defined in single bundle, returns `false`
     * if at least permission is denied permanently.
     */
    fun isDeniedPermanently(bundle: PermissionBundle): Boolean {
        // Rationale should be shown when at least one permission from the bundle is denied.
        val shouldShow = bundle.permissions.any { shouldShowRationale(it) }
        return !allGranted(bundle) && !shouldShow
    }

    /**
     * Displays the snackbar requesting for granting permissions **if they were rejected in the first place**. You should
     * check if permissions were rejected first, because this method doesn't do that. You should also call the
     * [requestPermissions] method if activity result code is equal to [ACTIVITY_REQUEST] - that means user returned
     * from settings and might grant required permissions.
     *
     * **Note:** this method checks if system is a Marshmallow or newer. If isn't, only displayes a critical error
     * (permissions should be always granted on lower Android version).
     *
     * **Call this method only in *onResult* listener - see [doRequestPermissions] method.**
     */
    @TargetApi(Build.VERSION_CODES.M)
    fun displaySnackbar(parent: ViewGroup) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Snackbar.make(parent, R.string.permission_manager_request_error_m, Snackbar.LENGTH_LONG).show()
            return
        }

        checkIfInsideListener()
        var message = getString(R.string.permission_manager_request_prefix) + " "
        val button: Int
        val permanent = initialBundles.any { isDeniedPermanently(it) }
        if (permanent) {
            message += getString(R.string.permission_manager_request_settings)
            button = R.string.permission_manager_request_settings_button
        } else {
            message += getString(R.string.permission_manager_request_normal)
            button = R.string.permission_manager_request_normal_button
        }

        val snackBar = Snackbar.make(parent, message, Snackbar.LENGTH_LONG)
        snackBar.setAction(button, {
            if (permanent) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", activity?.packageName ?: fragment!!.activity!!.packageName, null)
                activity?.startActivityForResult(intent, ACTIVITY_REQUEST)
                fragment?.startActivityForResult(intent, ACTIVITY_REQUEST)
            } else
                requestPermissions(resultListener)
        })
        snackBar.show()
    }

    /**
     * You must call this method from your activity's or fragment's method with the same name, forwarding all arguments
     * in order for this manager to work.
     */
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode != requestId) return
        requestId = 0

        this.permissions.clear()
        (0 until grantResults.size)
                .filter { grantResults[it] == PackageManager.PERMISSION_DENIED && shouldShowRationale(permissions[it]) }
                .forEach {
                    val item = initialBundles.firstOrNull { bundle -> bundle.permissions.contains(permissions[it]) }
                    if (item != null && !rationaleQueue.contains(item))
                        rationaleQueue.add(item)
                }

        displayRationales()
    }

    /**
     * Updates strings displayed by permission manager.
     * @param [id] one of the STRING_* fields
     * @param [value] new value
     */
    fun setString(id: Int, @StringRes value: Int) {
        if (id < STRINGS.size)
            STRINGS[id] = value
    }

    private fun shouldShowRationale(permission: String): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && (activity?.shouldShowRequestPermissionRationale(permission)
                ?: fragment!!.shouldShowRequestPermissionRationale(permission))
    }

    private fun checkIfInsideListener() {
        if (!insideListener)
            throw IllegalStateException("This method cannot be called from outside of the onResult listener")
    }

    private fun getString(@StringRes res: Int) = activity?.getString(res) ?: fragment!!.getString(res)

    private fun displayRationales() {
        if (rationaleQueue.size == 0) {
            if (permissions.size > 0) doRequestPermissions(resultListener)
            else {
                insideListener = true
                resultListener()
                insideListener = false
            }

            return
        }

        // Pop bundles until a rationale can be displayed
        var permission: PermissionBundle
        do {
            if (rationaleQueue.size == 0) {
                displayRationales()
                return
            }
            permission = rationaleQueue.poll()
        } while (!permission.shouldDisplayRationale)

        // Should there be a permission name in the title?
        val title = permission.rationaleTitleStr
                ?: getString(permission.rationaleTitle ?: STRINGS[STRING_RATIONALE_TITLE])
        val message = (permission.rationaleStr
                ?: getString(permission.rationale!!)) + ' ' + getString(STRINGS[STRING_RATIONALE_SUFFIX])

        AlertDialog.Builder(activity ?: fragment!!.context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.permission_manager_rationale_retry, { dialog, _ ->
                    permissions.add(permission)
                    dialog.dismiss()
                    displayRationales()
                })
                .setNegativeButton(R.string.permission_manager_rationale_sure, { dialog, _ ->
                    dialog.dismiss()
                    displayRationales()
                })
                .show()
    }

    private fun doRequestPermissions(onResult: () -> Unit) {
        if (requestId != 0) {
            // only one request is allowed at a time
            Log.w(TAG, "Another permission request is pending, ignoring this one.")
            return
        }

        if (!allGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // dynamic permissions work only on SDK >= Marshmallow
            requestId = Random().nextInt(16384)
            resultListener = onResult
            val permissionArray = permissions.flatMap { it.permissions.toList() }.toTypedArray()

            activity?.requestPermissions(permissionArray, requestId)
            fragment?.requestPermissions(permissionArray, requestId)
        } else
            onResult()
    }
}