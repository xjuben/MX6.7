package com.mx.exams.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.mx.exams.R;
import com.mx.exams.adapter.HistoryAdapter;
import com.mx.exams.cache.ACache;
import com.mx.exams.db.DownDbService;
import com.mx.exams.db.SQLUtil;
import com.mx.exams.http.HttpVolleyCallback;
import com.mx.exams.http.VolleyHttpUtil;
import com.mx.exams.model.HistoryModel;
import com.mx.exams.utils.LocationPhotoInstance;
import com.mx.mxbase.base.BaseActivity;
import com.mx.mxbase.constant.Constant;
import com.mx.mxbase.utils.FileUtils;
import com.mx.mxbase.utils.MXUamManager;
import com.mx.mxbase.utils.Toastor;
import com.mx.mxbase.view.AlertDialog;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;

/**
 * Created by zhengdelong on 16/9/29.
 */

public class MXHistoryActivity extends BaseActivity {

    int page;
    int pageCount;
    int rowCount;

    private ListView data_list_view;
    private HistoryAdapter historyAdapter;

    private LinearLayout ll_overall_back;

    private ImageView img_exams_list_left;
    private ImageView img_exams_list_right;
    private TextView tv_exams_list_page_count;

    private String subjectName = "";
    private String subId = "";

    private String cacheImgPath = FileUtils.getInstance().getDataFilePath();

    @Override
    protected int getMainContentViewId() {
        return R.layout.mx_activity_overall;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        initView();
        initData();
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
        dialogShowOrHide(false, "???????????????...");
    }

    @Override
    public void onActivitySaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {

    }

    @Override
    public void onActivityRestoreInstanceState(Bundle savedInstanceState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    private void initView() {
        subjectName = this.getIntent().getStringExtra("subjectName");
        ll_overall_back = (LinearLayout) findViewById(R.id.ll_overall_back);
        img_exams_list_left = (ImageView) findViewById(R.id.img_exams_list_left);
        img_exams_list_right = (ImageView) findViewById(R.id.img_exams_list_right);
        tv_exams_list_page_count = (TextView) findViewById(R.id.tv_exams_list_page_count);
        data_list_view = (ListView) findViewById(R.id.data_list_view);
        ll_overall_back.setOnClickListener(backClick);
    }

    private void initData() {
        subId = this.getIntent().getStringExtra("sub_id");
        getHistoryData(MXUamManager.queryUser(this), 1, subId);
    }

    private void setData(final List<HistoryModel> data) {
        if (data.size() > 0) {
            img_exams_list_left.setOnClickListener(img_exams_list_leftClick);
            img_exams_list_right.setOnClickListener(img_exams_list_rightClick);
        }

        historyAdapter = new HistoryAdapter(this, data);
        data_list_view.setAdapter(historyAdapter);
        tv_exams_list_page_count.setText(page + "/" + pageCount);
        data_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    if (data.get(i).getType() == 0) {
                        String bookId = SQLUtil.getInstance(MXHistoryActivity.this).getBookIdByCchId(data.get(i).getCchId() + "");
                        File dbfile = new File(DownDbService.getFilePathWithId(Integer.parseInt(bookId)));
                        if (!dbfile.exists()) {
                            Toastor.showToast(MXHistoryActivity.this, "???????????????????????????????????????????????????");
                        } else {
                            dialogShowOrHide(true, "???????????????...");
                            Intent intent = new Intent();
                            intent.setClass(MXHistoryActivity.this, MXWriteHomeWorkActivity.class);
                            intent.putExtra("book_id", bookId);
                            intent.putExtra("mid_title", data.get(i).getTitle());
                            intent.putExtra("cch_id", data.get(i).getCchId());
                            MXHistoryActivity.this.startActivity(intent);
                        }
                    } else {
                        dialogShowOrHide(true, "???????????????...");
                        Intent exams = new Intent(MXHistoryActivity.this, ExamsTestActivity.class);
                        exams.putExtra("test_pap_id", data.get(i).getCobId() + "");
                        exams.putExtra("test_pap_name", data.get(i).getTitle());
                        exams.putExtra("test_pap_cprId", "0");
                        startActivity(exams);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        data_list_view.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {
                new AlertDialog(MXHistoryActivity.this).builder().setTitle("??????").setMsg("??????????????????????").setCancelable(false).setNegativeButton("??????", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        JSONArray jsonArray = new JSONArray();
                        JSONObject jsonObject = new JSONObject();
                        dialogShowOrHide(true, "???????????????...");
                        try {
                            jsonObject.put("type", data.get(i).getType());
                            jsonObject.put("id", data.get(i).getId());
                            jsonArray.put(jsonObject);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        HashMap<String, String> del = new HashMap<String, String>();
                        del.put("appSession", MXUamManager.queryUser(MXHistoryActivity.this));
                        del.put("param", jsonArray.toString());
                        OkHttpUtils.post().url(Constant.DEL_HISTORY_EXAMS).params(del).build().connTimeOut(10000).execute(new StringCallback() {
                            @Override
                            public void onError(Call call, Exception e, int id) {
                                dialogShowOrHide(false, "???????????????...");
                                Toastor.showToast(MXHistoryActivity.this, "????????????");
                            }

                            @Override
                            public void onResponse(String response, int id) {
                                Toastor.showToast(MXHistoryActivity.this, "????????????");
                                if (data.get(i).getType() == 1) {
                                    for (int j = 0; j < 40; j++) {
                                        if (ACache.get(MXHistoryActivity.this).getAsString("papId" + data.get(i).getCobId() + j) != null) {
                                            ACache.get(MXHistoryActivity.this).remove("papId" + data.get(i).getCobId() + j);
                                        }
                                    }
                                    LocationPhotoInstance.getInstance().CleatrPhotoMirks(cacheImgPath + "savePap/" + data.get(i).getCobId() + "/");
                                } else {
                                    for (int j = 0; j < 40; j++) {
                                        if (ACache.get(MXHistoryActivity.this).getAsString("cchId" + data.get(i).getCchId() + j) != null) {
                                            ACache.get(MXHistoryActivity.this).remove("cchId" + data.get(i).getCchId() + j);
                                        }
                                    }
                                    LocationPhotoInstance.getInstance().CleatrPhotoMirks(cacheImgPath + "saveSync/" + data.get(i).getCchId() + "/");
                                }
                                getHistoryData(MXUamManager.queryUser(MXHistoryActivity.this), 1, subId);
                            }
                        });
                    }
                }).setPositiveButton("??????", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                }).show();
                return true;
            }
        });
    }


    View.OnClickListener backClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MXHistoryActivity.this.finish();
        }
    };

    View.OnClickListener img_exams_list_leftClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (page <= 1) {
                return;
            }
            getHistoryData(MXUamManager.queryUser(MXHistoryActivity.this), page - 1, subId);
        }
    };

    View.OnClickListener img_exams_list_rightClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (page >= pageCount) {
                return;
            }
            getHistoryData(MXUamManager.queryUser(MXHistoryActivity.this), page + 1, subId);
        }
    };

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_PAGE_UP:
                if (page <= 1) {
                } else {
                    getHistoryData(MXUamManager.queryUser(MXHistoryActivity.this), page - 1, subId);
                }
                return true;
            case KeyEvent.KEYCODE_PAGE_DOWN:
                if (page >= pageCount) {
                } else {
                    getHistoryData(MXUamManager.queryUser(MXHistoryActivity.this), page + 1, subId);
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                MXHistoryActivity.this.finish();
                return true;
        }
        return false;
    }

    private void getHistoryData(String appSession, int page, String subId) {

        HashMap<String, String> param = new HashMap<>();
        param.put("page", page + "");
        param.put("rows", 17 + "");
        param.put("subId", subId);
        param.put("appSession", appSession);
        final String url = Constant.HISTORYURL + page;
        VolleyHttpUtil.post(this, Constant.HISTORYURL, param, new HttpVolleyCallback() {
            @Override
            public void onSuccess(String data) {
                dialogShowOrHide(false, "???????????????...");
                Log.d("ex", "data===>" + data);
                ACache.get(MXHistoryActivity.this).put(url, data);
                setData(parseHistoryData(data));
            }

            @Override
            public void onFilad(String msg) {
                dialogShowOrHide(false, "???????????????...");
                if (ACache.get(MXHistoryActivity.this).getAsString(url) != null) {
                    String data = ACache.get(MXHistoryActivity.this).getAsString(url);
                    setData(parseHistoryData(data));
                } else {
                    Toastor.showToast(MXHistoryActivity.this, "????????????????????????");
                }
            }
        });

    }

    private List<HistoryModel> parseHistoryData(String data) {
        List<HistoryModel> dataList = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(data);
            int code = jsonObject.optInt("code", -1);
            if (code == 0) {
                JSONObject jsonObject1 = jsonObject.optJSONObject("result");
                int page = jsonObject1.optInt("page", -1);
                int pageCount = jsonObject1.optInt("pageCount", -1);
                int rowCount = jsonObject1.optInt("rowCount", -1);
                this.page = page;
                this.pageCount = pageCount;
                this.rowCount = rowCount;
                JSONArray jsonArray = jsonObject1.optJSONArray("list");
                HistoryModel historyModel;
                for (int i = 0; i < jsonArray.length(); i++) {
                    historyModel = new HistoryModel();
                    JSONObject liObj = jsonArray.getJSONObject(i);
                    historyModel.setTitle(liObj.optString("title"));
                    historyModel.setCreatetime(liObj.optString("createtime"));
                    historyModel.setId(liObj.optInt("id"));
                    historyModel.setMemId(liObj.optInt("memId"));
                    historyModel.setType(liObj.optInt("type"));
                    historyModel.setCobId(liObj.optInt("cobId"));
                    historyModel.setCchId(liObj.optInt("cchId"));
                    if (subjectName != null && !subjectName.equals("")) {
                        if (historyModel.getTitle().contains(subjectName)) {
                            dataList.add(historyModel);
                        }
                    } else {
                        dataList.add(historyModel);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toastor.showToast(this, "??????????????????");
        }
        return dataList;
    }
}
