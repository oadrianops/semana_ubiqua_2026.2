package br.com.nandesk.file.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("RF-18")
class ImagePipelineTest {

    private final ImagePipeline pipeline = new ImagePipeline(5 * 1024 * 1024, 1600, 400, 320, 12_000);

    @Test
    @DisplayName("RN-06: recodifica em JPEG com lado maior ≤ 1600 px e gera miniatura de 400 px")
    void resizesAndCreatesThumbnail() throws IOException {
        ImagePipeline.Processed result = pipeline.process(image(3000, 2000, "png"));
        assertThat(result.width()).isEqualTo(1600);
        assertThat(result.height()).isEqualTo(1067);
        BufferedImage full = ImageIO.read(new ByteArrayInputStream(result.jpeg()));
        BufferedImage thumb = ImageIO.read(new ByteArrayInputStream(result.thumbnail()));
        assertThat(Math.max(full.getWidth(), full.getHeight())).isEqualTo(1600);
        assertThat(Math.max(thumb.getWidth(), thumb.getHeight())).isEqualTo(400);
        assertThat(result.jpeg()[0]).isEqualTo((byte) 0xFF);
        assertThat(result.sha256()).hasSize(64);
    }

    @Test
    @DisplayName("RN-06: imagem pequena não é ampliada")
    void doesNotUpscale() throws IOException {
        ImagePipeline.Processed result = pipeline.process(image(800, 600, "jpg"));
        assertThat(result.width()).isEqualTo(800);
        assertThat(result.height()).isEqualTo(600);
    }

    @Test
    @DisplayName("RN-06: aceita só JPEG e PNG, verificados pelo conteúdo e não pela extensão")
    void rejectsByContent() {
        byte[] gif = "GIF89a-not-an-allowed-image".getBytes();
        assertThatThrownBy(() -> pipeline.process(gif))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.INVALID_PHOTO);
    }

    @Test
    @DisplayName("RN-06: recusa arquivo acima de 5 MB")
    void rejectsTooLarge() {
        ImagePipeline small = new ImagePipeline(10, 1600, 400, 320, 12_000);
        assertThatThrownBy(() -> small.process(new byte[20]))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.PAYLOAD_TOO_LARGE);
    }

    @Test
    @DisplayName("RN-06: recusa dimensões abaixo de 320 px (proteção do pipeline)")
    void rejectsTinyImages() throws IOException {
        byte[] tiny = image(100, 100, "png");
        assertThatThrownBy(() -> pipeline.process(tiny))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.INVALID_PHOTO);
    }

    @Test
    @DisplayName("RN-06: a mesma foto gera o mesmo hash (detecção de foto repetida)")
    void sameHashForSameBytes() throws IOException {
        byte[] bytes = image(640, 480, "jpg");
        assertThat(pipeline.process(bytes).sha256())
                .isEqualTo(pipeline.process(bytes).sha256());
    }

    static byte[] image(int width, int height, String format) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(74, 33, 82));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(242, 178, 58));
        g.fillOval(width / 4, height / 4, width / 2, height / 2);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }
}
