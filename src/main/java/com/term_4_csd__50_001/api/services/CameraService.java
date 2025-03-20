package com.term_4_csd__50_001.api.services;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_highgui;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.term_4_csd__50_001.api.Dotenv;
import com.term_4_csd__50_001.api.exceptions.ConflictException;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CameraService {

    private static volatile boolean listening = false;
    private final String cameraURL;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile String frameDataHash = "";
    private volatile byte[] frameData = new byte[0];

    @Autowired
    public CameraService(Dotenv dotenv) {
        cameraURL = dotenv.get(Dotenv.CAMERA_URL);
        startListening();
    }

    public boolean isListening() {
        return CameraService.listening;
    }

    private void setListening(boolean listening) {
        CameraService.listening = listening;
    }

    public byte[] getFrameData() {
        return frameData;
    }

    private void setFrameData(byte[] frameData) {
        this.frameData = frameData;
        setFrameDataHash(frameData);
    }

    public String getFrameDataHash() {
        return frameDataHash;
    }

    /**
     * Do not call this: this should only be called by setFrameData.
     * 
     * @param frameData
     */
    private void setFrameDataHash(byte[] frameData) {
        this.frameDataHash = String.valueOf(frameData.hashCode());
    }

    public void startListening() {
        if (isListening())
            throw new ConflictException("Already listening");

        executorService.submit(() -> {
            setListening(true);
            int attempts = 0;
            Frame frame;
            Mat mat;
            BytePointer bp;
            byte[] frameData;
            OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
            while (true) {
                try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(cameraURL);) {
                    grabber.setOption("rtsp_transport_option", "tcp");
                    grabber.start();
                    while ((frame = grabber.grab()) != null) {
                        mat = converter.convert(frame);
                        bp = new BytePointer();
                        boolean success = opencv_imgcodecs.imencode(".jpg", mat, bp);
                        if (success) {
                            frameData = new byte[(int) bp.limit()];
                            bp.get(frameData);
                            bp.close();
                            setFrameData(frameData);
                        }
                        // Uncomment below 2 lines to see if frames are being received
                        // opencv_highgui.imshow("Received Frame", mat);
                        // opencv_highgui.waitKey(1);
                    }
                    grabber.close();
                } catch (Exception e) {
                    attempts = attempts + 1;
                    log.warn("Could not connect to " + cameraURL + " for " + attempts + " times");
                } finally {
                    converter.close();
                    setListening(false);
                }
            }
        });
    }

    /**
     * Requires that shared secret is 16 bytes long when encoded in java default charset
     * 
     * @param sharedSecret
     * @throws InterruptedException
     */
    @Deprecated
    public void startListening(String sharedSecret) {
        final byte[] sharedSecretInBytes = Base64.getDecoder().decode(sharedSecret);

        if (isListening())
            throw new ConflictException("Already listening");

        executorService.submit(() -> {

            EventLoopGroup group = new NioEventLoopGroup();
            final int PORT = 5000;
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group).channel(NioDatagramChannel.class)
                        .option(ChannelOption.SO_BROADCAST, true)
                        .handler(new SimpleChannelInboundHandler<DatagramPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx,
                                    DatagramPacket packet) throws Exception {
                                ByteBuf content = packet.content();
                                byte[] receivedBytes = new byte[content.readableBytes()];
                                content.readBytes(receivedBytes);

                                // Extract secret key (first 16 bytes)
                                byte[] receivedKeyBytes = Arrays.copyOfRange(receivedBytes, 0, 16);
                                if (!Arrays.equals(receivedKeyBytes, sharedSecretInBytes)) {
                                    log.debug("Invalid Secret Key! Ignoring packet.");
                                    return;
                                }

                                // Extract frame data (after the 16-byte key)
                                byte[] receivedFrameData = new byte[receivedBytes.length - 16];
                                System.arraycopy(receivedBytes, 16, receivedFrameData, 0,
                                        receivedFrameData.length);
                                System.out
                                        .println(String.format("Extracted frame data of length %d",
                                                receivedFrameData.length));
                                setFrameData(receivedFrameData);

                                BytePointer bp = new BytePointer(receivedFrameData);
                                Mat encodedMat = new Mat(bp);
                                Mat frame = opencv_imgcodecs.imdecode(encodedMat,
                                        opencv_imgcodecs.IMREAD_COLOR);
                                if (!frame.empty()) {
                                    opencv_highgui.imshow("Received Frame", frame);
                                    opencv_highgui.waitKey(1); // Adjust delay if needed
                                }
                            }
                        });

                log.info("Netty UDP Server listening on port " + PORT + " at "
                        + LocalDateTime.now());
                setListening(true);
                Channel channel = bootstrap.bind(new InetSocketAddress(PORT)).sync().channel();
                channel.closeFuture().await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("UDP Listener interrupted");
            } finally {
                setListening(false);
                log.info("Netty UDP Server shutting down");
                group.shutdownGracefully();
            }
        });
    }
}
