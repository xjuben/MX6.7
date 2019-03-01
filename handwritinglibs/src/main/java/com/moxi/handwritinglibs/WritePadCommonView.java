package com.moxi.handwritinglibs;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.AttributeSet;

import com.moxi.handwritinglibs.asy.SaveCommonWrite;
import com.moxi.handwritinglibs.listener.DbPhotoListener;
import com.moxi.handwritinglibs.model.CodeAndIndex;
import com.moxi.handwritinglibs.utils.DbPhotoLoader;

/**
 * 绘制保存手写笔记
 * Created by 夏君 on 2017/2/9.
 */
public class WritePadCommonView extends WritePadCommonBaseView{

    public WritePadCommonView(Context context) {
        super(context);
    }

    public WritePadCommonView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    /**
     * 设置保存绘制的唯一标识
     * @param saveCode 保存绘制的唯一标识
     */
    public void setSaveCode(final String saveCode) {
//        onResume();
        setCodeAndIndex(new CodeAndIndex(saveCode,0));
        DbPhotoLoader.getInstance().loaderPhoto(saveCode,0, new DbPhotoListener() {
            @Override
            public void onLoaderSucess(String saveCodes,int index, Bitmap bitmap) {
                if (saveCode==saveCodes) {
                    setBitmap(bitmap);
                }
//                onPause();
            }
        });
    }

    /**
     * 保存笔记
     * @param name 笔记名称
     */
    public void saveWritePad(String name){
        if (null==getSaveCode())return;
        Bitmap bitmap=getBitmap();
        if (bitmap==null)return;
        //更改缓存里面的图片信息
        DbPhotoLoader.getInstance().addBitmapToLruCache(getSaveCode(),bitmap);
        //异步线程修改保存图片数据信息
        new SaveCommonWrite(name,getSaveCode(),bitmap).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
