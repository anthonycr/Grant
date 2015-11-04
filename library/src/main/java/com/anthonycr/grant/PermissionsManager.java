package com.anthonycr.grant;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A class to help you manage your permissions simply.
 */
public final class PermissionsManager {

    private static final String TAG = PermissionsManager.class.getSimpleName();
    private static final String[] EMPTY_PERMISSIONS = new String[0];
    private static final PermissionsManager INSTANCE = new PermissionsManager();

    private final Set<String> mPendingRequests = new HashSet<>(1);
    private final List<WeakReference<PermissionsResultAction>> mPendingActions = new ArrayList<>(1);

    public static PermissionsManager getInstance() {
        return INSTANCE;
    }

    private PermissionsManager() {}

    /**
     * This method adds the {@link PermissionsResultAction} to the current list
     * of pending actions that will be completed when the permissions are
     * received. The list of permissions passed to this method are registered
     * in the PermissionsResultAction object so that it will be notified of changes
     * made to these permissions.
     *
     * @param permissions the required permissions for the action to be executed.
     * @param action      the action to add to the current list of pending actions.
     */
    private synchronized void addPendingAction(@NonNull String[] permissions,
                                               @Nullable PermissionsResultAction action) {
        if (action == null) {
            return;
        }
        action.registerPermissions(permissions);
        mPendingActions.add(new WeakReference<>(action));
    }

    /**
     * This static method can be used to check whether or not you have a specific permission.
     * It is basically a less verbose method of using {@link ActivityCompat#checkSelfPermission(Context, String)}
     * and will simply return a boolean whether or not you have the permission. If you pass
     * in a null Context object, it will return false as otherwise it cannot check the permission.
     * However, the Activity parameter is nullable so that you can pass in a reference that you
     * are not always sure will be valid or not (e.g. getActivity() from Fragment).
     *
     * @param context    the Context necessary to check the permission
     * @param permission the permission to check
     * @return true if you have been granted the permission, false otherwise
     */
    @SuppressWarnings("unused")
    public static boolean hasPermission(@Nullable Context context, @NonNull String permission) {
        return context != null && ActivityCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * This static method can be used to check whether or not you have several specific permissions.
     * It is simpler than checking using {@link ActivityCompat#checkSelfPermission(Context, String)}
     * for each permission and will simply return a boolean whether or not you have all the permissions.
     * If you pass in a null Context object, it will return false as otherwise it cannot check the
     * permission. However, the Activity parameter is nullable so that you can pass in a reference
     * that you are not always sure will be valid or not (e.g. getActivity() from Fragment).
     *
     * @param context     the Context necessary to check the permission
     * @param permissions the permissions to check
     * @return true if you have been granted all the permissions, false otherwise
     */
    @SuppressWarnings("unused")
    public static boolean hasAllPermissions(@Nullable Context context, @NonNull String[] permissions) {
        if (context == null) {
            return false;
        }
        boolean hasAllPermissions = true;
        for (String perm : permissions) {
            hasAllPermissions &= ActivityCompat.checkSelfPermission(context, perm)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return hasAllPermissions;
    }

    /**
     * This method will request all the permissions declared in your application manifest
     * for the specified {@link PermissionsResultAction}. The purpose of this method is to enable
     * all permissions to be requested at one shot. The PermissionsResultAction is used to notify
     * you of the user allowing or denying each permission. The Activity and PermissionsResultAction
     * parameters are both annotated Nullable, but this method will not work if the Activity
     * is null. It is only annotated Nullable as a courtesy to prevent crashes in the case
     * that you call this from a Fragment where {@link Fragment#getActivity()} could yield
     * null. Additionally, you will not receive any notification of permissions being granted
     * if you provide a null PermissionsResultAction.
     *
     * @param activity the Activity necessary to request and check permissions.
     * @param action   the PermissionsResultAction used to notify you of permissions being accepted.
     */
    @SuppressWarnings("unused")
    public synchronized void requestAllManifestPermissionsIfNecessary(final @Nullable Activity activity,
                                                                      final @Nullable PermissionsResultAction action) {
        if (activity == null) {
            return;
        }
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                PackageInfo packageInfo = null;
                try {
                    Log.d(TAG, activity.getPackageName());
                    packageInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "A problem occurred when retrieving permissions", e);
                }
                if (packageInfo != null) {
                    String[] permissions = packageInfo.requestedPermissions;
                    if (permissions != null) {
                        for (String perm : permissions) {
                            Log.d(TAG, "Requesting permission if necessary: " + perm);
                        }
                    } else {
                        permissions = EMPTY_PERMISSIONS;
                    }
                    requestPermissionsIfNecessaryForResult(activity, permissions, action);
                }
            }
        });
    }

    /**
     * This method should be used to execute a {@link PermissionsResultAction} for the array
     * of permissions passed to this method. This method will request the permissions if
     * they need to be requested (i.e. we don't have permission yet) and will add the
     * PermissionsResultAction to the queue to be notified of permissions being granted or
     * denied. In the case of pre-Android Marshmallow, permissions will be granted immediately.
     * The Activity variable is nullable, but if it is null, the method will fail to execute.
     * This is only nullable as a courtesy for Fragments where getActivity() may yeild null
     * if the Fragment is not currently added to its parent Activity.
     *
     * @param activity    the activity necessary to request the permissions.
     * @param permissions the list of permissions to request for the {@link PermissionsResultAction}.
     * @param action      the PermissionsResultAction to notify when the permissions are granted or denied.
     */
    @SuppressWarnings("unused")
    public synchronized void requestPermissionsIfNecessaryForResult(@Nullable Activity activity,
                                                                    @NonNull String[] permissions,
                                                                    @Nullable PermissionsResultAction action) {
        if (activity == null) {
            return;
        }
        addPendingAction(permissions, action);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            for (String perm : permissions) {
                if (action != null) {
                    if (ActivityCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED) {
                        action.onResult(perm, PackageManager.PERMISSION_DENIED);
                    } else {
                        action.onResult(perm, PackageManager.PERMISSION_GRANTED);
                    }
                }
            }
        } else {
            List<String> permList = new ArrayList<>(1);
            for (String perm : permissions) {
                if (ActivityCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED) {
                    if (!mPendingRequests.contains(perm)) {
                        permList.add(perm);
                    }
                } else {
                    if (action != null) {
                        action.onResult(perm, PackageManager.PERMISSION_GRANTED);
                    }
                }
            }
            if (!permList.isEmpty()) {
                String[] permsToRequest = permList.toArray(new String[permList.size()]);
                mPendingRequests.addAll(permList);
                ActivityCompat.requestPermissions(activity, permsToRequest, 1);
            }
        }
    }

    /**
     * This method should be used to execute a {@link PermissionsResultAction} for the array
     * of permissions passed to this method. This method will request the permissions if
     * they need to be requested (i.e. we don't have permission yet) and will add the
     * PermissionsResultAction to the queue to be notified of permissions being granted or
     * denied. In the case of pre-Android Marshmallow, permissions will be granted immediately.
     * The Fragment variable is used, but if {@link Fragment#getActivity()} returns null, this method
     * will fail to work as the activity reference is necessary to check for permissions.
     *
     * @param fragment    the fragment necessary to request the permissions.
     * @param permissions the list of permissions to request for the {@link PermissionsResultAction}.
     * @param action      the PermissionsResultAction to notify when the permissions are granted or denied.
     */
    @SuppressWarnings("unused")
    public synchronized void requestPermissionsIfNecessaryForResult(@NonNull Fragment fragment,
                                                                    @NonNull String[] permissions,
                                                                    @Nullable PermissionsResultAction action) {
        Activity activity = fragment.getActivity();
        if (activity == null) {
            return;
        }
        addPendingAction(permissions, action);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            for (String perm : permissions) {
                if (action != null) {
                    if (ActivityCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED) {
                        action.onResult(perm, PackageManager.PERMISSION_DENIED);
                    } else {
                        action.onResult(perm, PackageManager.PERMISSION_GRANTED);
                    }
                }
            }
        } else {
            List<String> permList = new ArrayList<>(1);
            for (String perm : permissions) {
                if (ActivityCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED) {
                    if (!mPendingRequests.contains(perm)) {
                        permList.add(perm);
                    }
                } else {
                    if (action != null) {
                        action.onResult(perm, PackageManager.PERMISSION_GRANTED);
                    }
                }
            }
            if (!permList.isEmpty()) {
                String[] permsToRequest = permList.toArray(new String[permList.size()]);
                mPendingRequests.addAll(permList);
                fragment.requestPermissions(permsToRequest, 1);
            }
        }
    }

    /**
     * This method notifies the PermissionsManager that the permissions have change. If you are making
     * the permissions requests using an Activity, then this method should be called from the
     * Activity callback onRequestPermissionsResult() with the variables passed to that method. If
     * you are passing a Fragment to make the permissions request, then you should call this in
     * the {@link Fragment#onRequestPermissionsResult(int, String[], int[])} method.
     * It will notify all the pending PermissionsResultAction objects currently
     * in the queue, and will remove the permissions request from the list of pending requests.
     *
     * @param permissions the permissions that have changed.
     * @param results     the values for each permission.
     */
    @SuppressWarnings("unused")
    public synchronized void notifyPermissionsChange(@NonNull String[] permissions, @NonNull int[] results) {
        int size = permissions.length;
        if (results.length < size) {
            size = results.length;
        }
        Iterator<WeakReference<PermissionsResultAction>> iterator = mPendingActions.iterator();
        while (iterator.hasNext()) {
            PermissionsResultAction action = iterator.next().get();
            for (int n = 0; n < size; n++) {
                if (action == null || action.onResult(permissions[n], results[n])) {
                    iterator.remove();
                    break;
                }
            }
        }
        for (int n = 0; n < size; n++) {
            mPendingRequests.remove(permissions[n]);
        }
    }

}
