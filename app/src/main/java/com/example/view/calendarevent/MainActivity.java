package com.example.view.calendarevent;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        TextView tv = findViewById(R.id.add_calendar_event);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCalendarEvent();
            }
        });


    }

    private void addCalendarEvent() {
        if (Build.VERSION.SDK_INT >= 23) {
            //此处做动态权限申请
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR}, 1);
            } else {
                CalendarHleper calendarHelper = CalendarHleper.getInstance();
                calendarHelper.setData(this, "title", "描述" , System.currentTimeMillis());
                calendarHelper.addCalendarEvent(true);
                calendarHelper.setOnCalendarQueryComplete(new CalendarHleper.OnCalendarQueryCompleteListener() {
                    @Override
                    public void onQueryComplete(boolean isSucceed) {
                        if (isSucceed) Toast.makeText(getApplicationContext(), "succee", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
