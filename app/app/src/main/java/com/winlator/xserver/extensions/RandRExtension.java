package com.winlator.xserver.extensions;

import static com.winlator.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.XClient;
import com.winlator.xserver.XServer;
import com.winlator.xserver.errors.BadImplementation;
import com.winlator.xserver.errors.XRequestError;

import java.io.IOException;

/**
 * Minimal RANDR 1.3 extension: one CRTC, one output and one mode that always
 * match the size of the X server screen. Only exists so that native Linux
 * programs (SDL / Xlib) can enumerate the display instead of hanging.
 */
public class RandRExtension extends Extension {
    private static final int MAJOR_VERSION = 1;
    private static final int MINOR_VERSION = 3;
    private static final int TIMESTAMP = 1;
    private static final int CRTC_ID = 0x60;
    private static final int OUTPUT_ID = 0x61;
    private static final int MODE_ID = 0x62;
    private static final int REFRESH_RATE = 60;
    private static final String OUTPUT_NAME = "Android-0";

    private static abstract class ClientOpcodes {
        private static final int QUERY_VERSION = 0;
        private static final int SET_SCREEN_CONFIG = 2;
        private static final int SELECT_INPUT = 4;
        private static final int GET_SCREEN_INFO = 5;
        private static final int GET_SCREEN_SIZE_RANGE = 6;
        private static final int SET_SCREEN_SIZE = 7;
        private static final int GET_SCREEN_RESOURCES = 8;
        private static final int GET_OUTPUT_INFO = 9;
        private static final int LIST_OUTPUT_PROPERTIES = 10;
        private static final int GET_OUTPUT_PROPERTY = 15;
        private static final int GET_CRTC_INFO = 20;
        private static final int SET_CRTC_CONFIG = 21;
        private static final int GET_SCREEN_RESOURCES_CURRENT = 25;
        private static final int SET_OUTPUT_PRIMARY = 30;
        private static final int GET_OUTPUT_PRIMARY = 31;
    }

    public RandRExtension(XServer xServer, byte majorOpcode) {
        super(xServer, majorOpcode);
    }

    @Override
    public String getName() {
        return "RANDR";
    }

    @Override
    public byte getFirstEventId() {
        return 89;
    }

    private int width() {
        return xServer.screenInfo.width;
    }

    private int height() {
        return xServer.screenInfo.height;
    }

    private String modeName() {
        return width()+"x"+height();
    }

    private void writeHeader(XClient client, XOutputStream outputStream, int extraWords) {
        outputStream.writeByte(RESPONSE_CODE_SUCCESS);
        outputStream.writeByte((byte)0);
        outputStream.writeShort(client.getSequenceNumber());
        outputStream.writeInt(extraWords);
    }

    private void queryVersion(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        inputStream.skip(8);

        try (XStreamLock lock = outputStream.lock()) {
            writeHeader(client, outputStream, 0);
            outputStream.writeInt(MAJOR_VERSION);
            outputStream.writeInt(MINOR_VERSION);
            outputStream.writePad(16);
        }
    }

    private void getScreenSizeRange(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        inputStream.skip(4);

        try (XStreamLock lock = outputStream.lock()) {
            writeHeader(client, outputStream, 0);
            outputStream.writeShort((short)width());
            outputStream.writeShort((short)height());
            outputStream.writeShort((short)width());
            outputStream.writeShort((short)height());
            outputStream.writePad(16);
        }
    }

    private void getScreenResources(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        inputStream.skip(4);

        String name = modeName();
        int nameLength = name.length();
        int namePad = -nameLength & 3;
        int dataBytes = 4 + 4 + 32 + nameLength + namePad;

        try (XStreamLock lock = outputStream.lock()) {
            writeHeader(client, outputStream, dataBytes / 4);
            outputStream.writeInt(TIMESTAMP);
            outputStream.writeInt(TIMESTAMP);
            outputStream.writeShort((short)1);
            outputStream.writeShort((short)1);
            outputStream.writeShort((short)1);
            outputStream.writeShort((short)nameLength);
            outputStream.writePad(8);

            outputStream.writeInt(CRTC_ID);
            outputStream.writeInt(OUTPUT_ID);

            outputStream.writeInt(MODE_ID);
            outputStream.writeShort((short)width());
            outputStream.writeShort((short)height());
            outputStream.writeInt(width() * height() * REFRESH_RATE);
            outputStream.writeShort((short)width());
            outputStream.writeShort((short)width());
            outputStream.writeShort((short)width());
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)height());
            outputStream.writeShort((short)height());
            outputStream.writeShort((short)height());
            outputStream.writeShort((short)nameLength);
            outputStream.writeInt(0);

            outputStream.writeString8(name);
        }
    }

    private void getOutputInfo(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        inputStream.skip(8);

        int nameLength = OUTPUT_NAME.length();
        int namePad = -nameLength & 3;
        int dataBytes = 4 + 4 + nameLength + namePad;
        int mmWidth = (int)(width() * 25.4f / 96.0f);
        int mmHeight = (int)(height() * 25.4f / 96.0f);

        try (XStreamLock lock = outputStream.lock()) {
            writeHeader(client, outputStream, 1 + dataBytes / 4);
            outputStream.writeInt(TIMESTAMP);
            outputStream.writeInt(CRTC_ID);
            outputStream.writeInt(mmWidth);
            outputStream.writeInt(mmHeight);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeShort((short)1);
            outputStream.writeShort((short)1);
            outputStream.writeShort((short)1);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)nameLength);

            outputStream.writeInt(CRTC_ID);
            outputStream.writeInt(MODE_ID);
            outputStream.writeString8(OUTPUT_NAME);
        }
    }

    private void getCrtcInfo(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        inputStream.skip(8);

        try (XStreamLock lock = outputStream.lock()) {
            writeHeader(client, outputStream, 2);
            outputStream.writeInt(TIMESTAMP);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)width());
            outputStream.writeShort((short)height());
            outputStream.writeInt(MODE_ID);
            outputStream.writeShort((short)1);
            outputStream.writeShort((short)1);
            outputStream.writeShort((short)1);
            outputStream.writeShort((short)1);

            outputStream.writeInt(OUTPUT_ID);
            outputStream.writeInt(OUTPUT_ID);
        }
    }

    private void setCrtcConfig(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        // The screen size is fixed, so the request is accepted but nothing changes.
        client.skipRequest();

        try (XStreamLock lock = outputStream.lock()) {
            writeHeader(client, outputStream, 0);
            outputStream.writeInt(TIMESTAMP);
            outputStream.writePad(20);
        }
    }

    private void getOutputPrimary(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        inputStream.skip(4);

        try (XStreamLock lock = outputStream.lock()) {
            writeHeader(client, outputStream, 0);
            outputStream.writeInt(OUTPUT_ID);
            outputStream.writePad(20);
        }
    }

    private void listOutputProperties(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        inputStream.skip(4);

        try (XStreamLock lock = outputStream.lock()) {
            writeHeader(client, outputStream, 0);
            outputStream.writeShort((short)0);
            outputStream.writePad(22);
        }
    }

    private void getOutputProperty(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        client.skipRequest();

        try (XStreamLock lock = outputStream.lock()) {
            writeHeader(client, outputStream, 0);
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writePad(12);
        }
    }

    @Override
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int opcode = client.getRequestData() & 0xff;
        switch (opcode) {
            case ClientOpcodes.QUERY_VERSION:
                queryVersion(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_SCREEN_SIZE_RANGE:
                getScreenSizeRange(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_SCREEN_RESOURCES:
            case ClientOpcodes.GET_SCREEN_RESOURCES_CURRENT:
                getScreenResources(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_OUTPUT_INFO:
                getOutputInfo(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_CRTC_INFO:
                getCrtcInfo(client, inputStream, outputStream);
                break;
            case ClientOpcodes.SET_CRTC_CONFIG:
                setCrtcConfig(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_OUTPUT_PRIMARY:
                getOutputPrimary(client, inputStream, outputStream);
                break;
            case ClientOpcodes.LIST_OUTPUT_PROPERTIES:
                listOutputProperties(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_OUTPUT_PROPERTY:
                getOutputProperty(client, inputStream, outputStream);
                break;
            case ClientOpcodes.SELECT_INPUT:
            case ClientOpcodes.SET_SCREEN_SIZE:
            case ClientOpcodes.SET_OUTPUT_PRIMARY:
                break;
            default:
                throw new BadImplementation();
        }

        client.skipRequest();
    }
}
