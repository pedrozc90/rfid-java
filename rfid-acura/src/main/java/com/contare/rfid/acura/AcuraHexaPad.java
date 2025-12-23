package com.contare.rfid.acura;

import com.contare.rfid.devices.RfidDevice;
import com.contare.rfid.exceptions.RfidDeviceException;
import com.contare.rfid.objects.TagMetadata;
import com.fazecast.jSerialComm.SerialPort;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class AcuraHexaPad extends AcuraBaseDevice {

    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final String CMD_READ_TAG_ON = "readtag on";
    private static final String CMD_READ_TAG_OFF = "readtag off";
    private static final String CMD_GET_POWER = "readpower";
    private static final String CMD_SET_POWER = "readpower%d";

    private final Logger logger = Logger.getLogger(AcuraHexaPad.class.getName());

    private volatile SerialPort comm;
    private RfidDevice.Options opts;

    private volatile Thread _thread;
    private final Executor executor;
    private volatile Consumer<RfidDevice.Event> _callback;
    private boolean reading = false;

    public AcuraHexaPad(final Executor executor) {
        this.executor = executor;
    }

    @Override
    public boolean connect(final RfidDevice.Options opts) throws RfidDeviceException {
        Objects.requireNonNull(opts, "Options must not be null");

        final String serialPort = opts.getSerial();
        Objects.requireNonNull(serialPort, "serial port must not be null");

        final Integer baudRate = opts.getBaudRate();
        if (baudRate == null) {
            throw new IllegalArgumentException("baud rate must not be null");
        } else if (baudRate != 115_200) {
            throw new IllegalArgumentException("baud rate must be 115200");
        }

        if (isConnected()) {
            throw new RfidDeviceException("Device already connected.");
        }

        this.opts = opts;

        final String path = (OS.contains("linux"))
            ? String.format("/dev/%s", serialPort)
            : serialPort;

        comm = SerialPort.getCommPort(path);
        comm.setBaudRate(opts.getBaudRate());

        return comm.openPort();
    }

    @Override
    public void disconnect() throws RfidDeviceException {
        if (comm != null) {
            if (comm.isOpen()) {
                try {
                    final InputStream in = comm.getInputStream();
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e) {
                    logger.errorf(e, "Failed to close input stream.");
                }

                try {
                    final OutputStream out = comm.getOutputStream();
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    logger.errorf(e, "Failed to close output stream.");
                }

                boolean closed = comm.closePort();
                if (closed) {
                    logger.debugf("Closed serial port");
                }
            }
        }
    }

    @Override
    public boolean isConnected() {
        return comm != null && comm.isOpen();
    }

    @Override
    public RfidDevice.Params getInventoryParameters() {
        return null;
    }

    @Override
    public boolean setInventoryParameters(final RfidDevice.Params params) {
        return false;
    }

    @Override
    public void setCallback(final Consumer<RfidDevice.Event> callback) {
        _callback = callback;
    }

    @Override
    public boolean startInventory() throws RfidDeviceException {
        if (comm != null) {
            if (comm.isOpen()) {
                final String r = sendCommand(CMD_READ_TAG_ON);
                logger.debugf("Read tag on: %s", r);

                reading = true;

                _thread = new Thread(() -> {
                    final InputStream in = comm.getInputStream();
                    while (reading) {
                        try {
                            final int available = in.available();
                            if (available > 0) {
                                final byte[] buffer = new byte[1024];

                                final int read = in.read(buffer);
                                logger.debugf("Read '%d' bytes", read);

                                final String data = new String(buffer, 0, read);
                                logger.debugf("Read data '%s'", data);

                                final String[] lines = data.split("\r\n");
                                for (String line : lines) {
                                    final TagMetadata tag = parseTagMetadata(line);
                                    if (tag != null && tag.getRfid() != null) {
                                        if (_uniques.add(tag.getRfid())) {
                                            if (_buffer.add(tag)) {
                                                executor.execute(() -> {
                                                    _callback.accept(new TagEvent(tag));
                                                });
                                            } else {
                                                logger.warnf("Duplicate tag found for '%s'", tag.getRfid());
                                            }
                                        } else {
                                            logger.debugf("Tag '%s' already read.", tag.getRfid());
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) {
                            logger.errorf(e, "Failed to read data.");
                        }
                    }
                });

                _thread.start();
            }
        }
        return false;
    }

    @Override
    public boolean stopInventory() {
        if (comm != null) {
            if (comm.isOpen()) {
                try {
                    final String r = sendCommand(CMD_READ_TAG_OFF);
                } catch (RfidDeviceException e) {
                    logger.errorf(e, "Failed to stop serial port.");
                }
            }
        }

        if (_thread != null) {
            if (_thread.isAlive()) {
                _thread.interrupt();
                reading = false;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isReading() {
        return reading;
    }

    @Override
    public boolean killTag(final String rfid, final String password) throws RfidDeviceException {
        throw new UnsupportedOperationException("HexaPad do not support kill tag operations.");
    }

    @Override
    public RfidDevice.Frequency getFrequency() {
        return null;
    }

    @Override
    public boolean setFrequency(final RfidDevice.Frequency frequency) {
        return false;
    }

    @Override
    public int getPower() {
        try {
            if (isConnected()) {
                final String result = this.sendCommand(CMD_GET_POWER);

                final String[] lines = result.split("\n");
                for (String line : lines) {
                    final String trimmed = line.trim();
                    if (trimmed.matches("\\d+")) {
                        return Integer.parseInt(trimmed);
                    }
                }

                if (result.toLowerCase().contains("error")) {
                    throw new RfidDeviceException(result);
                }
            }
        } catch (NumberFormatException e) {
            logger.errorf(e, "Failed to parse power.");
        } catch (RfidDeviceException e) {
            logger.errorf(e, "Failed to read power");
        }
        return -1;
    }

    @Override
    public boolean setPower(final int value) {
        try {
            if (isConnected()) {
                final String cmd = String.format(CMD_SET_POWER, value);
                final String result = this.sendCommand(cmd);

                if (result.toLowerCase().contains("error")) {
                    throw new RfidDeviceException(result);
                }

                return true;
            }
        } catch (RfidDeviceException e) {
            logger.errorf(e, "Failed to read power");
        }
        return false;
    }

    @Override
    public boolean getBeep() {
        return false;
    }

    @Override
    public boolean setBeep(final boolean enabled) {
        return false;
    }

    @Override
    public boolean setTagFocus(final boolean enabled) {
        throw new UnsupportedOperationException("HexaPad do not support tag focus.");
    }

    @Override
    public void close() throws Exception {
        try {
            stopInventory();
            disconnect();
        } catch (RfidDeviceException e) {
            logger.errorf(e, "Failed to close device.");
        }
    }

    // HELPERS
    private String sendCommand(final String cmd) throws RfidDeviceException {
        try {
            final InputStream in = comm.getInputStream();

            // clear input buffer before sending command
            if (in.available() > 0) {
                long skipped = in.skip(in.available());
                logger.debugf("Skipped %d bytes", skipped);
            }

            final OutputStream out = comm.getOutputStream();
            out.write((cmd + "\r\n").getBytes());
            out.flush();

            // Espera alguns milissegundos para dar tempo ao leitor responder
            Thread.sleep(100);

            final byte[] buffer = new byte[1024];
            final int len = in.read(buffer);
            final String result = new String(buffer, 0, len).trim();
            logger.debugf("Response: " + result);
            return result;
        } catch (IOException e) {
            throw new RfidDeviceException(e, "Error while sending command to serial port.");
        } catch (InterruptedException e) {
            throw new RfidDeviceException(e);
        }
    }

    private TagMetadata parseTagMetadata(final String data) {
        try {
            final String[] parts = data.split("#");
            if (parts.length >= 1 && parts[0].matches("[a-fA-F0-9]+")) {
                final String epc = parts[0].trim().toUpperCase();
                final String rssi = (parts.length >= 2) ? parts[1].trim() : null;
                final Integer antenna = (parts.length >= 3) ? Integer.parseInt(parts[2].trim()) : null;
                return new TagMetadata(epc, null, rssi, antenna);
            } else {
                logger.warnf("Unable to parse tag metadata: '%s'", data);
            }
        } catch (NumberFormatException e) {
            logger.errorf("Error to parse antenna value from '%s'", data);
        } catch (Exception e) {
            logger.errorf(e, "Error while parsing tag metadata: '%s'", data);
        }
        return null;
    }

}
