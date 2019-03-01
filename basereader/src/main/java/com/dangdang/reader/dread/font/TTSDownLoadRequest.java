package com.dangdang.reader.dread.font;

import com.dangdang.reader.utils.DangdangConfig;
import com.dangdang.zframework.network.download.DownloadManagerFactory;
import com.dangdang.zframework.network.download.IDownload.GetDownload;

import java.io.File;

/**
 * 字典下载
 * 
 * @author Yhyu
 * @date 2014-12-11 下午1:38:42
 */
public class TTSDownLoadRequest extends GetDownload {
	private long mStartPos;
	private long mTotalSize;
	private String mUrl;
	private File mLocalFile;
	private String mIndentityId;
	private boolean mAddPublicParams = false;
	private DownloadManagerFactory.DownloadModule mModule;

	public TTSDownLoadRequest(DownloadManagerFactory.DownloadModule mModule) {
		this.mModule = mModule;
	}

	@Override
	public long getStartPosition() {
		return mStartPos;
	}

	@Override
	public long getTotalSize() {
		return mTotalSize;
	}

	@Override
	public File getLoaclFile() {
		return mLocalFile;
	}

	@Override
	public String getUrl() {
		return mUrl;
	}

	@Override
	public Object getTag() {
		return mIndentityId;
	}

	public boolean addPublicParams() {
		return mAddPublicParams;
	}

	@Override
	public DownloadManagerFactory.DownloadModule getDownloadModule() {
		return mModule;
	}

	public void setParams(String indentityId, long start, long totalSize, String url, File localFile) {
		mIndentityId = indentityId;
		mStartPos = start;
		mTotalSize = totalSize;
		mLocalFile = localFile;
		if (url == null) {
			StringBuilder builder = new StringBuilder(DangdangConfig.SERVER_EAPI_URL);
			mUrl = builder.toString();
			mAddPublicParams = true;
		} else {
			mUrl = url;
		}
	}
}
