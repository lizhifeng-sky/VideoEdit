package lzf.video.edit.decoder;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Android SDK 支持的编码器
 * @author Darcy
 * @version  android.os.Build.VERSION.SDK_INT >= 16
 */
public class AndroidAudioDecoder extends AudioDecoder{
	
	private static final String TAG = "AndroidAudioDecoder";
	
	AndroidAudioDecoder(String encodefile) {
		super(encodefile);
	}

	@Override
	public RawAudioInfo decodeToFile(String outFile) throws IOException{
		
		long beginTime = System.currentTimeMillis();
		
		final String encodeFile = mEncodeFile;
		MediaExtractor extractor = new MediaExtractor();
		extractor.setDataSource(encodeFile);
		
		MediaFormat mediaFormat = null;
		for (int i = 0; i < extractor.getTrackCount(); i++) {
			MediaFormat format = extractor.getTrackFormat(i);
			String mime = format.getString(MediaFormat.KEY_MIME);
			if (mime.startsWith("audio/")) {
				extractor.selectTrack(i);
				mediaFormat = format;
				break;
			}
		}
		
		if(mediaFormat == null){
			extractor.release();
			return null;
		}
		
        RawAudioInfo rawAudioInfo = new RawAudioInfo();
        rawAudioInfo.channel = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        rawAudioInfo.sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        rawAudioInfo.tempRawFile = outFile;
        FileOutputStream fosDecoder = new FileOutputStream(rawAudioInfo.tempRawFile);
        
        String mediaMime = mediaFormat.getString(MediaFormat.KEY_MIME);
        MediaCodec codec = MediaCodec.createDecoderByType(mediaMime);
        codec.configure(mediaFormat, null, null, 0);
        codec.start();
        
        ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();
        
        final double audioDurationUs = mediaFormat.getLong(MediaFormat.KEY_DURATION);
        final long kTimeOutUs = 5000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int totalRawSize = 0;
        try{
			while (!sawOutputEOS) {
			    if (!sawInputEOS) {
			        int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
			        if (inputBufIndex >= 0) {
			            ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
			            int sampleSize = extractor.readSampleData(dstBuf, 0);
			            if (sampleSize < 0) {
			                sawInputEOS = true;
			                codec.queueInputBuffer(inputBufIndex,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM );
			            } else {
			            	long presentationTimeUs = extractor.getSampleTime();
			                codec.queueInputBuffer(inputBufIndex,0,sampleSize,presentationTimeUs,0);
			                extractor.advance();
			            }
			        }
			    }
			    int res = codec.dequeueOutputBuffer(info, kTimeOutUs);
			    if (res >= 0) {

			    	 int outputBufIndex = res;
			    	// Simply ignore codec config buffers.
	        		if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!= 0) {
	                     codec.releaseOutputBuffer(outputBufIndex, false);
	                     continue;
	                 }
	        		 
	        		if(info.size != 0){
	        			
	        			ByteBuffer outBuf = codecOutputBuffers[outputBufIndex];
				        
				        outBuf.position(info.offset);
				        outBuf.limit(info.offset + info.size);
				        byte[] data = new byte[info.size];
				        outBuf.get(data);
				        totalRawSize += data.length;
				        fosDecoder.write(data);
				        if(mOnAudioDecoderListener != null)
				        	mOnAudioDecoderListener.onDecode(data, info.presentationTimeUs / audioDurationUs);
	        		}
			        codec.releaseOutputBuffer(outputBufIndex, false);
			        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
			            sawOutputEOS = true;
			        }
			        
			    } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
			        codecOutputBuffers = codec.getOutputBuffers();
			    } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
			        MediaFormat oformat = codec.getOutputFormat();
			    }
			}
			rawAudioInfo.size = totalRawSize;
			
			if(mOnAudioDecoderListener != null)
	        	mOnAudioDecoderListener.onDecode(null, 1);
			return rawAudioInfo;
        }finally{
        	fosDecoder.close();
        	codec.stop();
            codec.release();
            extractor.release();
        }
        
	}
}
