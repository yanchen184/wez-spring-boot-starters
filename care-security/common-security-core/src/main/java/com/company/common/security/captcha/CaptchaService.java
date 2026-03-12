package com.company.common.security.captcha;

import org.springframework.data.redis.core.RedisTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * Generates and verifies image CAPTCHAs.
 * Codes are stored in Redis with a configurable TTL.
 * Each CAPTCHA is single-use: consumed on verification.
 */
public class CaptchaService {

    private static final String REDIS_KEY_PREFIX = "captcha:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int IMAGE_WIDTH = 160;
    private static final int IMAGE_HEIGHT = 50;

    private final RedisTemplate<String, Object> redisTemplate;
    private final int codeLength;
    private final int expireSeconds;

    public CaptchaService(RedisTemplate<String, Object> redisTemplate,
                          int codeLength, int expireSeconds) {
        this.redisTemplate = redisTemplate;
        this.codeLength = codeLength;
        this.expireSeconds = expireSeconds;
    }

    public record CaptchaResult(String captchaId, String imageBase64) {}

    /**
     * Generate a new CAPTCHA: random numeric code + distorted image.
     */
    public CaptchaResult generateCaptcha() {
        String captchaId = UUID.randomUUID().toString();
        String code = generateNumericCode(codeLength);

        // Store in Redis
        String key = REDIS_KEY_PREFIX + captchaId;
        redisTemplate.opsForValue().set(key, code, Duration.ofSeconds(expireSeconds));

        // Generate image
        String imageBase64 = renderImage(code);

        return new CaptchaResult(captchaId, imageBase64);
    }

    /**
     * Verify the user's answer. Single-use: deletes from Redis on any attempt.
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

        return stored.toString().equals(answer.trim());
    }

    /**
     * Test-only: retrieve the stored answer for a captcha id.
     * Do NOT use in production code.
     */
    public String getAnswerForTest(String captchaId) {
        String key = REDIS_KEY_PREFIX + captchaId;
        Object stored = redisTemplate.opsForValue().get(key);
        return stored != null ? stored.toString() : null;
    }

    private String generateNumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private String renderImage(String code) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        // Noise lines
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(RANDOM.nextInt(200), RANDOM.nextInt(200), RANDOM.nextInt(200)));
            g.setStroke(new BasicStroke(1 + RANDOM.nextFloat()));
            g.drawLine(RANDOM.nextInt(IMAGE_WIDTH), RANDOM.nextInt(IMAGE_HEIGHT),
                       RANDOM.nextInt(IMAGE_WIDTH), RANDOM.nextInt(IMAGE_HEIGHT));
        }

        // Noise dots
        for (int i = 0; i < 30; i++) {
            g.setColor(new Color(RANDOM.nextInt(200), RANDOM.nextInt(200), RANDOM.nextInt(200)));
            g.fillOval(RANDOM.nextInt(IMAGE_WIDTH), RANDOM.nextInt(IMAGE_HEIGHT), 2, 2);
        }

        // Draw each character with slight rotation and color variation
        Font font = new Font("Arial", Font.BOLD, 32);
        g.setFont(font);
        int charWidth = IMAGE_WIDTH / (code.length() + 1);

        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(RANDOM.nextInt(100), RANDOM.nextInt(100), RANDOM.nextInt(100)));

            // Slight rotation
            double angle = (RANDOM.nextDouble() - 0.5) * 0.4;
            int x = charWidth * (i + 1) - 8;
            int y = 35 + RANDOM.nextInt(6) - 3;

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
}
