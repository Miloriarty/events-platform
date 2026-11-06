package org.example.pass;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.example.pass.service.QrCodes;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QrCodesTest {

    @Test
    void generatedCodeReadsBack() throws IOException, NotFoundException, ChecksumException, FormatException {
        String link = "http://localhost:4200/check-in?token=" + "a".repeat(180);

        var image = ImageIO.read(new ByteArrayInputStream(new QrCodes().png(link)));
        var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));

        assertEquals(link, new QRCodeReader().decode(bitmap).getText());
    }
}
