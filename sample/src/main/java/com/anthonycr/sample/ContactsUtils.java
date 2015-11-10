package com.anthonycr.sample;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

public class ContactsUtils {
    private static final String DEBUG_TAG = "PermissionTest";

    public static void readPhoneContacts(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (null != cursor && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                Log.e(DEBUG_TAG, "================= " + contactName + " ==========");
                if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor pCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    if (null != pCursor) {
                        while (pCursor.moveToNext()) {
                            int phoneType = pCursor.getInt(pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                            String phoneNo = pCursor.getString(pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            switch (phoneType) {
                                case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                                    Log.i(contactName + ": TYPE_MOBILE", " " + phoneNo);
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                                    Log.i(contactName + ": TYPE_HOME", " " + phoneNo);
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                                    Log.i(contactName + ": TYPE_WORK", " " + phoneNo);
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE:
                                    Log.i(contactName + ": TYPE_WORK_MOBILE", " " + phoneNo);
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
                                    Log.i(contactName + ": TYPE_OTHER", " " + phoneNo);
                                    break;
                                default:
                                    break;
                            }
                        }
                        pCursor.close();
                    }
                }
            }
            cursor.close();
        }
    }
}
