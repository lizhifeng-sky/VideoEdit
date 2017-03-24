package lzf.video.edit.decoder;

/**
 * Created by Administrator on 2017/3/23 0023.
 */
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * 使用注意事项
 * @作者 胡楠启
 * 在android中操作肯定需要操作SD卡的权限的。
 *调用顺序：
 *1、fenLiData//只要调用了它就会产生中间文件
 *2、initMP3Frame
 *3、CutingMp3
 *4、在调用端，切割完毕后删除中间产生的垃圾文件
 *String fenLiData = CutingMp3.fenLiData(str1);
 *File file=new File(fenLiData);
 *if(file.exists())file.delete();
 *原因是在工具端删除中间文件时，这个删除失败。懒得继续画精力去找 ，所以必须在调用端切割完毕后删除，
 *一避免垃圾文件占用内存
 */

public class CustomAudioDecoder {
    /**
     * 返回分离出MP3文件中的数据帧的文件路径
     */
    public static String fenLiData(String path) throws IOException {
        File file = new File(path);// 原文件
        File file1 = new File(path + "01");// 分离ID3V2后的文件,这是个中间文件，最后要被删除
        File file2 = new File(path + "001");// 分离id3v1后的文件
        RandomAccessFile rf = new RandomAccessFile(file, "rw");// 随机读取文件
        FileOutputStream fos = new FileOutputStream(file1);
        byte ID3[] = new byte[3];
        rf.read(ID3);
        String ID3str = new String(ID3);
        // 分离ID3v2
        if (ID3str.equals("ID3")) {
            rf.seek(6);
            byte[] ID3size = new byte[4];
            rf.read(ID3size);
            int size1 = (ID3size[0] & 0x7f) << 21;
            int size2 = (ID3size[1] & 0x7f) << 14;
            int size3 = (ID3size[2] & 0x7f) << 7;
            int size4 = (ID3size[3] & 0x7f);
            int size = size1 + size2 + size3 + size4 + 10;
            rf.seek(size);
            int lens = 0;
            byte[] bs = new byte[1024*4];
            while ((lens = rf.read(bs)) != -1) {
                fos.write(bs, 0, lens);
            }
            fos.close();
            rf.close();
        } else {// 否则完全复制文件
            int lens = 0;
            rf.seek(0);
            byte[] bs = new byte[1024*4];
            while ((lens = rf.read(bs)) != -1) {
                fos.write(bs, 0, lens);
            }
            fos.close();
            rf.close();
        }
        RandomAccessFile raf = new RandomAccessFile(file1, "rw");
        byte TAG[] = new byte[3];
        raf.seek(raf.length() - 128);
        raf.read(TAG);
        String tagstr = new String(TAG);
        if (tagstr.equals("TAG")) {
            FileOutputStream fs = new FileOutputStream(file2);
            raf.seek(0);
            byte[] bs=new byte[(int)(raf.length()-128)];
            raf.read(bs);
            fs.write(bs);
            raf.close();
            fs.close();
        } else {// 否则完全复制内容至file2
            FileOutputStream fs = new FileOutputStream(file2);
            raf.seek(0);
            byte[] bs = new byte[1024*4];
            int len = 0;
            while ((len = raf.read(bs)) != -1) {
                fs.write(bs, 0, len);
            }
            raf.close();
            fs.close();
        }
        if (file1.exists())// 删除中间文件
        {
            file1.delete();
        }
        return file2.getAbsolutePath();
    }

    /**
     * 分离出数据帧每一帧的大小并存在list数组里面
     * 失败则返回空
     * @param path
     * @return
     * @throws IOException
     */
    public static List<Integer> initMP3Frame(String path)  {
        File file = new File(path);
        List<Integer> list = new ArrayList<>();
		/*	int framSize=0;
			RandomAccessFile rad = new RandomAccessFile(file, "rw");
			byte[] head = new byte[4];
			rad.seek(framSize);
			rad.read(head);
			int bitRate = getBitRate((head[2] >> 4) & 0x0f) * 1000;
			int sampleRate = getSampleRate((head[2] >> 2) & 0x03);
			int paing = (head[2] >> 1) & 0x01;
			int len = 144 * bitRate / sampleRate + paing;
			for(int i=0,lens=(int)(file.length())/len;i<lens;i++){
				list.add(len);// 将数据帧的长度添加进来
			}*/
        int framSize = 0;
        RandomAccessFile rad = null;
        try {
            rad = new RandomAccessFile(file, "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while (framSize < file.length()) {
            byte[] head = new byte[4];
            try {
                rad.seek(framSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                rad.read(head);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int bitRate = getBitRate((head[2] >> 4) & 0x0f) * 1000;
            int sampleRate = getSampleRate((head[2] >> 2) & 0x03);
            int paing = (head[2] >> 1) & 0x01;
            if(bitRate==0||sampleRate==0)return null;
            int len = 144 * bitRate / sampleRate + paing;
            list.add(len);// 将数据帧的长度添加进来
            framSize += len;
        }
        return list;
    }

    /**
     * 返回切割后的MP3文件的路径 返回null则切割失败 开始时间和结束时间的整数部分都是秒，以秒为单位
     *
     *
     * @param list
     * @param startTime
     * @param stopTime
     * @return
     * @throws IOException
     */
    public static String CutingMp3(String path, String name,
                                   List<Integer> list, double startTime, double stopTime)
            throws IOException {
        File file = new File(path);
        String luJing= Environment.getExternalStorageDirectory().getPath();
        File f=new File(luJing);
        f.mkdirs();
        int start = (int) (startTime / 0.026);
        int stop = (int) (stopTime / 0.026);
        if ((start > stop) || (start < 0) || (stop < 0) || (stop > list.size())) {
            return null;
        } else {
            long seekStart = 0;// 开始剪切的字节的位置
            for (int i = 0; i < start; i++) {
                seekStart += list.get(i);
            }
            long seekStop = 0;// 结束剪切的的字节的位置
            for (int i = 0; i < stop; i++) {
                seekStop += list.get(i);
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.seek(seekStart);
            File file1 = new File(luJing, "music_分割.mp3");
            FileOutputStream out = new FileOutputStream(file1);
            byte[] bs=new byte[(int)(seekStop-seekStart)];
            raf.read(bs);
            out.write(bs);
            raf.close();
            out.close();
            File filed=new File(path);
            if(filed.exists())
                filed.delete();
            return file1.getAbsolutePath();
        }

    }

    private static int getBitRate(int i) {
        int a[] = {0,32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224,
                256, 320,0 };
        return a[i];
    }

    private static int getSampleRate(int i) {
        int a[] = { 44100, 48000, 32000,0 };
        return a[i];
    }
    /**
     * 返回合并后的文件的路径名,默认放在第一个文件的目录下
     * @param path
     * @param path1
     * @param name
     * @return
     * @throws IOException
     */
    public static String heBingMp3(String path,String path1,String name) throws IOException{
        String fenLiData = fenLiData(path);
        String fenLiData2 = fenLiData(path1);
        File file=new File(fenLiData);
        File file1=new File(fenLiData2);
        String luJing="/storage/emulated/0/"+"HH音乐播放器/合并/";
        File f=new File(luJing);
        f.mkdirs();
        //生成处理后的文件
        File file2=new File(luJing+name+"(HH合并).mp3");
        FileInputStream in=new FileInputStream(file);
        FileOutputStream out=new FileOutputStream(file2);
        byte bs[]=new byte[1024*4];
        int len=0;
        //先读第一个
        while((len=in.read(bs))!=-1){
            out.write(bs,0,len);
        }
        in.close();
        out.close();
        //再读第二个
        in=new FileInputStream(file1);
        out=new FileOutputStream(file2,true);//在文件尾打开输出流
        len=0;
        byte bs1[]=new byte[1024*4];
        while((len=in.read(bs1))!=-1){
            out.write(bs1,0,len);
        }
        in.close();
        out.close();
        if(file.exists())file.delete();
        if(file1.exists())file1.delete();
        return file2.getAbsolutePath();
    }
}


