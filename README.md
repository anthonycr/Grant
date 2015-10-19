# Grant
####Simplifying Android Permissions###

##Why is this library needed?
The Android permissions model has changed in Android Marshmallow. Now you can't count on all the permissions you've declared in your AndroidManifest file being granted at runtime. The user has control over granting these permissions to your app and can revoke them at any time, even when your app is running!

The new Android permissions API is simple, but it can introduce complexity into the flow of your application. Let's use the the `WRITE_EXTERNAL_STORAGE` permission as an example.

First, you need to check whether you have permission:
```java
ActivityCompat.checkSelfPermission(Activity activity, String permission)
```
You need to compare this to `PackageManager.PERMISSION_GRANTED`. If you need to check if you have permission for multiple permissions, then you have to have another condition using the above method. Already your code is getting convoluted. You also need to keep track of what permissions you don't have so you can ask for only those. If you already have one out of three permissions, you don't want to ask the user for that permission again otherwise they could change their mind and revoke your permission. Sooooo, once you've handled that, you request permissions using:
```java
ActivityCompat.requestPermissions(Activity activity, 
                                  String[] permissionsToRequest, 
                                  int REQUEST_CODE);
```
But, you aren't done yet. Then, you have to handle the result of those requests in
```java
@Override
public void onRequestPermissionsResult(int requestCode, 
                                       @NonNull String[] permissions, 
                                       @NonNull int[] grantResults) {
    // blah blah do something
}
```
You need to figure out where the request originated from based on the request code and need to figure out how to respond to the results you get back. You need to compare the results to the permissions, and further muddy up your application logic.

#####THIS IS TOO MESSY FOR SOMETHING SIMPLE

So... this is why we have Grant. Using Grant, you wrap your code in an anonymous PermissionsResultAction class, and send that and an array of the permissions that are required to run your code to the PermissionsManager, which handles all the request logic and executes your code when all the permissions have been granted by the user. This way, you don't have to break your application logic and can live a simpler life.

##How to Use

* `PermissionsManager`: This is a singleton class that handles the permissions requests and callbacks
* `PermissionsResultAction`: This is a callback that you use to execute code when the permissions you have requested are either granted or denied.

An Activity reference is needed to request permissions. All you need to add to your Activities is a single line of code that notifies the PermissionsManager of the Activity callback. Your Activity must implement the `onRequestedPermissionsResult` method that you will be able to override if your Activities extend AppCompatActivity. Your `onRequestedPermissionsResult` method should be as follows:

```java
@Override
public void onRequestPermissionsResult(int requestCode, 
                                       @NonNull String[] permissions, 
                                       @NonNull int[] grantResults) {
    PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
}
```

This will trigger the PermissionsManager to notify all actions that you have sent to it so they can run.

Then, in your code, when you need to execute code that requires permissions, instead of checking and requesting and handling the Activity callback manually, you can just wrap your code in an anonymous class and send it to the permissions manager like follows. For this example, we pretend we have a method `writeToStorage()` that requires the `WRITE_EXTERNAL_STORAGE` permission to function:

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
                      "We need the Storage Permission to do that :(", 
                      Toast.LENGTH_SHORT).show();
    }
});
```

You can add any number of permissions to a request, and the PermissionsResultAction.onGranted() will not be triggered until all the permissions have been granted by the user. If a permission is denied by the user, the onDenied(String) will be called and the PermissionsResultAction will no longer listen for permissions changes.

But, maybe in addition to this, you just want to get it out of the way and request all the permissions up front so you don't need to break the user flow later. In that case, you can just use a simple method which reads the permissions you want from your manifest and requests them for you like follows:

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
