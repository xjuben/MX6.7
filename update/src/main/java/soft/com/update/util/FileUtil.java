package soft.com.update.util;

import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by zhengdelong on 2016/10/26.
 */

public class FileUtil {

//    private static final String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
//            + File.separator + "mx" + File.separator;
//    private static final String fileName = "log.txt";

    public void saveCode(String code) {
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + "mx" + File.separator;
        String fileName = "log.txt";
        Log.d("update","code===>" + code);
        Log.d("update","filePath===>" + filePath);
        File file = new File(filePath + fileName);
        if (file.exists()){
            file.delete();
        }
        Log.d("update","saveCode ====> " + code);
        writeTxtToFile(code, filePath, fileName);
    }

    public void saveVersionName(String name){
        try{
            String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + File.separator + "yi.txt";
            File file = new File(filePath);
            Log.d("update","uuuu1111");
            if (file.exists()){
                Log.d("update","uuuu2222");
                file.delete();
            }
            if (!file.exists()){
                Log.d("update","uuuu3333");
                file.createNewFile();
            }
            FileOutputStream fout = new FileOutputStream(filePath);
            byte[] bytes = name.getBytes();
            fout.write(bytes,0,bytes.length);
            fout.close();
        }catch (Exception e){
            Log.d("update","uuuu====>" + e.getMessage());
        }

    }

    public String getCode(){
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + "mx" + File.separator;
        String fileName = "log.txt";
        StringBuffer sb = new StringBuffer();
        try{
            File file=new File(filePath + fileName);
            if(!file.exists()||file.isDirectory())
                throw new FileNotFoundException();
            FileInputStream fis=new FileInputStream(file);
            byte[] buf = new byte[1024];
            while((fis.read(buf))!=-1){
                sb.append(new String(buf));
//                buf=new byte[1024];//???????????????????????????????????????????????????
            }
        }catch (Exception e){
            return "";
        }

        return sb.toString();
    }

    // ????????????????????????????????????
    public void writeTxtToFile(String strcontent, String filePath, String fileName) {
        //?????????????????????????????????????????????????????????
        makeFilePath(filePath, fileName);

        String strFilePath = filePath + fileName;
        try {
            File file = new File(strFilePath);
            if (!file.exists()) {
//                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            FileOutputStream fout = new FileOutputStream(strFilePath);
            byte[] bytes = strcontent.getBytes();
            fout.write(bytes,0,bytes.length);
            fout.close();

        } catch (Exception e) {
            Log.d("update","writeTxtToFile Exception===>" + e.getMessage());

        }
    }

    // ????????????
    public File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            Log.d("update","makeFilePath Exception===>" + e.getMessage());
        }
        return file;
    }

    // ???????????????
    public void makeRootDirectory(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            Log.d("update","makeRootDirectory Exception==>" + e.getMessage());
        }
    }
}
