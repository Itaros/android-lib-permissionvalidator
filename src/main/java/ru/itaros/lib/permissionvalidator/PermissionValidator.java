/**   
 * This file is part of Permission Validator Android Module.
 * 
 * Permission Validator Android Module is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Permission Validator Android Module is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Permission Validator Android Module.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.itaros.lib.permissionvalidator;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import android.support.annotation.*;
import android.support.v7.app.AlertDialog;

import java.util.LinkedList;

import ru.itaros.lib.permissionvalidator.internal.PermissionStatusDescriptor;

/**
 * Created by Itaros on 4/11/2017.
 */

public final class PermissionValidator {

    public static abstract class OnPermissionResponseEvent {
        public abstract void onResponse(final PermissionStatusDescriptor[] permissions);
    }

    public static abstract class OnPermissionSuccessEvent extends OnPermissionResponseEvent {}
    public static abstract class OnPermissionFailureEvent extends OnPermissionResponseEvent {}

    private static class OngoingRequest{
        public OngoingRequest(int sequentalId, String[] permissions,
                              OnPermissionSuccessEvent onSuccess,
                              OnPermissionFailureEvent onFailure){
            this.sequentalId = sequentalId;
            this.permissions = permissions;
            this.onSuccess = onSuccess;
            this.onFailure = onFailure;
        }
        private int sequentalId;
        private String[] permissions;

        private OnPermissionSuccessEvent onSuccess;
        private OnPermissionFailureEvent onFailure;

        public int getSequentalId() {
            return sequentalId;
        }

        public String[] getPermissions() {
            return permissions;
        }

        public void callOnSuccess(final PermissionStatusDescriptor[] permissions){
            if(onSuccess!=null)
                onSuccess.onResponse(permissions);
        }
        public void callOnFailure(final PermissionStatusDescriptor[] permissions){
            if(onFailure!=null)
                onFailure.onResponse(permissions);
        }

        public int getPermissionCount() {
            return permissions.length;
        }
    }

    private final LinkedList<OngoingRequest> requests = new LinkedList<>();

    /**
     * Requests a set of permissions with an attempt to not die in the process
     * @param context Activity which serves as a context and onRequestPermissionResult acceptor
     * @param permissions Set of permissions as strings
     * @param onSuccess
     * @param onFailure
     * @param rationale
     * @return True if all permissions from the set are granted in advance(by default or by previous user action)
     */
    public boolean requestPermissions(@NonNull final Activity context, @NonNull final String[] permissions,
                                      @Nullable final OnPermissionSuccessEvent onSuccess,
                                      @Nullable final OnPermissionFailureEvent onFailure,
                                      @Nullable CharSequence rationale){
        boolean[] permissionResultArray = new boolean[permissions.length];
        int i = -1;
        for(String permission : permissions){
            i++;
            int permissionCheck = ContextCompat.checkSelfPermission(context, permission);
            if(permissionCheck == PackageManager.PERMISSION_GRANTED){
                permissionResultArray[i] = true;
            }else if(permissionCheck == PackageManager.PERMISSION_DENIED){
                permissionResultArray[i] = false;
            }
        }
        boolean compoundResult = true;
        for(boolean b : permissionResultArray){
            if(!b){
                compoundResult = false;
                break;
            }
        }
        if(compoundResult) {
            if(onSuccess!=null) {
                onSuccess.onResponse(PermissionStatusDescriptor.constructAllAsPassed(permissions));
            }
            return true;
        }
        if(rationale != null) {
            for (String permission : permissions) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
                    //Should show rationale
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(rationale);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            makeAndroidLevelRequest(context, permissions, onSuccess, onFailure);
                        }
                    });
                    AlertDialog rationaleAlert = builder.create();
                    rationaleAlert.show();
                    return false;
                }
            }
        }
        makeAndroidLevelRequest(context, permissions, onSuccess, onFailure);
        return false;
    }

    /**
     * Requests a single permission with an attempt to not die in the process
     * @param context
     * @param permission
     * @param onSuccess
     * @param onFailure
     * @return True if permission is granted in advance(by default or by previous user action)
     */
    public boolean requestPermission(@NonNull final Activity context, @NonNull final String permission,
                                     @Nullable final OnPermissionSuccessEvent onSuccess,
                                     @Nullable final OnPermissionFailureEvent onFailure,
                                     @Nullable CharSequence rationale){
        return requestPermissions(context, new String[]{permission},
                onSuccess, onFailure, rationale);
    }

    private void makeAndroidLevelRequest(@NonNull Activity context, @NonNull String[] permissions,
                                         @Nullable OnPermissionSuccessEvent onSuccess,
                                         @Nullable OnPermissionFailureEvent onFailure){
        int uniqReqId = getUniqueRequestId();
        OngoingRequest request = new OngoingRequest(uniqReqId, permissions, onSuccess, onFailure);
        requests.add(request);
        ActivityCompat.requestPermissions(context, request.getPermissions(), request.getSequentalId());
    }

    private int getUniqueRequestId() {
        int minimalId = 1;
        for (OngoingRequest request : requests) {
            if(request.getSequentalId()>=minimalId){
                minimalId = request.getSequentalId() + 1;
            }
        }
        return minimalId;
    }

    /**
     * Invoke from Activity method of that name
     */
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        LinkedList<OngoingRequest> removalQueue = new LinkedList<>();
        for(OngoingRequest request : requests){
            if(request.getSequentalId() == requestCode){
                removalQueue.add(request);
                //Getting permission index
                String[] permissionsLocal = request.getPermissions();
                PermissionStatusDescriptor[] descriptors = new PermissionStatusDescriptor[permissionsLocal.length];
                for(int i = 0 ; i < descriptors.length; i++){
                    descriptors[i] = new PermissionStatusDescriptor(permissionsLocal[i]);
                }
                for (PermissionStatusDescriptor staged : descriptors) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (permissions[i].equals(staged.permission)) {
                            staged.setGranted(grantResults[i] == PackageManager.PERMISSION_GRANTED);
                        }
                    }
                }
                //Check for full success
                boolean wasFullSuccess = true;
                for(PermissionStatusDescriptor descriptor : descriptors){
                    if(!descriptor.getIsGranted()){
                        wasFullSuccess = false;
                        break;
                    }
                }
                if(wasFullSuccess) {
                    request.callOnSuccess(descriptors);
                }else{
                    request.callOnFailure(descriptors);
                }
            }
        }
        for(OngoingRequest request : removalQueue){
            requests.remove(request);
        }
        removalQueue.clear();
    }

}
