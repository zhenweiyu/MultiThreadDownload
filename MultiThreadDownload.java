package MultiThreadDownload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Created by zhenweiyu on 2017/5/11.
 */
public class MultiThreadDownload {


    private CountDownLatch countDownLatch = null;

    public void download(String url,int threadNum){

        countDownLatch = new CountDownLatch(threadNum);
        try {
            URL connectUrl = new URL(url);
            try {
                URLConnection urlConnection = connectUrl.openConnection();
                HttpURLConnection httpURLConnection = (HttpURLConnection)urlConnection;
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setRequestProperty("Accept-Encoding","identity");
                httpURLConnection.setDoInput(true);
                httpURLConnection.setConnectTimeout(5000);
                printHttpHeader(httpURLConnection);
                if(httpURLConnection.getResponseCode()==200){
                    int fileLength = httpURLConnection.getContentLength();
                    File file = new File(getFileName(url));
                    RandomAccessFile randomAccessFile = new RandomAccessFile(file,"rwd");
                    randomAccessFile.setLength(fileLength);
                    randomAccessFile.close();
                    int part = fileLength/threadNum;
                    int remainPart = fileLength%threadNum;//the last thread download this part;
                    int start = 0;
                    int end = part-1;
                    for(int threadIndex = 0;threadIndex<threadNum;threadIndex++){
                        Thread downloadThread = new DownloadThread(url,file,start,end);
                        downloadThread.start();
                        start = end+1;
                        if(threadIndex!=threadNum-1){
                            end += part;
                        }
                        else {
                            end += part+remainPart;
                        }
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                System.out.println("downloading...");
                                countDownLatch.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
//                            if(checkFileCompleted(file,fileLength)){
//                                System.out.println("download successfully!");
//                            }else {
//                                System.out.println("download failure");
//                            }
                            System.out.println("download successfully!");
                        }
                    }).start();

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    class DownloadThread extends Thread{
        private URL url;
        private File file;
        private int start;
        private int end;

        public DownloadThread(String urlStr,File file,int start,int end){
            try {
                url = new URL(urlStr);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            this.file = file;
            this.start = start;
            this.end = end;
        }

        @Override
        public void run() {
            try {
                URLConnection urlConnection = url.openConnection();
                HttpURLConnection httpURLConnection = (HttpURLConnection)urlConnection;
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setDoInput(true);
                httpURLConnection.setRequestProperty("Rang",String.format("bytes=%d-%d",start,end));
                RandomAccessFile randomAccessFile = new RandomAccessFile(file,"rwd");
                randomAccessFile.seek(start);
                if(httpURLConnection.getResponseCode()==200){
                    InputStream inputStream = httpURLConnection.getInputStream();
                    byte []buffer = new byte[1024];
                    int len = 0;
                    while ((len = inputStream.read(buffer))!=-1){
                        randomAccessFile.write(buffer,0,len);
                    }
                    randomAccessFile.close();
                    inputStream.close();
                }else{
                    System.out.println(Thread.currentThread().getName()+"download fail");

                }

            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                countDownLatch.countDown();
            }
        }
    }


    private String getFileName(String url){
        return url.substring(url.lastIndexOf("/")+1);
    }

    private void printHttpHeader(HttpURLConnection connection){
        Map<String, List<String>> headerFields = connection.getHeaderFields();
        Set<Map.Entry<String, List<String>>> entrySet = headerFields.entrySet();
        Iterator<Map.Entry<String, List<String>>> iterator = entrySet.iterator();
        while(iterator.hasNext()) {
            Map.Entry<String, List<String>> next = iterator.next();
            String key=next.getKey();
            List<String> value = next.getValue();
            if(key==null)
                System.out.println(value.toString());
            else
                System.out.println(key+":"+value.toString());
        }
    }


//    private boolean checkFileCompleted(File file,int fileLength){
////       System.out.println(file.length()+" "+fileLength);
////       if(file.length()==fileLength){
////           return true;
////       }return false;
//        return false;
//    }


    public static void main(String []args){
        MultiThreadDownload multiThreadDownload = new MultiThreadDownload();
        multiThreadDownload.download("http://dl.softmgr.qq.com/original/desktop/sogou_wallpaper_2.5j-2.5.4.2687.exe",4);
    }



}
