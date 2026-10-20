package br.com.nandesk.file.application;

import br.com.nandesk.shared.crypto.Hashing;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.coobird.thumbnailator.Thumbnails;

/**
 * Pipeline de upload (RN-06, seção 9.9): tamanho → tipo pelo conteúdo (JPEG/PNG) → dimensões lidas antes de
 * decodificar (proteção contra bombas de descompressão) → redimensiona → recodifica em JPEG (descarta EXIF e GPS)
 * → miniatura → SHA-256 dos bytes originais.
 */
public class ImagePipeline {

    private static final double JPEG_QUALITY = 0.82;

    private final long maxBytes;
    private final int maxDimension;
    private final int thumbnailDimension;
    private final int minSourceDimension;
    private final int maxSourceDimension;

    public ImagePipeline(
            long maxBytes, int maxDimension, int thumbnailDimension, int minSourceDimension, int maxSourceDimension) {
        this.maxBytes = maxBytes;
        this.maxDimension = maxDimension;
        this.thumbnailDimension = thumbnailDimension;
        this.minSourceDimension = minSourceDimension;
        this.maxSourceDimension = maxSourceDimension;
    }

    public record Processed(byte[] jpeg, byte[] thumbnail, int width, int height, String sha256) {}

    public Processed process(byte[] original) {
        if (original.length > maxBytes) {
            throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE);
        }
        if (!isJpeg(original) && !isPng(original)) {
            throw new BusinessException(ErrorCode.INVALID_PHOTO);
        }
        checkDimensions(original);
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
            if (source == null) {
                throw new BusinessException(ErrorCode.INVALID_PHOTO);
            }
            BufferedImage resized = fit(source, maxDimension);
            byte[] jpeg = encode(resized);
            byte[] thumb = encode(fit(resized, thumbnailDimension));
            return new Processed(jpeg, thumb, resized.getWidth(), resized.getHeight(), Hashing.sha256Hex(original));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_PHOTO);
        }
    }

    private void checkDimensions(byte[] bytes) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new BusinessException(ErrorCode.INVALID_PHOTO);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                int longest = Math.max(width, height);
                int shortest = Math.min(width, height);
                if (shortest < minSourceDimension || longest > maxSourceDimension) {
                    throw new BusinessException(ErrorCode.INVALID_PHOTO);
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_PHOTO);
        }
    }

    private static BufferedImage fit(BufferedImage image, int maxSide) throws IOException {
        int longest = Math.max(image.getWidth(), image.getHeight());
        BufferedImage rgb = toRgb(image);
        if (longest <= maxSide) {
            return rgb;
        }
        return Thumbnails.of(rgb).size(maxSide, maxSide).keepAspectRatio(true).asBufferedImage();
    }

    private static BufferedImage toRgb(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        var g = rgb.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return rgb;
    }

    private static byte[] encode(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(image)
                .scale(1.0)
                .outputFormat("jpg")
                .outputQuality(JPEG_QUALITY)
                .toOutputStream(out);
        return out.toByteArray();
    }

    static boolean isJpeg(byte[] b) {
        return b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    static boolean isPng(byte[] b) {
        return b.length > 8
                && (b[0] & 0xFF) == 0x89
                && b[1] == 'P'
                && b[2] == 'N'
                && b[3] == 'G'
                && (b[4] & 0xFF) == 0x0D
                && (b[5] & 0xFF) == 0x0A;
    }
}
