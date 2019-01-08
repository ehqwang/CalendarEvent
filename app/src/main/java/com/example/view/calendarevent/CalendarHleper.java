package com.example.view.calendarevent;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;

import java.util.Calendar;
import java.util.TimeZone;

public class CalendarHleper {
    private static final int MY_INSERT = 0;
    private static final int MY_REMIND = 1;
    private static final int MY_ADD_USER = 2;
    private static final String CBK_COOKIE = "cookie";

    private static String CALENDER_URL = "content://com.android.calendar/calendars";
    private static String CALENDER_EVENT_URL = "content://com.android.calendar/events";
    private static String CALENDER_REMINDER_URL = "content://com.android.calendar/reminders";

    private static String CALENDARS_NAME = "name";
    private static String CALENDARS_ACCOUNT_NAME = "account_name";
    private static String CALENDARS_DISPLAY_NAME = "display_name";

    private String title;
    private String address;
    private long time;

    private Context mContext;
    private MyAsyncQueryHandler mAsyncQueryHandler;
    private OnCalendarQueryCompleteListener onCalendarQueryComplete;

    static CalendarHleper getInstance() {
        return new CalendarHleper();
    }

    private CalendarHleper() {

    }


    void setData(Context context, String title, String address, long time) {
        this.mContext = context;
        mAsyncQueryHandler = new MyAsyncQueryHandler(mContext.getContentResolver());
        this.title = title;
        this.address = address;
        this.time = time;
    }

    public void setOnCalendarQueryComplete(OnCalendarQueryCompleteListener onCalendarQueryComplete) {
        this.onCalendarQueryComplete = onCalendarQueryComplete;
    }

    public interface OnCalendarQueryCompleteListener {
        void onQueryComplete(boolean isSucceed);
    }

    private class MyAsyncQueryHandler extends AsyncQueryHandler {
        public MyAsyncQueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {

            if (cookie instanceof String) {
                String cbkCookie = cookie.toString();
                if (!CBK_COOKIE.equals(cbkCookie)) return;
            }
            switch (token) {
                case MY_ADD_USER:
                    if (uri != null) {
                        addCalendarEvent(false);
                    } else {
                        onCalendarQueryComplete.onQueryComplete(false);
                    }
                    break;
                case MY_INSERT:
                    if (uri != null) {
                        addRemind(uri);
                    }else {
                        onCalendarQueryComplete.onQueryComplete(false);
                    }
                    break;
                case MY_REMIND:
                    if (uri != null) {
                        onCalendarQueryComplete.onQueryComplete(true);
                    }
                    break;
            }

            super.onInsertComplete(token, cookie, uri);
        }
    }

    //获取用户账号
    private static int getUserCalendarAccount(Context context) {
        Cursor userCursor = context.getContentResolver().query(Uri.parse(CALENDER_URL), null, null, null, null);
        try {
            if (userCursor == null || userCursor.getCount() <= 0 ) { //查询返回空值
                return -1;
            }

            userCursor.moveToFirst();
            do {
                String aa = userCursor.getString(userCursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME));
                if (!TextUtils.isEmpty(aa) && aa.equals(CALENDARS_ACCOUNT_NAME)) {
                    return userCursor.getInt(userCursor.getColumnIndex(CalendarContract.Calendars._ID));
                }
            } while (userCursor.moveToNext());
            return -1;
        } finally {
            if (userCursor != null) {
                userCursor.close();
            }
        }

    }

    //添加日历事件
    public void addCalendarEvent(boolean isHasUser) {
        // 获取日历账户的id
        int calId = getUserCalendarAccount(mContext);

        //添加过一次账户之后 ID依然未空直接返回失败
        if (calId < 0 && !isHasUser) {
            onCalendarQueryComplete.onQueryComplete(false);
            return;
        }

        if (calId < 0) {
            addCalendarAccount();
            return;
        }

        final ContentValues event = new ContentValues();

        event.put(CalendarContract.Events.TITLE, title);
        event.put(CalendarContract.Events.DESCRIPTION, address);
        event.put(CalendarContract.Events.CALENDAR_ID, calId);

        Calendar mCalendar = Calendar.getInstance();
        mCalendar.setTimeInMillis(time);//设置开始时间
        long start = mCalendar.getTime().getTime();
        mCalendar.setTimeInMillis(start);//设置终止时间
        long end = mCalendar.getTime().getTime();

        event.put(CalendarContract.Events.DTSTART, start);
        event.put(CalendarContract.Events.DTEND, end);

        event.put(CalendarContract.Events.HAS_ALARM, 1);//设置有闹钟提醒
        event.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());  //这个是时区

        try {
            mAsyncQueryHandler.startInsert(MY_INSERT, CBK_COOKIE, Uri.parse(CALENDER_EVENT_URL), event);
        } catch (Exception e) {
            onCalendarQueryComplete.onQueryComplete(false);
//            e.printStackTrace();
        }
    }

    private void addCalendarAccount() {
        ContentValues value = new ContentValues();

        //  日历名称
        value.put(CalendarContract.Calendars.NAME, CALENDARS_NAME);
        //  日历账号，为邮箱格式
        value.put(CalendarContract.Calendars.ACCOUNT_NAME, CALENDARS_ACCOUNT_NAME);
        //  账户类型，com.android.exchange
        value.put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        //  展示给用户的日历名称
        value.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDARS_DISPLAY_NAME);
        //  它是一个表示被选中日历是否要被展示的值。
        //  0值表示关联这个日历的事件不应该展示出来。
        //  而1值则表示关联这个日历的事件应该被展示出来。
        //  这个值会影响CalendarContract.instances表中的生成行。
        value.put(CalendarContract.Calendars.VISIBLE, 1);
        //  账户标记颜色
        value.put(CalendarContract.Calendars.CALENDAR_COLOR, Color.GREEN);
        //  账户级别
        value.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
        //  它是一个表示日历是否应该被同步和是否应该把它的事件保存到设备上的值。
        //  0值表示不要同步这个日历或者不要把它的事件存储到设备上。
        //  1值则表示要同步这个日历的事件并把它的事件储存到设备上。
        value.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
        //  时区
        value.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().getID());
        //  账户拥有者
        value.put(CalendarContract.Calendars.OWNER_ACCOUNT, CALENDARS_ACCOUNT_NAME);
        value.put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 0);

        Uri calendarUri = Uri.parse(CALENDER_URL);
        calendarUri = calendarUri.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, CALENDARS_ACCOUNT_NAME)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                .build();

        try {
            mAsyncQueryHandler.startInsert(MY_ADD_USER, CBK_COOKIE, calendarUri, value);
        } catch (Exception e) {
            onCalendarQueryComplete.onQueryComplete(false);
//            e.printStackTrace();
        }

    }

    private void addRemind(Uri newEvent) {
        final ContentValues values = new ContentValues();
        values.put(CalendarContract.Reminders.EVENT_ID, ContentUris.parseId(newEvent));
        // 提前10分钟有提醒
        values.put(CalendarContract.Reminders.MINUTES, 2 * 60);
        values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
        try {
            mAsyncQueryHandler.startInsert(MY_REMIND, CBK_COOKIE, Uri.parse(CALENDER_REMINDER_URL), values);
        } catch (Exception e) {
            onCalendarQueryComplete.onQueryComplete(false);
//            e.printStackTrace();
        }
    }
}
