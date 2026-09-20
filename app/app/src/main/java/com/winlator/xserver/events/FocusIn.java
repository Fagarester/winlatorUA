package com.winlator.xserver.events;

import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.Window;

import java.io.IOException;

public class FocusIn extends Event {
    public static final byte DETAIL_NONLINEAR = 3;
    private final Window window;
    private final byte detail;

    public FocusIn(Window window, byte detail) {
        super(9);
        this.window = window;
        this.detail = detail;
    }

    @Override
    public void send(short sequenceNumber, XOutputStream outputStream) throws IOException {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(code);
            outputStream.writeByte(detail);
            outputStream.writeShort(sequenceNumber);
            outputStream.writeInt(window.id);
            outputStream.writeByte((byte)0);
            outputStream.writePad(23);
        }
    }
}
