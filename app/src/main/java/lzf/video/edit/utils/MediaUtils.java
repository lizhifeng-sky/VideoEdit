package lzf.video.edit.utils;

import android.annotation.TargetApi;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;

import java.io.IOException;

public class MediaUtils {

	/**
	 * 该音频是否符合采样率是sampleRate,通道数是channelCount,值为-1表示忽略该条件
	 * 
	 * @param audioFile
	 * @param sampleRate
	 * @param channelCount
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static boolean isMatchAudioFormat(String audioFile, int sampleRate, int channelCount){
		MediaExtractor mex = new MediaExtractor();
	    try {
	        mex.setDataSource(audioFile);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    
	    MediaFormat mf = mex.getTrackFormat(0);
	    
	    boolean result = true;
	    if(sampleRate != -1){
	    	result = sampleRate == mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);
	    }
	    
	    if(result && channelCount != -1){
	    	result = channelCount == mf.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
	    }
	
	    mex.release();
	    
	    return result;
	}
	
	/**
	 * 生成Wav文件的头部信息
	 * 
	 * @param rawAudioSize
	 * @param sampleRate
	 * @param channels
	 * 
	 */
    public final static byte[] createWaveFileHeader(long rawAudioSize,int channels, long sampleRate, int bitsPerSample){
                                    
        final byte[] header = new byte[44];
        
        //ChunkID : "RIFF"
        header[0] = 'R';   
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        
        //ChunkSize : ChunkSize = 36 + SubChunk2Size
        long totalDataLen = 36 + rawAudioSize;
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        
        //Format
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        
        //Subchunk1ID : "fmt"
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        
        //Subchunk1Size : 16 for PCM
        header[16] = 16; 
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        
        //AudioFormat : PCM = 1
        header[20] = 1; 
        header[21] = 0;
        
        //NumChannels : Mono = 1, Stereo = 2
        header[22] = (byte) channels;
        header[23] = 0;
        
        //SampleRate
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        
        //ByteRate : ByteRate = SampleRate * NumChannels * BitsPerSample/8
        long byteRate = sampleRate *  channels * bitsPerSample / 8;
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        
        //BlockAlign : BlockAlign = NumChannels * BitsPerSample/8
        header[32] = (byte) (2 * 16 / 8); 
        header[33] = 0;
        
        //BitsPerSample : 8 bits = 8, 16 bits = 16, etc
        header[34] = (byte) bitsPerSample;
        header[35] = 0;
        
        //Subchunk2ID : "data"
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        
        //Subchunk2Size : Subchunk2Size = NumSamples * NumChannels * BitsPerSample/8
        header[40] = (byte) (rawAudioSize & 0xff);
        header[41] = (byte) ((rawAudioSize >> 8) & 0xff);
        header[42] = (byte) ((rawAudioSize >> 16) & 0xff);
        header[43] = (byte) ((rawAudioSize >> 24) & 0xff);
        
        return header;
    }
}
