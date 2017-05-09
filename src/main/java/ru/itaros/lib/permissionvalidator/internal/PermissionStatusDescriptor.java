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

package ru.itaros.lib.permissionvalidator.internal;

/**
 * Created by Itaros on 5/9/2017.
 */

public final class PermissionStatusDescriptor {

    public final String permission;

    private boolean isGranted = false;

    public PermissionStatusDescriptor(String permission){
        this.permission = permission;
    }

    public void setGranted(boolean isGranted){
        this.isGranted = isGranted;
    }

    public boolean getIsGranted(){
        return isGranted;
    }


    public static PermissionStatusDescriptor[] constructAllAsPassed(String[] permissions) {
        PermissionStatusDescriptor[] descriptors = new PermissionStatusDescriptor[permissions.length];
        for(int i = 0; i < permissions.length; i++){
            descriptors[i] = new PermissionStatusDescriptor(permissions[i]);
            descriptors[i].setGranted(true);
        }
        return descriptors;
    }
}
