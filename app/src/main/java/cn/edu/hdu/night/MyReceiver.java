package cn.edu.hdu.night;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context,"玩了很久，休息一下吧 ^_^", Toast.LENGTH_LONG).show();
    }
}
