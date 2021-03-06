package com.moxi.writeNote.Activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.moxi.handwritinglibs.WriteNoFigerSaveView;
import com.moxi.handwritinglibs.asy.DeleteOneFileAsy;
import com.moxi.handwritinglibs.db.WritPadModel;
import com.moxi.handwritinglibs.db.WritePadUtils;
import com.moxi.handwritinglibs.db.index.IndexDbUtils;
import com.moxi.handwritinglibs.listener.DeleteListener;
import com.moxi.handwritinglibs.listener.NoteSaveWriteListener;
import com.moxi.handwritinglibs.model.CodeAndIndex;
import com.moxi.handwritinglibs.model.ExtendModel;
import com.moxi.handwritinglibs.utils.DataUtils;
import com.moxi.handwritinglibs.utils.DbPhotoLoader;
import com.moxi.handwritinglibs.utils.FingerUtils;
import com.moxi.handwritinglibs.utils.TimerUtils;
import com.moxi.writeNote.Activity.utils.PhotoCutActivity;
import com.moxi.writeNote.R;
import com.moxi.writeNote.WriteBaseActivity;
import com.moxi.writeNote.adapter.WriteNoteBackAdapter;
import com.moxi.writeNote.config.ActivityUtils;
import com.moxi.writeNote.dialog.CustomPenActivity;
import com.moxi.writeNote.dialog.PageSkipActivity;
import com.moxi.writeNote.dialog.ScreenLightAdjustActivity;
import com.moxi.writeNote.listener.InsertLister;
import com.moxi.writeNote.utils.BackPhotoUtils;
import com.moxi.writeNote.utils.BrushSettingUtils;
import com.moxi.writeNote.utils.DataUtuls;
import com.moxi.writeNote.utils.InsertFileAsy;
import com.moxi.writeNote.utils.UpdateDrawBackTheard;
import com.moxi.writeNote.utils.UpdateFileIndex;
import com.moxi.writeNote.view.InterceptView;
import com.moxi.writeNote.view.NoGrigView;
import com.moxi.writeNote.view.PaintBackView;
import com.mx.mxbase.base.MyApplication;
import com.mx.mxbase.constant.APPLog;
import com.mx.mxbase.interfaces.InsureOrQuitListener;
import com.mx.mxbase.utils.SystemUtils;
import com.mx.mxbase.utils.ToastUtils;
import com.mx.mxbase.view.WriteDrawLayout;
import com.onyx.android.sdk.api.device.epd.EpdController;
import com.onyx.android.sdk.api.device.epd.UpdateMode;
import com.onyx.android.sdk.device.Device;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import butterknife.Bind;

/**
 * ??????????????????????????????
 * Created by xj on 2017/6/19.
 */

public class NewActivity extends WriteBaseActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener, InterceptView.ClickOther {
    private static final int START_FILEMANAGER_RESULT = 101;
    private static final int START_CUTPHOTO_RESULT = 102;
    /**
     * ???????????????
     */
    private WritPadModel model;
    /**
     * ?????????????????????
     */
    private int index = 0;
    /**
     * ???????????????
     */
    private int fileSize = 0;
    /**
     * ?????????
     */
    @Bind(R.id.onclick_main_layout)
    InterceptView onclick_main_layout;
    @Bind(R.id.complete)
    TextView complete;
    @Bind(R.id.title)
    TextView title;

    @Bind(R.id.save_write)
    ImageButton save_write;
    @Bind(R.id.back_last)
    ImageButton back_last;
    @Bind(R.id.recover_next)
    ImageButton recover_next;
    @Bind(R.id.screen_light_adjust)//??????????????????
            ImageButton screen_light_adjust;
    @Bind(R.id.refuresh)
    ImageButton refuresh;
    @Bind(R.id.ash_can)
    ImageButton ash_can;
    @Bind(R.id.add_page)
    ImageButton add_page;
    @Bind(R.id.skin)
    ImageButton skin;
    //????????????view
    //?????????
    @Bind(R.id.rubber)
    WriteDrawLayout rubber;
    //??????
    @Bind(R.id.pen)
    WriteDrawLayout pen;
    //??????????????????
    @Bind(R.id.pen_group)
    RadioGroup pen_group;

    @Bind(R.id.clear_screen)
    ImageButton clear_screen;

    @Bind(R.id.last_page)
    ImageButton last_page;
    @Bind(R.id.show_index)
    TextView show_index;
    @Bind(R.id.next_page)
    ImageButton next_page;
    @Bind(R.id.line)
    View line;
    //??????View
    @Bind(R.id.write_view)
    WriteNoFigerSaveView write_view;
    @Bind(R.id.write_back)
    PaintBackView write_back;
    private boolean isClicksetingPen = true;
    /**
     * ????????????????????????????????????true,????????????????????????false;
     */
    private int noAllPageReplaceStyle = 0;

    private long clickTime = 0;
    /**
     * activity?????????onstop
     */
    private boolean isStop = false;

    @Override
    protected int getMainContentViewId() {
        return R.layout.new_activity;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            model = (WritPadModel) savedInstanceState.getSerializable("model");
            if (null == model) {
                android.os.Process.killProcess(android.os.Process.myPid());
                return;
            }
            if (model.id != -1)
                index = savedInstanceState.getInt("index");
        } else {
            Bundle bundle = getIntent().getExtras();
            model = (WritPadModel) bundle.getSerializable("model");
        }
        if (model.id != -1) {
            fileSize = WritePadUtils.getInstance().getTemporarySize(model.saveCode);
            DbPhotoLoader.getInstance().clearBitmap(model.saveCode, 0);
            click_item = model.getExtendModel().background;

            noAllPageReplaceStyle = model.getExtendModel().noAllPageReplaceStyle;

            index = IndexDbUtils.getInstance().getIndex(model.saveCode);
            //?????????????????????
            dialogShowOrHide(true, "");
            new UpdateFileIndex(model.saveCode, new UpdateFileIndex.updateFileListener() {
                @Override
                public void onUpdateSucess() {
                    dialogShowOrHide(false, "");
                    initView();
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            String name;
            if (model.name == null || model.name.isEmpty() || model.name.equals("????????????")) {
                name = DataUtuls.getCurrentTime(System.currentTimeMillis());
            } else {
                name = nameSpil(model.name);
            }
            model.name = name;
            model.saveCode = model.parentCode + "/" + name;
            noAllPageReplaceStyle = getSettingBackground();
            fileSize = 1;

            index = 0;
            initView();
        }
    }

    private void initView() {
        pen.setallValue(R.mipmap.pencil, false);
        rubber.setallValue(R.mipmap.rubber, false);

        pen.setOnClickListener(penClick);
        rubber.setOnClickListener(penClick);
        onclick_main_layout.setClickOther(this);
        complete.setOnClickListener(this);
        save_write.setOnClickListener(this);
        back_last.setOnClickListener(this);
        recover_next.setOnClickListener(this);
        screen_light_adjust.setOnClickListener(this);
        refuresh.setOnClickListener(this);
        ((RadioButton) findViewById(R.id.pen6)).setOnClickListener(this);
        add_page.setOnClickListener(this);
        ash_can.setOnClickListener(this);
        skin.setOnClickListener(this);
        clear_screen.setOnClickListener(this);
        last_page.setOnClickListener(this);
        next_page.setOnClickListener(this);
        show_index.setOnClickListener(this);

        pen_group.setOnCheckedChangeListener(this);

        title.setText(model.name);
        write_back.setDrawStyle(click_item);
        write_view.setUpListener(this);
        initSetWriteNote(false);
        pen.changeStatus(true);

    }

    /**
     * ??????????????????????????????
     *
     * @param filName
     * @return
     */
    private String nameSpil(String filName) {
        String str = filName.replaceAll("[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~???@#???%??????& amp;*????????????+|{}????????????????????????????????????|-]", "");
        return str;
    }

    private void initTimerUtils() {
        long timeouts = Settings.System.getLong(getContentResolver(), "screen_off_timeout",
                5 * 60 * 1000 /* default, 3 mininutes */);
        APPLog.e("timeouts", timeouts);
        int timeout = 0;//?????????????????? ???
        if (timeouts > Integer.MAX_VALUE || timeouts < 0) {
            timeout = Integer.MAX_VALUE;
        } else {
            timeout = (int) (timeouts / 1000);
        }
        timeout += 10;
        write_view.setTimerUtils(TimerUtils.TimerE.UP, 0, timeout, new TimerUtils.TimeListener() {
            @Override
            public void cuttentTime(int time) {
                APPLog.e("cuttentTime", time);
            }

            @Override
            public void TimeEnd() {
                if (!isStop) {
                    //????????????
                    SystemUtils.putOutScreen(NewActivity.this);
                    releaseWakeLock();
                }
            }
        });
    }

    View.OnClickListener penClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onClickOther();
            boolean isRubber = false;
            switch (v.getId()) {
                case R.id.pen:
                    isRubber = false;
                    break;
                case R.id.rubber:
                    isRubber = true;
                    break;
                default:
                    break;
            }
            if (write_view.isNibWipe() == isRubber) return;
            pen.changeStatus(!isRubber);
            rubber.changeStatus(isRubber);
            write_view.setNibWipe(isRubber);
            setingPenIndex();
        }
    };

    private void setingPenIndex() {
        isClicksetingPen = false;
        if (write_view.isNibWipe()) {
            ((RadioButton) (pen_group.getChildAt(BrushSettingUtils.getInstance(NewActivity.this).pitchOnRubberIndex()))).performClick();
        } else {
            ((RadioButton) (pen_group.getChildAt(BrushSettingUtils.getInstance(NewActivity.this).pitchdrawLineIndex()))).performClick();
        }
        write_view.setClearLineWidth(BrushSettingUtils.getInstance(this).getRubberSize());
        write_view.setDrawLineWidth(BrushSettingUtils.getInstance(this).getDrawLineSize());
        isClicksetingPen = true;
    }

    /**
     * ??????????????????
     *
     * @param group
     * @param checkedId
     */
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        write_view.setCanDraw(false, 1);
        int position = -1;
        switch (checkedId) {
            case R.id.pen0:
                position = 0;
                break;
            case R.id.pen1:
                position = 1;
                break;
            case R.id.pen2:
                position = 2;
                break;
            case R.id.pen3:
                position = 3;
                break;
            case R.id.pen4:
                position = 4;
                break;
            case R.id.pen5:
                position = 5;
                break;
            case R.id.pen6:

                break;
            default:
                break;
        }
        if (position != -1) {
            if (write_view.isNibWipe()) {
                int size = BrushSettingUtils.getInstance(NewActivity.this).getRubberIndexSize(position);
                write_view.setClearLineWidth(size);
            } else {
                int size = BrushSettingUtils.getInstance(NewActivity.this).getDrawLineIndexSize(position);
                write_view.setDrawLineWidth(size);
            }
        }
        write_view.setCanDraw(true, 2);
    }


    @Override
    public void onClick(View v) {
        if (write_view != null && write_view.getTimerUtils() != null)
            write_view.getTimerUtils().resetStartTime();
        if (Math.abs(System.currentTimeMillis() - clickTime) < 1000) {
//            if (v.getId() != R.id.back_last)
            return;
        }
        if (v.getId() != R.id.back_last && v.getId() != R.id.clear_screen && v.getId() != R.id.ash_can) {
            onClickOther();
            clickTime = System.currentTimeMillis();
        }

        switch (v.getId()) {
            case R.id.complete://??????
                if (!write_view.isDrawUp()) return;
                backActivity();
                break;
            case R.id.save_write://??????????????????
                write_view.setCanDraw(false, 4);
                dialogShowOrHide(true, "");
                write_view.saveWritePad(model, new NoteSaveWriteListener() {
                    @Override
                    public void isSucess(boolean is, WritPadModel model) {
                        dialogShowOrHide(false, "");
                        write_view.setCanDraw(true, 2);
                    }
                });
                break;
            case R.id.back_last://??????
                write_view.backLastDraw(true);
                break;
            case R.id.recover_next://??????
                write_view.backLastDraw(false);
                break;
            case R.id.screen_light_adjust://????????????
                if (isClicksetingPen) {
                    write_view.getTimerUtils().stopTimer();
                    write_view.setCanDraw(false, 5);
                    startActivity(new Intent(this, ScreenLightAdjustActivity.class));
                }
                break;
            case R.id.pen6://????????????
                //?????????????????????
                if (isClicksetingPen) {
                    write_view.setCanDraw(false, 6);
                    CustomPenActivity.startCustomPen(this, !write_view.isNibWipe());
                }
                break;
            case R.id.show_index:
                write_view.setCanDraw(false, 7);
                PageSkipActivity.startPageSkip(this, index, fileSize);
                break;
            case R.id.refuresh://????????????
                write_view.setCanDraw(false, 8);
                EpdController.refreshScreenRegion(getWindow().getDecorView(), 0, 0, Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1, UpdateMode.GC);
                getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        write_view.setCanDraw(true, 9);
                    }
                }, 1000);
                break;
            case R.id.ash_can://????????????
                String hitn = fileSize <= 1 ? "????????????????????????" : "?????????????????????????????????????????????";
                write_view.setCanDraw(false, 10);
                insureDialog(hitn, "delete", new InsureOrQuitListener() {
                    @Override
                    public void isInsure(Object code, boolean is) {
                        if (is && fileSize > 1) {
                            dialogShowOrHide(true, "????????????");
                            new DeleteOneFileAsy(model, index, new DeleteListener() {
                                @Override
                                public void onDelete(boolean isDelete) {
                                    for (int i = index; i < fileSize; i++) {
                                        DbPhotoLoader.getInstance().clearBitmap(model.saveCode, i);
                                    }
                                    fileSize = WritePadUtils.getInstance().getTemporarySize(model.saveCode);
                                    initSetWriteNote(false);
                                    dialogShowOrHide(false, "");
                                    write_view.setCanDraw(true, 11);

                                }
                            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        } else {
                            write_view.setCanDraw(true, 12);
                        }
                    }
                });

                break;
            case R.id.add_page://??????
                if (!internalJuge()) return;
                write_view.setCanDraw(false, 13);
                dialogShowOrHide(true, "????????????");
                WritPadModel mod;
                if (noAllPageReplaceStyle == 1) {
                    mod = new WritPadModel(model.name, model.saveCode, model.isFolder, model.parentCode, model._index, "");
                } else {
                    mod = model;
                }
                if (getInsertStype()) {//???????????????
                    index--;
                }
                new InsertFileAsy(mod, index, new InsertLister() {
                    @Override
                    public void onInsert(boolean isDelete) {
                        if (isfinish) return;
                        for (int i = index + 1; i < fileSize; i++) {
                            APPLog.e("clear_index", i);
                            DbPhotoLoader.getInstance().clearBitmap(model.saveCode, i);
                        }
                        if (getInsertStype()) {
                            //?????????????????????????????????????????????????????????
                            write_view.setCodeAndIndex(new CodeAndIndex(model.saveCode, index + 2));
                        }
                        fileSize++;
                        index++;
                        initSetWriteNote(true);

                        dialogShowOrHide(false, "");
                        write_view.setCanDraw(true, 14);
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                break;
            case R.id.skin:// ??????
                write_view.setCanDraw(false, 15);
                showPopupWindow(v);
                break;
            case R.id.clear_screen://??????
                write_view.setCanDraw(false, 16);
                insureDialog("?????????????????????????????????????????????", "delete", new InsureOrQuitListener() {
                    @Override
                    public void isInsure(Object code, boolean is) {
                        if (is) {
                            write_view.clearScreen();
                        }
                        write_view.setCanDraw(true, 17);
                    }
                });

                break;
            case R.id.last_page://?????????
                lastPage();
                break;
            case R.id.next_page://?????????
                nextPage();
                break;
            default:
                break;
        }

    }

    private void initSetWriteNote(boolean saveLast) {
        if (saveLast) {
            write_view.saveWritePad(model, null);
        }
        if (index > (fileSize - 1)) {
            index = fileSize - 1;
        }
        write_view.setSaveCode(model.saveCode, index);
        //TODO ??????????????????
        if (noAllPageReplaceStyle == 1) {
            /**
             * ????????????????????????
             */
            String extent = WritePadUtils.getInstance().getExtent(model.saveCode, index);
            APPLog.e("extent="+extent);
            //???????????????
            model.extend = extent;
            ExtendModel model = DataUtils.getExtendModel(extent);
            if (click_item != model.background||click_item==-1) {
                click_item = model.background;
                updataBack();
            }
        }
        //?????????????????????
        show_index.setText(String.valueOf(index + 1) + "/" + String.valueOf(fileSize));
    }

    private void lastPage() {
        if (index <= 0) {
            return;
        }
        index--;
        initSetWriteNote(true);
    }

    private void nextPage() {
        if (index >= (fileSize - 1)) {
            return;
        }
        index++;
        initSetWriteNote(true);
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putSerializable("model", model);
        outState.putInt("index", index);
    }

    @Override
    public void onActivityRestoreInstanceState(Bundle savedInstanceState) {

    }

    private boolean isFinish = false;
    private boolean isShowPopWindows = false;

    @Override
    public void onActivityDestroyed(Activity activity) {
        //??????????????????
        write_view.saveWritePad(model, null);

        ActivityUtils.getInstance().ClearActivity(this);
        dialogShowOrHide(false, "");
        isFinish = true;
        releaseWakeLock();
    }


    private int click_item = 0;
    PopupWindow popupWindow;
    private boolean isOther = false;

    private void showPopupWindow(View view) {
        releaseWakeLock();
        isOther = false;
        // ????????????????????????????????????????????????
        View contentView = LayoutInflater.from(this).inflate(
                R.layout.pop_new_skin_list, null);
        NoGrigView grigView = (NoGrigView) contentView.findViewById(R.id.back_gride);
        RadioGroup group_button = (RadioGroup) contentView.findViewById(R.id.group_button);
        ImageView image_position = (ImageView) contentView.findViewById(R.id.image_position);
        grigView.setOnItemClickListener(clickListener);
        group_button.setOnCheckedChangeListener(checkedChangeListener);
        if (noAllPageReplaceStyle == 1) {
            ((RadioButton) group_button.getChildAt(0)).setChecked(true);
        } else {
            ((RadioButton) group_button.getChildAt(1)).setChecked(true);
        }
        WriteNoteBackAdapter backAdapter = new WriteNoteBackAdapter(this, BackPhotoUtils.getBackMinPhoto(), click_item);
        grigView.setAdapter(backAdapter);

        popupWindow = new PopupWindow(contentView,
                MyApplication.ScreenWidth - 20, LinearLayout.LayoutParams.WRAP_CONTENT, true);

        popupWindow.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isOther)
                    isOther = event.getAction() == 2;

                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismissPopWindow(true);
                    return true;
                }
                return false;
            }
        });
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                write_view.setCanDraw(true, 18);
                acquireWakeLock();
            }
        });
        // ????????????????????????show
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        popupWindow.setOutsideTouchable(false);

        int[] location = new int[2];
        view.getLocationOnScreen(location);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) image_position.getLayoutParams();
        int padingtop = -3;
        params.setMargins(location[0] + (skin.getWidth() - 34) / 2, padingtop, 0, 0);
        popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, 10, location[1] - 335);
    }

    private void dismissPopWindow(boolean iswrite) {
        if (popupWindow != null) {
            if (!popupWindow.isShowing()) {
                popupWindow.showAsDropDown(skin);
            }
            popupWindow.dismiss();
            popupWindow = null;
        }
        if (iswrite) {
            getHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    write_view.setCanDraw(true, 19);
                    acquireWakeLock();
                }
            }, 300);
        }
    }

    RadioGroup.OnCheckedChangeListener checkedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.only_page://??????
                    noAllPageReplaceStyle = 1;
                    break;
                case R.id.all_page://??????
                    noAllPageReplaceStyle = 0;
                    break;
                default:
                    break;
            }
        }
    };

    public AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (position > 16) {
                if (position == 17) {//?????????????????????
                    //???????????????????????????????????????
                    Intent input = new Intent();
                    ComponentName cnInput = new ComponentName("com.moxi.filemanager", "com.moxi.filemanager.FileManagerActivity");
                    input.setComponent(cnInput);
                    input.putExtra("pick_attachment", true);
                    input.putExtra("pickAttachStyle", 1);
                    startActivityForResult(input, START_FILEMANAGER_RESULT);
                } else if (position == 18) {//???????????????????????????
                    startActivityForResult(new Intent(NewActivity.this, ListMyBackgroundActivity.class), START_CUTPHOTO_RESULT);
                }
            } else {
                click_item = position;
                saveUpdataBackground("");
            }
        }
    };

    private void saveUpdataBackground(String backgroundFilePath) {
        ExtendModel mo = model.getExtendModel();
        mo.background = click_item;
        mo.backgroundFilePath = backgroundFilePath;
        mo.noAllPageReplaceStyle = noAllPageReplaceStyle;
        model.setExtend(mo);
        //????????????
        if (noAllPageReplaceStyle == 1) {
            long _id = WritePadUtils.getInstance().judgeDataExist(model.saveCode, model.isFolder, index);
            if (_id != -1) {
                //??????????????????
                WritePadUtils.getInstance().upDateExtend(DataUtils.getExtendStr(mo), _id);
            }
        }
        updataBack();
    }

    /**
     * ??????????????????
     */
    public void updataBack() {
        write_back.setDrawStyle(click_item, model.getExtendModel().backgroundFilePath);
        dismissPopWindow(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 10) {
                //??????????????????
                index = data.getIntExtra("page", 0);
                initSetWriteNote(true);
            } else if (requestCode == START_FILEMANAGER_RESULT) {
                //?????????????????????????????????
                Uri uri = data.getData();
                try {
                    File file = new File(new URI(uri.toString()));
                    if (!file.exists() || !file.canRead() || file.length() == 0) {
                        ToastUtils.getInstance().showToastShort("??????????????????????????????");
                    } else {//
                        PhotoCutActivity.startACutctivity(this, file.getAbsolutePath(), START_CUTPHOTO_RESULT);
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == START_CUTPHOTO_RESULT) {
                //??????????????????????????????
                String filePath = data.getStringExtra("filePath");
                //????????????
                click_item = -1;
                saveUpdataBackground(filePath);
            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!write_view.isDrawUp()) return true;
        if (keyCode == KeyEvent.KEYCODE_POWER) {
            isShowPopWindows = true;
            write_view.setCanDraw(false, 20);
            Device.currentDevice.hideSystemStatusBar(this);
        } else if (keyCode == KeyEvent.KEYCODE_PAGE_UP || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            //?????????
            onClickOther();
            onClick(last_page);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            //?????????
            onClickOther();
            onClick(next_page);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            backActivity();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void backActivity() {
        write_view.setleaveScribbleMode(false, 0);
        dialogShowOrHide(true, "?????????...");
        int ii = index;
        if (noAllPageReplaceStyle == 0) {
            ii = -1;
        }
        new Thread(new UpdateDrawBackTheard(model.saveCode, ii, model.extend, new UpdateDrawBackTheard.UpdateListener() {
            @Override
            public void onResult() {
                if (isfinish) return;
                dialogText("?????????...");
                IndexDbUtils.getInstance().saveData(model.saveCode, index);
//                DrawIndexUtils.getInstance().saveData(model.saveCode,index);
                write_view.saveWritePad(model, new NoteSaveWriteListener() {
                    @Override
                    public void isSucess(boolean is, WritPadModel model) {
                        if (isfinish) return;
                        dialogShowOrHide(false, "?????????...");
//                        onBackPressed();
                        NewActivity.this.finish();
                    }
                });
            }
        })).start();

    }

    @Override
    protected void onStop() {
        super.onStop();
//        dismissPopWindow(false);
//        if (write_view != null)
//            write_view.setCanDraw(false);
//        releaseWakeLock();
        APPLog.e("onStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        APPLog.e("onPause");
        isStop = true;
        dismissPopWindow(false);
        if (write_view != null) {
            write_view.setCanDraw(false, 21);
            write_view.getTimerUtils().stopTimer();
        }
        FingerUtils.openOrOff(true);
        releaseWakeLock();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FingerUtils.openOrOff(false);
        APPLog.e("onResume");
        acquireWakeLock();
        setingPenIndex();
        getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (windowFocus)
                    write_view.setCanDraw(true, 22);
            }
        }, 500);
        isStop = false;
        initTimerUtils();
    }

    private boolean windowFocus = false;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        windowFocus = hasFocus;
        if (hasFocus) {
            getHandler().removeCallbacksAndMessages(null);
            isShowPopWindows = false;
            Device.currentDevice.hideSystemStatusBar(this);
            write_view.setCanDraw(true, 23);
        } else {
            write_view.setCanDraw(false, 24);
        }
    }

    @Override
    public void onClickOther() {
        if (!(popupWindow != null && popupWindow.isShowing())) {
            write_view.setleaveScribbleMode(true, 1);
        }
    }

    PowerManager.WakeLock wakeLock = null;

    //???????????????????????????????????????????????????????????????CPU??????????????????
    private void acquireWakeLock() {
        if (null == wakeLock || !wakeLock.isHeld()) {
            APPLog.e("??????WakeLock");
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "PostLocationService");
            if (null != wakeLock) {
                wakeLock.acquire();
            }
        }
    }

    //?????????????????????
    private void releaseWakeLock() {
        if (null != wakeLock && wakeLock.isHeld()) {
            APPLog.e("??????WakeLock");
            wakeLock.release();
            wakeLock = null;
        }
    }

}
