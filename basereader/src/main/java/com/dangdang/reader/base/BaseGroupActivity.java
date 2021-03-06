package com.dangdang.reader.base;

import android.annotation.TargetApi;
import android.app.ActivityGroup;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.dangdang.reader.R;
import com.dangdang.zframework.log.LogM;
import com.dangdang.zframework.utils.SystemBarTintManager;

public abstract class BaseGroupActivity extends ActivityGroup {

	protected SystemBarTintManager tintManager;
	
	protected abstract void onCreateImpl(Bundle savedInstanceState);

	protected abstract void onDestroyImpl();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initSystemBar();
		onCreateImpl(savedInstanceState);
		
		printLog("[ onCreate TaskId = " + getTaskId() + "]");
	}

	@Override
    protected void onSaveInstanceState(Bundle outState) {
    	
    }
	
	@SuppressWarnings("rawtypes")
	protected String switchActivity(Class clazz, ViewGroup container) {

		final Intent intent = new Intent(this, clazz);
		final String id = clazz.getSimpleName();

		return switchActivity(intent, id, container);
	}

	@SuppressWarnings("deprecation")
	protected String switchActivity(final Intent intent, final String id,
			ViewGroup container) {

		LocalActivityManager localActm = getLocalActivityManager();
		try {
			View inView = localActm.startActivity(id, intent).getDecorView();
			container.removeAllViews();
			container.addView(inView);
		} catch (Exception e) {
			e.printStackTrace();
			localActm.destroyActivity(id, true);
			View inView = localActm.startActivity(id, intent).getDecorView();
			container.removeAllViews();
			container.addView(inView);
		}

		return id;
	}

	protected void onDestroy() {
		super.onDestroy();
		printLog(" onDestroy()");
		onDestroyImpl();

	}

	protected void printLog(String log) {
		LogM.d(getClass().getSimpleName(), log);
	}

	protected void printLogE(String log) {
		LogM.e(getClass().getSimpleName(), log);
	}

	private void initSystemBar() {
		if (!isTransparentSystemBar())
			return;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			setTranslucentStatus(true);
		}		
		tintManager = new SystemBarTintManager(this);
		tintManager.setStatusBarTintEnabled(isFitSystemWindow());
		tintManager.setStatusBarTintResource(getSystemBarColor());
	}

	/**
	 * ???????????????????????????
	 * 
	 * @return
	 */
	public boolean isTransparentSystemBar() {
		return true;
	}

	/**
	 * ????????????????????????????????????false????????????????????????????????????????????????????????????false???????????????padding????????????
	 * 
	 * @return
	 */
	protected boolean isFitSystemWindow() {
		return true;
	}

	/**
	 * ????????????????????????????????????????????????title_bg?????????isFitSystemWindow??????false????????????????????????????????????
	 * 
	 * @return
	 */
	protected int getSystemBarColor() {
		return R.color.green_32; // title_bg;
	}

	@TargetApi(19)
	private void setTranslucentStatus(boolean on) {
		Window win = getWindow();
		WindowManager.LayoutParams winParams = win.getAttributes();
		final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
		if (on) {
			winParams.flags |= bits;
		} else {
			winParams.flags &= ~bits;
		}
		win.setAttributes(winParams);
	}

	/**
	 * ??????????????????????????????????????????
	 * 
	 * @return
	 */
	protected boolean isAnimation() {
		return true;
	}

	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(getContentView(layoutResID));
	}

	private View getContentView(int layoutResID) {
		View view = LayoutInflater.from(this).inflate(layoutResID, null);
		// ????????????
		if (isTransparentSystemBar() && isFitSystemWindow())
			view.setFitsSystemWindows(true);
		return view;
	}
	
	@Override
	public void sendBroadcast(Intent intent){
		if(intent == null)
			return;
		intent.setPackage(this.getPackageName());
		super.sendBroadcast(intent);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		//??????????????????
//		UmengStatistics.onPause(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		//??????????????????
//		UmengStatistics.onResume(this);
	}
}
