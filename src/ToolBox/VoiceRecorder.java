package ToolBox;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class VoiceRecorder {

    private TargetDataLine line;
    private final AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private volatile boolean isRecording = false;

    public void startRecording() {
        isRecording = true;
        Thread recordingThread = new Thread(() -> {
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                byte[] buffer = new byte[1024];
                while (isRecording) {
                    int count = line.read(buffer, 0, buffer.length);
                    if (count > 0) {
                        out.write(buffer, 0, count);
                    }
                }
            } catch (LineUnavailableException e) {
                System.err.println("Microphone not available: " + e.getMessage());
            } finally {
                if (line != null) {
                    line.stop();
                    line.close();
                }
            }
        });
        recordingThread.start();
        System.out.println("Recording started...");
    }

    public byte[] stopRecording() {
        isRecording = false;
        System.out.println("Recording stopped.");
        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] audioData = out.toByteArray();
        return createWavFile(audioData);
    }

    private byte[] createWavFile(byte[] audioData) {
        try (ByteArrayOutputStream wavOut = new ByteArrayOutputStream();
             AudioInputStream audioInputStream = new AudioInputStream(new ByteArrayInputStream(audioData), format, audioData.length / format.getFrameSize())) {

            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, wavOut);
            return wavOut.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
