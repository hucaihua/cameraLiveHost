package com.example.cameralivehost;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class H264Encoder{
    SocketLive socketLive;
    MediaCodec mediaCodec;

    int width = 720 ;
    int height = 1280;

    public static final int NAL_I = 5;
    public static final int NAL_SPS = 7;

    byte[] sps_pps;
    byte[] nv12;
    byte[] yuv;

    public H264Encoder(SocketLive socketLive, int width, int height) {
        this.width = width;
        this.height = height;
        this.socketLive = socketLive;
    }

    public void encodeFrame(byte[] input) {
        nv12 = YuvUtils.nv21toNV12(input);
        Bitmap bitmap = YuvUtils.showImage(nv12 , width , height);
        YuvUtils.portraitData2Raw(nv12, yuv, width, height);
        Bitmap bitmap1 = YuvUtils.showImage(yuv , width , height);
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(100000);
        if (inputBufferIndex >= 0) {
            ByteBuffer[] byteBuffers = mediaCodec.getInputBuffers();
            ByteBuffer inputBuffer = byteBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(yuv);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, yuv.length, System.currentTimeMillis(), 0);
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int index = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
        if (index >= 0 ){
            int bufferSize = bufferInfo.size;
            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(index);
            //解析bytes
            dealBytes(outputBuffer,bufferSize);
            mediaCodec.releaseOutputBuffer(index , false);
        }
    }

    public void startLive() {
        try {
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC , height ,width);
            //设置编码的数据格式来源，比如yuv，如果是从surface来的，直接写MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE , 20);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL , 10);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE , width * height);
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            //设置为编码器 ，并且不需要渲染，捕获数据后发送到网络。
            mediaCodec.configure(mediaFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
            yuv = new byte[width*height*3/2];
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    //输入的是一帧数据，判断是否是I帧，给I帧添加sps/pps，发送到server
    private void dealBytes(ByteBuffer outputBuffer, int bufferSize) {
        int offset = 4;
        if (outputBuffer.get(2) == 0x01){
            offset = 3;
        }

//        byte[] testBuffer = new byte[bufferSize];
//        Log.d("hucaihua", "帧大小--->  " + testBuffer.length);
//        outputBuffer.get(testBuffer);
//        FileUtil.writeContent(testBuffer);


        //找出当前帧的类型 , 分隔符00000001/000001后面的一个字节为配置信息，最后5位为帧类型
        //如果是sps/pps帧，则保存信息。
        if (sps_pps == null){
            if ((outputBuffer.get(offset) & 0x1f) == NAL_SPS){
                sps_pps = new byte[bufferSize];
                outputBuffer.get(sps_pps);
            }
        }
        //如果是I帧，我们需要添加sps/pps到帧的前面
        else if ((outputBuffer.get(offset) & 0x1f) == NAL_I){
            final byte[] iFrame = new byte[bufferSize];
            outputBuffer.get(iFrame);
            byte[] newBuf = new byte[sps_pps.length + bufferSize];
            System.arraycopy(sps_pps, 0, newBuf, 0, sps_pps.length);
            System.arraycopy(iFrame, 0, newBuf, sps_pps.length, iFrame.length);
            socketLive.sendData(newBuf);
            Log.v("david", "I帧大小--->  " + iFrame.length);
        }
        // 其他帧，直接发送
        else{
            final byte[] otherFrame = new byte[bufferSize];
            outputBuffer.get(otherFrame);
            socketLive.sendData(otherFrame);
//            Log.v("david", "视频数据  " + Arrays.toString(otherFrame));
        }
    }
}
