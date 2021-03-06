package com.moxi.haierc.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.moxi.haierc.R;
import com.moxi.haierc.adapter.HJRecentReadingAdapter;
import com.moxi.haierc.hjbook.HJBookIndexActivity;
import com.moxi.haierc.hjbook.hjdata.HJBookData;
import com.moxi.haierc.hjbook.hjutils.ScanBookUtils;
import com.moxi.haierc.model.UserModel;
import com.moxi.haierc.util.HJCheckVersionCode;
import com.moxi.haierc.util.Utils;
import com.moxi.updateapp.UpdateUtil;
import com.mx.mxbase.base.BaseActivity;
import com.mx.mxbase.constant.Constant;
import com.mx.mxbase.http.MXHttpHelper;
import com.mx.mxbase.utils.DeleteDDReaderBook;
import com.mx.mxbase.utils.FileUtils;
import com.mx.mxbase.utils.MXUamManager;
import com.mx.mxbase.utils.StringUtils;
import com.mx.mxbase.utils.Toastor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import butterknife.Bind;

/**
 * Created by King on 2017/8/1.
 */

public class HJNewMainActivity extends BaseActivity implements View.OnClickListener {

    @Bind(R.id.rl_hj_main_recent_reading)
    RelativeLayout rlHjRecentReading;
    @Bind(R.id.grid_view_hj_main_recent_reading)
    GridView gridViewHjRecentReading;
    @Bind(R.id.hj_icon)
    ImageView imageViewIcon;
    @Bind(R.id.tv_hj_user_name)
    TextView tvUsername;
    @Bind(R.id.hj_main_day)
    TextView tvDay;
    @Bind(R.id.hj_main_week)
    TextView tvWeek;
    @Bind(R.id.update_tv)
    TextView tvneed;

    private MXHttpHelper httpHelper;
    private int[] res = new int[]{
            R.mipmap.mx_img_new_avatar_0,
            R.mipmap.mx_img_new_avatar_1,
            R.mipmap.mx_img_new_avatar_2,
            R.mipmap.mx_img_new_avatar_3};

    private List<HJBookData> listRecent = new ArrayList<>();
    private HJRecentReadingAdapter adapter;
    private boolean NEED_REGET = false;
    private LocalBroadcastManager localBroadcastManager;

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        if (msg.arg1 == 2001) {
            if (msg.what == Integer.parseInt(Constant.SUCCESS)) {
                UserModel userModel = (UserModel) msg.obj;
                if (userModel != null) {
                    tvUsername.setText(userModel.getResult().getName());
                    if (userModel.getResult().getHeadPortrait() != -99) {
                        imageViewIcon.setImageResource(res[userModel.getResult().getHeadPortrait()]);
                    } else {
                        imageViewIcon.setImageResource(R.mipmap.img_mx_new_defate_icon);
                    }
                }
            } else {

            }
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        httpHelper = MXHttpHelper.getInstance(this);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        findViewById(R.id.ll_hj_dict).setOnClickListener(this);
        findViewById(R.id.ll_hj_practice).setOnClickListener(this);
        findViewById(R.id.ll_hj_reading).setOnClickListener(this);
        findViewById(R.id.ll_hj_settings).setOnClickListener(this);
        findViewById(R.id.rl_hj_icon).setOnClickListener(this);
        findViewById(R.id.ll_hj_new_main_lib).setOnClickListener(this);
        findViewById(R.id.ll_hj_new_main_net).setOnClickListener(this);

        listRecent = ScanBookUtils.getInstance(this).getBookData(2, "");
        adapter = new HJRecentReadingAdapter(this, gridViewHjRecentReading, listRecent);
        gridViewHjRecentReading.setAdapter(adapter);
        gridViewHjRecentReading.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (Utils.isFastClick()) {
                    HJBookData hjBookData = listRecent.get(position);
                    File curretnFile = new File(hjBookData.getFilePath());
                    if (curretnFile.exists()) {
                        FileUtils.getInstance().openFile(HJNewMainActivity.this, curretnFile);
                        ScanBookUtils.getInstance(HJNewMainActivity.this).updateBookReadTime(hjBookData.getId());
                    } else {
                        showToast("?????????????????????????????????????????????????????????");
                    }
                }
            }
        });
        IntentFilter filter = new IntentFilter("com.moxi.broadcast.external.action");
        localBroadcastManager.registerReceiver(external, filter);
    }

    //??????????????????????????????????????????
    BroadcastReceiver external = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            listRecent = ScanBookUtils.getInstance(HJNewMainActivity.this).getBookData(2, "");
            adapter.dataChange(listRecent);
        }
    };

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (NEED_REGET) {
            listRecent = ScanBookUtils.getInstance(HJNewMainActivity.this).getBookData(2, "");
            adapter.dataChange(listRecent);
            NEED_REGET = false;
        }
        new DeleteDDReaderBook(HJNewMainActivity.this, false, null).execute();
        getUserInfo(MXUamManager.queryUser(this));
        HJCheckVersionCode.getInstance(this).checkframework(tvneed);
//        openOrOff(true);
        setDate();
    }

    /**
     * ??????????????????
     *
     * @param appSession ??????session
     */
    private void getUserInfo(String appSession) {
        HashMap<String, String> user = new HashMap<>();
        if ("".equals(appSession)) {
            imageViewIcon.setImageResource(R.mipmap.img_mx_new_defate_icon);
            tvUsername.setText("?????????");
        } else {
            user.put("appSession", appSession);
            httpHelper.postStringBack(2001, Constant.GET_USER_INFO, user, getHandler(), UserModel.class);
        }
    }

    private void setDate() {
        final String dayNames[] = {"?????????", "?????????", "?????????", "?????????", "?????????", "?????????", "?????????"};
        Date date = new Date();
        SimpleDateFormat sim = new SimpleDateFormat("yyyy???MM???dd???");
        tvDay.setText(sim.format(date));

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (dayOfWeek < 0) dayOfWeek = 0;
        tvWeek.setText(dayNames[dayOfWeek]);
    }

    private void openOrOff(boolean open) {
        if (open) {
            writeStringValueToFile("/sys/devices/platform/onyx_misc.0/tp_disable", "0");
        } else {
            writeStringValueToFile("/sys/devices/platform/onyx_misc.0/tp_disable", "1");
        }
    }

    private boolean writeStringValueToFile(final String path, String value) {
        FileOutputStream fout = null;
        try {
            File file = new File(path);
            if (!file.exists()) {
                String command = "touch " + path;
                do_exec(command);
            }
            fout = new FileOutputStream(file);
            byte[] bytes = value.getBytes();
            fout.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    private void do_exec(String command) {
        try {
            Process p = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        NEED_REGET = true;
        dialogShowOrHide(false, "");
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {

    }

    @Override
    public void onActivityRestoreInstanceState(Bundle savedInstanceState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        localBroadcastManager.unregisterReceiver(external);
    }

    @Override
    protected int getMainContentViewId() {
        return R.layout.mx_activity_hj_new_main;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_hj_dict:
                try {
                    Intent music = new Intent();
                    ComponentName musicOther = new ComponentName("com.onyx.dict", "com.onyx.dict.activity.DictMainActivity");
                    music.setComponent(musicOther);
                    startActivity(music);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toastor.showToast(HJNewMainActivity.this, "??????????????????????????????????????????");
                }
                break;
            case R.id.ll_hj_practice://????????????
                if (StringUtils.getSDPath("hjschool").equals(""))return;
                String instructionName = "instruction_20171102 - imx6.pdf";
                String instructionPath = StringUtils.getSDPath("hjschool") + instructionName;
                if (!new File(instructionPath).exists()) {
                    FileUtils.copyAssets(HJNewMainActivity.this, instructionName, StringUtils.getSDPath("hjschool"));
                }
                FileUtils.getInstance().openFile(HJNewMainActivity.this, new File(instructionPath));
                break;
            case R.id.ll_hj_reading://????????????
                if (Utils.isSDCardCanReadAndWrite()) {
                    startActivity(new Intent(this, HJBookIndexActivity.class));
                } else {
                    Toastor.showToast(HJNewMainActivity.this, "sd???????????????");
                }
                break;
            case R.id.ll_hj_settings:
                //??????
                startActivity(new Intent(HJNewMainActivity.this, HJSettingActivity.class));
                break;
            case R.id.rl_hj_icon:
                try {
                    Intent sound = new Intent();
                    sound.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    ComponentName cnSound = new ComponentName("com.moxi.user", "com.mx.user.activity.MXLoginActivity");
                    sound.setComponent(cnSound);
                    sound.putExtra("flag_version_stu", "?????????");
                    startActivity(sound);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toastor.showToast(this, "??????????????????????????????????????????");
                    new UpdateUtil(this, this).checkInstall("com.moxi.user");
                }
                break;
            case R.id.ll_hj_new_main_lib:
                if (Utils.isSDCardCanReadAndWrite()) {
                    try {
                        Intent sound = new Intent();
                        sound.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        ComponentName cnSound = new ComponentName("com.moxi.bookstore", "com.moxi.bookstore.activity.bookManager.MXStacksActivity");
                        sound.setComponent(cnSound);
                        sound.putExtra("flag_version_stu", "");
                        startActivity(sound);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toastor.showToast(this, "??????????????????????????????????????????");
                        new UpdateUtil(this, this).checkInstall("com.moxi.bookstore");
                    }
                } else {
                    Toastor.showToast(this, "sd???????????????");
                }
                break;
            case R.id.ll_hj_new_main_net:
                if (Utils.isSDCardCanReadAndWrite()) {
                    try {
                        Intent sound = new Intent();
                        sound.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        ComponentName cnSound = new ComponentName("com.moxi.bookstore", "com.moxi.bookstore.activity.OnlieBookCityActivity");
                        sound.setComponent(cnSound);
                        sound.putExtra("flag_version_stu", "");
                        startActivity(sound);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toastor.showToast(this, "??????????????????????????????????????????");
                        new UpdateUtil(this, this).checkInstall("com.moxi.bookstore");
                    }
                } else {
                    Toastor.showToast(this, "sd???????????????");
                }
                break;
            default:
                break;
        }
    }


    @Override
    public void onBackPressed() {
    }
}
