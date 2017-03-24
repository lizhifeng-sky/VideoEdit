package lzf.video.edit.decoder;

import java.io.IOException;

/**
 * 音频解码器
 * @author Darcy
 *
 */
public abstract class AudioDecoder {
	 
	 String mEncodeFile;
	 
	 OnAudioDecoderListener mOnAudioDecoderListener;
	 
	 AudioDecoder(String encodefile){
		 this.mEncodeFile = encodefile;
	 }
	 
	 public static AudioDecoder createDefualtDecoder(String encodefile){
		 return new AndroidAudioDecoder(encodefile);
	 } 
	 
	 public void setOnAudioDecoderListener(OnAudioDecoderListener l ){
		 this.mOnAudioDecoderListener = l;
	 }
	 
	 /**
	  * 解码
	  * @return
	  * @throws IOException
	  */
	 public abstract RawAudioInfo decodeToFile(String outFile) throws IOException;
	 
	 public static class RawAudioInfo{
		 public String tempRawFile;
		 public int size;
		 public long sampleRate;
		 public int channel;
	 }
	 
	 public interface OnAudioDecoderListener{
		 /**
		  * monitor when processing decode
		  * @param decodedBytes
		  * @param progress range 0~1
		  * @throws IOException
		  */
		 void onDecode(byte[] decodedBytes, double progress) throws IOException;
	 }
}
