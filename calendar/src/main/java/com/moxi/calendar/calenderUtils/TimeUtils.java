package com.moxi.calendar.calenderUtils;

import android.text.format.Time;

import com.moxi.calendar.MainActivity;
import com.moxi.calendar.model.DateInfo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TimeUtils {
	public static int getCurrentYear() {
		Time t = new Time();
		t.setToNow();
		return t.year;
	}
	
	public static int getCurrentMonth() {
		Time t = new Time();
		t.setToNow();
		return t.month + 1;
	}
	
	public static int getCurrentDay() {
		Time t = new Time();
		t.setToNow();
		return t.monthDay;
	}
	
	public static int getTimeByPosition(int position, int originYear, int originMonth, String type) {
    	int year = originYear, month = originMonth;
    	if (position > MainActivity.counts) {
    		for (int i = MainActivity.counts; i < position; i++) {
    			month++;
    			if (month == 13) {
    				month = 1;
    				year++;
    			}
    		}
    	} else if (position < MainActivity.counts) {
    		for (int i = MainActivity.counts; i > position; i--) {
    			month--;
    			if (month == 0) {
    				month = 12;
    				year--;
    			}
    		}
    	}
    	if (type.equals("year")) {
    		return year;
    	}
    	return month;
	}
	
	public static int getWeekDay(String date) {  
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");  
        try {  
            calendar.setTime(sdf.parse(date));  
        } catch (ParseException e) {  
            e.printStackTrace();  
        }  
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);  
        if (dayOfWeek == 1) {
            dayOfWeek = 0;  
        }
        else {
            dayOfWeek -= 1; 
        }
        return dayOfWeek;  
    }  
	
	
	public static boolean isLeapYear(int year) {
		if (year % 400 == 0 || year % 100 != 0 && year % 4 == 0) {
			return true;
		}
		return false;
	}
	
	public static int getDaysOfMonth(int year, int month) {
		switch (month) {
		case 1:
		case 3:
		case 5:
		case 7:
		case 8:
		case 10:
		case 12:
			return 31;
		case 4:
		case 6:
		case 9:
		case 11:
			return 30;
		default:
			if (isLeapYear(year)) {
				return 29;
			}
			return 28;
		}
	}
	
	public static String getFormatDate(int year, int month) {
    	String formatYear = year + "";
    	String formatMonth = "";
    	if (month < 10) {
    		formatMonth = "0" + month;
    	} else {
    		formatMonth = month + "";
    	}
    	return formatYear + "-" + formatMonth + "-01";
	}
	
	public static String getFormatDate(int year, int month, int day) {
    	String formatYear = year + "";
    	String formatMonth = "";
    	String formatDay = "";
    	if (month < 10) {
    		formatMonth = "0" + month;
    	} else {
    		formatMonth = month + "";
    	}
    	if (day < 10) {
    		formatDay = "0" + day;
    	} else {
    		formatDay = day + "";
    	}
    	return formatYear + "-" + formatMonth + "-" + formatDay;
	}
	
	public static List<DateInfo> initCalendar(String formatDate, int month) throws Exception {
		int dates = 1;
		int year = Integer.parseInt(formatDate.substring(0, 4));
		int [] allDates = new int[42];
		for (int i = 0; i < allDates.length; i++) {
			allDates[i] = -1;
		}
		int firstDayOfMonth = TimeUtils.getWeekDay(formatDate);
		int totalDays = TimeUtils.getDaysOfMonth(year, month);
		for (int i = firstDayOfMonth; i < totalDays + firstDayOfMonth; i++) {
    		allDates[i] = dates;
    		dates++;
    	}
		
		List<DateInfo> list = new ArrayList<DateInfo>();
		DateInfo dateInfo;
		for (int i = 0; i < allDates.length; i++) {
    		dateInfo = new DateInfo();
    		dateInfo.date=(allDates[i]);
    		if (allDates[i] == -1) {
    			dateInfo.NongliDate=("");
    			dateInfo.isThisMonth=(false);
    			dateInfo.isWeekend=(false);
    		}
    		else {
    			String date = TimeUtils.getFormatDate(year, month, allDates[i]);
    			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    			long time = sdf.parse(date).getTime();
    			Lunar lunar = new Lunar(time);
    			if (lunar.isSFestival()) {
					dateInfo.NongliDate=(lunar.getSFestivalName());
					dateInfo.isHoliday=(true);
				} else {
					if (lunar.isLFestival() && lunar.getLunarMonthString().substring(0, 1).equals("???") == false) {
						dateInfo.NongliDate=(lunar.getLFestivalName());
						dateInfo.isHoliday=(true);
					} else {
						if (lunar.getLunarDayString().equals("??????")) {
							dateInfo.NongliDate=(lunar.getLunarMonthString() + "???");
						} else {
							dateInfo.NongliDate=(lunar.getLunarDayString());
						}
						dateInfo.isHoliday=(false);
					}
				}
    			dateInfo.isThisMonth=(true);
    			int t = getWeekDay(getFormatDate(year, month, allDates[i]));
    			if (t == 0 || t == 6) {
    				dateInfo.isWeekend=(true);
    			}
    			else {
    				dateInfo.isWeekend=(false);
    			}
    		}
    		list.add(dateInfo);
    	}
    	
    	int front = DataUtils.getFirstIndexOf(list);
    	int back = DataUtils.getLastIndexOf(list);
    	int lastMonthDays = getDaysOfMonth(year, month - 1);
    	int nextMonthDays = 1;
    	for (int i = front - 1; i >= 0; i--) {
    		list.get(i).date=(lastMonthDays);
    		lastMonthDays--;
    	}
    	for (int i = back + 1; i < list.size(); i++) {
    		list.get(i).date=(nextMonthDays);
    		nextMonthDays++;
    	}
    	return list;
	}
}
