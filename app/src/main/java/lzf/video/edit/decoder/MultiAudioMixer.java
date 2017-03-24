package lzf.video.edit.decoder;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * 目前只能对相同采样率，通道和采样精度的音频进行混音
 * @author Darcy
 */
public abstract class MultiAudioMixer {
	
	private  OnAudioMixListener mOnAudioMixListener;
	
	public static MultiAudioMixer createAudioMixer(){
		return new AverageAudioMixer();
	}
	
	public void setOnAudioMixListener(OnAudioMixListener l){
		this.mOnAudioMixListener = l;
	}
	
	/**
	 * <p>start to mix , you can call {@link #setOnAudioMixListener(OnAudioMixListener)} before this method to get mixed data. 
	 */
	public void mixAudios(File[] rawAudioFiles){
		
		final int fileSize = rawAudioFiles.length;

		FileInputStream[] audioFileStreams = new FileInputStream[fileSize];
		File audioFile = null;
		
		FileInputStream inputStream;
		byte[][] allAudioBytes = new byte[fileSize][];
		boolean[] streamDoneArray = new boolean[fileSize];
		byte[] buffer = new byte[512];
		int offset;
		
		try {
			
			for (int fileIndex = 0; fileIndex < fileSize; ++fileIndex) {
				audioFile = rawAudioFiles[fileIndex];
				audioFileStreams[fileIndex] = new FileInputStream(audioFile);
			}

			while(true){
				
				for(int streamIndex = 0 ; streamIndex < fileSize ; ++streamIndex){
					
					inputStream = audioFileStreams[streamIndex];
					if(!streamDoneArray[streamIndex] && (offset = inputStream.read(buffer)) != -1){
						allAudioBytes[streamIndex] = Arrays.copyOf(buffer,buffer.length);
					}else{
						streamDoneArray[streamIndex] = true;
						allAudioBytes[streamIndex] = new byte[512];
					}
				}
				
				byte[] mixBytes = mixRawAudioBytes(allAudioBytes);
				if(mixBytes != null && mOnAudioMixListener != null){
					mOnAudioMixListener.onMixing(mixBytes);
				}
				
				boolean done = true;
				for(boolean streamEnd : streamDoneArray){
					if(!streamEnd){
						done = false;
					}
				}
				
				if(done){
					if(mOnAudioMixListener != null)
						mOnAudioMixListener.onMixComplete();
					break;
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			if(mOnAudioMixListener != null)
				mOnAudioMixListener.onMixError(1);
		}finally{
			try {
				for(FileInputStream in : audioFileStreams){
					if(in != null)
						in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	abstract byte[] mixRawAudioBytes(byte[][] data);
	
	public interface OnAudioMixListener{
		/**
		 * invoke when mixing, if you want to stop the mixing process, you can throw an AudioMixException
		 * @param mixBytes
		 * @throws AudioMixException
		 */
		void onMixing(byte[] mixBytes) throws IOException;
		
		void onMixError(int errorCode);
		
		/**
		 * invoke when mix success
		 */
		void onMixComplete();
	}
	
	public static class AudioMixException extends IOException{
		private static final long serialVersionUID = -1344782236320621800L;
		
		public AudioMixException(String msg){
			super(msg);
		}
	}
	
	/**
	 * 平均值算法
	 * @author Darcy
	 */
	private static class AverageAudioMixer extends MultiAudioMixer{

		@Override
		byte[] mixRawAudioBytes(byte[][] bMulRoadAudioes) {
			
			if (bMulRoadAudioes == null || bMulRoadAudioes.length == 0)
				return null;

			byte[] realMixAudio = bMulRoadAudioes[0];
			
			if(bMulRoadAudioes.length == 1)
				return realMixAudio;
			
			for(int rw = 0 ; rw < bMulRoadAudioes.length ; ++rw){
				if(bMulRoadAudioes[rw].length != realMixAudio.length){
					Log.e("app", "column of the road of audio + " + rw +" is diffrent.");
					return null;
				}
			}
			
			int row = bMulRoadAudioes.length;
			int coloum = realMixAudio.length / 2;
			short[][] sMulRoadAudioes = new short[row][coloum];

			for (int r = 0; r < row; ++r) {
				for (int c = 0; c < coloum; ++c) {
					sMulRoadAudioes[r][c] = (short) ((bMulRoadAudioes[r][c * 2] & 0xff) | (bMulRoadAudioes[r][c * 2 + 1] & 0xff) << 8);
				}
			}

			short[] sMixAudio = new short[coloum];
			int mixVal;
			int sr = 0;
			for (int sc = 0; sc < coloum; ++sc) {
				mixVal = 0;
				sr = 0;
				for (; sr < row; ++sr) {
					mixVal += sMulRoadAudioes[sr][sc];
				}
				sMixAudio[sc] = (short) (mixVal / row);
			}

			for (sr = 0; sr < coloum; ++sr) {
				realMixAudio[sr * 2] = (byte) (sMixAudio[sr] & 0x00FF);
				realMixAudio[sr * 2 + 1] = (byte) ((sMixAudio[sr] & 0xFF00) >> 8);
			}

			return realMixAudio;
		}
		
	}
}
