# Grant
####Simplifying Android Permissions###
[![Build Status](https://travis-ci.org/anthonycr/Grant.svg)](https://travis-ci.org/anthonycr/Grant)

###Gradle usage
* `compile 'com.anthonycr.grant:permissions:1.0'`
* Available from jcenter

##What can this library do?
* It can request all your declared permissions in a single method and give you a callback when they have been granted.
* It can perform a task at some point in the future that requires a specific permission by checking if the app has the permission and requesting the permission if you do not already have it.
* It keeps your application logic simpler, no need to manage your permissions sensitive code inside the `onRequestPermissionsResult` and figure out what you need to call.
* If you're already convinced of this need for this library, **skip the next section** and go to the "How to use" section, otherwise read the next section and see how confusing permissions can get.

##Why is this library needed?
The Android permissions model has changed in Android Marshmallow. Now you can't count on all the permissions you've declared in your `AndroidManifest` file being granted at run-time. The user has control over granting these permissions to your app and can revoke them at any time, even while your app is running! Because of this, you need to check if you have permission to run certain code before executing it.


The new Android permissions API is fairly simple to understand, but it can introduce unnecessary complexity into the flow of your application. Let's use the the `WRITE_EXTERNAL_STORAGE` permission as an example of this unnecessary complexity. You want to write a String to a file on the external storage of the device.

First, you need to check whether you have permission to access external storage:
```java
Activity activity = this;
String permission = Manifest.Permisions.WRITE_EXTERNAL_STORAGE;
if(ActivityCompat.checkSelfPermission(activity, permission) 
                                     == PackageManager.PERMISSION_GRANTED) {
    // Proceed with your code execution
} else {
    // Uhhh I guess we have to ask for permission
}
```
If you need to check whether you have permission for multiple permissions, then you have to have another condition using the above method. You basically need a bunch of if/else conditions and you need to keep track of what permissions you do and do not have so you know which ones to request.

Oh, and if you already have a permission and you request it again, Android will happily present the user with the request dialog, giving them an opportunity to deny you the permission you already had. Already your code is getting convoluted. Sooooo, once you've handled those cases and know which permissions you want to request, you request permissions using:
```java
// You need a request code so you know where you made the request from
private static final int WRITE_EXTERNAL_STORAGE_TASK_CODE = 1;
.
.
.
Activity activity = this;
String[] permissionsToRequest = new String[] { Manifest.Permisions.WRITE_EXTERNAL_STORAGE };
ActivityCompat.requestPermissions(activity, String[] permissionsToRequest, 
                                  WRITE_EXTERNAL_STORAGE_TASK_CODE);
```
But, you aren't done yet. This doesn't magically return a value, instead you have to execute your code in the following callback. You have to handle the result of those requests in
```java
@Override
public void onRequestPermissionsResult(int requestCode, 
                                       @NonNull String[] permissions, 
                                       @NonNull int[] grantResults) {
	// check array lengths

    // check that the storage permission was granted

    // check that requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE because 
    // maybe the request was made from somewhere else and you shouldn't execute 
    // your code in that case.

    // blah blah execute your code or maybe other code, who knows.
}
```
You need to figure out where the request originated from based on the request code and need to figure out how to respond to the results you get back. You need to compare the results to the permissions, and further muddy up your application logic.

#####THIS IS TOO MESSY FOR SOMETHING SIMPLE

So... this is why we have Grant. Using Grant, you wrap your code in an anonymous `PermissionsResultAction` class, and send that and an array of the permissions that are required to run your code to the `PermissionsManager`, which handles all the request logic and executes your code when all the permissions have been granted by the user. This way, you don't have to break and muddy up your application logic.

##How to Use

####The Main Classes
* `PermissionsManager`: This is a singleton class that handles all the permissions requests, logic, and callbacks.
* `PermissionsResultAction`: This is a callback that you use to execute code when the permissions you have requested are either granted or denied.

####The How-To
An Activity reference is needed to request permissions. All you need to add to your Activities is a single line of code that notifies the `PermissionsManager` of the `Activity` callback. For API 23 and above, you should override the `Activity` callback `onRequestedPermissionsResult` in each of your Activities from which you make permissions requests. You should then add the following line of code to it so that the `PermissionsManager` can receive all the changes in the permissions being granted to your application:

```java
@Override
public void onRequestPermissionsResult(int requestCode, 
                                       @NonNull String[] permissions, 
                                       @NonNull int[] grantResults) {
    PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
}
```

This will trigger the `PermissionsManager` to notify all actions that you have sent to it so they can run.

####General Use Case

Then, in your code, when you need to execute code that requires permissions, instead of checking and requesting and handling the `Activity` callback manually, you can just wrap your code in an anonymous class and send it to the permissions manager like follows. For this example, we pretend we have a method `writeToStorage()` that requires the `WRITE_EXTERNAL_STORAGE` permission to function:

```java
PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this,
    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, new PermissionsResultAction() {

    @Override
    public void onGranted() {
        writeToStorage();
    }

    @Override
    public void onDenied(String permission) {
        Toast.makeText(MainActivity.this, 
                      "Sorry, we need the Storage Permission to do that", 
                      Toast.LENGTH_SHORT).show();
    }
});
```
#####BOOM. DONE. That's it. No messing around with weird logic and result codes.

You can add any number of permissions to a request, and the `PermissionsResultAction.onGranted()` will not be triggered until all the permissions have been granted by the user. If a permission is denied by the user, the `onDenied(String)` will be called and the `PermissionsResultAction` will be removed from the queue and will no longer listen for permissions changes.

####Global Use Case

But, maybe in addition to this, you just want to get it out of the way and request all the permissions up front so that you don't need to break the user flow later. In that case, you can just use a simple method which reads the permissions you want from your manifest and requests them for you like follows:

```java
PermissionsManager.getInstance().requestAllManifestPermissionsIfNecessary(this, 
                                                      new PermissionsResultAction() {
    @Override
    public void onGranted() {
      // Proceed with initialization
    }

    @Override
    public void onDenied(String permission) {
      // Notify the user that you need all of the permissions
    }
});
```

Note: you will still need to handle the permission checks before executing your code as the user could deny the permission from device settings while your app is running.

####Other Use Cases

Additionally, the library contains a couple methods to check whether you have permission for one or several permissions that is slightly less verbose than using one of the compat libaries. Particularly `boolean hasAllPermissions(Context context, String[] permissions)` is helpful when checking if you have all of several permissions quickly. An example use case would be if you requested all permissions when you first start the app, but the user denied some of those permissions, so elsewhere in your app you want to know if you have all the permissions necessary for an action or not so you can display a different UI element.

And that's it! Contributions and suggestions are welcome. Check out the sample application on how the library can be used correctly in actual code if these examples didn't make sense.

Got questions? Hit me up on [twitter](twitter.com/RestainoAnthony).

##License
Copyright 2015 Anthony Restaino

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
