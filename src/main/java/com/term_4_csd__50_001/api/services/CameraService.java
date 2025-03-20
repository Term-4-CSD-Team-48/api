package com.term_4_csd__50_001.api.services;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.stereotype.Service;
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

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final int PORT = 5000;
    private volatile boolean listening = false;
    private volatile String frameDataHash = "";
    private volatile byte[] frameData = new byte[0];

    public boolean isListening() {
        return listening;
    }

    private void setListening(boolean listening) {
        this.listening = listening;
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

    /**
     * Requires that shared secret is 16 bytes long when encoded in java default charset
     * 
     * @param sharedSecret
     * @throws InterruptedException
     */
    public void startListening(String sharedSecret) throws InterruptedException {
        final byte[] sharedSecretInBytes = Base64.getDecoder().decode(sharedSecret);

        if (isListening())
            return;

        executorService.submit(() -> {

            EventLoopGroup group = new NioEventLoopGroup();
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
                                log.debug(String.format("Extracted frame data of length %d",
                                        receivedFrameData.length));
                                setFrameData(receivedFrameData);
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
