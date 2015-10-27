package com.anthonycr.grant;

import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This abstract class should be used to create an if/else action that the PermissionsManager
 * can execute when the permissions you request are granted or denied. Simple use involves
 * creating an anonymous instance of it and passing that instance to the
 * requestPermissionsIfNecessaryForResult method. The result will be sent back to you as
 * either onGranted (all permissions have been granted), or onDenied (a required permission
 * has been denied). Ideally you put your functionality in the onGranted method and notify
 * the user what won't work in the onDenied method.
 */
public abstract class PermissionsResultAction {

    private final Set<String> mPermissions = new HashSet<>(1);
    private Looper mLooper = Looper.getMainLooper();

    /**
     * Default Constructor
     */
    public PermissionsResultAction() {}

    /**
     * Alternate Constructor. Pass the looper you wish the PermissionsResultAction
     * callbacks to be executed on if it is not the current Looper. For instance,
     * if you are making a permissions request from a background thread but wish the
     * callback to be on the UI thread, use this constructor to specify the UI Looper.
     *
     * @param looper the looper that the callbacks will be called using.
     */
    @SuppressWarnings("unused")
    public PermissionsResultAction(@NonNull Looper looper) {mLooper = looper;}

    public abstract void onGranted();

    public abstract void onDenied(String permission);

    /**
     * This method is called when a particular permission has changed.
     * This method will be called for all permissions, so this method determines
     * if the permission affects the state or not and whether it can proceed with
     * calling onGranted or if onDenied should be called.
     *
     * @param permission the permission that changed.
     * @param result     the result for that permission.
     * @return this method returns true if its primary action has been completed
     * and it should be removed from the data structure holding a reference to it.
     */
    public synchronized final boolean onResult(final @NonNull String permission, int result) {
        if (result == PackageManager.PERMISSION_GRANTED) {
            mPermissions.remove(permission);
            if (mPermissions.isEmpty()) {
                new Handler(mLooper).post(new Runnable() {
                    @Override
                    public void run() {
                        onGranted();
                    }
                });
                return true;
            }
        } else {
            new Handler(mLooper).post(new Runnable() {
                @Override
                public void run() {
                    onDenied(permission);
                }
            });
            return true;
        }
        return false;
    }

    /**
     * This method registers the PermissionsResultAction object for the specified permissions
     * so that it will know which permissions to look for changes to. The PermissionsResultAction
     * will then know to look out for changes to these permissions.
     *
     * @param perms the permissions to listen for
     */
    public synchronized final void registerPermissions(@NonNull String[] perms) {
        Collections.addAll(mPermissions, perms);
    }
}
