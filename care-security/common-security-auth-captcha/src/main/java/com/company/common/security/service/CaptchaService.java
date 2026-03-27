package com.company.common.security.service;

import com.company.common.security.autoconfigure.CareSecurityProperties;
import com.company.common.security.spi.CaptchaVerifier;
import org.springframework.data.redis.core.RedisTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Generates and verifies image CAPTCHAs.
 * Codes are stored in Redis with a configurable TTL.
 * Each CAPTCHA is single-use: consumed on verification.
 */
public class CaptchaService implements CaptchaVerifier {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);
    private static final String REDIS_KEY_PREFIX = "captcha:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String AUDIO_RESOURCE_PATH = "captcha/audio/";

    /** 預錄的中文數字語音 WAV（0-9），啟動時載入到記憶體 */
    private final Map<Character, byte[]> digitAudioCache = new HashMap<>();

    private final RedisTemplate<String, Object> redisTemplate;
    private final CareSecurityProperties.Captcha config;

    public CaptchaService(RedisTemplate<String, Object> redisTemplate,
                          CareSecurityProperties.Captcha config) {
        this.redisTemplate = redisTemplate;
        this.config = config;
        if (config.isAudioEnabled()) {
            loadDigitAudio();
        }
    }

    /** 載入預錄的語音檔到記憶體（0-9 數字 + a-z 英文字母） */
    private void loadDigitAudio() {
        // 載入 0-9
        for (char c = '0'; c <= '9'; c++) {
            loadOneAudio(c);
        }
        // 載入 a-z（檔名為小寫，查詢時統一轉小寫）
        for (char c = 'a'; c <= 'z'; c++) {
            loadOneAudio(c);
        }
        log.info("CAPTCHA audio loaded: {} characters", digitAudioCache.size());
    }

    private void loadOneAudio(char c) {
        String path = AUDIO_RESOURCE_PATH + c + ".wav";
        try {
            ClassPathResource resource = new ClassPathResource(path);
            try (InputStream is = resource.getInputStream()) {
                digitAudioCache.put(c, is.readAllBytes());
                log.debug("Loaded CAPTCHA audio: {}", path);
            }
        } catch (IOException e) {
            log.warn("Failed to load CAPTCHA audio: {}", path, e);
        }
    }

    public record CaptchaResult(String captchaId, String imageBase64) {}

    /**
     * Generate a new CAPTCHA: random code from configured charset + distorted image.
     */
    public CaptchaResult generateCaptcha() {
        String captchaId = UUID.randomUUID().toString();
        String code = generateCode(config.getLength());

        // Store in Redis
        String key = REDIS_KEY_PREFIX + captchaId;
        redisTemplate.opsForValue().set(key, code, Duration.ofSeconds(config.getExpireSeconds()));

        // Generate image
        String imageBase64 = renderImage(code);

        return new CaptchaResult(captchaId, imageBase64);
    }

    /**
     * Verify the user's answer (case-insensitive). Single-use: deletes from Redis on any attempt.
     */
    public boolean verifyCaptcha(String captchaId, String answer) {
        if (captchaId == null || answer == null) {
            return false;
        }

        String key = REDIS_KEY_PREFIX + captchaId;
        Object stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            return false;
        }

        // Delete immediately (single-use)
        redisTemplate.delete(key);

        return stored.toString().equalsIgnoreCase(answer.trim());
    }

    /**
     * Generate audio representation of the CAPTCHA code as WAV base64.
     * Each digit is spoken as an English word (e.g., "one", "two") using formant synthesis.
     *
     * @param captchaId the CAPTCHA ID (code is read from Redis without deleting)
     * @return WAV base64 string, or null if captchaId not found
     */
    public String generateAudioBase64(String captchaId) {
        if (captchaId == null) {
            return null;
        }

        String key = REDIS_KEY_PREFIX + captchaId;
        Object stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            return null;
        }

        String code = stored.toString();
        return renderAudio(code);
    }

    public boolean isAudioEnabled() {
        return config.isAudioEnabled();
    }

    // ---- private helpers ----

    private String generateCode(int length) {
        String chars = config.getChars();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String renderImage(String code) {
        int width = config.getWidth();
        int height = config.getHeight();
        int fontSize = config.getFontSize();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, width, height);

        // Noise lines
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(RANDOM.nextInt(200), RANDOM.nextInt(200), RANDOM.nextInt(200)));
            g.setStroke(new BasicStroke(1 + RANDOM.nextFloat()));
            g.drawLine(RANDOM.nextInt(width), RANDOM.nextInt(height),
                       RANDOM.nextInt(width), RANDOM.nextInt(height));
        }

        // Noise dots
        for (int i = 0; i < 30; i++) {
            g.setColor(new Color(RANDOM.nextInt(200), RANDOM.nextInt(200), RANDOM.nextInt(200)));
            g.fillOval(RANDOM.nextInt(width), RANDOM.nextInt(height), 2, 2);
        }

        // Draw each character with slight rotation and color variation
        Font font = new Font("Arial", Font.BOLD, fontSize);
        g.setFont(font);
        int charWidth = width / (code.length() + 1);

        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(RANDOM.nextInt(100), RANDOM.nextInt(100), RANDOM.nextInt(100)));

            // Slight rotation
            double angle = (RANDOM.nextDouble() - 0.5) * 0.4;
            int x = charWidth * (i + 1) - 8;
            int y = (height / 2) + (fontSize / 3) + RANDOM.nextInt(6) - 3;

            g.rotate(angle, x, y);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.rotate(-angle, x, y);
        }

        g.dispose();

        // Encode to base64 PNG
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render CAPTCHA image", e);
        }
    }

    /**
     * 拼接預錄的中文數字語音 WAV：
     * 讀取每個字元對應的 WAV PCM data，中間插入 300ms 靜音，最後輸出完整 WAV。
     */
    private String renderAudio(String code) {
        float sampleRate = 16000f;
        int bitsPerSample = 16;
        int gapSamples = (int) (0.3 * sampleRate); // 300ms 靜音
        byte[] gapBytes = new byte[gapSamples * 2]; // 16-bit = 2 bytes/sample

        try (ByteArrayOutputStream pcmOut = new ByteArrayOutputStream()) {
            for (int i = 0; i < code.length(); i++) {
                char ch = Character.toLowerCase(code.charAt(i));
                byte[] wavFile = digitAudioCache.get(ch);
                if (wavFile == null) {
                    // 沒有對應音檔，插入靜音
                    pcmOut.write(gapBytes);
                    continue;
                }
                // WAV header 固定 44 bytes，之後是 PCM data
                int headerSize = 44;
                if (wavFile.length > headerSize) {
                    pcmOut.write(wavFile, headerSize, wavFile.length - headerSize);
                }
                // 字元間加靜音（最後一個不加）
                if (i < code.length() - 1) {
                    pcmOut.write(gapBytes);
                }
            }

            byte[] allPcm = pcmOut.toByteArray();
            int totalSamples = allPcm.length / 2;

            AudioFormat format = new AudioFormat(sampleRate, bitsPerSample, 1, true, false);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(allPcm);
                 AudioInputStream ais = new AudioInputStream(bais, format, totalSamples);
                 ByteArrayOutputStream wavOut = new ByteArrayOutputStream()) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavOut);
                return Base64.getEncoder().encodeToString(wavOut.toByteArray());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render CAPTCHA audio", e);
        }
    }
}
