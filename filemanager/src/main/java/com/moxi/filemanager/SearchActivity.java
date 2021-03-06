package com.moxi.filemanager;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.PersistableBundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.moxi.filemanager.adapter.FileAdapter;
import com.moxi.filemanager.configer.ConfigerUtils;
import com.moxi.filemanager.model.FileModel;
import com.moxi.filemanager.utils.ClipboardUtils;
import com.moxi.filemanager.utils.SearchRunFile;
import com.moxi.filemanager.utils.StartFile;
import com.moxi.filemanager.view.FilePopWindow;
import com.mx.mxbase.base.BaseApplication;
import com.mx.mxbase.base.MyApplication;
import com.mx.mxbase.dialog.InputDialog;
import com.mx.mxbase.interfaces.InsureOrQuitListener;
import com.mx.mxbase.utils.DensityUtil;
import com.mx.mxbase.utils.FileUtils;
import com.mx.mxbase.utils.PakegeString;
import com.mx.mxbase.utils.StringUtils;
import com.mx.mxbase.utils.ToastUtils;
import com.mx.mxbase.view.AlertDialog;
import com.mx.mxbase.view.SildeFrameLayout;
import com.mx.mxbase.view.SlideLinerlayout.SlideListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;

import static org.litepal.LitePalApplication.getContext;

public class SearchActivity extends BaseFileManagerActivity implements View.OnClickListener, SlideListener, SildeFrameLayout.SildeEventListener,
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
        FilePopWindow.FileOperateListener{

    public static void startSearchActivity(Activity activity, boolean pick_attachment,int pickAttachStyle, int requestCode) {
        Intent intent = new Intent(activity, SearchActivity.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean("pick_attachment", pick_attachment);
        bundle.putInt("pickAttachStyle", pickAttachStyle);
        intent.putExtras(bundle);
        activity.startActivityForResult(intent, requestCode);
    }

    @Bind(R.id.back_rl)
    TextView back_rl;
    @Bind(R.id.search_file_progress)
    ProgressBar search_file_progress;
    @Bind(R.id.keyword_ed)
    EditText keyword_ed;

    @Bind(R.id.silde_layout)
    SildeFrameLayout silde_layout;
    @Bind(R.id.grid_items)
    GridView grid_items;

    @Bind(R.id.last_page)
    ImageButton last_page;
    @Bind(R.id.show_index)
    TextView show_index;
    @Bind(R.id.next_page)
    ImageButton next_page;

    /**
     * ??????????????????
     */
    private int CurrentIndex = 0;
    /**
     * ?????????
     */
    private int totalIndex = 1;
    private int pageSize = 30;

    private List<FileModel> listData = new ArrayList<>();
    private List<FileModel> sonList = new ArrayList<>();
    private FileAdapter adapter;
    private boolean pick_attachment = false;
    private int pickAttachStyle=0;
    //??????????????????1?????????????????????????????????
    private String searchKey;
    private SearchRunFile searchThread;
    private FilePopWindow filePopWindow;
    private ArrayList<String> files=new ArrayList<>();

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case 1://?????????????????????
                if (searchThread != null && searchThread.isAlive()) {
                    searchThread.cancle();
                }
                if (searchKey == null || searchKey.equals("")) {
                    search_file_progress.setVisibility(View.INVISIBLE);
                    listData.clear();
                    initSonData();
                } else {
                    search_file_progress.setVisibility(View.VISIBLE);
                    //????????????
                    searchThread = new SearchRunFile(searchKey, searchRunFileListener);
                    searchThread.start();
                }
                break;
            case 2://????????????????????????
                if (searchThread != null && searchThread.isAlive()) {
                    searchThread.cancle();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected int getMainContentViewId() {
        return R.layout.activity_search;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            savedInstanceState = getIntent().getExtras();
        }
        if (savedInstanceState != null) {
            pick_attachment = savedInstanceState.getBoolean("pick_attachment", false);
            pickAttachStyle = savedInstanceState.getInt("pickAttachStyle", 0);
        }
        silde_layout.setListener(this);

        back_rl.setOnClickListener(this);
        last_page.setOnClickListener(this);
        next_page.setOnClickListener(this);

        grid_items.setOnItemClickListener(this);
        grid_items.setOnItemLongClickListener(this);

        keyword_ed.addTextChangedListener(textWatcher);

        filePopWindow=new FilePopWindow(this,this);
    }

    SearchRunFile.SearchRunFileListener searchRunFileListener = new SearchRunFile.SearchRunFileListener() {
        @Override
        public void onSearcFile(final List<FileModel> list) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listData.clear();
                    listData.addAll(list);
                    initSonData();
                    search_file_progress.setVisibility(View.INVISIBLE);
                }
            });
        }
    };

    public void initSonData() {
        if (isfinish) return;
        //????????????
        totalIndex = listData.size() / pageSize;
        totalIndex += listData.size() % pageSize == 0 ? 0 : 1;

        //??????????????????
        if (CurrentIndex > totalIndex - 1) {
            CurrentIndex = totalIndex - 1;
        }
        if (CurrentIndex < 0) CurrentIndex = 0;
        if (totalIndex == 0) totalIndex = 1;

        if (listData.size() == 0) {
            adapterItems(listData);
        } else if (totalIndex - 1 == CurrentIndex) {
            adapterItems(listData.subList(CurrentIndex * pageSize, listData.size()));
        } else {
            adapterItems(listData.subList(CurrentIndex * pageSize, (CurrentIndex + 1) * pageSize));
        }
        setShowText();
    }

    private void adapterItems(List<FileModel> listModels) {
        if (listModels == null) return;
        sonList.clear();
        sonList.addAll(listModels);
        if (adapter == null) {
            int itemWidth = (grid_items.getWidth() - DensityUtil.dip2px(this, 30) * 4) / 5;
            int itemHeight = (grid_items.getHeight() - DensityUtil.dip2px(this, 30) * 5) / 6;

            adapter = new FileAdapter(this, sonList, itemWidth, itemHeight, false, 0);
            grid_items.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * ??????????????????????????????
     */
    public void setShowText() {
        show_index.setText(String.valueOf(CurrentIndex + 1) + "/" + totalIndex);
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            getHandler().sendEmptyMessage(2);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            getHandler().removeMessages(1);
            searchKey = s.toString();
            getHandler().sendEmptyMessageDelayed(1, 1000);
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //??????????????????
        FileModel fileModel = sonList.get(position);
        if (fileModel.file.isDirectory()) {
            //??????????????????
//            listener.clickFile(fileModel);
        } else {
            if (pick_attachment&&pickAttachStyle==0) {
                //????????????
                    backActivity(0, fileModel.file.getAbsolutePath());
                return;
            }
            //????????????
            switch (fileModel.getFileType()) {
                case 1://??????
                    if (pick_attachment&&pickAttachStyle==1) {
                        //????????????
                        backActivity(0, fileModel.file.getAbsolutePath());
                        return;
                    }
                        ArrayList<String> images = new ArrayList<>();
                        int index = 0;
                        String imagePath = fileModel.file.getAbsolutePath();
                        int i = 0;
                        for (FileModel model : listData) {
                            if (model.getFileType() == 1) {
                                images.add(model.file.getAbsolutePath());
                                if (model.file.getAbsolutePath().equals(imagePath)) index = i;
                                i++;
                            }
                        }
                        CheckImageActivity.startCheck(this, images, index);
                    break;
                case 2://pdf??????
                case 3://txt??????
                    try {
                        if (fileModel.file.exists()) {
                            Intent input = new Intent();
                            String openPath = fileModel.file.getAbsolutePath();
                            input.putExtra("file", openPath);
                            ComponentName cnInput = new ComponentName(PakegeString.bookstore, "com.moxi.bookstore.activity.RecentlyActivity");
                            input.setComponent(cnInput);

                            startActivity(input);
                        } else {
                            MyApplication.Toast("?????????????????????????????????????????????????????????");
                        }
                    } catch (Exception e) {
                        FileUtils.getInstance().openFile(getContext(), fileModel.file);
//                            new StartFile(getActivity(), fileModel.file.getAbsolutePath());
                    }
                    break;
                default://????????????
                    if (fileModel.file.getAbsolutePath().equals(StringUtils.getSDCardPath() + "/update.zip")) {
                        insureDialog("???????????????????????????", "update", new InsureOrQuitListener() {
                            @Override
                            public void isInsure(Object code, boolean is) {
                                if (is) {
                                    Intent input = new Intent();
                                    ComponentName cnInput = new ComponentName("com.onyx.android.onyxotaservice", "com.onyx.android.onyxotaservice.OtaInfoActivity");
                                    input.setComponent(cnInput);
                                    startActivity(input);
                                }
                            }
                        });
                    } else {
                        new StartFile(this, fileModel.file.getAbsolutePath());
                    }
                    break;
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (pick_attachment)return true;
        //?????????????????????
        if ( !ClipboardUtils.getInstance().isHaveEvent) {
            filePopWindow.showManagerOneFilePopupWindow(view, position, sonList.get(position).getFileType());
        }
        return true;
    }

    @Override
    public void onOperate(int style, final int position) {
        final File file = sonList.get(position).file;
        switch (style) {
            case 0://??????
                backActivity(1,file.getAbsolutePath());
                break;
            case 1://??????
                insureDialog("????????????????????????" + file.getName(), file, new InsureOrQuitListener() {
                    @Override
                    public void isInsure(Object code, boolean is) {
                        if (is) {
                            files.add(file.getAbsolutePath());
                            StringUtils.deleteFile((File) code);
                            listData.remove(sonList.get(position));
                            initSonData();
                        }
                    }
                });
                break;
            case 2://??????
                backActivity(2,file.getAbsolutePath());
                break;
            case 3://?????????
                inputTitleDialog(position);
                break;
            case 4://??????
                try {
                    String picPatch = file.getAbsolutePath();
                    Intent intent = new Intent();
                    ComponentName componentName = new ComponentName("com.onyx", "com.onyx.content.browser.activity.ChangeScreenSaverActivity");
                    intent.setComponent(componentName);
                    intent.putExtra("onyx_screen_saver_path", picPatch);
                    startActivity(intent);
                } catch (Exception e) {
                    ToastUtils.getInstance().showToastShort("??????????????????????????????????????????");
                }
                break;
            case 5://?????? ??????????????????????????????????????????????????????
                int fileType = sonList.get(position).getFileType();
                StringBuilder builder = new StringBuilder();
                builder.append("????????????:");
                builder.append(file.getAbsolutePath());
                builder.append("\n");

                builder.append("?????????:");
                builder.append(file.getName());
                builder.append("\n");

//                builder.append("????????????:");
//                builder.append(typeStr);
//                builder.append("\n");
                if (fileType != 0) {
                    long size = 0;
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(file);
                        size = fis.available();
                        builder.append("????????????:");
                        builder.append(android.text.format.Formatter.formatFileSize(getContext(), size));
                        builder.append("\n");
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                new AlertDialog(this).builder().setTitle("??????").setCancelable(false).setMsg(builder.toString()).
                        setNegativeButton("??????", null).show();
                break;
            default:
                break;
        }
    }

    /**
     * ???????????????
     * @param style 0:???????????????????????????1?????????2??????
     * @param path ????????????????????????
     */
    private void backActivity(int style,String path){
        Intent intent= new Intent();
        intent.putExtra("style",style);
        intent.putExtra("path",path);
        setResult(RESULT_OK,intent);
        this.finish();
    }

    @Override
    public void onBackPressed() {
        if (files.size()>0){
            Intent intent= new Intent();
            intent.putExtra("style",3);
            intent.putStringArrayListExtra("paths", files);
            setResult(RESULT_OK,intent);
        }
        super.onBackPressed();
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
        outState.putBoolean("pick_attachment", pick_attachment);
        outState.putInt("pickAttachStyle", pickAttachStyle);
    }

    @Override
    public void onActivityRestoreInstanceState(Bundle savedInstanceState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    @Override
    protected void onDestroy() {
        keyword_ed.removeTextChangedListener(textWatcher);
        super.onDestroy();
        if (searchThread != null && searchThread.isAlive()) {
            searchThread.cancle();
        }
    }

    @Override
    public void newFloder() {

    }

    @Override
    public void selectMore() {

    }

    @Override
    public void onSildeEventLeft() {
        if (CurrentIndex > 0 && (CurrentIndex <= totalIndex - 1)) {
            CurrentIndex--;
            initSonData();
        } else {
            ToastUtils.getInstance().showToastShort("??????????????????");
        }
    }

    @Override
    public void onSildeEventRight() {
        if (CurrentIndex >= totalIndex - 1) {
            ToastUtils.getInstance().showToastShort("?????????????????????");
            return;
        } else {
            CurrentIndex++;
            initSonData();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_PAGE_UP) {
            //?????????
            onSildeEventLeft();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN) {
            //?????????
            onSildeEventRight();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_rl://??????
                onBackPressed();
                break;
            case R.id.last_page:
                onSildeEventLeft();
                break;
            case R.id.next_page:
                onSildeEventRight();
                break;
            default:
                break;
        }
    }

    @Override
    public void moveDirection(boolean left, boolean up, boolean right, boolean down) {

    }
    /**
     * ?????????????????????????????????
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {

            // ???????????????????????????View????????????????????????EditText??????????????????????????????????????????????????????????????????
            View v = getCurrentFocus();

            if (StringUtils.isShouldHideInput(v, ev)) {
                StringUtils.closeIMM(this, v.getWindowToken());
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void inputTitleDialog(final int index) {
        InputDialog.getdialog(this, getString(R.string.re_name), "?????????????????????", new InputDialog.InputListener() {
            @Override
            public void quit() {
            }

            @Override
            public void insure(String name) {
                String prefix = sonList.get(index).getSuffix();
                prefix = prefix.equals("") ? "" : ("." + prefix);
                name += prefix;
                if (ConfigerUtils.isFail(name)) return;
                if (judgeExist(name)) {
                    BaseApplication.Toast("?????????????????????");
                    return;
                }
                String newname = sonList.get(index).file.getParent() + "/" + name;
                sonList.get(index).file.renameTo(new File(newname));
                files.add(sonList.get(index).file.getAbsolutePath());
                /**
                 * ??????????????????
                 */
                sonList.get(index).resetFile(new File(newname));
                initSonData();
            }
        });
    }
    private boolean judgeExist(String name) {
        for (FileModel model : listData) {
            if (model.getFileName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
