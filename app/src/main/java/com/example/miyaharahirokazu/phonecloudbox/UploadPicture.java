package com.example.miyaharahirokazu.phonecloudbox;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxFileSizeException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created by miyaharahirokazu on 15/09/02.
 */
public class UploadPicture extends AsyncTask<Void,Long,Boolean>{

    private DropboxAPI<?> mApi;
    private String mPath;
    private File mFile;

    private long mFileLen;
    private DropboxAPI.UploadRequest mRequest;
    private Context mContext;
    private final ProgressDialog mDialog;

    private String mErrorMsg;


    public UploadPicture(Context context, DropboxAPI<?> api, String dropboxPath, File file) {

        mContext =context.getApplicationContext();

        mFileLen = file.length();
        this.mApi = api;
        mPath = dropboxPath;
        mFile = file;

        mDialog = new ProgressDialog(context);
        mDialog.setMax(100);
        mDialog.setMessage("Uploading" + file.getName());
        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDialog.setProgress(0);
        mDialog.setButton(ProgressDialog.BUTTON_POSITIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cancel();
            }
        });
        mDialog.show();
    }

    private void cancel() {
        if(mRequest != null){
            new Thread(){
                @Override
                public void run(){
                    mRequest.abort();
                }
            }.start();
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try{
            FileInputStream fis = new FileInputStream(mFile);
            String path = mPath + mFile.getName();
            mRequest = mApi.putFileOverwriteRequest(path, fis, mFile.length(), new ProgressListener() {
                @Override
                public long progressInterval(){
                    return 500;
                }
                @Override
                public void onProgress(long bytes, long total) {
                    publishProgress(bytes);
                }
            });
            if(mRequest != null){
                mRequest.upload();
                return true;
            }
        }catch(DropboxUnlinkedException e){

            mErrorMsg = "This app was not authenticated properly.";
        }catch (DropboxFileSizeException e){

            mErrorMsg = "This file is too bit to upload.";
        }catch (DropboxPartialFileException e){

            mErrorMsg = "Upload canceled";
        }catch (DropboxServerException e){

            switch (e.error){
                case DropboxServerException._401_UNAUTHORIZED:
                    break;
                case DropboxServerException._403_FORBIDDEN:
                    break;
                case DropboxServerException._404_NOT_FOUND:
                    break;
                case DropboxServerException._507_INSUFFICIENT_STORAGE:
                    break;
                default:
                    break;
            }
            mErrorMsg = e.body.userError;
            if(mErrorMsg == null){
                mErrorMsg = e.body.error;
            }
        }catch (DropboxIOException e){

            mErrorMsg = "Network error. Try again.";
        }catch(DropboxParseException e){

            mErrorMsg = "Dropbox error. Try again.";
        }catch(DropboxException e){

            mErrorMsg = "Unknown error. Try again.";
        }catch (FileNotFoundException e){
            mErrorMsg = "File was not Found.";
        }
        return false;
    }

    @Override
    protected void onProgressUpdate(Long... progress){
        int percent = (int)(100.0*(double)progress[0]/mFileLen + 0.5);
        mDialog.setProgress(percent);
    }

    protected void onPostExecute(Boolean result){
        mDialog.dismiss();
        if(result){
            showToast("写真をアップしました");
        }else {
            showToast(mErrorMsg);
        }
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(mContext,msg,Toast.LENGTH_LONG);
        error.show();
    }
}
